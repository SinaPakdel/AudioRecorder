package ir.sina.audiorecord

import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import ir.sina.audiorecord.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity(), Timer.OnTimerTickListener {
    companion object {
        const val REQ_CODE = 101
        const val TAG = "MainActivity"
    }

    private lateinit var binding: ActivityMainBinding

    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var amplitudes: ArrayList<Float>

    private var isReadGranted = false
    private var isWriteGranted = false
    private var isRecordAudioGranted = false

    private var permissionGranted = false
    private lateinit var recorder: MediaRecorder
    private var dirPath = ""
    private var fileName = ""
    private var isRecording = false
    private var isPause = false
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    private val timer by lazy {
        Timer(this)
    }

    private val player by lazy {
        AudioPlayer(applicationContext)
    }
    private val vibrator by lazy {
        getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                isReadGranted = permissions[android.Manifest.permission.READ_EXTERNAL_STORAGE] ?: isReadGranted
                isWriteGranted = permissions[android.Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: isWriteGranted
                isRecordAudioGranted = permissions[android.Manifest.permission.RECORD_AUDIO] ?: isRecordAudioGranted
            }


        requestPermissions()
        bottomSheetBehavior = BottomSheetBehavior.from(binding.included.bottomSheetLayout)
        bottomSheetBehavior.apply {
            peekHeight = 0
            state = BottomSheetBehavior.STATE_COLLAPSED
        }
        binding.btnRecord.setOnClickListener {
            when {
                isPause -> resumeRecording()
                isRecording -> pauseRecording()
                else -> startRecording()
            }
//            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        }

        binding.btnList.setOnClickListener {
            Toast.makeText(this, "List Button", Toast.LENGTH_SHORT).show()
        }

        binding.btnDone.setOnClickListener {
            stopRecording()
            Toast.makeText(this, "Record Saved", Toast.LENGTH_SHORT).show()
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            binding.bottomSheetBg.visible()
            binding.included.fileNameInput.setText(fileName)
        }

        binding.btnDelete.setOnClickListener {
            stopRecording()
            Log.e(TAG, "onCreate: ")
            File("$dirPath$fileName.mp3").delete()
            Toast.makeText(this, "Record Deleted", Toast.LENGTH_SHORT).show()

        }

        binding.included.ok.setOnClickListener {
            dismiss()
            saveAudioFile()
        }
        binding.included.cancel.setOnClickListener {
            File("$dirPath$fileName.mp3").delete()
            dismiss()
        }

        binding.bottomSheetBg.setOnClickListener {
            File("$dirPath$fileName.mp3").delete()
            dismiss()
        }

        binding.playFile.setOnClickListener {
            "$dirPath$fileName.mp3"?.let {
                player.playFile(it)
            }
        }
        binding.btnDelete.isClickable = false
    }

    private fun saveAudioFile() {
        val newFileName = binding.included.fileNameInput.text.toString()
        if (newFileName != fileName) {
            File("$dirPath$fileName.mp3").renameTo(File("$dirPath$newFileName.mp3"))
        }
    }

    private fun dismiss() {
        binding.bottomSheetBg.gone()
        hideKeyboard(binding.included.fileNameInput)
        Handler(mainLooper).postDelayed({
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }, 100)
    }

    private fun hideKeyboard(view: View) {
        val methodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        methodManager.hideSoftInputFromWindow(view.windowToken, 0)
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
        recorder = MediaRecorder()
        dirPath = "${externalCacheDir?.absolutePath}/"
        fileName = SimpleDateFormat("yyyy.MM.DD_hh.mm.ss").format(Date())
        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile("$dirPath$fileName.mp3")
            try {
                prepare()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error preparing MediaRecorder: ${e.message}")
                return
            }
            start()

        }
        binding.btnRecord.setImageResource(R.drawable.ic_pause)
        isRecording = true
        isPause = false
        timer.start()

        binding.btnDelete.isClickable = true
        binding.btnDelete.setImageResource(R.drawable.ic_delete)
        binding.btnList.gone()
        binding.btnDone.visible()

    }

    private fun pauseRecording() {
        recorder.pause()
        isPause = true
        binding.btnRecord.setImageResource(R.drawable.ic_record)
        timer.pause()
    }

    private fun stopRecording() {
        timer.stop()
        recorder.apply {
            stop()
            release()
        }
        isPause = false
        isRecording = false

        with(binding) {
            btnList.visible()
            btnDone.gone()

            btnDelete.isClickable = false
            btnDelete.setImageResource(R.drawable.ic_delete_disabled)
            btnRecord.setImageResource(R.drawable.ic_record)
            tvTimer.text = "00:00.00"
        }

        amplitudes = binding.waves.clear()
    }

    private fun resumeRecording() {
        recorder.resume()
        isPause = false
        binding.btnRecord.setImageResource(R.drawable.ic_pause)
        timer.start()
        binding.btnDelete.isClickable = true
        binding.btnDelete.setImageResource(R.drawable.ic_delete)

    }

    override fun onTimerTick(duration: String) {
        binding.tvTimer.text = duration
        binding.waves.addAmplitude(recorder.maxAmplitude.toFloat())
    }


    private fun requestPermissions() {
        isReadGranted = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        isWriteGranted = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED


        isRecordAudioGranted = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val permissionsRequest: MutableList<String> = ArrayList()
        if (!isReadGranted) permissionsRequest.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        if (!isWriteGranted) permissionsRequest.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (!isRecordAudioGranted) permissionsRequest.add(android.Manifest.permission.RECORD_AUDIO)
        if (permissionsRequest.isNotEmpty()) permissionLauncher.launch(permissionsRequest.toTypedArray())

    }
}