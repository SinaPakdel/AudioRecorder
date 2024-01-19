package ir.sina.audiorecord

import android.view.View

fun View.gone() {
    this.visibility = View.GONE
}

fun View.visible() {
    this.visibility = View.VISIBLE
}
fun View.inVisible() {
    this.visibility = View.INVISIBLE
}