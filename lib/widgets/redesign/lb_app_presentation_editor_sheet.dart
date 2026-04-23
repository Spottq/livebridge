import 'dart:async';

import 'package:flutter/material.dart';

import '../../l10n/app_strings.dart';
import '../../models/app_models.dart';
import '../../theme/livebridge_tokens.dart';
import '../../utils/livebridge_haptics.dart';
import 'lb_icon.dart';
import 'lb_installed_app_avatar.dart';
import 'lb_list_component.dart';
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
  late AppPresentationTitleSource _titleSource =
      widget.value.effectiveTitleSource;
  late AppPresentationContentSource _contentSource =
      widget.value.effectiveContentSource;
  late bool _removeOriginalMessage = widget.value.removeOriginalMessage;

  AppPresentationOverride get _currentValue => widget.value.copyWith(
    titleSource: _titleSource,
    contentSource: _contentSource,
    removeOriginalMessage: _removeOriginalMessage,
  );

  Future<void> _setTitleSource(AppPresentationTitleSource value) async {
    if (value == _titleSource) {
      return;
    }
    setState(() => _titleSource = value);
    unawaited(LiveBridgeHaptics.selection());
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

    return Column(
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
                  _setTitleSource(AppPresentationTitleSource.notificationTitle),
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
                unawaited(_setTitleSource(AppPresentationTitleSource.appTitle));
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
    );
  }
}
