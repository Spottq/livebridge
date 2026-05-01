List<int> lbExtractVersionParts(String input) {
  final RegExpMatch? match = RegExp(
    r'v?\d+(?:\.\d+){1,3}(?:\+\d+)?',
    caseSensitive: false,
  ).firstMatch(input.trim());
  if (match == null) {
    return const <int>[];
  }

  final String normalized = match
      .group(0)!
      .trim()
      .toLowerCase()
      .replaceFirst(RegExp(r'^v'), '');
  if (normalized.isEmpty) {
    return const <int>[];
  }

  final List<String> parts = normalized.split('+');
  final List<int> versionParts = parts.first
      .split('.')
      .map((String value) => int.tryParse(value) ?? 0)
      .toList();
  if (versionParts.isEmpty) {
    return const <int>[];
  }

  if (parts.length > 1) {
    versionParts.add(int.tryParse(parts[1]) ?? 0);
  }
  return versionParts;
}

bool lbIsReleaseNewer({
  required String currentVersion,
  required String latestVersion,
}) {
  final List<int> currentParts = lbExtractVersionParts(currentVersion);
  final List<int> latestParts = lbExtractVersionParts(latestVersion);
  if (latestParts.isEmpty || currentParts.isEmpty) {
    return false;
  }

  final int maxLen = currentParts.length > latestParts.length
      ? currentParts.length
      : latestParts.length;
  for (int index = 0; index < maxLen; index += 1) {
    final int current = index < currentParts.length ? currentParts[index] : 0;
    final int latest = index < latestParts.length ? latestParts[index] : 0;
    if (latest > current) {
      return true;
    }
    if (latest < current) {
      return false;
    }
  }
  return false;
}
