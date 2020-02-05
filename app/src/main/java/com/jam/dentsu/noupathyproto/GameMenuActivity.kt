package com.jam.dentsu.noupathyproto
import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import kotlinx.android.synthetic.main.activity_play.*

class GameMenuActivity : AppCompatActivity() {
    private var currentDataset = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_menu)

        val level1Button = findViewById<Button>(R.id.level1Button)
        level1Button.setOnClickListener {

            val intent = Intent(this, GameActivity::class.java)
            intent.putExtra("DATA_SET",datasetName.text)
            intent.putExtra("GAME_LEVEL",1)
            startActivity(intent)
        }

        val level2Button = findViewById<Button>(R.id.level2Button)
        level2Button.setOnClickListener {

            val intent = Intent(this, GameActivity::class.java)
            intent.putExtra("DATA_SET",datasetName.text)
            intent.putExtra("GAME_LEVEL",2)
            startActivity(intent)
        }

        val level3Button = findViewById<Button>(R.id.level3Button)
        level3Button.setOnClickListener {

            val intent = Intent(this, GameActivity::class.java)
            intent.putExtra("DATA_SET",datasetName.text)
            intent.putExtra("GAME_LEVEL",3)
            startActivity(intent)
        }

        val level4Button = findViewById<Button>(R.id.level4Button)
        level4Button.setOnClickListener {

            val intent = Intent(this, GameActivity::class.java)
            intent.putExtra("DATA_SET",datasetName.text)
            intent.putExtra("GAME_LEVEL",4)
            startActivity(intent)
        }

        val level5Button = findViewById<Button>(R.id.level5Button)
        level5Button.setOnClickListener {

            val intent = Intent(this, GameActivity::class.java)
            intent.putExtra("DATA_SET",datasetName.text)
            intent.putExtra("GAME_LEVEL",5)
            startActivity(intent)
        }

        val level6Button = findViewById<Button>(R.id.level6Button)
        level6Button.setOnClickListener {

            val intent = Intent(this, GameActivity::class.java)
            intent.putExtra("DATA_SET",datasetName.text)
            intent.putExtra("GAME_LEVEL",6)
            startActivity(intent)
        }

        selectBtn.setOnClickListener {
            val intent = Intent(this, DatasetActivity::class.java)
            intent.putExtra("isGame", true)
            startActivityForResult(intent, 1)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {

            val selectedDataset = data.getStringExtra("dataset")
            currentDataset = selectedDataset
            datasetName.text = currentDataset
        }
    }

}
