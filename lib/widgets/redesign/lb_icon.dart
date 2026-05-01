import 'package:flutter/material.dart';
import 'package:ming_cute_icons/ming_cute_icons.dart';

enum LbIconSymbol {
  overflow,
  defaultsFlow,
  filterTwo,
  copy,
  copyThree,
  externalLink,
  refresh,
  search,
  close,
  handHeart,
  wallet,
  bankCard,
  github,
  telegram,
  telegramFilled,
  translate,
  downloadThree,
  upload,
  quillPen,
  leaf,
  taskTwo,
  taskTwoFilled,
  rulesChecklist,
  rulesChecklistFilled,
  book,
  dashboard,
  dashboardFilled,
  gitBranch,
  settingsTuning,
  compass,
  safeLock,
  magic,
  playCircle,
  signal,
  grid,
  forbidCircle,
  shieldCheck,
  shieldCheckFilled,
  happy,
  happyFilled,
  sad,
  sadFilled,
  cloudArrowDown,
  cloudArrowDownFilled,
  home,
  homeFilled,
  receipt,
  receiptFilled,
  settings,
  settingsFilled,
  alertOctagonFilled,
  checkCircleFilled,
  checkFilled,
  back,
  chevronRight,
}

class LbIcon extends StatelessWidget {
  const LbIcon({
    super.key,
    required this.symbol,
    required this.size,
    required this.color,
    this.animation,
  });

  final LbIconSymbol symbol;
  final double size;
  final Color color;
  final Animation<double>? animation;

  @override
  Widget build(BuildContext context) {
    if (symbol == LbIconSymbol.overflow) {
      return SizedBox.square(
        dimension: size,
        child: Center(
          child: _LbOverflowDots(color: color, size: size),
        ),
      );
    }

    return Icon(_iconFor(symbol), size: size, color: color);
  }
}

IconData _iconFor(LbIconSymbol symbol) {
  switch (symbol) {
    case LbIconSymbol.overflow:
      return MingCuteIcons.mgc_dots_vertical_fill;
    case LbIconSymbol.defaultsFlow:
      return MingCuteIcons.mgc_transfer_line;
    case LbIconSymbol.filterTwo:
      return MingCuteIcons.mgc_filter_2_line;
    case LbIconSymbol.copy:
      return MingCuteIcons.mgc_copy_2_line;
    case LbIconSymbol.copyThree:
      return MingCuteIcons.mgc_copy_3_line;
    case LbIconSymbol.externalLink:
      return MingCuteIcons.mgc_external_link_line;
    case LbIconSymbol.refresh:
      return MingCuteIcons.mgc_refresh_2_line;
    case LbIconSymbol.search:
      return MingCuteIcons.mgc_search_2_line;
    case LbIconSymbol.close:
      return MingCuteIcons.mgc_close_line;
    case LbIconSymbol.handHeart:
      return MingCuteIcons.mgc_hand_heart_line;
    case LbIconSymbol.wallet:
      return MingCuteIcons.mgc_wallet_3_line;
    case LbIconSymbol.bankCard:
      return MingCuteIcons.mgc_bank_card_line;
    case LbIconSymbol.github:
      return MingCuteIcons.mgc_github_line;
    case LbIconSymbol.telegram:
      return MingCuteIcons.mgc_telegram_line;
    case LbIconSymbol.telegramFilled:
      return MingCuteIcons.mgc_telegram_fill;
    case LbIconSymbol.translate:
      return MingCuteIcons.mgc_translate_2_line;
    case LbIconSymbol.downloadThree:
      return MingCuteIcons.mgc_download_3_line;
    case LbIconSymbol.upload:
      return MingCuteIcons.mgc_upload_3_line;
    case LbIconSymbol.quillPen:
      return MingCuteIcons.mgc_quill_pen_line;
    case LbIconSymbol.leaf:
      return MingCuteIcons.mgc_leaf_line;
    case LbIconSymbol.taskTwo:
      return MingCuteIcons.mgc_task_2_line;
    case LbIconSymbol.taskTwoFilled:
      return MingCuteIcons.mgc_task_2_fill;
    case LbIconSymbol.rulesChecklist:
      return MingCuteIcons.mgc_list_check_2_line;
    case LbIconSymbol.rulesChecklistFilled:
      return MingCuteIcons.mgc_list_check_2_fill;
    case LbIconSymbol.book:
      return MingCuteIcons.mgc_book_6_line;
    case LbIconSymbol.dashboard:
      return MingCuteIcons.mgc_dashboard_3_line;
    case LbIconSymbol.dashboardFilled:
      return MingCuteIcons.mgc_dashboard_3_fill;
    case LbIconSymbol.gitBranch:
      return MingCuteIcons.mgc_git_branch_line;
    case LbIconSymbol.settingsTuning:
      return MingCuteIcons.mgc_settings_7_line;
    case LbIconSymbol.compass:
      return MingCuteIcons.mgc_compass_2_line;
    case LbIconSymbol.safeLock:
      return MingCuteIcons.mgc_safe_lock_line;
    case LbIconSymbol.magic:
      return MingCuteIcons.mgc_magic_2_line;
    case LbIconSymbol.playCircle:
      return MingCuteIcons.mgc_play_circle_line;
    case LbIconSymbol.signal:
      return MingCuteIcons.mgc_signal_line;
    case LbIconSymbol.grid:
      return MingCuteIcons.mgc_grid_2_line;
    case LbIconSymbol.forbidCircle:
      return MingCuteIcons.mgc_forbid_circle_line;
    case LbIconSymbol.shieldCheck:
      return MingCuteIcons.mgc_safe_shield_line;
    case LbIconSymbol.shieldCheckFilled:
      return MingCuteIcons.mgc_safe_shield_fill;
    case LbIconSymbol.happy:
      return MingCuteIcons.mgc_happy_line;
    case LbIconSymbol.happyFilled:
      return MingCuteIcons.mgc_happy_fill;
    case LbIconSymbol.sad:
      return MingCuteIcons.mgc_sad_line;
    case LbIconSymbol.sadFilled:
      return MingCuteIcons.mgc_sad_fill;
    case LbIconSymbol.cloudArrowDown:
      return MingCuteIcons.mgc_cloud_2_line;
    case LbIconSymbol.cloudArrowDownFilled:
      return MingCuteIcons.mgc_cloud_2_fill;
    case LbIconSymbol.home:
      return MingCuteIcons.mgc_home_4_line;
    case LbIconSymbol.homeFilled:
      return MingCuteIcons.mgc_home_4_fill;
    case LbIconSymbol.receipt:
      return MingCuteIcons.mgc_bill_line;
    case LbIconSymbol.receiptFilled:
      return MingCuteIcons.mgc_bill_fill;
    case LbIconSymbol.settings:
      return MingCuteIcons.mgc_settings_3_line;
    case LbIconSymbol.settingsFilled:
      return MingCuteIcons.mgc_settings_3_fill;
    case LbIconSymbol.alertOctagonFilled:
      return MingCuteIcons.mgc_alert_octagon_fill;
    case LbIconSymbol.checkCircleFilled:
      return MingCuteIcons.mgc_check_circle_fill;
    case LbIconSymbol.checkFilled:
      return MingCuteIcons.mgc_check_fill;
    case LbIconSymbol.back:
      return MingCuteIcons.mgc_left_line;
    case LbIconSymbol.chevronRight:
      return MingCuteIcons.mgc_right_small_line;
  }
}

class _LbOverflowDots extends StatelessWidget {
  const _LbOverflowDots({required this.color, required this.size});

  final Color color;
  final double size;

  @override
  Widget build(BuildContext context) {
    final double dotSize = size * 0.18;
    final double spacing = size * 0.13;

    return Column(
      mainAxisSize: MainAxisSize.min,
      children: List<Widget>.generate(3, (int index) {
        return Padding(
          padding: EdgeInsets.symmetric(vertical: spacing / 2),
          child: Container(
            width: dotSize,
            height: dotSize,
            decoration: BoxDecoration(color: color, shape: BoxShape.circle),
          ),
        );
      }),
    );
  }
}
