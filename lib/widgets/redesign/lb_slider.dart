import 'dart:async';

import 'package:flutter/material.dart';

import '../../theme/livebridge_tokens.dart';
import '../../utils/livebridge_haptics.dart';

class LbSlider extends StatefulWidget {
  const LbSlider({
    super.key,
    required this.value,
    required this.max,
    this.min = 0,
    this.onChanged,
    this.onChangeStart,
    this.onChangeEnd,
    this.enabled = true,
  });

  final double value;
  final double min;
  final double max;
  final ValueChanged<double>? onChanged;
  final ValueChanged<double>? onChangeStart;
  final ValueChanged<double>? onChangeEnd;
  final bool enabled;

  @override
  State<LbSlider> createState() => _LbSliderState();
}

class _LbSliderState extends State<LbSlider> {
  int _lastHapticStep = -1;

  void _handleChangeStart(double value) {
    _lastHapticStep = value.round();
    widget.onChangeStart?.call(value);
  }

  void _handleChanged(double value) {
    final int snapped = value.round();
    if (snapped != _lastHapticStep) {
      _lastHapticStep = snapped;
      unawaited(LiveBridgeHaptics.selection());
    }
    widget.onChanged?.call(value);
  }

  void _handleChangeEnd(double value) {
    _lastHapticStep = -1;
    widget.onChangeEnd?.call(value);
  }

  @override
  Widget build(BuildContext context) {
    final LbPalette palette = LbPalette.of(context);
    final bool isInteractive = widget.enabled && widget.onChanged != null;

    return SliderTheme(
      data: SliderTheme.of(context).copyWith(
        trackHeight: LbSpacing.sliderTrackHeight,
        activeTrackColor: Colors.transparent,
        inactiveTrackColor: Colors.transparent,
        disabledActiveTrackColor: Colors.transparent,
        disabledInactiveTrackColor: Colors.transparent,
        overlayShape: SliderComponentShape.noOverlay,
        thumbShape: _LbSliderThumbShape(palette: palette),
        trackShape: _LbSliderTrackShape(
          palette: palette,
          isEnabled: isInteractive,
        ),
      ),
      child: Slider(
        value: widget.value.clamp(widget.min, widget.max),
        min: widget.min,
        max: widget.max,
        onChanged: isInteractive ? _handleChanged : null,
        onChangeStart: isInteractive ? _handleChangeStart : null,
        onChangeEnd: isInteractive ? _handleChangeEnd : null,
      ),
    );
  }
}

class _LbSliderTrackShape extends SliderTrackShape {
  const _LbSliderTrackShape({
    required this.palette,
    required this.isEnabled,
  });

  final LbPalette palette;
  final bool isEnabled;

  @override
  Rect getPreferredRect({
    required RenderBox parentBox,
    Offset offset = Offset.zero,
    required SliderThemeData sliderTheme,
    bool isEnabled = false,
    bool isDiscrete = false,
  }) {
    final double trackHeight = sliderTheme.trackHeight ?? 0;
    final double thumbWidth =
        sliderTheme.thumbShape?.getPreferredSize(isEnabled, isDiscrete).width ??
        LbSpacing.sliderThumbSize;
    final double horizontalInset = (thumbWidth / 2) + LbSpacing.sliderEndInset;
    final double trackLeft = offset.dx + horizontalInset;
    final double trackTop =
        offset.dy + (parentBox.size.height - trackHeight) / 2;
    final double trackWidth = parentBox.size.width - (horizontalInset * 2);
    return Rect.fromLTWH(trackLeft, trackTop, trackWidth, trackHeight);
  }

  @override
  void paint(
    PaintingContext context,
    Offset offset, {
    required RenderBox parentBox,
    required SliderThemeData sliderTheme,
    required Animation<double> enableAnimation,
    required Offset thumbCenter,
    bool isEnabled = false,
    bool isDiscrete = false,
    required TextDirection textDirection,
    Offset? secondaryOffset,
  }) {
    final Canvas canvas = context.canvas;
    final Rect innerRect = getPreferredRect(
      parentBox: parentBox,
      offset: offset,
      sliderTheme: sliderTheme,
      isEnabled: isEnabled,
      isDiscrete: isDiscrete,
    );
    final double thumbRadius =
        (sliderTheme.thumbShape
                    ?.getPreferredSize(isEnabled, isDiscrete)
                    .width ??
                LbSpacing.sliderThumbSize) /
            2;
    final double visualInset = thumbRadius + LbSpacing.sliderEndInset;
    final Rect visibleRect = Rect.fromLTWH(
      innerRect.left - visualInset,
      innerRect.top,
      innerRect.width + (visualInset * 2),
      innerRect.height,
    );
    final Radius radius = Radius.circular(visibleRect.height / 2);

    final RRect fullTrack = RRect.fromRectAndRadius(visibleRect, radius);
    canvas.drawRRect(
      fullTrack,
      Paint()..color = palette.toggleTrackInactive,
    );

    double activeWidth = (thumbCenter.dx -
            visibleRect.left +
            thumbRadius +
            LbSpacing.sliderActiveThumbOffset)
        .clamp(0, visibleRect.width);

    if (activeWidth > 0 && activeWidth < visibleRect.height) {
      activeWidth = visibleRect.height + LbSpacing.sliderActiveThumbOffset;
    }

    if (activeWidth <= 0) {
      return;
    }

    final Rect activeRect = Rect.fromLTWH(
      visibleRect.left,
      visibleRect.top,
      activeWidth,
      visibleRect.height,
    );
    canvas.drawRRect(
      RRect.fromRectAndRadius(activeRect, radius),
      Paint()..color = isEnabled ? palette.toggleTrackActive : palette.toggleTrackInactive,
    );
  }
}

class _LbSliderThumbShape extends SliderComponentShape {
  const _LbSliderThumbShape({required this.palette});

  final LbPalette palette;

  @override
  Size getPreferredSize(bool isEnabled, bool isDiscrete) {
    return const Size.square(LbSpacing.sliderThumbSize);
  }

  @override
  void paint(
    PaintingContext context,
    Offset center, {
    required Animation<double> activationAnimation,
    required Animation<double> enableAnimation,
    required bool isDiscrete,
    required TextPainter labelPainter,
    required RenderBox parentBox,
    required SliderThemeData sliderTheme,
    required TextDirection textDirection,
    required double value,
    required double textScaleFactor,
    required Size sizeWithOverflow,
  }) {
    final Canvas canvas = context.canvas;
    final double radius = LbSpacing.sliderThumbSize / 2;

    canvas.drawCircle(
      center,
      radius,
      Paint()
        ..color = palette.shadowOuter
        ..maskFilter = const MaskFilter.blur(
          BlurStyle.normal,
          LbSpacing.shadowBlur,
        ),
    );

    canvas.drawCircle(
      center,
      radius,
      Paint()..color = palette.toggleThumb,
    );

    canvas.drawCircle(
      center,
      radius,
      Paint()
        ..color = palette.thumbBorder
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1,
    );
  }
}
