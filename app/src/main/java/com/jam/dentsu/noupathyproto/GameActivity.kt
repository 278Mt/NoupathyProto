
package com.jam.dentsu.noupathyproto
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.animation.ObjectAnimator;
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Environment
import android.os.Handler
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.*
import kotlinx.android.synthetic.main.activity_game.*
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.schedule
import kotlin.random.Random
import kotlin.random.nextInt

class GameActivity : AppCompatActivity(), neuroNicleService.Companion.NNListener, LoaderManager.LoaderCallbacks<ResultData> {

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
    private var count_sound = 0
    private var succeed_sound = 0

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
    private var isGameStarted = false
    private var isSoundArrayPlaying = false
    private val format = SimpleDateFormat("yyyyMMdd-HH:mm:ss.SSS", Locale.getDefault())

    private var enemyListLeft = mutableListOf<String>()
    private var enemyListRight = mutableListOf<String>()
    private var leftTrialTime = 10
    private var clear_point = 100
    private var pre_trial_count = 7
    private var current_score = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        neuroNicleService.setListener(this)

        pref = getSharedPreferences("prefs", AppCompatActivity.MODE_PRIVATE)
        currentDataset = pref.getString("dataset_play", "DATASET_1")
        right_result.visibility = View.GONE
        left_result.visibility = View.GONE

        current_score = 0.0

        // ゲーム構成準備
        makeEnemyList()

        // ポイント初期化
        setEnemy()

        clear_point_text.text = clear_point.toString()

        // 再生準備
        loadSounds(getSoundSetID(currentDataset))
        playList = makePlayList()

        loadImages(getSoundSetID(currentDataset))


        val runnable = object : Runnable {
            override fun run() {

                if (setCount == 0) {
                    println("start")
                    isSoundArrayPlaying=false
                    sp.play(sound5000, 1.0f, 1.0f, 0,0,1.0f)
                    setCount++
                    nowPlaying("0", nowPlayingImg!!, playerProgress!!)
                    handler.postDelayed(this, 5000)
                }
                else if (setCount <= SetSize) {
                    println("set: " + setCount)
                    isSoundArrayPlaying=true
                    currentSound = playList[0].toString()
                    println("sound: " + currentSound)
                    when(currentSound) {
                        "1" -> sp.play(sound1, 1.0f, 1.0f, 0, 0, 1.0f)
                        "2" -> sp.play(sound2, 1.0f, 1.0f, 0, 0, 1.0f)
                        "3" -> sp.play(sound3, 1.0f, 1.0f, 0, 0, 1.0f)
                        "4" -> sp.play(sound4, 1.0f, 1.0f, 0, 0, 1.0f)
                        "5" -> sp.play(sound5, 1.0f, 1.0f, 0, 0, 1.0f)
                    }
                    nowPlaying(currentSound, nowPlayingImg!!, playerProgress!!)
                    playList.removeAt(0)
                    playCount++

                    if (playCount % 5 == 0) {
                        setCount++
                    }

                    handler.postDelayed(this, 250)
                }
                else if (setCount == SetSize+1) {
                    // 最後の空白
                    isSoundArrayPlaying=false
                    currentSound = ""
                    setCount++
                    sp.play(sound5000, 1.0f, 1.0f, 0,0,1.0f)
                    nowPlaying("0", nowPlayingImg!!, playerProgress!!)
                    handler.postDelayed(this, 1000)
                }
                else {
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
                    } else {
                        showLoadingDialog()
                        val args = Bundle()
                        args.putString("dir", currentDataset)
                        args.putString("file", currentFileName)
                        supportLoaderManager.initLoader(1, args, this@GameActivity)
                    }
                }
            }
        }

        Timer().schedule(500, 1000) {
            runOnUiThread {
                // TOBA
                // temporary refurbishing
                // if (neuroNicleService.instance.isConnected) {
                if (true) {

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

                //TOBA
                //temporary refurbishing
                //if (neuroNicleService.instance.isFitting) {
                if (true) {
                    fitting.setImageResource(R.drawable.com_ok_icon)
                } else {
                    fitting.setImageResource(R.drawable.com_ng_icon)

                    if(isSoundArrayPlaying){ //最初の最後の空白時間のノイズは許容する
                        isSuspend = true
                        setCount = SetSize+2
                    }
                }

                //TOBA
                //temporary refurbishing
                //if (neuroNicleService.instance.calibTime > 0) {
                if (false) {
                    calibration.text = "${neuroNicleService.instance.calibTime}"
                    start.isEnabled = false
                } else {
                    calibration.text = "OK"

                    if (!isRecording) {
                        start.isEnabled = true
                    } else {
                        start.isEnabled = false
                    }

                    start.isEnabled = true
                }

                //指示動作
                if(isGameStarted){
                    if(pre_trial_count > 5){
                        setEnemy()
                        setTextViewBasic(left_point)
                        setTextViewBasic(right_point)
                        count_text.text = "左右から選ぶボックスを決めて下さい"
                    }
                    else if(5 >= pre_trial_count && pre_trial_count > 0) {
                        count_text.text = "次の脳波計測開始まで、あと" + pre_trial_count.toString() + "秒"
                        sp.play(count_sound, 1.0f, 1.0f, 0, 0, 1.0f)
                    }

                    if(pre_trial_count==0){
                        showPlayerDialog()
                        currentSound = ""
                        isRecording = true
                        handler.post(runnable)
                    }
                    --pre_trial_count
                }
            }
        }

        start.setOnClickListener {
            isGameStarted = true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

//        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
//
//            val selectedDataset = data.getStringExtra("dataset")
//            currentDataset = selectedDataset
//            loadSounds(getSoundSetID(currentDataset))
//            pref.edit().putString("dataset_play", currentDataset).apply()
//        }
    }

    override fun onResume() {
        super.onResume()

        neuroNicleService.setListener(this)
    }

    override fun onPause() {
        super.onPause()

        neuroNicleService.setListener(null)
    }

    override fun onDataReceived(ch1: Int, ch2: Int) {

        if (isRecording) {

            val time = Date()
            pw?.print(format.format(time) + "," + ch1 + "," + ch2 + "," + currentSound + "\n")
        }
    }

    // ローダーが要求された時
    override fun onCreateLoader(p0: Int, p1: Bundle?): Loader<ResultData> {
        val dir: String? = p1?.getString("dir")
        val file: String? = p1?.getString("file")

        return PredictLoader(this, dir, file, 2)
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
            showResult()
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

    private fun showResult() {
        left_result.visibility = View.VISIBLE
        right_result.visibility = View.VISIBLE
        left_result.text = resultData.per_s[0]
        right_result.text = resultData.per_s[4]

        var left_prob = resultData.per_s[0].split("%")[0].toDouble()
        var right_prob = resultData.per_s[4].split("%")[0].toDouble()
        if(left_prob>=right_prob){
            right_point.visibility = View.INVISIBLE
            if(enemyListLeft[10-leftTrialTime].contains(".")) {
                current_score *= (enemyListLeft[10 - leftTrialTime]).toDouble()
            }
            else{
                current_score +=  enemyListLeft[10 - leftTrialTime].toInt()
            }
        }
        else{
            left_point.visibility = View.INVISIBLE
            if(enemyListRight[10-leftTrialTime].contains(".")) {
                current_score *= enemyListRight[10 - leftTrialTime].toDouble()
            }
            else{
                current_score += enemyListRight[10 - leftTrialTime].toInt()
            }
        }

        if(leftTrialTime==0){
            if(current_score>clear_point){
                sp.play(succeed_sound, 1.0f, 1.0f, 0, 0, 1.0f)
            }
        }
        leftTrialTime -= 1
        pre_trial_count = 7
    }

    private fun makeEnemyList(){

        val game_level = intent.getIntExtra("GAME_LEVEL",1)

        val mr = MakeRandom(level=game_level) // MUST input 1..6 on number of level

        var mr_list = mr.setList()

        for(i in 0..9){
            enemyListLeft.add(mr_list[i][0].toString())
            enemyListRight.add(mr_list[i][1].toString())
        }
    }

    private fun setEnemy(){
        if(enemyListLeft[10-leftTrialTime].contains(".")){
            left_point.text = "ポイント"+enemyListLeft[10 - leftTrialTime] + "倍"
        }
        else {
            left_point.text = enemyListLeft[10 - leftTrialTime] + "ポイント"
        }
        if(enemyListRight[10-leftTrialTime].contains(".")){
            right_point.text = "ポイント"+enemyListRight[10 - leftTrialTime] + "倍"
        }
        else {
            right_point.text = enemyListRight[10 - leftTrialTime] + "ポイント"
        }
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

    private fun loadImages(soundSetID: Int) {

        when (soundSetID) {
            1 -> {
                game_image1.setImageResource(R.drawable.plyr_s1_img)
                game_image5.setImageResource(R.drawable.plyr_s5_img)
            }
            2 -> {
                game_image1.setImageResource(R.drawable.plyr_s2_1_img)
                game_image5.setImageResource(R.drawable.plyr_s2_5_img)
            }
            3 -> {
                game_image1.setImageResource(R.drawable.plyr_s3_1_img)
                game_image5.setImageResource(R.drawable.plyr_s3_5_img)
            }
        }
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


        count_sound = sp.load(this, R.raw.game_count_down, 1)
        succeed_sound = sp.load(this, R.raw.game_succeed, 1)

        when (soundSetID) {
            1-> {
                sound1 = sp.load(this, R.raw.s1, 1)
                sound2 = sp.load(this, R.raw.s2, 1)
                sound3 = sp.load(this, R.raw.s3, 1)
                sound4 = sp.load(this, R.raw.s4, 1)
                sound5 = sp.load(this, R.raw.s5, 1)
            }
            2-> {
                sound1 = sp.load(this, R.raw.s2_1, 1)
                sound2 = sp.load(this, R.raw.s2_2, 1)
                sound3 = sp.load(this, R.raw.s2_3, 1)
                sound4 = sp.load(this, R.raw.s2_4, 1)
                sound5 = sp.load(this, R.raw.s2_5, 1)
            }
            3-> {
                sound1 = sp.load(this, R.raw.s3_1, 1)
                sound2 = sp.load(this, R.raw.s3_2, 1)
                sound3 = sp.load(this, R.raw.s3_3, 1)
                sound4 = sp.load(this, R.raw.s3_4, 1)
                sound5 = sp.load(this, R.raw.s3_5, 1)
            }
        }
        sound5000 = sp.load(this, R.raw.s5000, 1)


        // ロードが終わったか確認
        sp.setOnLoadCompleteListener { sp, sampleId, status ->
            //            Log.d("debug", "sampleId=$sampleId")
//            Log.d("debug", "status=$status")
        }
    }

    //アニメーション関係
    private fun setViewBasic(img: ImageView) {
        val objectAnimator = ObjectAnimator.ofFloat(img, "scaleY", 0.0f)
        objectAnimator.start()
    }

    //アニメーション関係
    private fun setTextViewBasic(txt: TextView) {
        val objectAnimator = ObjectAnimator.ofFloat(txt, "scaleY", 0.0f)
        objectAnimator.start()
    }

    private fun setViewInvisible(img: ImageView) {
        val objectAnimator = ObjectAnimator.ofFloat(img, "scaleY", 0.1f)
        objectAnimator.duration = 900
        objectAnimator.repeatCount = 0
        objectAnimator.start()
    }
}

// class val
// level    : level of a game
// datasets : count of datasets in one sequence of the game
// multiples: limitation of count of multiple
class MakeRandom(val level: Int, val datasets: Int=10, val multiples: Int=4) {

    // pointRange              : element of point box is -pointRange to pointRange
    // multipleMin, multipleMax: element multiple box
    // pointMin, pointMax      : element multiple box
    // clearWays               : limitation of the clear ways
    // clearRange              : range of the limitation
    // clearPts                : condition for terminating a game
    private val pointRange : Int    = 20
    private var multipleMin: Double = 1.0
    private var multipleMax: Double = 2.0
    private var pointMin   : Int    = 0
    private var pointMax   : Int    = pointRange
    private var clearWays  : Int    = 0
    private var clearRange : Int    = 20
    private val clearPts   : Int    = 100

    init {
        when(level) {
            // level MUST be in 1..6
            1, 2 -> {
                this.multipleMin = 1.0
                this.multipleMax = 2.0
                this.pointMin    = 0
                this.pointMax    = pointRange
                this.clearWays   = ((1 shl datasets) * (if (level == 1)  0.9 else 0.7)).toInt()
            }
            3, 4 -> {
                this.multipleMin = 1.0
                this.multipleMax = 2.0
                this.pointMin    = -pointRange
                this.pointMax    = pointRange
                this.clearWays   = ((1 shl datasets) * (if (level == 3)  0.5 else 0.3)).toInt()
            }
            5, 6 -> {
                this.multipleMin = -2.0
                this.multipleMax = 2.0
                this.pointMin    = -pointRange
                this.pointMax    = pointRange
                this.clearWays   = ((1 shl datasets) * (if (level == 5)  0.1 else 0.05)).toInt()
            }
            // level exception regarding level NOT being in 1..6
            else -> {
                throw Exception("Please input 1..6 on number of level")
            }
        }
    }

    // set list
    // output
    // mutable list (size [10][2])
    fun setList():MutableList<MutableList<Number> > {

        val list = MutableList<Number>(size=datasets*2) { 0 }
        // generate point boxes (default 16)
        for (i in 0 until datasets * 2 - multiples) {
            val randomInt = Random.nextInt(range=pointMin..pointMax)
            list[i] = randomInt
        }
        // generate multiple boxes (default 4)
        for (i in 0 until this.multiples) {
            val randomDouble = (Random.nextDouble(from=multipleMin, until=multipleMax+0.1) * 10).toInt().toDouble() / 10
            list[i + datasets * 2 - multiples] = randomDouble
        }

        val newList: MutableList<Number> = list.shuffled().toMutableList()
        // here is for NOT multiples at newList[0][:]
        for (i in 0..1) {
            if (newList[i] is Double) {
                val tmp: Double = newList[0] as Double
                var j = 0
                while (true) {
                    if (newList[j] is Int) {
                        newList[0] = newList[j]
                        newList[j] = tmp
                        break
                    }
                    ++j
                }
            }
        }
        var ways = 0

        while (true) {

            ways = 0

            for (i in 0 until (1 shl datasets)) {

                var point: Double = 0.0

                for (j in 0 until datasets) {
                    val data: Number = newList[j * 2 + ((i shr j) and 1)]
                    point = if (data is Int) point + data.toDouble() else point * data.toDouble()
                }
                if (point >= clearPts) ++ways
            }

            println("current clear ways: ${ways}")
            if (ways < clearWays-clearRange) {
                val arg = argminPoint(newList)
                newList[arg] = newList[arg].toInt() + 1
            }
            else if(ways > clearWays+clearRange) {
                val arg = argmaxPoint(newList)
                newList[arg] = newList[arg].toInt() - 1
            }
            else {
                break
            }
        }

        val res = MutableList(size=datasets) {MutableList<Number>(size=2) { 0 } }
        for (i in 0 until datasets) {
            // here is for NOT symnumerous at newList[0][:]
            res[i][0] = newList[i * 2]
            res[i][1] = newList[i * 2 + 1]
            if (res[i][0] == res[i][1]) {
                if (res[i][0] is Int) {
                    res[i][0] = res[i][0].toInt() + 10
                    if (res[i][0].toInt() > pointRange) {
                        res[i][0] = pointRange
                    }
                }
                else {
                    res[i][0] = res[i][0].toDouble() + 0.5
                    if (res[i][0].toDouble() > multipleMax) {
                        res[i][0] = multipleMax
                    }
                }
            }

            println("set ${i}: ${res[i][0]}, ${res[i][1]}")
        }

        return res
    }

    // argmin of point NOT multiple argument
    // list: mutable list consisted of Int or Double (size [20])
    // output
    // argmin of point NOT multiple
    private fun argminPoint(list: MutableList<Number>): Int {
        var res: Int = 0
        var minPoint: Int = pointRange
        for (i in 0 until datasets*2) {
            if (list[i] is Int && list[i].toInt() < minPoint) {
                res = i
                minPoint = list[i].toInt()
            }
        }

        return res
    }

    // argmax of point NOT multiple argument
    // list: mutable list consisted of Int or Double (size [20])
    // output
    // argmax of point NOT multiple
    private fun argmaxPoint(list: MutableList<Number>): Int {
        var res: Int = 0
        var maxPoint: Int = -pointRange
        for (i in 0 until datasets*2) {
            if (list[i] is Int && list[i].toInt() > maxPoint) {
                res = i
                maxPoint = list[i].toInt()
            }
        }

        return res
    }
}
