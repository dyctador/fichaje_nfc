package com.tuempresa.fichaje

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val TAG = "FichajeNFC"
    private lateinit var tvStatus: TextView
    private lateinit var tvLog: TextView

    // ---------- CONFIG: cambia aquí ----------
    private val WEBHOOK_URL = "https://TU_SERVIDOR.com/hit" // <-- pon aquí tu endpoint HTTPS
    private val DEVICE_ID = "casa_abuela_01" // identificador del dispositivo/ubicación
    // -----------------------------------------

    private var nfcAdapter: NfcAdapter? = null
    private val client = OkHttpClient()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvLog = findViewById(R.id.tvLog)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            showAlert("NFC no disponible", "Este dispositivo no tiene NFC.")
            tvStatus.text = "NFC no disponible en este dispositivo."
            return
        }

        if (!nfcAdapter!!.isEnabled) {
            // pedimos al usuario que active NFC
            AlertDialog.Builder(this)
                .setTitle("NFC desactivado")
                .setMessage("El NFC está desactivado. ¿Quieres ir a ajustes para activarlo?")
                .setPositiveButton("Ir a ajustes") { _, _ ->
                    startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                }
                .setNegativeButton("Cerrar", null)
                .show()
        }

        // Si la app se ha lanzado por un intent NFC, procesarlo
        handleIntentIfNfc(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntentIfNfc(it) }
    }

    private fun handleIntentIfNfc(intent: Intent) {
        val action = intent.action
        if (action == NfcAdapter.ACTION_TAG_DISCOVERED ||
            action == NfcAdapter.ACTION_TECH_DISCOVERED ||
            action == NfcAdapter.ACTION_NDEF_DISCOVERED) {

            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            if (tag != null) {
                val idBytes = tag.id
                val tagId = bytesToHex(idBytes)
                appendLog("Tag detectado: $tagId")
                val ndef = Ndef.get(tag)
                var ndefText: String? = null
                if (ndef != null) {
                    try {
                        val msg = ndef.cachedNdefMessage
                        if (msg != null && msg.records.isNotEmpty()) {
                            val rec = msg.records[0]
                            val payload = rec.payload
                            ndefText = String(payload)
                            appendLog("NDEF payload (raw): ${'$'}ndefText")
                        }
                    } catch (e: Exception) {
                        appendLog("Error leyendo NDEF: ${'$'}{e.message}")
                    }
                }
                sendHit(tagId, ndefText)
            } else {
                appendLog("Intent NFC sin Tag.")
            }
        }
    }

    private fun sendHit(tagId: String, ndefText: String?) {
        tvStatus.text = "Enviando chequeo..."
        val now = Date()
        val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).format(now)
        val local = SimpleDateFormat.getDateTimeInstance().format(now)

        val json = JSONObject()
        json.put("device", DEVICE_ID)
        json.put("tagId", tagId)
        json.put("ndef", if (ndefText != null) ndefText else JSONObject.NULL)
        json.put("timestamp_iso", iso)
        json.put("timestamp_local", local)

        scope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val body = json.toString().toRequestBody(mediaType)
                    val req = Request.Builder()
                        .url(WEBHOOK_URL)
                        .post(body)
                        .build()
                    val resp = client.newCall(req).execute()
                    val ok = resp.isSuccessful
                    resp.close()
                    ok
                } catch (e: Exception) {
                    appendLog("Error HTTP: ${'$'}{e.message}")
                    false
                }
            }

            if (success) {
                tvStatus.text = "Chequeo enviado ✅"
                appendLog("Enviado OK: ${'$'}local")
            } else {
                tvStatus.text = "Error enviando ❌"
                appendLog("Fallo al enviar. Reintenta o revisa el servidor.")
            }

            // volver al mensaje base tras unos segundos
            delay(3000)
            tvStatus.text = "Acerca la pegatina NFC para enviar un chequeo"
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02X", b))
        }
        return sb.toString()
    }

    private fun appendLog(msg: String) {
        runOnUiThread {
            val prev = tvLog.text.toString()
            val now = SimpleDateFormat.getTimeInstance().format(Date())
            tvLog.text = "$prev\n[$now] $msg"
        }
        Log.d(TAG, msg)
    }

    private fun showAlert(title: String, msg: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
