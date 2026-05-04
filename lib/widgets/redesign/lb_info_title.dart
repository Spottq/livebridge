import 'dart:async';

import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';

import '../../theme/livebridge_tokens.dart';
import 'lb_description_popover.dart';
import 'lb_hints_controller.dart';
import 'lb_icon.dart';

class LbInfoTitle extends StatefulWidget {
  const LbInfoTitle({
    super.key,
    required this.title,
    this.titleSuffix,
    this.description,
    required this.titleStyle,
    this.suffixStyle,
    this.iconColor,
    this.enabled = true,
  });

  final String title;
  final String? titleSuffix;
  final String? description;
  final TextStyle titleStyle;
  final TextStyle? suffixStyle;
  final Color? iconColor;
  final bool enabled;

  @override
  State<LbInfoTitle> createState() => _LbInfoTitleState();
}

class _LbInfoTitleState extends State<LbInfoTitle> {
  final GlobalKey _descriptionIconKey = GlobalKey();
  OverlayEntry? _descriptionOverlay;
  bool _listeningForOutsidePointer = false;

  @override
  void initState() {
    super.initState();
    LbHintsController.disabled.addListener(_handleHintsDisabledChanged);
    unawaited(LbHintsController.load());
  }

  @override
  void didUpdateWidget(covariant LbInfoTitle oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.description != widget.description) {
      _hideDescription();
    }
  }

  @override
  void dispose() {
    LbHintsController.disabled.removeListener(_handleHintsDisabledChanged);
    _hideDescription();
    super.dispose();
  }

  @override
  void deactivate() {
    _hideDescription();
    super.deactivate();
  }

  void _handleHintsDisabledChanged() {
    if (LbHintsController.disabled.value) {
      _hideDescription();
    }
  }

  void _startOutsidePointerListener() {
    if (_listeningForOutsidePointer) {
      return;
    }
    GestureBinding.instance.pointerRouter.addGlobalRoute(_handleGlobalPointer);
    _listeningForOutsidePointer = true;
  }

  void _stopOutsidePointerListener() {
    if (!_listeningForOutsidePointer) {
      return;
    }
    GestureBinding.instance.pointerRouter.removeGlobalRoute(
      _handleGlobalPointer,
    );
    _listeningForOutsidePointer = false;
  }

  void _handleGlobalPointer(PointerEvent event) {
    if (event is! PointerDownEvent) {
      return;
    }
    if (_isInsideDescriptionIcon(event.position)) {
      return;
    }
    _hideDescription();
  }

  bool _isInsideDescriptionIcon(Offset position) {
    final BuildContext? iconContext = _descriptionIconKey.currentContext;
    final RenderObject? renderObject = iconContext?.findRenderObject();
    if (renderObject is! RenderBox || !renderObject.hasSize) {
      return false;
    }
    final Offset origin = renderObject.localToGlobal(Offset.zero);
    return (origin & renderObject.size).contains(position);
  }

  void _toggleDescription() {
    if (_descriptionOverlay != null) {
      _hideDescription();
      return;
    }
    final String description = widget.description?.trim() ?? '';
    if (description.isEmpty) {
      return;
    }
    final BuildContext? iconContext = _descriptionIconKey.currentContext;
    final RenderBox? iconBox = iconContext?.findRenderObject() as RenderBox?;
    if (iconBox == null || !iconBox.hasSize) {
      return;
    }
    final Offset iconOrigin = iconBox.localToGlobal(Offset.zero);
    final Offset anchor =
        iconOrigin + Offset(iconBox.size.width / 2, iconBox.size.height / 2);
    _descriptionOverlay = OverlayEntry(
      builder: (BuildContext context) {
        return LbDescriptionPopover(text: description, anchor: anchor);
      },
    );
    Overlay.of(context, rootOverlay: true).insert(_descriptionOverlay!);
    _startOutsidePointerListener();
  }

  void _hideDescription() {
    _stopOutsidePointerListener();
    _descriptionOverlay?.remove();
    _descriptionOverlay = null;
  }

  @override
  Widget build(BuildContext context) {
    final LbPalette palette = LbPalette.of(context);
    final String normalizedDescription = widget.description?.trim() ?? '';

    return Row(
      children: <Widget>[
        Flexible(
          child: Text.rich(
            TextSpan(
              text: widget.title,
              style: widget.titleStyle,
              children: widget.titleSuffix == null
                  ? const <InlineSpan>[]
                  : <InlineSpan>[
                      WidgetSpan(
                        alignment: PlaceholderAlignment.middle,
                        child: Transform.translate(
                          offset: const Offset(0, -LbSpacing.inlineSuffixLift),
                          child: Padding(
                            padding: const EdgeInsets.only(left: 6),
                            child: Text(
                              widget.titleSuffix!,
                              style:
                                  widget.suffixStyle ??
                                  widget.titleStyle.copyWith(
                                    color: palette.textSecondary,
                                  ),
                            ),
                          ),
                        ),
                      ),
                    ],
            ),
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
          ),
        ),
        if (normalizedDescription.isNotEmpty)
          ValueListenableBuilder<bool>(
            valueListenable: LbHintsController.disabled,
            builder: (BuildContext context, bool hintsDisabled, Widget? child) {
              if (hintsDisabled) {
                return const SizedBox.shrink();
              }
              return Row(
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  const SizedBox(width: LbSpacing.listDescriptionIconGap),
                  GestureDetector(
                    key: _descriptionIconKey,
                    behavior: HitTestBehavior.opaque,
                    onTap: widget.enabled ? _toggleDescription : null,
                    child: LbIcon(
                      symbol: LbIconSymbol.info,
                      size: LbSpacing.listDescriptionIconSize,
                      color:
                          widget.iconColor ??
                          (widget.enabled
                              ? palette.textSecondary
                              : palette.textMuted),
                    ),
                  ),
                ],
              );
            },
          ),
      ],
    );
  }
}
