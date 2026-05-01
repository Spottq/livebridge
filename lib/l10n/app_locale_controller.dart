import 'package:flutter/material.dart';

import '../platform/livebridge_platform.dart';

const String appLanguageSystemId = 'system';

class AppLanguageOption {
  const AppLanguageOption({
    required this.id,
    required this.label,
    required this.locale,
  });

  final String id;
  final String label;
  final Locale? locale;
}

const List<AppLanguageOption> appLanguageOptions = <AppLanguageOption>[
  AppLanguageOption(
    id: appLanguageSystemId,
    label: 'Auto',
    locale: null,
  ),
  AppLanguageOption(
    id: 'en',
    label: 'English',
    locale: Locale('en'),
  ),
  AppLanguageOption(
    id: 'ru',
    label: 'Русский',
    locale: Locale('ru'),
  ),
  AppLanguageOption(
    id: 'tr',
    label: 'Türkçe',
    locale: Locale('tr'),
  ),
  AppLanguageOption(
    id: 'pt-BR',
    label: 'Português (Brasil)',
    locale: Locale('pt', 'BR'),
  ),
  AppLanguageOption(
    id: 'zh-Hans',
    label: '简体中文',
    locale: Locale.fromSubtags(languageCode: 'zh', scriptCode: 'Hans'),
  ),
  AppLanguageOption(
    id: 'zh-Hant',
    label: '繁體中文',
    locale: Locale.fromSubtags(languageCode: 'zh', scriptCode: 'Hant'),
  ),
];

final ValueNotifier<Locale?> appLocaleOverrideNotifier =
    ValueNotifier<Locale?>(null);

Future<void> loadAppLocalePreference() async {
  try {
    final String languageId = await LiveBridgePlatform.getAppLanguageTag();
    appLocaleOverrideNotifier.value = localeForAppLanguageId(languageId);
  } catch (_) {
    appLocaleOverrideNotifier.value = null;
  }
}

Future<void> setAppLocalePreference(String languageId) async {
  final String normalized = normalizeAppLanguageId(languageId);
  await LiveBridgePlatform.setAppLanguageTag(normalized);
  appLocaleOverrideNotifier.value = localeForAppLanguageId(normalized);
}

String normalizeAppLanguageId(String? raw) {
  final String value = raw?.trim().replaceAll('_', '-') ?? '';
  if (value.isEmpty || value.toLowerCase() == appLanguageSystemId) {
    return appLanguageSystemId;
  }

  for (final AppLanguageOption option in appLanguageOptions) {
    if (option.id.toLowerCase() == value.toLowerCase()) {
      return option.id;
    }
  }

  return appLanguageSystemId;
}

Locale? localeForAppLanguageId(String? languageId) {
  final String normalized = normalizeAppLanguageId(languageId);
  for (final AppLanguageOption option in appLanguageOptions) {
    if (option.id == normalized) {
      return option.locale;
    }
  }
  return null;
}

AppLanguageOption appLanguageOptionForId(String? languageId) {
  final String normalized = normalizeAppLanguageId(languageId);
  return appLanguageOptions.firstWhere(
    (AppLanguageOption option) => option.id == normalized,
    orElse: () => appLanguageOptions.first,
  );
}

List<Locale> supportedAppLocales() {
  return appLanguageOptions
      .map((AppLanguageOption option) => option.locale)
      .whereType<Locale>()
      .toList(growable: false);
}
