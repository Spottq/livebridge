import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../l10n/app_strings.dart';
import '../../platform/livebridge_platform.dart';
import '../../theme/livebridge_tokens.dart';
import '../../utils/livebridge_haptics.dart';
import '../../widgets/redesign/lb_detail_screen.dart';
import '../../widgets/redesign/lb_icon.dart';
import '../../widgets/redesign/lb_list_component.dart';
import '../../widgets/redesign/lb_toast.dart';

class SettingsUpdateScreen extends StatefulWidget {
  const SettingsUpdateScreen({
    super.key,
    required this.initialCurrentVersion,
    required this.initialUpdateAvailable,
  });

  final String initialCurrentVersion;
  final bool initialUpdateAvailable;

  @override
  State<SettingsUpdateScreen> createState() => _SettingsUpdateScreenState();
}

class _SettingsUpdateScreenState extends State<SettingsUpdateScreen>
    with SingleTickerProviderStateMixin {
  static const String _projectGithubUrl =
      'https://github.com/appsfolder/livebridge';
  static const String _projectDownloadPageUrl =
      'https://appsfolder.github.io/livebridge/';
  static const String _latestReleaseApiUrl =
      'https://api.github.com/repos/appsfolder/livebridge/releases/latest';

  bool _updateChecksEnabled = true;
  bool _updateAvailable = false;
  bool _isChecking = false;
  String _currentVersion = '';
  late final AnimationController _refreshRotationController;

  @override
  void initState() {
    super.initState();
    _refreshRotationController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 920),
    );
    _currentVersion = widget.initialCurrentVersion;
    _updateAvailable = widget.initialUpdateAvailable;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      unawaited(_loadState());
    });
  }

  @override
  void dispose() {
    _refreshRotationController.dispose();
    super.dispose();
  }

  void _setChecking(bool value) {
    if (_isChecking == value) {
      return;
    }
    if (value) {
      _refreshRotationController.repeat();
    } else {
      _refreshRotationController
        ..stop()
        ..reset();
    }
    setState(() => _isChecking = value);
  }

  Future<void> _loadState() async {
    try {
      final Future<bool> updateChecksFuture =
          LiveBridgePlatform.getUpdateChecksEnabled();
      final Future<String> currentVersionFuture =
          LiveBridgePlatform.getAppVersionName();
      final Future<bool> updateAvailableFuture =
          LiveBridgePlatform.getUpdateCachedAvailable();

      final bool updateChecksEnabled = await updateChecksFuture;
      final String currentVersion = await currentVersionFuture;
      final bool updateAvailable = await updateAvailableFuture;

      if (!mounted) {
        return;
      }

      setState(() {
        _updateChecksEnabled = updateChecksEnabled;
        _currentVersion = currentVersion;
        _updateAvailable = updateAvailable;
      });
    } catch (_) {}
  }

  Future<bool> _launchUrlWithFallback(Uri uri) async {
    final bool openedInBrowserView = await launchUrl(
      uri,
      mode: LaunchMode.inAppBrowserView,
    );
    if (openedInBrowserView) {
      return true;
    }
    return launchUrl(uri, mode: LaunchMode.externalApplication);
  }

  Future<void> _openProjectPage() async {
    final bool opened = await _launchUrlWithFallback(
      Uri.parse(_projectDownloadPageUrl),
    );
    if (!opened && mounted) {
      showLbToast(context, message: AppStrings.of(context).linkOpenFailed);
    }
  }

  Future<void> _openGithub() async {
    final bool opened = await _launchUrlWithFallback(
      Uri.parse(_projectGithubUrl),
    );
    if (!opened && mounted) {
      showLbToast(context, message: AppStrings.of(context).githubOpenFailed);
    }
  }

  Future<void> _setUpdateChecksEnabled(bool value) async {
    if (_updateChecksEnabled == value) {
      return;
    }
    setState(() => _updateChecksEnabled = value);
    await LiveBridgePlatform.setUpdateChecksEnabled(value);
    if (value) {
      await _checkForUpdatesNow(showFailureToast: false);
    }
  }

  Future<void> _checkForUpdatesNow({bool showFailureToast = true}) async {
    if (_isChecking) {
      return;
    }

    _setChecking(true);
    try {
      final _GithubReleaseInfo? latest = await _fetchLatestRelease();
      if (latest == null) {
        if (showFailureToast && mounted) {
          showLbToast(
            context,
            message: AppStrings.of(context).updateCheckFailed,
          );
        }
        return;
      }

      final String currentVersion = _currentVersion.isNotEmpty
          ? _currentVersion
          : await LiveBridgePlatform.getAppVersionName();
      final bool hasUpdate = _isReleaseNewer(
        currentVersion: currentVersion,
        latestVersion: latest.version,
      );

      await LiveBridgePlatform.setUpdateLastCheckAtMs(
        DateTime.now().millisecondsSinceEpoch,
      );
      await LiveBridgePlatform.setUpdateCachedLatestVersion(latest.version);
      await LiveBridgePlatform.setUpdateCachedAvailable(hasUpdate);

      if (hasUpdate) {
        final String lastNotifiedVersion =
            await LiveBridgePlatform.getUpdateLastNotifiedVersion();
        if (lastNotifiedVersion != latest.version) {
          final bool notified =
              await LiveBridgePlatform.showUpdateAvailableNotification(
                version: latest.version,
                releaseUrl: latest.htmlUrl,
              );
          if (notified) {
            await LiveBridgePlatform.setUpdateLastNotifiedVersion(
              latest.version,
            );
          }
        }
      }

      if (!mounted) {
        return;
      }
      setState(() {
        _currentVersion = currentVersion;
        _updateAvailable = hasUpdate;
      });
    } catch (_) {
      if (showFailureToast && mounted) {
        showLbToast(context, message: AppStrings.of(context).updateCheckFailed);
      }
    } finally {
      if (mounted) {
        _setChecking(false);
      }
    }
  }

  Future<_GithubReleaseInfo?> _fetchLatestRelease() async {
    final HttpClient client = HttpClient()
      ..connectionTimeout = const Duration(seconds: 8);
    try {
      final HttpClientRequest request = await client.getUrl(
        Uri.parse(_latestReleaseApiUrl),
      );
      request.headers.set(
        HttpHeaders.acceptHeader,
        'application/vnd.github+json',
      );
      request.headers.set(
        HttpHeaders.userAgentHeader,
        _currentVersion.isNotEmpty
            ? 'LiveBridge/${_currentVersion.trim()}'
            : 'LiveBridge/update-check',
      );

      final HttpClientResponse response = await request.close();
      if (response.statusCode != HttpStatus.ok) {
        return null;
      }

      final String payload = await utf8.decoder.bind(response).join();
      final dynamic decoded = jsonDecode(payload);
      if (decoded is! Map) {
        return null;
      }

      final Map<dynamic, dynamic> data = decoded;
      final String tag = (data['tag_name'] as String?)?.trim() ?? '';
      final String name = (data['name'] as String?)?.trim() ?? '';
      final String version = tag.isNotEmpty ? tag : name;
      if (version.isEmpty) {
        return null;
      }

      return _GithubReleaseInfo(
        version: version,
        htmlUrl: _projectDownloadPageUrl,
      );
    } catch (_) {
      return null;
    } finally {
      client.close(force: true);
    }
  }

  bool _isReleaseNewer({
    required String currentVersion,
    required String latestVersion,
  }) {
    final List<int> currentParts = _extractVersionParts(currentVersion);
    final List<int> latestParts = _extractVersionParts(latestVersion);
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

  List<int> _extractVersionParts(String input) {
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
    final String coreVersion = parts.first;
    final List<int> versionParts = coreVersion
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

  @override
  Widget build(BuildContext context) {
    final AppStrings strings = AppStrings.of(context);
    final LbPalette palette = LbPalette.of(context);

    return LbDetailScreen(
      title: strings.appUpdatesTitle,
      children: <Widget>[
        GestureDetector(
          onTap: () {
            if (_updateAvailable) {
              unawaited(LiveBridgeHaptics.openSurface());
              unawaited(_openProjectPage());
            } else {
              unawaited(LiveBridgeHaptics.selection());
              unawaited(_checkForUpdatesNow());
            }
          },
          behavior: HitTestBehavior.opaque,
          child: Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(
              horizontal: LbSpacing.md,
              vertical: LbSpacing.lg,
            ),
            decoration: BoxDecoration(
              color: _updateAvailable
                  ? palette.warningSurface
                  : palette.surface,
              borderRadius: BorderRadius.circular(LbRadius.card),
            ),
            child: Row(
              children: <Widget>[
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Text(
                        _updateAvailable
                            ? strings.appUpdateNewVersionTitle
                            : _isChecking
                            ? strings.appUpdateCheckingTitle
                            : strings.appUpdateAllSetTitle,
                        style: LbTextStyles.body.copyWith(
                          color: palette.textPrimary,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        _updateAvailable
                            ? strings.appUpdateDownloadsSubtitle
                            : _isChecking
                            ? strings.appUpdatePleaseWaitSubtitle
                            : strings.appUpdateLatestSubtitle,
                        style: LbTextStyles.body.copyWith(
                          color: palette.textSecondary,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: LbSpacing.md),
                SizedBox.square(
                  dimension: 34,
                  child: Stack(
                    alignment: Alignment.center,
                    children: <Widget>[
                      RotationTransition(
                        turns: _refreshRotationController,
                        alignment: const Alignment(0.0, 0.05),
                        child: LbIcon(
                          symbol: LbIconSymbol.refresh,
                          size: 34,
                          color: palette.textPrimary,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: LbSpacing.md),
        LbListComponent(
          items: <LbListItemData>[
            LbListItemData(
              title: strings.updateChecksTitle,
              showChevron: false,
              toggleValue: _updateChecksEnabled,
              onToggle: (bool value) {
                unawaited(_setUpdateChecksEnabled(value));
              },
              onTap: () {
                final bool nextValue = !_updateChecksEnabled;
                unawaited(LiveBridgeHaptics.toggle(nextValue));
                unawaited(_setUpdateChecksEnabled(nextValue));
              },
            ),
            LbListItemData(
              title: strings.visitProjectPageTitle,
              showChevron: false,
              trailingWidget: LbIcon(
                symbol: LbIconSymbol.externalLink,
                size: 24,
                color: palette.textPrimary,
              ),
              onTap: () {
                unawaited(LiveBridgeHaptics.openSurface());
                unawaited(_openProjectPage());
              },
            ),
            LbListItemData(
              title: strings.visitGithubTitle,
              showChevron: false,
              trailingWidget: LbIcon(
                symbol: LbIconSymbol.externalLink,
                size: 24,
                color: palette.textPrimary,
              ),
              onTap: () {
                unawaited(LiveBridgeHaptics.openSurface());
                unawaited(_openGithub());
              },
            ),
          ],
          rowHeight: LbSpacing.recentRowHeight,
          extendDividersToEnd: true,
        ),
      ],
    );
  }
}

class _GithubReleaseInfo {
  const _GithubReleaseInfo({required this.version, required this.htmlUrl});

  final String version;
  final String htmlUrl;
}
