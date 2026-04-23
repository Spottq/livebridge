import 'dart:convert';

class ConversionLogEntry {
  const ConversionLogEntry({
    required this.sourceKey,
    required this.packageName,
    required this.appLabel,
    required this.postedAtMs,
    required this.title,
    required this.text,
    required this.payloadJson,
  });

  final String sourceKey;
  final String packageName;
  final String appLabel;
  final int postedAtMs;
  final String title;
  final String text;
  final String payloadJson;

  factory ConversionLogEntry.fromJson(Map<String, dynamic> json) {
    return ConversionLogEntry(
      sourceKey: (json['source_key'] as String? ?? '').trim(),
      packageName: (json['package_name'] as String? ?? '').trim(),
      appLabel: (json['app_label'] as String? ?? '').trim(),
      postedAtMs: (json['posted_at_ms'] as num?)?.toInt() ?? 0,
      title: (json['title'] as String? ?? '').trim(),
      text: (json['text'] as String? ?? '').trim(),
      payloadJson: (json['payload_json'] as String? ?? '').trim(),
    );
  }

  static List<ConversionLogEntry> decodeList(String raw) {
    if (raw.trim().isEmpty) {
      return const <ConversionLogEntry>[];
    }
    final Object? decoded = jsonDecode(raw);
    if (decoded is! List) {
      return const <ConversionLogEntry>[];
    }
    return decoded
        .whereType<Map>()
        .map(
          (Map item) =>
              ConversionLogEntry.fromJson(Map<String, dynamic>.from(item)),
        )
        .where(
          (ConversionLogEntry entry) =>
              entry.sourceKey.isNotEmpty && entry.packageName.isNotEmpty,
        )
        .toList(growable: false);
  }
}
