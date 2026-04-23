import 'package:flutter/material.dart';
import 'package:soft_edge_blur/soft_edge_blur.dart';

import '../../theme/livebridge_tokens.dart';

class LbEdgeBlurOverlay extends StatelessWidget {
  const LbEdgeBlurOverlay({
    super.key,
    required this.child,
    this.topHeight = 0,
    this.bottomHeight = 0,
  });

  final Widget child;
  final double topHeight;
  final double bottomHeight;

  @override
  Widget build(BuildContext context) {
    final LbPalette palette = LbPalette.of(context);
    final List<EdgeBlur> edges = <EdgeBlur>[
      if (topHeight > 0)
        EdgeBlur(
          type: EdgeType.topEdge,
          size: topHeight,
          sigma: LbEffects.topEdgeBlurSigma,
          tintColor: palette.background.withValues(alpha: LbEffects.topEdgeBlurTint),
          controlPoints: <ControlPoint>[
            ControlPoint(position: 0, type: ControlPointType.visible),
            ControlPoint(
              position: LbEffects.topEdgeBlurHoldStop,
              type: ControlPointType.visible,
            ),
            ControlPoint(
              position: LbEffects.topEdgeBlurFadeStop,
              type: ControlPointType.transparent,
            ),
            ControlPoint(position: 1, type: ControlPointType.transparent),
          ],
        ),
      if (bottomHeight > 0)
        EdgeBlur(
          type: EdgeType.bottomEdge,
          size: bottomHeight,
          sigma: LbEffects.bottomEdgeBlurSigma,
          tintColor: palette.background.withValues(
            alpha: LbEffects.bottomEdgeBlurTint,
          ),
          controlPoints: <ControlPoint>[
            ControlPoint(position: 0, type: ControlPointType.visible),
            ControlPoint(
              position: LbEffects.bottomEdgeBlurHoldStop,
              type: ControlPointType.visible,
            ),
            ControlPoint(
              position: LbEffects.bottomEdgeBlurFadeStop,
              type: ControlPointType.transparent,
            ),
            ControlPoint(position: 1, type: ControlPointType.transparent),
          ],
        ),
    ];

    if (edges.isEmpty) {
      return child;
    }

    return Stack(
      fit: StackFit.expand,
      children: <Widget>[
        SoftEdgeBlur(
          edges: edges,
          child: child,
        ),
        if (topHeight > 0)
          Align(
            alignment: Alignment.topCenter,
            child: IgnorePointer(
              child: _LbEdgeTintOverlay(
                height: topHeight,
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                strongAlpha: LbEffects.topEdgeOverlayTint,
                fadeStop: LbEffects.topEdgeOverlayFadeStop,
              ),
            ),
          ),
        if (bottomHeight > 0)
          Align(
            alignment: Alignment.bottomCenter,
            child: IgnorePointer(
              child: _LbEdgeTintOverlay(
                height: bottomHeight,
                begin: Alignment.bottomCenter,
                end: Alignment.topCenter,
                strongAlpha: LbEffects.bottomEdgeOverlayTint,
                fadeStop: LbEffects.bottomEdgeOverlayFadeStop,
              ),
            ),
          ),
      ],
    );
  }
}

class _LbEdgeTintOverlay extends StatelessWidget {
  const _LbEdgeTintOverlay({
    required this.height,
    required this.begin,
    required this.end,
    required this.strongAlpha,
    required this.fadeStop,
  });

  final double height;
  final Alignment begin;
  final Alignment end;
  final double strongAlpha;
  final double fadeStop;

  @override
  Widget build(BuildContext context) {
    final LbPalette palette = LbPalette.of(context);

    return SizedBox(
      width: double.infinity,
      height: height,
      child: DecoratedBox(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: begin,
            end: end,
            colors: <Color>[
              palette.background.withValues(alpha: strongAlpha),
              palette.background.withValues(alpha: strongAlpha * 0.5),
              palette.background.withValues(alpha: 0),
            ],
            stops: <double>[0, fadeStop * 0.55, fadeStop],
          ),
        ),
      ),
    );
  }
}
