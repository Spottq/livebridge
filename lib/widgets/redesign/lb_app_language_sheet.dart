import 'dart:async';

import 'package:flutter/material.dart';

import '../../l10n/app_locale_controller.dart';
import '../../theme/livebridge_tokens.dart';
import '../../utils/livebridge_haptics.dart';
import 'lb_list_component.dart';
import 'lb_selection_indicator.dart';

class LbAppLanguageSheet extends StatelessWidget {
  const LbAppLanguageSheet({
    super.key,
    required this.title,
    required this.systemLabel,
    required this.selectedId,
    required this.onChanged,
  });

  final String title;
  final String systemLabel;
  final String selectedId;
  final ValueChanged<String> onChanged;

  String _labelFor(AppLanguageOption option) {
    return option.id == appLanguageSystemId ? systemLabel : option.label;
  }

  @override
  Widget build(BuildContext context) {
    final LbPalette palette = LbPalette.of(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: <Widget>[
        Text(
          title,
          style: LbTextStyles.title.copyWith(color: palette.textPrimary),
        ),
        const SizedBox(height: LbSpacing.md),
        LbListComponent(
          items: appLanguageOptions.map((AppLanguageOption option) {
            final bool selected = option.id == selectedId;
            return LbListItemData(
              title: _labelFor(option),
              showChevron: false,
              trailingWidget: LbSelectionIndicator(selected: selected),
              trailingWidgetWidth: LbSpacing.selectionIndicatorSize,
              onTap: () {
                unawaited(LiveBridgeHaptics.selection());
                onChanged(option.id);
                Navigator.of(context).pop();
              },
            );
          }).toList(growable: false),
          rowHeight: LbSpacing.recentRowHeight,
          extendDividersToEnd: true,
        ),
      ],
    );
  }
}
