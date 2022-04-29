package com.example.smh_bloodanalysis_library

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.smh_bloodanalysis_library.databinding.ActivityCameraBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.HashMap
import java.util.concurrent.Executors

private const val CAMERA_PERMISSION_REQUEST_CODE = 1

class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding

    var save = false
    var lastActivity:String = ""
    var mapAnalyses = HashMap<String, String>()

    var textExtraction = TextExtraction(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_camera)

        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (hasCameraPermission()) bindCameraUseCases()
        else requestPermission()
    }
    // checking to see whether user has already granted permission
    private fun hasCameraPermission() =
        ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestPermission(){
        // opening up dialog to ask for camera permission
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // user granted permissions - we can set up our scanner
            bindCameraUseCases()
        } else {
            // user did not grant permissions - we can't use the camera
            Toast.makeText(this,
                "Camera permission required",
                Toast.LENGTH_LONG
            ).show()
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
    private fun bindCameraUseCases() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // setting up the preview use case
            val previewUseCase = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.cameraView.surfaceProvider)
                }

            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            // setting up the analysis use case
            val analysisUseCase = ImageAnalysis.Builder()
                .build()

            // define the actual functionality of our analysis use case
            analysisUseCase.setAnalyzer(
                Executors.newSingleThreadExecutor(),
                { imageProxy ->
                    processImageProxy(recognizer, imageProxy)
                }
            )

            // configure to use the back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    previewUseCase,
                    analysisUseCase)
            } catch (illegalStateException: IllegalStateException) {
                // If the use case has already been bound to another lifecycle or method is not called on main thread.
                Log.e(TAG, illegalStateException.message.orEmpty())
            } catch (illegalArgumentException: IllegalArgumentException) {
                // If the provided camera selector is unable to resolve a camera to be used for the given use cases.
                Log.e(TAG, illegalArgumentException.message.orEmpty())
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onResume() {
        super.onResume()
        if (intent.getStringExtra(Utils().homeActivityKey).toString() != null){
            lastActivity = intent.getStringExtra(Utils().homeActivityKey).toString()
        }
    }

    private fun processImageProxy(
        barcodeScanner: TextRecognizer,
        imageProxy: ImageProxy
    ) {

        imageProxy.image?.let { image ->
            val inputImage =
                InputImage.fromMediaImage(
                    image,
                    imageProxy.imageInfo.rotationDegrees
                )
            if (save){
                imageProxy.image?.close()
                imageProxy.close()
            }else{
                barcodeScanner.process(inputImage)
                    .addOnSuccessListener { barcodeList ->
                        mapAnalyses = textExtraction.findValue(barcodeList)
                        val builder = StringBuilder()

                        mapAnalyses?.forEach{key,value -> builder.append("\n$key : $value") }
                        binding.bottomText.text = builder.toString()

                        if(mapAnalyses.isEmpty()){
                            binding.btnCapture.visibility = View.INVISIBLE
                        }else{
                            binding.btnCapture.visibility = View.VISIBLE
                        }

                    }
                    .addOnFailureListener {
                        // This failure will happen if the barcode scanning model
                        // fails to download from Google Play Services

                        Log.e(TAG, it.message.orEmpty())
                    }.addOnCompleteListener {
                        // When the image is from CameraX analysis use case, must
                        // call image.close() on received images when finished
                        // using them. Otherwise, new images may not be received
                        // or the camera may stall.

                        imageProxy.image?.close()
                        imageProxy.close()
                    }
            }
        }
    }

    fun takePhoto(view: View){
        save = true
        val lastActivityIntent = Intent(this, Class.forName(lastActivity))
        lastActivityIntent.putExtra(Utils().savedUri, mapAnalyses)
        finish()
        startActivity(lastActivityIntent)
    }

    companion object {
        val TAG: String = CameraActivity::class.java.simpleName
    }
}