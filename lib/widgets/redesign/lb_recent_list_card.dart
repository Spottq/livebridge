import 'package:flutter/material.dart';

import '../../theme/livebridge_tokens.dart';
import 'lb_list_component.dart';

class LbRecentListCard extends StatelessWidget {
  const LbRecentListCard({super.key, required this.items});

  final List<String> items;

  String _initialFor(String value) {
    final String trimmed = value.trim();
    if (trimmed.isEmpty) {
      return '?';
    }
    return trimmed[0].toUpperCase();
  }

  @override
  Widget build(BuildContext context) {
    return LbListComponent(
      items: items
          .map(
            (String item) => LbListItemData(
              title: item,
              leadingText: _initialFor(item),
            ),
          )
          .toList(),
      rowHeight: LbSpacing.recentRowHeight,
      leadingSize: LbSpacing.recentAvatarSize,
      leadingIconSize: LbTextStyles.caption.fontSize ?? 13,
      leadingGap: LbSpacing.recentAvatarGap,
    );
  }
}
