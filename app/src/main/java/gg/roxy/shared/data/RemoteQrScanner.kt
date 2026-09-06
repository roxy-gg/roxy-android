package gg.roxy.shared.data

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

interface RemoteQrScanner {
    fun startScan(
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit = {},
        onCanceled: () -> Unit = {},
    )
}

class PlayServicesRemoteQrScanner(
    private val context: Context,
) : RemoteQrScanner {
    override fun startScan(
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit,
        onCanceled: () -> Unit,
    ) {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()

        val scanner = GmsBarcodeScanning.getClient(context, options)
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val raw = barcode.rawValue
                if (!raw.isNullOrBlank()) {
                    onSuccess(raw)
                } else {
                    onFailure(IllegalStateException("No content found in QR code"))
                }
            }
            .addOnCanceledListener {
                onCanceled()
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
}
