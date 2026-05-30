import 'dart:async';

import 'package:flutter/material.dart';

import '../../l10n/app_strings.dart';
import '../../models/app_models.dart';
import '../../platform/livebridge_platform.dart';
import '../../theme/livebridge_tokens.dart';
import '../../utils/livebridge_haptics.dart';
import '../../widgets/notification_color_picker.dart';
import '../../widgets/redesign/lb_detail_screen.dart';
import '../../widgets/redesign/lb_icon.dart';
import '../../widgets/redesign/lb_info_title.dart';
import '../../widgets/redesign/lb_list_component.dart';
import '../../widgets/redesign/lb_modal_bottom_sheet.dart';
import '../../widgets/redesign/lb_selection_indicator.dart';
import '../../widgets/redesign/lb_slider.dart';

class RulesNetworkConnectionsScreen extends StatefulWidget {
  const RulesNetworkConnectionsScreen({super.key});

  @override
  State<RulesNetworkConnectionsScreen> createState() =>
      _RulesNetworkConnectionsScreenState();
}

class _RulesNetworkConnectionsScreenState
    extends State<RulesNetworkConnectionsScreen> {
  static const int _thresholdStepBytesPerSecond = 8 * 1024;
  static const int _thresholdMaxBytesPerSecond = 1024 * 1024;

  bool _vpnEnabled = true;
  bool _vpnLockscreenOnly = false;
  bool _externalDevicesEnabled = true;
  bool _ignoreDebuggingDevices = false;
  bool _networkSpeedEnabled = false;
  int _networkSpeedThresholdBytesPerSecond = 0;
  double _networkSpeedSliderValue = 0;
  NetworkSpeedDisplayMode _networkSpeedDisplayMode =
      NetworkSpeedDisplayMode.total;
  String _networkSpeedUploadPrefix = kDefaultNetworkSpeedUploadPrefix;
  String _networkSpeedDownloadPrefix = kDefaultNetworkSpeedDownloadPrefix;
  Set<NetworkSpeedUnit> _networkSpeedUnits = <NetworkSpeedUnit>{
    NetworkSpeedUnit.auto,
  };
  bool _networkSpeedPrioritizeUpload = false;
  bool _networkSpeedLockscreenOnly = false;
  bool _networkSpeedChipBackgroundDisabled = false;
  bool _networkSpeedRegularNotificationEnabled = false;
  bool _networkSpeedDailyUsageEnabled = false;
  int _networkSpeedNotificationColorArgb = defaultNotificationColorArgb;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      unawaited(_loadState());
    });
  }

  Future<void> _loadState() async {
    try {
      final Future<bool> vpnEnabledFuture =
          LiveBridgePlatform.getSmartVpnEnabled();
      final Future<bool> vpnLockscreenOnlyFuture =
          LiveBridgePlatform.getSmartVpnLockscreenOnly();
      final Future<bool> externalDevicesEnabledFuture =
          LiveBridgePlatform.getSmartExternalDevicesEnabled();
      final Future<bool> ignoreDebuggingFuture =
          LiveBridgePlatform.getSmartExternalDevicesIgnoreDebugging();
      final Future<bool> networkSpeedEnabledFuture =
          LiveBridgePlatform.getNetworkSpeedEnabled();
      final Future<int> networkSpeedThresholdFuture =
          LiveBridgePlatform.getNetworkSpeedMinThresholdBytesPerSecond();
      final Future<String> networkSpeedDisplayModeFuture =
          LiveBridgePlatform.getNetworkSpeedDisplayMode();
      final Future<String> networkSpeedUploadPrefixFuture =
          LiveBridgePlatform.getNetworkSpeedUploadPrefix();
      final Future<String> networkSpeedDownloadPrefixFuture =
          LiveBridgePlatform.getNetworkSpeedDownloadPrefix();
      final Future<String> networkSpeedUnitFuture =
          LiveBridgePlatform.getNetworkSpeedUnit();
      final Future<bool> networkSpeedPrioritizeUploadFuture =
          LiveBridgePlatform.getNetworkSpeedPrioritizeUpload();
      final Future<bool> networkSpeedLockscreenOnlyFuture =
          LiveBridgePlatform.getNetworkSpeedLockscreenOnly();
      final Future<bool> networkSpeedChipBackgroundDisabledFuture =
          LiveBridgePlatform.getNetworkSpeedChipBackgroundDisabled();
      final Future<bool> networkSpeedRegularNotificationFuture =
          LiveBridgePlatform.getNetworkSpeedRegularNotificationEnabled();
      final Future<bool> networkSpeedDailyUsageFuture =
          LiveBridgePlatform.getNetworkSpeedDailyUsageEnabled();
      final Future<int> networkSpeedNotificationColorFuture =
          LiveBridgePlatform.getNetworkSpeedNotificationColorArgb();

      final bool vpnEnabled = await vpnEnabledFuture;
      final bool vpnLockscreenOnly = await vpnLockscreenOnlyFuture;
      final bool externalDevicesEnabled = await externalDevicesEnabledFuture;
      final bool ignoreDebuggingDevices = await ignoreDebuggingFuture;
      final bool networkSpeedEnabled = await networkSpeedEnabledFuture;
      final int networkSpeedThresholdBytesPerSecond =
          await networkSpeedThresholdFuture;
      final String networkSpeedDisplayMode =
          await networkSpeedDisplayModeFuture;
      final String networkSpeedUploadPrefix =
          await networkSpeedUploadPrefixFuture;
      final String networkSpeedDownloadPrefix =
          await networkSpeedDownloadPrefixFuture;
      final String networkSpeedUnit = await networkSpeedUnitFuture;
      final bool networkSpeedPrioritizeUpload =
          await networkSpeedPrioritizeUploadFuture;
      final bool networkSpeedLockscreenOnly =
          await networkSpeedLockscreenOnlyFuture;
      final bool networkSpeedChipBackgroundDisabled =
          await networkSpeedChipBackgroundDisabledFuture;
      final bool networkSpeedRegularNotificationEnabled =
          await networkSpeedRegularNotificationFuture;
      final bool networkSpeedDailyUsageEnabled =
          await networkSpeedDailyUsageFuture;
      final int networkSpeedNotificationColor =
          await networkSpeedNotificationColorFuture;

      if (!mounted) {
        return;
      }

      final int normalizedThreshold = networkSpeedThresholdBytesPerSecond.clamp(
        0,
        _thresholdMaxBytesPerSecond,
      );

      setState(() {
        _vpnEnabled = vpnEnabled;
        _vpnLockscreenOnly = vpnLockscreenOnly;
        _externalDevicesEnabled = externalDevicesEnabled;
        _ignoreDebuggingDevices = ignoreDebuggingDevices;
        _networkSpeedEnabled = networkSpeedEnabled;
        _networkSpeedThresholdBytesPerSecond = normalizedThreshold;
        _networkSpeedSliderValue = _sliderPositionForBytesPerSecond(
          normalizedThreshold,
        );
        _networkSpeedDisplayMode = NetworkSpeedDisplayModeId.from(
          networkSpeedDisplayMode,
        );
        _networkSpeedUploadPrefix = networkSpeedUploadPrefix;
        _networkSpeedDownloadPrefix = networkSpeedDownloadPrefix;
        _networkSpeedUnits = NetworkSpeedUnitSelection.parse(networkSpeedUnit);
        if (_networkSpeedUnits.isEmpty) {
          _networkSpeedUnits = <NetworkSpeedUnit>{NetworkSpeedUnit.auto};
        }
        _networkSpeedPrioritizeUpload = networkSpeedPrioritizeUpload;
        _networkSpeedLockscreenOnly = networkSpeedLockscreenOnly;
        _networkSpeedChipBackgroundDisabled =
            networkSpeedChipBackgroundDisabled;
        _networkSpeedRegularNotificationEnabled =
            networkSpeedRegularNotificationEnabled;
        _networkSpeedDailyUsageEnabled = networkSpeedDailyUsageEnabled;
        _networkSpeedNotificationColorArgb = _opaqueNotificationColor(
          networkSpeedNotificationColor,
        );
      });
    } catch (_) {}
  }

  Future<void> _setVpnEnabled(bool value) async {
    if (value == _vpnEnabled) {
      return;
    }
    setState(() => _vpnEnabled = value);
    await LiveBridgePlatform.setSmartVpnEnabled(value);
  }

  Future<void> _setVpnLockscreenOnly(bool value) async {
    if (value == _vpnLockscreenOnly) {
      return;
    }
    setState(() => _vpnLockscreenOnly = value);
    await LiveBridgePlatform.setSmartVpnLockscreenOnly(value);
  }

  Future<void> _setExternalDevicesEnabled(bool value) async {
    if (value == _externalDevicesEnabled) {
      return;
    }
    setState(() => _externalDevicesEnabled = value);
    await LiveBridgePlatform.setSmartExternalDevicesEnabled(value);
  }

  Future<void> _setIgnoreDebuggingDevices(bool value) async {
    if (value == _ignoreDebuggingDevices) {
      return;
    }
    setState(() => _ignoreDebuggingDevices = value);
    await LiveBridgePlatform.setSmartExternalDevicesIgnoreDebugging(value);
  }

  Future<void> _setNetworkSpeedEnabled(bool value) async {
    if (value == _networkSpeedEnabled) {
      return;
    }
    setState(() => _networkSpeedEnabled = value);
    await LiveBridgePlatform.setNetworkSpeedEnabled(value);
  }

  Future<void> _setNetworkSpeedThresholdBytesPerSecond(int value) async {
    final int normalized = value.clamp(0, _thresholdMaxBytesPerSecond);
    if (_networkSpeedThresholdBytesPerSecond != normalized && mounted) {
      setState(() => _networkSpeedThresholdBytesPerSecond = normalized);
    }
    await LiveBridgePlatform.setNetworkSpeedMinThresholdBytesPerSecond(
      normalized,
    );
  }

  Future<void> _setNetworkSpeedDisplayMode(
    NetworkSpeedDisplayMode value,
  ) async {
    if (value == _networkSpeedDisplayMode) {
      return;
    }
    setState(() => _networkSpeedDisplayMode = value);
    await LiveBridgePlatform.setNetworkSpeedDisplayMode(value.id);
  }

  Future<void> _setNetworkSpeedPrefix({
    required bool forUpload,
    required String value,
  }) async {
    if (forUpload) {
      if (value == _networkSpeedUploadPrefix) {
        return;
      }
      setState(() => _networkSpeedUploadPrefix = value);
      await LiveBridgePlatform.setNetworkSpeedUploadPrefix(value);
      return;
    }
    if (value == _networkSpeedDownloadPrefix) {
      return;
    }
    setState(() => _networkSpeedDownloadPrefix = value);
    await LiveBridgePlatform.setNetworkSpeedDownloadPrefix(value);
  }

  Future<void> _setNetworkSpeedUnits(Set<NetworkSpeedUnit> values) async {
    final Set<NetworkSpeedUnit> normalized = values.isEmpty
        ? <NetworkSpeedUnit>{NetworkSpeedUnit.auto}
        : values;
    if (_sameUnitSelection(_networkSpeedUnits, normalized)) {
      return;
    }
    setState(() => _networkSpeedUnits = Set<NetworkSpeedUnit>.from(normalized));
    await LiveBridgePlatform.setNetworkSpeedUnit(
      NetworkSpeedUnitSelection.encode(normalized),
    );
  }

  Future<void> _setNetworkSpeedPrioritizeUpload(bool value) async {
    if (value == _networkSpeedPrioritizeUpload) {
      return;
    }
    setState(() => _networkSpeedPrioritizeUpload = value);
    await LiveBridgePlatform.setNetworkSpeedPrioritizeUpload(value);
  }

  Future<void> _setNetworkSpeedLockscreenOnly(bool value) async {
    if (value == _networkSpeedLockscreenOnly) {
      return;
    }
    setState(() => _networkSpeedLockscreenOnly = value);
    await LiveBridgePlatform.setNetworkSpeedLockscreenOnly(value);
  }

  Future<void> _setNetworkSpeedChipBackgroundDisabled(bool value) async {
    if (value == _networkSpeedChipBackgroundDisabled) {
      return;
    }
    setState(() => _networkSpeedChipBackgroundDisabled = value);
    await LiveBridgePlatform.setNetworkSpeedChipBackgroundDisabled(value);
  }

  Future<void> _setNetworkSpeedRegularNotificationEnabled(bool value) async {
    if (value == _networkSpeedRegularNotificationEnabled) {
      return;
    }
    setState(() => _networkSpeedRegularNotificationEnabled = value);
    await LiveBridgePlatform.setNetworkSpeedRegularNotificationEnabled(value);
  }

  Future<void> _setNetworkSpeedDailyUsageEnabled(bool value) async {
    if (value == _networkSpeedDailyUsageEnabled) {
      return;
    }
    setState(() => _networkSpeedDailyUsageEnabled = value);
    await LiveBridgePlatform.setNetworkSpeedDailyUsageEnabled(value);
  }

  Future<void> _setNetworkSpeedNotificationColorArgb(int value) async {
    final int normalized = _opaqueNotificationColor(value);
    if (normalized == _networkSpeedNotificationColorArgb) {
      return;
    }
    setState(() => _networkSpeedNotificationColorArgb = normalized);
    await LiveBridgePlatform.setNetworkSpeedNotificationColorArgb(normalized);
  }

  Future<void> _openNetworkSpeedColorPicker(AppStrings strings) async {
    final int? selectedColor = await showLbModalBottomSheet<int>(
      context: context,
      builder: (BuildContext context) {
        return NotificationColorPickerSheet(
          title: strings.selectNotificationColorTitle,
          doneLabel: strings.save,
          initialColorArgb: _networkSpeedNotificationColorArgb,
        );
      },
    );
    if (selectedColor == null) {
      return;
    }
    unawaited(LiveBridgeHaptics.selection());
    unawaited(_setNetworkSpeedNotificationColorArgb(selectedColor));
  }

  void _resetNetworkSpeedNotificationColor() {
    if (_networkSpeedNotificationColorArgb == defaultNotificationColorArgb) {
      return;
    }
    unawaited(LiveBridgeHaptics.warning());
    unawaited(
      _setNetworkSpeedNotificationColorArgb(defaultNotificationColorArgb),
    );
  }

  Future<void> _openDisplayModeSheet(AppStrings strings) async {
    final NetworkSpeedDisplayMode? selected =
        await showLbModalBottomSheet<NetworkSpeedDisplayMode>(
          context: context,
          builder: (BuildContext context) {
            return _SingleSelectSheet<NetworkSpeedDisplayMode>(
              title: strings.networkSpeedDisplayContentTitle,
              selectedValue: _networkSpeedDisplayMode,
              options: <_SheetOption<NetworkSpeedDisplayMode>>[
                _SheetOption<NetworkSpeedDisplayMode>(
                  value: NetworkSpeedDisplayMode.total,
                  label: strings.networkSpeedDisplayModeTotal,
                ),
                _SheetOption<NetworkSpeedDisplayMode>(
                  value: NetworkSpeedDisplayMode.upload,
                  label: strings.networkSpeedDisplayModeUpload,
                ),
                _SheetOption<NetworkSpeedDisplayMode>(
                  value: NetworkSpeedDisplayMode.download,
                  label: strings.networkSpeedDisplayModeDownload,
                ),
              ],
            );
          },
        );
    if (selected == null) {
      return;
    }
    unawaited(LiveBridgeHaptics.selection());
    unawaited(_setNetworkSpeedDisplayMode(selected));
  }

  Future<void> _openPrefixSheet({
    required AppStrings strings,
    required bool forUpload,
  }) async {
    final String currentValue = forUpload
        ? _networkSpeedUploadPrefix
        : _networkSpeedDownloadPrefix;
    final String? updated = await showLbModalBottomSheet<String>(
      context: context,
      builder: (BuildContext context) {
        return _NetworkSpeedPrefixSheet(
          title: forUpload
              ? strings.networkSpeedUploadPrefixTitle
              : strings.networkSpeedDownloadPrefixTitle,
          initialValue: currentValue,
          defaultValue: forUpload
              ? kDefaultNetworkSpeedUploadPrefix
              : kDefaultNetworkSpeedDownloadPrefix,
          resetLabel: strings.resetToDefault,
          saveLabel: strings.save,
        );
      },
    );
    if (updated == null) {
      return;
    }
    unawaited(LiveBridgeHaptics.confirm());
    unawaited(_setNetworkSpeedPrefix(forUpload: forUpload, value: updated));
  }

  Future<void> _openUnitsSheet(AppStrings strings) async {
    final Set<NetworkSpeedUnit>? updated =
        await showLbModalBottomSheet<Set<NetworkSpeedUnit>>(
          context: context,
          builder: (BuildContext context) {
            return _NetworkSpeedUnitSheet(
              title: strings.networkSpeedUnitTitle,
              initialValues: _networkSpeedUnits,
              optionLabelBuilder: (NetworkSpeedUnit unit) =>
                  _networkSpeedUnitLabel(unit, strings),
              confirmLabel: MaterialLocalizations.of(context).okButtonLabel,
            );
          },
        );
    if (updated == null) {
      return;
    }
    unawaited(LiveBridgeHaptics.confirm());
    unawaited(_setNetworkSpeedUnits(updated));
  }

  int _snapThresholdBytesPerSecond(double sliderValue) {
    return (sliderValue.round() * _thresholdStepBytesPerSecond)
        .clamp(0, _thresholdMaxBytesPerSecond)
        .toInt();
  }

  double _sliderPositionForBytesPerSecond(int bytesPerSecond) {
    return (bytesPerSecond / _thresholdStepBytesPerSecond)
        .clamp(0, _thresholdMaxBytesPerSecond / _thresholdStepBytesPerSecond)
        .toDouble();
  }

  bool _sameUnitSelection(Set<NetworkSpeedUnit> a, Set<NetworkSpeedUnit> b) {
    if (a.length != b.length) {
      return false;
    }
    return a.containsAll(b);
  }

  String _formatNetworkSpeedBytesPerSecond(int bytesPerSecond) {
    final int value = bytesPerSecond.clamp(0, 1 << 31).toInt();
    if (value <= 0) {
      return AppStrings.of(context).networkSpeedThresholdAlways;
    }
    if (value < 1024 * 1024) {
      return _formatCompactNetworkSpeedValue(value / 1024, 'kB/s');
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
    return '$formatted $suffix';
  }

  int _opaqueNotificationColor(int colorArgb) {
    return 0xFF000000 | (colorArgb & 0x00FFFFFF);
  }

  String _formatNotificationColorHex(int colorArgb) {
    final int rgb = colorArgb & 0x00FFFFFF;
    return '#${rgb.toRadixString(16).padLeft(6, '0').toUpperCase()}';
  }

  String _networkSpeedDisplayModeLabel(
    AppStrings strings,
    NetworkSpeedDisplayMode mode,
  ) {
    switch (mode) {
      case NetworkSpeedDisplayMode.total:
        return strings.networkSpeedDisplayModeTotal;
      case NetworkSpeedDisplayMode.upload:
        return strings.networkSpeedDisplayModeUpload;
      case NetworkSpeedDisplayMode.download:
        return strings.networkSpeedDisplayModeDownload;
    }
  }

  String _networkSpeedUnitLabel(NetworkSpeedUnit unit, AppStrings strings) {
    switch (unit) {
      case NetworkSpeedUnit.auto:
        return strings.networkSpeedUnitAuto;
      case NetworkSpeedUnit.bytes:
        return strings.networkSpeedUnitBytes;
      case NetworkSpeedUnit.kilobytes:
        return strings.networkSpeedUnitKilobytes;
      case NetworkSpeedUnit.megabytes:
        return strings.networkSpeedUnitMegabytes;
      case NetworkSpeedUnit.gigabytes:
        return strings.networkSpeedUnitGigabytes;
    }
  }

  String _networkSpeedUnitsSummary(AppStrings strings) {
    if (NetworkSpeedUnitSelection.usesAuto(_networkSpeedUnits)) {
      return strings.networkSpeedUnitAuto;
    }
    return kNetworkSpeedUnitValues
        .where(
          (NetworkSpeedUnit unit) =>
              unit != NetworkSpeedUnit.auto &&
              _networkSpeedUnits.contains(unit),
        )
        .map((NetworkSpeedUnit unit) => _networkSpeedUnitLabel(unit, strings))
        .join(', ');
  }

  @override
  Widget build(BuildContext context) {
    final AppStrings strings = AppStrings.of(context);
    final LbPalette palette = LbPalette.of(context);
    final double sliderMax =
        _thresholdMaxBytesPerSecond / _thresholdStepBytesPerSecond;

    final List<LbListItemData> primaryItems = <LbListItemData>[
      LbListItemData(
        title: strings.vpnsTitle,
        description: strings.vpnsDescription,
        showChevron: false,
        toggleValue: _vpnEnabled,
        onToggle: (bool value) {
          unawaited(_setVpnEnabled(value));
        },
        onTap: () {
          final bool nextValue = !_vpnEnabled;
          unawaited(LiveBridgeHaptics.toggle(nextValue));
          unawaited(_setVpnEnabled(nextValue));
        },
      ),
      LbListItemData(
        title: strings.smartVpnLockscreenOnlyTitle,
        description: strings.smartVpnLockscreenOnlySubtitle,
        showChevron: false,
        enabled: _vpnEnabled,
        toggleValue: _vpnLockscreenOnly,
        onToggle: _vpnEnabled
            ? (bool value) {
                unawaited(_setVpnLockscreenOnly(value));
              }
            : null,
        onTap: _vpnEnabled
            ? () {
                final bool nextValue = !_vpnLockscreenOnly;
                unawaited(LiveBridgeHaptics.toggle(nextValue));
                unawaited(_setVpnLockscreenOnly(nextValue));
              }
            : null,
      ),
      LbListItemData(
        title: strings.externalDevicesTitle,
        description: strings.externalDevicesDescription,
        showChevron: false,
        toggleValue: _externalDevicesEnabled,
        onToggle: (bool value) {
          unawaited(_setExternalDevicesEnabled(value));
        },
        onTap: () {
          final bool nextValue = !_externalDevicesEnabled;
          unawaited(LiveBridgeHaptics.toggle(nextValue));
          unawaited(_setExternalDevicesEnabled(nextValue));
        },
      ),
      LbListItemData(
        title: strings.ignoreDebuggingDevicesTitle,
        description: strings.ignoreDebuggingDevicesDescription,
        showChevron: false,
        toggleValue: _ignoreDebuggingDevices,
        enabled: _externalDevicesEnabled,
        onToggle: _externalDevicesEnabled
            ? (bool value) {
                unawaited(_setIgnoreDebuggingDevices(value));
              }
            : null,
        onTap: _externalDevicesEnabled
            ? () {
                final bool nextValue = !_ignoreDebuggingDevices;
                unawaited(LiveBridgeHaptics.toggle(nextValue));
                unawaited(_setIgnoreDebuggingDevices(nextValue));
              }
            : null,
      ),
    ];

    final List<LbListItemData> networkSpeedItems = <LbListItemData>[
      LbListItemData(
        title: strings.networkSpeedEnabledTitle,
        description: strings.networkSpeedEnabledSubtitle,
        showChevron: false,
        toggleValue: _networkSpeedEnabled,
        onToggle: (bool value) {
          unawaited(_setNetworkSpeedEnabled(value));
        },
        onTap: () {
          final bool nextValue = !_networkSpeedEnabled;
          unawaited(LiveBridgeHaptics.toggle(nextValue));
          unawaited(_setNetworkSpeedEnabled(nextValue));
        },
      ),
      LbListItemData(
        title: strings.networkSpeedDisplayContentTitle,
        subtitle: _networkSpeedDisplayModeLabel(
          strings,
          _networkSpeedDisplayMode,
        ),
        enabled: _networkSpeedEnabled,
        onTap: _networkSpeedEnabled
            ? () {
                unawaited(_openDisplayModeSheet(strings));
              }
            : null,
      ),
      LbListItemData(
        title: strings.networkSpeedUploadPrefixTitle,
        subtitle: strings.networkSpeedCurrentValue(_networkSpeedUploadPrefix),
        enabled: _networkSpeedEnabled,
        onTap: _networkSpeedEnabled
            ? () {
                unawaited(_openPrefixSheet(strings: strings, forUpload: true));
              }
            : null,
      ),
      LbListItemData(
        title: strings.networkSpeedDownloadPrefixTitle,
        subtitle: strings.networkSpeedCurrentValue(_networkSpeedDownloadPrefix),
        enabled: _networkSpeedEnabled,
        onTap: _networkSpeedEnabled
            ? () {
                unawaited(_openPrefixSheet(strings: strings, forUpload: false));
              }
            : null,
      ),
      LbListItemData(
        title: strings.networkSpeedUnitTitle,
        subtitle: _networkSpeedUnitsSummary(strings),
        enabled: _networkSpeedEnabled,
        onTap: _networkSpeedEnabled
            ? () {
                unawaited(_openUnitsSheet(strings));
              }
            : null,
      ),
      LbListItemData(
        title: strings.notificationColorTitle,
        subtitle: _formatNotificationColorHex(
          _networkSpeedNotificationColorArgb,
        ),
        showChevron: false,
        enabled: _networkSpeedEnabled,
        leadingChild: Opacity(
          opacity: _networkSpeedEnabled ? 1 : 0.45,
          child: NotificationColorSwatch(
            colorArgb: _networkSpeedNotificationColorArgb,
          ),
        ),
        trailingWidgetWidth: 44,
        trailingWidget: IconButton(
          tooltip: strings.resetToDefault,
          onPressed: _networkSpeedEnabled
              ? _resetNetworkSpeedNotificationColor
              : null,
          icon: LbIcon(
            symbol: LbIconSymbol.restore,
            size: 20,
            color: _networkSpeedEnabled
                ? palette.textSecondary
                : palette.textMuted,
          ),
          padding: EdgeInsets.zero,
          constraints: const BoxConstraints.tightFor(width: 40, height: 40),
        ),
        onTap: _networkSpeedEnabled
            ? () {
                unawaited(_openNetworkSpeedColorPicker(strings));
              }
            : null,
      ),
      LbListItemData(
        title: strings.networkSpeedPrioritizeUploadTitle,
        description: strings.networkSpeedPrioritizeUploadSubtitle,
        showChevron: false,
        enabled: _networkSpeedEnabled,
        toggleValue: _networkSpeedPrioritizeUpload,
        onToggle: _networkSpeedEnabled
            ? (bool value) {
                unawaited(_setNetworkSpeedPrioritizeUpload(value));
              }
            : null,
        onTap: _networkSpeedEnabled
            ? () {
                final bool nextValue = !_networkSpeedPrioritizeUpload;
                unawaited(LiveBridgeHaptics.toggle(nextValue));
                unawaited(_setNetworkSpeedPrioritizeUpload(nextValue));
              }
            : null,
      ),
      LbListItemData(
        title: strings.networkSpeedLockscreenOnlyTitle,
        description: strings.networkSpeedLockscreenOnlySubtitle,
        showChevron: false,
        enabled: _networkSpeedEnabled,
        toggleValue: _networkSpeedLockscreenOnly,
        onToggle: _networkSpeedEnabled
            ? (bool value) {
                unawaited(_setNetworkSpeedLockscreenOnly(value));
              }
            : null,
        onTap: _networkSpeedEnabled
            ? () {
                final bool nextValue = !_networkSpeedLockscreenOnly;
                unawaited(LiveBridgeHaptics.toggle(nextValue));
                unawaited(_setNetworkSpeedLockscreenOnly(nextValue));
              }
            : null,
      ),
      LbListItemData(
        title: strings.networkSpeedDisableChipBackgroundTitle,
        description: strings.networkSpeedDisableChipBackgroundSubtitle,
        showChevron: false,
        enabled: _networkSpeedEnabled,
        toggleValue: _networkSpeedChipBackgroundDisabled,
        onToggle: _networkSpeedEnabled
            ? (bool value) {
                unawaited(_setNetworkSpeedChipBackgroundDisabled(value));
              }
            : null,
        onTap: _networkSpeedEnabled
            ? () {
                final bool nextValue = !_networkSpeedChipBackgroundDisabled;
                unawaited(LiveBridgeHaptics.toggle(nextValue));
                unawaited(_setNetworkSpeedChipBackgroundDisabled(nextValue));
              }
            : null,
      ),
      LbListItemData(
        title: strings.networkSpeedRegularNotificationTitle,
        description: strings.networkSpeedRegularNotificationSubtitle,
        showChevron: false,
        enabled: _networkSpeedEnabled,
        toggleValue: _networkSpeedRegularNotificationEnabled,
        onToggle: _networkSpeedEnabled
            ? (bool value) {
                unawaited(_setNetworkSpeedRegularNotificationEnabled(value));
              }
            : null,
        onTap: _networkSpeedEnabled
            ? () {
                final bool nextValue = !_networkSpeedRegularNotificationEnabled;
                unawaited(LiveBridgeHaptics.toggle(nextValue));
                unawaited(
                  _setNetworkSpeedRegularNotificationEnabled(nextValue),
                );
              }
            : null,
      ),
      LbListItemData(
        title: strings.networkSpeedDailyUsageTitle,
        description: strings.networkSpeedDailyUsageSubtitle,
        showChevron: false,
        enabled: _networkSpeedEnabled,
        toggleValue: _networkSpeedDailyUsageEnabled,
        onToggle: _networkSpeedEnabled
            ? (bool value) {
                unawaited(_setNetworkSpeedDailyUsageEnabled(value));
              }
            : null,
        onTap: _networkSpeedEnabled
            ? () {
                final bool nextValue = !_networkSpeedDailyUsageEnabled;
                unawaited(LiveBridgeHaptics.toggle(nextValue));
                unawaited(_setNetworkSpeedDailyUsageEnabled(nextValue));
              }
            : null,
      ),
    ];

    return LbDetailScreen(
      title: strings.networkConnectionsTitle,
      children: <Widget>[
        LbListComponent(items: primaryItems, extendDividersToEnd: true),
        const SizedBox(height: LbSpacing.detailSectionGap),
        Container(
          width: double.infinity,
          decoration: BoxDecoration(
            color: palette.surface,
            borderRadius: BorderRadius.circular(LbRadius.card),
          ),
          child: Column(
            children: <Widget>[
              SizedBox(
                height: LbSpacing.listRowHeight,
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: LbSpacing.md),
                  child: Row(
                    children: <Widget>[
                      const SizedBox(width: LbSpacing.listTextOnlyInset),
                      Expanded(
                        child: LbInfoTitle(
                          title: strings.networkSpeedThresholdTitle,
                          description: strings.networkSpeedThresholdDescription,
                          titleStyle: LbTextStyles.body.copyWith(
                            color: palette.textPrimary,
                          ),
                        ),
                      ),
                      Text(
                        _formatNetworkSpeedBytesPerSecond(
                          _networkSpeedThresholdBytesPerSecond,
                        ),
                        style: LbTextStyles.body.copyWith(
                          color: palette.textSecondary,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              Padding(
                padding: const EdgeInsets.only(
                  left: LbSpacing.listTextOnlyInset + LbSpacing.md,
                ),
                child: Divider(
                  height: LbSpacing.recentSeparatorThickness,
                  thickness: LbSpacing.recentSeparatorThickness,
                  color: palette.recentSeparator,
                ),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(
                  LbSpacing.md + LbSpacing.listTextOnlyInset,
                  LbSpacing.md,
                  LbSpacing.md,
                  LbSpacing.md,
                ),
                child: AnimatedOpacity(
                  duration: const Duration(milliseconds: 180),
                  opacity: _networkSpeedEnabled ? 1 : 0.45,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Row(
                        children: <Widget>[
                          LbIcon(
                            symbol: LbIconSymbol.dashboardFilled,
                            size: 28,
                            color: palette.textPrimary,
                          ),
                          const SizedBox(width: LbSpacing.md),
                          Expanded(
                            child: LbSlider(
                              value: _networkSpeedSliderValue,
                              min: 0,
                              max: sliderMax,
                              enabled: _networkSpeedEnabled,
                              onChanged: (double value) {
                                setState(() {
                                  _networkSpeedSliderValue = value;
                                  _networkSpeedThresholdBytesPerSecond =
                                      _snapThresholdBytesPerSecond(value);
                                });
                              },
                              onChangeEnd: (double value) {
                                final int nextValue =
                                    _snapThresholdBytesPerSecond(value);
                                setState(() {
                                  _networkSpeedSliderValue =
                                      _sliderPositionForBytesPerSecond(
                                        nextValue,
                                      );
                                  _networkSpeedThresholdBytesPerSecond =
                                      nextValue;
                                });
                                unawaited(
                                  _setNetworkSpeedThresholdBytesPerSecond(
                                    nextValue,
                                  ),
                                );
                              },
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: LbSpacing.detailSectionGap),
        LbListComponent(items: networkSpeedItems, extendDividersToEnd: true),
      ],
    );
  }
}

class _SheetOption<T> {
  const _SheetOption({required this.value, required this.label});

  final T value;
  final String label;
}

class _SingleSelectSheet<T> extends StatelessWidget {
  const _SingleSelectSheet({
    required this.title,
    required this.selectedValue,
    required this.options,
  });

  final String title;
  final T selectedValue;
  final List<_SheetOption<T>> options;

  @override
  Widget build(BuildContext context) {
    final LbPalette palette = LbPalette.of(context);

    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Text(
          title,
          style: LbTextStyles.title.copyWith(color: palette.textPrimary),
        ),
        const SizedBox(height: LbSpacing.lg),
        for (int index = 0; index < options.length; index += 1) ...<Widget>[
          InkWell(
            borderRadius: BorderRadius.circular(LbRadius.card),
            onTap: () {
              Navigator.of(context).pop<T>(options[index].value);
            },
            child: Padding(
              padding: const EdgeInsets.symmetric(
                horizontal: LbSpacing.xs,
                vertical: LbSpacing.sm,
              ),
              child: Row(
                children: <Widget>[
                  Expanded(
                    child: Text(
                      options[index].label,
                      style: LbTextStyles.body.copyWith(
                        color: palette.textPrimary,
                      ),
                    ),
                  ),
                  LbSelectionIndicator(
                    selected: options[index].value == selectedValue,
                  ),
                ],
              ),
            ),
          ),
          if (index != options.length - 1)
            Divider(
              height: LbSpacing.recentSeparatorThickness * 2,
              thickness: LbSpacing.recentSeparatorThickness,
              color: palette.recentSeparator,
            ),
        ],
      ],
    );
  }
}

class _NetworkSpeedUnitSheet extends StatefulWidget {
  const _NetworkSpeedUnitSheet({
    required this.title,
    required this.initialValues,
    required this.optionLabelBuilder,
    required this.confirmLabel,
  });

  final String title;
  final Set<NetworkSpeedUnit> initialValues;
  final String Function(NetworkSpeedUnit unit) optionLabelBuilder;
  final String confirmLabel;

  @override
  State<_NetworkSpeedUnitSheet> createState() => _NetworkSpeedUnitSheetState();
}

class _NetworkSpeedUnitSheetState extends State<_NetworkSpeedUnitSheet> {
  late Set<NetworkSpeedUnit> _selectedValues = widget.initialValues.isEmpty
      ? <NetworkSpeedUnit>{NetworkSpeedUnit.auto}
      : Set<NetworkSpeedUnit>.from(widget.initialValues);

  void _toggleUnit(NetworkSpeedUnit unit) {
    final bool checked = _selectedValues.contains(unit);
    final Set<NetworkSpeedUnit> next;
    if (checked) {
      next = Set<NetworkSpeedUnit>.from(_selectedValues)..remove(unit);
    } else if (unit == NetworkSpeedUnit.auto) {
      next = <NetworkSpeedUnit>{NetworkSpeedUnit.auto};
    } else {
      next = Set<NetworkSpeedUnit>.from(_selectedValues)
        ..remove(NetworkSpeedUnit.auto)
        ..add(unit);
    }
    setState(() {
      _selectedValues = next.isEmpty
          ? <NetworkSpeedUnit>{NetworkSpeedUnit.auto}
          : next;
    });
  }

  @override
  Widget build(BuildContext context) {
    final LbPalette palette = LbPalette.of(context);

    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Text(
          widget.title,
          style: LbTextStyles.title.copyWith(color: palette.textPrimary),
        ),
        const SizedBox(height: LbSpacing.lg),
        for (
          int index = 0;
          index < kNetworkSpeedUnitValues.length;
          index += 1
        ) ...<Widget>[
          InkWell(
            borderRadius: BorderRadius.circular(LbRadius.card),
            onTap: () {
              unawaited(LiveBridgeHaptics.selection());
              _toggleUnit(kNetworkSpeedUnitValues[index]);
            },
            child: Padding(
              padding: const EdgeInsets.symmetric(
                horizontal: LbSpacing.xs,
                vertical: LbSpacing.sm,
              ),
              child: Row(
                children: <Widget>[
                  Expanded(
                    child: Text(
                      widget.optionLabelBuilder(kNetworkSpeedUnitValues[index]),
                      style: LbTextStyles.body.copyWith(
                        color: palette.textPrimary,
                      ),
                    ),
                  ),
                  LbSelectionIndicator(
                    selected: _selectedValues.contains(
                      kNetworkSpeedUnitValues[index],
                    ),
                  ),
                ],
              ),
            ),
          ),
          if (index != kNetworkSpeedUnitValues.length - 1)
            Divider(
              height: LbSpacing.recentSeparatorThickness * 2,
              thickness: LbSpacing.recentSeparatorThickness,
              color: palette.recentSeparator,
            ),
        ],
        const SizedBox(height: LbSpacing.lg),
        Align(
          alignment: Alignment.centerRight,
          child: FilledButton(
            onPressed: () {
              Navigator.of(context).pop<Set<NetworkSpeedUnit>>(
                Set<NetworkSpeedUnit>.from(_selectedValues),
              );
            },
            child: Text(widget.confirmLabel),
          ),
        ),
      ],
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
    final LbPalette palette = LbPalette.of(context);

    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.viewInsetsOf(context).bottom),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text(
            widget.title,
            style: LbTextStyles.title.copyWith(color: palette.textPrimary),
          ),
          const SizedBox(height: LbSpacing.lg),
          TextField(
            controller: _controller,
            autofocus: true,
            style: LbTextStyles.body.copyWith(color: palette.textPrimary),
            decoration: InputDecoration(
              filled: true,
              fillColor: palette.surface,
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(LbRadius.card),
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(LbRadius.card),
                borderSide: BorderSide(color: palette.recentSeparator),
              ),
              focusedBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(LbRadius.card),
                borderSide: BorderSide(color: palette.accent),
              ),
            ),
          ),
          const SizedBox(height: LbSpacing.lg),
          Row(
            mainAxisAlignment: MainAxisAlignment.end,
            children: <Widget>[
              TextButton(
                onPressed: () {
                  Navigator.of(context).pop<String>(widget.defaultValue);
                },
                child: Text(widget.resetLabel),
              ),
              const SizedBox(width: LbSpacing.sm),
              FilledButton(
                onPressed: () {
                  Navigator.of(context).pop<String>(_controller.text);
                },
                child: Text(widget.saveLabel),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
