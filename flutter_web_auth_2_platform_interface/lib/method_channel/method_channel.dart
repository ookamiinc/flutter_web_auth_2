import 'package:flutter/services.dart';
import 'package:flutter_web_auth_2_platform_interface/flutter_web_auth_2_platform_interface.dart';

/// Method channel implementation of the [FlutterWebAuth2Platform].
class FlutterWebAuth2MethodChannel extends FlutterWebAuth2Platform {
  static const MethodChannel channel = MethodChannel('flutter_web_auth_2');

  @override
  Future<String> authenticate({
    required String url,
    required List<String> callbackUrlSchemes,
    required Map<String, dynamic> options,
  }) async =>
      await channel.invokeMethod<String>('authenticate', <String, dynamic>{
        'url': url,
        'callbackUrlSchemes': callbackUrlSchemes,
        'options': options,
      }) ??
      '';

  @override
  Future clearAllDanglingCalls() async =>
      channel.invokeMethod('cleanUpDanglingCalls');
}
