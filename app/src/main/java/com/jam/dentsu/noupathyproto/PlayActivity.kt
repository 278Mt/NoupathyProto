package com.jam.dentsu.noupathyproto

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioAttributes
import android.media.SoundPool
import android.opengl.Visibility
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import android.widget.*
import kotlinx.android.synthetic.main.activity_play.*
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.schedule
import com.polyak.iconswitch.IconSwitch.Checked
import com.polyak.iconswitch.IconSwitch
import kotlinx.android.synthetic.main.dialog_play_result.*

class PlayActivity : AppCompatActivity(), neuroNicleService.Companion.NNListener, LoaderManager.LoaderCallbacks<ResultData>, IconSwitch.CheckedChangeListener {

    private var playerDialog: AlertDialog? = null
    private var nowPlayingImg: ImageView? = null
    private var playerProgress: ProgressBar? = null

    private var loadingDialog: AlertDialog? = null
    private var resultDialog: AlertDialog? = null
    private var noiseErrorDialog: AlertDialog? = null
    private var resultData =ResultData()
    private var wordEditDialog: AlertDialog? = null

    lateinit var pref : SharedPreferences
    private var words = mutableListOf<String>()

    private lateinit var sp: SoundPool
    private var sound1 = 0
    private var sound2 = 0
    private var sound3 = 0
    private var sound4 = 0
    private var sound5 = 0
    private var sound5000 = 0
    private var sound_noise = 0
    private var current_sound_noise = 0

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
    private var isRecording = false
    private var isSoundArrayPlaying = false
    private val format = SimpleDateFormat("yyyyMMdd-HH:mm:ss.SSS", Locale.getDefault())
    private var classNum = 2
    private var sound_levels = arrayOf(0.01f, 0.01f, 0.01f, 0.01f, 0.01f)
    private var acc_levels = arrayOf(0.0, 0.0, 0.0, 0.0, 0.0)

    private var p300Point = 125
    private var p300Volt = 50
    private var n200Point = 50
    private var n200Volt = -30

    private var repeatCount = 0
    private val repeatSize = 0
    private var safeCount = 0

    private var left_class = 1
    private var right_class = 5
    private var select_counts = arrayOf(0,0,0,0,0)

    private val runnable = object : Runnable {
        override fun run() {
            Log.d("setCount",setCount.toString())

            if (setCount == 0) {
                println("start")
                current_sound_noise = sp.play(sound_noise, 0.5f, 0.5f, 0, -1, 1.0f)
                isSoundArrayPlaying=false
                sp.play(sound5000, 1.0f, 1.0f, 0,0,1.0f)
                setCount++
                repeatCount = 0
                nowPlaying("0", nowPlayingImg!!, playerProgress!!)
                handler.postDelayed(this, 5000)
            }
            else if (setCount <= SetSize) {
                println("set: " + setCount)
                isSoundArrayPlaying=true
                currentSound = playList[0].toString()
                println("sound: " + currentSound)

//                    when(currentSound) {
//                        "1" -> sp.play(sound1, 1.0f, 1.0f, 0, 0, 1.0f)
//                        "2" -> sp.play(sound2, 1.0f, 1.0f, 0, 0, 1.0f)
//                        "3" -> sp.play(sound3, 1.0f, 1.0f, 0, 0, 1.0f)
//                        "4" -> sp.play(sound4, 1.0f, 1.0f, 0, 0, 1.0f)
//                        "5" -> sp.play(sound5, 1.0f, 1.0f, 0, 0, 1.0f)
//                    }

                //反応によって音量を変える
                when (currentSound) {
                    "1" -> sp.play(sound1, sound_levels[0], sound_levels[0], 0, 0, 1.0f)
                    "2" -> sp.play(sound2, sound_levels[1], sound_levels[1], 0, 0, 1.0f)
                    "3" -> sp.play(sound3, sound_levels[2], sound_levels[2], 0, 0, 1.0f)
                    "4" -> sp.play(sound4, sound_levels[3], sound_levels[3], 0, 0, 1.0f)
                    "5" -> sp.play(sound5, sound_levels[4], sound_levels[4], 0, 0, 1.0f)
                }

                nowPlaying(currentSound, nowPlayingImg!!, playerProgress!!)
                playList.removeAt(0)
                playCount++

                if (playCount % 5 == 0) {
                    setCount++
                }

                // 信号の分散を確認して終わらせるかどうか判定
                if(playCount==SetSize*5 && repeatCount < repeatSize && neuroNicleService.instance.debug_on==false) {
                    if ((calcVariance(v_argt_ch1[0]) + calcVariance(v_argt_ch2[0])) / 2 > 1000) {
                        setCount = 1
                        repeatCount += 1
                    } else if ((calcVariance(v_argt_ch1[1]) + calcVariance(v_argt_ch2[1])) / 2 > 1000) {
                        setCount = 1
                        repeatCount += 1
                    } else if ((calcVariance(v_argt_ch1[2]) + calcVariance(v_argt_ch2[2])) / 2 > 1000) {
                        setCount = 1
                        repeatCount += 1
                    } else if ((calcVariance(v_argt_ch1[3]) + calcVariance(v_argt_ch2[3])) / 2 > 1000) {
                        setCount = 1
                        repeatCount += 1
                    } else if ((calcVariance(v_argt_ch1[4]) + calcVariance(v_argt_ch2[4])) / 2 > 1000) {
                        setCount = 1
                        repeatCount += 1
                    }
                    setCount=1
                    repeatCount+=1
                }

                handler.postDelayed(this, SOA)
            }
            else if (setCount == SetSize+1) {
                for(i in 0..4){
                    v_argt_ch1[i].clear()
                    v_argt_ch2[i].clear()
                }

                // 最後の空白
                isSoundArrayPlaying=false
                currentSound = ""
                setCount++
                sp.play(sound5000, 1.0f, 1.0f, 0,0,1.0f)
                nowPlaying("0", nowPlayingImg!!, playerProgress!!)
                handler.postDelayed(this, 1000)
                Log.d("sound_noise","stop")
                sp.pause(current_sound_noise)
            }
            else {
                Log.d("setCount","end")
                println("end")
                setCount = 0
                playCount = 0
                playerDialog!!.dismiss()
                playList = makePlayList()
                isRecording = false
                pw?.close()
                handler.removeCallbacks(this)
                if (isSuspend) {
                    deleteFile(currentDataset, currentFileName)
                    isSuspend = false
                    isSoundArrayPlaying=false
                    showNoiseErrorDialog()
                    Log.d("sound_noise","suspend")
                    sp.stop(current_sound_noise)
                } else {
                    showLoadingDialog()
                    val args = Bundle()
                    args.putString("dir", currentDataset)
                    args.putString("file", currentFileName)
                    supportLoaderManager.initLoader(1, args, this@PlayActivity)
                }
            }
        }
    }

    fun calcVariance(data: MutableList<Int>) : Double{
        var variance : Double = 0.0
        var average = data.average()
        for(i in 0..(data.count()-1)){
            variance += (data[i]-average) * (data[i]-average)
        }
        variance /= data.count()

        Log.d("var",(variance/1000.0).toString())

        return variance/1000.0
    }

    override fun onStart() {
        super.onStart()
        setClassLimit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)

        neuroNicleService.setListener(this)
        volumeSw.setCheckedChangeListener(this)
        volumeSw.toggle()

        // 言葉
        setWords()

        currentDataset = pref.getString("dataset_play", "DATASET_1")
        datasetName.text = currentDataset

        //クラス数
        setClassLimit()

        // 再生準備
        loadSounds(getSoundSetID(currentDataset))
        playList = makePlayList()

        loadImages(getSoundSetID(currentDataset))

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

                    if(isSoundArrayPlaying){ //最初の最後の空白時間のノイズは許容する
                        isSuspend = true
                        setCount = SetSize+2
                    }
                }

                if (neuroNicleService.instance.calibTime > 0) {
                    calibration.text = "${neuroNicleService.instance.calibTime}"
                    startBtn.isEnabled = false
                } else {
                    calibration.text = "OK"
                    if (!isRecording && !neuroNicleService.instance.noiseDetected) {
                        startBtn.isEnabled = true
                    } else {
                        startBtn.isEnabled = false
                    }
                }
            }
        }

        selectBtn.setOnClickListener {
            val intent = Intent(this, DatasetActivity::class.java)
            intent.putExtra("isPlay", true)
            startActivityForResult(intent, 1)
        }

        play1.setOnClickListener {
            // play（ロードしたID, 左音量, 右音量, 優先度, ループ, 再生速度）
            sp.play(sound1, 1.0f, 1.0f, 0, 0, 1.0f)
        }
        play2.setOnClickListener { sp.play(sound2, 1.0f, 1.0f, 0, 0, 1.0f) }
        play3.setOnClickListener { sp.play(sound3, 1.0f, 1.0f, 0, 0, 1.0f) }
        play4.setOnClickListener { sp.play(sound4, 1.0f, 1.0f, 0, 0, 1.0f) }
        play5.setOnClickListener { sp.play(sound5, 1.0f, 1.0f, 0, 0, 1.0f) }

        startBtn.setOnClickListener {

            showPlayerDialog()
            currentSound = ""
            isRecording = true
            safeCount = 0

            stim_counts = arrayOf(-250,-250,-250,-250,-250)
            sound_levels = arrayOf(0.05f, 0.05f, 0.05f, 0.05f, 0.05f)
            select_counts = arrayOf(0,0,0,0,0)
            for(i in 0..4) {
                acc_levels[i] = 0.0
            }
            val time = Date()
            val file_format = SimpleDateFormat("yyyyMMdd_HHmmss_", Locale.getDefault())
            val fileTime = file_format.format(time)
            currentFileName = fileTime + currentTarget + "_play.csv"
            val fw = FileWriter(Environment.getExternalStorageDirectory().getPath() + "/Noupathy/" + currentDataset + "/" + currentFileName)
            pw = PrintWriter(BufferedWriter(fw))
            pw?.print("Timestamp,Ch1,Ch2,Sound\n")

            handler.post(runnable)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {

            val selectedDataset = data.getStringExtra("dataset")
            currentDataset = selectedDataset
            loadSounds(getSoundSetID(currentDataset))
            loadImages(getSoundSetID(currentDataset))
            pref.edit().putString("dataset_play", currentDataset).apply()
            datasetName.text = currentDataset
        }
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
    private var stim_counts = arrayOf(-250,-250,-250,-250,-250)

    //音量調整用のeeg判定
    private var temp_ch1s = arrayOf(0,0,0,0,0)
    private var temp_ch2s = arrayOf(0,0,0,0,0)
    //音量調整用のeeg判定をするためのグラウンド
    private var ground_ch1 = mutableListOf(0,0,0,0,0)
    private var ground_ch2 = mutableListOf(0,0,0,0,0)

    //信号分散計測用の配列
    private var v_argt_ch1 = mutableListOf(mutableListOf<Int>(), mutableListOf<Int>(),mutableListOf<Int>(),mutableListOf<Int>(),mutableListOf<Int>())
    private var v_argt_ch2 = mutableListOf(mutableListOf<Int>(), mutableListOf<Int>(),mutableListOf<Int>(),mutableListOf<Int>(),mutableListOf<Int>())

    override fun onDataReceived(ch1: Int, ch2: Int) {

        if (isRecording) {

            val time = Date()
            pw?.print(format.format(time) + "," + ch1 + "," + ch2 + "," + currentSound + "\n")
            val receivedSound = currentSound
            if(receivedSound!="") {
                ground_ch1.removeAt(0)
                ground_ch2.removeAt(0)
                ground_ch1.add(ch1)
                ground_ch2.add(ch2)
                if (receivedSound.toInt() != temp_sound_label) {
                    stim_counts[receivedSound.toInt()-1] = 0
                    temp_ch1s[receivedSound.toInt()-1]= ground_ch1.average().toInt()
                    temp_ch2s[receivedSound.toInt()-1] = ground_ch2.average().toInt()

                    temp_sound_label = receivedSound.toInt()
                }
                for (i in 0..4) {
                    if(i!=left_class-1 && i!=right_class-1){
                        continue
                    }

                    if (stim_counts[i] == p300Point) {
                        Log.d(
                            "eeg_level",
                            i.toString() + "," + ch1.toString() + "," + ch2.toString()
                        )
                        v_argt_ch1[receivedSound.toInt() - 1].add(ch1)
                        v_argt_ch2[receivedSound.toInt() - 1].add(ch2)

                        if ((ch1 - temp_ch1s[i] + ch2 - temp_ch2s[i]) / 2 >= p300Volt) {
                            sound_levels[i] += 0.10f
                        } else {
                            sound_levels[i] -= 0.10f
                        }
                        if (sound_levels[i] <= 0) {
                            sound_levels[i] = 0.05f
                        }
                    }

                    if (stim_counts[i] == n200Point) {
                        Log.d(
                            "eeg_level",
                            i.toString() + "," + ch1.toString() + "," + ch2.toString()
                        )
                        v_argt_ch1[receivedSound.toInt() - 1].add(ch1)
                        v_argt_ch2[receivedSound.toInt() - 1].add(ch2)

                        if ((ch1 - temp_ch1s[i] + ch2 - temp_ch2s[i]) / 2 <= n200Volt) {
                            sound_levels[i] += 0.10f
                        } else {
                            sound_levels[i] -= 0.10f
                        }
                        if (sound_levels[i] <= 0) {
                            sound_levels[i] = 0.05f
                        }
                    }
                }
                stim_counts[0] += 1
                stim_counts[1] += 1
                stim_counts[2] += 1
                stim_counts[3] += 1
                stim_counts[4] += 1
            }
        }
    }
    // ローダーが要求された時
    override fun onCreateLoader(p0: Int, p1: Bundle?): Loader<ResultData> {
        val dir: String? = p1?.getString("dir")
        val file: String? = p1?.getString("file")

        return PredictLoader(this, dir, file, classNum)
    }

    // ローダーがリセットされた時
    override fun onLoaderReset(p0: Loader<ResultData>) {
        // 何もしない
        println("reset")
    }

    // 学習完了コールバック
    override fun onLoadFinished(p0: Loader<ResultData>, p1: ResultData?) {

        if (p1 != null) {

            resultData = p1
            loadingDialog!!.dismiss()

            //受け取ったままのデータがper_d
            //Android側で変換をかけたのが、acc_levels
            //クラス制限による結果調整
            /*
            when (classNum){
                4 -> {
                    acc_levels[2] = 0.0
                }

                3->{
                    acc_levels[0] = (resultData.per_d[0]/(resultData.per_d[0]+resultData.per_d[2]+resultData.per_d[4]))*100.0
                    acc_levels[2] = (resultData.per_d[4]/(resultData.per_d[0]+resultData.per_d[2]+resultData.per_d[4]))*100.0
                    acc_levels[4] = (resultData.per_d[4]/(resultData.per_d[0]+resultData.per_d[2]+resultData.per_d[4]))*100.0
                    acc_levels[1] = 0.0
                    acc_levels[3] = 0.0
                }
                2->{
                    acc_levels[1] = 0.0
                    acc_levels[2] = 0.0
                    acc_levels[3] = 0.0
                    acc_levels[0] = (resultData.per_d[0]/(resultData.per_d[0]+resultData.per_d[4]))*100.0
                    acc_levels[4] = (resultData.per_d[4]/(resultData.per_d[0]+resultData.per_d[4]))*100.0
                }
            }
            */
            for(i in 1..5){
                if(i!=left_class && i!=right_class){
                    acc_levels[i-1] = 0.0
                }
                else{
                    acc_levels[i-1] = (resultData.per_d[i-1]/(resultData.per_d[left_class-1]+resultData.per_d[right_class-1]))*100.0
                }
            }

            resultData.select = 0
            var max_percentarge = 0.0
            for(i in listOf(0,1,2,3,4)){
                if(acc_levels[i]>max_percentarge){
                    max_percentarge = acc_levels[i]
                    resultData.select = i+1
                }
            }

            for(i in 0..4) {
                resultData.per_s[i] = String.format("%.2f", acc_levels[i])+"%"
            }

            select_counts[resultData.select-1]+=1

            if(acc_levels[resultData.select - 1]>100/classNum+100/classNum/5) {
                showResultDialog()
            }
            /*
            else if(select_counts[resultData.select - 1]>=3){
                showResultDialog()
            }
            */
            else if(safeCount==2){
                showStopDialog()
            }
            else{
                safeCount+=1
                showErrorDialog()
            }
        }
        supportLoaderManager.destroyLoader(1)
    }

    private fun showPlayerDialog() {

        val inflater = this.layoutInflater.inflate(R.layout.dialog_player, null, false)
        val stopBtn = inflater.findViewById<Button>(R.id.stop)
        val title = inflater.findViewById<TextView>(R.id.playerTitle)
        title.text = "脳波を測定しています"
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
        nowPlayingImg = inflater.findViewById(R.id.image)
        nowPlayingImg!!.visibility = View.INVISIBLE

        playerDialog = AlertDialog.Builder(this).apply {

            setView(inflater)
            stopBtn.setOnClickListener {
                // 途中終了
                isSuspend = true
                setCount = SetSize+2
            }
        }.create()

        playerDialog!!.setCancelable(false)
        playerDialog!!.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        playerDialog!!.show()
    }

    private fun showLoadingDialog() {

        val inflater = this.layoutInflater.inflate(R.layout.dialog_play_datasend, null, false)

        loadingDialog = AlertDialog.Builder(this).apply {
            setView(inflater)
        }.create()

        loadingDialog!!.setCancelable(false)
        loadingDialog!!.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        loadingDialog!!.show()
    }

    private fun showErrorDialog() {

        val inflater = this.layoutInflater.inflate(R.layout.dialog_play_result, null, false)
        val endBtn = inflater.findViewById<Button>(R.id.end)
        val startAgainBtn = inflater.findViewById<Button>(R.id.startAgain)
        val image1 = inflater.findViewById<ImageView>(R.id.rst1)
        val image2 = inflater.findViewById<ImageView>(R.id.rst2)
        val image3 = inflater.findViewById<ImageView>(R.id.rst3)
        val image4 = inflater.findViewById<ImageView>(R.id.rst4)
        val image5 = inflater.findViewById<ImageView>(R.id.rst5)
        when (getSoundSetID(currentDataset)) {
            1 -> {
                image1.setImageResource(R.drawable.snd_s1_1_img_on)
                image2.setImageResource(R.drawable.snd_s1_2_img_on)
                image3.setImageResource(R.drawable.snd_s1_3_img_on)
                image4.setImageResource(R.drawable.snd_s1_4_img_on)
                image5.setImageResource(R.drawable.snd_s1_5_img_on)
            }
            2 -> {
                image1.setImageResource(R.drawable.snd_s2_1_img_on)
                image2.setImageResource(R.drawable.snd_s2_2_img_on)
                image3.setImageResource(R.drawable.snd_s2_3_img_on)
                image4.setImageResource(R.drawable.snd_s2_4_img_on)
                image5.setImageResource(R.drawable.snd_s2_5_img_on)
            }
            3 -> {
                image1.setImageResource(R.drawable.snd_s3_1_img_on)
                image2.setImageResource(R.drawable.snd_s3_2_img_on)
                image3.setImageResource(R.drawable.snd_s3_3_img_on)
                image4.setImageResource(R.drawable.snd_s3_4_img_on)
                image5.setImageResource(R.drawable.snd_s3_5_img_on)
            }
        }
        val speakLbl = inflater.findViewById<TextView>(R.id.speak)
        speakLbl.textSize = 30.0f
        speakLbl.text = """精度が低いため、もう一度実行して下さい。"""

        val select1 = inflater.findViewById<ImageView>(R.id.select1)
        val select2 = inflater.findViewById<ImageView>(R.id.select2)
        val select3 = inflater.findViewById<ImageView>(R.id.select3)
        val select4 = inflater.findViewById<ImageView>(R.id.select4)
        val select5 = inflater.findViewById<ImageView>(R.id.select5)

        val per1 = inflater.findViewById<TextView>(R.id.per1)
        per1.text = resultData.per_s[0]
        val per2 = inflater.findViewById<TextView>(R.id.per2)
        per2.text = resultData.per_s[1]
        val per3 = inflater.findViewById<TextView>(R.id.per3)
        per3.text = resultData.per_s[2]
        val per4 = inflater.findViewById<TextView>(R.id.per4)
        per4.text = resultData.per_s[3]
        val per5 = inflater.findViewById<TextView>(R.id.per5)
        per5.text = resultData.per_s[4]

        resultDialog = AlertDialog.Builder(this).apply {

            setView(inflater)
            endBtn.setOnClickListener {
                resultDialog!!.dismiss()
            }
            startAgainBtn.visibility = View.VISIBLE
            startAgainBtn.setOnClickListener {
                resultDialog!!.dismiss()
                showPlayerDialog()
                currentSound = ""
                isRecording = true

                stim_counts = arrayOf(-250,-250,-250,-250,-250)
                sound_levels = arrayOf(0.05f, 0.05f, 0.05f, 0.05f, 0.05f)

                //追記する場合
//                val fw = FileWriter(Environment.getExternalStorageDirectory().getPath() + "/Noupathy/" + currentDataset + "/" + currentFileName,true)
//                pw = PrintWriter(BufferedWriter(fw))
                //新規ファイル作成の場合
                val time = Date()
                val file_format = SimpleDateFormat("yyyyMMdd_HHmmss_", Locale.getDefault())
                val fileTime = file_format.format(time)
                currentFileName = fileTime + currentTarget + "_play.csv"
                val fw = FileWriter(Environment.getExternalStorageDirectory().getPath() + "/Noupathy/" + currentDataset + "/" + currentFileName)
                pw = PrintWriter(BufferedWriter(fw))
                pw?.print("Timestamp,Ch1,Ch2,Sound\n")
                handler.post(runnable)
            }
        }.create()

        resultDialog!!.setCancelable(false)
        resultDialog!!.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        resultDialog!!.show()
    }

    private fun showResultDialog() {

        val inflater = this.layoutInflater.inflate(R.layout.dialog_play_result, null, false)
        val endBtn = inflater.findViewById<Button>(R.id.end)
        val startAgainBtn = inflater.findViewById<Button>(R.id.startAgain)
        val image1 = inflater.findViewById<ImageView>(R.id.rst1)
        val image2 = inflater.findViewById<ImageView>(R.id.rst2)
        val image3 = inflater.findViewById<ImageView>(R.id.rst3)
        val image4 = inflater.findViewById<ImageView>(R.id.rst4)
        val image5 = inflater.findViewById<ImageView>(R.id.rst5)
        when (getSoundSetID(currentDataset)) {
            1 -> {
                image1.setImageResource(R.drawable.snd_s1_1_img_on)
                image2.setImageResource(R.drawable.snd_s1_2_img_on)
                image3.setImageResource(R.drawable.snd_s1_3_img_on)
                image4.setImageResource(R.drawable.snd_s1_4_img_on)
                image5.setImageResource(R.drawable.snd_s1_5_img_on)
            }
            2 -> {
                image1.setImageResource(R.drawable.snd_s2_1_img_on)
                image2.setImageResource(R.drawable.snd_s2_2_img_on)
                image3.setImageResource(R.drawable.snd_s2_3_img_on)
                image4.setImageResource(R.drawable.snd_s2_4_img_on)
                image5.setImageResource(R.drawable.snd_s2_5_img_on)
            }
            3 -> {
                image1.setImageResource(R.drawable.snd_s3_1_img_on)
                image2.setImageResource(R.drawable.snd_s3_2_img_on)
                image3.setImageResource(R.drawable.snd_s3_3_img_on)
                image4.setImageResource(R.drawable.snd_s3_4_img_on)
                image5.setImageResource(R.drawable.snd_s3_5_img_on)
            }
        }
        val speakLbl = inflater.findViewById<TextView>(R.id.speak)
        speakLbl.text = "「" + words[resultData.select - 1] + "」"

        val select1 = inflater.findViewById<ImageView>(R.id.select1)
        val select2 = inflater.findViewById<ImageView>(R.id.select2)
        val select3 = inflater.findViewById<ImageView>(R.id.select3)
        val select4 = inflater.findViewById<ImageView>(R.id.select4)
        val select5 = inflater.findViewById<ImageView>(R.id.select5)

        val per1 = inflater.findViewById<TextView>(R.id.per1)
        per1.text = resultData.per_s[0]
        val per2 = inflater.findViewById<TextView>(R.id.per2)
        per2.text = resultData.per_s[1]
        val per3 = inflater.findViewById<TextView>(R.id.per3)
        per3.text = resultData.per_s[2]
        val per4 = inflater.findViewById<TextView>(R.id.per4)
        per4.text = resultData.per_s[3]
        val per5 = inflater.findViewById<TextView>(R.id.per5)
        per5.text = resultData.per_s[4]


        when (resultData.select) {
            1 -> {
                select1.isSelected = true
                hiligtedText(per1)
            }
            2 -> {
                select2.isSelected = true
                hiligtedText(per2)
            }
            3 -> {
                select3.isSelected = true
                hiligtedText(per3)
            }
            4 -> {
                select4.isSelected = true
                hiligtedText(per4)
            }
            5 -> {
                select5.isSelected = true
                hiligtedText(per5)
            }
        }

        resultDialog = AlertDialog.Builder(this).apply {

            setView(inflater)
            startAgainBtn.visibility = View.INVISIBLE
            endBtn.setOnClickListener {
                resultDialog!!.dismiss()
            }
        }.create()

        resultDialog!!.setCancelable(false)
        resultDialog!!.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        resultDialog!!.show()
    }

    private fun showStopDialog() {

        val inflater = this.layoutInflater.inflate(R.layout.dialog_play_result, null, false)
        val endBtn = inflater.findViewById<Button>(R.id.end)
        val startAgainBtn = inflater.findViewById<Button>(R.id.startAgain)
        val image1 = inflater.findViewById<ImageView>(R.id.rst1)
        val image2 = inflater.findViewById<ImageView>(R.id.rst2)
        val image3 = inflater.findViewById<ImageView>(R.id.rst3)
        val image4 = inflater.findViewById<ImageView>(R.id.rst4)
        val image5 = inflater.findViewById<ImageView>(R.id.rst5)
        when (getSoundSetID(currentDataset)) {
            1 -> {
                image1.setImageResource(R.drawable.snd_s1_1_img_on)
                image2.setImageResource(R.drawable.snd_s1_2_img_on)
                image3.setImageResource(R.drawable.snd_s1_3_img_on)
                image4.setImageResource(R.drawable.snd_s1_4_img_on)
                image5.setImageResource(R.drawable.snd_s1_5_img_on)
            }
            2 -> {
                image1.setImageResource(R.drawable.snd_s2_1_img_on)
                image2.setImageResource(R.drawable.snd_s2_2_img_on)
                image3.setImageResource(R.drawable.snd_s2_3_img_on)
                image4.setImageResource(R.drawable.snd_s2_4_img_on)
                image5.setImageResource(R.drawable.snd_s2_5_img_on)
            }
            3 -> {
                image1.setImageResource(R.drawable.snd_s3_1_img_on)
                image2.setImageResource(R.drawable.snd_s3_2_img_on)
                image3.setImageResource(R.drawable.snd_s3_3_img_on)
                image4.setImageResource(R.drawable.snd_s3_4_img_on)
                image5.setImageResource(R.drawable.snd_s3_5_img_on)
            }
        }
        val speakLbl = inflater.findViewById<TextView>(R.id.speak)
        speakLbl.textSize = 30.0f
        speakLbl.text = """疲れているようです。一度休憩して下さい。"""

        val select1 = inflater.findViewById<ImageView>(R.id.select1)
        val select2 = inflater.findViewById<ImageView>(R.id.select2)
        val select3 = inflater.findViewById<ImageView>(R.id.select3)
        val select4 = inflater.findViewById<ImageView>(R.id.select4)
        val select5 = inflater.findViewById<ImageView>(R.id.select5)

        val per1 = inflater.findViewById<TextView>(R.id.per1)
        per1.text = resultData.per_s[0]
        val per2 = inflater.findViewById<TextView>(R.id.per2)
        per2.text = resultData.per_s[1]
        val per3 = inflater.findViewById<TextView>(R.id.per3)
        per3.text = resultData.per_s[2]
        val per4 = inflater.findViewById<TextView>(R.id.per4)
        per4.text = resultData.per_s[3]
        val per5 = inflater.findViewById<TextView>(R.id.per5)
        per5.text = resultData.per_s[4]

        resultDialog = AlertDialog.Builder(this).apply {

            setView(inflater)
            startAgainBtn.visibility = View.INVISIBLE
            endBtn.setOnClickListener {
                resultDialog!!.dismiss()
            }
        }.create()

        resultDialog!!.setCancelable(false)
        resultDialog!!.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        resultDialog!!.show()
    }

    private fun hiligtedText(text: TextView) {
        text.textSize = 22.5F
        text.setTextColor(getColor(R.color.peachy_pink))
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

    private fun showWordEditDialog(num: Int, word: String) {

        val inflater = this.layoutInflater.inflate(R.layout.dialog_wordedit, null, false)
        val okBtn = inflater.findViewById<Button>(R.id.wordSetBtn)
        val desc = inflater.findViewById<TextView>(R.id.wordDesc)
        desc.text = num.toString() + "が選択された時の言葉を設定します"
        val field = inflater.findViewById<EditText>(R.id.wordTxt)
        field.setText(word)

        wordEditDialog = AlertDialog.Builder(this).apply {
            setView(inflater)
            okBtn.setOnClickListener {
                pref.edit().putString("word"+num.toString(), field.text.toString()).apply()
                setWords()
                setClassLimit()
                wordEditDialog!!.dismiss()
            }
        }.create()

        wordEditDialog!!.setCancelable(false)
        wordEditDialog!!.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        wordEditDialog!!.show()
    }

    private fun setWords() {

        pref = getSharedPreferences("prefs", AppCompatActivity.MODE_PRIVATE)

        words.removeAll(words)
        words.add(pref.getString("word1", "飲み物が飲みたいです"))
        words.add(pref.getString("word2", "トイレに行きたいです"))
        words.add(pref.getString("word3", "服を着替えたいです"))
        words.add(pref.getString("word4", "音楽をかけてください"))
        words.add(pref.getString("word5", "吸引してほしいです"))
        word1.setText(words[0])
        word2.setText(words[1])
        word3.setText(words[2])
        word4.setText(words[3])
        word5.setText(words[4])

        edit1.setOnClickListener { showWordEditDialog(1, words[0]) }
        edit2.setOnClickListener { showWordEditDialog(2, words[1]) }
        edit3.setOnClickListener { showWordEditDialog(3, words[2]) }
        edit4.setOnClickListener { showWordEditDialog(4, words[3]) }
        edit5.setOnClickListener { showWordEditDialog(5, words[4]) }
    }

    private fun setClassLimit(){
        /*
        limit2.visibility = View.VISIBLE
        limit3.visibility = View.VISIBLE
        limit4.visibility = View.VISIBLE
        edit2.isEnabled = false
        edit3.isEnabled = false
        edit4.isEnabled = false

//        if(getAccuracy(currentDataset,5).split("%")[0].toDouble()>93){
        if(getAccuracy(currentDataset,5).split("%")[0].toDouble()>100){
            limit2.visibility = View.GONE
            limit3.visibility = View.GONE
            limit4.visibility = View.GONE
            edit2.isEnabled = true
            edit3.isEnabled = true
            edit4.isEnabled = true
            classNum=5
        }
//        else if(getAccuracy(currentDataset,4).split("%")[0].toDouble()>91){
        else if(getAccuracy(currentDataset,4).split("%")[0].toDouble()>100){
            limit2.visibility = View.GONE
            limit4.visibility = View.GONE
            edit2.isEnabled = true
            edit4.isEnabled = true
            classNum=4
        }
//        else if(getAccuracy(currentDataset,3).split("%")[0].toDouble()>88){
        else if(getAccuracy(currentDataset,3).split("%")[0].toDouble()>100){
            limit3.visibility = View.GONE
            edit3.isEnabled = true
            classNum=3
        }
        else{
            classNum=2
        }
        */
        val class_acc = getClassAccuracy(currentDataset)
        if(class_acc[0]>=class_acc[1]){
            //同列の場合は1
            left_class = 1
        }
        else{
            left_class = 2
        }
        if(class_acc[3]<=class_acc[4]){
            //同列の場合は5
            right_class = 5
        }
        else{
            right_class = 4
        }
        //左のクラスの方が精度が低く
        if(class_acc[left_class-1]<class_acc[right_class-1]){
            //左のクラスの方が真ん中よりも低いときは真ん中を左として採用
            if(class_acc[left_class-1]<class_acc[2]){
                left_class = 3
            }
        }//右のクラスの方が精度が低く
        else{
            //右のクラスの方が真ん中よりも低い時は真ん中を右として採用
            if(class_acc[right_class-1]<class_acc[2]){
                right_class = 3
            }
        }

        play1.visibility = View.VISIBLE
        word1.visibility = View.VISIBLE
        edit1.visibility = View.VISIBLE
        status1.visibility = View.VISIBLE
        view1.visibility = View.VISIBLE
        play2.visibility = View.VISIBLE
        word2.visibility = View.VISIBLE
        edit2.visibility = View.VISIBLE
        status2.visibility = View.VISIBLE
        view2.visibility = View.VISIBLE
        play3.visibility = View.VISIBLE
        word3.visibility = View.VISIBLE
        edit3.visibility = View.VISIBLE
        status3.visibility = View.VISIBLE
        view3.visibility = View.VISIBLE
        play4.visibility = View.VISIBLE
        word4.visibility = View.VISIBLE
        edit4.visibility = View.VISIBLE
        status4.visibility = View.VISIBLE
        view4.visibility = View.VISIBLE
        play5.visibility = View.VISIBLE
        word5.visibility = View.VISIBLE
        edit5.visibility = View.VISIBLE
        status5.visibility = View.VISIBLE
        view5.visibility = View.VISIBLE

        for(i in 1..5){
            if(i!=left_class && i!=right_class){
                when(i){
                    1->{
                        play1.visibility = View.INVISIBLE
                        word1.visibility = View.INVISIBLE
                        edit1.visibility = View.INVISIBLE
                        status1.visibility = View.INVISIBLE
                        view1.visibility = View.INVISIBLE
                    }
                    2->{
                        play2.visibility = View.INVISIBLE
                        word2.visibility = View.INVISIBLE
                        edit2.visibility = View.INVISIBLE
                        status2.visibility = View.INVISIBLE
                        view2.visibility = View.INVISIBLE
                    }
                    3->{
                        play3.visibility = View.INVISIBLE
                        word3.visibility = View.INVISIBLE
                        edit3.visibility = View.INVISIBLE
                        status3.visibility = View.INVISIBLE
                        view3.visibility = View.INVISIBLE
                    }
                    4->{
                        play4.visibility = View.INVISIBLE
                        word4.visibility = View.INVISIBLE
                        edit4.visibility = View.INVISIBLE
                        status4.visibility = View.INVISIBLE
                        view4.visibility = View.INVISIBLE
                    }
                    5->{
                        play5.visibility = View.INVISIBLE
                        word5.visibility = View.INVISIBLE
                        edit5.visibility = View.INVISIBLE
                        status5.visibility = View.INVISIBLE
                        view5.visibility = View.INVISIBLE
                    }
                }
            }
        }

        //反応速度
        p300Point = getP300Time(currentDataset).toInt()
        p300Volt = getP300Voltage(currentDataset).toInt()
        n200Point = getN200Time(currentDataset).toInt()
        n200Volt = getN200Voltage(currentDataset).toInt()
    }

    private fun loadSounds(soundSetID: Int) {

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        sp = SoundPool.Builder()
            .setAudioAttributes(audioAttributes)
            .setMaxStreams(5)
            .build()

        when (soundSetID) {
            1-> {
                sound1 = sp.load(this, R.raw.s0_0, 1)
                sound2 = sp.load(this, R.raw.s0_1, 1)
                sound3 = sp.load(this, R.raw.s0_2, 1)
                sound4 = sp.load(this, R.raw.s0_3, 1)
                sound5 = sp.load(this, R.raw.s0_4, 1)
            }
            2-> {
                sound1 = sp.load(this, R.raw.s1_0, 1)
                sound2 = sp.load(this, R.raw.s1_1, 1)
                sound3 = sp.load(this, R.raw.s1_2, 1)
                sound4 = sp.load(this, R.raw.s1_3, 1)
                sound5 = sp.load(this, R.raw.s1_4, 1)
            }
            3-> {
                sound1 = sp.load(this, R.raw.s2_0, 1)
                sound2 = sp.load(this, R.raw.s2_1, 1)
                sound3 = sp.load(this, R.raw.s2_2, 1)
                sound4 = sp.load(this, R.raw.s2_3, 1)
                sound5 = sp.load(this, R.raw.s2_4, 1)
            }
        }
        sound5000 = sp.load(this, R.raw.s5000, 1)
        sound_noise = sp.load(this, R.raw.pink_noise, 1)


        // ロードが終わったか確認
        sp.setOnLoadCompleteListener { sp, sampleId, status ->
            //            Log.d("debug", "sampleId=$sampleId")
//            Log.d("debug", "status=$status")
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
            play1.setOnClickListener { sp.play(sound1, 0.05f, 0.05f, 0, 0, 1.0f) }
            play2.setOnClickListener { sp.play(sound2, 0.05f, 0.05f, 0, 0, 1.0f) }
            play3.setOnClickListener { sp.play(sound3, 0.05f, 0.05f, 0, 0, 1.0f) }
            play4.setOnClickListener { sp.play(sound4, 0.05f, 0.05f, 0, 0, 1.0f) }
            play5.setOnClickListener { sp.play(sound5, 0.05f, 0.05f, 0, 0, 1.0f) }
        }
        else {
            play1.setOnClickListener { sp.play(sound1, 1.0f, 1.0f, 0, 0, 1.0f) }
            play2.setOnClickListener { sp.play(sound2, 1.0f, 1.0f, 0, 0, 1.0f) }
            play3.setOnClickListener { sp.play(sound3, 1.0f, 1.0f, 0, 0, 1.0f) }
            play4.setOnClickListener { sp.play(sound4, 1.0f, 1.0f, 0, 0, 1.0f) }
            play5.setOnClickListener { sp.play(sound5, 1.0f, 1.0f, 0, 0, 1.0f) }
        }
    }
}
