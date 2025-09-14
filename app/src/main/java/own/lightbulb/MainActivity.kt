package own.lightbulb

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException


class MainActivity : AppCompatActivity() {
    private var camera: Camera? = null
    private var cameraflash = false
    private var lighton = false
    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    private var previewView: PreviewView? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var isBind = false
    private var isFirstStart = true
    private var lightbutton: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        //transparent status bars
        val w = getWindow()
        w.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        enableEdgeToEdge()
        checkPermission()

        previewView = findViewById(R.id.previewView)
        lightbutton = findViewById(R.id.light)


        lightbutton?.setOnClickListener() {
            if (isBind) {
                if (cameraflash) {
                    if (!lighton) {
                        camera?.cameraControl?.enableTorch(true)
                        lightbutton!!.setImageResource(R.drawable.lightbulb_on_sm)
                        lighton = true
                    } else {
                        camera?.cameraControl?.enableTorch(false)
                        lightbutton!!.setImageResource(R.drawable.lightbulb_off_sm)
                        lighton = false
                    }
                }
            }

        }

    }

    protected override fun onStart() {
        super.onStart()
        if (isFirstStart) {
            isFirstStart = false
        } else {
            if (!isBind) {
                bindPreview()
            }
        }
    }

    protected override fun onStop() {
        super.onStop()
        if (isBind) {
            unbind()
        }
    }

    protected override fun onDestroy() {
        super.onDestroy()
    }

    private fun unbind() {
        cameraProvider!!.unbindAll()
        isBind = false
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf<String>(Manifest.permission.CAMERA),
                PERMISSIONS_REQUEST_CAMERA
            )
        } else {
            initCameraProviderFuture()
        }
    }

    public override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CAMERA) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initCameraProviderFuture()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Camera permission is required to take a photo.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun initCameraProviderFuture() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture!!.addListener(object : Runnable {
            override fun run() {
                try {
                    cameraProvider = cameraProviderFuture!!.get()
                    bindPreview()
                } catch (e: ExecutionException) {
                } catch (e: InterruptedException) {
                }
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindPreview() {
        val aspectRatioStrategy = AspectRatioStrategy(AspectRatio.RATIO_16_9, AspectRatioStrategy.FALLBACK_RULE_AUTO)
        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(aspectRatioStrategy)
            .build()
        val preview = Preview.Builder().setResolutionSelector(resolutionSelector).build()
        previewView!!.setScaleType(PreviewView.ScaleType.FILL_CENTER)

        previewView
        preview.setSurfaceProvider(previewView!!.getSurfaceProvider())

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        camera = cameraProvider?.bindToLifecycle(this, cameraSelector, preview)

        isBind = true

        cameraflash = camera?.cameraInfo?.hasFlashUnit()!!
        Log.i("Lightbulb", "cameraFlash: $cameraflash")


    }



    companion object {
        const val PERMISSIONS_REQUEST_CAMERA: Int = 100
    }
}