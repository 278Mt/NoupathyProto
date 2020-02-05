package com.jam.dentsu.noupathyproto

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import kotlin.random.Random

val SetSize = 10 // 何セットやるか
val SOA:Long = 280 //Interstimulus interval(ISI)
val repeatSize = 3

fun makePlayList(): MutableList<Int> {

    var list = mutableListOf<Int>()
    var sounds = mutableListOf(1,2,3,4,5)
    var secondLastSound = 0
    var lastSound = 0
    var set = 0

    while (set < SetSize*repeatSize) {
        val s = chooseSound(sounds, lastSound, secondLastSound)
        if (s != null) {
            list.add(s)
            secondLastSound = lastSound
            lastSound = s
            sounds.remove(s)
        }

        if (sounds.size == 0) {

            set++
            sounds = mutableListOf(1,2,3,4,5)
        }
    }
    return list
}

private fun chooseSound(list: List<Int>, last: Int, secondLast: Int): Int? {

    val i = Random.nextInt(list.size)
    val s = list[i]
    if (s == last || s==secondLast) {
        chooseSound(list, last, secondLast)
        return null
    }
    return s
}

fun nowPlaying(num: String, image: ImageView, progress: ProgressBar) {

    progress.visibility = View.INVISIBLE

    when(num) {
        "0"->{
            image.visibility = View.INVISIBLE
            progress.visibility = View.VISIBLE
        }
        "1"->{
            image.visibility = View.VISIBLE
            image.translationX = -585F
        }
        "2"->{
            image.visibility = View.VISIBLE
            image.translationX = -295F
        }
        "3"->{
            image.visibility = View.VISIBLE
            image.translationX = 0F
        }
        "4"->{
            image.visibility = View.VISIBLE
            image.translationX = 295F
        }
        "5"->{
            image.visibility = View.VISIBLE
            image.translationX = 585F
        }
    }
}

fun highlightView(num: String, s1: View, s2: View, s3: View, s4: View, s5: View, image: ImageView) {

    when(num) {
        "0"->{
            s1.background = ColorDrawable(Color.LTGRAY)
            s2.background = ColorDrawable(Color.LTGRAY)
            s3.background = ColorDrawable(Color.LTGRAY)
            s4.background = ColorDrawable(Color.LTGRAY)
            s5.background = ColorDrawable(Color.LTGRAY)
            image.setImageResource(R.mipmap.image0)
        }
        "1"->{
            s1.background = ColorDrawable(Color.MAGENTA)
            s2.background = ColorDrawable(Color.LTGRAY)
            s3.background = ColorDrawable(Color.LTGRAY)
            s4.background = ColorDrawable(Color.LTGRAY)
            s5.background = ColorDrawable(Color.LTGRAY)
            image.setImageResource(R.mipmap.image1)
        }
        "2"->{
            s1.background = ColorDrawable(Color.LTGRAY)
            s2.background = ColorDrawable(Color.MAGENTA)
            s3.background = ColorDrawable(Color.LTGRAY)
            s4.background = ColorDrawable(Color.LTGRAY)
            s5.background = ColorDrawable(Color.LTGRAY)
            image.setImageResource(R.mipmap.image2)
        }
        "3"->{
            s1.background = ColorDrawable(Color.LTGRAY)
            s2.background = ColorDrawable(Color.LTGRAY)
            s3.background = ColorDrawable(Color.MAGENTA)
            s4.background = ColorDrawable(Color.LTGRAY)
            s5.background = ColorDrawable(Color.LTGRAY)
            image.setImageResource(R.mipmap.image3)
        }
        "4"->{
            s1.background = ColorDrawable(Color.LTGRAY)
            s2.background = ColorDrawable(Color.LTGRAY)
            s3.background = ColorDrawable(Color.LTGRAY)
            s4.background = ColorDrawable(Color.MAGENTA)
            s5.background = ColorDrawable(Color.LTGRAY)
            image.setImageResource(R.mipmap.image4)
        }
        "5"->{
            s1.background = ColorDrawable(Color.LTGRAY)
            s2.background = ColorDrawable(Color.LTGRAY)
            s3.background = ColorDrawable(Color.LTGRAY)
            s4.background = ColorDrawable(Color.LTGRAY)
            s5.background = ColorDrawable(Color.MAGENTA)
            image.setImageResource(R.mipmap.image5)
        }
    }
}
