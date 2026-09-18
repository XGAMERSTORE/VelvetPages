package com.xgqrscanner.modern

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.zxing.BarcodeFormat
import org.json.JSONArray
import org.json.JSONObject

data class ScanRecord(
    val value: String,
    val format: String,
    val timestamp: Long
)

object HistoryStore {
    private const val PREFS = "qr_scanner_prefs"
    private const val KEY = "scan_history"
    private const val MAX_ITEMS = 100

    fun load(context: Context): List<ScanRecord> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(
                        ScanRecord(
                            value = item.optString("value"),
                            format = item.optString("format", "CODE"),
                            timestamp = item.optLong("timestamp")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun add(context: Context, record: ScanRecord): List<ScanRecord> {
        val updated = (listOf(record) + load(context)
            .filterNot { it.value == record.value && record.timestamp - it.timestamp < 2500L })
            .take(MAX_ITEMS)
        save(context, updated)
        return updated
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY)
            .apply()
    }

    private fun save(context: Context, records: List<ScanRecord>) {
        val array = JSONArray()
        records.forEach {
            array.put(
                JSONObject()
                    .put("value", it.value)
                    .put("format", it.format)
                    .put("timestamp", it.timestamp)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, array.toString())
            .apply()
    }
}

fun barcodeFormatName(format: Int): String = when (format) {
    Barcode.FORMAT_QR_CODE -> "QR"
    Barcode.FORMAT_DATA_MATRIX -> "DATA MATRIX"
    Barcode.FORMAT_AZTEC -> "AZTEC"
    Barcode.FORMAT_PDF417 -> "PDF417"
    Barcode.FORMAT_CODE_128 -> "CODE 128"
    Barcode.FORMAT_CODE_39 -> "CODE 39"
    Barcode.FORMAT_CODE_93 -> "CODE 93"
    Barcode.FORMAT_CODABAR -> "CODABAR"
    Barcode.FORMAT_EAN_13 -> "EAN-13"
    Barcode.FORMAT_EAN_8 -> "EAN-8"
    Barcode.FORMAT_ITF -> "ITF"
    Barcode.FORMAT_UPC_A -> "UPC-A"
    Barcode.FORMAT_UPC_E -> "UPC-E"
    else -> "CODE"
}

enum class GenerateFormat(
    val label: String,
    val zxing: BarcodeFormat,
    val square: Boolean
) {
    QR("QR Code", BarcodeFormat.QR_CODE, true),
    DATA_MATRIX("Data Matrix", BarcodeFormat.DATA_MATRIX, true),
    AZTEC("Aztec", BarcodeFormat.AZTEC, true),
    CODE_128("Code 128", BarcodeFormat.CODE_128, false),
    CODE_39("Code 39", BarcodeFormat.CODE_39, false),
    EAN_13("EAN-13", BarcodeFormat.EAN_13, false)
}
