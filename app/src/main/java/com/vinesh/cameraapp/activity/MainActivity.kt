package com.vinesh.cameraapp.activity

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.common.util.concurrent.ListenableFuture
import com.vinesh.cameraapp.R
import com.vinesh.cameraapp.assets.Constants
import com.vinesh.cameraapp.ui.theme.CameraAppTheme
import com.vinesh.cameraapp.ui.theme.PrimaryColor
import com.vinesh.cameraapp.ui.theme.SecondaryColor
import java.io.File
import java.io.FileOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class MainActivity : ComponentActivity() {

    private lateinit var cameraSelector: CameraSelector
    private var imageCapture: ImageCapture? = null
    private lateinit var lifecycleOwner: LifecycleOwner
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var previewView: PreviewView
    private val activity = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CameraAppTheme {
                Surface(color = MaterialTheme.colors.background) {
                    CameraScreen()
                }
            }
        }
    }

    private fun allPermissionGranted() = true//Constants.REQUIRED_PERMISSIONS.all {
//        ContextCompat.checkSelfPermission(baseContext,it) == PackageManager.PERMISSION_GRANTED
//    }
//
//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if(requestCode == Constants.REQUEST_CODE_PERMISSIONS){
//            if(allPermissionGranted()){
//                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//                startCamera()
//            }
//            else{
//                Toast.makeText(this,"Permissions Denied", Toast.LENGTH_LONG).show()
//                finishAndRemoveTask()
//            }
//        }
//    }

    @Preview
    @Composable
    fun CameraScreen() {

        Box(modifier = Modifier.background(Color.White)
            .width(50.dp)
            .height(50.dp)) {
            Box(modifier = Modifier.background(Color.Black)
                .width(20.dp)
                .height(20.dp)
                .align(Alignment.Center)) {

            }
        }

        ConstraintLayout(modifier = Modifier
            .background(SecondaryColor)
            .fillMaxSize()
        ){
            val (flashSection,cameraPreview,bottomControls) = createRefs()
            FlashSection(
                Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth()
                    .constrainAs(flashSection) {
                        top.linkTo(parent.top, 0.dp)
                    }
                    .layoutId("cameraFlashSection")
            )

            val context = LocalContext.current
            lifecycleOwner = LocalLifecycleOwner.current
            cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            previewView = PreviewView(context).apply{
                id = R.id.cameraxPreviewView
            }

            AndroidView(
                factory = {previewView},
                Modifier
                    .padding(horizontal = 32.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(PrimaryColor)
                    .constrainAs(cameraPreview) {
                        top.linkTo(flashSection.bottom, 0.dp)
                        bottom.linkTo(bottomControls.top, 0.dp)
                        height = Dimension.fillToConstraints
                    }
                    .layoutId("cameraPreview")
            ){
                if(allPermissionGranted()){
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    startCamera()
                }
                else{
                    ActivityCompat.requestPermissions(activity,Constants.REQUIRED_PERMISSIONS,Constants.REQUEST_CODE_PERMISSIONS)
                }
            }

            BottomControls(
                Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(SecondaryColor)
                    .constrainAs(bottomControls) {
                        bottom.linkTo(parent.bottom, 0.dp)
                    }
                    .layoutId("cameraBottomControls")
            )
        }
    }

    @Composable
    fun FlashSection(modifier: Modifier) {
        Box(modifier = modifier){
            Text(text = "Flash",
                modifier = Modifier
                    .align(Alignment.Center),
                color = PrimaryColor,
                fontSize = 24.sp
            )
        }
    }

    @Composable
    fun BottomControls(modifier: Modifier) {

        ConstraintLayout(modifier = modifier) {

            val context = LocalContext.current
            val (btnCapture,switchCamera,btnPhotos) = createRefs()
            createHorizontalChain(switchCamera,btnCapture,btnPhotos)

            Image(
                painterResource(id = R.drawable.switch_camera_icon),
                "Switch Camera",
                modifier = Modifier
                    .constrainAs(switchCamera) {
                        top.linkTo(parent.top, 0.dp)
                        bottom.linkTo(parent.bottom, 16.dp)
                        height = Dimension.fillToConstraints
                    }
                    .clickable {
                        Constants.cameraSelector =
                            if (Constants.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            } else {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            }
                        startCamera()
                    }
            )

            Image(
                painterResource(id = R.drawable.button_capture),
                "Capture Image",
                modifier = Modifier
                    .constrainAs(btnCapture) {
                        top.linkTo(parent.top, 0.dp)
                        bottom.linkTo(parent.bottom, 16.dp)
                        height = Dimension.fillToConstraints
                    }
                    .layoutId("btnCapture")
                    .clickable {
                        takePicture()
                    },
            )

            Image(
                painterResource(id = R.drawable.switch_camera_icon),
                "See All Photos",
                modifier = Modifier
                    .constrainAs(btnPhotos) {
                        top.linkTo(parent.top, 0.dp)
                        bottom.linkTo(parent.bottom, 16.dp)
                        height = Dimension.fillToConstraints
                    }
                    .layoutId("btnPhotos")
            )

        }

    }

    private fun startCamera() {

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = androidx.camera.core.Preview.Builder().build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            try{
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner,
                    Constants.cameraSelector,preview,imageCapture)
            }catch (e: Exception){
                Log.d("Exception","Camera Start Fail ${e.message}")
            }

        }, ContextCompat.getMainExecutor(this))

    }

    private fun takePicture() {

        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val encryptedPref = EncryptedSharedPreferences.create(
            Constants.ENCRYPTED_SHARED_PREF_NAME,
            masterKey,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
        encryptedPref.edit()
            .putString("password","test_practice_te")
            .apply()

        if(imageCapture!=null){
            imageCapture?.takePicture(ContextCompat.getMainExecutor(this),object: ImageCapture.OnImageCapturedCallback(){
                override fun onCaptureSuccess(image: ImageProxy) {
                    try{
                        val buffer = image.planes[0].buffer
                        val byteArray = ByteArray(buffer.remaining())
                        buffer.get(byteArray)

                        val password = encryptedPref.getString("password","").toString()
                        val secretKey = SecretKeySpec(password.toByteArray(),"AES")
                        val iv = IvParameterSpec(password.toByteArray())
                        val encryptedArray:ByteArray
                        Cipher.getInstance("AES/CTR/PKCS5Padding").run {
                            init(Cipher.ENCRYPT_MODE, secretKey, iv)
                            encryptedArray = doFinal(byteArray)
                            val outputFile = File(
                                externalMediaDirs.firstOrNull(),
                                "Encrypted ${System.currentTimeMillis()}.txt"
                            )
                            Log.d("Debug","Success")
                            FileOutputStream(outputFile.path).run {
                                write(encryptedArray)
                                releaseInstance()
                                close()
                            }
                        }

                    }catch (e: Exception){
                        Log.d("Exception","Crash ${e.message}")
                    }

                    image.close()

                }

                override fun onError(exception: ImageCaptureException) {
                    Log.d("Exception","Take Picture Error: ${exception.message}")
                }
            })
        }

    }

}