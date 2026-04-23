import 'dart:math' as math;
import 'dart:ui' show lerpDouble;

import 'package:flutter/material.dart';

import '../../l10n/app_strings.dart';
import '../../theme/livebridge_tokens.dart';

class LbAppsLoadingState extends StatefulWidget {
  const LbAppsLoadingState({super.key});

  @override
  State<LbAppsLoadingState> createState() => _LbAppsLoadingStateState();
}

class _LbAppsLoadingStateState extends State<LbAppsLoadingState>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1180),
    )..repeat();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final LbPalette palette = LbPalette.of(context);

    return SizedBox(
      width: double.infinity,
      height: LbSpacing.appsLoadingHeight,
      child: Align(
        alignment: Alignment.topCenter,
        child: Padding(
          padding: const EdgeInsets.only(top: LbSpacing.lg),
          child: AnimatedBuilder(
            animation: _controller,
            builder: (BuildContext context, _) {
              final double progress = _controller.value;
              return Column(
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  SizedBox(
                    width: LbSpacing.appsLoadingSpinnerSize,
                    height: LbSpacing.appsLoadingSpinnerSize,
                    child: CustomPaint(
                      painter: _LbSpinnerPainter(
                        color: palette.textPrimary,
                        progress: progress,
                      ),
                    ),
                  ),
                  const SizedBox(height: LbSpacing.appsLoadingTextGap),
                  Text(
                    AppStrings.of(context).loadingApps,
                    style: LbTextStyles.body.copyWith(
                      color: palette.textSecondary,
                    ),
                  ),
                ],
              );
            },
          ),
        ),
      ),
    );
  }
}

class _LbSpinnerPainter extends CustomPainter {
  const _LbSpinnerPainter({required this.color, required this.progress});

  final Color color;
  final double progress;

  @override
  void paint(Canvas canvas, Size size) {
    final double strokeWidth = LbSpacing.appsLoadingStroke;
    final Rect rect = Offset.zero & size;
    final double pulse = (1 - math.cos(progress * math.pi * 2)) / 2;
    final double sweepAngle = lerpDouble(
      math.pi * 0.72,
      math.pi * 1.44,
      Curves.easeInOut.transform(pulse),
    )!;
    final double startAngle = (-math.pi / 2) + (progress * math.pi * 4);
    final Paint paint = Paint()
      ..color = color
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth
      ..strokeCap = StrokeCap.round;

    canvas.drawArc(
      rect.deflate(strokeWidth / 2),
      startAngle,
      sweepAngle,
      false,
      paint,
    );
  }

  @override
  bool shouldRepaint(covariant _LbSpinnerPainter oldDelegate) {
    return oldDelegate.color != color || oldDelegate.progress != progress;
  }
}
