import 'dart:ui';

import 'package:flutter/material.dart';

import '../../l10n/app_strings.dart';
import '../../theme/livebridge_tokens.dart';
import 'lb_icon.dart';

class LbSearchPill extends StatelessWidget {
  const LbSearchPill({
    super.key,
    required this.expanded,
    required this.controller,
    required this.focusNode,
    required this.onOpen,
    required this.onClose,
    this.placeholder,
  });

  final bool expanded;
  final TextEditingController controller;
  final FocusNode focusNode;
  final VoidCallback onOpen;
  final VoidCallback onClose;
  final String? placeholder;

  @override
  Widget build(BuildContext context) {
    final LbPalette palette = LbPalette.of(context);
    final String effectivePlaceholder =
        placeholder ?? AppStrings.of(context).searchForApps;
    final double bottomInset = MediaQuery.paddingOf(context).bottom;
    final double keyboardInset = MediaQuery.viewInsetsOf(context).bottom;
    final double bottomOffset = keyboardInset > 0
        ? keyboardInset + LbSpacing.searchPillKeyboardGap
        : bottomInset + LbSpacing.searchPillBottomGap;

    return AnimatedPadding(
      duration: const Duration(milliseconds: 220),
      curve: Curves.easeOutCubic,
      padding: EdgeInsets.fromLTRB(
        LbSpacing.screenHorizontal,
        0,
        LbSpacing.screenHorizontal,
        bottomOffset,
      ),
      child: LayoutBuilder(
        builder: (BuildContext context, BoxConstraints constraints) {
          final Widget shell = AnimatedContainer(
            duration: const Duration(milliseconds: 240),
            curve: Curves.easeOutCubic,
            width: expanded
                ? constraints.maxWidth
                : LbSpacing.searchPillCollapsedSize,
            height: LbSpacing.searchPillHeight,
            padding: EdgeInsets.symmetric(
              horizontal: expanded ? LbSpacing.searchPillHorizontalPadding : 0,
            ),
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
            child: expanded
                ? Row(
                    children: <Widget>[
                      Expanded(
                        child: GestureDetector(
                          onTap: () => focusNode.requestFocus(),
                          behavior: HitTestBehavior.opaque,
                          child: Align(
                            alignment: Alignment.centerLeft,
                            child: TextField(
                              controller: controller,
                              focusNode: focusNode,
                              cursorColor: palette.textPrimary,
                              style: LbTextStyles.title.copyWith(
                                color: palette.textPrimary,
                              ),
                              decoration: InputDecoration(
                                hintText: effectivePlaceholder,
                                hintStyle: LbTextStyles.title.copyWith(
                                  color: palette.textSecondary,
                                ),
                                border: InputBorder.none,
                                isDense: true,
                                contentPadding: EdgeInsets.zero,
                              ),
                            ),
                          ),
                        ),
                      ),
                      const SizedBox(width: LbSpacing.searchPillInputGap),
                      GestureDetector(
                        onTap: onClose,
                        behavior: HitTestBehavior.opaque,
                        child: SizedBox(
                          width:
                              LbSpacing.searchPillCloseIconSize + LbSpacing.sm,
                          height:
                              LbSpacing.searchPillCloseIconSize + LbSpacing.sm,
                          child: Center(
                            child: LbIcon(
                              symbol: LbIconSymbol.close,
                              size: LbSpacing.searchPillCloseIconSize,
                              color: palette.textPrimary,
                            ),
                          ),
                        ),
                      ),
                    ],
                  )
                : GestureDetector(
                    onTap: onOpen,
                    behavior: HitTestBehavior.opaque,
                    child: Center(
                      child: LbIcon(
                        symbol: LbIconSymbol.search,
                        size: LbSpacing.searchPillIconSize,
                        color: palette.textPrimary,
                      ),
                    ),
                  ),
          );

          return Align(
            alignment: Alignment.bottomRight,
            child: ClipPath(
              clipper: const ShapeBorderClipper(shape: StadiumBorder()),
              child: BackdropFilter(
                filter: ImageFilter.blur(
                  sigmaX: LbSpacing.bottomNavBlur,
                  sigmaY: LbSpacing.bottomNavBlur,
                ),
                child: shell,
              ),
            ),
          );
        },
      ),
    );
  }
}
