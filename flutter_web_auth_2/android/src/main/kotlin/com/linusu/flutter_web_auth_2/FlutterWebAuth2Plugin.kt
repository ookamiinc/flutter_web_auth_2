package com.linusu.flutter_web_auth_2

import android.content.Context
import android.content.Intent
import android.net.Uri

import androidx.browser.customtabs.CustomTabsIntent

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar

class FlutterWebAuth2Plugin(private var context: Context? = null, private var channel: MethodChannel? = null): MethodCallHandler, FlutterPlugin {
  companion object {
    val callbacks = mutableMapOf<String, Result>()
    var validSchemes: MutableList<String> = mutableListOf()

    @JvmStatic
    fun registerWith(registrar: Registrar) {
        val plugin = FlutterWebAuth2Plugin()
        plugin.initInstance(registrar.messenger(), registrar.context())
    }

  }

  fun initInstance(messenger: BinaryMessenger, context: Context) {
      this.context = context
      channel = MethodChannel(messenger, "flutter_web_auth_2")
      channel?.setMethodCallHandler(this)
  }

  override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
      initInstance(binding.binaryMessenger, binding.applicationContext)
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
      context = null
      channel = null
  }

  override fun onMethodCall(call: MethodCall, resultCallback: Result) {
    when (call.method) {
        "authenticate" -> {
          val url = Uri.parse(call.argument("url"))
          val callbackUrlSchemes = call.argument<List<String>>("callbackUrlSchemes") ?: emptyList()
          val options = call.argument<Map<String, Any>>("options")!!

          if (callbackUrlSchemes.isEmpty()) {
              resultCallback.error("INVALID_SCHEME", "No callbackUrlSchemes provided", null)
              return
          }

          callbackUrlSchemes.forEach { scheme ->
              if (callbacks.containsKey(scheme)) {
                  resultCallback.error("CANCELED", "Another authentication process is ongoing", null)
                  return
              }
          }

          callbackUrlSchemes.forEach { scheme ->
              callbacks[scheme] = resultCallback
          }
          validSchemes.addAll(callbackUrlSchemes)

          val intent = CustomTabsIntent.Builder().build()
          val keepAliveIntent = Intent(context, KeepAliveService::class.java)

          intent.intent.addFlags(options["intentFlags"] as Int)
          intent.intent.putExtra("android.support.customtabs.extra.KEEP_ALIVE", keepAliveIntent)

          intent.launchUrl(context!!, url)
        }
        "cleanUpDanglingCalls" -> {
          val canceledCallbacks = mutableListOf<Result>()

          callbacks.forEach { (scheme, danglingResultCallback) ->
              if (scheme !in validSchemes) {
                  canceledCallbacks.add(danglingResultCallback)
              }
          }

          callbacks.clear()
          validSchemes.clear()

          canceledCallbacks.firstOrNull()?.error("CANCELED", "User canceled login", null)

          resultCallback.success(null)
        }
        else -> resultCallback.notImplemented()
    }
  }
}
