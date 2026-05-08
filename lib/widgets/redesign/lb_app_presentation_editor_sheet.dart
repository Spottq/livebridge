import 'dart:async';

import 'package:flutter/material.dart';

import '../../l10n/app_strings.dart';
import '../../models/app_models.dart';
import '../../theme/livebridge_tokens.dart';
import '../../utils/livebridge_haptics.dart';
import '../notification_color_picker.dart';
import 'lb_icon.dart';
import 'lb_installed_app_avatar.dart';
import 'lb_list_component.dart';
import 'lb_modal_bottom_sheet.dart';
import 'lb_selection_indicator.dart';

class LbAppPresentationEditorSheet extends StatefulWidget {
  const LbAppPresentationEditorSheet({
    super.key,
    required this.title,
    this.app,
    required this.value,
    required this.onChanged,
  });

  final String title;
  final InstalledApp? app;
  final AppPresentationOverride value;
  final ValueChanged<AppPresentationOverride> onChanged;

  @override
  State<LbAppPresentationEditorSheet> createState() =>
      _LbAppPresentationEditorSheetState();
}

class _LbAppPresentationEditorSheetState
    extends State<LbAppPresentationEditorSheet> {
  late AppNotificationIconSource _iconSource = widget.value.iconSource;
  late AppPresentationTitleSource _titleSource =
      widget.value.effectiveTitleSource;
  late AppPresentationContentSource _contentSource =
      widget.value.effectiveContentSource;
  late bool _removeOriginalMessage = widget.value.removeOriginalMessage;
  late int? _notificationColorArgb = widget.value.notificationColorArgb;
  late bool _notificationColorEnabled = widget.value.notificationColorEnabled;

  AppPresentationOverride get _currentValue => widget.value.copyWith(
    iconSource: _iconSource,
    titleSource: _titleSource,
    contentSource: _contentSource,
    removeOriginalMessage: _removeOriginalMessage,
    notificationColorArgb: _notificationColorArgb,
    notificationColorEnabled: _notificationColorEnabled,
  );

  Future<void> _setIconSource(AppNotificationIconSource value) async {
    if (value == _iconSource) {
      return;
    }
    setState(() => _iconSource = value);
    unawaited(LiveBridgeHaptics.selection());
    widget.onChanged(_currentValue);
  }

  Future<void> _setTitleSource(AppPresentationTitleSource value) async {
    if (value == _titleSource) {
      return;
    }
    setState(() => _titleSource = value);
    unawaited(LiveBridgeHaptics.selection());
    widget.onChanged(_currentValue);
  }

  Future<void> _setCustomNotificationColorEnabled(bool value) async {
    if (_notificationColorEnabled == value) {
      return;
    }
    setState(() {
      _notificationColorEnabled = value;
      _notificationColorArgb ??= defaultNotificationColorArgb;
    });
    unawaited(LiveBridgeHaptics.toggle(value));
    widget.onChanged(_currentValue);
  }

  Future<void> _openNotificationColorPicker() async {
    final AppStrings strings = AppStrings.of(context);
    final int? selectedColor = await showLbModalBottomSheet<int>(
      context: context,
      builder: (BuildContext context) {
        return NotificationColorPickerSheet(
          title: strings.selectNotificationColorTitle,
          doneLabel: strings.save,
          initialColorArgb:
              _notificationColorArgb ?? defaultNotificationColorArgb,
        );
      },
    );
    if (selectedColor == null || !mounted) {
      return;
    }
    setState(() => _notificationColorArgb = selectedColor);
    unawaited(LiveBridgeHaptics.selection());
    widget.onChanged(_currentValue);
  }

  void _resetNotificationColor() {
    if (_notificationColorArgb == defaultNotificationColorArgb) {
      return;
    }
    setState(() => _notificationColorArgb = defaultNotificationColorArgb);
    unawaited(LiveBridgeHaptics.warning());
    widget.onChanged(_currentValue);
  }

  Future<void> _setContentSource(AppPresentationContentSource value) async {
    if (value == _contentSource) {
      return;
    }
    setState(() => _contentSource = value);
    unawaited(LiveBridgeHaptics.selection());
    widget.onChanged(_currentValue);
  }

  Future<void> _setRemoveOriginalMessage(bool value) async {
    if (value == _removeOriginalMessage) {
      return;
    }
    setState(() => _removeOriginalMessage = value);
    unawaited(LiveBridgeHaptics.toggle(value));
    widget.onChanged(_currentValue);
  }

  @override
  Widget build(BuildContext context) {
    final AppStrings strings = AppStrings.of(context);
    final LbPalette palette = LbPalette.of(context);
    final Color groupedBackground = palette.surfaceSoft;

    return SingleChildScrollView(
      physics: const BouncingScrollPhysics(),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            children: <Widget>[
              if (widget.app != null)
                LbInstalledAppAvatar(app: widget.app!, size: 42)
              else
                Container(
                  width: 42,
                  height: 42,
                  decoration: BoxDecoration(
                    color: palette.accent,
                    shape: BoxShape.circle,
                  ),
                  child: Center(
                    child: LbIcon(
                      symbol: LbIconSymbol.defaultsFlow,
                      size: 18,
                      color: palette.background,
                    ),
                  ),
                ),
              const SizedBox(width: LbSpacing.md),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: <Widget>[
                    Text(
                      widget.title,
                      style: LbTextStyles.title.copyWith(
                        color: palette.textPrimary,
                      ),
                    ),
                    if (widget.app != null) ...<Widget>[
                      const SizedBox(height: 2),
                      Text(
                        widget.app!.packageName,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: LbTextStyles.body.copyWith(
                          color: palette.textSecondary,
                        ),
                      ),
                    ],
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: LbSpacing.detailSectionGap),
          LbListComponent(
            items: <LbListItemData>[
              LbListItemData(
                title: strings.removeOriginalMessageTitle,
                titleSuffix: strings.experimentalSuffix,
                showChevron: false,
                toggleValue: _removeOriginalMessage,
                toggleTriggerHaptics: false,
                onToggle: (bool value) {
                  unawaited(_setRemoveOriginalMessage(value));
                },
                onTap: () {
                  unawaited(_setRemoveOriginalMessage(!_removeOriginalMessage));
                },
              ),
            ],
            backgroundColor: groupedBackground,
            rowHeight: LbSpacing.recentRowHeight,
            extendDividersToEnd: true,
          ),
          const SizedBox(height: LbSpacing.xl),
          Text(
            strings.appPresentationIconSourceLabel,
            style: LbTextStyles.title.copyWith(color: palette.textSecondary),
          ),
          const SizedBox(height: LbSpacing.sm),
          LbListComponent(
            items: <LbListItemData>[
              LbListItemData(
                title: strings.appPresentationIconApp,
                showChevron: false,
                trailingWidget: LbSelectionIndicator(
                  selected: _iconSource == AppNotificationIconSource.app,
                ),
                onTap: () {
                  unawaited(_setIconSource(AppNotificationIconSource.app));
                },
              ),
              LbListItemData(
                title: strings.appPresentationIconNotification,
                showChevron: false,
                trailingWidget: LbSelectionIndicator(
                  selected:
                      _iconSource == AppNotificationIconSource.notification,
                ),
                onTap: () {
                  unawaited(
                    _setIconSource(AppNotificationIconSource.notification),
                  );
                },
              ),
            ],
            backgroundColor: groupedBackground,
            rowHeight: LbSpacing.recentRowHeight,
            extendDividersToEnd: true,
          ),
          const SizedBox(height: LbSpacing.xl),
          Text(
            strings.notificationColorTitle,
            style: LbTextStyles.title.copyWith(color: palette.textSecondary),
          ),
          const SizedBox(height: LbSpacing.sm),
          LbListComponent(
            items: <LbListItemData>[
              LbListItemData(
                title: strings.customNotificationColorTitle,
                showChevron: false,
                toggleValue: _notificationColorEnabled,
                toggleTriggerHaptics: false,
                onToggle: (bool value) {
                  unawaited(_setCustomNotificationColorEnabled(value));
                },
                onTap: () {
                  unawaited(
                    _setCustomNotificationColorEnabled(
                      !_notificationColorEnabled,
                    ),
                  );
                },
              ),
              if (_notificationColorArgb != null)
                LbListItemData(
                  title: _formatNotificationColorHex(_notificationColorArgb!),
                  showChevron: false,
                  leadingChild: NotificationColorSwatch(
                    colorArgb: _notificationColorArgb!,
                  ),
                  trailingWidgetWidth: 44,
                  trailingWidget: IconButton(
                    tooltip: strings.resetToDefault,
                    onPressed: _resetNotificationColor,
                    icon: LbIcon(
                      symbol: LbIconSymbol.restore,
                      size: 20,
                      color: palette.textSecondary,
                    ),
                    padding: EdgeInsets.zero,
                    constraints: const BoxConstraints.tightFor(
                      width: 40,
                      height: 40,
                    ),
                  ),
                  onTap: () {
                    unawaited(_openNotificationColorPicker());
                  },
                ),
            ],
            backgroundColor: groupedBackground,
            rowHeight: LbSpacing.recentRowHeight,
            extendDividersToEnd: true,
          ),
          const SizedBox(height: LbSpacing.xl),
          Text(
            strings.titleSourceTitle,
            style: LbTextStyles.title.copyWith(color: palette.textSecondary),
          ),
          const SizedBox(height: LbSpacing.sm),
          LbListComponent(
            items: <LbListItemData>[
              LbListItemData(
                title: strings.notificationTitleOption,
                showChevron: false,
                trailingWidget: LbSelectionIndicator(
                  selected:
                      _titleSource ==
                      AppPresentationTitleSource.notificationTitle,
                ),
                onTap: () {
                  unawaited(
                    _setTitleSource(
                      AppPresentationTitleSource.notificationTitle,
                    ),
                  );
                },
              ),
              LbListItemData(
                title: strings.appTitleOption,
                showChevron: false,
                trailingWidget: LbSelectionIndicator(
                  selected: _titleSource == AppPresentationTitleSource.appTitle,
                ),
                onTap: () {
                  unawaited(
                    _setTitleSource(AppPresentationTitleSource.appTitle),
                  );
                },
              ),
            ],
            backgroundColor: groupedBackground,
            rowHeight: LbSpacing.recentRowHeight,
            extendDividersToEnd: true,
          ),
          const SizedBox(height: LbSpacing.detailSectionGap),
          Text(
            strings.contentSourceTitle,
            style: LbTextStyles.title.copyWith(color: palette.textSecondary),
          ),
          const SizedBox(height: LbSpacing.sm),
          LbListComponent(
            items: <LbListItemData>[
              LbListItemData(
                title: strings.notificationTextOption,
                showChevron: false,
                trailingWidget: LbSelectionIndicator(
                  selected:
                      _contentSource ==
                      AppPresentationContentSource.notificationText,
                ),
                onTap: () {
                  unawaited(
                    _setContentSource(
                      AppPresentationContentSource.notificationText,
                    ),
                  );
                },
              ),
              LbListItemData(
                title: strings.notificationTitleOption,
                showChevron: false,
                trailingWidget: LbSelectionIndicator(
                  selected:
                      _contentSource ==
                      AppPresentationContentSource.notificationTitle,
                ),
                onTap: () {
                  unawaited(
                    _setContentSource(
                      AppPresentationContentSource.notificationTitle,
                    ),
                  );
                },
              ),
            ],
            backgroundColor: groupedBackground,
            rowHeight: LbSpacing.recentRowHeight,
            extendDividersToEnd: true,
          ),
        ],
      ),
    );
  }
}

String _formatNotificationColorHex(int colorArgb) {
  final int rgb = colorArgb & 0x00FFFFFF;
  return '#${rgb.toRadixString(16).padLeft(6, '0').toUpperCase()}';
}
