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
    private var s1_1 = 0
    private var s1_2 = 0
    private var s1_3 = 0
    private var s1_4 = 0
    private var s1_5 = 0

    private var s2_1 = 0
    private var s2_2 = 0
    private var s2_3 = 0
    private var s2_4 = 0
    private var s2_5 = 0

    private var s3_1 = 0
    private var s3_2 = 0
    private var s3_3 = 0
    private var s3_4 = 0
    private var s3_5 = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sound_select)

        loadSounds()

        val currentDataset = intent.getStringExtra("currentDataset")
        val result = Intent()

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

        sound1_1.setOnClickListener { sp.play(s1_1, 1.0f, 1.0f, 0, 0, 1.0f) }
        sound1_2.setOnClickListener { sp.play(s1_2, 1.0f, 1.0f, 0, 0, 1.0f) }
        sound1_3.setOnClickListener { sp.play(s1_3, 1.0f, 1.0f, 0, 0, 1.0f) }
        sound1_4.setOnClickListener { sp.play(s1_4, 1.0f, 1.0f, 0, 0, 1.0f) }
        sound1_5.setOnClickListener { sp.play(s1_5, 1.0f, 1.0f, 0, 0, 1.0f) }

        sound2_1.setOnClickListener { sp.play(s2_1, 1.0f, 1.0f, 0, 0, 1.0f) }
        sound2_2.setOnClickListener { sp.play(s2_2, 1.0f, 1.0f, 0, 0, 1.0f) }
        sound2_3.setOnClickListener { sp.play(s2_3, 1.0f, 1.0f, 0, 0, 1.0f) }
        sound2_4.setOnClickListener { sp.play(s2_4, 1.0f, 1.0f, 0, 0, 1.0f) }
        sound2_5.setOnClickListener { sp.play(s2_5, 1.0f, 1.0f, 0, 0, 1.0f) }

        sound3_1.setOnClickListener { sp.play(s3_1, 1.0f, 1.0f, 0, 0, 1.0f) }
        sound3_2.setOnClickListener { sp.play(s3_2, 1.0f, 1.0f, 0, 0, 1.0f) }
        sound3_3.setOnClickListener { sp.play(s3_3, 1.0f, 1.0f, 0, 0, 1.0f) }
        sound3_4.setOnClickListener { sp.play(s3_4, 1.0f, 1.0f, 0, 0, 1.0f) }
        sound3_5.setOnClickListener { sp.play(s3_5, 1.0f, 1.0f, 0, 0, 1.0f) }
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

        s1_1 = sp.load(this, R.raw.s1, 1)
        s1_2 = sp.load(this, R.raw.s2, 1)
        s1_3 = sp.load(this, R.raw.s3, 1)
        s1_4 = sp.load(this, R.raw.s4, 1)
        s1_5 = sp.load(this, R.raw.s5, 1)

        s2_1 = sp.load(this, R.raw.s2_1, 1)
        s2_2 = sp.load(this, R.raw.s2_2, 1)
        s2_3 = sp.load(this, R.raw.s2_3, 1)
        s2_4 = sp.load(this, R.raw.s2_4, 1)
        s2_5 = sp.load(this, R.raw.s2_5, 1)

        s3_1 = sp.load(this, R.raw.s3_1, 1)
        s3_2 = sp.load(this, R.raw.s3_2, 1)
        s3_3 = sp.load(this, R.raw.s3_3, 1)
        s3_4 = sp.load(this, R.raw.s3_4, 1)
        s3_5 = sp.load(this, R.raw.s3_5, 1)

    }
}
