import 'package:flutter/material.dart';

import '../../theme/livebridge_tokens.dart';
import 'lb_icon.dart';

class LbProfileCard extends StatelessWidget {
  const LbProfileCard({
    super.key,
    required this.title,
    required this.subtitle,
    this.icon,
    this.avatarChild,
    this.onTap,
    this.backgroundColor,
    this.titleColor,
    this.subtitleColor,
    this.chevronColor,
  }) : assert(icon != null || avatarChild != null);

  final String title;
  final String subtitle;
  final LbIconSymbol? icon;
  final Widget? avatarChild;
  final VoidCallback? onTap;
  final Color? backgroundColor;
  final Color? titleColor;
  final Color? subtitleColor;
  final Color? chevronColor;

  @override
  Widget build(BuildContext context) {
    final LbPalette palette = LbPalette.of(context);

    return Semantics(
      button: onTap != null,
      child: RepaintBoundary(
        child: GestureDetector(
          onTap: onTap,
          behavior: HitTestBehavior.opaque,
          child: Container(
            width: double.infinity,
            height: LbSpacing.profileCardHeight,
            padding: const EdgeInsets.symmetric(horizontal: LbSpacing.md),
            decoration: BoxDecoration(
              color: backgroundColor ?? palette.surface,
              borderRadius: BorderRadius.circular(LbRadius.card),
            ),
            child: Row(
              children: <Widget>[
                ClipOval(
                  child: ColoredBox(
                    color: palette.accent,
                    child: SizedBox(
                      width: LbSpacing.profileCardAvatarSize,
                      height: LbSpacing.profileCardAvatarSize,
                      child: Center(
                        child: avatarChild ??
                            LbIcon(
                              symbol: icon!,
                              size: LbSpacing.profileCardIconSize,
                              color: palette.background,
                            ),
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: LbSpacing.profileCardGap),
                Expanded(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Text(
                        title,
                        style: LbTextStyles.cardTitle.copyWith(
                          color: titleColor ?? palette.textPrimary,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        subtitle,
                        style: LbTextStyles.body.copyWith(
                          color: subtitleColor ?? palette.textSecondary,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: LbSpacing.sm),
                LbIcon(
                  symbol: LbIconSymbol.chevronRight,
                  size: LbSpacing.recentChevronSize,
                  color: chevronColor ?? palette.chevron,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
