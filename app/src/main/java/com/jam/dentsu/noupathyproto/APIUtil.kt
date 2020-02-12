package com.jam.dentsu.noupathyproto

import android.os.Handler
import android.content.Context
import android.os.Environment
import android.support.v4.content.AsyncTaskLoader
import android.util.Log
import org.json.JSONObject
import java.io.*
import java.net.URL
import java.text.DecimalFormat
import javax.net.ssl.HttpsURLConnection
import okhttp3.*
import org.json.JSONArray
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPOutputStream



private val server_url: String = "https://noupathy-server.herokuapp.com/"

fun httpGet(url: String): InputStream? {

    val con = URL(url).openConnection() as HttpsURLConnection

    // 接続の設定
    con.apply {
        requestMethod = "GET"
        connectTimeout = 15000
        readTimeout = 15000
        instanceFollowRedirects = true
    }

    con.connect()

    if (con.responseCode in 200..299) {
        // 成功したらレスポンスの入力ストリームをBufferedInputStreamとして返す
        return BufferedInputStream(con.inputStream)
    }

    // 失敗
    return null
}

//学習精度取得
fun getAccuracy(dir: String, matrix_size: Int): String {

    val file = File("${Environment.getExternalStorageDirectory().path}/${APP_ROOT}/${dir}", "model.json")
    val reader = BufferedReader(FileReader(file))
    val data = reader.readLines().joinToString()

    val json = JSONObject(data)
    val acc = json.getString("acc${matrix_size}").toDouble()
    val per = DecimalFormat("##0.00%")

    return per.format(acc)
}

//P300出現ポイント取得
fun getP300Time(dir: String): Double{
    val file = File("${Environment.getExternalStorageDirectory().path}/${APP_ROOT}/${dir}", "model.json")
    val reader = BufferedReader(FileReader(file))
    val data = reader.readLines().joinToString()

    val json = JSONObject(data)
    val t_1 = json.getString("t_argmax_Ch1").toDouble()
    val t_2 = json.getString("t_argmax_Ch2").toDouble()
    var t_mean = (t_1+t_2)/2

    return t_mean
}

//N200出現ポイント取得
fun getN200Time(dir: String): Double{
    val file = File("${Environment.getExternalStorageDirectory().path}/${APP_ROOT}/${dir}", "model.json")
    val reader = BufferedReader(FileReader(file))
    val data = reader.readLines().joinToString()

    val json = JSONObject(data)
    val t_1 = json.getString("t_argmin_Ch1").toDouble()
    val t_2 = json.getString("t_argmin_Ch2").toDouble()
    var t_mean = (t_1+t_2)/2

    return t_mean
}

//P300強度取得
fun getP300Voltage(dir: String): Double{
    val file = File("${Environment.getExternalStorageDirectory().path}/${APP_ROOT}/${dir}", "model.json")
    val reader = BufferedReader(FileReader(file))
    val data = reader.readLines().joinToString()

    val json = JSONObject(data)
    val v_1 = json.getString("center_vmax_Ch1").toDouble()
    val v_2 = json.getString("center_vmax_Ch2").toDouble()
    var v_mean = (v_1+v_2)/2

    return v_mean
}

//N200強度取得
fun getN200Voltage(dir: String): Double{
    val file = File("${Environment.getExternalStorageDirectory().path}/${APP_ROOT}/${dir}", "model.json")
    val reader = BufferedReader(FileReader(file))
    val data = reader.readLines().joinToString()

    val json = JSONObject(data)
    val v_1 = json.getString("center_vmin_Ch1").toDouble()
    val v_2 = json.getString("center_vmin_Ch2").toDouble()
    var v_mean = (v_1+v_2)/2

    return v_mean
}

fun saveLearningData(dir: String, data: String) {

    val file = File("${Environment.getExternalStorageDirectory().path}/${APP_ROOT}/${dir}", "model.json")
    val writer = BufferedWriter(FileWriter(file))
    writer.use {
        it.write(data)
        it.flush()
    }
}

fun loadClfData(dir: String?): String{
    val file = File("${Environment.getExternalStorageDirectory().path}/${APP_ROOT}/${dir}", "model.json")
    val reader = BufferedReader(FileReader(file))
    val data = reader.readLines().joinToString()

    val json = JSONObject(data)

    return json.getString("clf")
}

fun loadStepIndexData(dir: String?): String{
    val file = File("${Environment.getExternalStorageDirectory().path}/${APP_ROOT}/${dir}", "model.json")
    val reader = BufferedReader(FileReader(file))
    val data = reader.readLines().joinToString()

    val json = JSONObject(data)

    return json.getString("step_index")
}


fun loadRaw(path: String?): MutableMap<String, MutableList<Int>>{
    var ch1: MutableList<Int> = mutableListOf()
    var ch2: MutableList<Int> = mutableListOf()
    var soundLabel: MutableList<Int> = mutableListOf()
    val fr = FileReader(path)
    val br = BufferedReader(fr)
    var str: String? = br.readLine()
    str = br.readLine()
    while(str != null){
        var row = str.split(",")
        ch1.add(row[1].toInt())
        ch2.add(row[2].toInt())
        if(row[3]=="") {
            soundLabel.add(0)
        }
        else{
            soundLabel.add(row[3].toInt())
        }
        str = br.readLine()
    }

    var dataMap = mutableMapOf("ch1" to ch1, "ch2" to ch2, "select_music" to soundLabel)

    return dataMap
}

fun PostDataToServer(json: JSONObject){
    //HTTPリクエストの開始
    val handler = Handler()
    val jsonString = json.toString()
    val postBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"),jsonString)
    val request = Request.Builder().url("${server_url}/storing").post(postBody).build()
    val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    var response_flag = false
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {}
        override fun onResponse(call: Call, response: Response) {
            val responseText: String? = response.body()?.string()
            Log.d("toridge.okhttp3", responseText)
            val json = JSONObject(responseText)
            val status = json.getString("status")
            Log.d("toridge.okhttp3", status)
            response_flag = true
        }
    })

    while(response_flag==false) {
        Thread.sleep(300)
    }
}

// Learning APIを叩いてStringで結果を取得
class LearningLoader(context: Context, dir: String?, user: String, sound: Int): AsyncTaskLoader<String>(context) {

    private var cache: String? = null
    private var current_dir: String? = dir
    private var sub_id: String = user
    private var sound_set_id: Int = sound

    override fun loadInBackground(): String? {
        val path = "${Environment.getExternalStorageDirectory().path}/${APP_ROOT}/${current_dir}/"
        val dir = File(path)
        val files = dir.list()
        var keys = mutableListOf<String>()
        Log.i("toridge.okhttp3", "これから読み込み")

        var learning_file_total = 0
        var sent_file_count = 0
        learning_progress_percent = 0.0
        //学習データの数をカウント
        for(file in files) {
            if (file.contains("_play.csv") || file.contains("DS_Store") || file == "soundset.json") {
                continue
            }
            learning_file_total+=1
        }
        learning_file_total *=3
        learning_file_total+=1

        //学習データの送信処理
        var repCount: Array<Int> = arrayOf(1,1,1,1,1)
        for(file in files) {
            if(file.contains("_play.csv") || file.contains("DS_Store") || file == "soundset.json"){
                continue
            }
            Log.d("http3", file)
            val raw_data = loadRaw(path + file)
            val selectMusic: List<Int>? = raw_data["select_music"]

            val target_tag = file.split('_')[2].split('.')[0]
            val iteration_tag = repCount[target_tag.toInt() - 1].toString();

            //脳波データの送信
            for(ch_num in listOf("1","2")){
                val json = JSONObject()
                json.put("key","Ch${ch_num}_${iteration_tag}_${target_tag}")
                val raw: List<Int>? = raw_data["ch${ch_num}"]
                json.put("data", JSONArray(raw))
                json.put("soundset",sound_set_id)
                json.put("id",sub_id)
                Log.d("http3", "ch${ch_num}_${iteration_tag}_${target_tag}")
                PostDataToServer(json)
                keys.add("Ch${ch_num}_${iteration_tag}_${target_tag}")
                sent_file_count+=1
                learning_progress_percent = sent_file_count.toDouble()/learning_file_total.toDouble()*100
                Log.d("http3", "${learning_progress_percent},${sent_file_count},${learning_file_total}")
            }

            //被験者id, サウンドラベルの送信
            val json = JSONObject()
            json.put("key", "SelectMusic_${iteration_tag}_${target_tag}")
            json.put("data", JSONArray(selectMusic))
            json.put("soundset", sound_set_id)
            json.put("id", sub_id)
            Log.d("http3", "SelectMusic_${iteration_tag}_${target_tag}")
            PostDataToServer(json)

            repCount[target_tag.toInt() - 1] = repCount[target_tag.toInt() - 1] + 1
            keys.add("SelectMusic_${iteration_tag}_${target_tag}")
            sent_file_count+=1
            learning_progress_percent =sent_file_count.toDouble()/learning_file_total.toDouble()*100
        }

        //学習処理の開始
        val json = JSONObject()
        val handler = Handler()

        json.put("keys",JSONArray(keys))
        json.put("id",sub_id)

        val jsonString = json.toString()
        val postBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"),
            jsonString)

        val learning_request = Request.Builder().url("${server_url}/learning").post(postBody).build()

        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        client.newCall(learning_request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                val responseText: String? = response.body()?.string()
                Log.d("toridge.okhttp3", responseText)
                val json = JSONObject(responseText)
                val status = json.getString("status")
                Log.d("toridge.okhttp3", status)
                if(status.contains("background.")) {
                    Log.d("http3", "now background")
                }
                else{
                    Log.d("http3", "error")
                }
            }
        })


        Log.i("toridge.okhttp3", "これから学習リクエスト")

        val request = Request.Builder().url("${server_url}/get-learning-result").post(postBody).build()

        var response_flag = false
        while(response_flag==false) {
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }
                override fun onResponse(call: Call, response: Response) {
                    val responseText: String? = response.body()?.string()
                    Log.d("toridge.okhttp3", responseText)
                    val json = JSONObject(responseText)
                    val status = json.getString("status")
                    Log.d("toridge.okhttp3", status)
                    if(status.contains("perfectly.")) {
                        deliverResult(responseText)
                        response_flag=true
                    }
                }
            })
            Thread.sleep(5000)
        }

        sent_file_count+=1
        learning_progress_percent = sent_file_count.toDouble()/learning_file_total.toDouble()*100

        return null
    }

    // コールバッククラスに返す前に通る処理（キャッシュしてからコールバックに結果を返す）
    override fun deliverResult(data: String?) {

        // 破棄されていたら結果を返さない
        if (isReset || data == null) return

        // 結果をキャッシュする
        cache = data
        super.deliverResult(data)
    }

    // バックグラウンド処理が開始される前に呼ばれる
    override fun onStartLoading() {

        // キャッシュがあるなら、キャッシュを返す
        if (cache != null) {
            deliverResult(cache)
        }

        // コンテンツが変化している場合やキャッシュがない場合には通信を行う
        if (takeContentChanged() || cache == null) {
            forceLoad()
        }
    }

    // ローダーが停止する前に呼ばれる
    override fun onStopLoading() {
        cancelLoad()
    }

    // ローダーが破棄される前に呼ばれる
    override fun onReset() {
        super.onReset()
        onStopLoading()
        cache = null
    }
}


data class ResultData(var select: Int=0, var per_d: MutableList<Double> = mutableListOf(), var per_s: MutableList<String> = mutableListOf())

// Predict APIを叩いてStringで結果を取得
class PredictLoader(context: Context, dir: String?, file: String?, classNum: Int?): AsyncTaskLoader<ResultData>(context) {

    private var cache: ResultData? = null
    private var current_dir: String? = dir
    private var current_file: String? = file
    private var class_num: Int? = classNum
    private val server_url: String = "https://noupathy-server.herokuapp.com/"

    override fun loadInBackground(): ResultData? {

        val debug_on = false

        if(debug_on){
            deliverResult(getResult("{'y_pred': '0.003966626131895807,0.16943232092410512,0.30903131222890684,0.17749221802863138,0.3400775226864608'}"))
            return null
        }

        // 学習データ読み込み
        val path = "${Environment.getExternalStorageDirectory().path}/${APP_ROOT}/${current_dir}/"
        val dir = File(path)
        Log.i("toridge.okhttp3", "これから読み込み")

        val raw_data = loadRaw(path + current_file)
        val ch1: List<Int>? = raw_data["ch1"]
        val ch2: List<Int>? = raw_data["ch2"]
        val selectMusic: List<Int>? = raw_data["select_music"]

        //HTTPリクエスト
        val handler = Handler()
        val json = JSONObject()
        json.put("Ch1",JSONArray(ch1))
        json.put("Ch2",JSONArray(ch2))
        json.put("SelectMusic", JSONArray(selectMusic))
        json.put("json_model", loadClfData(current_dir).toString())
        json.put("step_index", loadStepIndexData(current_dir).toString())
        json.put("class_num", class_num.toString())

        Log.i("toridge.okhttp3", "これからリクエスト")

        val postBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json.toString())
        val request = Request.Builder().url("${server_url}/predicting").post(postBody).build()

        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()


        var response_flag = false
        while(response_flag==false) {
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace();
                }
                override fun onResponse(call: Call, response: Response) {
                    val responseText: String? = response.body()?.string()
                    Log.d("toridge.okhttp3", responseText)
                    if(responseText?.contains("<!DOCTYPE html>")==false) {
                        val json = JSONObject(responseText)
                        val status = json.getString("status")
                        Log.d("toridge.okhttp3", status)
                        if(status.contains("perfectly.")) {
                            deliverResult(getResult(responseText))
                            response_flag=true
                        }
                    }
                }
            })
            Thread.sleep(5000)
        }

        return null
    }

    // コールバッククラスに返す前に通る処理（キャッシュしてからコールバックに結果を返す）
    override fun deliverResult(data: ResultData?) {

        // 破棄されていたら結果を返さない
        if (isReset || data == null) return

        // 結果をキャッシュする
        cache = data
        super.deliverResult(data)
    }

    // バックグラウンド処理が開始される前に呼ばれる
    override fun onStartLoading() {

        // キャッシュがあるなら、キャッシュを返す
        if (cache != null) {
            deliverResult(cache)
        }

        // コンテンツが変化している場合やキャッシュがない場合には通信を行う
        if (takeContentChanged() || cache == null) {
            forceLoad()
        }
    }

    // ローダーが停止する前に呼ばれる
    override fun onStopLoading() {
        cancelLoad()
    }

    // ローダーが破棄される前に呼ばれる
    override fun onReset() {
        super.onReset()
        onStopLoading()
        cache = null
    }

    private fun getResult(res: String?): ResultData {

        var result = ResultData()

        val json = JSONObject(res)
        val pred = json.getString("y_pred")
        val pred_arr = pred.split(",")

        for (p in pred_arr) {
            result.per_d.add((String.format("%.4f", p.toDouble()).toDouble()))

            val per = DecimalFormat("##0.00%")
            result.per_s.add(per.format(p.toDouble()))
        }

        result.select = result.per_d.indexOf(result.per_d.max())+1
        println("result: ${result}")

        return result
    }
}