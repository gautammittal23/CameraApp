package com.app.cameraapp.Camera

import android.Manifest
import android.R.attr
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Camera
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.speech.RecognizerIntent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.app.cameraapp.R
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.log.logcat
import io.fotoapparat.log.loggers
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.selector.back
import io.fotoapparat.selector.front
import io.fotoapparat.selector.off
import io.fotoapparat.selector.torch
import io.fotoapparat.view.CameraView
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.util.*


class MainActivity : AppCompatActivity() {


    var fotoapparat: Fotoapparat? = null
    val filename = UUID.randomUUID().toString()+".png"
    val sd = Environment.getExternalStorageDirectory()
    val dest = File(sd, filename)
    var fotoapparatState : FotoapparatState? = null
    var cameraStatus : CameraState? = null
    var flashState: FlashState? = null

    val permissions = arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createFotoapparat()

        cameraStatus = CameraState.BACK
        flashState = FlashState.OFF
        fotoapparatState = FotoapparatState.OFF

        fab_camera.setOnClickListener {
            takePhoto()
        }

        fab_switch_camera.setOnClickListener {
            switchCamera()
        }

        fab_flash.setOnClickListener {
            changeFlashState()
        }


        fab_mic.setOnClickListener {
            displaySpeechRecognizer()
        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


        if (requestCode == 10 && resultCode == Activity.RESULT_OK) {
            val spokenText: String? =
                data!!.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).let { results ->
                    results[0]
                }
            if (spokenText.equals("smile"))
            {

                val timer = object: CountDownTimer(4000, 1000) {
                    override fun onTick(millisUntilFinished: Long)
                    {
                        val sec=millisUntilFinished/1000

                        img_counter.visibility=View.VISIBLE

                        if(sec==1L)
                            img_counter.setBackgroundResource(R.drawable.ic_1)
                        else if(sec==2L)
                            img_counter.setBackgroundResource(R.drawable.ic_2)
                        else if(sec==3L)
                            img_counter.setBackgroundResource(R.drawable.ic_3)
                    }

                    override fun onFinish() {

                        img_counter.visibility=View.GONE

                        Toast.makeText(applicationContext,"Ticker Finish",Toast.LENGTH_LONG).show()

                        takePhoto()

                    }
                }
                timer.start()

               // Toast.makeText(this,"My Text Recognised",Toast.LENGTH_LONG).show()

            }

            else
            {
                Toast.makeText(
                    applicationContext,
                    "To Click a photo using Voice Capture Say : Smile ",
                    Toast.LENGTH_LONG
                ).show()

            }


        }

    }


    // Create an intent that can start the Speech Recognizer activity
    private fun displaySpeechRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        }
        // Start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, 10)
    }





    private fun createFotoapparat(){
        val cameraView = findViewById<CameraView>(R.id.camera_view)

      

        fotoapparat = Fotoapparat(
            context = this,
            view = camera_view,
            scaleType = ScaleType.CenterCrop,
            lensPosition = back(),
            logger = loggers(
                logcat()
            ),
            cameraErrorCallback = { error ->
                println("Recorder errors: $error")
            }
        )
    }

    private fun changeFlashState() {
        fotoapparat?.updateConfiguration(
            CameraConfiguration(
                flashMode = if(flashState == FlashState.TORCH)
                {
                    off()
                }


                else
                {
                    torch()
                }
            )
        )

        if(flashState == FlashState.TORCH)
        {
            flashState = FlashState.OFF
            fab_flash.setImageResource(R.drawable.ic_flash_off)
        }
        else
        {
            flashState = FlashState.TORCH
            fab_flash.setImageResource(R.drawable.ic_flash_on)
        }
    }

    private fun switchCamera() {
        fotoapparat?.switchTo(
            lensPosition =  if (cameraStatus == CameraState.BACK) front() else back(),
            cameraConfiguration = CameraConfiguration()
        )

        if(cameraStatus == CameraState.BACK)
        {
            cameraStatus = CameraState.FRONT

            fab_flash.isClickable=false

            fotoapparat?.autoFocus()
        }
        else
        {
            fab_flash.isClickable=true
            cameraStatus = CameraState.BACK
            fotoapparat?.autoFocus()
        }
    }

    private fun takePhoto() {
        if (hasNoPermissions()) {
            requestPermission()
        }else{
            Toast.makeText(this,"Take Photo worked",Toast.LENGTH_LONG).show()

            fotoapparat
                ?.takePicture()
                ?.saveToFile(dest)
        }
    }



    override fun onStart() {
        super.onStart()
        if (hasNoPermissions()) {
            requestPermission()
        }else{
            fotoapparat?.start()
            fotoapparatState = FotoapparatState.ON
        }
    }

    private fun hasNoPermissions(): Boolean{
        return ContextCompat.checkSelfPermission(this,

            Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED|| ContextCompat.checkSelfPermission(this,

            Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,
            Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission(){
        ActivityCompat.requestPermissions(this, permissions,0)
    }

    override fun onStop() {
        super.onStop()
        fotoapparat?.stop()
        FotoapparatState.OFF
    }

    override fun onResume() {
        super.onResume()
        if(!hasNoPermissions() && fotoapparatState == FotoapparatState.OFF){
            val intent = Intent(baseContext, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }



}

enum class CameraState{
    FRONT, BACK
}

enum class FlashState{
    TORCH, OFF
}

enum class FotoapparatState{
    ON, OFF
}

