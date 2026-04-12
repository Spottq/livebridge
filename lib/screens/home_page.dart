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
import '../utils/livebridge_haptics.dart';
import '../widgets/shared_widgets.dart';
import 'app_presentation_settings_page.dart';

enum _PackagePickerTarget { conversion, otp, bypass, dedup }

class LiveBridgeHomePage extends StatefulWidget {
  const LiveBridgeHomePage({super.key});

  @override
  State<LiveBridgeHomePage> createState() => _LiveBridgeHomePageState();
}

class _LiveBridgeHomePageState extends State<LiveBridgeHomePage>
    with SingleTickerProviderStateMixin, WidgetsBindingObserver {
  static const String _forkGithubUrl = 'https://github.com/Spottq/livebridge';
  static const String _projectGithubUrl =
      'https://github.com/appsfolder/livebridge';
  static const String _originalGithubUrl =
      'https://github.com/appsfolder/livebridge';
  static const String _standardDownloadUrl =
      'https://appsfolder.github.io/livebridge/#download';
  static const String _updateGithubReleasesUrl =
      'https://appsfolder.github.io/livebridge/';
  static const String _projectGithubBugReportUrl =
      'https://github.com/appsfolder/livebridge/issues/new/choose?template=bug_report.yml';
  static const String _latestReleaseApiUrl =
      'https://api.github.com/repos/appsfolder/livebridge/releases/latest';
  static const String _dictionaryRawUrl =
      'https://raw.githubusercontent.com/appsfolder/livebridge/refs/heads/main/android/app/src/main/assets/liveupdate_dictionary.json';
  static const bool _dictionaryAutoSyncEnabled = false;
  static const Duration _updateCheckInterval = Duration(hours: 6);
  static const String _expandableSettingNativeProgress = 'native_progress';
  static const String _expandableSettingWeather = 'weather';
  static const String _expandableSettingExternalDevices = 'external_devices';
  static const String _expandableSettingNotificationDedup =
      'notification_dedup';
  static const int _networkSpeedThresholdStepBytesPerSecond = 8 * 1024;
  static const int _networkSpeedThresholdMaxBytesPerSecond = 1024 * 1024;

  final TextEditingController _rulesController = TextEditingController();
  final TextEditingController _otpRulesController = TextEditingController();
  final TextEditingController _bypassRulesController = TextEditingController();
  final TextEditingController _notificationDedupRulesController =
      TextEditingController();

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
  bool _syncDndEnabled = false;
  bool _aospCuttingEnabled = false;
  bool _animatedIslandEnabled = false;
  bool _hyperBridgeEnabled = false;
  bool _notificationDedupEnabled = false;
  bool _onlyWithProgress = true;
  bool _textProgressEnabled = true;
  bool _networkSpeedEnabled = false;
  int _networkSpeedMinThresholdBytesPerSecond = 0;
  bool _networkSpeedPrioritizeUpload = false;
  bool _networkSpeedLockscreenOnly = false;
  bool _networkSpeedChipBackgroundDisabled = false;
  bool _smartDetectionEnabled = true;
  bool _smartMediaPlaybackEnabled = false;
  bool _smartNavigationEnabled = true;
  bool _smartWeatherEnabled = true;
  bool _smartWeatherLockscreenOnly = false;
  bool _smartExternalDevicesEnabled = true;
  bool _smartExternalDevicesIgnoreDebugging = true;
  bool _smartVpnEnabled = true;
  bool _otpDetectionEnabled = true;
  bool _otpAutoCopyEnabled = false;
  bool _hasCustomParserDictionary = false;
  bool _dictionaryActionInProgress = false;
  bool _showBackgroundWarning = false;
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
  final Set<String> _expandedSelectedAppNotes = <String>{};
  final Set<String> _expandedInlineSettings = <String>{};
  final Map<String, InstalledApp> _previewAppsByPackage =
      <String, InstalledApp>{};
  bool _expandedSectionsLoaded = false;
  bool _hasPersistedExpandedSections = false;
  bool _didInitSectionDefaults = false;
  bool _previewAppsLoaded = false;
  bool _previewAppsLoading = false;
  Future<void>? _previewAppsWarmupTask;
  PackageMode _packageMode = PackageMode.all;
  PackageMode _otpPackageMode = PackageMode.all;
  NetworkSpeedDisplayMode _networkSpeedDisplayMode =
      NetworkSpeedDisplayMode.total;
  Set<NetworkSpeedUnit> _networkSpeedUnits = <NetworkSpeedUnit>{
    NetworkSpeedUnit.auto,
  };
  String _networkSpeedUploadPrefix = '▲ ';
  String _networkSpeedDownloadPrefix = '▼ ';
  PackageMode _notificationDedupPackageMode = PackageMode.all;
  NotificationDedupMode _notificationDedupMode =
      NotificationDedupMode.otpStatus;
  late final AnimationController _masterBlockedShakeController;
  late final Animation<double> _masterBlockedShakeOffset;
  bool _masterBlockedHapticInProgress = false;
  int _lastNetworkSpeedSliderHapticValue = -1;
  int _lastNetworkSpeedSliderHapticAtMs = 0;

  bool get _canToggleMaster => _listenerEnabled && _notificationsGranted;
  bool get _masterSwitchValue => _canToggleMaster && _converterEnabled;
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
    unawaited(_ensurePreviewAppsLoaded(forceRefresh: true));
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
    _bypassRulesController.dispose();
    _notificationDedupRulesController.dispose();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      unawaited(_refreshState(showLoading: false));
      unawaited(_checkForUpdatesIfNeeded());
      unawaited(_ensurePreviewAppsLoaded(forceRefresh: true));
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
      final bool syncDndEnabled = await LiveBridgePlatform.getSyncDndEnabled();
      final bool aospCuttingEnabled =
          await LiveBridgePlatform.getAospCuttingEnabled();
      final bool animatedIslandEnabled =
          await LiveBridgePlatform.getAnimatedIslandEnabled();
      final bool hyperBridgeEnabled =
          await LiveBridgePlatform.getHyperBridgeEnabled();
      final bool notificationDedupEnabled =
          await LiveBridgePlatform.getNotificationDedupEnabled();
      final NotificationDedupMode notificationDedupMode =
          NotificationDedupModeId.from(
            await LiveBridgePlatform.getNotificationDedupMode(),
          );
      final bool onlyWithProgress =
          await LiveBridgePlatform.getOnlyWithProgress();
      final bool textProgressEnabled =
          await LiveBridgePlatform.getTextProgressEnabled();
      final bool networkSpeedEnabled =
          await LiveBridgePlatform.getNetworkSpeedEnabled();
      final int networkSpeedMinThresholdBytesPerSecond =
          await LiveBridgePlatform.getNetworkSpeedMinThresholdBytesPerSecond();
      final String networkSpeedDisplayMode =
          await LiveBridgePlatform.getNetworkSpeedDisplayMode();
      final String networkSpeedUploadPrefix =
          await LiveBridgePlatform.getNetworkSpeedUploadPrefix();
      final String networkSpeedDownloadPrefix =
          await LiveBridgePlatform.getNetworkSpeedDownloadPrefix();
      final String networkSpeedUnit =
          await LiveBridgePlatform.getNetworkSpeedUnit();
      final bool networkSpeedPrioritizeUpload =
          await LiveBridgePlatform.getNetworkSpeedPrioritizeUpload();
      final bool networkSpeedLockscreenOnly =
          await LiveBridgePlatform.getNetworkSpeedLockscreenOnly();
      final bool networkSpeedChipBackgroundDisabled =
          await LiveBridgePlatform.getNetworkSpeedChipBackgroundDisabled();
      final bool smartDetectionEnabled =
          await LiveBridgePlatform.getSmartStatusDetectionEnabled();
      final bool smartMediaPlaybackEnabled =
          await LiveBridgePlatform.getSmartMediaPlaybackEnabled();
      final bool smartNavigationEnabled =
          await LiveBridgePlatform.getSmartNavigationEnabled();
      final bool smartWeatherEnabled =
          await LiveBridgePlatform.getSmartWeatherEnabled();
      final bool smartWeatherLockscreenOnly =
          await LiveBridgePlatform.getSmartWeatherLockscreenOnly();
      final bool smartExternalDevicesEnabled =
          await LiveBridgePlatform.getSmartExternalDevicesEnabled();
      final bool smartExternalDevicesIgnoreDebugging =
          await LiveBridgePlatform.getSmartExternalDevicesIgnoreDebugging();
      final bool smartVpnEnabled =
          await LiveBridgePlatform.getSmartVpnEnabled();
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
      final String bypassPackageRules =
          await LiveBridgePlatform.getBypassPackageRules();
      final String notificationDedupPackageRules =
          await LiveBridgePlatform.getNotificationDedupPackageRules();
      final PackageMode notificationDedupPackageMode = PackageModeId.from(
        await LiveBridgePlatform.getNotificationDedupPackageMode(),
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
        _syncDndEnabled = syncDndEnabled;
        _aospCuttingEnabled = aospCuttingEnabled;
        _animatedIslandEnabled = animatedIslandEnabled;
        _hyperBridgeEnabled = hyperBridgeEnabled;
        _notificationDedupEnabled = notificationDedupEnabled;
        _notificationDedupMode = notificationDedupMode;
        _onlyWithProgress = onlyWithProgress;
        _textProgressEnabled = textProgressEnabled;
        _networkSpeedEnabled = networkSpeedEnabled;
        _networkSpeedMinThresholdBytesPerSecond =
            networkSpeedMinThresholdBytesPerSecond;
        _networkSpeedDisplayMode = NetworkSpeedDisplayModeId.from(
          networkSpeedDisplayMode,
        );
        _networkSpeedUploadPrefix = networkSpeedUploadPrefix;
        _networkSpeedDownloadPrefix = networkSpeedDownloadPrefix;
        _networkSpeedUnits = NetworkSpeedUnitSelection.parse(networkSpeedUnit);
        _networkSpeedPrioritizeUpload = networkSpeedPrioritizeUpload;
        _networkSpeedLockscreenOnly = networkSpeedLockscreenOnly;
        _networkSpeedChipBackgroundDisabled =
            networkSpeedChipBackgroundDisabled;
        _smartDetectionEnabled = smartDetectionEnabled;
        _smartMediaPlaybackEnabled = smartMediaPlaybackEnabled;
        _smartNavigationEnabled = smartNavigationEnabled;
        _smartWeatherEnabled = smartWeatherEnabled;
        _smartWeatherLockscreenOnly = smartWeatherLockscreenOnly;
        _smartExternalDevicesEnabled = smartExternalDevicesEnabled;
        _smartExternalDevicesIgnoreDebugging =
            smartExternalDevicesIgnoreDebugging;
        _smartVpnEnabled = smartVpnEnabled;
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
        _deviceLabelForWarning = deviceInfo.label;
        _packageMode = packageMode;
        _otpPackageMode = otpPackageMode;
        _notificationDedupPackageMode = notificationDedupPackageMode;
        _rulesController.text = packageRules;
        _bypassRulesController.text = bypassPackageRules;
        _notificationDedupRulesController.text = notificationDedupPackageRules;
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

  Future<void> _persistRules({required _PackagePickerTarget target}) async {
    try {
      switch (target) {
        case _PackagePickerTarget.conversion:
          await LiveBridgePlatform.setPackageRules(_rulesController.text);
          await LiveBridgePlatform.setPackageMode(_packageMode.id);
          break;
        case _PackagePickerTarget.otp:
          await LiveBridgePlatform.setOtpPackageRules(_otpRulesController.text);
          await LiveBridgePlatform.setOtpPackageMode(_otpPackageMode.id);
          break;
        case _PackagePickerTarget.bypass:
          await LiveBridgePlatform.setBypassPackageRules(
            _bypassRulesController.text,
          );
          break;
        case _PackagePickerTarget.dedup:
          await LiveBridgePlatform.setNotificationDedupPackageRules(
            _notificationDedupRulesController.text,
          );
          await LiveBridgePlatform.setNotificationDedupPackageMode(
            _notificationDedupPackageMode.id,
          );
          break;
      }
    } catch (_) {
      if (mounted) {
        _snack(AppStrings.of(context).saveFailed);
      }
    }
  }

  Future<void> _setOnlyWithProgress(bool value) async {
    LiveBridgeHaptics.toggle(value);
    setState(() => _onlyWithProgress = value);
    await LiveBridgePlatform.setOnlyWithProgress(value);
  }

  Future<void> _setTextProgressEnabled(bool value) async {
    LiveBridgeHaptics.toggle(value);
    setState(() => _textProgressEnabled = value);
    await LiveBridgePlatform.setTextProgressEnabled(value);
  }

  Future<void> _setNetworkSpeedEnabled(bool value) async {
    HapticFeedback.selectionClick();
    setState(() => _networkSpeedEnabled = value);
    await LiveBridgePlatform.setNetworkSpeedEnabled(value);
  }

  Future<void> _setNetworkSpeedMinThresholdBytesPerSecond(
    int value, {
    bool persist = true,
  }) async {
    final int normalized = value
        .clamp(0, _networkSpeedThresholdMaxBytesPerSecond)
        .toInt();
    if (normalized != _networkSpeedMinThresholdBytesPerSecond) {
      setState(() => _networkSpeedMinThresholdBytesPerSecond = normalized);
    }
    if (persist) {
      await LiveBridgePlatform.setNetworkSpeedMinThresholdBytesPerSecond(
        normalized,
      );
    }
  }

  Future<void> _setNetworkSpeedDisplayMode(
    NetworkSpeedDisplayMode value,
  ) async {
    HapticFeedback.selectionClick();
    setState(() => _networkSpeedDisplayMode = value);
    await LiveBridgePlatform.setNetworkSpeedDisplayMode(value.id);
  }

  Future<void> _setNetworkSpeedUploadPrefix(String value) async {
    HapticFeedback.selectionClick();
    setState(() => _networkSpeedUploadPrefix = value);
    await LiveBridgePlatform.setNetworkSpeedUploadPrefix(value);
  }

  Future<void> _setNetworkSpeedDownloadPrefix(String value) async {
    HapticFeedback.selectionClick();
    setState(() => _networkSpeedDownloadPrefix = value);
    await LiveBridgePlatform.setNetworkSpeedDownloadPrefix(value);
  }

  Future<void> _setNetworkSpeedUnits(Set<NetworkSpeedUnit> values) async {
    HapticFeedback.selectionClick();
    setState(() => _networkSpeedUnits = Set<NetworkSpeedUnit>.from(values));
    await LiveBridgePlatform.setNetworkSpeedUnit(
      NetworkSpeedUnitSelection.encode(values),
    );
  }

  Future<void> _setNetworkSpeedPrioritizeUpload(bool value) async {
    HapticFeedback.selectionClick();
    setState(() => _networkSpeedPrioritizeUpload = value);
    await LiveBridgePlatform.setNetworkSpeedPrioritizeUpload(value);
  }

  Future<void> _setNetworkSpeedLockscreenOnly(bool value) async {
    HapticFeedback.selectionClick();
    setState(() => _networkSpeedLockscreenOnly = value);
    await LiveBridgePlatform.setNetworkSpeedLockscreenOnly(value);
  }

  Future<void> _setNetworkSpeedChipBackgroundDisabled(bool value) async {
    HapticFeedback.selectionClick();
    setState(() => _networkSpeedChipBackgroundDisabled = value);
    await LiveBridgePlatform.setNetworkSpeedChipBackgroundDisabled(value);
  }

  void _updateNetworkSpeedThresholdDraft(double sliderValue) {
    final int snappedValue = _snapNetworkSpeedThresholdBytesPerSecond(
      sliderValue,
    );
    if (snappedValue == _networkSpeedMinThresholdBytesPerSecond) {
      return;
    }

    setState(() => _networkSpeedMinThresholdBytesPerSecond = snappedValue);

    final int nowMs = DateTime.now().millisecondsSinceEpoch;
    if (snappedValue != _lastNetworkSpeedSliderHapticValue &&
        nowMs - _lastNetworkSpeedSliderHapticAtMs >= 72) {
      _lastNetworkSpeedSliderHapticValue = snappedValue;
      _lastNetworkSpeedSliderHapticAtMs = nowMs;
      unawaited(LiveBridgeHaptics.selection());
    }
  }

  Future<void> _setConverterEnabled(bool value) async {
    if (!_canToggleMaster) return;
    LiveBridgeHaptics.toggle(value);
    setState(() => _converterEnabled = value);
    await LiveBridgePlatform.setConverterEnabled(value);
  }

  Future<void> _setKeepAliveForeground(bool value) async {
    LiveBridgeHaptics.toggle(value);
    setState(() => _keepAliveForegroundEnabled = value);
    await LiveBridgePlatform.setKeepAliveForegroundEnabled(value);
  }

  Future<void> _setSyncDnd(bool value) async {
    LiveBridgeHaptics.toggle(value);
    setState(() => _syncDndEnabled = value);
    await LiveBridgePlatform.setSyncDndEnabled(value);
  }

  Future<void> _setAospCutting(bool value) async {
    LiveBridgeHaptics.toggle(value);
    setState(() => _aospCuttingEnabled = value);
    await LiveBridgePlatform.setAospCuttingEnabled(value);
  }

  Future<void> _setSamsungRemoteReparserEnabled(bool value) async {
    HapticFeedback.selectionClick();
    setState(() => _samsungRemoteReparserEnabled = value);
    await LiveBridgePlatform.setSamsungRemoteReparserEnabled(value);
  }

  Future<void> _setAnimatedIsland(bool value) async {
    LiveBridgeHaptics.toggle(value);
    setState(() => _animatedIslandEnabled = value);
    await LiveBridgePlatform.setAnimatedIslandEnabled(value);
  }

  Future<void> _setHyperBridge(bool value) async {
    LiveBridgeHaptics.toggle(value);
    setState(() => _hyperBridgeEnabled = value);
    await LiveBridgePlatform.setHyperBridgeEnabled(value);
  }

  Future<void> _setNotificationDedupEnabled(bool value) async {
    LiveBridgeHaptics.toggle(value);
    setState(() => _notificationDedupEnabled = value);
    await LiveBridgePlatform.setNotificationDedupEnabled(value);
  }

  Future<void> _setNotificationDedupMode(NotificationDedupMode value) async {
    LiveBridgeHaptics.selection();
    setState(() => _notificationDedupMode = value);
    await LiveBridgePlatform.setNotificationDedupMode(value.id);
  }

  void _toggleInlineSetting(String settingId) {
    final bool opening = !_expandedInlineSettings.contains(settingId);
    setState(() {
      if (opening) {
        _expandedInlineSettings.add(settingId);
      } else {
        _expandedInlineSettings.remove(settingId);
      }
    });
    unawaited(LiveBridgeHaptics.expand(opening));
  }

  Future<void> _setUpdateChecksEnabled(bool value) async {
    LiveBridgeHaptics.toggle(value);
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
      await _checkReleaseUpdateAvailability();
      if (_dictionaryAutoSyncEnabled) {
        await _syncParserDictionaryWithGithubIfNeeded();
      }
    } catch (_) {
    } finally {
      _isCheckingUpdates = false;
    }
  }

  Future<void> _checkReleaseUpdateAvailability() async {
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
  }

  Future<void> _syncParserDictionaryWithGithubIfNeeded() async {
    if (_dictionaryActionInProgress) {
      return;
    }

    final _GithubDictionaryInfo? githubDictionary =
        await _fetchGithubDictionary();
    if (githubDictionary == null) {
      return;
    }

    final String localRaw = (await LiveBridgePlatform.getParserDictionaryJson())
        .trim();
    final String? localNormalized = _normalizeDictionaryJson(localRaw);
    if (localNormalized == githubDictionary.normalized) {
      return;
    }

    final bool saved = await LiveBridgePlatform.setCustomParserDictionary(
      githubDictionary.raw,
    );
    if (saved && mounted) {
      setState(() => _hasCustomParserDictionary = true);
    }
  }

  Future<_GithubDictionaryInfo?> _fetchGithubDictionary() async {
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
            : 'LiveBridge/dictionary-auto-sync',
      );

      final HttpClientResponse response = await request.close();
      if (response.statusCode != HttpStatus.ok) {
        return null;
      }

      final String raw = (await utf8.decoder.bind(response).join()).trim();
      if (raw.isEmpty) {
        return null;
      }

      final String? normalized = _normalizeDictionaryJson(raw);
      if (normalized == null) {
        return null;
      }

      return _GithubDictionaryInfo(raw: raw, normalized: normalized);
    } catch (_) {
      return null;
    } finally {
      client.close(force: true);
    }
  }

  String? _normalizeDictionaryJson(String raw) {
    final String payload = raw.trim();
    if (payload.isEmpty) {
      return null;
    }
    try {
      final dynamic decoded = jsonDecode(payload);
      if (decoded is! Map) {
        return null;
      }
      return jsonEncode(_normalizeJsonNode(decoded));
    } catch (_) {
      return null;
    }
  }

  dynamic _normalizeJsonNode(dynamic value) {
    if (value is Map) {
      final List<MapEntry<String, dynamic>> entries =
          value.entries
              .map(
                (MapEntry<dynamic, dynamic> entry) => MapEntry<String, dynamic>(
                  entry.key.toString(),
                  _normalizeJsonNode(entry.value),
                ),
              )
              .toList()
            ..sort(
              (
                MapEntry<String, dynamic> left,
                MapEntry<String, dynamic> right,
              ) => left.key.compareTo(right.key),
            );
      return <String, dynamic>{
        for (final MapEntry<String, dynamic> entry in entries)
          entry.key: entry.value,
      };
    }
    if (value is List) {
      return value.map<dynamic>(_normalizeJsonNode).toList(growable: false);
    }
    return value;
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
      final String version = tag.isNotEmpty ? tag : name;
      if (version.isEmpty) {
        return null;
      }

      return _GithubReleaseInfo(
        version: version,
        htmlUrl: _updateGithubReleasesUrl,
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
      await LiveBridgeHaptics.blockedPulse();
    } finally {
      _masterBlockedHapticInProgress = false;
    }
  }

  Future<void> _setSmartDetection(bool value) async {
    LiveBridgeHaptics.toggle(value);
    setState(() => _smartDetectionEnabled = value);
    await LiveBridgePlatform.setSmartStatusDetectionEnabled(value);
  }

  Future<void> _setSmartMediaPlayback(bool value) async {
    LiveBridgeHaptics.toggle(value);
    setState(() => _smartMediaPlaybackEnabled = value);
    await LiveBridgePlatform.setSmartMediaPlaybackEnabled(value);
  }

  Future<void> _setSmartNavigation(bool value) async {
    LiveBridgeHaptics.toggle(value);
    setState(() => _smartNavigationEnabled = value);
    await LiveBridgePlatform.setSmartNavigationEnabled(value);
  }

  Future<void> _setSmartWeather(bool value) async {
    LiveBridgeHaptics.toggle(value);
    setState(() => _smartWeatherEnabled = value);
    await LiveBridgePlatform.setSmartWeatherEnabled(value);
  }

  Future<void> _setSmartWeatherLockscreenOnly(bool value) async {
    LiveBridgeHaptics.toggle(value);
    setState(() => _smartWeatherLockscreenOnly = value);
    await LiveBridgePlatform.setSmartWeatherLockscreenOnly(value);
  }

  Future<void> _setSmartExternalDevices(bool value) async {
    LiveBridgeHaptics.toggle(value);
    setState(() => _smartExternalDevicesEnabled = value);
    await LiveBridgePlatform.setSmartExternalDevicesEnabled(value);
  }

  Future<void> _setSmartExternalDevicesIgnoreDebugging(bool value) async {
    LiveBridgeHaptics.toggle(value);
    setState(() => _smartExternalDevicesIgnoreDebugging = value);
    await LiveBridgePlatform.setSmartExternalDevicesIgnoreDebugging(value);
  }

  Future<void> _setSmartVpn(bool value) async {
    LiveBridgeHaptics.toggle(value);
    setState(() => _smartVpnEnabled = value);
    await LiveBridgePlatform.setSmartVpnEnabled(value);
  }

  String _networkSpeedDisplayModeLabel(
    NetworkSpeedDisplayMode mode,
    AppStrings s,
  ) {
    switch (mode) {
      case NetworkSpeedDisplayMode.total:
        return s.networkSpeedDisplayModeTotal;
      case NetworkSpeedDisplayMode.upload:
        return s.networkSpeedDisplayModeUpload;
      case NetworkSpeedDisplayMode.download:
        return s.networkSpeedDisplayModeDownload;
    }
  }

  String _networkSpeedUnitOptionLabel(NetworkSpeedUnit unit, AppStrings s) {
    switch (unit) {
      case NetworkSpeedUnit.auto:
        return s.networkSpeedUnitAuto;
      case NetworkSpeedUnit.bytes:
        return s.networkSpeedUnitBytes;
      case NetworkSpeedUnit.kilobytes:
        return s.networkSpeedUnitKilobytes;
      case NetworkSpeedUnit.megabytes:
        return s.networkSpeedUnitMegabytes;
      case NetworkSpeedUnit.gigabytes:
        return s.networkSpeedUnitGigabytes;
    }
  }

  Future<void> _openNetworkSpeedPrefixSheet({
    required AppStrings s,
    required bool forUpload,
  }) async {
    final String currentValue = forUpload
        ? _networkSpeedUploadPrefix
        : _networkSpeedDownloadPrefix;
    final String? updated = await showModalBottomSheet<String>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(32)),
      ),
      builder: (BuildContext context) => _NetworkSpeedPrefixSheet(
        title: forUpload
            ? s.networkSpeedUploadPrefixTitle
            : s.networkSpeedDownloadPrefixTitle,
        initialValue: currentValue,
        defaultValue: forUpload
            ? kDefaultNetworkSpeedUploadPrefix
            : kDefaultNetworkSpeedDownloadPrefix,
        resetLabel: s.resetToDefault,
        saveLabel: s.save,
      ),
    );

    if (updated == null || updated == currentValue) {
      return;
    }
    if (forUpload) {
      await _setNetworkSpeedUploadPrefix(updated);
    } else {
      await _setNetworkSpeedDownloadPrefix(updated);
    }
  }

  Widget _buildNetworkSpeedUnitDropdown(AppStrings s) {
    return _NetworkSpeedUnitDropdown(
      label: s.networkSpeedUnitTitle,
      values: _networkSpeedUnits,
      optionLabelBuilder: (NetworkSpeedUnit unit) =>
          _networkSpeedUnitOptionLabel(unit, s),
      onChanged: (Set<NetworkSpeedUnit> values) {
        unawaited(_setNetworkSpeedUnits(values));
      },
    );
  }

  Future<void> _setOtpDetection(bool value) async {
    LiveBridgeHaptics.toggle(value);
    setState(() => _otpDetectionEnabled = value);
    await LiveBridgePlatform.setOtpDetectionEnabled(value);
  }

  Future<void> _setOtpAutoCopy(bool value) async {
    LiveBridgeHaptics.toggle(value);
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

  void _cachePreviewApps(List<InstalledApp> apps) {
    _previewAppsByPackage.clear();
    for (final InstalledApp app in apps) {
      _previewAppsByPackage[app.packageName.toLowerCase()] = app;
    }
    _previewAppsLoaded = true;
  }

  Future<void> _ensurePreviewAppsLoaded({bool forceRefresh = false}) {
    if (!forceRefresh && _previewAppsLoaded) {
      return Future<void>.value();
    }
    final Future<void>? inFlight = _previewAppsWarmupTask;
    if (_previewAppsLoading && inFlight != null) {
      return inFlight;
    }

    _previewAppsLoading = true;
    final Future<void> task = () async {
      try {
        final List<InstalledApp> apps =
            await LiveBridgePlatform.getInstalledApps(
              forceRefresh: forceRefresh,
            );
        if (mounted) {
          setState(() => _cachePreviewApps(apps));
        } else {
          _cachePreviewApps(apps);
        }
      } catch (_) {
      } finally {
        _previewAppsLoading = false;
        _previewAppsWarmupTask = null;
        if (mounted) {
          setState(() {});
        }
      }
    }();
    _previewAppsWarmupTask = task;
    return task;
  }

  Future<void> _openPackagePicker({
    required _PackagePickerTarget target,
  }) async {
    if (!await _ensureAppListAccess()) return;
    LiveBridgeHaptics.openSurface();

    await _ensurePreviewAppsLoaded();
    final List<InstalledApp> apps = _previewAppsByPackage.values.toList();
    if (!mounted || apps.isEmpty) {
      if (mounted) _snack(AppStrings.of(context).appsLoadFailed);
      return;
    }

    final AppStrings s = AppStrings.of(context);
    _cachePreviewApps(apps);
    late final TextEditingController targetController;
    late final String pickerTitle;
    switch (target) {
      case _PackagePickerTarget.conversion:
        targetController = _rulesController;
        pickerTitle = s.pickerTitle;
        break;
      case _PackagePickerTarget.otp:
        targetController = _otpRulesController;
        pickerTitle = s.otpPickerTitle;
        break;
      case _PackagePickerTarget.bypass:
        targetController = _bypassRulesController;
        pickerTitle = s.bypassPickerTitle;
        break;
      case _PackagePickerTarget.dedup:
        targetController = _notificationDedupRulesController;
        pickerTitle = s.notificationDedupPickerTitle;
        break;
    }
    final Set<String> initial = _parsePackagesFromInput(targetController.text);

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
          title: pickerTitle,
          apps: apps,
          initialSelected: initial,
          applyLabel: s.applySelection,
          searchHint: s.searchAppHint,
          showSystemAppsLabel: s.showSystemApps,
          hideSystemAppsLabel: s.hideSystemApps,
        );
      },
    );

    if (!mounted || selected == null) return;

    final List<String> values = selected.toList()..sort();
    setState(() {
      targetController.text = values.join('\n');
    });
    await _persistRules(target: target);
  }

  Future<void> _openAppPresentationSettings() async {
    if (!await _ensureAppListAccess()) return;
    if (!mounted) return;
    LiveBridgeHaptics.openSurface();
    await Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (BuildContext context) => const AppPresentationSettingsPage(),
      ),
    );
  }

  Future<void> _openDefaultAppPresentationBehavior() async {
    await showDefaultAppPresentationBehaviorEditor(context);
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
                  onPressed: () {
                    LiveBridgeHaptics.selection();
                    Navigator.of(dialogContext).pop(false);
                  },
                  child: Text(s.cancel),
                ),
                FilledButton(
                  onPressed: () {
                    LiveBridgeHaptics.confirm();
                    Navigator.of(dialogContext).pop(true);
                  },
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
    LiveBridgeHaptics.confirm();
    final bool granted =
        await LiveBridgePlatform.requestNotificationPermission();
    if (!mounted) return;
    final AppStrings s = AppStrings.of(context);
    _snack(granted ? s.permissionGranted : s.permissionDenied);
    await _refreshState();
  }

  Future<void> _openListenerSettings() async {
    LiveBridgeHaptics.openSurface();
    final bool opened =
        await LiveBridgePlatform.openNotificationListenerSettings();
    if (!mounted || opened) return;
    _snack(AppStrings.of(context).listenerUnavailable);
  }

  Future<void> _openAppNotificationSettings() async {
    LiveBridgeHaptics.openSurface();
    final bool opened = await LiveBridgePlatform.openAppNotificationSettings();
    if (!mounted || opened) return;
    _snack(AppStrings.of(context).notificationsUnavailable);
  }

  Future<void> _openPromotedSettings() async {
    LiveBridgeHaptics.openSurface();
    final bool opened =
        await LiveBridgePlatform.openPromotedNotificationSettings();
    if (!mounted || opened) return;
    _snack(AppStrings.of(context).liveUpdatesUnavailable);
  }

  Future<void> _downloadParserDictionary() async {
    if (_dictionaryActionInProgress) return;
    LiveBridgeHaptics.confirm();
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
    LiveBridgeHaptics.confirm();
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
    LiveBridgeHaptics.confirm();
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
    LiveBridgeHaptics.warning();
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

  Future<bool> _launchExternalUrl(Uri uri) async {
    final bool openedInBrowserView = await launchUrl(
      uri,
      mode: LaunchMode.inAppBrowserView,
    );
    if (openedInBrowserView) {
      return true;
    }
    return launchUrl(uri, mode: LaunchMode.externalApplication);
  }

  Future<void> _openForkGithub() async {
    LiveBridgeHaptics.openSurface();
    final Uri uri = Uri.parse(_forkGithubUrl);
    final bool opened = await _launchExternalUrl(uri);
    if (!opened && mounted) {
      _snack(AppStrings.of(context).githubOpenFailed);
    }
  }

  Future<void> _openProjectGithub() async {
    LiveBridgeHaptics.openSurface();
    final Uri uri = Uri.parse(_projectGithubUrl);
    final bool opened = await _launchExternalUrl(uri);
    if (!opened && mounted) {
      _snack(AppStrings.of(context).githubOpenFailed);
    }
  }

  Future<void> _openUpdateGithub() async {
    LiveBridgeHaptics.openSurface();
    final Uri uri = Uri.parse(_updateGithubReleasesUrl);
    final bool opened = await _launchExternalUrl(uri);
    if (!opened && mounted) {
      _snack(AppStrings.of(context).githubOpenFailed);
    }
  }

  Future<void> _openStandardDownloadPage() async {
    LiveBridgeHaptics.openSurface();
    final Uri uri = Uri.parse(_standardDownloadUrl);
    final bool opened = await _launchExternalUrl(uri);
    if (!opened && mounted) {
      final AppStrings s = AppStrings.of(context);
      _snack(
        s.isRu
            ? '\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043e\u0442\u043a\u0440\u044b\u0442\u044c \u0441\u0441\u044b\u043b\u043a\u0443.'
            : 'Unable to open link.',
      );
    }
  }

  String _blockedTitle(AppStrings s) => s.isRu
      ? '\u042d\u0442\u043e \u0432\u0435\u0440\u0441\u0438\u044f LiveBridge \u0442\u043e\u043b\u044c\u043a\u043e \u0434\u043b\u044f Samsung'
      : 'This LiveBridge build is for Samsung only';

  String _blockedSubtitle(AppStrings s) => s.isRu
      ? '\u0423 \u0432\u0430\u0441 \u043d\u0435 Samsung, \u043f\u043e\u0436\u0430\u043b\u0443\u0439\u0441\u0442\u0430 \u0443\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u0435 \u043e\u0431\u044b\u0447\u043d\u0443\u044e \u0432\u0435\u0440\u0441\u0438\u044e LiveBridge.'
      : 'You do not have a Samsung device. Please install the regular LiveBridge build.';

  String _blockedInstallAction(AppStrings s) => s.isRu
      ? '\u0423\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u044c \u043e\u0431\u044b\u0447\u043d\u0443\u044e \u0432\u0435\u0440\u0441\u0438\u044e'
      : 'Install regular version';

  List<String> _parseRulesText(String raw) {
    return raw
        .split(RegExp(r'[\s,\n\r\t;]+'))
        .map((String item) => item.trim())
        .where((String item) => item.isNotEmpty)
        .toList();
  }

  Future<String> _buildBugReportDiagnosticsJson({
    required String localeTag,
  }) async {
    final DateTime now = DateTime.now();
    final DeviceInfo deviceInfo = await LiveBridgePlatform.getDeviceInfo();
    final String appPresentationOverridesRaw =
        await LiveBridgePlatform.getAppPresentationOverrides();

    final List<String> packageRules = _parseRulesText(_rulesController.text);
    final List<String> bypassPackageRules = _parseRulesText(
      _bypassRulesController.text,
    );
    final List<String> notificationDedupPackageRules = _parseRulesText(
      _notificationDedupRulesController.text,
    );
    final List<String> otpPackageRules = _parseRulesText(
      _otpRulesController.text,
    );
    final List<String> expandedSections = _expandedSections.toList()..sort();

    final Map<String, dynamic> payload = <String, dynamic>{
      'schema': 'livebridge_bug_report_v1',
      'generated_at_utc': now.toUtc().toIso8601String(),
      'generated_at_local': now.toIso8601String(),
      'timezone_name': now.timeZoneName,
      'timezone_offset_minutes': now.timeZoneOffset.inMinutes,
      'locale': localeTag,
      'platform': <String, dynamic>{
        'os': Platform.operatingSystem,
        'os_version': Platform.operatingSystemVersion,
      },
      'app': <String, dynamic>{
        'version': _currentAppVersion,
        'latest_release_version': _latestReleaseVersion,
        'update_available': _updateAvailable,
      },
      'device': <String, dynamic>{
        'label': deviceInfo.label,
        'manufacturer': deviceInfo.manufacturer,
        'brand': deviceInfo.brand,
        'market_name': deviceInfo.marketName,
        'model': deviceInfo.model,
        'raw_model': deviceInfo.rawModel,
        'product': deviceInfo.product,
        'display': deviceInfo.display,
        'fingerprint': deviceInfo.fingerprint,
        'is_pixel': deviceInfo.isPixel,
        'is_samsung': deviceInfo.isSamsung,
        'is_aosp_device': deviceInfo.isAospDevice,
        'hide_live_updates_promotion':
            deviceInfo.shouldHideLiveUpdatesPromotion,
      },
      'permissions': <String, dynamic>{
        'listener_enabled': _listenerEnabled,
        'notifications_granted': _notificationsGranted,
        'can_post_promoted': _canPostPromoted,
      },
      'settings': <String, dynamic>{
        'converter_enabled': _converterEnabled,
        'keep_alive_foreground_enabled': _keepAliveForegroundEnabled,
        'network_speed_enabled': _networkSpeedEnabled,
        'network_speed_min_threshold_bytes_per_second':
            _networkSpeedMinThresholdBytesPerSecond,
        'sync_dnd_enabled': _syncDndEnabled,
        'update_checks_enabled': _updateChecksEnabled,
        'only_with_progress': _onlyWithProgress,
        'text_progress_enabled': _textProgressEnabled,
        'smart_detection_enabled': _smartDetectionEnabled,
        'smart_media_playback_enabled': _smartMediaPlaybackEnabled,
        'smart_navigation_enabled': _smartNavigationEnabled,
        'smart_weather_enabled': _smartWeatherEnabled,
        'smart_weather_lockscreen_only': _smartWeatherLockscreenOnly,
        'smart_external_devices_enabled': _smartExternalDevicesEnabled,
        'smart_external_devices_ignore_debugging':
            _smartExternalDevicesIgnoreDebugging,
        'smart_vpn_enabled': _smartVpnEnabled,
        'otp_detection_enabled': _otpDetectionEnabled,
        'otp_auto_copy_enabled': _otpAutoCopyEnabled,
        'aosp_cutting_enabled': _aospCuttingEnabled,
        'animated_island_enabled': _animatedIslandEnabled,
        'hyper_bridge_enabled': _hyperBridgeEnabled,
        'notification_dedup_enabled': _notificationDedupEnabled,
        'notification_dedup_mode': _notificationDedupMode.id,
      },
      'rules': <String, dynamic>{
        'package_mode': _packageMode.id,
        'package_rules': packageRules,
        'package_rules_count': packageRules.length,
        'bypass_package_rules': bypassPackageRules,
        'bypass_package_rules_count': bypassPackageRules.length,
        'notification_dedup_package_mode': _notificationDedupPackageMode.id,
        'notification_dedup_package_rules': notificationDedupPackageRules,
        'notification_dedup_package_rules_count':
            notificationDedupPackageRules.length,
        'otp_package_mode': _otpPackageMode.id,
        'otp_package_rules': otpPackageRules,
        'otp_package_rules_count': otpPackageRules.length,
      },
      'additional_state': <String, dynamic>{
        'has_custom_parser_dictionary': _hasCustomParserDictionary,
        'app_presentation_overrides_length': appPresentationOverridesRaw.length,
        'expanded_sections': expandedSections,
      },
    };

    return const JsonEncoder.withIndent('  ').convert(payload);
  }

  Future<bool> _copyBugReportDiagnosticsToClipboard() async {
    try {
      final String localeTag = Localizations.localeOf(context).toLanguageTag();
      final String payload = await _buildBugReportDiagnosticsJson(
        localeTag: localeTag,
      );
      await Clipboard.setData(ClipboardData(text: payload));
      return true;
    } catch (_) {
      return false;
    }
  }

  Future<void> _openBugReport() async {
    LiveBridgeHaptics.confirm();
    final AppStrings s = AppStrings.of(context);
    final bool copied = await _copyBugReportDiagnosticsToClipboard();
    if (mounted) {
      _snack(copied ? s.bugReportCopied : s.bugReportCopyFailed);
    }
    final Uri uri = Uri.parse(_projectGithubBugReportUrl);
    final bool opened = await _launchExternalUrl(uri);
    if (!opened && mounted) {
      _snack(AppStrings.of(context).githubOpenFailed);
    }
  }

  Future<void> _hideBackgroundWarning() async {
    LiveBridgeHaptics.selection();
    await LiveBridgePlatform.setBackgroundWarningDismissed(true);
    if (!mounted) return;
    setState(() => _showBackgroundWarning = false);
  }

  Widget _buildGithubLinkCard({
    required String caption,
    required String url,
    required VoidCallback onTap,
    bool highlight = false,
  }) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;
    final Color accentColor = highlight
        ? colorScheme.error
        : colorScheme.onSurfaceVariant;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Text(
          caption,
          style: Theme.of(context).textTheme.labelMedium?.copyWith(
            fontWeight: FontWeight.w700,
            color: colorScheme.onSurfaceVariant,
          ),
        ),
        const SizedBox(height: 6),
        InkWell(
          borderRadius: BorderRadius.circular(14),
          onTap: onTap,
          child: Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
            decoration: BoxDecoration(
              color: highlight
                  ? colorScheme.errorContainer.withValues(alpha: 0.55)
                  : colorScheme.surfaceContainerHighest.withValues(alpha: 0.3),
              borderRadius: BorderRadius.circular(14),
              border: highlight
                  ? Border.all(
                      color: colorScheme.error.withValues(alpha: 0.28),
                      width: 1.1,
                    )
                  : null,
            ),
            child: Row(
              children: <Widget>[
                Icon(Icons.code_rounded, size: 20, color: accentColor),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(
                    url,
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      fontWeight: FontWeight.w600,
                      color: highlight ? colorScheme.error : null,
                    ),
                  ),
                ),
                Icon(Icons.open_in_new_rounded, size: 18, color: accentColor),
              ],
            ),
          ),
        ),
      ],
    );
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
      'experimental',
      'app_presentation',
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
    final bool opening = !_expandedSections.contains(sectionId);
    LiveBridgeHaptics.expand(opening);
    setState(() {
      if (!opening) {
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
                    const SizedBox(height: 24),
                    _buildAccessCard(s),
                    const SizedBox(height: 24),
                    _buildRulesCard(s),
                    const SizedBox(height: 24),
                    _buildBypassCard(s),
                    const SizedBox(height: 24),
                    _buildSmartCard(s),
                    const SizedBox(height: 24),
                    _buildNetworkSpeedCard(s),
                    const SizedBox(height: 24),
                    _buildOtpCard(s),
                    const SizedBox(height: 24),
                    _buildExperimentalCard(s),
                    const SizedBox(height: 24),
                    _buildAppPresentationCard(s),
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
    final EdgeInsets viewPadding = MediaQuery.of(context).padding;

    return Padding(
      padding: EdgeInsets.fromLTRB(
        24,
        viewPadding.top + 24,
        24,
        viewPadding.bottom + 24,
      ),
      child: Column(
        children: <Widget>[
          Expanded(
            child: Center(
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 400),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: <Widget>[
                    Stack(
                      alignment: Alignment.center,
                      children: <Widget>[
                        Container(
                          width: 120,
                          height: 120,
                          decoration: BoxDecoration(
                            color: colorScheme.errorContainer.withValues(
                              alpha: 0.3,
                            ),
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
                      _blockedTitle(s),
                      textAlign: TextAlign.center,
                      style: Theme.of(context).textTheme.headlineSmall
                          ?.copyWith(fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 16),
                    Text(
                      _blockedSubtitle(s),
                      textAlign: TextAlign.center,
                      style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                        color: colorScheme.onSurfaceVariant,
                        height: 1.4,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
          SizedBox(
            width: double.infinity,
            child: FilledButton.icon(
              onPressed: _openStandardDownloadPage,
              icon: const Icon(Icons.download_rounded),
              label: Text(
                _blockedInstallAction(s),
                textAlign: TextAlign.center,
              ),
            ),
          ),
        ],
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
                        value: _masterSwitchValue,
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
              _converterEnabled
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
            value: _syncDndEnabled,
            onChanged: _setSyncDnd,
            title: Text(
              s.syncDndTitle,
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
            subtitle: Text(
              s.syncDndSubtitle,
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
          _buildGithubLinkCard(
            caption: _hasUpdateAlert ? 'Update source' : 'Forked by',
            url: _hasUpdateAlert ? s.downloadPageUrl : _forkGithubUrl,
            onTap: () {
              unawaited(
                _hasUpdateAlert ? _openUpdateGithub() : _openForkGithub(),
              );
            },
            highlight: _hasUpdateAlert,
          ),
          const SizedBox(height: 10),
          _buildGithubLinkCard(
            caption: 'Original app:',
            url: _originalGithubUrl.replaceFirst('https://', ''),
            onTap: () {
              unawaited(_openProjectGithub());
            },
          ),
          const SizedBox(height: 10),
          SizedBox(
            width: double.infinity,
            child: OutlinedButton.icon(
              onPressed: _openBugReport,
              icon: const Icon(Icons.bug_report_rounded, size: 18),
              label: Text(s.reportBug),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildAppPresentationCard(AppStrings s) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;

    return _sectionPanel(
      sectionId: 'app_presentation',
      title: s.appPresentationSettings,
      icon: Icons.tune_rounded,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text(
            s.appPresentationSubtitle,
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
              color: colorScheme.onSurfaceVariant,
              height: 1.35,
            ),
          ),
          const SizedBox(height: 14),
          Material(
            color: Colors.transparent,
            child: InkWell(
              borderRadius: BorderRadius.circular(16),
              onTap: _openDefaultAppPresentationBehavior,
              child: Ink(
                width: double.infinity,
                padding: const EdgeInsets.symmetric(
                  horizontal: 14,
                  vertical: 12,
                ),
                decoration: BoxDecoration(
                  color: colorScheme.primaryContainer.withValues(alpha: 0.38),
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Row(
                  children: <Widget>[
                    Container(
                      width: 34,
                      height: 34,
                      decoration: BoxDecoration(
                        color: colorScheme.primary.withValues(alpha: 0.12),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Icon(
                        Icons.auto_fix_high_rounded,
                        size: 18,
                        color: colorScheme.primary,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Text(
                        s.appPresentationDefaultSummary,
                        style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ),
                    Icon(
                      Icons.chevron_right_rounded,
                      color: colorScheme.onSurfaceVariant,
                    ),
                  ],
                ),
              ),
            ),
          ),
          const SizedBox(height: 12),
          SizedBox(
            width: double.infinity,
            child: FilledButton.icon(
              onPressed: _openAppPresentationSettings,
              icon: const Icon(Icons.tune_rounded, size: 18),
              label: Text(s.appPresentationSettings),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildExperimentalCard(AppStrings s) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;

    return _sectionPanel(
      sectionId: 'experimental',
      title: s.experimentalTitle,
      icon: Icons.science_rounded,
      child: Column(
        children: <Widget>[
          SwitchListTile.adaptive(
            value: _animatedIslandEnabled,
            onChanged: _setAnimatedIsland,
            title: Text(
              s.animatedIslandTitle,
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
            subtitle: Text(
              s.animatedIslandSubtitle,
              style: TextStyle(
                color: colorScheme.onSurfaceVariant,
                fontSize: 13,
              ),
            ),
            contentPadding: EdgeInsets.zero,
            activeThumbColor: colorScheme.primary,
          ),
          const SizedBox(height: 8),
          _buildExpandableTile(
            settingId: _expandableSettingNotificationDedup,
            title: s.notificationDedupTitle,
            subtitle: s.notificationDedupSubtitle,
            trailing: Switch.adaptive(
              value: _notificationDedupEnabled,
              onChanged: _setNotificationDedupEnabled,
              activeThumbColor: colorScheme.primary,
            ),
            expandedChild: _buildNotificationDedupOptionsPanel(s),
          ),
          const SizedBox(height: 8),
          SwitchListTile.adaptive(
            value: _hyperBridgeEnabled,
            onChanged: _setHyperBridge,
            title: Text(
              s.hyperBridgeTitle,
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
            subtitle: Text(
              s.hyperBridgeSubtitle,
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

  Widget _buildModernDropdown<T>({
    required String label,
    required T currentValue,
    required List<DropdownMenuItem<T>> items,
    required void Function(T?) onChanged,
    VoidCallback? onTap,
  }) {
    final colorScheme = Theme.of(context).colorScheme;
    final bool isLight = colorScheme.brightness == Brightness.light;
    final Color fieldColor = isLight
        ? Colors.white
        : colorScheme.surfaceContainerLow;
    final Color menuColor = isLight
        ? Colors.white
        : colorScheme.surfaceContainer;
    final Color borderColor = colorScheme.primary.withValues(
      alpha: isLight ? 0.5 : 0.65,
    );

    return DropdownButtonFormField<T>(
      initialValue: currentValue,
      onTap: onTap,
      onChanged: onChanged,
      isExpanded: true,
      icon: Icon(
        Icons.keyboard_arrow_down_rounded,
        color: colorScheme.onSurfaceVariant,
      ),
      dropdownColor: menuColor,
      borderRadius: BorderRadius.circular(24),
      decoration: InputDecoration(
        labelText: label,
        labelStyle: TextStyle(
          fontSize: 14,
          fontWeight: FontWeight.w500,
          color: colorScheme.onSurfaceVariant,
        ),
        filled: true,
        fillColor: fieldColor,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(20),
          borderSide: BorderSide(color: borderColor, width: 1.2),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(20),
          borderSide: BorderSide(color: borderColor, width: 1.2),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(20),
          borderSide: BorderSide(color: colorScheme.primary, width: 1.8),
        ),
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 20,
          vertical: 16,
        ),
      ),
      items: items,
    );
  }

  Widget _buildNotificationDedupStatusesSwitchRow(AppStrings s) {
    final bool value =
        _notificationDedupMode == NotificationDedupMode.otpStatus;
    return _buildInlinePanelSwitchRow(
      title: s.notificationDedupStatusesTitle,
      subtitle: s.notificationDedupStatusesSubtitle,
      value: value,
      onChanged: (bool enabled) {
        final NotificationDedupMode nextMode = enabled
            ? NotificationDedupMode.otpStatus
            : NotificationDedupMode.otpOnly;
        unawaited(_setNotificationDedupMode(nextMode));
      },
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
          _buildModernDropdown<PackageMode>(
            label: s.modeLabel,
            currentValue: _packageMode,
            items: PackageMode.values.map((PackageMode mode) {
              return DropdownMenuItem<PackageMode>(
                value: mode,
                child: Text(
                  _getModeLabel(mode, s),
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              );
            }).toList(),
            onChanged: (val) {
              if (val != null) {
                LiveBridgeHaptics.selection();
                setState(() => _packageMode = val);
                unawaited(
                  _persistRules(target: _PackagePickerTarget.conversion),
                );
              }
            },
            onTap: LiveBridgeHaptics.openSurface,
          ),
          const SizedBox(height: 16),
          _selectedAppsNote(
            noteId: 'conversion',
            selectedPackages: _parsePackagesFromInput(_rulesController.text),
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
            onPick: () =>
                _openPackagePicker(target: _PackagePickerTarget.conversion),
            s: s,
          ),
          const Padding(
            padding: EdgeInsets.symmetric(vertical: 16),
            child: Divider(height: 1),
          ),
          _buildExpandableTile(
            settingId: _expandableSettingNativeProgress,
            title: s.onlyProgressTitle,
            subtitle: s.onlyProgressSubtitle,
            trailing: Switch.adaptive(
              value: _onlyWithProgress,
              onChanged: _setOnlyWithProgress,
              activeThumbColor: Theme.of(context).colorScheme.primary,
            ),
            expandedChild: _buildNativeProgressOptionsPanel(s),
          ),
        ],
      ),
    );
  }

  Widget _buildBypassCard(AppStrings s) {
    final Color color = Theme.of(context).colorScheme.onSurfaceVariant;

    return _sectionPanel(
      sectionId: 'bypass',
      title: s.bypassRulesTitle,
      icon: Icons.content_paste_go_rounded,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text(
            s.bypassRulesSubtitle,
            style: TextStyle(color: color, fontSize: 13),
          ),
          const SizedBox(height: 16),
          _selectedAppsNote(
            noteId: 'bypass',
            selectedPackages: _parsePackagesFromInput(
              _bypassRulesController.text,
            ),
            s: s,
          ),
          const SizedBox(height: 8),
          _ruleButtonsRow(
            onPick: () =>
                _openPackagePicker(target: _PackagePickerTarget.bypass),
            s: s,
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
            value: _smartMediaPlaybackEnabled,
            onChanged: _setSmartMediaPlayback,
            title: Text(
              s.smartMediaPlaybackTitle,
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
            subtitle: Text(
              s.smartMediaPlaybackSubtitle,
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
          const SizedBox(height: 8),
          _buildExpandableTile(
            settingId: _expandableSettingWeather,
            title: s.smartWeatherTitle,
            subtitle: _smartDetectionEnabled
                ? s.smartWeatherSubtitle
                : s.smartNavigationDisabledSubtitle,
            trailing: Switch.adaptive(
              value: _smartWeatherEnabled,
              onChanged: _smartDetectionEnabled ? _setSmartWeather : null,
              activeThumbColor: Theme.of(context).colorScheme.primary,
            ),
            expandedChild: _buildWeatherOptionsPanel(s),
          ),
          const SizedBox(height: 8),
          _buildExpandableTile(
            settingId: _expandableSettingExternalDevices,
            title: s.smartExternalDevicesTitle,
            subtitle: _smartDetectionEnabled
                ? s.smartExternalDevicesSubtitle
                : s.smartNavigationDisabledSubtitle,
            trailing: Switch.adaptive(
              value: _smartExternalDevicesEnabled,
              onChanged: _smartDetectionEnabled
                  ? _setSmartExternalDevices
                  : null,
              activeThumbColor: Theme.of(context).colorScheme.primary,
            ),
            expandedChild: _buildExternalDevicesOptionsPanel(s),
          ),
          const SizedBox(height: 8),
          SwitchListTile.adaptive(
            value: _smartVpnEnabled,
            onChanged: _smartDetectionEnabled ? _setSmartVpn : null,
            title: Text(
              s.smartVpnTitle,
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
            subtitle: Text(
              _smartDetectionEnabled
                  ? s.smartVpnSubtitle
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

  Widget _buildNetworkSpeedCard(AppStrings s) {
    return _sectionPanel(
      sectionId: 'network_speed',
      title: s.networkSpeedCardTitle,
      icon: Icons.speed_rounded,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          SwitchListTile.adaptive(
            value: _networkSpeedEnabled,
            onChanged: _setNetworkSpeedEnabled,
            title: Text(
              s.networkSpeedEnabledTitle,
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
            subtitle: Text(
              s.networkSpeedEnabledSubtitle,
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
            opacity: _networkSpeedEnabled ? 1 : 0.4,
            child: IgnorePointer(
              ignoring: !_networkSpeedEnabled,
              child: Column(
                children: <Widget>[
                  _buildNetworkSpeedThresholdPanel(s),
                  const SizedBox(height: 12),
                  _buildModernDropdown<NetworkSpeedDisplayMode>(
                    label: s.networkSpeedDisplayContentTitle,
                    currentValue: _networkSpeedDisplayMode,
                    items: NetworkSpeedDisplayMode.values.map((
                      NetworkSpeedDisplayMode mode,
                    ) {
                      return DropdownMenuItem<NetworkSpeedDisplayMode>(
                        value: mode,
                        child: Text(
                          _networkSpeedDisplayModeLabel(mode, s),
                          style: const TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      );
                    }).toList(),
                    onChanged: (NetworkSpeedDisplayMode? value) {
                      if (value == null || value == _networkSpeedDisplayMode) {
                        return;
                      }
                      unawaited(_setNetworkSpeedDisplayMode(value));
                    },
                    onTap: LiveBridgeHaptics.openSurface,
                  ),
                  const SizedBox(height: 12),
                  _buildActionSettingTile(
                    title: s.networkSpeedUploadPrefixTitle,
                    value: s.networkSpeedCurrentValue(
                      _networkSpeedUploadPrefix,
                    ),
                    icon: Icons.north_rounded,
                    onTap: () =>
                        _openNetworkSpeedPrefixSheet(s: s, forUpload: true),
                  ),
                  const SizedBox(height: 12),
                  _buildActionSettingTile(
                    title: s.networkSpeedDownloadPrefixTitle,
                    value: s.networkSpeedCurrentValue(
                      _networkSpeedDownloadPrefix,
                    ),
                    icon: Icons.south_rounded,
                    onTap: () =>
                        _openNetworkSpeedPrefixSheet(s: s, forUpload: false),
                  ),
                  const SizedBox(height: 12),
                  _buildNetworkSpeedUnitDropdown(s),
                  const SizedBox(height: 12),
                  SwitchListTile.adaptive(
                    value: _networkSpeedPrioritizeUpload,
                    onChanged: _setNetworkSpeedPrioritizeUpload,
                    title: Text(
                      s.networkSpeedPrioritizeUploadTitle,
                      style: const TextStyle(fontWeight: FontWeight.w600),
                    ),
                    subtitle: Text(
                      s.networkSpeedPrioritizeUploadSubtitle,
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
                    value: _networkSpeedLockscreenOnly,
                    onChanged: _setNetworkSpeedLockscreenOnly,
                    title: Text(
                      s.networkSpeedLockscreenOnlyTitle,
                      style: const TextStyle(fontWeight: FontWeight.w600),
                    ),
                    subtitle: Text(
                      s.networkSpeedLockscreenOnlySubtitle,
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
                    value: _networkSpeedChipBackgroundDisabled,
                    onChanged: _setNetworkSpeedChipBackgroundDisabled,
                    title: Text(
                      s.networkSpeedDisableChipBackgroundTitle,
                      style: const TextStyle(fontWeight: FontWeight.w600),
                    ),
                    subtitle: Text(
                      s.networkSpeedDisableChipBackgroundSubtitle,
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
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildNetworkSpeedThresholdSetting(AppStrings s) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;
    final double sliderMax =
        _networkSpeedThresholdMaxBytesPerSecond /
        _networkSpeedThresholdStepBytesPerSecond;
    final String currentValueLabel = _formatNetworkSpeedThresholdValue(
      s,
      _networkSpeedMinThresholdBytesPerSecond,
    );

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: _expandablePanelBackgroundColor(colorScheme),
        borderRadius: BorderRadius.circular(20),
        border: _expandablePanelBorder(colorScheme),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            children: <Widget>[
              Expanded(
                child: Text(
                  s.networkSpeedThresholdTitle,
                  style: const TextStyle(fontWeight: FontWeight.w700),
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 10,
                  vertical: 6,
                ),
                decoration: BoxDecoration(
                  color: colorScheme.primary.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(999),
                ),
                child: Text(
                  currentValueLabel,
                  style: TextStyle(
                    color: colorScheme.primary,
                    fontWeight: FontWeight.w700,
                    fontSize: 12,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Text(
            s.networkSpeedThresholdSubtitle,
            style: TextStyle(color: colorScheme.onSurfaceVariant, fontSize: 13),
          ),
          const SizedBox(height: 12),
          SliderTheme(
            data: SliderTheme.of(
              context,
            ).copyWith(overlayShape: SliderComponentShape.noOverlay),
            child: Slider.adaptive(
              value: _networkSpeedSliderPositionForBytesPerSecond(
                _networkSpeedMinThresholdBytesPerSecond,
              ),
              min: 0,
              max: sliderMax,
              label: currentValueLabel,
              onChangeStart: (double value) {
                _lastNetworkSpeedSliderHapticValue =
                    _snapNetworkSpeedThresholdBytesPerSecond(value);
                _lastNetworkSpeedSliderHapticAtMs = 0;
              },
              onChanged: _updateNetworkSpeedThresholdDraft,
              onChangeEnd: (double value) {
                _lastNetworkSpeedSliderHapticValue = -1;
                unawaited(
                  _setNetworkSpeedMinThresholdBytesPerSecond(
                    _snapNetworkSpeedThresholdBytesPerSecond(value),
                  ),
                );
              },
            ),
          ),
          Row(
            children: <Widget>[
              Expanded(
                child: Text(
                  s.networkSpeedThresholdAlways,
                  style: TextStyle(
                    color: colorScheme.onSurfaceVariant,
                    fontSize: 12,
                  ),
                ),
              ),
              Text(
                _formatNetworkSpeedBytesPerSecond(
                  _networkSpeedThresholdMaxBytesPerSecond,
                ),
                style: TextStyle(
                  color: colorScheme.onSurfaceVariant,
                  fontSize: 12,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  int _snapNetworkSpeedThresholdBytesPerSecond(double sliderValue) {
    final int snappedValue =
        sliderValue.round() * _networkSpeedThresholdStepBytesPerSecond;
    return snappedValue
        .clamp(0, _networkSpeedThresholdMaxBytesPerSecond)
        .toInt();
  }

  double _networkSpeedSliderPositionForBytesPerSecond(int bytesPerSecond) {
    return (bytesPerSecond / _networkSpeedThresholdStepBytesPerSecond)
        .clamp(
          0,
          _networkSpeedThresholdMaxBytesPerSecond /
              _networkSpeedThresholdStepBytesPerSecond,
        )
        .toDouble();
  }

  String _formatNetworkSpeedThresholdValue(AppStrings s, int bytesPerSecond) {
    if (bytesPerSecond <= 0) {
      return s.networkSpeedThresholdAlways;
    }
    return '>= ${_formatNetworkSpeedBytesPerSecond(bytesPerSecond)}';
  }

  String _formatNetworkSpeedBytesPerSecond(int bytesPerSecond) {
    final int value = bytesPerSecond.clamp(0, 1 << 31).toInt();
    if (value < 1024) {
      return '${value}B/s';
    }
    if (value < 1024 * 1024) {
      return _formatCompactNetworkSpeedValue(value / 1024, 'KB/s');
    }
    if (value < 1024 * 1024 * 1024) {
      return _formatCompactNetworkSpeedValue(value / (1024 * 1024), 'MB/s');
    }
    return _formatCompactNetworkSpeedValue(
      value / (1024 * 1024 * 1024),
      'GB/s',
    );
  }

  String _formatCompactNetworkSpeedValue(double value, String suffix) {
    final String formatted = value < 10
        ? value.toStringAsFixed(1)
        : value.toStringAsFixed(0);
    return '$formatted$suffix';
  }

  Widget _buildActionSettingTile({
    required String title,
    required String value,
    required IconData icon,
    required VoidCallback onTap,
  }) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;
    return Material(
      color: Colors.transparent,
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: () {
          LiveBridgeHaptics.openSurface();
          onTap();
        },
        child: Container(
          width: double.infinity,
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
          decoration: BoxDecoration(
            color: colorScheme.primaryContainer.withValues(alpha: 0.38),
            borderRadius: BorderRadius.circular(16),
          ),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: <Widget>[
              Container(
                width: 34,
                height: 34,
                decoration: BoxDecoration(
                  color: colorScheme.primary.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(icon, size: 18, color: colorScheme.primary),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Text(
                      title,
                      style: const TextStyle(
                        fontWeight: FontWeight.w700,
                        fontSize: 15,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      value,
                      style: TextStyle(
                        color: colorScheme.onSurfaceVariant,
                        fontSize: 13,
                        height: 1.2,
                      ),
                    ),
                  ],
                ),
              ),
              Icon(
                Icons.chevron_right_rounded,
                color: colorScheme.onSurfaceVariant,
              ),
            ],
          ),
        ),
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
                  _buildModernDropdown<PackageMode>(
                    label: s.otpModeLabel,
                    currentValue: _otpPackageMode,
                    items: PackageMode.values.map((PackageMode mode) {
                      return DropdownMenuItem<PackageMode>(
                        value: mode,
                        child: Text(
                          _getModeLabel(mode, s),
                          style: const TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      );
                    }).toList(),
                    onChanged: (val) {
                      if (val != null) {
                        LiveBridgeHaptics.selection();
                        setState(() => _otpPackageMode = val);
                        unawaited(
                          _persistRules(target: _PackagePickerTarget.otp),
                        );
                      }
                    },
                    onTap: LiveBridgeHaptics.openSurface,
                  ),
                  const SizedBox(height: 16),
                  _selectedAppsNote(
                    noteId: 'otp',
                    selectedPackages: _parsePackagesFromInput(
                      _otpRulesController.text,
                    ),
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
                    onPick: () =>
                        _openPackagePicker(target: _PackagePickerTarget.otp),
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

  Widget _buildExpandableTile({
    required String settingId,
    required String title,
    required String subtitle,
    required Widget trailing,
    required Widget expandedChild,
  }) {
    final ThemeData theme = Theme.of(context);
    final ColorScheme colorScheme = Theme.of(context).colorScheme;
    final bool isLight = colorScheme.brightness == Brightness.light;
    final bool expanded = _expandedInlineSettings.contains(settingId);
    final Color outerBorderColor = expanded
        ? colorScheme.primary.withValues(alpha: 0.26)
        : Colors.transparent;
    final Color outerBackgroundColor = expanded
        ? (isLight
              ? theme.scaffoldBackgroundColor
              : colorScheme.primaryContainer.withValues(alpha: 0.18))
        : Colors.transparent;
    final Color rowHighlightColor = expanded
        ? colorScheme.primary.withValues(alpha: isLight ? 0.04 : 0.06)
        : Colors.transparent;

    return AnimatedContainer(
      duration: const Duration(milliseconds: 220),
      curve: Curves.easeOutCubic,
      decoration: BoxDecoration(
        color: outerBackgroundColor,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: outerBorderColor),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Material(
            color: Colors.transparent,
            child: InkWell(
              borderRadius: BorderRadius.circular(20),
              splashFactory: NoSplash.splashFactory,
              overlayColor: const WidgetStatePropertyAll<Color>(
                Colors.transparent,
              ),
              splashColor: Colors.transparent,
              highlightColor: Colors.transparent,
              hoverColor: Colors.transparent,
              focusColor: Colors.transparent,
              onTap: () => _toggleInlineSetting(settingId),
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 220),
                curve: Curves.easeOutCubic,
                decoration: BoxDecoration(
                  color: rowHighlightColor,
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Padding(
                  padding: EdgeInsets.fromLTRB(
                    expanded ? 16 : 0,
                    10,
                    expanded ? 10 : 0,
                    10,
                  ),
                  child: Row(
                    children: <Widget>[
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: <Widget>[
                            Text(
                              title,
                              style: const TextStyle(
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              subtitle,
                              style: TextStyle(
                                color: colorScheme.onSurfaceVariant,
                                fontSize: 13,
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(width: 12),
                      _buildExpandableTileChevron(expanded, colorScheme),
                      const SizedBox(width: 4),
                      trailing,
                    ],
                  ),
                ),
              ),
            ),
          ),
          TweenAnimationBuilder<double>(
            tween: Tween<double>(begin: 0, end: expanded ? 1 : 0),
            duration: const Duration(milliseconds: 240),
            curve: Curves.easeOutCubic,
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 16),
              child: expandedChild,
            ),
            builder: (BuildContext context, double value, Widget? child) {
              return ClipRect(
                child: Align(
                  alignment: Alignment.topCenter,
                  heightFactor: value,
                  child: IgnorePointer(
                    ignoring: value < 0.99,
                    child: Opacity(
                      opacity: value.clamp(0, 1),
                      child: Transform.translate(
                        offset: Offset(0, (1 - value) * -14),
                        child: child,
                      ),
                    ),
                  ),
                ),
              );
            },
          ),
        ],
      ),
    );
  }

  Widget _buildExpandableTileChevron(bool expanded, ColorScheme colorScheme) {
    return AnimatedContainer(
      duration: const Duration(milliseconds: 220),
      curve: Curves.easeOutCubic,
      width: 28,
      height: 28,
      decoration: BoxDecoration(
        color: expanded
            ? colorScheme.primary.withValues(alpha: 0.12)
            : Colors.transparent,
        shape: BoxShape.circle,
      ),
      child: AnimatedRotation(
        turns: expanded ? 0.25 : 0.0,
        duration: const Duration(milliseconds: 220),
        curve: Curves.easeInOutCubic,
        child: Icon(
          Icons.chevron_right_rounded,
          size: 20,
          color: expanded ? colorScheme.primary : colorScheme.onSurfaceVariant,
        ),
      ),
    );
  }

  Widget _buildNetworkSpeedThresholdPanel(AppStrings s) {
    return _buildNetworkSpeedThresholdSetting(s);
  }

  Widget _buildWeatherOptionsPanel(AppStrings s) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: _expandablePanelBackgroundColor(colorScheme),
        borderRadius: BorderRadius.circular(20),
        border: _expandablePanelBorder(colorScheme),
      ),
      child: _buildInlinePanelSwitchRow(
        title: s.smartWeatherLockscreenOnlyTitle,
        subtitle: s.smartWeatherLockscreenOnlySubtitle,
        value: _smartWeatherLockscreenOnly,
        onChanged: _setSmartWeatherLockscreenOnly,
      ),
    );
  }

  Widget _buildExternalDevicesOptionsPanel(AppStrings s) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: _expandablePanelBackgroundColor(colorScheme),
        borderRadius: BorderRadius.circular(20),
        border: _expandablePanelBorder(colorScheme),
      ),
      child: _buildInlinePanelSwitchRow(
        title: s.smartExternalDevicesIgnoreDebuggingTitle,
        subtitle: s.smartExternalDevicesIgnoreDebuggingSubtitle,
        value: _smartExternalDevicesIgnoreDebugging,
        onChanged: _setSmartExternalDevicesIgnoreDebugging,
      ),
    );
  }

  Widget _buildNativeProgressOptionsPanel(AppStrings s) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: _expandablePanelBackgroundColor(colorScheme),
        borderRadius: BorderRadius.circular(20),
        border: _expandablePanelBorder(colorScheme),
      ),
      child: _buildInlinePanelSwitchRow(
        title: s.textProgressTitle,
        subtitle: s.textProgressSubtitle,
        value: _textProgressEnabled,
        onChanged: _setTextProgressEnabled,
      ),
    );
  }

  Widget _buildNotificationDedupOptionsPanel(AppStrings s) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: _expandablePanelBackgroundColor(colorScheme),
        borderRadius: BorderRadius.circular(20),
        border: _expandablePanelBorder(colorScheme),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          _buildNotificationDedupStatusesSwitchRow(s),
          const SizedBox(height: 14),
          _buildModernDropdown(
            label: s.modeLabel,
            currentValue: _notificationDedupPackageMode,
            items: PackageMode.values.map((PackageMode mode) {
              return DropdownMenuItem<PackageMode>(
                value: mode,
                child: Text(
                  _getModeLabel(mode, s),
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              );
            }).toList(),
            onChanged: (PackageMode? value) {
              if (value == null) {
                return;
              }
              LiveBridgeHaptics.selection();
              setState(() => _notificationDedupPackageMode = value);
              unawaited(_persistRules(target: _PackagePickerTarget.dedup));
            },
            onTap: LiveBridgeHaptics.openSurface,
          ),
          const SizedBox(height: 16),
          _selectedAppsNote(
            noteId: 'dedup',
            selectedPackages: _parsePackagesFromInput(
              _notificationDedupRulesController.text,
            ),
            s: s,
          ),
          const SizedBox(height: 8),
          Text(
            s.pickAppsHint,
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
              color: colorScheme.onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 16),
          _ruleButtonsRow(
            onPick: () =>
                _openPackagePicker(target: _PackagePickerTarget.dedup),
            s: s,
          ),
        ],
      ),
    );
  }

  Color _expandablePanelBackgroundColor(ColorScheme colorScheme) {
    if (colorScheme.brightness == Brightness.light) {
      return Colors.white;
    }
    return colorScheme.surface.withValues(alpha: 0.86);
  }

  Border? _expandablePanelBorder(ColorScheme colorScheme) {
    if (colorScheme.brightness != Brightness.light) {
      return null;
    }
    return Border.all(color: colorScheme.outlineVariant.withValues(alpha: 0.5));
  }

  Widget _buildInlinePanelSwitchRow({
    required String title,
    required String subtitle,
    required bool value,
    required ValueChanged<bool>? onChanged,
  }) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;

    return Material(
      color: Colors.transparent,
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        splashFactory: NoSplash.splashFactory,
        overlayColor: const WidgetStatePropertyAll<Color>(Colors.transparent),
        splashColor: Colors.transparent,
        highlightColor: Colors.transparent,
        hoverColor: Colors.transparent,
        focusColor: Colors.transparent,
        onTap: onChanged == null ? null : () => onChanged(!value),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 4),
          child: Row(
            children: <Widget>[
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Text(
                      title,
                      style: const TextStyle(fontWeight: FontWeight.w600),
                    ),
                    if (subtitle.isNotEmpty) ...<Widget>[
                      const SizedBox(height: 4),
                      Text(
                        subtitle,
                        style: TextStyle(
                          color: colorScheme.onSurfaceVariant,
                          fontSize: 13,
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              const SizedBox(width: 12),
              Switch.adaptive(
                value: value,
                onChanged: onChanged,
                activeThumbColor: colorScheme.primary,
              ),
            ],
          ),
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
    required String noteId,
    required Set<String> selectedPackages,
    required AppStrings s,
  }) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;
    final int selectedCount = selectedPackages.length;
    final bool expanded = _expandedSelectedAppNotes.contains(noteId);

    final List<InstalledApp> selectedApps =
        selectedPackages.map((String packageName) {
          return _previewAppsByPackage[packageName] ??
              InstalledApp(packageName: packageName, label: packageName);
        }).toList()..sort(
          (a, b) => a.label.toLowerCase().compareTo(b.label.toLowerCase()),
        );

    Future<void> toggleExpanded() async {
      if (selectedCount == 0) {
        return;
      }
      final bool opening = !expanded;
      LiveBridgeHaptics.expand(opening);
      setState(() {
        if (opening) {
          _expandedSelectedAppNotes.add(noteId);
        } else {
          _expandedSelectedAppNotes.remove(noteId);
        }
      });
      if (opening) {
        await _ensurePreviewAppsLoaded();
      }
    }

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: colorScheme.primaryContainer.withValues(alpha: 0.4),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        children: <Widget>[
          GestureDetector(
            onTap: selectedCount == 0 ? null : toggleExpanded,
            behavior: HitTestBehavior.opaque,
            child: Padding(
              padding: const EdgeInsets.symmetric(vertical: 2),
              child: Row(
                children: <Widget>[
                  Icon(
                    Icons.checklist_rounded,
                    size: 20,
                    color: colorScheme.primary,
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      selectedCount == 0
                          ? s.noAppsSelected
                          : s.selectedAppsCount(selectedCount),
                      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                        fontWeight: FontWeight.w600,
                        color: colorScheme.primary.withValues(alpha: 0.8),
                      ),
                    ),
                  ),
                  Icon(
                    expanded
                        ? Icons.keyboard_arrow_up_rounded
                        : Icons.keyboard_arrow_down_rounded,
                    color: selectedCount == 0
                        ? colorScheme.onSurfaceVariant
                        : colorScheme.primary,
                  ),
                ],
              ),
            ),
          ),
          if (expanded && selectedApps.isNotEmpty) ...<Widget>[
            const SizedBox(height: 8),
            const Divider(height: 1),
            const SizedBox(height: 8),
            ...selectedApps.map((InstalledApp app) {
              final Widget compactIcon = app.icon != null
                  ? ClipRRect(
                      borderRadius: BorderRadius.circular(7),
                      child: Image.memory(
                        app.icon!,
                        width: 24,
                        height: 24,
                        fit: BoxFit.cover,
                      ),
                    )
                  : Container(
                      width: 24,
                      height: 24,
                      decoration: BoxDecoration(
                        color: colorScheme.primary.withValues(alpha: 0.16),
                        borderRadius: BorderRadius.circular(7),
                      ),
                      child: Center(
                        child: Text(
                          app.label.isNotEmpty
                              ? app.label[0].toUpperCase()
                              : '?',
                          style: TextStyle(
                            fontSize: 11,
                            fontWeight: FontWeight.w700,
                            color: colorScheme.primary,
                          ),
                        ),
                      ),
                    );
              return Padding(
                padding: const EdgeInsets.symmetric(vertical: 4),
                child: Row(
                  children: <Widget>[
                    compactIcon,
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text(
                        app.label,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                  ],
                ),
              );
            }),
          ],
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

class _GithubDictionaryInfo {
  const _GithubDictionaryInfo({required this.raw, required this.normalized});

  final String raw;
  final String normalized;
}

class _NetworkSpeedUnitDropdown extends StatefulWidget {
  const _NetworkSpeedUnitDropdown({
    required this.label,
    required this.values,
    required this.optionLabelBuilder,
    required this.onChanged,
  });

  final String label;
  final Set<NetworkSpeedUnit> values;
  final String Function(NetworkSpeedUnit unit) optionLabelBuilder;
  final ValueChanged<Set<NetworkSpeedUnit>> onChanged;

  @override
  State<_NetworkSpeedUnitDropdown> createState() =>
      _NetworkSpeedUnitDropdownState();
}

class _NetworkSpeedUnitDropdownState extends State<_NetworkSpeedUnitDropdown> {
  final MenuController _menuController = MenuController();
  late Set<NetworkSpeedUnit> _draftValues = Set<NetworkSpeedUnit>.from(
    widget.values,
  );
  Set<NetworkSpeedUnit> _committedValues = <NetworkSpeedUnit>{};
  bool _menuOpen = false;

  @override
  void initState() {
    super.initState();
    _committedValues = Set<NetworkSpeedUnit>.from(widget.values);
  }

  @override
  void didUpdateWidget(covariant _NetworkSpeedUnitDropdown oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (!_menuOpen && !_sameSelection(widget.values, _draftValues)) {
      _draftValues = Set<NetworkSpeedUnit>.from(widget.values);
      _committedValues = Set<NetworkSpeedUnit>.from(widget.values);
    }
  }

  bool _sameSelection(Set<NetworkSpeedUnit> a, Set<NetworkSpeedUnit> b) {
    if (identical(a, b)) {
      return true;
    }
    if (a.length != b.length) {
      return false;
    }
    return a.containsAll(b);
  }

  void _toggleUnit(NetworkSpeedUnit unit) {
    final bool checked = _draftValues.contains(unit);
    final Set<NetworkSpeedUnit> next;
    if (checked) {
      next = Set<NetworkSpeedUnit>.from(_draftValues)..remove(unit);
    } else if (unit == NetworkSpeedUnit.auto) {
      next = <NetworkSpeedUnit>{NetworkSpeedUnit.auto};
    } else {
      next = Set<NetworkSpeedUnit>.from(_draftValues)
        ..remove(NetworkSpeedUnit.auto)
        ..add(unit);
    }
    setState(() => _draftValues = next);
  }

  void _commitSelection() {
    final Set<NetworkSpeedUnit> next = Set<NetworkSpeedUnit>.from(_draftValues);
    if (_sameSelection(next, _committedValues)) {
      return;
    }
    _committedValues = next;
    widget.onChanged(next);
  }

  void _toggleMenu() {
    if (_menuController.isOpen) {
      _menuController.close();
      return;
    }
    LiveBridgeHaptics.openSurface();
    _menuController.open();
  }

  String _summaryLabel() {
    if (_draftValues.isEmpty || _draftValues.contains(NetworkSpeedUnit.auto)) {
      return widget.optionLabelBuilder(NetworkSpeedUnit.auto);
    }
    return kNetworkSpeedUnitValues
        .where(
          (NetworkSpeedUnit unit) =>
              unit != NetworkSpeedUnit.auto && _draftValues.contains(unit),
        )
        .map(widget.optionLabelBuilder)
        .join(', ');
  }

  @override
  Widget build(BuildContext context) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;
    final bool isLight = colorScheme.brightness == Brightness.light;
    final Color fieldColor = isLight
        ? Colors.white
        : colorScheme.surfaceContainerLow;
    final Color menuColor = isLight
        ? Colors.white
        : colorScheme.surfaceContainer;
    final Color borderColor = colorScheme.primary.withValues(
      alpha: isLight ? 0.5 : 0.65,
    );

    return MenuAnchor(
      controller: _menuController,
      consumeOutsideTap: true,
      crossAxisUnconstrained: false,
      style: MenuStyle(
        backgroundColor: WidgetStatePropertyAll<Color>(menuColor),
        shape: WidgetStatePropertyAll<OutlinedBorder>(
          RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        ),
        side: WidgetStatePropertyAll<BorderSide>(
          BorderSide(color: borderColor, width: 1.2),
        ),
        padding: const WidgetStatePropertyAll<EdgeInsets>(EdgeInsets.all(0)),
      ),
      onOpen: () => setState(() => _menuOpen = true),
      onClose: () {
        if (mounted) {
          setState(() => _menuOpen = false);
        }
        _commitSelection();
      },
      menuChildren: <Widget>[
        SizedBox(
          width: 320,
          child: Padding(
            padding: const EdgeInsets.all(12),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: <Widget>[
                for (final NetworkSpeedUnit unit in kNetworkSpeedUnitValues)
                  _NetworkSpeedUnitMenuRow(
                    title: widget.optionLabelBuilder(unit),
                    checked: _draftValues.contains(unit),
                    onTap: () {
                      LiveBridgeHaptics.selection();
                      _toggleUnit(unit);
                    },
                  ),
                const SizedBox(height: 8),
                Align(
                  alignment: Alignment.centerRight,
                  child: FilledButton.tonalIcon(
                    onPressed: _menuController.close,
                    icon: const Icon(Icons.check_rounded, size: 18),
                    label: Text(
                      MaterialLocalizations.of(context).okButtonLabel,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
      builder:
          (BuildContext context, MenuController controller, Widget? child) {
            return Material(
              color: Colors.transparent,
              child: InkWell(
                borderRadius: BorderRadius.circular(20),
                onTap: _toggleMenu,
                child: InputDecorator(
                  isEmpty: false,
                  decoration: InputDecoration(
                    labelText: widget.label,
                    labelStyle: TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w500,
                      color: colorScheme.onSurfaceVariant,
                    ),
                    filled: true,
                    fillColor: fieldColor,
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(20),
                      borderSide: BorderSide(color: borderColor, width: 1.2),
                    ),
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(20),
                      borderSide: BorderSide(color: borderColor, width: 1.2),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(20),
                      borderSide: BorderSide(
                        color: colorScheme.primary,
                        width: 1.8,
                      ),
                    ),
                    contentPadding: const EdgeInsets.symmetric(
                      horizontal: 20,
                      vertical: 16,
                    ),
                    suffixIcon: Icon(
                      _menuOpen
                          ? Icons.keyboard_arrow_up_rounded
                          : Icons.keyboard_arrow_down_rounded,
                      color: colorScheme.onSurfaceVariant,
                    ),
                  ),
                  child: Text(
                    _summaryLabel(),
                    style: const TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ),
              ),
            );
          },
    );
  }
}

class _NetworkSpeedUnitMenuRow extends StatelessWidget {
  const _NetworkSpeedUnitMenuRow({
    required this.title,
    required this.checked,
    required this.onTap,
  });

  final String title;
  final bool checked;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;
    return Material(
      color: Colors.transparent,
      child: InkWell(
        borderRadius: BorderRadius.circular(14),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 4),
          child: Row(
            children: <Widget>[
              Checkbox(
                value: checked,
                onChanged: (_) => onTap(),
                visualDensity: VisualDensity.compact,
                materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Text(
                  title,
                  style: TextStyle(
                    fontSize: 15,
                    fontWeight: checked ? FontWeight.w700 : FontWeight.w500,
                    color: checked
                        ? colorScheme.onSurface
                        : colorScheme.onSurfaceVariant,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _NetworkSpeedPrefixSheet extends StatefulWidget {
  const _NetworkSpeedPrefixSheet({
    required this.title,
    required this.initialValue,
    required this.defaultValue,
    required this.resetLabel,
    required this.saveLabel,
  });

  final String title;
  final String initialValue;
  final String defaultValue;
  final String resetLabel;
  final String saveLabel;

  @override
  State<_NetworkSpeedPrefixSheet> createState() =>
      _NetworkSpeedPrefixSheetState();
}

class _NetworkSpeedPrefixSheetState extends State<_NetworkSpeedPrefixSheet> {
  late final TextEditingController _controller = TextEditingController(
    text: widget.initialValue,
  );

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      top: false,
      child: Padding(
        padding: EdgeInsets.only(
          left: 24,
          right: 24,
          top: 8,
          bottom: 24 + MediaQuery.of(context).viewInsets.bottom,
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            Text(
              widget.title,
              style: Theme.of(
                context,
              ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800),
            ),
            const SizedBox(height: 20),
            TextField(
              controller: _controller,
              autofocus: true,
              decoration: InputDecoration(
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(18),
                ),
              ),
            ),
            const SizedBox(height: 24),
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: <Widget>[
                OutlinedButton.icon(
                  onPressed: () {
                    HapticFeedback.selectionClick();
                    Navigator.of(context).pop(widget.defaultValue);
                  },
                  icon: const Icon(Icons.refresh_rounded),
                  label: Text(widget.resetLabel),
                ),
                const SizedBox(width: 12),
                FilledButton.icon(
                  onPressed: () {
                    HapticFeedback.selectionClick();
                    Navigator.of(context).pop(_controller.text);
                  },
                  icon: const Icon(Icons.check_rounded),
                  label: Text(widget.saveLabel),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
