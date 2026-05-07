import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../l10n/app_locale_controller.dart';
import '../../l10n/app_strings.dart';
import '../../platform/livebridge_platform.dart';
import '../../theme/livebridge_tokens.dart';
import '../../utils/livebridge_haptics.dart';
import '../../widgets/redesign/lb_detail_screen.dart';
import '../../widgets/redesign/lb_icon.dart';
import '../../widgets/redesign/lb_list_component.dart';
import '../../widgets/redesign/lb_toast.dart';

class SettingsBackupRestoreScreen extends StatefulWidget {
  const SettingsBackupRestoreScreen({super.key});

  @override
  State<SettingsBackupRestoreScreen> createState() =>
      _SettingsBackupRestoreScreenState();
}

class _SettingsBackupRestoreScreenState
    extends State<SettingsBackupRestoreScreen> {
  bool _busy = false;

  Future<void> _exportSettings() async {
    if (_busy) {
      return;
    }
    unawaited(LiveBridgeHaptics.confirm());
    setState(() => _busy = true);
    try {
      final String savedUri =
          await LiveBridgePlatform.saveLiveBridgeSettingsBackupToDownloads();
      if (!mounted) {
        return;
      }
      final AppStrings strings = AppStrings.of(context);
      showLbToast(
        context,
        message: savedUri.trim().isEmpty
            ? strings.liveBridgeSettingsExportFailed
            : strings.liveBridgeSettingsExported,
        icon: savedUri.trim().isEmpty ? null : LbIconSymbol.downloadThree,
      );
    } catch (_) {
      if (mounted) {
        showLbToast(
          context,
          message: AppStrings.of(context).liveBridgeSettingsExportFailed,
        );
      }
    } finally {
      if (mounted) {
        setState(() => _busy = false);
      }
    }
  }

  Future<void> _importSettingsFile() async {
    if (_busy) {
      return;
    }
    unawaited(LiveBridgeHaptics.openSurface());
    setState(() => _busy = true);
    try {
      final FilePickerResult? result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: const <String>['lbst'],
        withData: true,
      );
      if (result == null || result.files.isEmpty) {
        return;
      }

      final PlatformFile selected = result.files.first;
      final String raw = selected.bytes != null
          ? utf8.decode(selected.bytes!)
          : await File(selected.path!).readAsString();
      await _importSettingsRaw(raw);
    } catch (_) {
      if (mounted) {
        showLbToast(
          context,
          message: AppStrings.of(context).liveBridgeSettingsImportFailed,
        );
      }
    } finally {
      if (mounted) {
        setState(() => _busy = false);
      }
    }
  }

  Future<void> _importFromDebugClipboard() async {
    if (_busy) {
      return;
    }
    unawaited(LiveBridgeHaptics.openSurface());
    setState(() => _busy = true);
    try {
      final ClipboardData? data = await Clipboard.getData('text/plain');
      final String raw = data?.text?.trim() ?? '';
      if (!_looksLikeJsonObject(raw)) {
        if (mounted) {
          showLbToast(
            context,
            message: AppStrings.of(context).copyOldDebugJsonFirst,
          );
        }
        return;
      }
      await _importSettingsRaw(raw);
    } catch (_) {
      if (mounted) {
        showLbToast(
          context,
          message: AppStrings.of(context).copyOldDebugJsonFirst,
        );
      }
    } finally {
      if (mounted) {
        setState(() => _busy = false);
      }
    }
  }

  bool _looksLikeJsonObject(String raw) {
    if (!raw.startsWith('{') || !raw.endsWith('}')) {
      return false;
    }
    try {
      return jsonDecode(raw) is Map;
    } catch (_) {
      return false;
    }
  }

  Future<void> _importSettingsRaw(String raw) async {
    final bool imported =
        await LiveBridgePlatform.importLiveBridgeSettingsBackup(raw);
    if (!mounted) {
      return;
    }
    final AppStrings strings = AppStrings.of(context);
    if (!imported) {
      showLbToast(context, message: strings.liveBridgeSettingsImportFailed);
      return;
    }

    await loadAppLocalePreference();
    if (!mounted) {
      return;
    }
    unawaited(LiveBridgeHaptics.confirm());
    showLbToast(
      context,
      message: strings.liveBridgeSettingsImported,
      icon: LbIconSymbol.upload,
    );
  }

  @override
  Widget build(BuildContext context) {
    final AppStrings strings = AppStrings.of(context);
    final LbPalette palette = LbPalette.of(context);

    return LbDetailScreen(
      title: strings.backupRestoreTitle,
      children: <Widget>[
        LbListComponent(
          items: <LbListItemData>[
            LbListItemData(
              title: strings.exportLiveBridgeSettingsTitle,
              showChevron: false,
              enabled: !_busy,
              trailingWidget: LbIcon(
                symbol: LbIconSymbol.downloadThree,
                size: 24,
                color: palette.textPrimary,
              ),
              onTap: () {
                unawaited(_exportSettings());
              },
            ),
            LbListItemData(
              title: strings.importLiveBridgeSettingsTitle,
              showChevron: false,
              enabled: !_busy,
              trailingWidget: LbIcon(
                symbol: LbIconSymbol.upload,
                size: 24,
                color: palette.textPrimary,
              ),
              onTap: () {
                unawaited(_importSettingsFile());
              },
            ),
            LbListItemData(
              title: strings.importFromDebugTitle,
              showChevron: false,
              enabled: !_busy,
              trailingWidget: LbIcon(
                symbol: LbIconSymbol.codeBraces,
                size: 24,
                color: palette.textPrimary,
              ),
              onTap: () {
                unawaited(_importFromDebugClipboard());
              },
            ),
          ],
          rowHeight: LbSpacing.recentRowHeight,
          extendDividersToEnd: true,
        ),
      ],
    );
  }
}
