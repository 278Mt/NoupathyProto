package com.jam.dentsu.noupathyproto

import android.app.Activity
import android.content.Intent
import android.media.AudioAttributes
import android.media.SoundPool
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import kotlinx.android.synthetic.main.activity_sound_select.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

class SoundSelectActivity : AppCompatActivity() {

    private lateinit var sp: SoundPool
    private var sArray = arrayOf(arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sound_select)

        loadSounds()

        val currentDataset = intent.getStringExtra("currentDataset")
        val result = Intent()

        val soundBtnList = listOf(
            listOf(soundBtn1_1, soundBtn1_2, soundBtn1_3, soundBtn1_4, soundBtn1_5),
            listOf(soundBtn2_1, soundBtn2_2, soundBtn2_3, soundBtn2_4, soundBtn2_5),
            listOf(soundBtn3_1, soundBtn3_2, soundBtn3_3, soundBtn3_4, soundBtn3_5)
        )

        for (i in 0 until 3) {
            for (j in 0 until 5) {
                soundBtnList[i][j].setOnClickListener {
                    val intent = Intent(this, PostSoundSelectActivity::class.java)
                    intent.putExtra("soundNumString", "$i-$j")
                    println("soundNumString: ${intent.getStringExtra("soundNumString")}")
                    startActivityForResult(intent, 1)
                    loadSounds()
                }
            }
        }

        val useList = listOf(use1, use2, use3)

        for (i in 0 until 3) {
            useList[i].setOnClickListener {
                setSoundSetID(currentDataset, i+1)
                setResult(Activity.RESULT_OK, result)
                finish()
            }
        }

        println("sound1_1 = $sound1_1")

        val soundList = listOf(
            listOf(sound1_1, sound1_2, sound1_3, sound1_4, sound1_5),
            listOf(sound2_1, sound2_2, sound2_3, sound2_4, sound2_5),
            listOf(sound3_1, sound3_2, sound3_3, sound3_4, sound3_5)
        )

        for (i in 0 until 3) {
            for (j in 0 until 5) {
                soundList[i][j].setOnClickListener { sp.play(sArray[i][j], 1.0f, 1.0f, 0, 0, 1.0f) }
            }
        }
    }

    private fun loadSounds() {

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
                println(partNewExList)
                sArray[i][j] = sp.load(this, partNewExList[j].toInt(), 1)
            }
        }
        println("sArray: $sArray, is int: ${sArray[0][0] is Int}")
    }
}
