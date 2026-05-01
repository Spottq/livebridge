import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../l10n/app_strings.dart';
import '../../platform/livebridge_platform.dart';
import '../../theme/livebridge_tokens.dart';
import '../../utils/livebridge_haptics.dart';
import '../../utils/version_compare.dart';
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
  String _latestReleaseVersion = '';
  String _releaseNotes = '';
  bool _isLoadingReleaseNotes = false;
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
      final Future<String> latestVersionFuture =
          LiveBridgePlatform.getUpdateCachedLatestVersion();

      final bool updateChecksEnabled = await updateChecksFuture;
      final String currentVersion = await currentVersionFuture;
      final bool updateAvailable = await updateAvailableFuture;
      final String latestVersion = await latestVersionFuture;
      final bool sanitizedUpdateAvailable =
          updateAvailable &&
          lbIsReleaseNewer(
            currentVersion: currentVersion,
            latestVersion: latestVersion,
          );

      if (updateAvailable && !sanitizedUpdateAvailable) {
        unawaited(LiveBridgePlatform.setUpdateCachedAvailable(false));
        unawaited(LiveBridgePlatform.setUpdateCachedLatestVersion(''));
      }

      if (!mounted) {
        return;
      }

      setState(() {
        _updateChecksEnabled = updateChecksEnabled;
        _currentVersion = currentVersion;
        _updateAvailable = sanitizedUpdateAvailable;
        _latestReleaseVersion = sanitizedUpdateAvailable ? latestVersion : '';
        if (!sanitizedUpdateAvailable) {
          _releaseNotes = '';
        }
      });
      if (sanitizedUpdateAvailable) {
        unawaited(_loadReleaseNotesForUpdate(latestVersion));
      }
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
      final bool hasUpdate = lbIsReleaseNewer(
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
        _latestReleaseVersion = hasUpdate ? latest.version : '';
        _releaseNotes = hasUpdate ? _formatReleaseNotes(latest.body) : '';
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

  Future<void> _loadReleaseNotesForUpdate(String expectedVersion) async {
    if (_isLoadingReleaseNotes || expectedVersion.trim().isEmpty) {
      return;
    }
    if (mounted) {
      setState(() => _isLoadingReleaseNotes = true);
    }
    try {
      final _GithubReleaseInfo? latest = await _fetchLatestRelease();
      if (latest == null || !mounted) {
        return;
      }

      final String currentVersion = _currentVersion.isNotEmpty
          ? _currentVersion
          : await LiveBridgePlatform.getAppVersionName();
      final bool hasUpdate = lbIsReleaseNewer(
        currentVersion: currentVersion,
        latestVersion: latest.version,
      );
      setState(() {
        _currentVersion = currentVersion;
        _updateAvailable = hasUpdate;
        _latestReleaseVersion = hasUpdate ? latest.version : '';
        _releaseNotes = hasUpdate ? _formatReleaseNotes(latest.body) : '';
      });
      if (hasUpdate) {
        unawaited(
          LiveBridgePlatform.setUpdateCachedLatestVersion(latest.version),
        );
        unawaited(LiveBridgePlatform.setUpdateCachedAvailable(true));
      } else {
        unawaited(LiveBridgePlatform.setUpdateCachedAvailable(false));
        unawaited(LiveBridgePlatform.setUpdateCachedLatestVersion(''));
      }
    } finally {
      if (mounted) {
        setState(() => _isLoadingReleaseNotes = false);
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
        body: (data['body'] as String?)?.trim() ?? '',
      );
    } catch (_) {
      return null;
    } finally {
      client.close(force: true);
    }
  }

  String _formatReleaseNotes(String raw) {
    final String normalized = raw
        .replaceAll('\r\n', '\n')
        .replaceAll('\r', '\n')
        .replaceAll(RegExp(r'<!--[\s\S]*?-->'), '')
        .trim();
    if (normalized.isEmpty) {
      return '';
    }

    final List<String> lines = <String>[];
    for (final String rawLine in normalized.split('\n')) {
      String line = rawLine.trimRight();
      if (line.trim().isEmpty) {
        if (lines.isNotEmpty && lines.last.isNotEmpty) {
          lines.add('');
        }
        continue;
      }

      line = line
          .replaceAll(RegExp(r'^#{1,6}\s*'), '')
          .replaceAll(RegExp(r'^[-*]\s+'), '• ')
          .replaceAll(RegExp(r'^\d+\.\s+'), '• ')
          .replaceAll(RegExp(r'!\[[^\]]*\]\([^)]*\)'), '')
          .replaceAllMapped(
            RegExp(r'\[([^\]]+)\]\([^)]*\)'),
            (Match match) => match.group(1) ?? '',
          )
          .replaceAllMapped(
            RegExp(r'`([^`]+)`'),
            (Match match) => match.group(1) ?? '',
          )
          .replaceAllMapped(
            RegExp(r'\*\*([^*]+)\*\*'),
            (Match match) => match.group(1) ?? '',
          )
          .replaceAllMapped(
            RegExp(r'__([^_]+)__'),
            (Match match) => match.group(1) ?? '',
          )
          .trimRight();

      if (line.trim().isNotEmpty) {
        lines.add(line);
      }
    }

    return lines.join('\n').replaceAll(RegExp(r'\n{3,}'), '\n\n').trim();
  }

  Widget _buildReleaseNotesCard(AppStrings strings, LbPalette palette) {
    final String notes = _releaseNotes.trim();
    final bool hasNotes = notes.isNotEmpty;

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(
        horizontal: LbSpacing.md,
        vertical: LbSpacing.md,
      ),
      decoration: BoxDecoration(
        color: palette.surface,
        borderRadius: BorderRadius.circular(LbRadius.card),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text(
            strings.appUpdateLogTitle,
            style: LbTextStyles.body.copyWith(color: palette.textPrimary),
          ),
          const SizedBox(height: LbSpacing.sm),
          AnimatedSwitcher(
            duration: const Duration(milliseconds: 180),
            child: Text(
              hasNotes
                  ? notes
                  : _isLoadingReleaseNotes
                  ? strings.appUpdateLogLoading
                  : strings.appUpdateLogUnavailable,
              key: ValueKey<String>(
                hasNotes
                    ? 'notes:${_latestReleaseVersion.trim()}'
                    : 'placeholder:$_isLoadingReleaseNotes',
              ),
              style: LbTextStyles.caption.copyWith(
                color: palette.textSecondary,
                height: 1.35,
              ),
            ),
          ),
        ],
      ),
    );
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
        if (_updateAvailable) ...<Widget>[
          const SizedBox(height: LbSpacing.sm),
          _buildReleaseNotesCard(strings, palette),
        ],
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
  const _GithubReleaseInfo({
    required this.version,
    required this.htmlUrl,
    required this.body,
  });

  final String version;
  final String htmlUrl;
  final String body;
}
