import 'dart:convert';

import '../../models/app_models.dart';
import 'rules_runtime.dart';

const String lbDefaultAppPresentationKey = '__default__';

class LbAppPresentationOverridesState {
  const LbAppPresentationOverridesState({
    this.defaultOverride = const AppPresentationOverride(),
    this.packageOverrides = const <String, AppPresentationOverride>{},
  });

  final AppPresentationOverride defaultOverride;
  final Map<String, AppPresentationOverride> packageOverrides;

  AppPresentationOverride resolve(String packageName) {
    return packageOverrides[lbNormalizePackageName(packageName)] ??
        defaultOverride;
  }
}

LbAppPresentationOverridesState lbParseAppPresentationOverrides(String raw) {
  final String normalized = raw.trim();
  if (normalized.isEmpty) {
    return const LbAppPresentationOverridesState();
  }

  final dynamic decoded = jsonDecode(normalized);
  if (decoded is! Map) {
    return const LbAppPresentationOverridesState();
  }

  AppPresentationOverride defaultOverride = const AppPresentationOverride();
  if (decoded[lbDefaultAppPresentationKey] is Map) {
    defaultOverride = AppPresentationOverride.fromJsonEntry(
      Map<String, dynamic>.from(decoded[lbDefaultAppPresentationKey] as Map),
    );
  }

  final Map<String, AppPresentationOverride> overrides =
      <String, AppPresentationOverride>{};
  for (final MapEntry<dynamic, dynamic> entry in decoded.entries) {
    final String packageName = lbNormalizePackageName(entry.key as String? ?? '');
    if (packageName.isEmpty ||
        packageName == lbDefaultAppPresentationKey ||
        entry.value is! Map) {
      continue;
    }

    final AppPresentationOverride value = AppPresentationOverride.fromJsonEntry(
      Map<String, dynamic>.from(entry.value as Map),
    );
    if (!_lbOverridesEqual(value, defaultOverride)) {
      overrides[packageName] = value;
    }
  }

  return LbAppPresentationOverridesState(
    defaultOverride: defaultOverride,
    packageOverrides: overrides,
  );
}

String lbEncodeAppPresentationOverrides(LbAppPresentationOverridesState state) {
  final Map<String, dynamic> payload = <String, dynamic>{};
  if (!state.defaultOverride.isDefault) {
    payload[lbDefaultAppPresentationKey] = state.defaultOverride.toJsonEntry();
  }

  final List<String> sortedKeys = state.packageOverrides.keys.toList()..sort();
  for (final String packageName in sortedKeys) {
    final AppPresentationOverride? value = state.packageOverrides[packageName];
    if (value == null || _lbOverridesEqual(value, state.defaultOverride)) {
      continue;
    }
    payload[packageName] = value.toJsonEntry();
  }

  return jsonEncode(payload);
}

bool _lbOverridesEqual(
  AppPresentationOverride left,
  AppPresentationOverride right,
) {
  return left.compactTextSource == right.compactTextSource &&
      left.iconSource == right.iconSource &&
      left.effectiveTitleSource == right.effectiveTitleSource &&
      left.effectiveContentSource == right.effectiveContentSource &&
      left.removeOriginalMessage == right.removeOriginalMessage;
}
