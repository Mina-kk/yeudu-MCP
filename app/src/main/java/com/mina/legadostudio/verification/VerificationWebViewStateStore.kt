package com.mina.legadostudio.verification

import android.content.Context
import android.os.Bundle
import android.os.Parcel
import android.webkit.WebView
import java.io.File

class VerificationWebViewStateStore(private val context: Context) {
    private val dir = File(context.filesDir, "verification-webview-state").apply { mkdirs() }

    fun save(id: String, view: WebView) {
        runCatching {
            val bundle = Bundle()
            view.saveState(bundle)
            val parcel = Parcel.obtain()
            try {
                bundle.writeToParcel(parcel, 0)
                File(dir, "$id.bin").writeBytes(parcel.marshall())
            } finally { parcel.recycle() }
        }
    }

    fun restore(id: String, view: WebView): Boolean = runCatching {
        val file = File(dir, "$id.bin")
        if (!file.isFile) return false
        val parcel = Parcel.obtain()
        try {
            val bytes = file.readBytes()
            parcel.unmarshall(bytes, 0, bytes.size)
            parcel.setDataPosition(0)
            val bundle = parcel.readBundle(context.classLoader) ?: return false
            view.restoreState(bundle) != null
        } finally { parcel.recycle() }
    }.getOrDefault(false)

    fun clear(id: String) { File(dir, "$id.bin").delete() }
}
