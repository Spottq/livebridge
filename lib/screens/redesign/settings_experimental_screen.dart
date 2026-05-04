import 'dart:async';

import 'package:flutter/material.dart';

import '../../l10n/app_strings.dart';
import '../../platform/livebridge_platform.dart';
import '../../theme/livebridge_tokens.dart';
import '../../utils/livebridge_haptics.dart';
import '../../widgets/redesign/lb_detail_screen.dart';
import '../../widgets/redesign/lb_icon.dart';
import '../../widgets/redesign/lb_info_title.dart';
import '../../widgets/redesign/lb_list_component.dart';
import '../../widgets/redesign/lb_slider.dart';
import '../../widgets/redesign/lb_toggle.dart';

class SettingsExperimentalScreen extends StatefulWidget {
  const SettingsExperimentalScreen({super.key});

  @override
  State<SettingsExperimentalScreen> createState() =>
      _SettingsExperimentalScreenState();
}

class _SettingsExperimentalScreenState
    extends State<SettingsExperimentalScreen> {
  static const int _updateFrequencyMinMs = 750;
  static const int _updateFrequencyMaxMs = 3000;
  static const int _updateFrequencyStepMs = 10;

  bool _otpDedupEnabled = false;
  bool _smartDedupEnabled = false;
  bool _animatedIslandEnabled = false;
  int _animatedIslandUpdateFrequencyMs = 2250;
  double _animatedIslandSliderValue = 150;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      unawaited(_loadState());
    });
  }

  Future<void> _loadState() async {
    try {
      final Future<bool> otpDedupFuture =
          LiveBridgePlatform.getOtpRemoveOriginalMessageEnabled();
      final Future<bool> smartDedupFuture =
          LiveBridgePlatform.getSmartRemoveOriginalMessageEnabled();
      final Future<bool> animatedIslandEnabledFuture =
          LiveBridgePlatform.getAnimatedIslandEnabled();
      final Future<int> animatedIslandUpdateFrequencyFuture =
          LiveBridgePlatform.getAnimatedIslandUpdateFrequencyMs();

      final bool otpDedupEnabled = await otpDedupFuture;
      final bool smartDedupEnabled = await smartDedupFuture;
      final bool animatedIslandEnabled = await animatedIslandEnabledFuture;
      final int animatedIslandUpdateFrequencyMs =
          await animatedIslandUpdateFrequencyFuture;

      if (!mounted) {
        return;
      }

      final int normalizedFrequencyMs = animatedIslandUpdateFrequencyMs.clamp(
        _updateFrequencyMinMs,
        _updateFrequencyMaxMs,
      );

      setState(() {
        _otpDedupEnabled = otpDedupEnabled;
        _smartDedupEnabled = smartDedupEnabled;
        _animatedIslandEnabled = animatedIslandEnabled;
        _animatedIslandUpdateFrequencyMs = normalizedFrequencyMs;
        _animatedIslandSliderValue = _sliderPositionForMs(
          normalizedFrequencyMs,
        );
      });
    } catch (_) {}
  }

  Future<void> _setOtpDedupEnabled(bool value) async {
    if (value == _otpDedupEnabled) {
      return;
    }
    setState(() => _otpDedupEnabled = value);
    await LiveBridgePlatform.setOtpRemoveOriginalMessageEnabled(value);
  }

  Future<void> _setSmartDedupEnabled(bool value) async {
    if (value == _smartDedupEnabled) {
      return;
    }
    setState(() => _smartDedupEnabled = value);
    await LiveBridgePlatform.setSmartRemoveOriginalMessageEnabled(value);
  }

  Future<void> _setAnimatedIslandEnabled(bool value) async {
    if (value == _animatedIslandEnabled) {
      return;
    }
    setState(() => _animatedIslandEnabled = value);
    await LiveBridgePlatform.setAnimatedIslandEnabled(value);
  }

  Future<void> _setAnimatedIslandUpdateFrequencyMs(int value) async {
    final int normalized = value.clamp(
      _updateFrequencyMinMs,
      _updateFrequencyMaxMs,
    );
    if (_animatedIslandUpdateFrequencyMs != normalized && mounted) {
      setState(() => _animatedIslandUpdateFrequencyMs = normalized);
    }
    await LiveBridgePlatform.setAnimatedIslandUpdateFrequencyMs(normalized);
  }

  int _snapUpdateFrequencyMs(double sliderValue) {
    return (_updateFrequencyMinMs +
            (sliderValue.round() * _updateFrequencyStepMs))
        .clamp(_updateFrequencyMinMs, _updateFrequencyMaxMs)
        .toInt();
  }

  double _sliderPositionForMs(int value) {
    return ((value - _updateFrequencyMinMs) / _updateFrequencyStepMs)
        .clamp(
          0,
          (_updateFrequencyMaxMs - _updateFrequencyMinMs) /
              _updateFrequencyStepMs,
        )
        .toDouble();
  }

  String _formatUpdateFrequency(int milliseconds) {
    return '${(milliseconds / 1000).toStringAsFixed(2)}s';
  }

  @override
  Widget build(BuildContext context) {
    final AppStrings strings = AppStrings.of(context);
    final LbPalette palette = LbPalette.of(context);
    final double sliderMax =
        (_updateFrequencyMaxMs - _updateFrequencyMinMs) /
        _updateFrequencyStepMs;

    final List<LbListItemData> primaryItems = <LbListItemData>[
      LbListItemData(
        title: strings.otpDedupTitle,
        description: strings.otpDedupDescription,
        showChevron: false,
        toggleValue: _otpDedupEnabled,
        onToggle: (bool value) {
          unawaited(_setOtpDedupEnabled(value));
        },
        onTap: () {
          final bool nextValue = !_otpDedupEnabled;
          unawaited(LiveBridgeHaptics.toggle(nextValue));
          unawaited(_setOtpDedupEnabled(nextValue));
        },
      ),
      LbListItemData(
        title: strings.smartConversionDedupTitle,
        description: strings.smartConversionDedupDescription,
        showChevron: false,
        toggleValue: _smartDedupEnabled,
        onToggle: (bool value) {
          unawaited(_setSmartDedupEnabled(value));
        },
        onTap: () {
          final bool nextValue = !_smartDedupEnabled;
          unawaited(LiveBridgeHaptics.toggle(nextValue));
          unawaited(_setSmartDedupEnabled(nextValue));
        },
      ),
    ];

    return LbDetailScreen(
      title: strings.experimentalTitle,
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
                        child: LbInfoTitle(
                          title: strings.animatedIslandRedesignTitle,
                          description: strings.animatedIslandDescription,
                          titleStyle: LbTextStyles.body.copyWith(
                            color: palette.textPrimary,
                          ),
                        ),
                      ),
                      LbToggle(
                        value: _animatedIslandEnabled,
                        onChanged: (bool value) {
                          unawaited(_setAnimatedIslandEnabled(value));
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
                          child: LbInfoTitle(
                            title: strings.updateFrequencyTitle,
                            description: strings.updateFrequencyDescription,
                            titleStyle: LbTextStyles.body.copyWith(
                              color: palette.textPrimary,
                            ),
                          ),
                        ),
                        const SizedBox(width: LbSpacing.sliderValueGap),
                        Text(
                          _formatUpdateFrequency(
                            _animatedIslandUpdateFrequencyMs,
                          ),
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
                          symbol: LbIconSymbol.settingsTuning,
                          size: 28,
                          color: palette.textPrimary,
                        ),
                        const SizedBox(width: LbSpacing.md),
                        Expanded(
                          child: LbSlider(
                            value: _animatedIslandSliderValue,
                            min: 0,
                            max: sliderMax,
                            onChanged: (double value) {
                              setState(() {
                                _animatedIslandSliderValue = value;
                                _animatedIslandUpdateFrequencyMs =
                                    _snapUpdateFrequencyMs(value);
                              });
                            },
                            onChangeEnd: (double value) {
                              final int nextValue = _snapUpdateFrequencyMs(
                                value,
                              );
                              setState(() {
                                _animatedIslandSliderValue =
                                    _sliderPositionForMs(nextValue);
                                _animatedIslandUpdateFrequencyMs = nextValue;
                              });
                              unawaited(
                                _setAnimatedIslandUpdateFrequencyMs(nextValue),
                              );
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
