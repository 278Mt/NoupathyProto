package com.jam.dentsu.noupathyproto

import android.os.Environment
import org.json.JSONObject
import java.io.*
import java.nio.file.Files

val APP_ROOT = "Noupathy"

data class DataSet(val name: String, val status: String)

data class DataSets(val dataset: List<DataSet>)

data class DatasetStatus(var data1: Int=0, var data2: Int=0, var data3: Int=0, var data4: Int=0, var data5: Int=0, var complete: Boolean=false, var learning: Boolean=false)

fun createDummyDataset(): DataSets {

    val datasets = arrayListOf<DataSet>()

    val data1 = DataSet(name = "test1", status = "done")
    val data2 = DataSet(name = "test2", status = "none")

    datasets.add(data1)
    datasets.add(data2)

    return DataSets(datasets)
}

fun getDataset(): DataSets {

    val datasets = arrayListOf<DataSet>()

    val dirs = getAllDirs()

    for (dir in dirs) {
        var st = ""
        val status = getDirStatus(dir.name)
        if (status.learning) {
            val acc = getAccuracy(dir.name,5)
            st = "学習済 ${acc}"
        }
        val set = DataSet(name = dir.name, status = st)
        datasets.add(set)
    }

    return DataSets(datasets)
}

private fun makeAppRootDir() {

    val path = "${Environment.getExternalStorageDirectory().path}/${APP_ROOT}/"
    val root = File(path)
    if (!root.exists()) {
        root.mkdir()
    }
}

fun addDatasetDir() {

    makeAppRootDir()

    val name = "DATASET_${(getDirCount()+1)}"

    val path = "${Environment.getExternalStorageDirectory().path}/${APP_ROOT}/${name}"
    val dir = File(path)
    if (!dir.exists()) {
        dir.mkdir()
    }
}

fun getDirCount(): Int {

    makeAppRootDir()
    val count = getAllDirs().size

    return count
}

private fun getAllDirs(): Array<File> {

    val path = "${Environment.getExternalStorageDirectory().path}/${APP_ROOT}/"
    val root = File(path)
    val dirs = root.listFiles()

    return dirs
}

fun getDirStatus(dir: String): DatasetStatus {

    val path = "${Environment.getExternalStorageDirectory().path}/${APP_ROOT}/${dir}/"
    val dir = File(path)
    val files = dir.list()

    var status = DatasetStatus()

    for (file in files) {
        println(file)
        if (File(file).extension == "csv") {
            val num = file.substring(file.length-5, file.length-4)
            when(num) {
                "1"-> status.data1++
                "2"-> status.data2++
                "3"-> status.data3++
                "4"-> status.data4++
                "5"-> status.data5++
            }
        } else {
            if (file == "model.json") status.learning = true
        }
        if (status.data1 + status.data2 + status.data3 + status.data4 + status.data5 == 3*5) status.complete = true
    }

    return status
}

fun learnedDataIsExist(): Boolean {

    val dirs = getAllDirs()
    for (dir in dirs) {
        println("dir: ${dir}")
        val path = File(dir.toURI())
        val files = path.list()

        if (files.contains("model.json")) {
            return true
        }
    }
    return false
}

fun getNextTarget(dataset: DatasetStatus): Int {

    if (dataset.learning) return -1

    if (dataset.complete) return 0

    val list = listOf(dataset.data1, dataset.data2, dataset.data3, dataset.data4, dataset.data5)
    val min = list.indexOf(list.min())

    return min+1
}

fun deleteFile(dir: String, name: String) {

    val path = "${Environment.getExternalStorageDirectory().path}/${APP_ROOT}/${dir}/${name}"
    val file = File(path)
    file.delete()
}

fun getSoundSetID(dir: String): Int {

    var sound_set_id = 1

    val file = File("${Environment.getExternalStorageDirectory().path}/${APP_ROOT}/${dir}", "soundset.json")

    if (file.exists()) {

        val reader = BufferedReader(FileReader(file))
        val data = reader.readLines().joinToString()
        val json = JSONObject(data)
        sound_set_id = json.getInt("sound_set_id")
    } else {

        val data = JSONObject()
        data.put("sound_set_id", sound_set_id)

        // 作成しておく
        val writer = BufferedWriter(FileWriter(file))
        writer.use {
            it.write(data.toString())
            it.flush()
        }
    }

    return sound_set_id
}

fun setSoundSetID(dir: String, id: Int) {

    val file = File("${Environment.getExternalStorageDirectory().path}/${APP_ROOT}/${dir}", "soundset.json")

    val reader = BufferedReader(FileReader(file))
    val data = reader.readLines().joinToString()
    val json = JSONObject(data)
    json.put("sound_set_id", id)

    val writer = BufferedWriter(FileWriter(file))
    writer.use {
        it.write(json.toString())
        it.flush()
    }
}
