package com.jam.dentsu.noupathyproto

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioAttributes
import android.media.SoundPool
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.*
import kotlinx.android.synthetic.main.activity_record.*
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.schedule
import kotlin.concurrent.thread
import com.polyak.iconswitch.IconSwitch.Checked
import com.polyak.iconswitch.IconSwitch
import org.json.JSONObject
import java.io.*

var learning_progress_percent: Double = 0.0

class RecordActivity : AppCompatActivity(), neuroNicleService.Companion.NNListener, LoaderManager.LoaderCallbacks<String>, IconSwitch.CheckedChangeListener {

    private var playerDialog: AlertDialog? = null
    private var nowPlayingImage: ImageView? = null
    private var playerProgress: ProgressBar? = null

    private var learnDialog: AlertDialog? = null
    private var learnResultDialog: AlertDialog? = null
    private var noiseErrorDialog: AlertDialog? = null
    private var recordEndDialog: AlertDialog? = null
    private var resultAccuracy5 = "70.01%"
    private var resultAccuracy4 = "88.0%"
    private var resultAccuracy3 = "82.45%"
    private var resultAccuracy2 = "00.00%"
    private var p300Point = 0.0
    private val p300AveragePoint = 130.0

    lateinit var pref: SharedPreferences

    private lateinit var sp: SoundPool
    private var soundList = mutableListOf(0, 0, 0, 0, 0)
    private var sound5000 = 0
    private var sound_noise = 0
    private var current_sound_noise = 0
    private var isSoundSelectEnabled = true

    private val handler = Handler()
    private var setCount = 0
    private var playCount = 0
    private var playList = mutableListOf<Int>()
    private var currentSound = ""

    private var currentDataset = ""
    private var currentTarget = 0

    private var pw: PrintWriter? = null
    private var currentFileName = ""
    private var isSuspend = false
    private var isLearningDone = false
    private var isRecording = false
    private var isSoundArrayPlaying = false
    private val format = SimpleDateFormat("yyyyMMdd-HH:mm:ss.SSS", Locale.getDefault())

    private var sound_levels = arrayOf(0.01f, 0.01f, 0.01f, 0.01f, 0.01f)
    private var pitch_levels = arrayOf(1.0f, 1.0f, 1.0f, 1.0f, 1.0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)

        neuroNicleService.setListener(this)
        volume.setCheckedChangeListener(this)
        volume.toggle()

        pref = getSharedPreferences("prefs", AppCompatActivity.MODE_PRIVATE)
        currentDataset = pref.getString("dataset_record", "DATASET_1")
        datasetName.text = currentDataset

        // 再生準備
        loadSounds(getSoundSetID(currentDataset))
        playList = makePlayList()

        loadImages(getSoundSetID(currentDataset))

        // データ取得状況
        updateLearningStatus(false)

        Timer().schedule(500, 1000) {
            runOnUiThread {
                if (neuroNicleService.instance.isConnected) {

                    connect.setImageResource(R.drawable.com_ok_icon)

                    if (!neuroNicleService.instance.batteryAlert) {
                        battery.translationY = 0F
                        battery.textSize = 30.0F
                        battery.text = "OK"
                    } else {
                        battery.translationY = -10F
                        battery.textSize = 22.0F
                        battery.text = "要充電"
                    }
                } else {
                    connect.setImageResource(R.drawable.com_ng_icon)

                    battery.text = "---"
                }

                if (neuroNicleService.instance.isFitting && !neuroNicleService.instance.noiseDetected) {
                    fitting.setImageResource(R.drawable.com_ok_icon)
                } else {
                    fitting.setImageResource(R.drawable.com_ng_icon)

                    if (isSoundArrayPlaying) { //最初の最後の空白時間のノイズは許容する
                        isSuspend = true
                        setCount = SetSize + 2
                    }
                }

                //Log.d("fitting", isSoundArrayPlaying.toString())

                if (neuroNicleService.instance.calibTime > 0) {
                    calibration.text = "${neuroNicleService.instance.calibTime}"
                } else {
                    calibration.text = "OK"
                }

                if (neuroNicleService.instance.calibTime > 0) {
                    calibration.text = "${neuroNicleService.instance.calibTime}"
                    start.isEnabled = false
                } else {
                    calibration.text = "OK"
                    start.isEnabled = !isLearningDone && !isRecording && !neuroNicleService.instance.noiseDetected
                }
            }
        }

        selectBtn.setOnClickListener {
            val intent = Intent(this, DatasetActivity::class.java)
            intent.putExtra("isPlay", false)
            startActivityForResult(intent, 1)
        }

        soundBtn.setOnClickListener {
            if (isSoundSelectEnabled) {
                val intent = Intent(this, SoundSelectActivity::class.java)
                intent.putExtra("currentDataset", currentDataset)
                startActivityForResult(intent, 2)
            } else {
                Toast.makeText(applicationContext, "記録の開始後は音を変更できません", Toast.LENGTH_SHORT).show()
            }
        }

        play1.setOnClickListener {
            // play（ロードしたID, 左音量, 右音量, 優先度, ループ, 再生速度）
            sp.play(soundList[0], 1.0f, 1.0f, 0, 0, 1.0f)
        }
        play2.setOnClickListener { sp.play(soundList[1], 1.0f, 1.0f, 0, 0, 1.0f) }
        play3.setOnClickListener { sp.play(soundList[2], 1.0f, 1.0f, 0, 0, 1.0f) }
        play4.setOnClickListener { sp.play(soundList[3], 1.0f, 1.0f, 0, 0, 1.0f) }
        play5.setOnClickListener { sp.play(soundList[4], 1.0f, 1.0f, 0, 0, 1.0f) }

        val runnable = object : Runnable {
            override fun run() {

                if (setCount == 0) {
                    println("start")
                    current_sound_noise = sp.play(sound_noise, 0.5f, 0.5f, 0, -1, 1.0f)
                    isSoundArrayPlaying = false
                    sp.play(sound5000, 1.0f, 1.0f, 0, 0, 1.0f)
                    setCount++
                    nowPlaying("0", nowPlayingImage!!, playerProgress!!)
                    handler.postDelayed(this, 5000)
                } else if (setCount <= SetSize) {
                    println("set: ${setCount}")
                    isSoundArrayPlaying = true
                    currentSound = playList[0].toString()
                    println("sound: ${currentSound}")

                    //反応によって音量を変える
                    val currentSoundInt = currentSound.toInt()-1
                    sp.play(soundList[currentSoundInt], sound_levels[currentSoundInt], sound_levels[currentSoundInt], 0, 0, 1.0f)

                    nowPlaying(currentSound, nowPlayingImage!!, playerProgress!!)
                    playList.removeAt(0)
                    playCount++

                    if (playCount % 5 == 0) {
                        setCount++
                    }

                    //ISIに乱数を加える
                    //handler.postDelayed(this, SOA+Random().nextInt(200)-100)
                    //通常のISI
                    handler.postDelayed(this, SOA)
                } else if (setCount == SetSize + 1) {
                    // 最後の空白
                    isSoundArrayPlaying = false
                    currentSound = ""
                    setCount++
                    sp.play(sound5000, 1.0f, 1.0f, 0, 0, 1.0f)
                    nowPlaying("0", nowPlayingImage!!, playerProgress!!)
                    handler.postDelayed(this, 1000)
                    Log.d("sound_noise","stop")
                    sp.stop(current_sound_noise)
                } else {
                    println("end")
                    setCount = 0
                    playCount = 0
                    playerDialog!!.dismiss()
                    playList = makePlayList()
                    isRecording = false
                    pw?.close()
                    if (isSuspend) {
                        deleteFile(currentDataset, currentFileName)
                        isSuspend = false
                        isSoundArrayPlaying = false
                        showNoiseErrorDialog()
                        sp.stop(current_sound_noise)
                    } else {
                        updateLearningStatus(true)
                    }
                    handler.removeCallbacks(this)
                }
            }
        }

        start.setOnClickListener {

            showPlayerDialog()
            currentSound = ""
            isRecording = true

            sound_levels[0] = 0.05f
            sound_levels[1] = 0.05f
            sound_levels[2] = 0.05f
            sound_levels[3] = 0.05f
            sound_levels[4] = 0.05f

            pitch_levels[0] = 1.0f
            pitch_levels[1] = 1.0f
            pitch_levels[2] = 1.0f
            pitch_levels[3] = 1.0f
            pitch_levels[4] = 1.0f

            val time = Date()
            val file_format = SimpleDateFormat("yyyyMMdd_HHmmss_", Locale.getDefault())
            val fileTime = file_format.format(time)
            currentFileName = "${fileTime}${currentTarget}.csv"
            val fw =
                FileWriter("${Environment.getExternalStorageDirectory().getPath()}/Noupathy/${currentDataset}/${currentFileName}")
            pw = PrintWriter(BufferedWriter(fw))
            pw?.print("Timestamp,Ch1,Ch2,Sound\n")

            handler.post(runnable)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {

            val selectedDataset = data.getStringExtra("dataset")
            currentDataset = selectedDataset
            // 保存
            pref.edit().putString("dataset_record", currentDataset).apply()
            // 表示
            datasetName.text = currentDataset
            updateLearningStatus(false)
        }

        loadSounds(getSoundSetID(currentDataset))
        loadImages(getSoundSetID(currentDataset))
    }

    override fun onResume() {
        super.onResume()

        neuroNicleService.setListener(this)
    }

    override fun onPause() {
        super.onPause()

        neuroNicleService.setListener(null)
    }

    private var temp_sound_label = 0
    private var stim_count = 0
    private var temp_Ch1 = 0
    private var temp_Ch2 = 0
    private var ground_Ch1 = mutableListOf(0,0,0,0,0)
    private var ground_Ch2 = mutableListOf(0,0,0,0,0)

    override fun onDataReceived(Ch1: Int, Ch2: Int) {

        if (isRecording) {

            val time = Date()
            pw?.print("${format.format(time)},${Ch1},${Ch2},${currentSound}\n")
            if(currentSound!="") {
                ground_Ch1.removeAt(0)
                ground_Ch2.removeAt(0)
                ground_Ch1.add(Ch1)
                ground_Ch2.add(Ch2)
                if (currentSound.toInt() != temp_sound_label) {
                    if (currentTarget == currentSound.toInt()) {
                        stim_count = 0
                        temp_Ch1 = ground_Ch1.average().toInt()
                        temp_Ch2 = ground_Ch2.average().toInt()
                    }
                    temp_sound_label = currentSound.toInt()
                }
                if (stim_count == 125) {
                    if ((Ch1 - temp_Ch1 + Ch2 - temp_Ch2) / 2 > 100) {
                        sound_levels[currentTarget - 1] += 0.10f
                        pitch_levels[currentTarget -1] = 1.05f
                    }
                    else{
                        sound_levels[currentTarget - 1] -= 0.10f
                        pitch_levels[currentTarget -1] = 1.0f
                    }
                    if(sound_levels[currentTarget - 1]<=0){
                        sound_levels[currentTarget - 1] = 0.05f
                    }

                }
                stim_count += 1
            }
        }
    }

    private fun updateLearningStatus(isRecordEnd: Boolean) {

        val status = getDirStatus(currentDataset)
        println(status)

        status1.text = "${status.data1} / 3"
        status2.text = "${status.data2} / 3"
        status3.text = "${status.data3} / 3"
        status4.text = "${status.data4} / 3"
        status5.text = "${status.data5} / 3"

        currentTarget = getNextTarget(status)
        when (currentTarget) {
            -1 -> {
                isLearningDone = true
                targetImg.visibility = View.INVISIBLE
                doneImg.visibility = View.VISIBLE
                accLbl.visibility = View.VISIBLE
                accLbl.text = getAccuracy(currentDataset,5)
            }
            0 -> {
                isLearningDone = false
                showLearnDialog()
            }
            1, 2, 3, 4, 5 -> {
                if (isRecordEnd) showRecordEndDialog()
                isLearningDone = false
                targetImg.visibility = View.VISIBLE
                doneImg.visibility = View.INVISIBLE
                accLbl.visibility = View.INVISIBLE
                when (currentTarget) {
                    1 -> targetImg.translationX = -590F
                    2 -> targetImg.translationX = -295F
                    3 -> targetImg.translationX = 0F
                    4 -> targetImg.translationX = 295F
                    5 -> targetImg.translationX = 590F
                }
            }
        }

        isSoundSelectEnabled = status.data1 > 0
        isSoundSelectEnabled = true
    }

    // ローダーが要求された時
    override fun onCreateLoader(p0: Int, p1: Bundle?): Loader<String> {
        val param: String? = p1?.getString("dir")
        val user_id = pref.getString("user_id", "000003")
        val sound_set_id = getSoundSetID(currentDataset)
        return LearningLoader(this, param, user_id!!, sound_set_id)
    }

    // ローダーがリセットされた時
    override fun onLoaderReset(p0: Loader<String>) {
        // 何もしない
    }

    // 学習完了コールバック
    override fun onLoadFinished(p0: Loader<String>, p1: String?) {

        if (p1 != null) {
            saveLearningData(currentDataset, p1)
            resultAccuracy5 = getAccuracy(currentDataset,5)
            resultAccuracy4 = getAccuracy(currentDataset,4)
            resultAccuracy3 = getAccuracy(currentDataset,3)
            resultAccuracy2 = getAccuracy(currentDataset,2)

            //反応速度
            p300Point = getP300Time(currentDataset)

            updateLearningStatus(false)
            learnDialog!!.dismiss()
            supportLoaderManager.destroyLoader(1)
            showLearnResultDialog()
        }
    }

    private fun showPlayerDialog() {

        val inflater = this.layoutInflater.inflate(R.layout.dialog_player, null, false)
        val stopBtn = inflater.findViewById<Button>(R.id.stop)
        val image1 = inflater.findViewById<ImageView>(R.id.plyr1)
        val image2 = inflater.findViewById<ImageView>(R.id.plyr2)
        val image3 = inflater.findViewById<ImageView>(R.id.plyr3)
        val image4 = inflater.findViewById<ImageView>(R.id.plyr4)
        val image5 = inflater.findViewById<ImageView>(R.id.plyr5)
        when (getSoundSetID(currentDataset)) {
            1 -> {
                image1.setImageResource(R.drawable.plyr_s1_img)
                image2.setImageResource(R.drawable.plyr_s2_img)
                image3.setImageResource(R.drawable.plyr_s3_img)
                image4.setImageResource(R.drawable.plyr_s4_img)
                image5.setImageResource(R.drawable.plyr_s5_img)
            }
            2 -> {
                image1.setImageResource(R.drawable.plyr_s2_1_img)
                image2.setImageResource(R.drawable.plyr_s2_2_img)
                image3.setImageResource(R.drawable.plyr_s2_3_img)
                image4.setImageResource(R.drawable.plyr_s2_4_img)
                image5.setImageResource(R.drawable.plyr_s2_5_img)
            }
            3 -> {
                image1.setImageResource(R.drawable.plyr_s3_1_img)
                image2.setImageResource(R.drawable.plyr_s3_2_img)
                image3.setImageResource(R.drawable.plyr_s3_3_img)
                image4.setImageResource(R.drawable.plyr_s3_4_img)
                image5.setImageResource(R.drawable.plyr_s3_5_img)
            }
        }
        playerProgress = inflater.findViewById(R.id.progress)
        nowPlayingImage = inflater.findViewById(R.id.image)
        nowPlayingImage!!.visibility = View.INVISIBLE

        playerDialog = AlertDialog.Builder(this).apply {

            setView(inflater)
            stopBtn.setOnClickListener {
                // 途中終了
                isSuspend = true
                setCount = SetSize + 2
            }
        }.create()

        playerDialog!!.setCancelable(false)
        playerDialog!!.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        playerDialog!!.show()
    }

    private fun showRecordEndDialog() {

        val inflater = this.layoutInflater.inflate(R.layout.dialog_record_end, null, false)
        val endBtn = inflater.findViewById<Button>(R.id.endBtn)
        recordEndDialog = AlertDialog.Builder(this).apply {
            setView(inflater)
            endBtn.setOnClickListener {
                recordEndDialog!!.dismiss()
            }
        }.create()

        recordEndDialog!!.setCancelable(false)
        recordEndDialog!!.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        recordEndDialog!!.show()
    }

    private fun showLearnDialog() {

        val inflater = this.layoutInflater.inflate(R.layout.dialog_learning, null, false)
        val descLbl = inflater.findViewById<TextView>(R.id.learningDesc)
        val progress = inflater.findViewById<ProgressBar>(R.id.learningProgressPercent)
        val learnBtn = inflater.findViewById<Button>(R.id.lerningBtn)
        val cancelBtn = inflater.findViewById<Button>(R.id.cancelBtn)
        val sendingLbl = inflater.findViewById<TextView>(R.id.sending_text_label)

        learnDialog = AlertDialog.Builder(this).apply {
            setView(inflater)
            learnBtn.setOnClickListener {
                learnBtn.visibility = android.widget.Button.INVISIBLE
                progress.visibility = android.widget.ProgressBar.VISIBLE
                progress.setMax(100)
                descLbl.text = "学習中です\nしばらくこのままでお待ちください"
                learning_progress_percent = 0.0
                thread {
                    while (learning_progress_percent < 100) {
                        progress.setProgress(learning_progress_percent.toInt())
                        sendingLbl.post(Runnable() {
                            sendingLbl.setText("%.2f".format(learning_progress_percent)+"％ 完了")
                        })
                        Thread.sleep(1000)
                    }
                }
                val args = Bundle()
                args.putString("dir", currentDataset)
                supportLoaderManager.initLoader(1, args, this@RecordActivity)
            }
            cancelBtn.setOnClickListener {
                learnDialog!!.dismiss()
            }

        }.create()

        learnDialog!!.setCancelable(false)
        learnDialog!!.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        learnDialog!!.show()
    }

    private fun showLearnResultDialog() {

        val inflater = this.layoutInflater.inflate(R.layout.dialog_learn_result, null, false)
        val accLbl1 = inflater.findViewById<TextView>(R.id.accLbl1)
        val accLbl2 = inflater.findViewById<TextView>(R.id.accLbl2)
        val accLbl3 = inflater.findViewById<TextView>(R.id.accLbl3)
        val accLbl4 = inflater.findViewById<TextView>(R.id.accLbl4)
        val accLbl5 = inflater.findViewById<TextView>(R.id.accLbl5)
        val accDesc = inflater.findViewById<TextView>(R.id.accDesc)
        val indicator = inflater.findViewById<ImageView>(R.id.speedIndicator)
        val speedDesc = inflater.findViewById<TextView>(R.id.speedDesc)
        val endBtn = inflater.findViewById<Button>(R.id.doneBtn)

        learnResultDialog = AlertDialog.Builder(this).apply {
            setView(inflater)
              val classAcc = getClassAccuracy(currentDataset)
             accLbl1.text = String.format("%.2f", classAcc[0]*100)+"%"
             accLbl2.text = String.format("%.2f", classAcc[1]*100)+"%"
             accLbl3.text = String.format("%.2f", classAcc[2]*100)+"%"
             accLbl4.text = String.format("%.2f", classAcc[3]*100)+"%"
             accLbl5.text = String.format("%.2f", classAcc[4]*100)+"%"
             /*
             var classNum = ""
             if (resultAccuracy5.split("%")[0].toDouble()>80) {
                 classNum = "5分類"
                 hiligtedText(accLbl5)
             } else if (resultAccuracy4.split("%")[0].toDouble()>80) {
                 classNum = "4分類"
                 hiligtedText(accLbl4)
             } else if (resultAccuracy3.split("%")[0].toDouble()>80) {
                 classNum = "3分類"
                 hiligtedText(accLbl3)
             } else {
                 classNum = "2分類"
                 hiligtedText(accLbl2)
             }
             */
             //accDesc.text = "このデータセットでは${classNum}が使用されます"

            if (p300Point < p300AveragePoint) {
                speedDesc.text = "反応が少し早いようです。\n音が鳴ってから回数を数えるタイミングを\n遅くするように意識しましょう"
            } else if (p300Point > p300AveragePoint) {
                speedDesc.text = "反応が少し遅いようです。\n音が鳴ってから回数を数えるタイミングを\n早くするように意識しましょう"
            } else {
                speedDesc.text = "ちょうど良いタイミングです。"
            }

            endBtn.setOnClickListener {
                learnResultDialog!!.dismiss()
            }
        }.create()

        learnResultDialog!!.setCancelable(false)
        learnResultDialog!!.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        learnResultDialog!!.show()
        showSpeedGraph(indicator)
    }

    private fun hiligtedText(text: TextView) {
        text.textSize = 20.5F
        text.setTextColor(getColor(R.color.peachy_pink))
    }

    private fun showSpeedGraph(indicator: ImageView) {
        val now = 570.0 //indicator.x
        val target = 695.0 //graphR.x
        val per = p300Point / p300AveragePoint - 1
        val distance = (target - now) * per * 2

        startTranslate(distance.toFloat(), indicator)
    }

    private fun startTranslate(to: Float, target: ImageView) {
        val translateAnimation = TranslateAnimation(
            Animation.ABSOLUTE, 0.0f,
            Animation.ABSOLUTE, to,
            Animation.ABSOLUTE, 0.0f,
            Animation.ABSOLUTE, 0.0f
        )

        translateAnimation.duration = 2500
        translateAnimation.repeatCount = 0
        translateAnimation.fillAfter = true

        target.startAnimation(translateAnimation)
    }

    private fun showNoiseErrorDialog() {

        val inflater = this.layoutInflater.inflate(R.layout.dialog_noise_error, null, false)
        val okBtn = inflater.findViewById<Button>(R.id.endBtn)
        noiseErrorDialog = AlertDialog.Builder(this).apply {
            setView(inflater)
            okBtn.setOnClickListener {
                noiseErrorDialog!!.dismiss()
            }
        }.create()

        noiseErrorDialog!!.setCancelable(false)
        noiseErrorDialog!!.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        noiseErrorDialog!!.show()
    }

    private fun loadSounds(soundSetID: Int) {

        val sArray = mutableListOf(
            mutableListOf(0, 0, 0, 0, 0),
            mutableListOf(0, 0, 0, 0, 0),
            mutableListOf(0, 0, 0, 0, 0)
        )

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        sp = SoundPool.Builder()
            .setAudioAttributes(audioAttributes)
            .setMaxStreams(5)
            .build()

        val dir = "${Environment.getExternalStorageDirectory().path}/${APP_ROOT}"
        val part_path = "rrawslist.json"
        val file = File(dir, part_path)

        val reader = BufferedReader(FileReader(file))
        val json = JSONObject(reader.readLines().joinToString())

        println("json: $json")
        println("RRawSList: ${json["RRawSList"]}")

        val ex = json["RRawSList"].toString()
        val exList = ex.substring(2..(ex.length-3)).split("], [").toMutableList()
        for (i in 0 until 3) {
            val partNewExList = exList[i].split(", ")
            for (j in 0 until 5) {
                sArray[i][j] = partNewExList[j].toInt()
            }
        }
        println("sArray: $sArray, is int: ${sArray[0][0] is Int}")

        for (i in 0 until 5) {
            soundList[i] = sp.load(this, sArray[soundSetID-1][i], 1)
        }
        sound5000 = sp.load(this, R.raw.s5000, 1)

        // ロードが終わったか確認
        sp.setOnLoadCompleteListener { sp, sampleId, status ->
            /*
            Log.d("debug", "sampleId=$sampleId")
            Log.d("debug", "status=$status")
             */
        }
    }

    private fun loadImages(soundSetID: Int) {

        when (soundSetID) {
            1 -> {
                play1.setBackgroundDrawable(getDrawable(R.drawable.rec_s1_btn_set))
                play2.setBackgroundDrawable(getDrawable(R.drawable.rec_s2_btn_set))
                play3.setBackgroundDrawable(getDrawable(R.drawable.rec_s3_btn_set))
                play4.setBackgroundDrawable(getDrawable(R.drawable.rec_s4_btn_set))
                play5.setBackgroundDrawable(getDrawable(R.drawable.rec_s5_btn_set))
            }
            2 -> {
                play1.setBackgroundDrawable(getDrawable(R.drawable.rec_s2_1_btn_set))
                play2.setBackgroundDrawable(getDrawable(R.drawable.rec_s2_2_btn_set))
                play3.setBackgroundDrawable(getDrawable(R.drawable.rec_s2_3_btn_set))
                play4.setBackgroundDrawable(getDrawable(R.drawable.rec_s2_4_btn_set))
                play5.setBackgroundDrawable(getDrawable(R.drawable.rec_s2_5_btn_set))
            }
            3 -> {
                play1.setBackgroundDrawable(getDrawable(R.drawable.rec_s3_1_btn_set))
                play2.setBackgroundDrawable(getDrawable(R.drawable.rec_s3_2_btn_set))
                play3.setBackgroundDrawable(getDrawable(R.drawable.rec_s3_3_btn_set))
                play4.setBackgroundDrawable(getDrawable(R.drawable.rec_s3_4_btn_set))
                play5.setBackgroundDrawable(getDrawable(R.drawable.rec_s3_5_btn_set))
            }
        }
    }

    override fun onCheckChanged(current: Checked?) {

        if (current.toString() == "LEFT") {
            play1.setOnClickListener { sp.play(soundList[0], 0.05f, 0.05f, 0, 0, 1.0f) }
            play2.setOnClickListener { sp.play(soundList[1], 0.05f, 0.05f, 0, 0, 1.0f) }
            play3.setOnClickListener { sp.play(soundList[2], 0.05f, 0.05f, 0, 0, 1.0f) }
            play4.setOnClickListener { sp.play(soundList[3], 0.05f, 0.05f, 0, 0, 1.0f) }
            play5.setOnClickListener { sp.play(soundList[4], 0.05f, 0.05f, 0, 0, 1.0f) }
        }
        else {
            play1.setOnClickListener { sp.play(soundList[0], 1.0f, 1.0f, 0, 0, 1.0f) }
            play2.setOnClickListener { sp.play(soundList[1], 1.0f, 1.0f, 0, 0, 1.0f) }
            play3.setOnClickListener { sp.play(soundList[2], 1.0f, 1.0f, 0, 0, 1.0f) }
            play4.setOnClickListener { sp.play(soundList[3], 1.0f, 1.0f, 0, 0, 1.0f) }
            play5.setOnClickListener { sp.play(soundList[4], 1.0f, 1.0f, 0, 0, 1.0f) }
        }
    }
}
