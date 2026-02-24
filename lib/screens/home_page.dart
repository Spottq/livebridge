import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:url_launcher/url_launcher.dart';

import '../l10n/app_strings.dart';
import '../models/app_models.dart';
import '../platform/livebridge_platform.dart';
import '../widgets/shared_widgets.dart';
import 'app_presentation_settings_page.dart';

class LiveBridgeHomePage extends StatefulWidget {
  const LiveBridgeHomePage({super.key});

  @override
  State<LiveBridgeHomePage> createState() => _LiveBridgeHomePageState();
}

class _LiveBridgeHomePageState extends State<LiveBridgeHomePage>
    with SingleTickerProviderStateMixin, WidgetsBindingObserver {
  static const String _projectGithubUrl =
      'https://github.com/appsfolder/livebridge';
  static const String _projectGithubReleasesUrl =
      'https://github.com/appsfolder/livebridge/releases';
  static const String _latestReleaseApiUrl =
      'https://api.github.com/repos/appsfolder/livebridge/releases/latest';
  static const String _dictionaryRawUrl =
      'https://raw.githubusercontent.com/appsfolder/livebridge/refs/heads/main/android/app/src/main/assets/liveupdate_dictionary.json';
  static const Duration _updateCheckInterval = Duration(hours: 6);

  final TextEditingController _rulesController = TextEditingController();
  final TextEditingController _otpRulesController = TextEditingController();

  Timer? _statusRefreshTimer;
  Timer? _updateRefreshTimer;
  bool _isRefreshingState = false;
  bool _isCheckingUpdates = false;
  bool _isLoading = true;
  bool _deviceBlocked = false;
  bool _listenerEnabled = false;
  bool _notificationsGranted = false;
  bool _canPostPromoted = false;
  bool _converterEnabled = true;
  bool _keepAliveForegroundEnabled = false;
  bool _aospCuttingEnabled = false;
  bool _onlyWithProgress = true;
  bool _textProgressEnabled = true;
  bool _smartDetectionEnabled = true;
  bool _smartNavigationEnabled = true;
  bool _otpDetectionEnabled = true;
  bool _otpAutoCopyEnabled = false;
  bool _hasCustomParserDictionary = false;
  bool _dictionaryActionInProgress = false;
  bool _showBackgroundWarning = false;
  bool _showSamsungDeveloperWarning = false;
  bool _hidePromotedAccess = false;
  bool _isSamsungDevice = false;
  bool _isAospDevice = false;
  bool _samsungRemoteReparserEnabled = true;
  bool _updateChecksEnabled = true;
  bool _updateAvailable = false;
  String _latestReleaseVersion = '';
  String _currentAppVersion = '';
  String _deviceLabelForWarning = '';
  final Set<String> _expandedSections = <String>{};
  bool _expandedSectionsLoaded = false;
  bool _hasPersistedExpandedSections = false;
  bool _didInitSectionDefaults = false;
  PackageMode _packageMode = PackageMode.all;
  PackageMode _otpPackageMode = PackageMode.all;
  late final AnimationController _masterBlockedShakeController;
  late final Animation<double> _masterBlockedShakeOffset;
  bool _masterBlockedHapticInProgress = false;

  bool get _canToggleMaster => _listenerEnabled && _notificationsGranted;
  bool get _effectiveConverterEnabled => _canToggleMaster && _converterEnabled;
  bool get _hasAllAccessPermissions =>
      _listenerEnabled &&
      _notificationsGranted &&
      (_hidePromotedAccess || _canPostPromoted);
  bool get _hasUpdateAlert => _updateChecksEnabled && _updateAvailable;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _masterBlockedShakeController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 420),
    );
    _masterBlockedShakeOffset =
        TweenSequence<double>([
          TweenSequenceItem(
            tween: Tween<double>(begin: 0, end: 10),
            weight: 14,
          ),
          TweenSequenceItem(
            tween: Tween<double>(begin: 10, end: -10),
            weight: 20,
          ),
          TweenSequenceItem(
            tween: Tween<double>(begin: -10, end: 8),
            weight: 18,
          ),
          TweenSequenceItem(
            tween: Tween<double>(begin: 8, end: -8),
            weight: 18,
          ),
          TweenSequenceItem(
            tween: Tween<double>(begin: -8, end: 4),
            weight: 14,
          ),
          TweenSequenceItem(
            tween: Tween<double>(begin: 4, end: -4),
            weight: 10,
          ),
          TweenSequenceItem(tween: Tween<double>(begin: -4, end: 0), weight: 6),
        ]).animate(
          CurvedAnimation(
            parent: _masterBlockedShakeController,
            curve: Curves.easeOut,
          ),
        );
    _refreshState();
    _statusRefreshTimer = Timer.periodic(const Duration(seconds: 5), (_) {
      _refreshState(showLoading: false);
    });
    _updateRefreshTimer = Timer.periodic(const Duration(minutes: 30), (_) {
      unawaited(_checkForUpdatesIfNeeded());
    });
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _statusRefreshTimer?.cancel();
    _updateRefreshTimer?.cancel();
    _masterBlockedShakeController.dispose();
    _rulesController.dispose();
    _otpRulesController.dispose();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      unawaited(_checkForUpdatesIfNeeded());
    }
  }

  Future<void> _refreshState({bool showLoading = true}) async {
    if (_isRefreshingState) return;
    _isRefreshingState = true;

    if (showLoading) setState(() => _isLoading = true);

    try {
      final bool deviceBlocked = await LiveBridgePlatform.isDeviceBlocked();
      if (deviceBlocked) {
        if (mounted) {
          setState(() {
            _deviceBlocked = true;
            _isLoading = false;
          });
        }
        return;
      }

      final bool listenerEnabled =
          await LiveBridgePlatform.isNotificationListenerEnabled();
      final bool notificationsGranted =
          await LiveBridgePlatform.isNotificationPermissionGranted();
      final bool canPostPromoted =
          await LiveBridgePlatform.canPostPromotedNotifications();
      final bool converterEnabled =
          await LiveBridgePlatform.getConverterEnabled();
      final bool keepAliveForegroundEnabled =
          await LiveBridgePlatform.getKeepAliveForegroundEnabled();
      final bool aospCuttingEnabled =
          await LiveBridgePlatform.getAospCuttingEnabled();
      final bool onlyWithProgress =
          await LiveBridgePlatform.getOnlyWithProgress();
      final bool textProgressEnabled =
          await LiveBridgePlatform.getTextProgressEnabled();
      final bool smartDetectionEnabled =
          await LiveBridgePlatform.getSmartStatusDetectionEnabled();
      final bool smartNavigationEnabled =
          await LiveBridgePlatform.getSmartNavigationEnabled();
      final bool otpDetectionEnabled =
          await LiveBridgePlatform.getOtpDetectionEnabled();
      final bool otpAutoCopyEnabled =
          await LiveBridgePlatform.getOtpAutoCopyEnabled();
      final bool updateChecksEnabled =
          await LiveBridgePlatform.getUpdateChecksEnabled();
      final bool updateCachedAvailable =
          await LiveBridgePlatform.getUpdateCachedAvailable();
      final String updateCachedLatestVersion =
          await LiveBridgePlatform.getUpdateCachedLatestVersion();
      final String currentAppVersion =
          await LiveBridgePlatform.getAppVersionName();
      final bool normalizedCachedUpdateAvailable =
          updateCachedAvailable &&
          _isReleaseNewer(
            currentVersion: currentAppVersion,
            latestVersion: updateCachedLatestVersion,
          );
      final bool hasCustomParserDictionary =
          await LiveBridgePlatform.hasCustomParserDictionary();
      final bool backgroundWarningDismissed =
          await LiveBridgePlatform.getBackgroundWarningDismissed();
      final bool samsungWarningDismissed =
          await LiveBridgePlatform.getSamsungWarningDismissed();
      final bool samsungRemoteReparserEnabled =
          await LiveBridgePlatform.getSamsungRemoteReparserEnabled();
      final bool hasExpandedSectionsState =
          await LiveBridgePlatform.hasExpandedSectionsState();
      final String expandedSectionsRaw = hasExpandedSectionsState
          ? await LiveBridgePlatform.getExpandedSections()
          : '';
      final Set<String> restoredExpandedSections = _parseExpandedSections(
        expandedSectionsRaw,
      );
      final DeviceInfo deviceInfo = await LiveBridgePlatform.getDeviceInfo();
      final String packageRules = await LiveBridgePlatform.getPackageRules();
      final PackageMode packageMode = PackageModeId.from(
        await LiveBridgePlatform.getPackageMode(),
      );
      final String otpPackageRules =
          await LiveBridgePlatform.getOtpPackageRules();
      final PackageMode otpPackageMode = PackageModeId.from(
        await LiveBridgePlatform.getOtpPackageMode(),
      );

      if (!mounted) return;

      setState(() {
        if (!_expandedSectionsLoaded) {
          _expandedSections
            ..clear()
            ..addAll(restoredExpandedSections);
          _hasPersistedExpandedSections = hasExpandedSectionsState;
          _expandedSectionsLoaded = true;
        }

        final bool allAccessPermissionsGranted =
            listenerEnabled &&
            notificationsGranted &&
            (deviceInfo.shouldHideLiveUpdatesPromotion || canPostPromoted);
        if (!_didInitSectionDefaults) {
          if (!_hasPersistedExpandedSections) {
            if (allAccessPermissionsGranted) {
              _expandedSections.remove('access');
            } else {
              _expandedSections.add('access');
            }
          }
          _didInitSectionDefaults = true;
        }
        _deviceBlocked = false;
        _listenerEnabled = listenerEnabled;
        _notificationsGranted = notificationsGranted;
        _canPostPromoted = canPostPromoted;
        _converterEnabled = converterEnabled;
        _keepAliveForegroundEnabled = keepAliveForegroundEnabled;
        _aospCuttingEnabled = aospCuttingEnabled;
        _onlyWithProgress = onlyWithProgress;
        _textProgressEnabled = textProgressEnabled;
        _smartDetectionEnabled = smartDetectionEnabled;
        _smartNavigationEnabled = smartNavigationEnabled;
        _otpDetectionEnabled = otpDetectionEnabled;
        _otpAutoCopyEnabled = otpAutoCopyEnabled;
        _updateChecksEnabled = updateChecksEnabled;
        _updateAvailable = normalizedCachedUpdateAvailable;
        _latestReleaseVersion = updateCachedLatestVersion;
        _currentAppVersion = currentAppVersion;
        _hasCustomParserDictionary = hasCustomParserDictionary;
        _hidePromotedAccess = deviceInfo.shouldHideLiveUpdatesPromotion;
        _isSamsungDevice = deviceInfo.isSamsung;
        _isAospDevice = deviceInfo.isAospDevice;
        _samsungRemoteReparserEnabled = samsungRemoteReparserEnabled;
        _showBackgroundWarning =
            !deviceInfo.isPixel &&
            !deviceInfo.isSamsung &&
            !backgroundWarningDismissed;
        _showSamsungDeveloperWarning =
            deviceInfo.isSamsung && !samsungWarningDismissed;
        _deviceLabelForWarning = deviceInfo.label;
        _packageMode = packageMode;
        _otpPackageMode = otpPackageMode;
        _rulesController.text = packageRules;
        _otpRulesController.text = otpPackageRules;
        _isLoading = false;
      });

      if (normalizedCachedUpdateAvailable != updateCachedAvailable) {
        unawaited(
          LiveBridgePlatform.setUpdateCachedAvailable(
            normalizedCachedUpdateAvailable,
          ),
        );
      }

      if (showLoading) {
        unawaited(_checkForUpdatesIfNeeded());
      }
    } on PlatformException catch (error) {
      if (!mounted) return;
      setState(() => _isLoading = false);
      _snack('Native error: ${error.message}');
    } finally {
      _isRefreshingState = false;
    }
  }

  Future<void> _persistRules({required bool otpMode}) async {
    try {
      if (otpMode) {
        await LiveBridgePlatform.setOtpPackageRules(_otpRulesController.text);
        await LiveBridgePlatform.setOtpPackageMode(_otpPackageMode.id);
      } else {
        await LiveBridgePlatform.setPackageRules(_rulesController.text);
        await LiveBridgePlatform.setPackageMode(_packageMode.id);
      }
    } catch (_) {
      if (mounted) {
        _snack(AppStrings.of(context).saveFailed);
      }
    }
  }

  Future<void> _setOnlyWithProgress(bool value) async {
    HapticFeedback.selectionClick();
    setState(() => _onlyWithProgress = value);
    await LiveBridgePlatform.setOnlyWithProgress(value);
  }

  Future<void> _setTextProgressEnabled(bool value) async {
    HapticFeedback.selectionClick();
    setState(() => _textProgressEnabled = value);
    await LiveBridgePlatform.setTextProgressEnabled(value);
  }

  Future<void> _setConverterEnabled(bool value) async {
    if (!_canToggleMaster) return;
    HapticFeedback.selectionClick();
    setState(() => _converterEnabled = value);
    await LiveBridgePlatform.setConverterEnabled(value);
  }

  Future<void> _setKeepAliveForeground(bool value) async {
    HapticFeedback.selectionClick();
    setState(() => _keepAliveForegroundEnabled = value);
    await LiveBridgePlatform.setKeepAliveForegroundEnabled(value);
  }

  Future<void> _setAospCutting(bool value) async {
    HapticFeedback.selectionClick();
    setState(() => _aospCuttingEnabled = value);
    await LiveBridgePlatform.setAospCuttingEnabled(value);
  }

  Future<void> _setSamsungRemoteReparserEnabled(bool value) async {
    HapticFeedback.selectionClick();
    setState(() => _samsungRemoteReparserEnabled = value);
    await LiveBridgePlatform.setSamsungRemoteReparserEnabled(value);
  }

  Future<void> _setUpdateChecksEnabled(bool value) async {
    HapticFeedback.selectionClick();
    setState(() => _updateChecksEnabled = value);
    await LiveBridgePlatform.setUpdateChecksEnabled(value);
    if (value) {
      await _checkForUpdatesIfNeeded(force: true);
    }
  }

  Future<void> _checkForUpdatesIfNeeded({bool force = false}) async {
    if (_isCheckingUpdates) {
      return;
    }
    _isCheckingUpdates = true;

    try {
      final bool checksEnabled =
          await LiveBridgePlatform.getUpdateChecksEnabled();
      if (!checksEnabled) {
        return;
      }

      final int nowMs = DateTime.now().millisecondsSinceEpoch;
      final int lastCheckAtMs =
          await LiveBridgePlatform.getUpdateLastCheckAtMs();
      if (!force &&
          lastCheckAtMs > 0 &&
          nowMs - lastCheckAtMs < _updateCheckInterval.inMilliseconds) {
        return;
      }

      await LiveBridgePlatform.setUpdateLastCheckAtMs(nowMs);

      final _GithubReleaseInfo? latest = await _fetchLatestRelease();
      if (latest == null) {
        return;
      }

      final String currentVersion = _currentAppVersion.isNotEmpty
          ? _currentAppVersion
          : await LiveBridgePlatform.getAppVersionName();
      final bool hasUpdate = _isReleaseNewer(
        currentVersion: currentVersion,
        latestVersion: latest.version,
      );

      await LiveBridgePlatform.setUpdateCachedLatestVersion(latest.version);
      await LiveBridgePlatform.setUpdateCachedAvailable(hasUpdate);

      if (!mounted) {
        return;
      }
      setState(() {
        _currentAppVersion = currentVersion;
        _latestReleaseVersion = latest.version;
        _updateAvailable = hasUpdate;
      });

      if (!hasUpdate) {
        return;
      }

      final String lastNotifiedVersion =
          await LiveBridgePlatform.getUpdateLastNotifiedVersion();
      if (lastNotifiedVersion == latest.version) {
        return;
      }

      final bool notified =
          await LiveBridgePlatform.showUpdateAvailableNotification(
            version: latest.version,
            releaseUrl: latest.htmlUrl,
          );
      if (notified) {
        await LiveBridgePlatform.setUpdateLastNotifiedVersion(latest.version);
      }
    } catch (_) {
    } finally {
      _isCheckingUpdates = false;
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
        _currentAppVersion.isNotEmpty
            ? 'LiveBridge/${_currentAppVersion.trim()}'
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
      final String htmlUrl = (data['html_url'] as String?)?.trim() ?? '';
      final String version = tag.isNotEmpty ? tag : name;
      if (version.isEmpty) {
        return null;
      }

      return _GithubReleaseInfo(
        version: version,
        htmlUrl: htmlUrl.isNotEmpty ? htmlUrl : _projectGithubReleasesUrl,
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
    if (latestParts.isEmpty) {
      return false;
    }
    if (currentParts.isEmpty) {
      return false;
    }

    final int maxLen = currentParts.length > latestParts.length
        ? currentParts.length
        : latestParts.length;
    for (int i = 0; i < maxLen; i++) {
      final int current = i < currentParts.length ? currentParts[i] : 0;
      final int latest = i < latestParts.length ? latestParts[i] : 0;
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

  Future<void> _showMasterBlockedFeedback() async {
    if (_canToggleMaster) return;

    _masterBlockedShakeController.forward(from: 0);
    if (_masterBlockedHapticInProgress) return;

    _masterBlockedHapticInProgress = true;
    try {
      for (int i = 0; i < 3; i++) {
        HapticFeedback.selectionClick();
        await Future<void>.delayed(const Duration(milliseconds: 75));
      }
    } finally {
      _masterBlockedHapticInProgress = false;
    }
  }

  Future<void> _setSmartDetection(bool value) async {
    HapticFeedback.selectionClick();
    setState(() => _smartDetectionEnabled = value);
    await LiveBridgePlatform.setSmartStatusDetectionEnabled(value);
  }

  Future<void> _setSmartNavigation(bool value) async {
    HapticFeedback.selectionClick();
    setState(() => _smartNavigationEnabled = value);
    await LiveBridgePlatform.setSmartNavigationEnabled(value);
  }

  Future<void> _setOtpDetection(bool value) async {
    HapticFeedback.selectionClick();
    setState(() => _otpDetectionEnabled = value);
    await LiveBridgePlatform.setOtpDetectionEnabled(value);
  }

  Future<void> _setOtpAutoCopy(bool value) async {
    HapticFeedback.selectionClick();
    setState(() => _otpAutoCopyEnabled = value);
    await LiveBridgePlatform.setOtpAutoCopyEnabled(value);
  }

  Set<String> _parsePackagesFromInput(String value) {
    return value
        .split(RegExp(r'[,\n;\s]+'))
        .map((String e) => e.trim().toLowerCase())
        .where((String e) => e.isNotEmpty)
        .toSet();
  }

  Future<void> _openPackagePicker({required bool otpMode}) async {
    if (!await _ensureAppListAccess()) return;
    HapticFeedback.lightImpact();

    final List<InstalledApp> apps = await LiveBridgePlatform.getInstalledApps();
    if (!mounted || apps.isEmpty) {
      if (mounted) _snack(AppStrings.of(context).appsLoadFailed);
      return;
    }

    final TextEditingController target = otpMode
        ? _otpRulesController
        : _rulesController;
    final Set<String> initial = _parsePackagesFromInput(target.text);

    final Set<String>? selected = await showModalBottomSheet<Set<String>>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(32)),
      ),
      builder: (BuildContext context) {
        return PackagePickerSheet(
          title: otpMode
              ? AppStrings.of(context).otpPickerTitle
              : AppStrings.of(context).pickerTitle,
          apps: apps,
          initialSelected: initial,
          applyLabel: AppStrings.of(context).applySelection,
          searchHint: AppStrings.of(context).searchAppHint,
        );
      },
    );

    if (!mounted || selected == null) return;

    final List<String> values = selected.toList()..sort();
    setState(() {
      target.text = values.join('\n');
    });
    await _persistRules(otpMode: otpMode);
  }

  Future<void> _openAppPresentationSettings() async {
    if (!await _ensureAppListAccess()) return;
    if (!mounted) return;
    HapticFeedback.selectionClick();
    await Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (BuildContext context) => const AppPresentationSettingsPage(),
      ),
    );
  }

  Future<bool> _ensureAppListAccess() async {
    final bool alreadyGranted =
        await LiveBridgePlatform.getAppListAccessGranted();
    if (alreadyGranted) return true;
    if (!mounted) return false;

    final AppStrings s = AppStrings.of(context);
    final bool granted =
        await showDialog<bool>(
          context: context,
          builder: (BuildContext dialogContext) {
            return AlertDialog(
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(24),
              ),
              title: Text(s.appsAccessTitle),
              content: Text(s.appsAccessMessage),
              actions: <Widget>[
                TextButton(
                  onPressed: () => Navigator.of(dialogContext).pop(false),
                  child: Text(s.cancel),
                ),
                FilledButton(
                  onPressed: () => Navigator.of(dialogContext).pop(true),
                  child: Text(s.allow),
                ),
              ],
            );
          },
        ) ??
        false;

    if (!granted) return false;

    final bool saved = await LiveBridgePlatform.setAppListAccessGranted(true);
    if (!saved && mounted) _snack(s.appsAccessSaveFailed);
    return true;
  }

  Future<void> _requestNotificationPermission() async {
    final bool granted =
        await LiveBridgePlatform.requestNotificationPermission();
    if (!mounted) return;
    final AppStrings s = AppStrings.of(context);
    _snack(granted ? s.permissionGranted : s.permissionDenied);
    await _refreshState();
  }

  Future<void> _openListenerSettings() async {
    final bool opened =
        await LiveBridgePlatform.openNotificationListenerSettings();
    if (!mounted || opened) return;
    _snack(AppStrings.of(context).listenerUnavailable);
  }

  Future<void> _openAppNotificationSettings() async {
    final bool opened = await LiveBridgePlatform.openAppNotificationSettings();
    if (!mounted || opened) return;
    _snack(AppStrings.of(context).notificationsUnavailable);
  }

  Future<void> _openPromotedSettings() async {
    final bool opened =
        await LiveBridgePlatform.openPromotedNotificationSettings();
    if (!mounted || opened) return;
    _snack(AppStrings.of(context).liveUpdatesUnavailable);
  }

  Future<void> _acknowledgeBlockedJoke() async {
    final bool saved = await LiveBridgePlatform.setPixelJokeBypassEnabled(true);
    if (!mounted) return;
    if (!saved) {
      _snack(AppStrings.of(context).blockedBypassSaveFailed);
      return;
    }
    await _refreshState();
  }

  Future<void> _downloadParserDictionary() async {
    if (_dictionaryActionInProgress) return;
    HapticFeedback.selectionClick();
    setState(() => _dictionaryActionInProgress = true);
    final AppStrings s = AppStrings.of(context);

    try {
      final String savedUri =
          await LiveBridgePlatform.saveParserDictionaryToDownloads();
      if (savedUri.trim().isEmpty) {
        _snack(s.dictionaryDownloadFailed);
      } else {
        _snack(s.dictionarySaved);
      }
    } catch (_) {
      if (mounted) _snack(s.dictionaryDownloadFailed);
    } finally {
      if (mounted) setState(() => _dictionaryActionInProgress = false);
    }
  }

  Future<void> _updateParserDictionaryFromGithub() async {
    if (_dictionaryActionInProgress) return;
    HapticFeedback.selectionClick();
    setState(() => _dictionaryActionInProgress = true);
    final AppStrings s = AppStrings.of(context);
    final HttpClient client = HttpClient()
      ..connectionTimeout = const Duration(seconds: 8);

    try {
      final HttpClientRequest request = await client.getUrl(
        Uri.parse(_dictionaryRawUrl),
      );
      request.headers.set(HttpHeaders.acceptHeader, 'application/json');
      request.headers.set(
        HttpHeaders.userAgentHeader,
        _currentAppVersion.isNotEmpty
            ? 'LiveBridge/${_currentAppVersion.trim()}'
            : 'LiveBridge/dictionary-update',
      );

      final HttpClientResponse response = await request.close();
      if (response.statusCode != HttpStatus.ok) {
        _snack(s.dictionaryUpdateFailed);
        return;
      }

      final String raw = (await utf8.decoder.bind(response).join()).trim();
      if (raw.isEmpty) {
        _snack(s.dictionaryEmpty);
        return;
      }
      final dynamic decoded = jsonDecode(raw);
      if (decoded is! Map) {
        _snack(s.dictionaryInvalid);
        return;
      }

      final bool saved = await LiveBridgePlatform.setCustomParserDictionary(
        raw,
      );
      if (!mounted) return;
      if (saved) {
        setState(() => _hasCustomParserDictionary = true);
        _snack(s.dictionaryUpdateDone);
      } else {
        _snack(s.dictionaryUpdateFailed);
      }
    } catch (_) {
      if (mounted) _snack(s.dictionaryUpdateFailed);
    } finally {
      client.close(force: true);
      if (mounted) setState(() => _dictionaryActionInProgress = false);
    }
  }

  Future<void> _uploadParserDictionary() async {
    if (_dictionaryActionInProgress) return;
    HapticFeedback.selectionClick();
    setState(() => _dictionaryActionInProgress = true);
    final AppStrings s = AppStrings.of(context);

    try {
      final FilePickerResult? result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: const <String>['json'],
        withData: true,
      );
      if (result == null || result.files.isEmpty) {
        return;
      }

      final PlatformFile selected = result.files.first;
      String raw = '';
      final Uint8List? bytes = selected.bytes;
      if (bytes != null) {
        raw = utf8.decode(bytes);
      } else if ((selected.path ?? '').isNotEmpty) {
        raw = await File(selected.path!).readAsString();
      }

      if (raw.trim().isEmpty) {
        _snack(s.dictionaryEmpty);
        return;
      }

      final bool saved = await LiveBridgePlatform.setCustomParserDictionary(
        raw,
      );
      if (!mounted) return;
      if (saved) {
        setState(() => _hasCustomParserDictionary = true);
        _snack(s.dictionaryUploadDone);
      } else {
        _snack(s.dictionaryUploadFailed);
      }
    } on PlatformException catch (error) {
      if (mounted) {
        _snack(
          error.code == 'invalid_dictionary'
              ? s.dictionaryInvalid
              : s.dictionaryUploadFailed,
        );
      }
    } catch (_) {
      if (mounted) _snack(s.dictionaryUploadFailed);
    } finally {
      if (mounted) setState(() => _dictionaryActionInProgress = false);
    }
  }

  Future<void> _resetParserDictionary() async {
    if (_dictionaryActionInProgress) return;
    HapticFeedback.selectionClick();
    setState(() => _dictionaryActionInProgress = true);
    final AppStrings s = AppStrings.of(context);
    try {
      final bool cleared =
          await LiveBridgePlatform.clearCustomParserDictionary();
      if (!mounted) return;
      if (cleared) {
        setState(() => _hasCustomParserDictionary = false);
        _snack(s.dictionaryResetDone);
      } else {
        _snack(s.dictionaryResetFailed);
      }
    } catch (_) {
      if (mounted) _snack(s.dictionaryResetFailed);
    } finally {
      if (mounted) setState(() => _dictionaryActionInProgress = false);
    }
  }

  Future<void> _openGithub() async {
    final Uri uri = Uri.parse(
      _hasUpdateAlert ? _projectGithubReleasesUrl : _projectGithubUrl,
    );
    final bool opened = await launchUrl(
      uri,
      mode: LaunchMode.externalApplication,
    );
    if (!opened && mounted) {
      _snack(AppStrings.of(context).githubOpenFailed);
    }
  }

  Future<void> _hideBackgroundWarning() async {
    HapticFeedback.selectionClick();
    await LiveBridgePlatform.setBackgroundWarningDismissed(true);
    if (!mounted) return;
    setState(() => _showBackgroundWarning = false);
  }

  Future<void> _hideSamsungWarning() async {
    HapticFeedback.selectionClick();
    await LiveBridgePlatform.setSamsungWarningDismissed(true);
    if (!mounted) return;
    setState(() => _showSamsungDeveloperWarning = false);
  }

  void _snack(String value) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(value)));
  }

  Set<String> _parseExpandedSections(String raw) {
    const Set<String> known = <String>{
      'access',
      'rules',
      'smart',
      'otp',
      'settings',
    };
    return raw
        .split(RegExp(r'[,\s;\n\r\t]+'))
        .map((String e) => e.trim())
        .where((String e) => known.contains(e))
        .toSet();
  }

  String _serializeExpandedSections() {
    final List<String> sorted = _expandedSections.toList()..sort();
    return sorted.join(',');
  }

  Future<void> _persistExpandedSections() async {
    try {
      await LiveBridgePlatform.setExpandedSections(
        _serializeExpandedSections(),
      );
    } catch (_) {}
  }

  void _toggleSection(String sectionId) {
    HapticFeedback.selectionClick();
    setState(() {
      if (_expandedSections.contains(sectionId)) {
        _expandedSections.remove(sectionId);
      } else {
        _expandedSections.add(sectionId);
      }
      _hasPersistedExpandedSections = true;
    });
    unawaited(_persistExpandedSections());
  }

  String _getModeLabel(PackageMode mode, AppStrings s) {
    switch (mode) {
      case PackageMode.all:
        return s.modeAll;
      case PackageMode.include:
        return s.modeInclude;
      case PackageMode.exclude:
        return s.modeExclude;
    }
  }

  @override
  Widget build(BuildContext context) {
    final AppStrings s = AppStrings.of(context);

    return Scaffold(
      body: SafeArea(
        top: false,
        bottom: false,
        child: _isLoading
            ? const Center(child: CircularProgressIndicator())
            : _deviceBlocked
            ? _buildBlockedScreen(s)
            : RefreshIndicator(
                onRefresh: () => _refreshState(showLoading: false),
                child: ListView(
                  physics: const AlwaysScrollableScrollPhysics(
                    parent: BouncingScrollPhysics(),
                  ),
                  padding: EdgeInsets.only(
                    top: MediaQuery.of(context).padding.top + 16,
                    left: 16,
                    right: 16,
                    bottom: MediaQuery.of(context).padding.bottom + 32,
                  ),
                  children: <Widget>[
                    _buildHero(s),
                    if (_showBackgroundWarning) ...<Widget>[
                      const SizedBox(height: 16),
                      _buildBackgroundWarning(s),
                    ],
                    if (_showSamsungDeveloperWarning) ...<Widget>[
                      const SizedBox(height: 16),
                      _buildSamsungDeveloperWarning(s),
                    ],
                    const SizedBox(height: 24),
                    _buildAccessCard(s),
                    const SizedBox(height: 24),
                    _buildRulesCard(s),
                    const SizedBox(height: 24),
                    _buildSmartCard(s),
                    const SizedBox(height: 24),
                    _buildOtpCard(s),
                    const SizedBox(height: 24),
                    _buildSettingsCard(s),
                  ],
                ),
              ),
      ),
    );
  }

  Widget _buildBlockedScreen(AppStrings s) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;

    return Padding(
      padding: const EdgeInsets.all(32),
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 400),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              Stack(
                alignment: Alignment.center,
                children: <Widget>[
                  Container(
                    width: 120,
                    height: 120,
                    decoration: BoxDecoration(
                      color: colorScheme.errorContainer.withValues(alpha: 0.3),
                      shape: BoxShape.circle,
                    ),
                  ),
                  Icon(
                    Icons.phone_android_rounded,
                    size: 64,
                    color: colorScheme.error,
                  ),
                ],
              ),
              const SizedBox(height: 32),
              Text(
                s.blockedTitle,
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 16),
              Text(
                s.blockedSubtitle,
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                  color: colorScheme.onSurfaceVariant,
                ),
              ),
              const SizedBox(height: 48),
              SizedBox(
                width: double.infinity,
                child: FilledButton.icon(
                  onPressed: _acknowledgeBlockedJoke,
                  icon: const Icon(Icons.visibility_off_outlined),
                  label: Text(
                    s.blockedBypassAction,
                    textAlign: TextAlign.center,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildHero(AppStrings s) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;

    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(32),
        color: colorScheme.primary,
      ),
      child: Padding(
        padding: const EdgeInsets.all(28),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  s.heroTitle,
                  style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                    fontWeight: FontWeight.w900,
                    color: colorScheme.onPrimary,
                    letterSpacing: -0.5,
                  ),
                ),
                AnimatedBuilder(
                  animation: _masterBlockedShakeOffset,
                  builder: (BuildContext context, Widget? child) {
                    return Transform.translate(
                      offset: Offset(_masterBlockedShakeOffset.value, 0),
                      child: child,
                    );
                  },
                  child: GestureDetector(
                    behavior: HitTestBehavior.opaque,
                    onTap: _canToggleMaster ? null : _showMasterBlockedFeedback,
                    child: Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 6,
                        vertical: 2,
                      ),
                      decoration: BoxDecoration(
                        color: colorScheme.onPrimary.withValues(alpha: 0.14),
                        borderRadius: BorderRadius.circular(999),
                      ),
                      child: Switch.adaptive(
                        value: _effectiveConverterEnabled,
                        onChanged: _canToggleMaster
                            ? _setConverterEnabled
                            : null,
                        thumbIcon: WidgetStateProperty.resolveWith<Icon?>((
                          Set<WidgetState> states,
                        ) {
                          if (states.contains(WidgetState.selected)) {
                            return Icon(
                              Icons.bolt_rounded,
                              size: 17,
                              color: colorScheme.onPrimary,
                            );
                          }
                          return const Icon(Icons.flash_off_rounded, size: 14);
                        }),
                        activeThumbColor: Theme.of(context).colorScheme.primary,
                      ),
                    ),
                  ),
                ),
              ],
            ),
            if (!_canToggleMaster) ...<Widget>[
              const SizedBox(height: 8),
              Text(
                s.masterToggleLockedHint,
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color: colorScheme.onPrimary.withValues(alpha: 0.9),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildSettingsCard(AppStrings s) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;

    return _sectionPanel(
      sectionId: 'settings',
      title: s.settingsTitle,
      icon: Icons.settings_rounded,
      collapsedStatusOk: _hasUpdateAlert ? false : null,
      child: Column(
        children: <Widget>[
          SwitchListTile.adaptive(
            value: _keepAliveForegroundEnabled,
            onChanged: _setKeepAliveForeground,
            title: Text(
              s.keepAliveForegroundTitle,
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
            subtitle: Text(
              _effectiveConverterEnabled
                  ? s.keepAliveForegroundSubtitle
                  : s.keepAliveForegroundInactiveSubtitle,
              style: TextStyle(
                color: colorScheme.onSurfaceVariant,
                fontSize: 13,
              ),
            ),
            contentPadding: EdgeInsets.zero,
            activeThumbColor: colorScheme.primary,
          ),
          const SizedBox(height: 8),
          SwitchListTile.adaptive(
            value: _updateChecksEnabled,
            onChanged: _setUpdateChecksEnabled,
            title: Text(
              s.updateChecksTitle,
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
            subtitle: Text(
              s.updateChecksSubtitle,
              style: TextStyle(
                color: colorScheme.onSurfaceVariant,
                fontSize: 13,
              ),
            ),
            contentPadding: EdgeInsets.zero,
            activeThumbColor: colorScheme.primary,
          ),
          if (_isSamsungDevice) ...<Widget>[
            const SizedBox(height: 8),
            SwitchListTile.adaptive(
              value: _samsungRemoteReparserEnabled,
              onChanged: _setSamsungRemoteReparserEnabled,
              title: Text(
                s.samsungRemoteParserTitle,
                style: const TextStyle(fontWeight: FontWeight.w600),
              ),
              subtitle: Text(
                s.samsungRemoteParserSubtitle,
                style: TextStyle(
                  color: colorScheme.onSurfaceVariant,
                  fontSize: 13,
                ),
              ),
              contentPadding: EdgeInsets.zero,
              activeThumbColor: colorScheme.primary,
            ),
          ],
          if (_isAospDevice) ...<Widget>[
            const SizedBox(height: 8),
            SwitchListTile.adaptive(
              value: _aospCuttingEnabled,
              onChanged: _setAospCutting,
              title: Text(
                s.aospCuttingTitle,
                style: const TextStyle(fontWeight: FontWeight.w600),
              ),
              subtitle: Text(
                s.aospCuttingSubtitle,
                style: TextStyle(
                  color: colorScheme.onSurfaceVariant,
                  fontSize: 13,
                ),
              ),
              contentPadding: EdgeInsets.zero,
              activeThumbColor: colorScheme.primary,
            ),
          ],
          const SizedBox(height: 12),
          SizedBox(
            width: double.infinity,
            child: OutlinedButton.icon(
              onPressed: _openAppPresentationSettings,
              icon: const Icon(Icons.tune_rounded, size: 18),
              label: Text(s.appPresentationSettings),
            ),
          ),
          const SizedBox(height: 10),
          SizedBox(
            width: double.infinity,
            child: FilledButton.icon(
              onPressed: _dictionaryActionInProgress
                  ? null
                  : _updateParserDictionaryFromGithub,
              icon: const Icon(Icons.system_update_alt_rounded, size: 18),
              label: Text(s.updateDictionary),
            ),
          ),
          const SizedBox(height: 10),
          SizedBox(
            width: double.infinity,
            child: FilledButton.tonalIcon(
              onPressed: _dictionaryActionInProgress
                  ? null
                  : _downloadParserDictionary,
              icon: const Icon(Icons.download_rounded, size: 18),
              label: Text(s.downloadDictionary),
            ),
          ),
          const SizedBox(height: 10),
          SizedBox(
            width: double.infinity,
            child: OutlinedButton.icon(
              onPressed: _dictionaryActionInProgress
                  ? null
                  : _uploadParserDictionary,
              icon: const Icon(Icons.upload_file_rounded, size: 18),
              label: Text(s.uploadDictionary),
            ),
          ),
          if (_hasCustomParserDictionary) ...<Widget>[
            const SizedBox(height: 8),
            Align(
              alignment: Alignment.centerRight,
              child: TextButton.icon(
                onPressed: _dictionaryActionInProgress
                    ? null
                    : _resetParserDictionary,
                icon: const Icon(Icons.restart_alt_rounded, size: 18),
                label: Text(s.resetDictionary),
              ),
            ),
          ],
          const SizedBox(height: 8),
          if (_hasUpdateAlert) ...<Widget>[
            Align(
              alignment: Alignment.centerLeft,
              child: Container(
                width: double.infinity,
                padding: const EdgeInsets.symmetric(
                  horizontal: 12,
                  vertical: 8,
                ),
                decoration: BoxDecoration(
                  color: colorScheme.errorContainer.withValues(alpha: 0.7),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(
                    color: colorScheme.error.withValues(alpha: 0.24),
                  ),
                ),
                child: Text(
                  s.updateAvailableBanner(_latestReleaseVersion),
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: colorScheme.error,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
            ),
            const SizedBox(height: 8),
          ],
          InkWell(
            borderRadius: BorderRadius.circular(14),
            onTap: _openGithub,
            child: Container(
              width: double.infinity,
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
              decoration: BoxDecoration(
                color: _hasUpdateAlert
                    ? colorScheme.errorContainer.withValues(alpha: 0.55)
                    : colorScheme.surfaceContainerHighest.withValues(
                        alpha: 0.3,
                      ),
                borderRadius: BorderRadius.circular(14),
                border: _hasUpdateAlert
                    ? Border.all(
                        color: colorScheme.error.withValues(alpha: 0.28),
                        width: 1.1,
                      )
                    : null,
              ),
              child: Row(
                children: <Widget>[
                  Icon(
                    Icons.code_rounded,
                    size: 20,
                    color: _hasUpdateAlert
                        ? colorScheme.error
                        : colorScheme.onSurfaceVariant,
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      _hasUpdateAlert ? s.githubReleasesUrl : s.githubUrl,
                      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                        fontWeight: FontWeight.w600,
                        color: _hasUpdateAlert ? colorScheme.error : null,
                      ),
                    ),
                  ),
                  Icon(
                    Icons.open_in_new_rounded,
                    size: 18,
                    color: _hasUpdateAlert
                        ? colorScheme.error
                        : colorScheme.onSurfaceVariant,
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBackgroundWarning(AppStrings s) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: colorScheme.errorContainer.withValues(alpha: 0.5),
        borderRadius: BorderRadius.circular(24),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: colorScheme.error.withValues(alpha: 0.1),
              shape: BoxShape.circle,
            ),
            child: Icon(Icons.warning_amber_rounded, color: colorScheme.error),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  s.backgroundWarningTitle,
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  s.backgroundWarningBody(_deviceLabelForWarning),
                  style: Theme.of(
                    context,
                  ).textTheme.bodySmall?.copyWith(height: 1.4),
                ),
                const SizedBox(height: 10),
                Align(
                  alignment: Alignment.centerRight,
                  child: TextButton.icon(
                    onPressed: _hideBackgroundWarning,
                    icon: const Icon(Icons.visibility_off_rounded, size: 18),
                    label: Text(s.hideWarningBanner),
                    style: TextButton.styleFrom(
                      foregroundColor: colorScheme.error,
                      visualDensity: VisualDensity.compact,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSamsungDeveloperWarning(AppStrings s) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: colorScheme.errorContainer.withValues(alpha: 0.5),
        borderRadius: BorderRadius.circular(24),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: colorScheme.error.withValues(alpha: 0.1),
              shape: BoxShape.circle,
            ),
            child: Icon(Icons.warning_amber_rounded, color: colorScheme.error),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  s.samsungWarningTitle,
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  s.samsungWarningBody,
                  style: Theme.of(
                    context,
                  ).textTheme.bodySmall?.copyWith(height: 1.4),
                ),
                const SizedBox(height: 10),
                Align(
                  alignment: Alignment.centerRight,
                  child: TextButton.icon(
                    onPressed: _hideSamsungWarning,
                    icon: const Icon(Icons.visibility_off_rounded, size: 18),
                    label: Text(s.hideWarningBanner),
                    style: TextButton.styleFrom(
                      foregroundColor: colorScheme.error,
                      visualDensity: VisualDensity.compact,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildAccessCard(AppStrings s) {
    return _sectionPanel(
      sectionId: 'access',
      title: s.accessTitle,
      icon: Icons.admin_panel_settings_rounded,
      collapsedStatusOk: _hasAllAccessPermissions,
      child: Column(
        children: <Widget>[
          _statusRow(
            label: s.listenerAccess,
            enabled: _listenerEnabled,
            actionLabel: s.open,
            onPressed: _openListenerSettings,
          ),
          _statusRow(
            label: s.postNotifications,
            enabled: _notificationsGranted,
            actionLabel: _notificationsGranted ? s.open : s.request,
            onPressed: _notificationsGranted
                ? _openAppNotificationSettings
                : _requestNotificationPermission,
          ),
          if (!_hidePromotedAccess)
            _statusRow(
              label: s.liveUpdatesAccess,
              enabled: _canPostPromoted,
              actionLabel: s.open,
              onPressed: _openPromotedSettings,
            ),
        ],
      ),
    );
  }

  Widget _buildModernDropdown({
    required String label,
    required PackageMode currentValue,
    required void Function(PackageMode?) onChanged,
    VoidCallback? onTap,
    required AppStrings s,
  }) {
    final colorScheme = Theme.of(context).colorScheme;

    return DropdownButtonFormField<PackageMode>(
      initialValue: currentValue,
      onTap: onTap,
      onChanged: onChanged,
      isExpanded: true,
      icon: Icon(
        Icons.keyboard_arrow_down_rounded,
        color: colorScheme.onSurfaceVariant,
      ),
      dropdownColor: colorScheme.surfaceContainer,
      borderRadius: BorderRadius.circular(24),
      decoration: InputDecoration(
        labelText: label,
        labelStyle: TextStyle(
          fontSize: 14,
          fontWeight: FontWeight.w500,
          color: colorScheme.onSurfaceVariant,
        ),
        filled: true,
        fillColor: colorScheme.surfaceContainerHighest.withValues(alpha: 0.3),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(20),
          borderSide: BorderSide.none,
        ),
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 20,
          vertical: 16,
        ),
      ),
      items: PackageMode.values.map((mode) {
        return DropdownMenuItem<PackageMode>(
          value: mode,
          child: Text(
            _getModeLabel(mode, s),
            style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w500),
          ),
        );
      }).toList(),
    );
  }

  Widget _buildRulesCard(AppStrings s) {
    return _sectionPanel(
      sectionId: 'rules',
      title: s.rulesTitle,
      icon: Icons.rule_folder_rounded,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          _buildModernDropdown(
            label: s.modeLabel,
            currentValue: _packageMode,
            onChanged: (val) {
              if (val != null) {
                HapticFeedback.selectionClick();
                setState(() => _packageMode = val);
                unawaited(_persistRules(otpMode: false));
              }
            },
            onTap: () => HapticFeedback.lightImpact(),
            s: s,
          ),
          const SizedBox(height: 16),
          _selectedAppsNote(
            selectedCount: _parsePackagesFromInput(
              _rulesController.text,
            ).length,
            s: s,
          ),
          const SizedBox(height: 8),
          Text(
            s.pickAppsHint,
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
              color: Theme.of(context).colorScheme.onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 16),
          _ruleButtonsRow(
            onPick: () => _openPackagePicker(otpMode: false),
            s: s,
          ),
          const Padding(
            padding: EdgeInsets.symmetric(vertical: 16),
            child: Divider(height: 1),
          ),
          SwitchListTile.adaptive(
            value: _onlyWithProgress,
            onChanged: _setOnlyWithProgress,
            title: Text(
              s.onlyProgressTitle,
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
            subtitle: Text(
              s.onlyProgressSubtitle,
              style: TextStyle(
                color: Theme.of(context).colorScheme.onSurfaceVariant,
                fontSize: 13,
              ),
            ),
            contentPadding: EdgeInsets.zero,
            activeThumbColor: Theme.of(context).colorScheme.primary,
          ),
          const SizedBox(height: 8),
          SwitchListTile.adaptive(
            value: _textProgressEnabled,
            onChanged: _setTextProgressEnabled,
            title: Text(
              s.textProgressTitle,
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
            subtitle: Text(
              s.textProgressSubtitle,
              style: TextStyle(
                color: Theme.of(context).colorScheme.onSurfaceVariant,
                fontSize: 13,
              ),
            ),
            contentPadding: EdgeInsets.zero,
            activeThumbColor: Theme.of(context).colorScheme.primary,
          ),
        ],
      ),
    );
  }

  Widget _buildSmartCard(AppStrings s) {
    return _sectionPanel(
      sectionId: 'smart',
      title: s.smartCardTitle,
      icon: Icons.auto_awesome_rounded,
      child: Column(
        children: <Widget>[
          SwitchListTile.adaptive(
            value: _smartDetectionEnabled,
            onChanged: _setSmartDetection,
            title: Text(
              s.smartDetectionTitle,
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
            subtitle: Text(
              s.smartDetectionSubtitle,
              style: TextStyle(
                color: Theme.of(context).colorScheme.onSurfaceVariant,
                fontSize: 13,
              ),
            ),
            contentPadding: EdgeInsets.zero,
            activeThumbColor: Theme.of(context).colorScheme.primary,
          ),
          const SizedBox(height: 8),
          SwitchListTile.adaptive(
            value: _smartNavigationEnabled,
            onChanged: _smartDetectionEnabled ? _setSmartNavigation : null,
            title: Text(
              s.smartNavigationTitle,
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
            subtitle: Text(
              _smartDetectionEnabled
                  ? s.smartNavigationSubtitle
                  : s.smartNavigationDisabledSubtitle,
              style: TextStyle(
                color: Theme.of(context).colorScheme.onSurfaceVariant,
                fontSize: 13,
              ),
            ),
            contentPadding: EdgeInsets.zero,
            activeThumbColor: Theme.of(context).colorScheme.primary,
          ),
        ],
      ),
    );
  }

  Widget _buildOtpCard(AppStrings s) {
    return _sectionPanel(
      sectionId: 'otp',
      title: s.otpTitle,
      icon: Icons.password_rounded,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          SwitchListTile.adaptive(
            value: _otpDetectionEnabled,
            onChanged: _setOtpDetection,
            title: Text(
              s.otpEnabledTitle,
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
            subtitle: Text(
              s.otpEnabledSubtitle,
              style: TextStyle(
                color: Theme.of(context).colorScheme.onSurfaceVariant,
                fontSize: 13,
              ),
            ),
            contentPadding: EdgeInsets.zero,
            activeThumbColor: Theme.of(context).colorScheme.primary,
          ),
          const SizedBox(height: 8),
          SwitchListTile.adaptive(
            value: _otpAutoCopyEnabled,
            onChanged: _otpDetectionEnabled ? _setOtpAutoCopy : null,
            title: Text(
              s.otpAutoCopyTitle,
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
            subtitle: Text(
              _otpDetectionEnabled
                  ? s.otpAutoCopySubtitle
                  : s.otpAutoCopyDisabledSubtitle,
              style: TextStyle(
                color: Theme.of(context).colorScheme.onSurfaceVariant,
                fontSize: 13,
              ),
            ),
            contentPadding: EdgeInsets.zero,
            activeThumbColor: Theme.of(context).colorScheme.primary,
          ),
          const Padding(
            padding: EdgeInsets.symmetric(vertical: 16),
            child: Divider(height: 1),
          ),
          AnimatedOpacity(
            duration: const Duration(milliseconds: 200),
            opacity: _otpDetectionEnabled ? 1 : 0.4,
            child: IgnorePointer(
              ignoring: !_otpDetectionEnabled,
              child: Column(
                children: <Widget>[
                  _buildModernDropdown(
                    label: s.otpModeLabel,
                    currentValue: _otpPackageMode,
                    onChanged: (val) {
                      if (val != null) {
                        HapticFeedback.selectionClick();
                        setState(() => _otpPackageMode = val);
                        unawaited(_persistRules(otpMode: true));
                      }
                    },
                    onTap: () => HapticFeedback.lightImpact(),
                    s: s,
                  ),
                  const SizedBox(height: 16),
                  _selectedAppsNote(
                    selectedCount: _parsePackagesFromInput(
                      _otpRulesController.text,
                    ).length,
                    s: s,
                  ),
                  const SizedBox(height: 8),
                  Text(
                    s.pickAppsHint,
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                      color: Theme.of(context).colorScheme.onSurfaceVariant,
                    ),
                  ),
                  const SizedBox(height: 16),
                  _ruleButtonsRow(
                    onPick: () => _openPackagePicker(otpMode: true),
                    s: s,
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _statusRow({
    required String label,
    required bool enabled,
    required String actionLabel,
    required VoidCallback onPressed,
  }) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;
    const Color successColor = Color(0xFF22C55E);

    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        decoration: BoxDecoration(
          color: colorScheme.surfaceContainerHighest.withValues(alpha: 0.3),
          borderRadius: BorderRadius.circular(20),
        ),
        child: Row(
          children: <Widget>[
            Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: enabled
                    ? successColor.withValues(alpha: 0.16)
                    : colorScheme.error.withValues(alpha: 0.1),
                shape: BoxShape.circle,
              ),
              child: Icon(
                enabled ? Icons.check_rounded : Icons.close_rounded,
                size: 20,
                color: enabled ? successColor : colorScheme.error,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Text(
                label,
                style: const TextStyle(fontWeight: FontWeight.w500),
              ),
            ),
            FilledButton.tonal(
              onPressed: onPressed,
              style: FilledButton.styleFrom(
                minimumSize: const Size(0, 36),
                padding: const EdgeInsets.symmetric(horizontal: 16),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
              child: Text(actionLabel),
            ),
          ],
        ),
      ),
    );
  }

  Widget _ruleButtonsRow({
    required VoidCallback onPick,
    required AppStrings s,
  }) {
    return SizedBox(
      width: double.infinity,
      child: OutlinedButton.icon(
        onPressed: onPick,
        icon: const Icon(Icons.apps_rounded, size: 20),
        label: Text(s.pickApps, maxLines: 1, overflow: TextOverflow.ellipsis),
      ),
    );
  }

  Widget _selectedAppsNote({
    required int selectedCount,
    required AppStrings s,
  }) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: colorScheme.primaryContainer.withValues(alpha: 0.4),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Row(
        children: <Widget>[
          Icon(Icons.checklist_rounded, size: 20, color: colorScheme.primary),
          const SizedBox(width: 12),
          Text(
            selectedCount == 0
                ? s.noAppsSelected
                : s.selectedAppsCount(selectedCount),
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
              fontWeight: FontWeight.w600,
              color: colorScheme.primary.withValues(alpha: 0.8),
            ),
          ),
        ],
      ),
    );
  }

  Widget _sectionPanel({
    required String sectionId,
    required String title,
    required IconData icon,
    required Widget child,
    bool? collapsedStatusOk,
  }) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;
    final bool isExpanded = _expandedSections.contains(sectionId);
    final bool isDark = colorScheme.brightness == Brightness.dark;
    final Color panelColor = isDark
        ? colorScheme.surfaceContainerLow
        : Colors.white;
    final Color sectionIconBg = colorScheme.primary.withValues(
      alpha: isDark ? 0.2 : 0.1,
    );
    final Color shadowColor = isDark
        ? colorScheme.shadow.withValues(alpha: 0.28)
        : Colors.black.withValues(alpha: 0.03);

    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: panelColor,
        borderRadius: BorderRadius.circular(32),
        boxShadow: [
          BoxShadow(
            color: shadowColor,
            blurRadius: 24,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          InkWell(
            borderRadius: BorderRadius.circular(16),
            onTap: () => _toggleSection(sectionId),
            child: Padding(
              padding: const EdgeInsets.symmetric(vertical: 2),
              child: Row(
                children: [
                  Stack(
                    clipBehavior: Clip.none,
                    children: <Widget>[
                      Container(
                        padding: const EdgeInsets.all(10),
                        decoration: BoxDecoration(
                          color: sectionIconBg,
                          borderRadius: BorderRadius.circular(14),
                        ),
                        child: Icon(icon, color: colorScheme.primary, size: 22),
                      ),
                      if (!isExpanded && collapsedStatusOk != null)
                        Positioned(
                          top: -2,
                          right: -2,
                          child: Container(
                            width: 12,
                            height: 12,
                            decoration: BoxDecoration(
                              color: collapsedStatusOk
                                  ? const Color(0xFF22C55E)
                                  : const Color(0xFFEF4444),
                              shape: BoxShape.circle,
                              border: Border.all(color: panelColor, width: 1.8),
                            ),
                          ),
                        ),
                    ],
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Text(
                      title,
                      style: Theme.of(context).textTheme.titleLarge?.copyWith(
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                  AnimatedRotation(
                    turns: isExpanded ? 0.5 : 0.0,
                    duration: const Duration(milliseconds: 220),
                    curve: Curves.easeInOutCubic,
                    child: Icon(
                      Icons.keyboard_arrow_down_rounded,
                      size: 24,
                      color: Theme.of(context).colorScheme.onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ),
          ),
          AnimatedSwitcher(
            duration: const Duration(milliseconds: 260),
            switchInCurve: Curves.easeOutCubic,
            switchOutCurve: Curves.easeInCubic,
            transitionBuilder: (Widget child, Animation<double> animation) {
              final Animation<double> curve = CurvedAnimation(
                parent: animation,
                curve: Curves.easeInOutCubic,
              );
              return ClipRect(
                child: SizeTransition(
                  sizeFactor: curve,
                  axis: Axis.vertical,
                  axisAlignment: -1.0,
                  child: SlideTransition(
                    position: Tween<Offset>(
                      begin: const Offset(0, -0.06),
                      end: Offset.zero,
                    ).animate(curve),
                    child: FadeTransition(opacity: curve, child: child),
                  ),
                ),
              );
            },
            child: isExpanded
                ? Padding(
                    key: ValueKey<String>('expanded_$sectionId'),
                    padding: const EdgeInsets.only(top: 20),
                    child: child,
                  )
                : SizedBox(key: ValueKey<String>('collapsed_$sectionId')),
          ),
        ],
      ),
    );
  }
}

class _GithubReleaseInfo {
  const _GithubReleaseInfo({required this.version, required this.htmlUrl});

  final String version;
  final String htmlUrl;
}
