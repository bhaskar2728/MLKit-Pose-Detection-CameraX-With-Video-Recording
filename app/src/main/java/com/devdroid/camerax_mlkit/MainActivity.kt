package com.devdroid.camerax_mlkit

import android.annotation.SuppressLint
import android.graphics.Point
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Button
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var poseDetector:  PoseDetector
    private lateinit var videoCapture: VideoCapture
    private lateinit var cameraProviderFuture : ListenableFuture<ProcessCameraProvider>
    private lateinit var btnStart:Button
    private lateinit var btnStop:Button

    @SuppressLint("MissingPermission", "RestrictedApi", "UnsafeExperimentalUsageError")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)

        btnStart.setOnClickListener {
            val file = File(externalMediaDirs.first(),
                "${System.currentTimeMillis()}.mp4")

           val outputFileOptions =  VideoCapture.OutputFileOptions.Builder(file).build()


            videoCapture.startRecording(outputFileOptions,ContextCompat.getMainExecutor(this),object: VideoCapture.OnVideoSavedCallback{
                override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                    Log.d("Check:","On Video Saved")
                }

                override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                    Log.d("Check:","On Video Error" + message)
                }

            })
        }

        btnStop.setOnClickListener {
            videoCapture.stopRecording()
        }

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)

        },ContextCompat.getMainExecutor(this))

        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()

        poseDetector = PoseDetection.getClient(options)


    }

    @SuppressLint("RestrictedApi", "UnsafeExperimentalUsageError", "NewApi")
    private fun bindPreview(cameraProvider: ProcessCameraProvider){

        Log.d("Check:","inside bind preview")

        val preview = Preview.Builder().build()

        preview.setSurfaceProvider(previewView.surfaceProvider)

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
        val point = Point()
        val size = display?.getRealSize(point)

        videoCapture = VideoCapture.Builder()
            .setTargetResolution(Size(point.x,point.y))
            .build()



        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetResolution(Size(point.x,point.y))
            .build()

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), ImageAnalysis.Analyzer { imageProxy ->

            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val image = imageProxy.image

            if(image!=null){

                val processImage = InputImage.fromMediaImage(image,rotationDegrees)

                poseDetector.process(processImage)
                    .addOnSuccessListener {

                        if(parentLayout.childCount>3){
                            parentLayout.removeViewAt(3)
                        }
                        if(it.allPoseLandmarks.isNotEmpty()){

                            if(parentLayout.childCount>3){
                                parentLayout.removeViewAt(3)
                            }

                            val element = Draw(applicationContext,it)
                            parentLayout.addView(element)
                        }
                        imageProxy.close()
                    }
                    .addOnFailureListener{


                        imageProxy.close()
                    }
            }


        })

        cameraProvider.bindToLifecycle(this,cameraSelector,imageAnalysis,preview,videoCapture)

    }
}