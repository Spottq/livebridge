import 'package:flutter/material.dart';

import '../../theme/livebridge_tokens.dart';
import 'lb_icon.dart';

class LbDefaultsCard extends StatelessWidget {
  const LbDefaultsCard({
    super.key,
    required this.title,
    required this.subtitle,
    required this.onTap,
    this.icon = LbIconSymbol.defaultsFlow,
    this.iconColor,
    this.enabled = true,
  });

  final String title;
  final String subtitle;
  final VoidCallback onTap;
  final LbIconSymbol icon;
  final Color? iconColor;
  final bool enabled;

  @override
  Widget build(BuildContext context) {
    final LbPalette palette = LbPalette.of(context);

    return Semantics(
      button: true,
      child: GestureDetector(
        onTap: enabled ? onTap : null,
        behavior: HitTestBehavior.opaque,
        child: Container(
          width: double.infinity,
          padding: const EdgeInsets.symmetric(
            horizontal: LbSpacing.md,
            vertical: LbSpacing.lg,
          ),
          decoration: BoxDecoration(
            color: palette.surface,
            borderRadius: BorderRadius.circular(LbRadius.card),
          ),
          child: Row(
            children: <Widget>[
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Text(
                      title,
                      style: LbTextStyles.body.copyWith(
                        color: enabled
                            ? palette.textPrimary
                            : palette.textSecondary,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      subtitle,
                      style: LbTextStyles.body.copyWith(
                        color: enabled
                            ? palette.textSecondary
                            : palette.textMuted,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: LbSpacing.md),
              LbIcon(
                symbol: icon,
                size: 28,
                color: enabled
                    ? iconColor ?? palette.chevron
                    : palette.textMuted,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
