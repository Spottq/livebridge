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

class ConversionLogPage {
  const ConversionLogPage({
    required this.entries,
    required this.hasMore,
    required this.totalCount,
  });

  final List<ConversionLogEntry> entries;
  final bool hasMore;
  final int totalCount;

  factory ConversionLogPage.decode(String raw) {
    if (raw.trim().isEmpty) {
      return const ConversionLogPage(
        entries: <ConversionLogEntry>[],
        hasMore: false,
        totalCount: 0,
      );
    }

    final Object? decoded = jsonDecode(raw);
    if (decoded is! Map) {
      return const ConversionLogPage(
        entries: <ConversionLogEntry>[],
        hasMore: false,
        totalCount: 0,
      );
    }

    final Map<String, dynamic> page = Map<String, dynamic>.from(decoded);
    final Object? entriesJson = page['entries'];
    final List<ConversionLogEntry> entries = entriesJson is List
        ? entriesJson
              .whereType<Map>()
              .map(
                (Map item) => ConversionLogEntry.fromJson(
                  Map<String, dynamic>.from(item),
                ),
              )
              .where(
                (ConversionLogEntry entry) =>
                    entry.sourceKey.isNotEmpty && entry.packageName.isNotEmpty,
              )
              .toList(growable: false)
        : const <ConversionLogEntry>[];

    return ConversionLogPage(
      entries: entries,
      hasMore: page['has_more'] as bool? ?? false,
      totalCount: (page['total_count'] as num?)?.toInt() ?? entries.length,
    );
  }
}
