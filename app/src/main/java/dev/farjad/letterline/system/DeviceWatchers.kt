package dev.farjad.letterline.system

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Flashlight state, for the "flashlight is on" row on home. Needs no permission. */
class TorchWatcher(context: Context) {
    private val cameras = context.getSystemService(CameraManager::class.java)
    private val lit = HashSet<String>()
    private val _on = MutableStateFlow(false)
    val on: StateFlow<Boolean> = _on

    fun start() {
        runCatching {
            cameras.registerTorchCallback(
                object : CameraManager.TorchCallback() {
                    override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                        if (enabled) lit += cameraId else lit -= cameraId
                        _on.value = lit.isNotEmpty()
                    }

                    override fun onTorchModeUnavailable(cameraId: String) {
                        lit -= cameraId
                        _on.value = lit.isNotEmpty()
                    }
                },
                Handler(Looper.getMainLooper()),
            )
        }
    }

    fun turnOff() {
        lit.toList().forEach { id -> runCatching { cameras.setTorchMode(id, false) } }
    }
}

/**
 * Plug and Play: true while headphones (wired, USB or Bluetooth) are connected, plus the
 * packages that play music, so home can offer them.
 */
class HeadphoneWatcher(private val context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected

    private val headphoneTypes = buildSet {
        add(AudioDeviceInfo.TYPE_WIRED_HEADSET)
        add(AudioDeviceInfo.TYPE_WIRED_HEADPHONES)
        add(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
        add(AudioDeviceInfo.TYPE_USB_HEADSET)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(AudioDeviceInfo.TYPE_BLE_HEADSET)
    }

    fun start() {
        refresh()
        audio.registerAudioDeviceCallback(
            object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = refresh()
                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = refresh()
            },
            Handler(Looper.getMainLooper()),
        )
    }

    private fun refresh() {
        _connected.value = runCatching {
            audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { it.type in headphoneTypes }
        }.getOrDefault(false)
    }

    /** Apps that call themselves music players, or offer a media browser (most streaming apps). */
    fun musicPackages(): Set<String> {
        val pm = context.packageManager
        val players = runCatching {
            pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC), 0)
                .map { it.activityInfo.packageName }
        }.getOrDefault(emptyList())
        val legacy = runCatching {
            pm.queryIntentActivities(Intent(MediaStore.INTENT_ACTION_MUSIC_PLAYER), 0).map { it.activityInfo.packageName }
        }.getOrDefault(emptyList())
        val browsers = runCatching {
            pm.queryIntentServices(Intent("android.media.browse.MediaBrowserService"), 0).map { it.serviceInfo.packageName }
        }.getOrDefault(emptyList())
        return (players + legacy + browsers).toSet()
    }
}
