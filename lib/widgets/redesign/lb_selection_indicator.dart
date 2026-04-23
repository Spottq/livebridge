import 'package:flutter/material.dart';

import '../../theme/livebridge_tokens.dart';

class LbSelectionIndicator extends StatelessWidget {
  const LbSelectionIndicator({
    super.key,
    required this.selected,
  });

  final bool selected;

  @override
  Widget build(BuildContext context) {
    final LbPalette palette = LbPalette.of(context);

    return AnimatedContainer(
      duration: const Duration(milliseconds: 220),
      curve: Curves.easeOutCubic,
      width: LbSpacing.selectionIndicatorSize,
      height: LbSpacing.selectionIndicatorSize,
      decoration: BoxDecoration(
        color: selected ? palette.accent : Colors.transparent,
        shape: BoxShape.circle,
        border: Border.all(
          color: selected ? palette.accent : palette.chevron,
          width: LbSpacing.selectionIndicatorStroke,
        ),
      ),
      child: Center(
        child: AnimatedScale(
          duration: const Duration(milliseconds: 220),
          curve: Curves.easeOutCubic,
          scale: selected ? 1 : 0,
          child: AnimatedOpacity(
            duration: const Duration(milliseconds: 170),
            curve: Curves.easeOut,
            opacity: selected ? 1 : 0,
            child: Container(
              width: LbSpacing.selectionIndicatorInnerSize,
              height: LbSpacing.selectionIndicatorInnerSize,
              decoration: BoxDecoration(
                color: palette.toggleThumb,
                shape: BoxShape.circle,
                boxShadow: <BoxShadow>[
                  BoxShadow(
                    color: palette.shadowOuter,
                    offset: const Offset(0, LbSpacing.shadowOffsetY),
                    blurRadius: LbSpacing.shadowBlur,
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
