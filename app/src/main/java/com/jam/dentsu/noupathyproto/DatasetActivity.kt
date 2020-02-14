package com.jam.dentsu.noupathyproto

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import android.widget.Toast


class DatasetActivity : AppCompatActivity() {

    var adapter: DatasetAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dataset)

        val dataset = getDataset()

        val isPlay = intent.getBooleanExtra("isPlay", false)
        val isGame = intent.getBooleanExtra("isGame", false)

        val list = findViewById<ListView>(R.id.datasets)
        adapter = DatasetAdapter(this, dataset.dataset)
        list.adapter = adapter

        list.setOnItemClickListener { _, _, position, _ ->

            // 選択されたデータセットを返す（Playモードの時は学習済データのみ選択可）
            if (isPlay && !adapter!!.getItem(position).status.contains("学習済")) {
                Toast.makeText(applicationContext, "未学習のデータセットは選択できません", Toast.LENGTH_SHORT).show()
            }
            else if(isGame && !adapter!!.getItem(position).status.contains("学習済")) {
                    Toast.makeText(applicationContext, "未学習のデータセットは選択できません", Toast.LENGTH_SHORT).show()
            } else {
                val dataset = adapter!!.getItem(position).name
                val result = Intent()
                result.putExtra("dataset", dataset)
                setResult(Activity.RESULT_OK, result)
                finish()
            }
        }

        val addBtn = findViewById<Button>(R.id.add)
        addBtn.setOnClickListener {

            addDatasetDir()
            // 更新
            adapter = DatasetAdapter(this, dataset.dataset)
            list.adapter = adapter
            adapter!!.notifyDataSetChanged()
        }
    }
}