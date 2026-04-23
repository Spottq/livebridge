import 'package:flutter/material.dart';

import '../../theme/livebridge_tokens.dart';

class LbRecentEmptyState extends StatelessWidget {
  const LbRecentEmptyState({
    super.key,
    required this.title,
    required this.actionLabel,
    this.onAction,
  });

  final String title;
  final String actionLabel;
  final VoidCallback? onAction;

  @override
  Widget build(BuildContext context) {
    final LbPalette palette = LbPalette.of(context);

    return SizedBox(
      width: double.infinity,
      height: LbSpacing.recentEmptyStateHeight,
      child: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: <Widget>[
            Text(
              title,
              style: LbTextStyles.title.copyWith(color: palette.textSecondary),
            ),
            const SizedBox(height: LbSpacing.xs),
            GestureDetector(
              onTap: onAction,
              behavior: HitTestBehavior.opaque,
              child: Text(
                actionLabel,
                style: LbTextStyles.title.copyWith(color: palette.link),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
