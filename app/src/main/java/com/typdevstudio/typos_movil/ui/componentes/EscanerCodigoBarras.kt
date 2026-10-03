package com.typdevstudio.typos_movil.ui.componentes

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.typdevstudio.typos_movil.ui.theme.AzulPrimario
import com.typdevstudio.typos_movil.ui.theme.Blanco
import java.util.concurrent.Executors

@Composable
fun DialogoEscanerCodigoBarras(
    mostrar: Boolean,
    alDetectarCodigo: (String) -> Unit,
    alCerrar: () -> Unit
) {
    if (!mostrar) return

    val contexto = LocalContext.current
    var tienePermisoCamara by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                contexto,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcherPermiso = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido ->
        tienePermisoCamara = concedido
    }

    LaunchedEffect(Unit) {
        if (!tienePermisoCamara) {
            launcherPermiso.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(
        onDismissRequest = alCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (tienePermisoCamara) {
                CamaraEscanerVista(
                    alDetectarCodigo = { codigo ->
                        alDetectarCodigo(codigo)
                        alCerrar()
                    }
                )

                // Marco guía para apuntar el código de barras
                Box(
                    modifier = Modifier
                        .size(width = 280.dp, height = 180.dp)
                        .align(Alignment.Center)
                        .border(
                            width = 3.dp,
                            color = AzulPrimario,
                            shape = RoundedCornerShape(16.dp)
                        )
                )

                // Texto informativo superior
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 48.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Apunta la cámara al código de barras",
                        color = Blanco,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "El escaneo se realizará automáticamente",
                        color = Blanco.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Blanco)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Se requiere permiso de cámara",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AzulPrimario
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Para escanear códigos de barra rápidamente es necesario conceder el permiso de acceso a la cámara.",
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        BotonPos(
                            texto = "Conceder Permiso",
                            alHacerClic = { launcherPermiso.launch(Manifest.permission.CAMERA) }
                        )
                    }
                }
            }

            // Botón flotante para cerrar
            IconButton(
                onClick = alCerrar,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 40.dp, end = 16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(50))
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Cerrar cámara",
                    tint = Blanco
                )
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CamaraEscanerVista(
    alDetectarCodigo: (String) -> Unit
) {
    val contexto = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val ejecutorCamara = remember { Executors.newSingleThreadExecutor() }
    var escaneoProcesado by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            ejecutorCamara.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val escanerMlKit = BarcodeScanning.getClient()

                val analisisImagen = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analisisImagen.setAnalyzer(ejecutorCamara) { imageProxy ->
                    procesarImagen(imageProxy, escanerMlKit) { codigo ->
                        if (!escaneoProcesado) {
                            escaneoProcesado = true
                            alDetectarCodigo(codigo)
                        }
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        analisisImagen
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@OptIn(ExperimentalGetImage::class)
private fun procesarImagen(
    imageProxy: ImageProxy,
    escaner: com.google.mlkit.vision.barcode.BarcodeScanner,
    alDetectar: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val imagen = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        escaner.process(imagen)
            .addOnSuccessListener { codigos ->
                for (codigo in codigos) {
                    val valor = codigo.rawValue ?: codigo.displayValue
                    if (!valor.isNullOrBlank()) {
                        alDetectar(valor)
                        break
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}
