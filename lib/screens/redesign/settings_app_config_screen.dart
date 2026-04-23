import 'dart:async';

import 'package:flutter/material.dart';

import '../../l10n/app_strings.dart';
import '../../platform/livebridge_platform.dart';
import '../../theme/livebridge_tokens.dart';
import '../../utils/livebridge_haptics.dart';
import '../../widgets/redesign/lb_detail_screen.dart';
import '../../widgets/redesign/lb_icon.dart';
import '../../widgets/redesign/lb_list_component.dart';
import '../../widgets/redesign/lb_slider.dart';
import '../../widgets/redesign/lb_toggle.dart';

class SettingsAppConfigScreen extends StatefulWidget {
  const SettingsAppConfigScreen({super.key});

  @override
  State<SettingsAppConfigScreen> createState() =>
      _SettingsAppConfigScreenState();
}

class _SettingsAppConfigScreenState extends State<SettingsAppConfigScreen> {
  static const int _logLengthMinMb = 1;
  static const int _logLengthMaxMb = 25;
  static const int _bytesPerMb = 1024 * 1024;

  bool _altBackgroundMode = false;
  bool _syncDnd = true;
  bool _preventDismissing = false;
  bool _conversionLogEnabled = false;
  int _logLengthMb = 5;
  double _logLengthSliderValue = (5 - _logLengthMinMb).toDouble();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      unawaited(_loadState());
    });
  }

  Future<void> _loadState() async {
    try {
      final Future<bool> altBackgroundFuture =
          LiveBridgePlatform.getKeepAliveForegroundEnabled();
      final Future<bool> syncDndFuture = LiveBridgePlatform.getSyncDndEnabled();
      final Future<bool> preventDismissingFuture =
          LiveBridgePlatform.getPreventMirrorDismissEnabled();
      final Future<bool> conversionLogEnabledFuture =
          LiveBridgePlatform.getConversionLogEnabled();
      final Future<int> conversionLogMaxBytesFuture =
          LiveBridgePlatform.getConversionLogMaxBytes();

      final bool altBackgroundMode = await altBackgroundFuture;
      final bool syncDnd = await syncDndFuture;
      final bool preventDismissing = await preventDismissingFuture;
      final bool conversionLogEnabled = await conversionLogEnabledFuture;
      final int conversionLogMaxBytes = await conversionLogMaxBytesFuture;

      if (!mounted) {
        return;
      }

      final int normalizedLogLengthMb = (conversionLogMaxBytes / _bytesPerMb)
          .round()
          .clamp(_logLengthMinMb, _logLengthMaxMb);

      setState(() {
        _altBackgroundMode = altBackgroundMode;
        _syncDnd = syncDnd;
        _preventDismissing = preventDismissing;
        _conversionLogEnabled = conversionLogEnabled;
        _logLengthMb = normalizedLogLengthMb;
        _logLengthSliderValue = _sliderPositionForMb(normalizedLogLengthMb);
      });
    } catch (_) {}
  }

  Future<void> _setAltBackgroundMode(bool value) async {
    if (value == _altBackgroundMode) {
      return;
    }
    setState(() => _altBackgroundMode = value);
    await LiveBridgePlatform.setKeepAliveForegroundEnabled(value);
  }

  Future<void> _setSyncDnd(bool value) async {
    if (value == _syncDnd) {
      return;
    }
    setState(() => _syncDnd = value);
    await LiveBridgePlatform.setSyncDndEnabled(value);
  }

  Future<void> _setPreventDismissing(bool value) async {
    if (value == _preventDismissing) {
      return;
    }
    setState(() => _preventDismissing = value);
    await LiveBridgePlatform.setPreventMirrorDismissEnabled(value);
  }

  Future<void> _setConversionLogEnabled(bool value) async {
    if (value == _conversionLogEnabled) {
      return;
    }
    setState(() => _conversionLogEnabled = value);
    await LiveBridgePlatform.setConversionLogEnabled(value);
  }

  Future<void> _setConversionLogLengthMb(int value) async {
    final int normalized = value.clamp(_logLengthMinMb, _logLengthMaxMb);
    if (_logLengthMb != normalized && mounted) {
      setState(() => _logLengthMb = normalized);
    }
    await LiveBridgePlatform.setConversionLogMaxBytes(normalized * _bytesPerMb);
  }

  int _snapLogLengthMb(double sliderValue) {
    return (_logLengthMinMb + sliderValue.round()).clamp(
      _logLengthMinMb,
      _logLengthMaxMb,
    );
  }

  double _sliderPositionForMb(int value) {
    return (value - _logLengthMinMb)
        .clamp(0, _logLengthMaxMb - _logLengthMinMb)
        .toDouble();
  }

  @override
  Widget build(BuildContext context) {
    final AppStrings strings = AppStrings.of(context);
    final LbPalette palette = LbPalette.of(context);
    final double sliderMax = (_logLengthMaxMb - _logLengthMinMb).toDouble();

    final List<LbListItemData> primaryItems = <LbListItemData>[
      LbListItemData(
        title: strings.keepAliveForegroundTitle,
        showChevron: false,
        toggleValue: _altBackgroundMode,
        onToggle: (bool value) {
          unawaited(_setAltBackgroundMode(value));
        },
        onTap: () {
          final bool nextValue = !_altBackgroundMode;
          unawaited(LiveBridgeHaptics.toggle(nextValue));
          unawaited(_setAltBackgroundMode(nextValue));
        },
      ),
      LbListItemData(
        title: strings.syncDndTitle,
        showChevron: false,
        toggleValue: _syncDnd,
        onToggle: (bool value) {
          unawaited(_setSyncDnd(value));
        },
        onTap: () {
          final bool nextValue = !_syncDnd;
          unawaited(LiveBridgeHaptics.toggle(nextValue));
          unawaited(_setSyncDnd(nextValue));
        },
      ),
      LbListItemData(
        title: strings.preventDismissingTitle,
        showChevron: false,
        toggleValue: _preventDismissing,
        onToggle: (bool value) {
          unawaited(_setPreventDismissing(value));
        },
        onTap: () {
          final bool nextValue = !_preventDismissing;
          unawaited(LiveBridgeHaptics.toggle(nextValue));
          unawaited(_setPreventDismissing(nextValue));
        },
      ),
    ];

    return LbDetailScreen(
      title: strings.appConfigTitle,
      children: <Widget>[
        LbListComponent(
          items: primaryItems,
          rowHeight: LbSpacing.recentRowHeight,
          extendDividersToEnd: true,
        ),
        const SizedBox(height: LbSpacing.md),
        Container(
          width: double.infinity,
          decoration: BoxDecoration(
            color: palette.surface,
            borderRadius: BorderRadius.circular(LbRadius.card),
          ),
          child: Column(
            children: <Widget>[
              SizedBox(
                height: LbSpacing.recentRowHeight,
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: LbSpacing.md),
                  child: Row(
                    children: <Widget>[
                      const SizedBox(width: LbSpacing.listTextOnlyInset),
                      Expanded(
                        child: Text(
                          strings.conversionLogTitle,
                          style: LbTextStyles.body.copyWith(
                            color: palette.textPrimary,
                          ),
                        ),
                      ),
                      LbToggle(
                        value: _conversionLogEnabled,
                        onChanged: (bool value) {
                          unawaited(_setConversionLogEnabled(value));
                        },
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
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Row(
                      children: <Widget>[
                        Expanded(
                          child: Text(
                            strings.logLengthTitle,
                            style: LbTextStyles.body.copyWith(
                              color: palette.textPrimary,
                            ),
                          ),
                        ),
                        const SizedBox(width: LbSpacing.sliderValueGap),
                        Text(
                          '${_logLengthMb}MB',
                          style: LbTextStyles.body.copyWith(
                            color: palette.textSecondary,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: LbSpacing.sliderSectionGap),
                    Row(
                      children: <Widget>[
                        LbIcon(
                          symbol: LbIconSymbol.gitBranch,
                          size: 28,
                          color: palette.textPrimary,
                        ),
                        const SizedBox(width: LbSpacing.md),
                        Expanded(
                          child: LbSlider(
                            value: _logLengthSliderValue,
                            min: 0,
                            max: sliderMax,
                            onChanged: (double value) {
                              setState(() {
                                _logLengthSliderValue = value;
                                _logLengthMb = _snapLogLengthMb(value);
                              });
                            },
                            onChangeEnd: (double value) {
                              final int nextValue = _snapLogLengthMb(value);
                              setState(() {
                                _logLengthSliderValue = _sliderPositionForMb(
                                  nextValue,
                                );
                                _logLengthMb = nextValue;
                              });
                              unawaited(_setConversionLogLengthMb(nextValue));
                            },
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}
