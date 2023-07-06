package com.android.settings.deviceinfo.aboutphone

import android.content.Context
import android.os.Build
import android.os.SystemProperties
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import android.widget.*

import com.android.settings.R

import androidx.appcompat.app.AlertDialog
import android.app.usage.StorageStatsManager
import android.os.storage.StorageManager
import android.os.storage.VolumeInfo
import android.text.format.Formatter
import java.io.IOException
import android.util.Log

typealias onDeviceChanged = ((deviceName: String) -> Unit)?

class AboutDevice : FrameLayout {

    private var listener: onDeviceChanged = null
    private var freeBytes: Long = 0
    private var usedBytes: Long = 0
    private var totalBytes: Long = 0
    var used: String? = null
    var total: String? = null
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        inflate(context, R.layout.device_info, this)
        setStorageText(manageStorageInfo())

        // Device
        var mDeviceName = Settings.Global.getString(
            context.contentResolver,
            Settings.Global.DEVICE_NAME
        )
        if (mDeviceName == null) {
            mDeviceName = Build.MODEL
        }
        findViewById<LinearLayout>(R.id.deviceEdit).setOnClickListener {
            val alert: AlertDialog.Builder = AlertDialog.Builder(context, R.style.Theme_AlertDialog)
            val dialogView: View = View.inflate(context, R.layout.litmus_device_name_dialog, null)
            val mEditText: EditText = dialogView.findViewById(R.id.device_edit_text)
            alert.setTitle(context.getString(R.string.my_device_info_device_name_preference_title))
            alert.setView(dialogView)
            alert.setPositiveButton(android.R.string.ok) { dialog, _ ->
                listener?.invoke(mEditText.text.toString())
                dialog.dismiss()
            }
            alert.setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            // Update device name before showing dialog
            mDeviceName = Settings.Global.getString(
                context.contentResolver,
                Settings.Global.DEVICE_NAME
            )
            if (mDeviceName == null) {
                mDeviceName = Build.MODEL
            }
            mEditText.setText(mDeviceName)
            alert.show()
        }
    }

    fun setListener(listener: onDeviceChanged) {
        this.listener = listener
    }

    fun setDeviceName(deviceName: String, validator: Boolean) {
        if (validator) {
            findViewById<TextView>(R.id.deviceName).text = deviceName
            listener?.invoke(deviceName)
        }
    }

    fun setDeviceName(deviceName: String) {
        findViewById<TextView>(R.id.deviceName).text = deviceName
    }

    fun setModelName(modelName: String?) {
        findViewById<TextView>(R.id.device_model).text = modelName
    }
    fun setStorageText(storagetext: String?){
        findViewById<TextView>(R.id.storage_value).text = storagetext
    }
    private fun manageStorageInfo(): String {
        val storageManager: StorageManager? = context.getSystemService(StorageManager::class.java)
        if (storageManager != null) {
            val volumes = storageManager.volumes
            for (vol in volumes) {
                val path = vol.getPath()
                if (vol.isMountedReadable) {
                    if (vol.getType() == VolumeInfo.TYPE_PRIVATE) {
                        val stats = context.getSystemService(StorageStatsManager::class.java)
                        try {
                            totalBytes = stats.getTotalBytes(vol.getFsUuid())
                            freeBytes = stats.getFreeBytes(vol.getFsUuid())
                            usedBytes = totalBytes - freeBytes
                        } catch (e: IOException) {
                            Log.w("StorageManager", e)
                        }
                    }
                    used = Formatter.formatFileSize(context, usedBytes, Formatter.FLAG_SHORTER)
                    total =
                        Formatter.formatFileSize(context, totalBytes, Formatter.FLAG_SHORTER)
                }
            }
        }
        return used + String.format("/%s", total)
    }
}
