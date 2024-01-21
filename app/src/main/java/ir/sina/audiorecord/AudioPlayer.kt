package ir.sina.audiorecord

import android.content.Context
import android.media.MediaPlayer
import androidx.core.net.toUri
import java.io.File

interface IAudioPlayer {
    fun playFile(file: String)
    fun stop()
}
class AudioPlayer(private val context: Context) : IAudioPlayer {
    private var player: MediaPlayer? = null

    override fun playFile(file: String) {
        MediaPlayer.create(context, file.toUri()).apply {
            player = this
            start()
        }
    }

    override fun stop() {
        player?.stop()
        player?.release()
        player = null
    }
}