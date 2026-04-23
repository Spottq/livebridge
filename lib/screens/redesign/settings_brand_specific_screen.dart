import 'dart:async';

import 'package:flutter/material.dart';

import '../../l10n/app_strings.dart';
import '../../platform/livebridge_platform.dart';
import '../../theme/livebridge_tokens.dart';
import '../../widgets/redesign/lb_detail_screen.dart';
import '../../widgets/redesign/lb_icon.dart';
import '../../widgets/redesign/lb_slider.dart';
import '../../widgets/redesign/lb_toggle.dart';

class SettingsBrandSpecificScreen extends StatefulWidget {
  const SettingsBrandSpecificScreen({super.key});

  @override
  State<SettingsBrandSpecificScreen> createState() =>
      _SettingsBrandSpecificScreenState();
}

class _SettingsBrandSpecificScreenState
    extends State<SettingsBrandSpecificScreen> {
  static const int _aospCuttingLengthMin = 7;
  static const int _aospCuttingLengthMax = 12;

  bool _hyperBridgeEnabled = false;
  bool _aospCuttingEnabled = false;
  int _aospCuttingLength = 7;
  double _aospCuttingLengthSliderValue = (7 - _aospCuttingLengthMin).toDouble();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      unawaited(_loadState());
    });
  }

  Future<void> _loadState() async {
    try {
      final Future<bool> hyperBridgeFuture =
          LiveBridgePlatform.getHyperBridgeEnabled();
      final Future<bool> aospCuttingFuture =
          LiveBridgePlatform.getAospCuttingEnabled();
      final Future<int> aospCuttingLengthFuture =
          LiveBridgePlatform.getAospCuttingLength();

      final bool hyperBridgeEnabled = await hyperBridgeFuture;
      final bool aospCuttingEnabled = await aospCuttingFuture;
      final int aospCuttingLength = await aospCuttingLengthFuture;

      if (!mounted) {
        return;
      }

      final int normalizedLength = aospCuttingLength.clamp(
        _aospCuttingLengthMin,
        _aospCuttingLengthMax,
      );

      setState(() {
        _hyperBridgeEnabled = hyperBridgeEnabled;
        _aospCuttingEnabled = aospCuttingEnabled;
        _aospCuttingLength = normalizedLength;
        _aospCuttingLengthSliderValue = _sliderPositionForLength(
          normalizedLength,
        );
      });
    } catch (_) {}
  }

  Future<void> _setHyperBridgeEnabled(bool value) async {
    if (value == _hyperBridgeEnabled) {
      return;
    }
    setState(() => _hyperBridgeEnabled = value);
    await LiveBridgePlatform.setHyperBridgeEnabled(value);
  }

  Future<void> _setAospCuttingEnabled(bool value) async {
    if (value == _aospCuttingEnabled) {
      return;
    }
    setState(() => _aospCuttingEnabled = value);
    await LiveBridgePlatform.setAospCuttingEnabled(value);
  }

  Future<void> _setAospCuttingLength(int value) async {
    final int normalized = value.clamp(
      _aospCuttingLengthMin,
      _aospCuttingLengthMax,
    );
    if (_aospCuttingLength != normalized && mounted) {
      setState(() => _aospCuttingLength = normalized);
    }
    await LiveBridgePlatform.setAospCuttingLength(normalized);
  }

  int _snapLength(double sliderValue) {
    return (_aospCuttingLengthMin + sliderValue.round()).clamp(
      _aospCuttingLengthMin,
      _aospCuttingLengthMax,
    );
  }

  double _sliderPositionForLength(int value) {
    return (value - _aospCuttingLengthMin)
        .clamp(0, _aospCuttingLengthMax - _aospCuttingLengthMin)
        .toDouble();
  }

  @override
  Widget build(BuildContext context) {
    final AppStrings strings = AppStrings.of(context);
    final LbPalette palette = LbPalette.of(context);
    final double sliderMax = (_aospCuttingLengthMax - _aospCuttingLengthMin)
        .toDouble();

    return LbDetailScreen(
      title: strings.brandSpecificTitle,
      children: <Widget>[
        Container(
          width: double.infinity,
          decoration: BoxDecoration(
            color: palette.surface,
            borderRadius: BorderRadius.circular(LbRadius.card),
          ),
          child: SizedBox(
            height: LbSpacing.recentRowHeight,
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: LbSpacing.md),
              child: Row(
                children: <Widget>[
                  const SizedBox(width: LbSpacing.listTextOnlyInset),
                  Expanded(
                    child: Text(
                      strings.xiaomiHyperIslandTitle,
                      style: LbTextStyles.body.copyWith(
                        color: palette.textPrimary,
                      ),
                    ),
                  ),
                  LbToggle(
                    value: _hyperBridgeEnabled,
                    onChanged: (bool value) {
                      unawaited(_setHyperBridgeEnabled(value));
                    },
                  ),
                ],
              ),
            ),
          ),
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
                          strings.aospCuttingTitle,
                          style: LbTextStyles.body.copyWith(
                            color: palette.textPrimary,
                          ),
                        ),
                      ),
                      LbToggle(
                        value: _aospCuttingEnabled,
                        onChanged: (bool value) {
                          unawaited(_setAospCuttingEnabled(value));
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
                            strings.lengthTitle,
                            style: LbTextStyles.body.copyWith(
                              color: palette.textPrimary,
                            ),
                          ),
                        ),
                        const SizedBox(width: LbSpacing.sliderValueGap),
                        Text(
                          '$_aospCuttingLength',
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
                          symbol: LbIconSymbol.compass,
                          size: 28,
                          color: palette.textPrimary,
                        ),
                        const SizedBox(width: LbSpacing.md),
                        Expanded(
                          child: LbSlider(
                            value: _aospCuttingLengthSliderValue,
                            min: 0,
                            max: sliderMax,
                            onChanged: (double value) {
                              setState(() {
                                _aospCuttingLengthSliderValue = value;
                                _aospCuttingLength = _snapLength(value);
                              });
                            },
                            onChangeEnd: (double value) {
                              final int nextValue = _snapLength(value);
                              setState(() {
                                _aospCuttingLengthSliderValue =
                                    _sliderPositionForLength(nextValue);
                                _aospCuttingLength = nextValue;
                              });
                              unawaited(_setAospCuttingLength(nextValue));
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
