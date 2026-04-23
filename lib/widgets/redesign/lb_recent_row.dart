import 'package:flutter/material.dart';

import '../../theme/livebridge_tokens.dart';
import 'lb_icon.dart';

class LbRecentRow extends StatelessWidget {
  const LbRecentRow({super.key, required this.name, this.showDivider = true});

  final String name;
  final bool showDivider;

  String get _initial {
    final String trimmed = name.trim();
    if (trimmed.isEmpty) {
      return '?';
    }
    return trimmed[0].toUpperCase();
  }

  @override
  Widget build(BuildContext context) {
    final LbPalette palette = LbPalette.of(context);

    return SizedBox(
      height: LbSpacing.recentRowHeight,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: LbSpacing.md),
        child: Row(
          children: <Widget>[
            Container(
              width: LbSpacing.recentAvatarSize,
              height: LbSpacing.recentAvatarSize,
              decoration: BoxDecoration(
                color: palette.surfaceSoft,
                shape: BoxShape.circle,
              ),
              child: Center(
                child: Text(
                  _initial,
                  style: LbTextStyles.caption.copyWith(color: palette.accent),
                ),
              ),
            ),
            const SizedBox(width: LbSpacing.recentAvatarGap),
            Expanded(
              child: SizedBox(
                height: double.infinity,
                child: Container(
                  decoration: BoxDecoration(
                    border: showDivider
                        ? Border(
                            bottom: BorderSide(
                              color: palette.recentSeparator,
                              width: LbSpacing.recentSeparatorThickness,
                            ),
                          )
                        : null,
                  ),
                  child: Row(
                    children: <Widget>[
                      Expanded(
                        child: Text(
                          name,
                          style: LbTextStyles.body.copyWith(
                            color: palette.textPrimary,
                          ),
                        ),
                      ),
                      const SizedBox(width: LbSpacing.sm),
                      LbIcon(
                        symbol: LbIconSymbol.chevronRight,
                        size: LbSpacing.recentChevronSize,
                        color: palette.chevron,
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
