import 'dart:convert';

class DictionaryLanguageOption {
  const DictionaryLanguageOption({
    required this.id,
    required this.label,
    required this.assetFileName,
  });

  final String id;
  final String label;
  final String assetFileName;
}

const List<DictionaryLanguageOption> lbDictionaryLanguages =
    <DictionaryLanguageOption>[
      DictionaryLanguageOption(
        id: 'en',
        label: 'English',
        assetFileName: 'liveupdate_dictionary_en.json',
      ),
      DictionaryLanguageOption(
        id: 'ru',
        label: 'Русский',
        assetFileName: 'liveupdate_dictionary_ru.json',
      ),
      DictionaryLanguageOption(
        id: 'zh',
        label: '中文',
        assetFileName: 'liveupdate_dictionary_zh.json',
      ),
    ];

const String lbDictionaryRemoteBaseUrl =
    'https://raw.githubusercontent.com/appsfolder/livebridge/refs/heads/main/android/app/src/main/assets';

String lbDictionaryRemoteUrl(DictionaryLanguageOption language) =>
    '$lbDictionaryRemoteBaseUrl/${language.assetFileName}';

Set<String> lbNormalizeDictionaryLanguageIds(Iterable<String> raw) {
  final Set<String> supported = lbDictionaryLanguages
      .map((DictionaryLanguageOption language) => language.id)
      .toSet();
  return raw
      .map((String value) => value.trim().toLowerCase())
      .where((String value) => supported.contains(value))
      .toSet();
}

Map<String, dynamic> lbBuildLanguageScopedDictionary({
  required Map<String, dynamic> combined,
  required String languageId,
}) {
  final String normalizedLanguageId = languageId.trim().toLowerCase();
  final Map<String, dynamic> scoped = <String, dynamic>{};

  for (final MapEntry<String, dynamic> entry in combined.entries) {
    final String key = entry.key;
    final dynamic value = entry.value;

    switch (key) {
      case 'smart_rules':
        final List<dynamic> scopedRules = _scopeSmartRules(
          value as List<dynamic>?,
          normalizedLanguageId,
        );
        if (scopedRules.isNotEmpty) {
          scoped[key] = scopedRules;
        }
        continue;
      case 'status_labels':
        final Map<String, dynamic> scopedStatusLabels = _scopeStatusLabels(
          value as Map<String, dynamic>?,
          normalizedLanguageId,
        );
        if (scopedStatusLabels.isNotEmpty) {
          scoped[key] = scopedStatusLabels;
        }
        continue;
      default:
        break;
    }

    if (_languageSensitiveArrayKeys.contains(key)) {
      final List<dynamic> filtered = _filterLanguageList(
        value as List<dynamic>?,
        normalizedLanguageId,
      );
      if (filtered.isNotEmpty) {
        scoped[key] = filtered;
      }
      continue;
    }

    if (_languageSensitiveStringKeys.contains(key)) {
      final String? filtered = _filterLanguageString(
        value,
        normalizedLanguageId,
      );
      if (filtered != null) {
        scoped[key] = filtered;
      }
      continue;
    }

    scoped[key] = _deepCopyJsonNode(value);
  }

  return scoped;
}

const Set<String> _languageSensitiveArrayKeys = <String>{
  'otp_strong_triggers',
  'weather_package_hints',
  'vpn_package_markers',
  'vpn_download_markers',
  'vpn_upload_markers',
  'external_device_generic_names',
  'order_context_hints',
};

const Set<String> _languageSensitiveStringKeys = <String>{
  'otp_loose_trigger_pattern',
  'money_context_pattern',
  'text_progress_include_context_pattern',
  'text_progress_exclude_context_pattern',
  'weather_context_pattern',
  'weather_day_pattern',
  'weather_condition_pattern',
  'weather_condition_thunder_pattern',
  'weather_condition_rain_pattern',
  'weather_condition_snow_pattern',
  'weather_condition_fog_pattern',
  'weather_condition_wind_pattern',
  'weather_condition_sun_pattern',
  'weather_condition_cloud_pattern',
  'vpn_context_pattern',
  'navigation_instruction_pattern',
};

final RegExp _cjkRegExp = RegExp(r'[\u3400-\u9FFF\uF900-\uFAFF]');
final RegExp _cyrillicRegExp = RegExp(r'[\u0400-\u04FF]');
final RegExp _latinRegExp = RegExp(r'[A-Za-z]');

List<dynamic> _scopeSmartRules(List<dynamic>? raw, String languageId) {
  if (raw == null) {
    return const <dynamic>[];
  }
  final List<dynamic> values = <dynamic>[];
  for (final dynamic item in raw) {
    if (item is! Map) {
      continue;
    }
    final Map<String, dynamic> source = Map<String, dynamic>.from(item);
    final Map<String, dynamic> scoped = <String, dynamic>{};
    source.forEach((String key, dynamic value) {
      switch (key) {
        case 'text_triggers':
        case 'exclude_patterns':
          final List<dynamic> filtered = _filterLanguageList(
            value as List<dynamic>?,
            languageId,
          );
          if (filtered.isNotEmpty) {
            scoped[key] = filtered;
          }
          break;
        case 'signals':
          final List<dynamic> signals = _scopeSignals(
            value as List<dynamic>?,
            languageId,
          );
          if (signals.isNotEmpty) {
            scoped[key] = signals;
          }
          break;
        default:
          scoped[key] = _deepCopyJsonNode(value);
          break;
      }
    });

    final List<dynamic> signals = scoped['signals'] as List<dynamic>? ?? const <dynamic>[];
    if (signals.isEmpty) {
      continue;
    }
    values.add(scoped);
  }
  return values;
}

List<dynamic> _scopeSignals(List<dynamic>? raw, String languageId) {
  if (raw == null) {
    return const <dynamic>[];
  }
  final List<dynamic> values = <dynamic>[];
  for (final dynamic item in raw) {
    if (item is! Map) {
      continue;
    }
    final Map<String, dynamic> source = Map<String, dynamic>.from(item);
    final String? pattern = _filterLanguageString(source['pattern'], languageId);
    if (pattern == null) {
      continue;
    }
    values.add(<String, dynamic>{
      'stage': source['stage'],
      'pattern': pattern,
    });
  }
  return values;
}

Map<String, dynamic> _scopeStatusLabels(
  Map<String, dynamic>? raw,
  String languageId,
) {
  if (raw == null) {
    return const <String, dynamic>{};
  }
  final Map<String, dynamic> values = <String, dynamic>{};
  for (final MapEntry<String, dynamic> entry in raw.entries) {
    if (entry.value is! Map) {
      continue;
    }
    final Map<String, dynamic> localized = Map<String, dynamic>.from(
      entry.value as Map,
    );
    final Map<String, dynamic> scopedLocales = <String, dynamic>{};
    for (final MapEntry<String, dynamic> localeEntry in localized.entries) {
      final String normalizedLocale = localeEntry.key
          .trim()
          .replaceAll('_', '-')
          .toLowerCase();
      if (!_matchesLanguageForLocale(normalizedLocale, languageId)) {
        continue;
      }
      scopedLocales[localeEntry.key] = _deepCopyJsonNode(localeEntry.value);
    }
    if (scopedLocales.isNotEmpty) {
      values[entry.key] = scopedLocales;
    }
  }
  return values;
}

List<dynamic> _filterLanguageList(List<dynamic>? raw, String languageId) {
  if (raw == null) {
    return const <dynamic>[];
  }
  return raw
      .where((dynamic value) => _matchesLanguageForValue(value, languageId))
      .map(_deepCopyJsonNode)
      .toList();
}

String? _filterLanguageString(dynamic raw, String languageId) {
  if (raw is! String || raw.trim().isEmpty) {
    return null;
  }
  return _matchesLanguageForValue(raw, languageId) ? raw : null;
}

bool _matchesLanguageForValue(dynamic raw, String languageId) {
  if (raw == null) {
    return false;
  }
  final String source = raw is String ? raw : jsonEncode(raw);
  final Set<String> detected = _detectLanguages(source);
  return detected.contains(languageId);
}

bool _matchesLanguageForLocale(String localeKey, String languageId) {
  if (languageId == 'zh') {
    return localeKey.startsWith('zh');
  }
  return localeKey == languageId || localeKey.startsWith('$languageId-');
}

Set<String> _detectLanguages(String raw) {
  final String value = raw.trim();
  if (value.isEmpty) {
    return const <String>{'en', 'ru', 'zh'};
  }

  final Set<String> languages = <String>{};
  if (_cjkRegExp.hasMatch(value)) {
    languages.add('zh');
  }
  if (_cyrillicRegExp.hasMatch(value)) {
    languages.add('ru');
  }
  if (_latinRegExp.hasMatch(value)) {
    languages.add('en');
  }

  return languages.isEmpty ? const <String>{'en', 'ru', 'zh'} : languages;
}

dynamic _deepCopyJsonNode(dynamic node) {
  if (node is Map) {
    return <String, dynamic>{
      for (final MapEntry<dynamic, dynamic> entry in node.entries)
        entry.key.toString(): _deepCopyJsonNode(entry.value),
    };
  }
  if (node is List) {
    return node.map(_deepCopyJsonNode).toList();
  }
  return node;
}
