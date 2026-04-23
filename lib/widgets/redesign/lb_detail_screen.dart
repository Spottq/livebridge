import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../theme/livebridge_tokens.dart';
import '../../utils/livebridge_haptics.dart';
import 'lb_icon.dart';
import 'lb_swipe_dismiss_detail.dart';

class LbDetailScreen extends StatelessWidget {
  const LbDetailScreen({
    super.key,
    required this.title,
    required this.children,
    this.trailing,
    this.floatingBottom,
    this.floatingBottomReservedHeight = 0,
  });

  final String title;
  final List<Widget> children;
  final Widget? trailing;
  final Widget? floatingBottom;
  final double floatingBottomReservedHeight;

  @override
  Widget build(BuildContext context) {
    final LbPalette palette = LbPalette.of(context);
    final double topInset = MediaQuery.paddingOf(context).top;
    final double bottomInset = MediaQuery.paddingOf(context).bottom;
    final Brightness brightness = Theme.of(context).brightness;

    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: LbAppTheme.overlayStyle(brightness),
      child: PopScope(
        canPop: true,
        child: Scaffold(
          resizeToAvoidBottomInset: false,
          backgroundColor: Colors.transparent,
          body: LbSwipeDismissDetail(
            child: ColoredBox(
              color: palette.background,
              child: LayoutBuilder(
                builder: (BuildContext context, BoxConstraints constraints) {
                  return Stack(
                    children: <Widget>[
                      SingleChildScrollView(
                        physics: const BouncingScrollPhysics(),
                        padding: EdgeInsets.fromLTRB(
                          LbSpacing.screenHorizontal,
                          topInset + LbSpacing.detailTopInset,
                          LbSpacing.screenHorizontal,
                          LbSpacing.screenBottom +
                              bottomInset +
                              floatingBottomReservedHeight,
                        ),
                        child: ConstrainedBox(
                          constraints: BoxConstraints(
                            minHeight: constraints.maxHeight,
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: <Widget>[
                              Row(
                                children: <Widget>[
                                  GestureDetector(
                                    onTap: () {
                                      unawaited(LiveBridgeHaptics.selection());
                                      Navigator.of(context).maybePop();
                                    },
                                    behavior: HitTestBehavior.opaque,
                                    child: SizedBox(
                                      width:
                                          LbSpacing.detailBackIconSize +
                                          LbSpacing.xs,
                                      height:
                                          LbSpacing.detailBackIconSize +
                                          LbSpacing.xs,
                                      child: Center(
                                        child: LbIcon(
                                          symbol: LbIconSymbol.back,
                                          size: LbSpacing.detailBackIconSize,
                                          color: palette.textPrimary,
                                        ),
                                      ),
                                    ),
                                  ),
                                  const SizedBox(
                                    width: LbSpacing.detailHeaderGap,
                                  ),
                                  Expanded(
                                    child: Text(
                                      title,
                                      style: LbTextStyles.detailTitle.copyWith(
                                        color: palette.textPrimary,
                                      ),
                                    ),
                                  ),
                                  if (trailing != null) ...<Widget>[
                                    const SizedBox(width: LbSpacing.md),
                                    trailing!,
                                  ],
                                ],
                              ),
                              const SizedBox(height: LbSpacing.detailTitleGap),
                              ...children,
                            ],
                          ),
                        ),
                      ),
                      if (floatingBottom != null) floatingBottom!,
                    ],
                  );
                },
              ),
            ),
          ),
        ),
      ),
    );
  }
}
