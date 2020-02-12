package com.jam.dentsu.noupathyproto

import android.app.Activity
import android.content.Intent
import android.media.AudioAttributes
import android.media.SoundPool
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_sound_select.*

class SoundSelectActivity : AppCompatActivity() {

    private lateinit var sp: SoundPool
    private var sArray = arrayOf(arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0), arrayOf(0, 0, 0, 0, 0))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sound_select)

        loadSounds()

        val currentDataset = intent.getStringExtra("currentDataset")
        val result = Intent()

        soundBtn1_1.setOnClickListener { regardingSoundBtn(1, 1) }
        soundBtn1_2.setOnClickListener { regardingSoundBtn(1, 2) }
        soundBtn1_3.setOnClickListener { regardingSoundBtn(1, 3) }
        soundBtn1_4.setOnClickListener { regardingSoundBtn(1, 4) }
        soundBtn1_5.setOnClickListener { regardingSoundBtn(1, 5) }

        soundBtn2_1.setOnClickListener { regardingSoundBtn(2, 1) }
        soundBtn2_2.setOnClickListener { regardingSoundBtn(2, 2) }
        soundBtn2_3.setOnClickListener { regardingSoundBtn(2, 3) }
        soundBtn2_4.setOnClickListener { regardingSoundBtn(2, 4) }
        soundBtn2_5.setOnClickListener { regardingSoundBtn(2, 5) }

        soundBtn3_1.setOnClickListener { regardingSoundBtn(3, 1) }
        soundBtn3_2.setOnClickListener { regardingSoundBtn(3, 2) }
        soundBtn3_3.setOnClickListener { regardingSoundBtn(3, 3) }
        soundBtn3_4.setOnClickListener { regardingSoundBtn(3, 4) }
        soundBtn3_5.setOnClickListener { regardingSoundBtn(3, 5) }

        use1.setOnClickListener {
            setSoundSetID(currentDataset, 1)
            setResult(Activity.RESULT_OK, result)
            finish()
        }

        use2.setOnClickListener {
            setSoundSetID(currentDataset, 2)
            setResult(Activity.RESULT_OK, result)
            finish()
        }

        use3.setOnClickListener {
            setSoundSetID(currentDataset, 3)
            setResult(Activity.RESULT_OK, result)
            finish()
        }

        println("sound1_1 = ${sound1_1}")

        sound1_1.setOnClickListener { sp.play(sArray[0][0], 1.0f, 1.0f, 0, 0, 1.0f) }
        sound1_2.setOnClickListener { sp.play(sArray[0][1], 1.0f, 1.0f, 0, 0, 1.0f) }
        sound1_3.setOnClickListener { sp.play(sArray[0][2], 1.0f, 1.0f, 0, 0, 1.0f) }
        sound1_4.setOnClickListener { sp.play(sArray[0][3], 1.0f, 1.0f, 0, 0, 1.0f) }
        sound1_5.setOnClickListener { sp.play(sArray[0][4], 1.0f, 1.0f, 0, 0, 1.0f) }

        sound2_1.setOnClickListener { sp.play(sArray[1][0], 1.0f, 1.0f, 0, 0, 1.0f) }
        sound2_2.setOnClickListener { sp.play(sArray[1][1], 1.0f, 1.0f, 0, 0, 1.0f) }
        sound2_3.setOnClickListener { sp.play(sArray[1][2], 1.0f, 1.0f, 0, 0, 1.0f) }
        sound2_4.setOnClickListener { sp.play(sArray[1][3], 1.0f, 1.0f, 0, 0, 1.0f) }
        sound2_5.setOnClickListener { sp.play(sArray[1][4], 1.0f, 1.0f, 0, 0, 1.0f) }

        sound3_1.setOnClickListener { sp.play(sArray[2][0], 1.0f, 1.0f, 0, 0, 1.0f) }
        sound3_2.setOnClickListener { sp.play(sArray[2][1], 1.0f, 1.0f, 0, 0, 1.0f) }
        sound3_3.setOnClickListener { sp.play(sArray[2][2], 1.0f, 1.0f, 0, 0, 1.0f) }
        sound3_4.setOnClickListener { sp.play(sArray[2][3], 1.0f, 1.0f, 0, 0, 1.0f) }
        sound3_5.setOnClickListener { sp.play(sArray[2][4], 1.0f, 1.0f, 0, 0, 1.0f) }
    }

    private fun regardingSoundBtn(row: Int, col: Int) {
        val intent = Intent(this, PostSoundSelectActivity::class.java)
        intent.putExtra("soundNumArray", arrayOf(row, col))
        println(intent.getIntArrayExtra("soundNumArray"))
        startActivityForResult(intent, 1)
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

        sArray[0][0] = sp.load(this, R.raw.s0_0, 1)
        sArray[0][1] = sp.load(this, R.raw.s0_1, 1)
        sArray[0][2] = sp.load(this, R.raw.s0_2, 1)
        sArray[0][3] = sp.load(this, R.raw.s0_3, 1)
        sArray[0][4] = sp.load(this, R.raw.s0_4, 1)

        sArray[1][0] = sp.load(this, R.raw.s1_0, 1)
        sArray[1][1] = sp.load(this, R.raw.s1_1, 1)
        sArray[1][2] = sp.load(this, R.raw.s1_2, 1)
        sArray[1][3] = sp.load(this, R.raw.s1_3, 1)
        sArray[1][4] = sp.load(this, R.raw.s1_4, 1)

        sArray[2][0] = sp.load(this, R.raw.s2_0, 1)
        sArray[2][1] = sp.load(this, R.raw.s2_1, 1)
        sArray[2][2] = sp.load(this, R.raw.s2_2, 1)
        sArray[2][3] = sp.load(this, R.raw.s2_3, 1)
        sArray[2][4] = sp.load(this, R.raw.s2_4, 1)

    }
}
