package ir.sina.audiorecord

import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.ActivityCompat
import ir.sina.audiorecord.databinding.ActivityMainBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity(), Timer.OnTimerTickListener {
    private lateinit var amplitudes: ArrayList<Float>
    private lateinit var binding: ActivityMainBinding

    companion object {
        const val REQ_CODE = 101
    }

    private val permissions = arrayOf(android.Manifest.permission.RECORD_AUDIO)
    private var permissionGranted = false
    private lateinit var recorder: MediaRecorder
    private var dirPath = ""
    private var fileName = ""
    private var isRecording = false
    private var isPaues = false
    private lateinit var timer: Timer
    private lateinit var vibrator: Vibrator
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        timer = Timer(this)

        permissionGranted = ActivityCompat.checkSelfPermission(
            this,
            permissions[0]
        ) == PackageManager.PERMISSION_GRANTED
        if (!permissionGranted) ActivityCompat.requestPermissions(this, permissions, REQ_CODE)
        binding.btnRecord.setOnClickListener {
            when {
                isPaues -> resumeRecording()
                isRecording -> pauseRecording()
                else -> startRecording()
            }
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    private fun pauseRecording() {
        recorder.pause()
        isPaues = true
        binding.btnRecord.setImageResource(R.drawable.ic_record)
        timer.pause()
    }

    private fun resumeRecording() {
        recorder.resume()
        isPaues = false
        binding.btnRecord.setImageResource(R.drawable.ic_pause)
        timer.start()
        binding.btnDelete.isClickable=true
        binding.btnDelete.setImageResource(R.drawable.ic_delete)

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CODE) permissionGranted =
            grantResults[0] == PackageManager.PERMISSION_GRANTED
    }


    private fun startRecording() {
        if (!permissionGranted) {
            ActivityCompat.requestPermissions(this, permissions, REQ_CODE)
            return
        }
        recorder = MediaRecorder()
        dirPath = "${externalCacheDir?.absolutePath}/"
        val simpleDateFormat = SimpleDateFormat("yyyy.MM.DD_hh.mm")
        val date = simpleDateFormat.format(Date())
        fileName = "audio_record_${date}"
        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile("$dirPath$fileName.mp3")
            try {
                prepare()
            } catch (e: IOException) {
            }
            start()
        }
        isRecording = true
        isPaues = false
        binding.btnRecord.setImageResource(R.drawable.ic_pause)
        timer.start()
    }


    private fun stopRecording() {
        timer.stop()
        recorder.apply {
            stop()
            release()
        }
        isPaues = false
        isRecording = false
        binding.btnDelete.isClickable=false
        binding.btnDelete.setImageResource(R.drawable.ic_delete)
        binding.btnRecord.setImageResource(R.drawable.ic_record)
        binding.tvTimer.text="00:00.00"

        amplitudes = binding.waves.clear()
    }


    override fun onTimerTick(duration: String) {

        binding.tvTimer.text = duration
        binding.waves.addAmplitude(recorder.maxAmplitude.toFloat())
    }
}