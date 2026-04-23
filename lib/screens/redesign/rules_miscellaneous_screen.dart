import 'dart:async';

import 'package:flutter/material.dart';

import '../../l10n/app_strings.dart';
import '../../models/app_models.dart';
import '../../platform/livebridge_platform.dart';
import '../../theme/livebridge_tokens.dart';
import '../../utils/livebridge_haptics.dart';
import '../../widgets/redesign/lb_detail_screen.dart';
import '../../widgets/redesign/lb_icon.dart';
import '../../widgets/redesign/lb_list_component.dart';
import '../../widgets/redesign/lb_slider.dart';

class RulesMiscellaneousScreen extends StatefulWidget {
  const RulesMiscellaneousScreen({super.key});

  @override
  State<RulesMiscellaneousScreen> createState() =>
      _RulesMiscellaneousScreenState();
}

class _RulesMiscellaneousScreenState extends State<RulesMiscellaneousScreen> {
  bool _navigationEnabled = true;
  bool _mediaPlaybackEnabled = true;
  bool _weatherEnabled = false;
  bool _weatherLockscreenOnly = false;
  bool _smartFlashlightEnabled = false;
  int _smartFlashlightLevel = 4;
  FlashlightCapability _flashlightCapability = const FlashlightCapability();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      unawaited(_loadState());
    });
  }

  Future<void> _loadState() async {
    try {
      final Future<bool> navigationFuture =
          LiveBridgePlatform.getSmartNavigationEnabled();
      final Future<bool> mediaPlaybackFuture =
          LiveBridgePlatform.getSmartMediaPlaybackEnabled();
      final Future<bool> weatherFuture =
          LiveBridgePlatform.getSmartWeatherEnabled();
      final Future<bool> weatherLockscreenOnlyFuture =
          LiveBridgePlatform.getSmartWeatherLockscreenOnly();
      final Future<bool> flashlightEnabledFuture =
          LiveBridgePlatform.getSmartFlashlightEnabled();
      final Future<int> flashlightLevelFuture =
          LiveBridgePlatform.getSmartFlashlightLevel();
      final Future<FlashlightCapability> flashlightCapabilityFuture =
          LiveBridgePlatform.getFlashlightCapability();

      final bool navigationEnabled = await navigationFuture;
      final bool mediaPlaybackEnabled = await mediaPlaybackFuture;
      final bool weatherEnabled = await weatherFuture;
      final bool weatherLockscreenOnly = await weatherLockscreenOnlyFuture;
      final bool flashlightEnabled = await flashlightEnabledFuture;
      final int flashlightLevel = await flashlightLevelFuture;
      final FlashlightCapability flashlightCapability =
          await flashlightCapabilityFuture;

      if (!mounted) {
        return;
      }

      setState(() {
        _navigationEnabled = navigationEnabled;
        _mediaPlaybackEnabled = mediaPlaybackEnabled;
        _weatherEnabled = weatherEnabled;
        _weatherLockscreenOnly = weatherLockscreenOnly;
        _smartFlashlightEnabled = flashlightEnabled;
        _smartFlashlightLevel = flashlightLevel.clamp(0, 4);
        _flashlightCapability = flashlightCapability;
      });
    } catch (_) {}
  }

  Future<void> _setNavigationEnabled(bool value) async {
    if (value == _navigationEnabled) {
      return;
    }
    setState(() => _navigationEnabled = value);
    await LiveBridgePlatform.setSmartNavigationEnabled(value);
  }

  Future<void> _setMediaPlaybackEnabled(bool value) async {
    if (value == _mediaPlaybackEnabled) {
      return;
    }
    setState(() => _mediaPlaybackEnabled = value);
    await LiveBridgePlatform.setSmartMediaPlaybackEnabled(value);
  }

  Future<void> _setWeatherEnabled(bool value) async {
    if (value == _weatherEnabled) {
      return;
    }
    setState(() => _weatherEnabled = value);
    await LiveBridgePlatform.setSmartWeatherEnabled(value);
  }

  Future<void> _setWeatherLockscreenOnly(bool value) async {
    if (value == _weatherLockscreenOnly) {
      return;
    }
    setState(() => _weatherLockscreenOnly = value);
    await LiveBridgePlatform.setSmartWeatherLockscreenOnly(value);
  }

  Future<void> _setSmartFlashlightEnabled(bool value) async {
    if (!_flashlightCapability.available && value) {
      return;
    }
    setState(() => _smartFlashlightEnabled = value);
    await LiveBridgePlatform.setSmartFlashlightEnabled(value);

    final bool actualEnabled =
        await LiveBridgePlatform.getSmartFlashlightEnabled();
    final FlashlightCapability capability =
        await LiveBridgePlatform.getFlashlightCapability();
    if (!mounted) {
      return;
    }
    setState(() {
      _smartFlashlightEnabled = actualEnabled;
      _flashlightCapability = capability;
    });
  }

  Future<void> _setSmartFlashlightLevel(int value) async {
    final int normalized = value.clamp(0, 4);
    if (_smartFlashlightLevel == normalized) {
      return;
    }
    setState(() => _smartFlashlightLevel = normalized);
    await LiveBridgePlatform.setSmartFlashlightLevel(normalized);
  }

  String _flashlightSubtitle(AppStrings strings) {
    if (!_flashlightCapability.available) {
      return strings.smartFlashlightUnavailableSubtitle;
    }
    if (!_flashlightCapability.supportsInteractiveLevels) {
      return strings.smartFlashlightUnsupportedSubtitle;
    }
    return strings.smartFlashlightSubtitle;
  }

  @override
  Widget build(BuildContext context) {
    final AppStrings strings = AppStrings.of(context);
    final LbPalette palette = LbPalette.of(context);
    final List<LbListItemData> items = <LbListItemData>[
      LbListItemData(
        title: strings.navigationMapsTitle,
        subtitle: strings.smartNavigationSubtitle,
        showChevron: false,
        toggleValue: _navigationEnabled,
        onToggle: (bool value) {
          unawaited(_setNavigationEnabled(value));
        },
        onTap: () {
          final bool nextValue = !_navigationEnabled;
          unawaited(LiveBridgeHaptics.toggle(nextValue));
          unawaited(_setNavigationEnabled(nextValue));
        },
      ),
      LbListItemData(
        title: strings.mediaPlaybackRedesignTitle,
        subtitle: strings.smartMediaPlaybackSubtitle,
        showChevron: false,
        toggleValue: _mediaPlaybackEnabled,
        onToggle: (bool value) {
          unawaited(_setMediaPlaybackEnabled(value));
        },
        onTap: () {
          final bool nextValue = !_mediaPlaybackEnabled;
          unawaited(LiveBridgeHaptics.toggle(nextValue));
          unawaited(_setMediaPlaybackEnabled(nextValue));
        },
      ),
      LbListItemData(
        title: strings.weatherBroadcastsTitle,
        subtitle: strings.smartWeatherSubtitle,
        showChevron: false,
        toggleValue: _weatherEnabled,
        onToggle: (bool value) {
          unawaited(_setWeatherEnabled(value));
        },
        onTap: () {
          final bool nextValue = !_weatherEnabled;
          unawaited(LiveBridgeHaptics.toggle(nextValue));
          unawaited(_setWeatherEnabled(nextValue));
        },
      ),
      LbListItemData(
        title: strings.smartWeatherLockscreenOnlyTitle,
        subtitle: strings.smartWeatherLockscreenOnlySubtitle,
        showChevron: false,
        enabled: _weatherEnabled,
        toggleValue: _weatherLockscreenOnly,
        onToggle: _weatherEnabled
            ? (bool value) {
                unawaited(_setWeatherLockscreenOnly(value));
              }
            : null,
        onTap: _weatherEnabled
            ? () {
                final bool nextValue = !_weatherLockscreenOnly;
                unawaited(LiveBridgeHaptics.toggle(nextValue));
                unawaited(_setWeatherLockscreenOnly(nextValue));
              }
            : null,
      ),
    ];

    return LbDetailScreen(
      title: strings.miscellaneousTitle,
      children: <Widget>[
        LbListComponent(items: items, extendDividersToEnd: true),
        const SizedBox(height: LbSpacing.detailSectionGap),
        LbListComponent(
          items: <LbListItemData>[
            LbListItemData(
              title: strings.smartFlashlightTitle,
              subtitle: _flashlightSubtitle(strings),
              showChevron: false,
              enabled: _flashlightCapability.available,
              toggleValue: _smartFlashlightEnabled,
              onToggle: _flashlightCapability.available
                  ? (bool value) {
                      unawaited(_setSmartFlashlightEnabled(value));
                    }
                  : null,
              onTap: _flashlightCapability.available
                  ? () {
                      final bool nextValue = !_smartFlashlightEnabled;
                      unawaited(LiveBridgeHaptics.toggle(nextValue));
                      unawaited(_setSmartFlashlightEnabled(nextValue));
                    }
                  : null,
            ),
          ],
          extendDividersToEnd: true,
        ),
        if (_flashlightCapability.available) ...<Widget>[
          const SizedBox(height: LbSpacing.detailSectionGap),
          Container(
            width: double.infinity,
            decoration: BoxDecoration(
              color: palette.surface,
              borderRadius: BorderRadius.circular(LbRadius.card),
            ),
            child: Padding(
              padding: const EdgeInsets.fromLTRB(
                LbSpacing.md + LbSpacing.listTextOnlyInset,
                LbSpacing.md,
                LbSpacing.md,
                LbSpacing.md,
              ),
              child: AnimatedOpacity(
                duration: const Duration(milliseconds: 180),
                opacity: _smartFlashlightEnabled ? 1 : 0.45,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Row(
                      children: <Widget>[
                        Expanded(
                          child: Text(
                            strings.smartFlashlightBrightnessLabel(
                              _smartFlashlightLevel + 1,
                            ),
                            style: LbTextStyles.body.copyWith(
                              color: palette.textPrimary,
                            ),
                          ),
                        ),
                        if (_flashlightCapability.supportsInteractiveLevels)
                          Text(
                            '${_smartFlashlightLevel + 1}/5',
                            style: LbTextStyles.body.copyWith(
                              color: palette.textSecondary,
                            ),
                          ),
                      ],
                    ),
                    const SizedBox(height: LbSpacing.sliderSectionGap),
                    if (_flashlightCapability
                        .supportsInteractiveLevels) ...<Widget>[
                      Row(
                        children: <Widget>[
                          LbIcon(
                            symbol: LbIconSymbol.magic,
                            size: 28,
                            color: palette.textPrimary,
                          ),
                          const SizedBox(width: LbSpacing.md),
                          Expanded(
                            child: LbSlider(
                              value: _smartFlashlightLevel.toDouble(),
                              min: 0,
                              max: 4,
                              enabled: _smartFlashlightEnabled,
                              onChanged: (double value) {
                                setState(
                                  () => _smartFlashlightLevel = value.round(),
                                );
                              },
                              onChangeEnd: (double value) {
                                unawaited(
                                  _setSmartFlashlightLevel(value.round()),
                                );
                              },
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: LbSpacing.sm),
                      Text(
                        strings.smartFlashlightLevelSelectorHint,
                        style: LbTextStyles.caption.copyWith(
                          color: palette.textSecondary,
                        ),
                      ),
                    ] else
                      Text(
                        _smartFlashlightEnabled
                            ? strings.smartFlashlightFallbackWarning
                            : strings.smartFlashlightUnsupportedSubtitle,
                        style: LbTextStyles.caption.copyWith(
                          color: palette.textSecondary,
                        ),
                      ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ],
    );
  }
}
