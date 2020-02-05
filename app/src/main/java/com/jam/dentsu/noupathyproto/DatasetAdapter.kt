package com.jam.dentsu.noupathyproto

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class DatasetAdapter(private val context: Context, private var datasets: List<DataSet> = getDataset().dataset) : BaseAdapter() {

    private val inflater = LayoutInflater.from(context)

    private fun createView(parent: ViewGroup?): View {

        val view = inflater.inflate(R.layout.dataset_cell, parent, false)
        view.tag = ViewHolder(view)

        datasets  = getDataset().dataset

        return view
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

        // convertViewがある場合はそれを使い、ない場合は新しく作る
        val view = convertView ?: createView(parent)

        // positionから必要なデータを取得
        val name = getItem(position).name
        val status = getItem(position).status

        // タグからViewHolderを取得
        val viewHolder = view.tag as ViewHolder
        viewHolder.name.text = name
        viewHolder.status.text = status

        return view
    }

    // positionで指定されたインデックスにあるデータを返す
    override fun getItem(position: Int) = this.datasets[position]


    // アダプター内で行の一意性を保証するためのID。DBを使う場合は各レコードのIDなど。ここでは便宜的にpositionをLong型に変換して返している
    override fun getItemId(position: Int): Long {

        return position.toLong()
    }

    // 表示するデータの件数
    override fun getCount(): Int {

        return datasets.size
    }
}

private class ViewHolder(view: View) {

    val name = view.findViewById<TextView>(R.id.name)
    val status = view.findViewById<TextView>(R.id.status)
}