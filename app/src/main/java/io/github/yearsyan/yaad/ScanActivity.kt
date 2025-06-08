package io.github.yearsyan.yaad

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.google.zxing.MultiFormatReader
import com.kongzue.dialogx.dialogs.PopTip
import io.github.yearsyan.yaad.ui.components.QRScanOverlay
import io.github.yearsyan.yaad.utils.decodeQRCode
import io.github.yearsyan.yaad.utils.yuvToBitmap
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ScanActivity : ComponentActivity() {

    private lateinit var textureView: TextureView
    private lateinit var cameraManager: CameraManager
    private var cameraDevice: CameraDevice? = null
    private var imageReader: ImageReader? = null
    private var captureSession: CameraCaptureSession? = null
    private var multiFormatReader: MultiFormatReader = MultiFormatReader()
    private val cameraReqCode = 1
    private var decodeJob: Job? = null
    private var needDecode = true
    private var previewSurface: Surface? = null

    private val cameraStateCallback =
        object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                startPreview()
            }

            override fun onDisconnected(camera: CameraDevice) {
                camera.close()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                camera.close()
                Toast.makeText(
                        this@ScanActivity,
                        "Camera error",
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }
        setContentView(R.layout.activity_scan)
        textureView = findViewById(R.id.scan_texture)
        findViewById<ComposeView>(R.id.compose_overlay).setContent {
            Scaffold(
                modifier = Modifier.background(Color.Transparent),
                containerColor = Color.Transparent,
            ) { innerPadding ->
                QRScanOverlay(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    onGallerySelected = { bitmap ->
                        val res = decodeQRCode(multiFormatReader, bitmap)
                        if (res == null) {
                            PopTip.show("not found")
                        } else {
                            handleResult(res)
                        }
                    },
                    onBackClick = { finish() },
                    onToggleFlash = { flashOn ->
                        val requestBuilder =
                            createRequest(flashOn) ?: return@QRScanOverlay false
                        captureSession?.setRepeatingRequest(
                            requestBuilder.build(),
                            null,
                            null
                        ) ?: false
                        return@QRScanOverlay true
                    }
                )
            }
        }
        cameraManager =
            getSystemService(Context.CAMERA_SERVICE) as CameraManager
        textureView.surfaceTextureListener = surfaceTextureListener
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private val surfaceTextureListener =
        object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surfaceTexture: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                if (checkCameraPermission()) {
                    openCamera()
                } else {
                    ActivityCompat.requestPermissions(
                        this@ScanActivity,
                        arrayOf(Manifest.permission.CAMERA),
                        cameraReqCode
                    )
                }
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {}

            override fun onSurfaceTextureDestroyed(
                surface: SurfaceTexture
            ): Boolean = true

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }

    private fun openCamera() {
        try {
            val cameraId =
                cameraManager.cameraIdList.first { id ->
                    val characteristics =
                        cameraManager.getCameraCharacteristics(id)
                    val lensFacing =
                        characteristics.get(CameraCharacteristics.LENS_FACING)
                    lensFacing == CameraCharacteristics.LENS_FACING_BACK
                }

            if (
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            cameraManager.openCamera(cameraId, cameraStateCallback, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun configureTransform(
        viewWidth: Int,
        viewHeight: Int,
        previewSize: Size
    ) {
        if (viewWidth == 0 || viewHeight == 0) return

        val matrix = Matrix()
        val viewRect =
            android.graphics.RectF(
                0f,
                0f,
                viewWidth.toFloat(),
                viewHeight.toFloat()
            )
        val bufferRect =
            android.graphics.RectF(
                0f,
                0f,
                previewSize.height.toFloat(),
                previewSize.width.toFloat()
            )
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()

        bufferRect.offset(
            centerX - bufferRect.centerX(),
            centerY - bufferRect.centerY()
        )
        matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)

        val scale =
            maxOf(
                viewWidth.toFloat() / previewSize.height,
                viewHeight.toFloat() / previewSize.width
            )
        matrix.postScale(scale, scale, centerX, centerY)

        textureView.setTransform(matrix)
    }

    private fun createRequest(flashOn: Boolean): CaptureRequest.Builder? {
        val imageSurface = imageReader?.surface ?: return null
        val previewRequestBuilder =
            cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                ?: return null
        val localPreviewSurface = previewSurface ?: return null
        previewRequestBuilder.addTarget(localPreviewSurface)
        previewRequestBuilder.addTarget(imageSurface)
        if (flashOn) {
            previewRequestBuilder.set(
                CaptureRequest.FLASH_MODE,
                CameraMetadata.FLASH_MODE_TORCH
            )
        }
        return previewRequestBuilder
    }

    private fun startPreview() {
        val cameraDev = cameraDevice ?: return
        val surfaceTexture = textureView.surfaceTexture ?: return
        previewSurface = Surface(surfaceTexture)
        val characteristics =
            cameraManager.getCameraCharacteristics(cameraDev.id)
        val configMap =
            characteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
            )
        val previewSizes =
            configMap?.getOutputSizes(SurfaceTexture::class.java) ?: return

        val viewRatio = textureView.width.toFloat() / textureView.height
        val bestSize =
            previewSizes.minByOrNull {
                val ratio = it.width.toFloat() / it.height
                abs(ratio - viewRatio)
            } ?: previewSizes[0]

        surfaceTexture.setDefaultBufferSize(bestSize.width, bestSize.height)
        configureTransform(textureView.width, textureView.height, bestSize)
        imageReader =
            ImageReader.newInstance(
                bestSize.width,
                bestSize.height,
                ImageFormat.YUV_420_888,
                2
            )
        val imageSurface = imageReader?.surface ?: return
        imageReader?.setOnImageAvailableListener(
            { reader ->
                if (decodeJob?.isActive == true || !needDecode) {
                    reader.acquireLatestImage()?.close()
                    return@setOnImageAvailableListener
                }
                decodeJob =
                    lifecycleScope.launch(Dispatchers.Default) {
                        val image = reader.acquireLatestImage() ?: return@launch
                        val bitmap = yuvToBitmap(image)
                        image.close()
                        decodeQRCode(multiFormatReader, bitmap)?.let {
                            handleResult(it)
                        }
                    }
            },
            Handler(Looper.getMainLooper())
        )

        try {
            val previewRequestBuilder = createRequest(false) ?: return
            val surfaces = listOf(previewSurface, imageSurface)
            val stateCallback =
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        previewRequestBuilder.set(
                            CaptureRequest.CONTROL_MODE,
                            CameraMetadata.CONTROL_MODE_AUTO
                        )
                        session.setRepeatingRequest(
                            previewRequestBuilder.build(),
                            null,
                            null
                        )
                    }

                    override fun onConfigureFailed(
                        session: CameraCaptureSession
                    ) {
                        Toast.makeText(
                                this@ScanActivity,
                                "Capture session failed",
                                Toast.LENGTH_SHORT
                            )
                            .show()
                    }
                }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val outputConfigs = surfaces.map { OutputConfiguration(it!!) }
                val executor = Executors.newSingleThreadExecutor()
                val sessionConfig =
                    SessionConfiguration(
                        SessionConfiguration.SESSION_REGULAR,
                        outputConfigs,
                        executor,
                        stateCallback
                    )
                cameraDev.createCaptureSession(sessionConfig)
            } else {
                cameraDev.createCaptureSession(surfaces, stateCallback, null)
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults,
            deviceId
        )
        if (
            requestCode == cameraReqCode &&
                grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT)
                .show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        captureSession?.close()
        cameraDevice?.close()
        imageReader?.close()
    }

    private fun handleResult(res: String) {
        runOnUiThread { Toast.makeText(this, res, Toast.LENGTH_SHORT).show() }
    }
}
