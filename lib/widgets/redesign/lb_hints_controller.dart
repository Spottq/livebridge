import 'dart:async';

import 'package:flutter/foundation.dart';

import '../../platform/livebridge_platform.dart';

class LbHintsController {
  const LbHintsController._();

  static final ValueNotifier<bool> disabled = ValueNotifier<bool>(false);
  static bool _loaded = false;

  static Future<void> load() async {
    if (_loaded) {
      return;
    }
    _loaded = true;
    try {
      disabled.value = await LiveBridgePlatform.getHintsDisabled();
    } catch (_) {}
  }

  static void updateLocal(bool value) {
    _loaded = true;
    if (disabled.value != value) {
      disabled.value = value;
    }
  }

  static Future<void> setDisabled(bool value) async {
    updateLocal(value);
    try {
      await LiveBridgePlatform.setHintsDisabled(value);
    } catch (_) {}
  }
}
