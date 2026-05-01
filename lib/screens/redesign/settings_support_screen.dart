import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../l10n/app_strings.dart';
import '../../theme/livebridge_tokens.dart';
import '../../utils/livebridge_haptics.dart';
import '../../widgets/redesign/lb_detail_screen.dart';
import '../../widgets/redesign/lb_icon.dart';
import '../../widgets/redesign/lb_list_component.dart';
import '../../widgets/redesign/lb_toast.dart';

class SettingsSupportScreen extends StatelessWidget {
  const SettingsSupportScreen({super.key});

  static const String _boostyUrl = 'https://boosty.to/appsfolder';
  static const String _cryptoDonationText = '';
  static const String _githubUrl = 'https://github.com/appsfolder/livebridge';
  static const String _telegramUrl = 'https://t.me/livebridge_dev';

  Future<void> _openUrl(BuildContext context, String rawUrl) async {
    final AppStrings strings = AppStrings.of(context);
    final String normalized = rawUrl.trim();
    if (normalized.isEmpty) {
      showLbToast(context, message: strings.supportMethodNotConfigured);
      return;
    }

    unawaited(LiveBridgeHaptics.openSurface());
    final bool opened = await launchUrl(
      Uri.parse(normalized),
      mode: LaunchMode.externalApplication,
    );
    if (!opened && context.mounted) {
      showLbToast(context, message: strings.linkOpenFailed);
    }
  }

  Future<void> _copyCryptoDetails(BuildContext context) async {
    final AppStrings strings = AppStrings.of(context);
    final String normalized = _cryptoDonationText.trim();
    if (normalized.isEmpty) {
      showLbToast(context, message: strings.supportMethodNotConfigured);
      return;
    }

    await Clipboard.setData(ClipboardData(text: normalized));
    if (!context.mounted) {
      return;
    }
    unawaited(LiveBridgeHaptics.selection());
    showLbToast(context, message: strings.supportCryptoCopied);
  }

  @override
  Widget build(BuildContext context) {
    final AppStrings strings = AppStrings.of(context);
    final LbPalette palette = LbPalette.of(context);

    return LbDetailScreen(
      title: strings.supportLiveBridgeTitle,
      children: <Widget>[
        _SupportIntroCard(
          title: strings.supportIntroTitle,
          body: strings.supportIntroBody,
        ),
        const SizedBox(height: LbSpacing.md),
        LbListComponent(
          items: <LbListItemData>[
            LbListItemData(
              title: strings.supportBoostyTitle,
              subtitle: strings.supportBoostySubtitle,
              leadingIcon: LbIconSymbol.bankCard,
              showChevron: false,
              trailingWidget: LbIcon(
                symbol: LbIconSymbol.externalLink,
                size: LbSpacing.listTrailingIconSize,
                color: palette.textPrimary,
              ),
              onTap: () => unawaited(_openUrl(context, _boostyUrl)),
            ),
            LbListItemData(
              title: strings.supportCryptoTitle,
              subtitle: strings.supportCryptoSubtitle,
              leadingIcon: LbIconSymbol.shieldCheck,
              showChevron: false,
              trailingWidget: LbIcon(
                symbol: LbIconSymbol.copy,
                size: LbSpacing.listTrailingIconSize,
                color: palette.textPrimary,
              ),
              onTap: () => unawaited(_copyCryptoDetails(context)),
            ),
          ],
          rowHeight: LbSpacing.recentRowHeight,
          extendDividersToEnd: true,
        ),
        const SizedBox(height: LbSpacing.md),
        LbListComponent(
          items: <LbListItemData>[
            LbListItemData(
              title: strings.supportDiscussTitle,
              subtitle: strings.supportDiscussSubtitle,
              leadingIcon: LbIconSymbol.telegram,
              showChevron: false,
              trailingWidget: LbIcon(
                symbol: LbIconSymbol.externalLink,
                size: LbSpacing.listTrailingIconSize,
                color: palette.textPrimary,
              ),
              onTap: () => unawaited(_openUrl(context, _telegramUrl)),
            ),
            LbListItemData(
              title: strings.supportGithubTitle,
              subtitle: strings.supportGithubSubtitle,
              leadingIcon: LbIconSymbol.github,
              showChevron: false,
              trailingWidget: LbIcon(
                symbol: LbIconSymbol.externalLink,
                size: LbSpacing.listTrailingIconSize,
                color: palette.textPrimary,
              ),
              onTap: () => unawaited(_openUrl(context, _githubUrl)),
            ),
          ],
          rowHeight: LbSpacing.recentRowHeight,
          extendDividersToEnd: true,
        ),
      ],
    );
  }
}

class _SupportIntroCard extends StatelessWidget {
  const _SupportIntroCard({required this.title, required this.body});

  final String title;
  final String body;

  @override
  Widget build(BuildContext context) {
    final LbPalette palette = LbPalette.of(context);

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(LbSpacing.lg),
      decoration: BoxDecoration(
        color: palette.surface,
        borderRadius: BorderRadius.circular(LbRadius.card),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          ClipOval(
            child: ColoredBox(
              color: palette.accent,
              child: SizedBox.square(
                dimension: LbSpacing.profileCardAvatarSize,
                child: Center(
                  child: LbIcon(
                    symbol: LbIconSymbol.handHeart,
                    size: LbSpacing.profileCardIconSize,
                    color: palette.background,
                  ),
                ),
              ),
            ),
          ),
          const SizedBox(width: LbSpacing.profileCardGap),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  title,
                  style: LbTextStyles.cardTitle.copyWith(
                    color: palette.textPrimary,
                  ),
                ),
                const SizedBox(height: LbSpacing.xs),
                Text(
                  body,
                  style: LbTextStyles.body.copyWith(
                    color: palette.textSecondary,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
