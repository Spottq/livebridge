import 'dart:ui' show lerpDouble;
import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../../theme/livebridge_tokens.dart';
import 'lb_icon.dart';

enum LbBottomNavMotion { sway, pop, spin }

class LbBottomNavData {
  const LbBottomNavData({
    required this.label,
    required this.icon,
    required this.motion,
    this.activeIcon,
  });

  final String label;
  final LbIconSymbol icon;
  final LbBottomNavMotion motion;
  final LbIconSymbol? activeIcon;
}

class LbBottomNavItem extends StatelessWidget {
  const LbBottomNavItem({
    super.key,
    required this.label,
    required this.icon,
    required this.activeProgress,
    required this.motion,
    this.motionProgress = 0,
    this.activeIcon,
    this.onTap,
    this.showsActiveBackground = true,
  });

  final String label;
  final LbIconSymbol icon;
  final double activeProgress;
  final LbBottomNavMotion motion;
  final double motionProgress;
  final LbIconSymbol? activeIcon;
  final VoidCallback? onTap;
  final bool showsActiveBackground;

  @override
  Widget build(BuildContext context) {
    final LbPalette palette = LbPalette.of(context);
    final double emphasis = activeProgress.clamp(0.0, 1.0);
    final Color inactiveColor = palette.navForegroundInactive;
    final Color foregroundColor = Color.lerp(
      inactiveColor,
      palette.navForegroundActive,
      emphasis,
    )!;
    final double horizontalPadding = lerpDouble(
      LbSpacing.bottomNavItemHorizontalPadding,
      LbSpacing.bottomNavItemActiveHorizontalPadding,
      emphasis,
    )!;
    final double verticalPadding = lerpDouble(
      LbSpacing.bottomNavItemVerticalPadding,
      LbSpacing.bottomNavItemActiveVerticalPadding,
      emphasis,
    )!;

    return Semantics(
      button: true,
      selected: emphasis >= 0.5,
      child: GestureDetector(
        onTap: onTap,
        behavior: HitTestBehavior.opaque,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 220),
          curve: Curves.easeOutCubic,
          margin: const EdgeInsets.symmetric(
            horizontal: LbSpacing.bottomNavItemGap,
          ),
          padding: EdgeInsets.symmetric(
            horizontal: horizontalPadding,
            vertical: verticalPadding,
          ),
          decoration: ShapeDecoration(
            color: showsActiveBackground
                ? palette.navActiveFill.withValues(alpha: 0.6 * emphasis)
                : Colors.transparent,
            shape: const StadiumBorder(),
            shadows: const <BoxShadow>[],
          ),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              _LbAnimatedNavIcon(
                motion: motion,
                progress: motionProgress.clamp(0.0, 1.0),
                child: LbIcon(
                  symbol: emphasis >= 0.5 ? (activeIcon ?? icon) : icon,
                  size: LbSpacing.bottomNavIconSize,
                  color: foregroundColor,
                ),
              ),
              const SizedBox(height: LbSpacing.bottomNavLabelGap),
              SizedBox(
                width: double.infinity,
                child: FittedBox(
                  fit: BoxFit.scaleDown,
                  child: Text(
                    label,
                    maxLines: 1,
                    softWrap: false,
                    textAlign: TextAlign.center,
                    style: LbTextStyles.navLabel.copyWith(
                      color: foregroundColor,
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _LbAnimatedNavIcon extends StatefulWidget {
  const _LbAnimatedNavIcon({
    required this.motion,
    required this.progress,
    required this.child,
  });

  final LbBottomNavMotion motion;
  final double progress;
  final Widget child;

  @override
  State<_LbAnimatedNavIcon> createState() => _LbAnimatedNavIconState();
}

class _LbAnimatedNavIconState extends State<_LbAnimatedNavIcon>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;
  bool _wasActive = false;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 480),
    );
    _wasActive = widget.progress > 0;
    if (_wasActive) {
      _controller.value = widget.progress.clamp(0.0, 1.0);
    }
  }

  @override
  void didUpdateWidget(covariant _LbAnimatedNavIcon oldWidget) {
    super.didUpdateWidget(oldWidget);
    final bool isActive = widget.progress > 0;

    if (!_wasActive && isActive) {
      _controller.forward(from: 0);
    }

    _wasActive = isActive;
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _controller,
      builder: (BuildContext context, _) {
        final double progress = _controller.value;
        double angle = 0;
        double offsetX = 0;
        double scale = 1;

        switch (widget.motion) {
          case LbBottomNavMotion.sway:
            final double wave = math.sin(progress * math.pi * 3);
            final double damping = 1 - (progress * 0.45);
            angle = wave * 0.18 * damping;
            offsetX = wave * 2.4 * damping;
          case LbBottomNavMotion.pop:
            scale = TweenSequence<double>(<TweenSequenceItem<double>>[
              TweenSequenceItem<double>(
                tween: Tween<double>(
                  begin: 1,
                  end: 0.9,
                ).chain(CurveTween(curve: Curves.easeOut)),
                weight: 26,
              ),
              TweenSequenceItem<double>(
                tween: Tween<double>(
                  begin: 0.9,
                  end: 1.16,
                ).chain(CurveTween(curve: Curves.easeOutBack)),
                weight: 34,
              ),
              TweenSequenceItem<double>(
                tween: Tween<double>(
                  begin: 1.16,
                  end: 1,
                ).chain(CurveTween(curve: Curves.easeInOut)),
                weight: 40,
              ),
            ]).transform(progress);
          case LbBottomNavMotion.spin:
            angle = Tween<double>(
              begin: 0,
              end: math.pi * 2,
            ).chain(CurveTween(curve: Curves.easeOutCubic)).transform(progress);
        }

        return Transform.translate(
          offset: Offset(offsetX, 0),
          child: Transform.rotate(
            angle: angle,
            child: Transform.scale(scale: scale, child: widget.child),
          ),
        );
      },
    );
  }
}
