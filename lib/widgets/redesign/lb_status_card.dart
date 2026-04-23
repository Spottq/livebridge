import 'package:flutter/material.dart';

import '../../theme/livebridge_tokens.dart';
import 'lb_toggle.dart';

class LbStatusCard extends StatelessWidget {
  const LbStatusCard({
    super.key,
    required this.title,
    required this.isActive,
    this.secondaryText,
    this.secondaryAccentText,
    this.secondaryAccentOnTap,
    this.onToggle,
    this.toggleOffset,
    this.toggleHapticsEnabled = true,
  });

  final String title;
  final bool isActive;
  final String? secondaryText;
  final String? secondaryAccentText;
  final VoidCallback? secondaryAccentOnTap;
  final ValueChanged<bool>? onToggle;
  final double? toggleOffset;
  final bool toggleHapticsEnabled;

  @override
  Widget build(BuildContext context) {
    final LbPalette palette = LbPalette.of(context);
    final TextStyle secondaryStyle = LbTextStyles.body.copyWith(
      color: palette.textPrimary,
    );

    return SizedBox(
      width: double.infinity,
      height: LbSpacing.statusCardMinHeight,
      child: Container(
        padding: const EdgeInsets.all(LbSpacing.statusCardPadding),
        decoration: BoxDecoration(
          color: palette.surface,
          borderRadius: BorderRadius.circular(LbRadius.card),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Expanded(
                  child: Text(
                    title,
                    style: LbTextStyles.cardTitle.copyWith(
                      color: palette.textPrimary,
                    ),
                  ),
                ),
                const SizedBox(width: LbSpacing.sm),
                Transform.translate(
                  offset: Offset(toggleOffset ?? 0, 0),
                  child: Padding(
                    padding: EdgeInsets.only(
                      top: LbSpacing.statusCardToggleTopOffset,
                    ),
                    child: LbToggle(
                      value: isActive,
                      onChanged: onToggle,
                      triggerHaptics: toggleHapticsEnabled,
                    ),
                  ),
                ),
              ],
            ),
            const Spacer(),
            if (secondaryText != null || secondaryAccentText != null)
              Wrap(
                crossAxisAlignment: WrapCrossAlignment.center,
                children: <Widget>[
                  if (secondaryText != null)
                    Text(secondaryText!, style: secondaryStyle),
                  if (secondaryAccentText != null)
                    GestureDetector(
                      onTap: secondaryAccentOnTap,
                      behavior: HitTestBehavior.opaque,
                      child: Text(
                        secondaryAccentText!,
                        style: secondaryStyle.copyWith(color: palette.link),
                      ),
                    ),
                ],
              ),
          ],
        ),
      ),
    );
  }
}
