import 'dart:ui';
import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../../theme/livebridge_tokens.dart';
import 'lb_bottom_nav_item.dart';

class LbBottomFloatingNav extends StatefulWidget {
  const LbBottomFloatingNav({
    super.key,
    required this.items,
    required this.currentIndex,
    this.selectionProgress,
    this.motionTargetIndex,
    this.motionProgress = 0,
    this.onItemSelected,
    this.onSelectionDragStart,
    this.onSelectionDragUpdate,
    this.onSelectionDragEnd,
    this.blur = true,
  }) : assert(items.length == 3, 'LbBottomFloatingNav expects exactly 3 items');

  final List<LbBottomNavData> items;
  final int currentIndex;
  final double? selectionProgress;
  final int? motionTargetIndex;
  final double motionProgress;
  final ValueChanged<int>? onItemSelected;
  final VoidCallback? onSelectionDragStart;
  final ValueChanged<double>? onSelectionDragUpdate;
  final ValueChanged<double>? onSelectionDragEnd;
  final bool blur;

  @override
  State<LbBottomFloatingNav> createState() => _LbBottomFloatingNavState();
}

class _LbBottomFloatingNavState extends State<LbBottomFloatingNav>
    with SingleTickerProviderStateMixin {
  bool _isDraggingSelector = false;
  double _dragStartDx = 0;
  double _dragStartProgress = 0;
  double _dragCurrentProgress = 0;
  late final AnimationController _highlightReleaseController;

  @override
  void initState() {
    super.initState();
    _highlightReleaseController = AnimationController(
      vsync: this,
      duration: LbMotion.bottomNavHighlightReleaseDuration,
    );
  }

  @override
  void dispose() {
    _highlightReleaseController.dispose();
    super.dispose();
  }

  double _pageSwipeScale(double progress) {
    final double offsetFromTab = (progress - progress.roundToDouble()).abs();
    if (offsetFromTab < 0.001) {
      return 1;
    }

    final double normalized = (offsetFromTab / 0.5).clamp(0.0, 1.0);
    return lerpDouble(
      1,
      LbMotion.bottomNavHighlightSwipeScale,
      Curves.easeOut.transform(normalized),
    )!;
  }

  double _tapTransitionScale() {
    if (widget.motionTargetIndex == null) {
      return 1;
    }

    return TweenSequence<double>(<TweenSequenceItem<double>>[
      TweenSequenceItem<double>(
        tween: Tween<double>(
          begin: 1,
          end: LbMotion.bottomNavHighlightTapScale,
        ).chain(CurveTween(curve: Curves.easeOutCubic)),
        weight: 26,
      ),
      TweenSequenceItem<double>(
        tween: Tween<double>(
          begin: LbMotion.bottomNavHighlightTapScale,
          end: LbMotion.bottomNavHighlightReleaseOvershoot,
        ).chain(CurveTween(curve: Curves.easeOutBack)),
        weight: 28,
      ),
      TweenSequenceItem<double>(
        tween: Tween<double>(
          begin: LbMotion.bottomNavHighlightReleaseOvershoot,
          end: 1,
        ).chain(CurveTween(curve: Curves.easeOutCubic)),
        weight: 46,
      ),
    ]).transform(widget.motionProgress.clamp(0.0, 1.0));
  }

  double _dragReleaseScale() {
    return TweenSequence<double>(<TweenSequenceItem<double>>[
      TweenSequenceItem<double>(
        tween: Tween<double>(
          begin: LbMotion.bottomNavHighlightDragScale,
          end: LbMotion.bottomNavHighlightReleaseOvershoot,
        ).chain(CurveTween(curve: Curves.easeOutBack)),
        weight: 58,
      ),
      TweenSequenceItem<double>(
        tween: Tween<double>(
          begin: LbMotion.bottomNavHighlightReleaseOvershoot,
          end: 1,
        ).chain(CurveTween(curve: Curves.easeOutCubic)),
        weight: 42,
      ),
    ]).transform(_highlightReleaseController.value);
  }

  double _highlightScale(double effectiveProgress) {
    if (_isDraggingSelector) {
      return LbMotion.bottomNavHighlightDragScale;
    }

    if (_highlightReleaseController.isAnimating) {
      return _dragReleaseScale();
    }

    final double pageScale = _pageSwipeScale(effectiveProgress);
    final double tapScale = _tapTransitionScale();

    if (pageScale < 0.999) {
      return tapScale < 1 ? math.min(pageScale, tapScale) : pageScale;
    }

    return tapScale;
  }

  void _playHighlightRelease() {
    _highlightReleaseController.forward(from: 0);
  }

  @override
  Widget build(BuildContext context) {
    const StadiumBorder shape = StadiumBorder();
    final double effectiveProgress =
        widget.selectionProgress ?? widget.currentIndex.toDouble();
    final LbPalette palette = LbPalette.of(context);

    final Widget navShell = Container(
      height: LbSpacing.bottomNavHeight,
      padding: const EdgeInsets.all(LbSpacing.bottomNavInnerPadding),
      decoration: ShapeDecoration(
        color: palette.navBlurTint,
        shape: StadiumBorder(side: BorderSide(color: palette.navBorder)),
        shadows: <BoxShadow>[
          BoxShadow(
            color: palette.shadowOuter,
            offset: Offset(0, LbSpacing.bottomNavShadowOffsetY),
            blurRadius: LbSpacing.bottomNavShadowBlur,
          ),
        ],
      ),
      child: LayoutBuilder(
        builder: (BuildContext context, BoxConstraints constraints) {
          final double itemWidth = constraints.maxWidth / widget.items.length;
          final double capsuleWidth =
              itemWidth - (LbSpacing.bottomNavHighlightInset * 2);
          final double capsuleLeft =
              (effectiveProgress * itemWidth) +
              LbSpacing.bottomNavHighlightInset;
          final Rect activeRect = Rect.fromLTWH(
            capsuleLeft,
            LbSpacing.bottomNavHighlightInset,
            capsuleWidth,
            constraints.maxHeight - (LbSpacing.bottomNavHighlightInset * 2),
          );

          return AnimatedBuilder(
            animation: _highlightReleaseController,
            builder: (BuildContext context, _) {
              final double highlightScale = _highlightScale(effectiveProgress);

              return GestureDetector(
                behavior: HitTestBehavior.translucent,
                onHorizontalDragStart: widget.onSelectionDragUpdate == null
                    ? null
                    : (DragStartDetails details) {
                        if (
                            !activeRect.inflate(6).contains(details.localPosition)) {
                          _isDraggingSelector = false;
                          return;
                        }
                        _highlightReleaseController.stop();
                        setState(() {
                          _isDraggingSelector = true;
                          _dragStartDx = details.localPosition.dx;
                          _dragStartProgress = effectiveProgress;
                          _dragCurrentProgress = effectiveProgress;
                        });
                        widget.onSelectionDragStart?.call();
                      },
                onHorizontalDragUpdate: widget.onSelectionDragUpdate == null
                    ? null
                    : (DragUpdateDetails details) {
                        if (!_isDraggingSelector) {
                          return;
                        }
                        final double nextProgress =
                            (_dragStartProgress +
                                    ((details.localPosition.dx - _dragStartDx) /
                                        itemWidth))
                                .clamp(
                                  0.0,
                                  (widget.items.length - 1).toDouble(),
                                );
                        _dragCurrentProgress = nextProgress;
                        widget.onSelectionDragUpdate?.call(nextProgress);
                      },
                onHorizontalDragEnd: widget.onSelectionDragEnd == null
                    ? null
                    : (DragEndDetails details) {
                        if (!_isDraggingSelector) {
                          return;
                        }
                        setState(() => _isDraggingSelector = false);
                        _playHighlightRelease();
                        widget.onSelectionDragEnd?.call(_dragCurrentProgress);
                      },
                onHorizontalDragCancel: () {
                  if (!_isDraggingSelector) {
                    return;
                  }
                  setState(() => _isDraggingSelector = false);
                  _playHighlightRelease();
                  widget.onSelectionDragEnd?.call(_dragCurrentProgress);
                },
                child: Stack(
                  children: <Widget>[
                    Positioned(
                      left: capsuleLeft,
                      top: LbSpacing.bottomNavHighlightInset,
                      bottom: LbSpacing.bottomNavHighlightInset,
                      child: IgnorePointer(
                        child: Transform.scale(
                          scale: highlightScale,
                          child: Container(
                            width: capsuleWidth,
                            decoration: ShapeDecoration(
                              color: palette.navActiveFill,
                              shape: const StadiumBorder(),
                            ),
                          ),
                        ),
                      ),
                    ),
                    Row(
                      children: <Widget>[
                        for (
                          int index = 0;
                          index < widget.items.length;
                          index += 1
                        )
                          Expanded(
                            child: Builder(
                              builder: (BuildContext context) {
                                final double activeProgress =
                                    (1 - (effectiveProgress - index).abs())
                                        .clamp(0.0, 1.0);

                                return LbBottomNavItem(
                                  label: widget.items[index].label,
                                  icon: widget.items[index].icon,
                                  motion: widget.items[index].motion,
                                  activeIcon: widget.items[index].activeIcon,
                                  activeProgress: activeProgress,
                                  motionProgress:
                                      widget.motionTargetIndex == index
                                      ? widget.motionProgress
                                      : 0,
                                  showsActiveBackground: false,
                                  onTap: widget.onItemSelected == null
                                      ? null
                                      : () =>
                                            widget.onItemSelected!.call(index),
                                );
                              },
                            ),
                          ),
                      ],
                    ),
                  ],
                ),
              );
            },
          );
        },
      ),
    );

    return ClipPath(
      clipper: const ShapeBorderClipper(shape: shape),
      child: widget.blur
          ? BackdropFilter(
              filter: ImageFilter.blur(
                sigmaX: LbSpacing.bottomNavBlur,
                sigmaY: LbSpacing.bottomNavBlur,
              ),
              child: navShell,
            )
          : navShell,
    );
  }
}
