package com.smartconfig

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.SupplicantState
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.WritableNativeMap
import com.facebook.react.bridge.WritableMap
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener
import com.espressif.iot.esptouch.EsptouchTask
import com.espressif.iot.esptouch.IEsptouchListener
import com.espressif.iot.esptouch.IEsptouchResult
import com.espressif.iot.esptouch.IEsptouchTask
import com.espressif.iot.esptouch.util.TouchNetUtil
import java.net.InetAddress

@ReactModule(name = SmartconfigModule.NAME)
class SmartconfigModule(reactContext: ReactApplicationContext) :
  NativeSmartconfigSpec(reactContext), PermissionListener {

  private val context: ReactApplicationContext = reactContext
  private var espPromise: Promise? = null
  private var checkPromise: Promise? = null
  private var mEsptouchTask: IEsptouchTask? = null
  private var alertDialog: AlertDialog? = null

  override fun getName(): String = NAME

  private fun getPermissionAwareActivity(): PermissionAwareActivity {
    val activity: Activity = currentActivity
      ?: throw IllegalStateException("Not attached to an Activity.")
    if (activity !is PermissionAwareActivity) {
      throw IllegalStateException("Host Activity does not implement PermissionAwareActivity.")
    }
    return activity
  }

  override fun checkLocation(promise: Promise) {
    checkPromise = promise
    val ctx: Context = reactApplicationContext.baseContext
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED
      ) {
        requestLocationPermission()
      } else {
        isConnected()
      }
    } else {
      promise.resolve("isConnected")
    }
  }

  private fun requestLocationPermission() {
    val activity = getPermissionAwareActivity()
    val messagePermission =
      "App has no access to location.\nPlease allow access to your location in settings."
    val onCancel = DialogInterface.OnClickListener { _, _ ->
      alertDialog = null
      checkPromise?.reject("NOT_GRANTED")
    }
    val onOk = DialogInterface.OnClickListener { _, _ ->
      activity.requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION, this)
    }

    if (activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
      val builder = AlertDialog.Builder(currentActivity)
        .setMessage(messagePermission)
        .setPositiveButton("Go to settings", onOk)
        .setNegativeButton("Later", onCancel)

      if (alertDialog == null) {
        alertDialog = builder.show()
      }
    } else {
      activity.requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION, this)
    }
  }

  private fun isConnected() {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val gps = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
    val network = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    if (gps || network) {
      checkPromise?.resolve("isConnected")
    } else {
      checkPromise?.reject("NotConnected")
    }
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ): Boolean {
    if (requestCode == REQUEST_LOCATION) {
      if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        isConnected()
      } else {
        checkPromise?.reject("NOT_GRANTED")
      }
    }
    return true
  }

  override fun getConnectedInfo(promise: Promise) {
    val result: WritableMap = WritableNativeMap()
    val wm = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val wifiInfo: WifiInfo = wm.connectionInfo
    try {
      if (!TouchNetUtil.isWifiConnected(wifiInfo)) {
        promise.reject("NotConnected")
        return
      }
      if (wifiInfo.supplicantState != SupplicantState.COMPLETED) {
        promise.reject("Connecting")
        return
      }
      val ssid = TouchNetUtil.getSsidString(wifiInfo)
      val ipValue = wifiInfo.ipAddress
      var ip: InetAddress? = if (ipValue != 0) {
        TouchNetUtil.getAddress(ipValue)
      } else {
        TouchNetUtil.getIPv4Address() ?: TouchNetUtil.getIPv6Address()
      }
      val ipAddress = ip?.hostAddress ?: ""
      result.putString("ip", ipAddress)
      result.putBoolean("is5G", TouchNetUtil.is5G(wifiInfo.frequency))
      result.putString("ssid", ssid)
      result.putString("bssid", wifiInfo.bssid)
      result.putString("state", "Connected")
      promise.resolve(result)
    } catch (e: Exception) {
      Log.e(TAG, "unexpected exception", e)
      promise.reject("ERROR", e)
    }
  }

  override fun startEspTouch(apSsid: String, apBssid: String, apPassword: String, promise: Promise) {
    espPromise = promise
    val mApSsid = apSsid.toByteArray()
    val mApPassword = apPassword.toByteArray()
    val deviceCountData = "1".toByteArray()
    val broadcastData = "1".toByteArray()
    val taskResultCount =
      if (deviceCountData.isEmpty()) -1 else String(deviceCountData).toInt()
    mEsptouchTask = EsptouchTask(mApSsid, ByteArray(6), mApPassword, context)
    mEsptouchTask?.setPackageBroadcast(broadcastData[0].toInt() == 1)
    mEsptouchTask?.setEsptouchListener(myListener)

    Thread {
      val resultList: List<IEsptouchResult> = mEsptouchTask!!.executeForResults(taskResultCount)
      val firstResult = resultList[0]
      if (!firstResult.isCancelled && !firstResult.isSuc) {
        espPromise?.reject("No Device Found")
      }
    }.start()
  }

  override fun stopEspTouch(promise: Promise) {
    mEsptouchTask?.interrupt()
    promise.resolve("OK")
  }

  private val myListener = IEsptouchListener { result ->
    if (result.isSuc) {
      espPromise?.resolve(result.bssid)
      Log.e(TAG, "onEsptouchResultAdded")
    }
  }

  companion object {
    const val NAME = "Smartconfig"
    private const val REQUEST_LOCATION = 1503
    private const val TAG = "wifi"
  }
}