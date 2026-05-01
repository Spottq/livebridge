import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class LbColors {
  const LbColors._();

  static const Color white = Color(0xFFFFFFFF);
  static const Color link = Color(0xFF5D70FF);
  static const Color accent = Color(0xFF7BBDB7);
  static const Color accentLight = Color(0xFF77C3BB);
  static const Color toggleActive = Color(0xFF79C0B9);
  static const Color toggleInactive = Color(0xFFA0A0A0);
  static const Color warning = Color(0xFFBD7B7B);
}

@immutable
class LbPalette {
  const LbPalette({
    required this.background,
    required this.surface,
    required this.surfaceSoft,
    required this.surfaceRaised,
    required this.pressedOverlay,
    required this.warningSurface,
    required this.textPrimary,
    required this.textSecondary,
    required this.textMuted,
    required this.accent,
    required this.accentStrong,
    required this.navForegroundActive,
    required this.navForegroundInactive,
    required this.toggleTrackActive,
    required this.toggleTrackInactive,
    required this.toggleThumb,
    required this.divider,
    required this.navBorder,
    required this.navBlurTint,
    required this.navActiveFill,
    required this.recentSeparator,
    required this.chevron,
    required this.link,
    required this.warning,
    required this.shadowOuter,
    required this.shadowInner,
    required this.shadowHighlight,
    required this.thumbBorder,
  });

  final Color background;
  final Color surface;
  final Color surfaceSoft;
  final Color surfaceRaised;
  final Color pressedOverlay;
  final Color warningSurface;
  final Color textPrimary;
  final Color textSecondary;
  final Color textMuted;
  final Color accent;
  final Color accentStrong;
  final Color navForegroundActive;
  final Color navForegroundInactive;
  final Color toggleTrackActive;
  final Color toggleTrackInactive;
  final Color toggleThumb;
  final Color divider;
  final Color navBorder;
  final Color navBlurTint;
  final Color navActiveFill;
  final Color recentSeparator;
  final Color chevron;
  final Color link;
  final Color warning;
  final Color shadowOuter;
  final Color shadowInner;
  final Color shadowHighlight;
  final Color thumbBorder;

  static const LbPalette dark = LbPalette(
    background: Color(0xFF0C1514),
    surface: Color(0xFF1C2626),
    surfaceSoft: Color(0xFF1E2929),
    surfaceRaised: Color(0xFF223030),
    pressedOverlay: Color(0x26000000),
    warningSurface: Color(0xFF261C1C),
    textPrimary: Color(0xFFF3F6F4),
    textSecondary: Color(0xFFA8AFAC),
    textMuted: Color(0xFF7D8784),
    accent: LbColors.accent,
    accentStrong: LbColors.accent,
    navForegroundActive: Color(0xFF71D3C9),
    navForegroundInactive: Color(0xFF71D3C9),
    toggleTrackActive: LbColors.toggleActive,
    toggleTrackInactive: LbColors.toggleInactive,
    toggleThumb: LbColors.white,
    divider: Color(0x14FFFFFF),
    navBorder: Color(0x1AFFFFFF),
    navBlurTint: Color(0xD91C2626),
    navActiveFill: Color(0x5286DDD5),
    recentSeparator: Color(0xFF313131),
    chevron: Color(0xFF505050),
    link: LbColors.link,
    warning: LbColors.warning,
    shadowOuter: Color(0x30000000),
    shadowInner: Color(0x29000000),
    shadowHighlight: Color(0x14FFFFFF),
    thumbBorder: Color(0x18FFFFFF),
  );

  static const LbPalette light = LbPalette(
    background: Color(0xFFF5F7F6),
    surface: Color(0xFFFFFFFF),
    surfaceSoft: Color(0xFFF0F4F3),
    surfaceRaised: Color(0xFFFFFFFF),
    pressedOverlay: Color(0x12000000),
    warningSurface: Color(0xFFF7ECEC),
    textPrimary: Color(0xFF0A0D0C),
    textSecondary: Color(0xFFC4C4C4),
    textMuted: Color(0xFFC4C4C4),
    accent: LbColors.accent,
    accentStrong: LbColors.accent,
    navForegroundActive: Color(0xFF68B8B0),
    navForegroundInactive: Color(0xFF375750),
    toggleTrackActive: LbColors.toggleActive,
    toggleTrackInactive: LbColors.toggleInactive,
    toggleThumb: LbColors.white,
    divider: Color(0xFFEFEFEF),
    navBorder: Color(0x1A375750),
    navBlurTint: Color(0xD9FFFFFF),
    navActiveFill: Color(0x66A6D2CE),
    recentSeparator: Color(0xFFEFEFEF),
    chevron: Color(0xFFC4C4C4),
    link: LbColors.link,
    warning: LbColors.warning,
    shadowOuter: Color(0x14000000),
    shadowInner: Color(0x12000000),
    shadowHighlight: Color(0x0AFFFFFF),
    thumbBorder: Color(0x14000000),
  );

  static LbPalette of(BuildContext context) {
    return Theme.of(context).brightness == Brightness.light ? light : dark;
  }
}

class LbAppTheme {
  const LbAppTheme._();

  static ThemeData light() =>
      _themeFor(brightness: Brightness.light, palette: LbPalette.light);

  static ThemeData dark() =>
      _themeFor(brightness: Brightness.dark, palette: LbPalette.dark);

  static ThemeData _themeFor({
    required Brightness brightness,
    required LbPalette palette,
  }) {
    final ColorScheme baseScheme = brightness == Brightness.light
        ? const ColorScheme.light()
        : const ColorScheme.dark();

    return ThemeData(
      brightness: brightness,
      useMaterial3: false,
      fontFamily: LbTextStyles.fontFamily,
      splashFactory: NoSplash.splashFactory,
      highlightColor: Colors.transparent,
      hoverColor: Colors.transparent,
      scaffoldBackgroundColor: palette.background,
      canvasColor: palette.surface,
      colorScheme: baseScheme.copyWith(
        primary: palette.navForegroundActive,
        secondary: palette.accent,
        surface: palette.surface,
        onSurface: palette.textPrimary,
      ),
    );
  }

  static SystemUiOverlayStyle overlayStyle(Brightness brightness) {
    final bool isLight = brightness == Brightness.light;

    return SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      statusBarIconBrightness: isLight ? Brightness.dark : Brightness.light,
      statusBarBrightness: isLight ? Brightness.light : Brightness.dark,
      systemNavigationBarColor: Colors.transparent,
      systemNavigationBarIconBrightness: isLight
          ? Brightness.dark
          : Brightness.light,
      systemNavigationBarDividerColor: Colors.transparent,
      systemStatusBarContrastEnforced: false,
      systemNavigationBarContrastEnforced: false,
    );
  }
}

class LbSpacing {
  const LbSpacing._();

  static const double xs = 6;
  static const double sm = 10;
  static const double md = 16;
  static const double lg = 24;
  static const double xl = 32;

  static const double screenHorizontal = 20;
  static const double heroTopInset = 60;
  static const double detailTopInset = 10;
  static const double titleToCardGap = 28;
  static const double recentSectionGap = 8;
  static const double screenBottom = 48;
  static const double statusCardPadding = 14;
  static const double statusCardMinHeight = 138;
  static const double statusCardToggleTopOffset = 2;
  static const double profileCardHeight = 92;
  static const double profileCardAvatarSize = 58;
  static const double profileCardIconSize = 24;
  static const double profileCardGap = 16;
  static const double statCardGap = 12;
  static const double statsToRecentGap = 22;
  static const double statCardPadding = 15;
  static const double statCardAspectRatio = 1.08;
  static const double statCardIcon = 30;
  static const double statCardIconGap = 7;
  static const double recentRowHeight = 63;
  static const double recentAvatarSize = 26;
  static const double recentAvatarGap = 13;
  static const double recentChevronSize = 19;
  static const double recentSeparatorThickness = 1;
  static const double recentEmptyStateHeight = 300;
  static const double listRowHeight = 78;
  static const double listLeadingSize = 38;
  static const double listLeadingIconSize = 18;
  static const double listLeadingGap = 18;
  static const double listTextOnlyInset = 8;
  static const double listTrailingGap = 14;
  static const double listAccessoryWidth = 40;
  static const double listTrailingIconSize = 24;
  static const double listTrailingStatusGap = 8;
  static const double listTrailingChevronIconGap = 4;
  static const double listDividerTrailingInset = 10;
  static const double listPressedRadius = 14;
  static const double inlineSuffixLift = 3;
  static const double sliderTrackHeight = 28;
  static const double sliderThumbSize = 18;
  static const double sliderIconBadgeSize = 34;
  static const double sliderSectionGap = 18;
  static const double sliderValueGap = 12;
  static const double sliderEndInset = 5;
  static const double sliderActiveThumbOffset = 5;
  static const double headerIconSize = 26;
  static const double headerIconTopInset = 0;
  static const double detailBackIconSize = 24;
  static const double detailHeaderGap = 14;
  static const double detailSectionGap = 22;
  static const double detailTitleGap = 26;
  static const double sectionActionGap = 12;
  static const double selectionIndicatorSize = 28;
  static const double selectionIndicatorInnerSize = 18;
  static const double selectionIndicatorStroke = 2.5;
  static const double appsLoadingHeight = 220;
  static const double appsLoadingSpinnerSize = 54;
  static const double appsLoadingStroke = 5;
  static const double appsLoadingTextGap = 16;
  static const double modalSheetHandleWidth = 92;
  static const double modalSheetHandleHeight = 6;
  static const double modalSheetTopPadding = 10;
  static const double modalSheetContentTopGap = 24;
  static const double modalSheetEntranceOffset = 48;
  static const double searchPillHeight = 66;
  static const double searchPillCollapsedSize = 66;
  static const double searchPillBottomGap = 12;
  static const double searchPillKeyboardGap = 12;
  static const double searchPillReservedHeight = 90;
  static const double searchPillHorizontalPadding = 20;
  static const double searchPillIconSize = 24;
  static const double searchPillCloseIconSize = 28;
  static const double searchPillInputGap = 12;
  static const double searchPillExpandedInset = 8;
  static const double bottomNavHeight = 68;
  static const double bottomNavMarginBottom = 4;
  static const double bottomNavHorizontalInset = 54;
  static const double bottomNavBlur = 6;
  static const double bottomNavInnerPadding = 5;
  static const double bottomNavHighlightInset = 0;
  static const double bottomNavItemGap = 4;
  static const double bottomNavIconSize = 25;
  static const double bottomNavItemHorizontalPadding = 10;
  static const double bottomNavItemActiveHorizontalPadding = 20;
  static const double bottomNavItemVerticalPadding = 8;
  static const double bottomNavItemActiveVerticalPadding = 8;
  static const double bottomNavLabelGap = 4;
  static const double bottomNavShadowBlur = 10;
  static const double bottomNavShadowOffsetY = 3;
  static const double toggleWidth = 40;
  static const double toggleHeight = 26;
  static const double togglePadding = 3;
  static const double toggleThumbSize = 20;
  static const double toggleThumbStretch = 9;
  static const double toggleThumbRadius = 10;
  static const double shadowOffsetY = 2;
  static const double shadowBlur = 4;
}

class LbRadius {
  const LbRadius._();

  static const double card = 20;
  static const double smallCard = 20;
  static const double nav = 999;
  static const double avatar = 999;
}

class LbEffects {
  const LbEffects._();

  static const double modalSheetBackdropBlur = 7;
  static const double modalSheetBackdropTint = 0.16;
}

class LbMotion {
  const LbMotion._();

  static const double detailRouteBackgroundShift = 140;
  static const double detailRouteBackgroundShiftStart = 0.0;
  static const Duration bottomNavHighlightReleaseDuration = Duration(
    milliseconds: 320,
  );
  static const double bottomNavHighlightTapScale = 0.84;
  static const double bottomNavHighlightDragScale = 0.852;
  static const double bottomNavHighlightSwipeScale = 0.9;
  static const double bottomNavHighlightReleaseOvershoot = 1.018;
  static const double modalSheetDismissThreshold = 0.2;
  static const double modalSheetDismissVelocity = 980;
}

class LbTextStyles {
  const LbTextStyles._();

  static const String fontFamily = 'SfProRounded';

  static const TextStyle display = TextStyle(
    fontFamily: fontFamily,
    fontSize: 43,
    fontWeight: FontWeight.w500,
    height: 0.96,
    letterSpacing: -1.35,
  );

  static const TextStyle cardTitle = TextStyle(
    fontFamily: fontFamily,
    fontSize: 23,
    fontWeight: FontWeight.w500,
    height: 1.06,
    letterSpacing: -0.7,
  );

  static const TextStyle title = TextStyle(
    fontFamily: fontFamily,
    fontSize: 19,
    fontWeight: FontWeight.w500,
    height: 1.1,
    letterSpacing: -0.35,
  );

  static const TextStyle detailTitle = TextStyle(
    fontFamily: fontFamily,
    fontSize: 24,
    fontWeight: FontWeight.w500,
    height: 1.02,
    letterSpacing: -0.5,
  );

  static const TextStyle body = TextStyle(
    fontFamily: fontFamily,
    fontSize: 15,
    fontWeight: FontWeight.w500,
    height: 1.18,
    letterSpacing: -0.2,
  );

  static const TextStyle caption = TextStyle(
    fontFamily: fontFamily,
    fontSize: 13,
    fontWeight: FontWeight.w500,
    height: 1.2,
    letterSpacing: -0.05,
  );

  static const TextStyle statTitle = TextStyle(
    fontFamily: fontFamily,
    fontSize: 13,
    fontWeight: FontWeight.w500,
    height: 1.16,
    letterSpacing: -0.15,
  );

  static const TextStyle statCaption = TextStyle(
    fontFamily: fontFamily,
    fontSize: 11,
    fontWeight: FontWeight.w500,
    height: 1.18,
    letterSpacing: -0.02,
  );

  static const TextStyle inlineSuffix = TextStyle(
    fontFamily: fontFamily,
    fontSize: 11,
    fontWeight: FontWeight.w500,
    height: 1,
    letterSpacing: -0.02,
  );

  static const TextStyle navLabel = TextStyle(
    fontFamily: fontFamily,
    fontSize: 11,
    fontWeight: FontWeight.w500,
    height: 1.05,
    letterSpacing: -0.02,
  );
}
