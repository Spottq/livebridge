import 'dart:async';

import 'package:flutter/material.dart';

import '../../theme/livebridge_tokens.dart';
import 'lb_icon.dart';

OverlayEntry? _activeToastEntry;

void showLbToast(
  BuildContext context, {
  required String message,
  LbIconSymbol? icon,
  Duration duration = const Duration(milliseconds: 1800),
}) {
  _activeToastEntry?.remove();
  _activeToastEntry = null;

  final OverlayState overlay = Overlay.of(context, rootOverlay: true);

  late final OverlayEntry entry;
  entry = OverlayEntry(
    builder: (BuildContext overlayContext) {
      return _LbToastOverlay(
        message: message,
        icon: icon,
        duration: duration,
        onClosed: () {
          if (entry.mounted) {
            entry.remove();
          }
          if (_activeToastEntry == entry) {
            _activeToastEntry = null;
          }
        },
      );
    },
  );

  _activeToastEntry = entry;
  overlay.insert(entry);
}

class _LbToastOverlay extends StatefulWidget {
  const _LbToastOverlay({
    required this.message,
    required this.duration,
    required this.onClosed,
    this.icon,
  });

  final String message;
  final LbIconSymbol? icon;
  final Duration duration;
  final VoidCallback onClosed;

  @override
  State<_LbToastOverlay> createState() => _LbToastOverlayState();
}

class _LbToastOverlayState extends State<_LbToastOverlay> {
  bool _visible = false;
  Timer? _hideTimer;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) {
        return;
      }
      setState(() => _visible = true);
      _hideTimer = Timer(widget.duration, _dismiss);
    });
  }

  @override
  void dispose() {
    _hideTimer?.cancel();
    super.dispose();
  }

  void _dismiss() {
    if (!mounted) {
      widget.onClosed();
      return;
    }
    setState(() => _visible = false);
    Timer(const Duration(milliseconds: 180), widget.onClosed);
  }

  @override
  Widget build(BuildContext context) {
    final LbPalette palette = LbPalette.of(context);
    final double bottomInset = MediaQuery.paddingOf(context).bottom;

    return IgnorePointer(
      child: Material(
        color: Colors.transparent,
        child: Align(
          alignment: Alignment.bottomCenter,
          child: Padding(
            padding: EdgeInsets.fromLTRB(
              LbSpacing.screenHorizontal,
              0,
              LbSpacing.screenHorizontal,
              bottomInset + 18,
            ),
            child: AnimatedSlide(
              offset: _visible ? Offset.zero : const Offset(0, 0.18),
              duration: const Duration(milliseconds: 180),
              curve: Curves.easeOutCubic,
              child: AnimatedOpacity(
                opacity: _visible ? 1 : 0,
                duration: const Duration(milliseconds: 180),
                curve: Curves.easeOutCubic,
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 420),
                  child: DecoratedBox(
                    decoration: BoxDecoration(
                      color: palette.surfaceRaised,
                      borderRadius: BorderRadius.circular(25),
                      border: Border.all(
                        color: palette.navBorder.withValues(alpha: 0.55),
                      ),
                      boxShadow: <BoxShadow>[
                        BoxShadow(
                          color: Colors.black.withValues(alpha: 0.12),
                          blurRadius: 18,
                          offset: const Offset(0, 8),
                        ),
                      ],
                    ),
                    child: Padding(
                      padding: const EdgeInsets.symmetric(
                        horizontal: LbSpacing.md,
                        vertical: LbSpacing.sm,
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: <Widget>[
                          if (widget.icon != null) ...<Widget>[
                            LbIcon(
                              symbol: widget.icon!,
                              size: 20,
                              color: palette.accent,
                            ),
                            const SizedBox(width: LbSpacing.sm),
                          ],
                          Flexible(
                            child: Text(
                              widget.message,
                              style: LbTextStyles.body.copyWith(
                                color: palette.textPrimary,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
