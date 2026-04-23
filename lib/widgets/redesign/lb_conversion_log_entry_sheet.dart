import 'dart:async';

import 'package:flutter/material.dart';

import '../../l10n/app_strings.dart';
import '../../models/app_models.dart';
import '../../models/conversion_log_entry.dart';
import '../../theme/livebridge_tokens.dart';
import 'lb_icon.dart';
import 'lb_installed_app_avatar.dart';

class LbConversionLogEntrySheet extends StatelessWidget {
  const LbConversionLogEntrySheet({
    super.key,
    required this.entry,
    required this.formattedTime,
    required this.onCopyPayload,
    this.app,
  });

  final ConversionLogEntry entry;
  final String formattedTime;
  final InstalledApp? app;
  final VoidCallback onCopyPayload;

  InstalledApp get _displayApp =>
      app ??
      InstalledApp(packageName: entry.packageName, label: entry.appLabel);

  @override
  Widget build(BuildContext context) {
    final AppStrings strings = AppStrings.of(context);
    final LbPalette palette = LbPalette.of(context);
    final double maxHeight = MediaQuery.sizeOf(context).height * 0.56;

    return ConstrainedBox(
      constraints: BoxConstraints(maxHeight: maxHeight),
      child: SingleChildScrollView(
        physics: const BouncingScrollPhysics(),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            Row(
              children: <Widget>[
                LbInstalledAppAvatar(app: _displayApp, size: 42),
                const SizedBox(width: LbSpacing.md),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisSize: MainAxisSize.min,
                    children: <Widget>[
                      Text(
                        strings.conversionLogFrom(entry.appLabel),
                        style: LbTextStyles.title.copyWith(
                          color: palette.textPrimary,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        strings.conversionLogAt(formattedTime),
                        style: LbTextStyles.body.copyWith(
                          color: palette.textSecondary,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: LbSpacing.xl),
            _LbLogFieldSection(
              title: strings.conversionLogEntryTitleLabel,
              child: Text(
                entry.title.isEmpty ? '—' : entry.title,
                style: LbTextStyles.title.copyWith(color: palette.textPrimary),
              ),
            ),
            const SizedBox(height: LbSpacing.detailSectionGap),
            _LbLogFieldSection(
              title: strings.notificationTextOption,
              child: Text(
                entry.text.isEmpty ? '—' : entry.text,
                style: LbTextStyles.title.copyWith(
                  color: palette.textPrimary,
                  height: 1.22,
                ),
              ),
            ),
            const SizedBox(height: LbSpacing.detailSectionGap),
            _LbLogFieldSection(
              title: strings.payloadJsonTitle,
              trailing: _LbCopyPayloadButton(onTap: onCopyPayload),
              child: SelectableText(
                entry.payloadJson.isEmpty ? '{}' : entry.payloadJson,
                style: LbTextStyles.body.copyWith(
                  color: palette.textPrimary,
                  height: 1.3,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _LbLogFieldSection extends StatelessWidget {
  const _LbLogFieldSection({
    required this.title,
    required this.child,
    this.trailing,
  });

  final String title;
  final Widget child;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) {
    final LbPalette palette = LbPalette.of(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Row(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: <Widget>[
            Expanded(
              child: Text(
                title,
                style: LbTextStyles.title.copyWith(
                  color: palette.textSecondary,
                ),
              ),
            ),
            if (trailing != null) trailing!,
          ],
        ),
        const SizedBox(height: LbSpacing.sm),
        Container(
          width: double.infinity,
          padding: const EdgeInsets.all(LbSpacing.md),
          decoration: BoxDecoration(
            color: palette.surfaceSoft,
            borderRadius: BorderRadius.circular(25),
          ),
          child: child,
        ),
      ],
    );
  }
}

class _LbCopyPayloadButton extends StatefulWidget {
  const _LbCopyPayloadButton({required this.onTap});

  final VoidCallback onTap;

  @override
  State<_LbCopyPayloadButton> createState() => _LbCopyPayloadButtonState();
}

class _LbCopyPayloadButtonState extends State<_LbCopyPayloadButton> {
  bool _pressed = false;
  bool _copied = false;
  Timer? _copiedResetTimer;

  @override
  void dispose() {
    _copiedResetTimer?.cancel();
    super.dispose();
  }

  void _setPressed(bool value) {
    if (_pressed == value || !mounted) {
      return;
    }
    setState(() => _pressed = value);
  }

  void _showCopiedState() {
    _copiedResetTimer?.cancel();
    if (mounted) {
      setState(() => _copied = true);
    }
    _copiedResetTimer = Timer(const Duration(milliseconds: 900), () {
      if (!mounted) {
        return;
      }
      setState(() => _copied = false);
    });
  }

  @override
  Widget build(BuildContext context) {
    final LbPalette palette = LbPalette.of(context);

    return GestureDetector(
      onTapDown: (_) => _setPressed(true),
      onTapCancel: () => _setPressed(false),
      onTapUp: (_) => _setPressed(false),
      onTap: () {
        _showCopiedState();
        widget.onTap();
      },
      behavior: HitTestBehavior.opaque,
      child: AnimatedScale(
        scale: _pressed ? 0.92 : 1,
        duration: _pressed ? Duration.zero : const Duration(milliseconds: 150),
        curve: Curves.easeOutCubic,
        child: AnimatedOpacity(
          opacity: _pressed ? 0.8 : 1,
          duration: _pressed
              ? Duration.zero
              : const Duration(milliseconds: 150),
          curve: Curves.easeOutCubic,
          child: SizedBox(
            width: 36,
            height: 36,
            child: Center(
              child: AnimatedSwitcher(
                duration: const Duration(milliseconds: 180),
                switchInCurve: Curves.easeOutCubic,
                switchOutCurve: Curves.easeOutCubic,
                transitionBuilder: (Widget child, Animation<double> animation) {
                  return ScaleTransition(
                    scale: Tween<double>(
                      begin: 0.88,
                      end: 1,
                    ).animate(animation),
                    child: FadeTransition(opacity: animation, child: child),
                  );
                },
                child: LbIcon(
                  key: ValueKey<bool>(_copied),
                  symbol: _copied ? LbIconSymbol.copyThree : LbIconSymbol.copy,
                  size: 24,
                  color: palette.textPrimary,
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
