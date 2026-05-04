import 'dart:math' as math;
import 'dart:ui';

import 'package:flutter/material.dart';

import '../../theme/livebridge_tokens.dart';

class LbDescriptionPopover extends StatelessWidget {
  const LbDescriptionPopover({
    super.key,
    required this.text,
    required this.anchor,
  });

  final String text;
  final Offset anchor;

  @override
  Widget build(BuildContext context) {
    final LbPalette palette = LbPalette.of(context);
    final Size viewport = MediaQuery.sizeOf(context);
    final EdgeInsets padding = MediaQuery.paddingOf(context);
    final double maxWidth =
        viewport.width - LbSpacing.descriptionPopoverHorizontalInset * 2;
    final double maxPopoverWidth = math.min(
      LbSpacing.descriptionPopoverMaxWidth,
      maxWidth,
    );
    final bool isLight = Theme.of(context).brightness == Brightness.light;
    final Color textColor = isLight
        ? palette.textPrimary.withValues(alpha: 0.72)
        : palette.textSecondary;
    final TextStyle textStyle = LbTextStyles.body.copyWith(
      color: textColor,
      height: 1.22,
    );
    final TextPainter textPainter =
        TextPainter(
          text: TextSpan(text: text, style: textStyle),
          textDirection: Directionality.of(context),
          textScaler: MediaQuery.textScalerOf(context),
        )..layout(
          maxWidth:
              maxPopoverWidth -
              LbSpacing.descriptionPopoverPaddingHorizontal * 2,
        );
    final double minPopoverWidth =
        LbSpacing.descriptionPopoverArrowWidth +
        LbSpacing.descriptionPopoverPaddingHorizontal * 2;
    final double width =
        (textPainter.width + LbSpacing.descriptionPopoverPaddingHorizontal * 2)
            .clamp(minPopoverWidth, maxPopoverWidth)
            .toDouble();
    final double left = (anchor.dx - width / 2)
        .clamp(
          LbSpacing.descriptionPopoverHorizontalInset,
          viewport.width - width - LbSpacing.descriptionPopoverHorizontalInset,
        )
        .toDouble();
    final double top = (anchor.dy + LbSpacing.descriptionPopoverVerticalGap)
        .clamp(
          padding.top + LbSpacing.sm,
          viewport.height - padding.bottom - LbSpacing.xl,
        )
        .toDouble();
    final double arrowLeft = (anchor.dx - left)
        .clamp(
          LbSpacing.descriptionPopoverArrowWidth / 2 + LbSpacing.sm,
          width - LbSpacing.descriptionPopoverArrowWidth / 2 - LbSpacing.sm,
        )
        .toDouble();
    final Color background = palette.background.withValues(
      alpha: LbEffects.descriptionPopoverBackdropAlpha,
    );
    final BorderRadius borderRadius = BorderRadius.circular(LbRadius.card);

    return Material(
      type: MaterialType.transparency,
      child: Stack(
        children: <Widget>[
          Positioned(
            left: left,
            top: top,
            width: width,
            child: Stack(
              clipBehavior: Clip.none,
              children: <Widget>[
                Positioned(
                  left: arrowLeft - LbSpacing.descriptionPopoverArrowWidth / 2,
                  top: 0,
                  child: _LbDescriptionArrowSurface(
                    background: background,
                    shadow: palette.shadowOuter.withValues(
                      alpha: LbEffects.descriptionPopoverShadowAlpha,
                    ),
                    ambientShadow: palette.shadowOuter.withValues(
                      alpha: LbEffects.descriptionPopoverAmbientShadowAlpha,
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.only(
                    top: LbSpacing.descriptionPopoverArrowHeight - 1,
                  ),
                  child: Container(
                    width: width,
                    decoration: BoxDecoration(
                      borderRadius: borderRadius,
                      boxShadow: <BoxShadow>[
                        BoxShadow(
                          color: palette.shadowOuter.withValues(
                            alpha: LbEffects.descriptionPopoverShadowAlpha,
                          ),
                          blurRadius: LbEffects.descriptionPopoverShadowBlur,
                          offset: const Offset(
                            0,
                            LbEffects.descriptionPopoverShadowOffsetY,
                          ),
                        ),
                        BoxShadow(
                          color: palette.shadowOuter.withValues(
                            alpha:
                                LbEffects.descriptionPopoverAmbientShadowAlpha,
                          ),
                          blurRadius:
                              LbEffects.descriptionPopoverAmbientShadowBlur,
                          offset: Offset.zero,
                        ),
                      ],
                    ),
                    child: ClipRRect(
                      borderRadius: borderRadius,
                      child: Stack(
                        children: <Widget>[
                          BackdropFilter(
                            filter: ImageFilter.blur(
                              sigmaX: LbEffects.descriptionPopoverBackdropBlur,
                              sigmaY: LbEffects.descriptionPopoverBackdropBlur,
                            ),
                            child: ColoredBox(
                              color: background,
                              child: Padding(
                                padding: const EdgeInsets.symmetric(
                                  horizontal: LbSpacing
                                      .descriptionPopoverPaddingHorizontal,
                                  vertical: LbSpacing
                                      .descriptionPopoverPaddingVertical,
                                ),
                                child: Visibility(
                                  visible: false,
                                  maintainAnimation: true,
                                  maintainSize: true,
                                  maintainState: true,
                                  child: Text(text, style: textStyle),
                                ),
                              ),
                            ),
                          ),
                          Positioned.fill(
                            child: Padding(
                              padding: const EdgeInsets.symmetric(
                                horizontal: LbSpacing
                                    .descriptionPopoverPaddingHorizontal,
                                vertical:
                                    LbSpacing.descriptionPopoverPaddingVertical,
                              ),
                              child: TweenAnimationBuilder<double>(
                                tween: Tween<double>(begin: 0, end: 1),
                                duration: const Duration(milliseconds: 140),
                                curve: Curves.easeOutCubic,
                                builder:
                                    (
                                      BuildContext context,
                                      double value,
                                      Widget? child,
                                    ) {
                                      return Opacity(
                                        opacity: value,
                                        child: Transform.translate(
                                          offset: Offset(0, (1 - value) * 3),
                                          child: child,
                                        ),
                                      );
                                    },
                                child: Text(text, style: textStyle),
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _LbDescriptionArrowSurface extends StatelessWidget {
  const _LbDescriptionArrowSurface({
    required this.background,
    required this.shadow,
    required this.ambientShadow,
  });

  final Color background;
  final Color shadow;
  final Color ambientShadow;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: LbSpacing.descriptionPopoverArrowWidth,
      height: LbSpacing.descriptionPopoverArrowHeight,
      child: Stack(
        fit: StackFit.expand,
        clipBehavior: Clip.none,
        children: <Widget>[
          CustomPaint(
            painter: _LbDescriptionArrowShadowPainter(
              shadow: shadow,
              ambientShadow: ambientShadow,
            ),
          ),
          ClipPath(
            clipper: const _LbDescriptionArrowClipper(),
            child: BackdropFilter(
              filter: ImageFilter.blur(
                sigmaX: LbEffects.descriptionPopoverBackdropBlur,
                sigmaY: LbEffects.descriptionPopoverBackdropBlur,
              ),
              child: ColoredBox(color: background),
            ),
          ),
        ],
      ),
    );
  }
}

class _LbDescriptionArrowClipper extends CustomClipper<Path> {
  const _LbDescriptionArrowClipper();

  @override
  Path getClip(Size size) {
    return _descriptionArrowPath(size);
  }

  @override
  bool shouldReclip(covariant _LbDescriptionArrowClipper oldClipper) {
    return false;
  }
}

class _LbDescriptionArrowShadowPainter extends CustomPainter {
  const _LbDescriptionArrowShadowPainter({
    required this.shadow,
    required this.ambientShadow,
  });

  final Color shadow;
  final Color ambientShadow;

  @override
  void paint(Canvas canvas, Size size) {
    final Path path = _descriptionArrowPath(size);
    canvas.drawPath(
      path.shift(
        const Offset(0, LbEffects.descriptionPopoverShadowOffsetY * 0.35),
      ),
      Paint()
        ..color = shadow
        ..maskFilter = const MaskFilter.blur(
          BlurStyle.normal,
          LbEffects.descriptionPopoverShadowBlur,
        ),
    );
    canvas.drawPath(
      path,
      Paint()
        ..color = ambientShadow
        ..maskFilter = const MaskFilter.blur(
          BlurStyle.normal,
          LbEffects.descriptionPopoverAmbientShadowBlur,
        ),
    );
  }

  @override
  bool shouldRepaint(covariant _LbDescriptionArrowShadowPainter oldDelegate) {
    return oldDelegate.shadow != shadow ||
        oldDelegate.ambientShadow != ambientShadow;
  }
}

Path _descriptionArrowPath(Size size) {
  return Path()
    ..moveTo(size.width / 2, 0)
    ..cubicTo(
      size.width * 0.62,
      size.height * 0.12,
      size.width * 0.62,
      size.height,
      size.width,
      size.height,
    )
    ..lineTo(0, size.height)
    ..cubicTo(
      size.width * 0.38,
      size.height,
      size.width * 0.38,
      size.height * 0.12,
      size.width / 2,
      0,
    )
    ..close();
}
