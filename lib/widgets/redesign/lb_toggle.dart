import 'dart:async';

import 'package:flutter/material.dart';

import '../../theme/livebridge_tokens.dart';
import '../../utils/livebridge_haptics.dart';

class LbToggle extends StatefulWidget {
  const LbToggle({
    super.key,
    required this.value,
    this.onChanged,
    this.enabled = true,
    this.triggerHaptics = true,
  });

  final bool value;
  final ValueChanged<bool>? onChanged;
  final bool enabled;
  final bool triggerHaptics;

  @override
  State<LbToggle> createState() => _LbToggleState();
}

class _LbToggleState extends State<LbToggle>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;
  bool _isPressed = false;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 220),
      value: widget.value ? 1 : 0,
    );
  }

  @override
  void didUpdateWidget(covariant LbToggle oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.value == widget.value) {
      return;
    }

    _controller.animateTo(widget.value ? 1 : 0, curve: Curves.easeOutCubic);
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final LbPalette palette = LbPalette.of(context);
    final bool isInteractive = widget.enabled && widget.onChanged != null;

    return Semantics(
      button: true,
      checked: widget.value,
      enabled: isInteractive,
      child: GestureDetector(
        onTap: !isInteractive
            ? null
            : () {
                final bool nextValue = !widget.value;
                if (widget.triggerHaptics) {
                  unawaited(LiveBridgeHaptics.toggle(nextValue));
                }
                widget.onChanged!.call(nextValue);
              },
        onTapDown: !isInteractive
            ? null
            : (_) => setState(() => _isPressed = true),
        onTapCancel: !isInteractive
            ? null
            : () => setState(() => _isPressed = false),
        onTapUp: !isInteractive
            ? null
            : (_) => setState(() => _isPressed = false),
        behavior: HitTestBehavior.opaque,
        child: AnimatedScale(
          scale: _isPressed && isInteractive ? 0.97 : 1,
          duration: const Duration(milliseconds: 120),
          curve: Curves.easeOutCubic,
          child: ClipPath(
            clipper: const ShapeBorderClipper(shape: StadiumBorder()),
            child: AnimatedBuilder(
              animation: _controller,
              builder: (BuildContext context, _) {
                final double progress = _controller.value;
                final double stretchProgress = 1 - ((progress - 0.5).abs() * 2);
                final double thumbWidth =
                    LbSpacing.toggleThumbSize +
                    (LbSpacing.toggleThumbStretch * stretchProgress);
                final Color baseTrackColor = widget.enabled
                    ? Color.lerp(
                        palette.toggleTrackInactive,
                        palette.toggleTrackActive,
                        progress,
                      )!
                    : palette.toggleTrackInactive;

                return CustomPaint(
                  foregroundPainter: _LbToggleInnerShadowPainter(
                    color: progress > 0.5
                        ? palette.shadowInner
                        : palette.shadowOuter,
                    highlightColor: palette.shadowHighlight,
                  ),
                  child: Container(
                    width: LbSpacing.toggleWidth,
                    height: LbSpacing.toggleHeight,
                    padding: const EdgeInsets.all(LbSpacing.togglePadding),
                    decoration: ShapeDecoration(
                      color: Color.lerp(
                        baseTrackColor,
                        palette.toggleThumb,
                        _isPressed && isInteractive ? 0.12 : 0,
                      ),
                      shape: const StadiumBorder(),
                    ),
                    child: Align(
                      alignment: Alignment.lerp(
                        Alignment.centerLeft,
                        Alignment.centerRight,
                        progress,
                      )!,
                      child: Container(
                        width: thumbWidth,
                        height: LbSpacing.toggleThumbSize,
                        decoration: BoxDecoration(
                          color: palette.toggleThumb,
                          border: Border.all(color: palette.thumbBorder),
                          borderRadius: BorderRadius.circular(
                            LbSpacing.toggleThumbRadius,
                          ),
                          boxShadow: <BoxShadow>[
                            BoxShadow(
                              color: palette.shadowOuter,
                              offset: Offset(0, LbSpacing.shadowOffsetY),
                              blurRadius: LbSpacing.shadowBlur,
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                );
              },
            ),
          ),
        ),
      ),
    );
  }
}

class _LbToggleInnerShadowPainter extends CustomPainter {
  const _LbToggleInnerShadowPainter({
    required this.color,
    required this.highlightColor,
  });

  final Color color;
  final Color highlightColor;

  @override
  void paint(Canvas canvas, Size size) {
    final RRect outer = RRect.fromRectAndRadius(
      Offset.zero & size,
      Radius.circular(size.height / 2),
    );
    final RRect shifted = outer.shift(
      const Offset(0, -LbSpacing.shadowOffsetY),
    );
    final Path shadowBand = Path()
      ..addRRect(outer)
      ..addRRect(shifted)
      ..fillType = PathFillType.evenOdd;

    canvas.save();
    canvas.clipRRect(outer);
    canvas.drawPath(
      shadowBand,
      Paint()
        ..color = color
        ..maskFilter = const MaskFilter.blur(
          BlurStyle.normal,
          LbSpacing.shadowBlur,
        ),
    );
    canvas.drawRRect(
      outer.shift(const Offset(0, LbSpacing.shadowOffsetY)),
      Paint()
        ..color = highlightColor.withValues(alpha: 0.08)
        ..maskFilter = const MaskFilter.blur(
          BlurStyle.normal,
          LbSpacing.shadowBlur,
        ),
    );
    canvas.restore();
  }

  @override
  bool shouldRepaint(covariant _LbToggleInnerShadowPainter oldDelegate) {
    return oldDelegate.color != color ||
        oldDelegate.highlightColor != highlightColor;
  }
}
