import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';

import '../../l10n/app_strings.dart';
import '../../models/app_models.dart';
import '../../platform/livebridge_platform.dart';
import 'editor_word_lists.dart';
import 'editor_app_scope_screen.dart';
import 'rules_runtime.dart';
import '../../theme/livebridge_tokens.dart';
import '../../widgets/redesign/lb_apps_loading_state.dart';
import '../../widgets/redesign/lb_detail_screen.dart';
import '../../widgets/redesign/lb_editor_field.dart';
import '../../widgets/redesign/lb_list_component.dart';
import '../../widgets/redesign/lb_toast.dart';

class SettingsTextFiltersScreen extends StatefulWidget {
  const SettingsTextFiltersScreen({super.key});
  @override
  State<SettingsTextFiltersScreen> createState() =>
      _SettingsTextFiltersScreenState();
}

class _SettingsTextFiltersScreenState extends State<SettingsTextFiltersScreen> {
  final _allow = TextEditingController();
  final _deny = TextEditingController();
  final _template = TextEditingController();
  Map<String, dynamic> _rules = {};
  Map<String, String> _apps = {};
  String _scope = '*';
  bool _all = false;
  bool _loading = true;
  bool _failed = false;
  bool _saving = false;
  bool _choosingScope = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final filters = LiveBridgePlatform.getNotificationTextFilters();
      final apps = LiveBridgePlatform.getAppListAccessGranted()
          .then<List<InstalledApp>>(
            (granted) => granted
                ? LiveBridgePlatform.getInstalledApps()
                : <InstalledApp>[],
          )
          .catchError((_) => <InstalledApp>[]);
      final observedFuture = LiveBridgePlatform.getSourceChannels().catchError(
        (_) => "[]",
      );
      final rules = Map<String, dynamic>.from(jsonDecode(await filters) as Map);
      final installed = await apps;
      final observed = jsonDecode(await observedFuture) as List;
      if (!mounted) return;
      _rules = rules;
      _apps = {for (final app in installed) app.packageName: app.label};
      for (final item in observed) {
        _apps.putIfAbsent(
          item['packageName'] as String,
          () => (item['appLabel'] ?? item['packageName']) as String,
        );
      }
      for (final pkg in rules.keys.where((v) => v != '*')) {
        _apps.putIfAbsent(pkg, () => pkg);
      }
      _readScope();
    } catch (_) {
      if (mounted) _failed = true;
    }
    if (mounted) setState(() => _loading = false);
  }

  void _readScope() {
    final rule = _rules[_scope] as Map? ?? {};
    _allow.text = ((rule['allow'] as List?) ?? []).join('\n');
    _deny.text = ((rule['deny'] as List?) ?? []).join('\n');
    _all = rule['all'] == true;
    _template.text = rule['template'] as String? ?? '';
  }

  void _stash() {
    final allow = editorWords(_allow.text);
    final deny = editorWords(_deny.text);
    if (allow.isEmpty && deny.isEmpty && _template.text.trim().isEmpty) {
      _rules.remove(_scope);
    } else {
      _rules[_scope] = {
        'allow': allow,
        'deny': deny,
        'all': _all,
        'template': _template.text.trim(),
      };
    }
  }

  Future<void> _save() async {
    if (_saving || _loading || _failed) return;
    _stash();
    final s = AppStrings.of(context);
    if (_rules.values.any(
      (v) =>
          !validEditorWords(((v['allow'] as List?) ?? []).cast<String>()) ||
          !validEditorWords(((v['deny'] as List?) ?? []).cast<String>()) ||
          ((v['template'] as String?)?.length ?? 0) > 200,
    )) {
      showLbToast(context, message: s.editorLimit);
      return;
    }
    setState(() => _saving = true);
    try {
      if (!await LiveBridgePlatform.setNotificationTextFilters(
        jsonEncode(_rules),
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
    _allow.dispose();
    _deny.dispose();
    _template.dispose();
    super.dispose();
  }

  Future<void> _chooseScope() async {
    if (_saving || _choosingScope) return;
    FocusScope.of(context).unfocus();
    setState(() => _choosingScope = true);
    try {
      // Enumerate installed apps only after the same consent used elsewhere in the app.
      if (await lbEnsureAppListAccess(context)) {
        final apps = await LiveBridgePlatform.getInstalledApps();
        if (!mounted) return;
        for (final app in apps) {
          _apps[app.packageName] = app.label;
        }
      }
      if (!mounted) return;
      final selected = await Navigator.of(context).push<String>(
        MaterialPageRoute(
          builder: (_) =>
              EditorAppScopeScreen(options: _apps, selected: _scope),
        ),
      );
      if (!mounted || selected == null || selected == _scope) return;
      _stash();
      setState(() {
        _scope = selected;
        _readScope();
      });
    } catch (_) {
      if (mounted) {
        showLbToast(context, message: AppStrings.of(context).settingsSaveError);
      }
    } finally {
      if (mounted) setState(() => _choosingScope = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final s = AppStrings.of(context);
    final palette = LbPalette.of(context);
    return LbDetailScreen(
      title: s.textFiltersTitle,
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
            s.settingsSaveError,
            style: LbTextStyles.body.copyWith(color: palette.textSecondary),
          )
        else ...[
          LbListComponent(
            items: [
              LbListItemData(
                title: _scope == '*'
                    ? s.filterAllApps
                    : (_apps[_scope] ?? _scope),
                description: s.filterHelp,
                enabled: !_saving,
                onTap: () => unawaited(_chooseScope()),
              ),
            ],
          ),
          const SizedBox(height: LbSpacing.detailSectionGap),
          LbEditorField(
            fieldKey: const ValueKey('allowWords'),
            label: s.filterAllowWords,
            controller: _allow,
            enabled: !_saving,
            minLines: 3,
            maxLines: 8,
          ),
          LbListComponent(
            items: [
              LbListItemData(
                title: s.filterMatchAll,
                showChevron: false,
                enabled: !_saving,
                toggleValue: _all,
                onToggle: (value) => setState(() => _all = value),
                onTap: () => setState(() => _all = !_all),
              ),
            ],
          ),
          const SizedBox(height: LbSpacing.md),
          LbEditorField(
            fieldKey: const ValueKey('denyWords'),
            label: s.filterDenyWords,
            controller: _deny,
            enabled: !_saving,
            minLines: 3,
            maxLines: 8,
          ),
          LbEditorField(
            fieldKey: const ValueKey('textTemplate'),
            label: s.filterTemplateTitle,
            controller: _template,
            enabled: !_saving,
            maxLength: 200,
            minLines: 2,
            maxLines: 4,
          ),
          Text(
            s.filterTemplateHelp,
            style: LbTextStyles.caption.copyWith(color: palette.textSecondary),
          ),
        ],
      ],
    );
  }
}
