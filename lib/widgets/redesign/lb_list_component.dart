import 'dart:async';

import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';

import '../../theme/livebridge_tokens.dart';
import 'lb_description_popover.dart';
import 'lb_hints_controller.dart';
import 'lb_icon.dart';
import 'lb_toggle.dart';

class LbListItemData {
  const LbListItemData({
    required this.title,
    this.titleSuffix,
    this.description,
    this.subtitle,
    this.leadingChild,
    this.leadingIcon,
    this.leadingText,
    this.leadingBackgroundColor,
    this.leadingForegroundColor,
    this.trailingIcon,
    this.trailingIconColor,
    this.trailingWidget,
    this.trailingWidgetWidth,
    this.showChevron = true,
    this.toggleValue,
    this.toggleTriggerHaptics = true,
    this.onToggle,
    this.onTap,
    this.accentLeading = false,
    this.enabled = true,
  });

  final String title;
  final String? titleSuffix;
  final String? description;
  final String? subtitle;
  final Widget? leadingChild;
  final LbIconSymbol? leadingIcon;
  final String? leadingText;
  final Color? leadingBackgroundColor;
  final Color? leadingForegroundColor;
  final LbIconSymbol? trailingIcon;
  final Color? trailingIconColor;
  final Widget? trailingWidget;
  final double? trailingWidgetWidth;
  final bool showChevron;
  final bool? toggleValue;
  final bool toggleTriggerHaptics;
  final ValueChanged<bool>? onToggle;
  final VoidCallback? onTap;
  final bool accentLeading;
  final bool enabled;
}

class LbListComponent extends StatelessWidget {
  const LbListComponent({
    super.key,
    required this.items,
    this.backgroundColor,
    this.rowHeight = LbSpacing.listRowHeight,
    this.leadingSize = LbSpacing.listLeadingSize,
    this.leadingIconSize = LbSpacing.listLeadingIconSize,
    this.leadingGap = LbSpacing.listLeadingGap,
    this.extendDividersToEnd = false,
  });

  final List<LbListItemData> items;
  final Color? backgroundColor;
  final double rowHeight;
  final double leadingSize;
  final double leadingIconSize;
  final double leadingGap;
  final bool extendDividersToEnd;

  @override
  Widget build(BuildContext context) {
    final LbPalette palette = LbPalette.of(context);

    return RepaintBoundary(
      child: Container(
        width: double.infinity,
        decoration: BoxDecoration(
          color: backgroundColor ?? palette.surface,
          borderRadius: BorderRadius.circular(LbRadius.card),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: <Widget>[
            for (int index = 0; index < items.length; index += 1)
              _LbListRow(
                item: items[index],
                rowHeight: rowHeight,
                leadingSize: leadingSize,
                leadingIconSize: leadingIconSize,
                leadingGap: leadingGap,
                extendDividersToEnd: extendDividersToEnd,
                isFirst: index == 0,
                isLast: index == items.length - 1,
                showDivider: index != items.length - 1,
              ),
          ],
        ),
      ),
    );
  }
}

class _LbListRow extends StatefulWidget {
  const _LbListRow({
    required this.item,
    required this.rowHeight,
    required this.leadingSize,
    required this.leadingIconSize,
    required this.leadingGap,
    required this.extendDividersToEnd,
    required this.isFirst,
    required this.isLast,
    required this.showDivider,
  });

  final LbListItemData item;
  final double rowHeight;
  final double leadingSize;
  final double leadingIconSize;
  final double leadingGap;
  final bool extendDividersToEnd;
  final bool isFirst;
  final bool isLast;
  final bool showDivider;

  @override
  State<_LbListRow> createState() => _LbListRowState();
}

class _LbListRowState extends State<_LbListRow> {
  final GlobalKey _descriptionIconKey = GlobalKey();
  OverlayEntry? _descriptionOverlay;
  bool _listeningForOutsidePointer = false;
  bool _isPressed = false;

  @override
  void initState() {
    super.initState();
    LbHintsController.disabled.addListener(_handleHintsDisabledChanged);
    unawaited(LbHintsController.load());
  }

  @override
  void didUpdateWidget(covariant _LbListRow oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.item.description != widget.item.description) {
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

  void _setPressed(bool value) {
    if (_isPressed == value || !mounted) {
      return;
    }
    setState(() => _isPressed = value);
  }

  Future<void> _handleTap() async {
    if (widget.item.onTap == null || !widget.item.enabled) {
      return;
    }
    if (!_isPressed) {
      _setPressed(true);
    }
    await Future<void>.delayed(const Duration(milliseconds: 42));
    if (!mounted) {
      return;
    }
    _setPressed(false);
    await Future<void>.delayed(const Duration(milliseconds: 22));
    if (!mounted) {
      return;
    }
    widget.item.onTap!.call();
  }

  void _toggleDescription() {
    if (_descriptionOverlay != null) {
      _hideDescription();
      return;
    }
    final String description = widget.item.description?.trim() ?? '';
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
    final Radius cardRadius = const Radius.circular(LbRadius.card);
    final bool isEnabled = widget.item.enabled;
    final bool hasTrailing =
        widget.item.showChevron || widget.item.toggleValue != null;
    final bool hasLeading =
        widget.item.leadingChild != null ||
        widget.item.leadingIcon != null ||
        widget.item.leadingText?.isNotEmpty == true;
    final bool hasCompactTrailingPair =
        widget.item.trailingIcon != null &&
        widget.item.showChevron &&
        widget.item.toggleValue == null &&
        widget.item.trailingWidget == null;
    final double dividerInset = hasLeading ? 0 : LbSpacing.listTextOnlyInset;
    double dividerTrailingInset = 0;

    if (!widget.extendDividersToEnd) {
      if (widget.item.toggleValue != null) {
        dividerTrailingInset =
            LbSpacing.listDividerTrailingInset + LbSpacing.listAccessoryWidth;
      } else if (widget.item.trailingWidget != null) {
        dividerTrailingInset =
            LbSpacing.listDividerTrailingInset +
            (widget.item.trailingWidgetWidth ?? LbSpacing.listAccessoryWidth);
      } else if (widget.item.showChevron) {
        dividerTrailingInset = 0;
      } else {
        dividerTrailingInset = LbSpacing.listDividerTrailingInset;
      }

      if (widget.item.trailingIcon != null &&
          !widget.item.showChevron &&
          widget.item.trailingWidget == null) {
        dividerTrailingInset += LbSpacing.listTrailingIconSize;
      }
    }

    return Semantics(
      button: widget.item.onTap != null,
      child: GestureDetector(
        onTap: widget.item.onTap == null || !isEnabled
            ? null
            : () => unawaited(_handleTap()),
        onTapDown: widget.item.onTap == null || !isEnabled
            ? null
            : (_) => _setPressed(true),
        onTapCancel: widget.item.onTap == null || !isEnabled
            ? null
            : () => _setPressed(false),
        behavior: HitTestBehavior.translucent,
        child: SizedBox(
          height: widget.rowHeight,
          child: AnimatedContainer(
            duration: _isPressed
                ? Duration.zero
                : const Duration(milliseconds: 140),
            curve: Curves.easeOutCubic,
            decoration: BoxDecoration(
              color: _isPressed ? palette.pressedOverlay : Colors.transparent,
              borderRadius: BorderRadius.only(
                topLeft: widget.isFirst ? cardRadius : Radius.zero,
                topRight: widget.isFirst ? cardRadius : Radius.zero,
                bottomLeft: widget.isLast ? cardRadius : Radius.zero,
                bottomRight: widget.isLast ? cardRadius : Radius.zero,
              ),
            ),
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: LbSpacing.md),
              child: Row(
                children: <Widget>[
                  if (hasLeading) ...<Widget>[
                    _LbLeadingCircle(
                      item: widget.item,
                      size: widget.leadingSize,
                      iconSize: widget.leadingIconSize,
                    ),
                    SizedBox(width: widget.leadingGap),
                  ],
                  Expanded(
                    child: Column(
                      children: <Widget>[
                        Expanded(
                          child: Row(
                            children: <Widget>[
                              if (!hasLeading)
                                const SizedBox(
                                  width: LbSpacing.listTextOnlyInset,
                                ),
                              Expanded(
                                child: _LbTextBlock(
                                  title: widget.item.title,
                                  titleSuffix: widget.item.titleSuffix,
                                  description: widget.item.description,
                                  subtitle: widget.item.subtitle,
                                  enabled: isEnabled,
                                  descriptionIconKey: _descriptionIconKey,
                                  onDescriptionTap: _toggleDescription,
                                ),
                              ),
                              if (hasCompactTrailingPair)
                                SizedBox(
                                  width:
                                      LbSpacing.listAccessoryWidth +
                                      LbSpacing.listTrailingIconSize,
                                  child: Row(
                                    mainAxisAlignment: MainAxisAlignment.end,
                                    mainAxisSize: MainAxisSize.min,
                                    children: <Widget>[
                                      LbIcon(
                                        symbol: widget.item.trailingIcon!,
                                        size: LbSpacing.listTrailingIconSize,
                                        color:
                                            widget.item.trailingIconColor ??
                                            palette.warning,
                                      ),
                                      const SizedBox(
                                        width: LbSpacing
                                            .listTrailingChevronIconGap,
                                      ),
                                      LbIcon(
                                        symbol: LbIconSymbol.chevronRight,
                                        size: LbSpacing.recentChevronSize,
                                        color: palette.chevron,
                                      ),
                                    ],
                                  ),
                                ),
                              if (!hasCompactTrailingPair &&
                                  widget.item.trailingIcon != null) ...<Widget>[
                                LbIcon(
                                  symbol: widget.item.trailingIcon!,
                                  size: LbSpacing.listTrailingIconSize,
                                  color:
                                      widget.item.trailingIconColor ??
                                      palette.warning,
                                ),
                                const SizedBox(
                                  width: LbSpacing.listTrailingStatusGap,
                                ),
                              ],
                              if (widget.item.trailingWidget !=
                                  null) ...<Widget>[
                                SizedBox(
                                  width:
                                      widget.item.trailingWidgetWidth ??
                                      LbSpacing.listAccessoryWidth,
                                  child: Align(
                                    alignment: Alignment.centerRight,
                                    child: widget.item.trailingWidget!,
                                  ),
                                ),
                              ],
                              if (!hasCompactTrailingPair &&
                                  hasTrailing) ...<Widget>[
                                SizedBox(
                                  width: LbSpacing.listAccessoryWidth,
                                  child: Align(
                                    alignment: Alignment.centerRight,
                                    child: widget.item.toggleValue != null
                                        ? LbToggle(
                                            value: widget.item.toggleValue!,
                                            onChanged: widget.item.onToggle,
                                            enabled: isEnabled,
                                            triggerHaptics: widget
                                                .item
                                                .toggleTriggerHaptics,
                                          )
                                        : widget.item.showChevron
                                        ? LbIcon(
                                            symbol: LbIconSymbol.chevronRight,
                                            size: LbSpacing.recentChevronSize,
                                            color: palette.chevron,
                                          )
                                        : null,
                                  ),
                                ),
                              ],
                            ],
                          ),
                        ),
                        if (widget.showDivider)
                          Padding(
                            padding: EdgeInsets.only(
                              left: dividerInset,
                              right: dividerTrailingInset,
                            ),
                            child: Divider(
                              height: LbSpacing.recentSeparatorThickness,
                              thickness: LbSpacing.recentSeparatorThickness,
                              color: palette.recentSeparator,
                            ),
                          ),
                      ],
                    ),
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

class _LbTextBlock extends StatelessWidget {
  const _LbTextBlock({
    required this.title,
    this.titleSuffix,
    this.description,
    this.subtitle,
    required this.enabled,
    required this.descriptionIconKey,
    required this.onDescriptionTap,
  });

  final String title;
  final String? titleSuffix;
  final String? description;
  final String? subtitle;
  final bool enabled;
  final GlobalKey descriptionIconKey;
  final VoidCallback onDescriptionTap;

  @override
  Widget build(BuildContext context) {
    final LbPalette palette = LbPalette.of(context);
    final Color titleColor = enabled
        ? palette.textPrimary
        : palette.textSecondary;
    final Color subtitleColor = enabled
        ? palette.textSecondary
        : palette.textMuted;

    if (subtitle == null) {
      return Align(
        alignment: Alignment.centerLeft,
        child: _LbTitleLine(
          title: title,
          titleSuffix: titleSuffix,
          titleColor: titleColor,
          suffixColor: subtitleColor,
          description: description,
          enabled: enabled,
          descriptionIconKey: descriptionIconKey,
          onDescriptionTap: onDescriptionTap,
        ),
      );
    }

    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        _LbTitleLine(
          title: title,
          titleSuffix: titleSuffix,
          titleColor: titleColor,
          suffixColor: subtitleColor,
          description: description,
          enabled: enabled,
          descriptionIconKey: descriptionIconKey,
          onDescriptionTap: onDescriptionTap,
        ),
        const SizedBox(height: 2),
        Text(
          subtitle!,
          style: LbTextStyles.caption.copyWith(color: subtitleColor),
        ),
      ],
    );
  }
}

class _LbTitleLine extends StatelessWidget {
  const _LbTitleLine({
    required this.title,
    this.titleSuffix,
    required this.titleColor,
    required this.suffixColor,
    this.description,
    required this.enabled,
    required this.descriptionIconKey,
    required this.onDescriptionTap,
  });

  final String title;
  final String? titleSuffix;
  final Color titleColor;
  final Color suffixColor;
  final String? description;
  final bool enabled;
  final GlobalKey descriptionIconKey;
  final VoidCallback onDescriptionTap;

  @override
  Widget build(BuildContext context) {
    final LbPalette palette = LbPalette.of(context);
    final String normalizedDescription = description?.trim() ?? '';

    return Row(
      mainAxisSize: MainAxisSize.max,
      children: <Widget>[
        Flexible(
          child: Text.rich(
            TextSpan(
              text: title,
              style: LbTextStyles.body.copyWith(color: titleColor),
              children: titleSuffix == null
                  ? const <InlineSpan>[]
                  : <InlineSpan>[
                      WidgetSpan(
                        alignment: PlaceholderAlignment.middle,
                        child: Transform.translate(
                          offset: const Offset(0, -LbSpacing.inlineSuffixLift),
                          child: Padding(
                            padding: const EdgeInsets.only(left: 6),
                            child: Text(
                              titleSuffix!,
                              style: LbTextStyles.inlineSuffix.copyWith(
                                color: suffixColor,
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
                    key: descriptionIconKey,
                    behavior: HitTestBehavior.opaque,
                    onTap: enabled ? onDescriptionTap : null,
                    child: LbIcon(
                      symbol: LbIconSymbol.info,
                      size: LbSpacing.listDescriptionIconSize,
                      color: enabled
                          ? palette.textSecondary
                          : palette.textMuted,
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

class _LbLeadingCircle extends StatelessWidget {
  const _LbLeadingCircle({
    required this.item,
    required this.size,
    required this.iconSize,
  });

  final LbListItemData item;
  final double size;
  final double iconSize;

  @override
  Widget build(BuildContext context) {
    final LbPalette palette = LbPalette.of(context);
    final Color backgroundColor =
        item.leadingBackgroundColor ??
        (item.accentLeading ? palette.accent : palette.surfaceSoft);
    final Color foregroundColor =
        item.leadingForegroundColor ??
        (item.accentLeading ? palette.background : palette.accent);

    return Container(
      width: size,
      height: size,
      decoration: item.leadingChild == null
          ? BoxDecoration(color: backgroundColor, shape: BoxShape.circle)
          : null,
      child: item.leadingChild != null
          ? Center(child: item.leadingChild!)
          : Center(
              child: item.leadingIcon != null
                  ? LbIcon(
                      symbol: item.leadingIcon!,
                      size: iconSize,
                      color: foregroundColor,
                    )
                  : Text(
                      item.leadingText ?? '?',
                      style: LbTextStyles.caption.copyWith(
                        color: foregroundColor,
                      ),
                    ),
            ),
    );
  }
}
