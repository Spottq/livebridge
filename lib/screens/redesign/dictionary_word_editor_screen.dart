import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';

import '../../l10n/app_strings.dart';
import '../../platform/livebridge_platform.dart';
import 'editor_word_lists.dart';
import '../../theme/livebridge_tokens.dart';
import '../../widgets/redesign/lb_apps_loading_state.dart';
import '../../widgets/redesign/lb_detail_screen.dart';
import '../../widgets/redesign/lb_editor_field.dart';
import '../../widgets/redesign/lb_list_component.dart';
import '../../widgets/redesign/lb_toast.dart';

class DictionaryWordEditorScreen extends StatefulWidget {
  const DictionaryWordEditorScreen({super.key});
  @override
  State<DictionaryWordEditorScreen> createState() =>
      _DictionaryWordEditorScreenState();
}

class _DictionaryWordEditorScreenState
    extends State<DictionaryWordEditorScreen> {
  static const fields = [
    'otp_strong_triggers',
    'progress_words',
    'weather_words',
    'weather_package_hints',
    'known_navigation_packages',
    'navigation_package_markers',
    'vpn_package_markers',
    'order_context_hints',
    'food_words',
    'food_packages',
    'taxi_words',
    'taxi_packages',
  ];
  final _controllers = {for (final key in fields) key: TextEditingController()};
  bool _loading = true;
  bool _saving = false;
  bool _failed = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final raw = await LiveBridgePlatform.getDictionaryWordAdditions();
      final data = jsonDecode(raw) as Map;
      if (!mounted) return;
      for (final key in fields) {
        _controllers[key]!.text = ((data[key] as List?) ?? []).join('\n');
      }
    } catch (_) {
      if (mounted) _failed = true;
    }
    if (mounted) setState(() => _loading = false);
  }

  Future<void> _save() async {
    if (_saving || _loading || _failed) return;
    final words = {
      for (final key in fields) key: editorWords(_controllers[key]!.text),
    };
    final s = AppStrings.of(context);
    if (!words.values.every(validEditorWords)) {
      showLbToast(context, message: s.editorLimit);
      return;
    }
    setState(() => _saving = true);
    try {
      if (!await LiveBridgePlatform.setDictionaryWordAdditions(
        jsonEncode(words),
      )) {
        throw StateError('save failed');
      }
      if (mounted) {
        showLbToast(context, message: s.appPresentationSaved);
      }
    } catch (_) {
      if (mounted) {
        showLbToast(context, message: s.settingsSaveError);
      }
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  void dispose() {
    for (final c in _controllers.values) {
      c.dispose();
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final s = AppStrings.of(context);
    final palette = LbPalette.of(context);
    return LbDetailScreen(
      title: s.dictionaryEditorTitle,
      avoidKeyboard: true,
      floatingBottom: _loading || _failed
          ? null
          : LbEditorSaveBar(
              label: s.save,
              onPressed: _saving ? null : () => unawaited(_save()),
            ),
      floatingBottomReservedHeight: 84,
      children: [
        if (_loading)
          const LbAppsLoadingState()
        else if (_failed)
          Text(
            s.dictionaryUpdateFailed,
            style: LbTextStyles.body.copyWith(color: palette.textSecondary),
          )
        else ...[
          Text(
            s.wordEditorHelp,
            style: LbTextStyles.body.copyWith(color: palette.textSecondary),
          ),
          const SizedBox(height: LbSpacing.detailSectionGap),
          for (final key in fields)
            LbEditorField(
              fieldKey: ValueKey(key),
              label: s.dictionaryWordField(key),
              controller: _controllers[key]!,
              enabled: !_saving,
            ),
          LbListComponent(
            items: [
              LbListItemData(
                title: s.editorClear,
                showChevron: false,
                enabled: !_saving,
                onTap: () {
                  for (final c in _controllers.values) {
                    c.clear();
                  }
                },
              ),
            ],
          ),
        ],
      ],
    );
  }
}
