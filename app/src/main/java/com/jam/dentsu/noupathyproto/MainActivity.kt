package com.jam.dentsu.noupathyproto

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.widget.*
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import java.io.*


class MainActivity : AppCompatActivity() {

    lateinit var pref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermission()

        setRRawSList()

        // はじめに１つデータセットを作っておく
        if (getDirCount() == 0) {
            addDatasetDir()
        }

        val connMgr = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connMgr.activeNetworkInfo
        if ( networkInfo != null && networkInfo.isConnected ) {
            // 何もしない
        } else {
            val dialog = AlertDialog.Builder(this).apply {
                setTitle("ネットワークに接続されていません")
                setPositiveButton("ネットワーク環境設定を開く", DialogInterface.OnClickListener { _, _ ->
                    val intent = Intent()
                    intent.action = Settings.ACTION_WIFI_SETTINGS
                    startActivity(intent)
                })
            }.create()

            dialog.show()
        }

        recordButton.setOnClickListener {

            val intent = Intent(this, RecordActivity::class.java)
            startActivity(intent)
        }

        playButton.setOnClickListener {

            val intent = Intent(this, PlayActivity::class.java)
            startActivity(intent)
        }

        gameButton.setOnClickListener {

            val intent = Intent(this, GameMenuActivity::class.java)
            startActivity(intent)
        }

        pref = getSharedPreferences("prefs", AppCompatActivity.MODE_PRIVATE)

        val id = pref.getString("user_id", "000003")
        idText.text = "User id: ${id}"

        idSetBtn.setOnClickListener {

            showIdSettingDialog()
        }

        neuroNicleService.onCreateApplication(applicationContext)
        neuroNicleService.StartNN()
    }

    override fun onStart() {
        super.onStart()

        if (learnedDataIsExist()) {
            gameButton.isEnabled = true
            playButton.isEnabled = true
        } else {
            gameButton.isEnabled = false
            playButton.isEnabled = false
        }
    }

    private fun showIdSettingDialog() {

        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("ユーザーID設定")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL

        val idBox = EditText(this)
        idBox.setText(pref.getString("user_id", "000003"))
        layout.addView(idBox)

        val passBox = EditText(this)
        passBox.hint = "管理用パスワード"
        layout.addView(passBox)

        dialog.setView(layout)

        dialog.setPositiveButton("OK", DialogInterface.OnClickListener{_, _ ->

            if (passBox.getText().toString() != "00000000") {

                Toast.makeText(this, "パスワードが違います", Toast.LENGTH_SHORT).show()
            } else {

                val id = idBox.getText().toString()
                if (id.length > 3) {
                    pref.edit().putString("user_id", id).apply()
                    idText.text = "User id: ${id}"
                } else {
                    Toast.makeText(this, "idは4文字以上で設定してください", Toast.LENGTH_SHORT).show()
                }
            }
        })
        dialog.setCancelable(false)
        dialog.setNegativeButton("Cancel", null)
        dialog.show()
    }


    private fun checkPermission() {

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        }
    }

}

fun setRRawSList(update: Boolean=false) {
    /*
    mainを開いた時にNoupathy/rrawslist.jsonがなかったら新規作成、あったら読み込み
     */
    val dir = "${Environment.getExternalStorageDirectory().path}/${APP_ROOT}"
    val part_path = "rrawslist.json"
    val path = "${dir}/${part_path}"
    val root = File(path)
    val file = File(dir, part_path)

    println("update: ${update}")

    if (update || !root.exists()) {
        println("create ${path}")
        val json = JSONObject()
        json.put("RRawSList", listOf(
            listOf(R.raw.s0_0, R.raw.s0_1, R.raw.s0_2, R.raw.s0_3, R.raw.s0_4),
            listOf(R.raw.s1_0, R.raw.s1_1, R.raw.s1_2, R.raw.s1_3, R.raw.s1_4),
            listOf(R.raw.s2_0, R.raw.s2_1, R.raw.s2_2, R.raw.s2_3, R.raw.s2_4)
        ))
        println("json: ${json}")

        val writer = BufferedWriter(FileWriter(file))
        writer.use {
            it.write(json.toString())
            it.flush()
        }

        println("json file creation successful!!")

    }
    else {
        println("found ${path}")

        // read
        val reader = BufferedReader(FileReader(file))
        val json = JSONObject(reader.readLines().joinToString())
        println("json: ${json}")
    }

}
