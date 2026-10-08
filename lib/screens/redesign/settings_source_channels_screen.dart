import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';

import '../../l10n/app_strings.dart';
import '../../models/app_models.dart';
import '../../platform/livebridge_platform.dart';
import '../../theme/livebridge_tokens.dart';
import '../../widgets/redesign/lb_apps_loading_state.dart';
import '../../widgets/redesign/lb_detail_screen.dart';
import '../../widgets/redesign/lb_icon.dart';
import '../../widgets/redesign/lb_incremental_list_component.dart';
import '../../widgets/redesign/lb_installed_app_avatar.dart';
import '../../widgets/redesign/lb_list_component.dart';
import '../../widgets/redesign/lb_search_pill.dart';
import '../../widgets/redesign/lb_toast.dart';

class SettingsSourceChannelsScreen extends StatefulWidget {
  const SettingsSourceChannelsScreen({super.key});
  @override
  State<SettingsSourceChannelsScreen> createState() =>
      _SettingsSourceChannelsScreenState();
}

class _SettingsSourceChannelsScreenState
    extends State<SettingsSourceChannelsScreen> {
  List<Map<String, dynamic>> _channels = [];
  final Set<String> _saving = {};
  final _search = TextEditingController();
  final _focus = FocusNode();
  String _query = '';
  bool _searchExpanded = false;
  bool _loading = true;
  bool _failed = false;

  @override
  void initState() {
    super.initState();
    _search.addListener(
      () => setState(() => _query = _search.text.trim().toLowerCase()),
    );
    unawaited(_load());
  }

  @override
  void dispose() {
    _search.dispose();
    _focus.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    if (_saving.isNotEmpty) return;
    setState(() {
      _loading = true;
      _failed = false;
    });
    try {
      final raw = await LiveBridgePlatform.getSourceChannels();
      final values = (jsonDecode(raw) as List)
          .cast<Map>()
          .map((v) => Map<String, dynamic>.from(v))
          .toList();
      if (mounted) setState(() => _channels = values);
    } catch (_) {
      if (mounted) setState(() => _failed = true);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  String _key(Map<String, dynamic> item) =>
      jsonEncode([item['packageName'], item['channelId']]);

  Future<void> _setEnabled(Map<String, dynamic> item, bool enabled) async {
    final key = _key(item);
    if (_loading || _saving.contains(key)) return;
    setState(() => _saving.add(key));
    try {
      final saved = await LiveBridgePlatform.setSourceChannelEnabled(
        item['packageName'] as String,
        item['channelId'] as String,
        enabled,
      );
      if (!saved) throw StateError('Save failed');
      if (mounted) setState(() => item['enabled'] = enabled);
    } catch (_) {
      if (mounted) {
        showLbToast(
          context,
          message: AppStrings.of(context).sourceChannelsError,
        );
      }
    } finally {
      if (mounted) setState(() => _saving.remove(key));
    }
  }

  @override
  Widget build(BuildContext context) {
    final s = AppStrings.of(context);
    final palette = LbPalette.of(context);
    final rows = _channels
        .where(
          (v) =>
              '${v['appLabel']} ${v['packageName']} ${v['name']} ${v['channelId']}'
                  .toLowerCase()
                  .contains(_query),
        )
        .toList();
    return LbDetailScreen(
      title: s.sourceChannelsTitle,
      trailing: Semantics(
        button: true,
        label: s.sourceChannelsRefresh,
        child: GestureDetector(
          onTap: _loading || _saving.isNotEmpty
              ? null
              : () => unawaited(_load()),
          behavior: HitTestBehavior.opaque,
          child: SizedBox.square(
            dimension: LbSpacing.headerIconSize + LbSpacing.sm,
            child: Center(
              child: LbIcon(
                symbol: LbIconSymbol.refresh,
                size: LbSpacing.headerIconSize,
                color: _loading || _saving.isNotEmpty
                    ? palette.textMuted
                    : palette.textPrimary,
              ),
            ),
          ),
        ),
      ),
      floatingBottom: _loading
          ? null
          : LbSearchPill(
              placeholder: s.sourceChannelsSearch,
              expanded: _searchExpanded,
              controller: _search,
              focusNode: _focus,
              onOpen: () {
                setState(() => _searchExpanded = true);
                _focus.requestFocus();
              },
              onClose: () {
                _focus.unfocus();
                _search.clear();
                setState(() => _searchExpanded = false);
              },
            ),
      floatingBottomReservedHeight: LbSpacing.searchPillReservedHeight,
      children: [
        Text(
          s.sourceChannelsDescription,
          style: LbTextStyles.body.copyWith(color: palette.textSecondary),
        ),
        const SizedBox(height: LbSpacing.detailSectionGap),
        if (_loading)
          const LbAppsLoadingState()
        else if (_failed)
          Text(
            s.sourceChannelsError,
            style: LbTextStyles.body.copyWith(color: palette.textSecondary),
          )
        else if (_channels.isEmpty)
          Text(
            s.sourceChannelsEmpty,
            style: LbTextStyles.body.copyWith(color: palette.textSecondary),
          )
        else if (rows.isEmpty)
          Text(
            s.searchNoResults,
            style: LbTextStyles.body.copyWith(color: palette.textSecondary),
          )
        else
          LbIncrementalListComponent(
            key: ValueKey(_query),
            items: rows.map((item) {
              final name = (item['appLabel'] ?? item['packageName']) as String;
              final enabled = item['enabled'] == true;
              return LbListItemData(
                title: (item['name'] ?? item['channelId']) as String,
                subtitle: name,
                description: '${item['packageName']}\n${item['channelId']}',
                leadingChild: LbInstalledAppAvatar(
                  app: InstalledApp(
                    packageName: item['packageName'] as String,
                    label: name,
                  ),
                  size: LbSpacing.recentAvatarSize,
                ),
                showChevron: false,
                toggleValue: enabled,
                enabled: !_saving.contains(_key(item)),
                onToggle: (value) => unawaited(_setEnabled(item, value)),
                onTap: () => unawaited(_setEnabled(item, !enabled)),
              );
            }).toList(),
            rowHeight: LbSpacing.recentRowHeight,
            extendDividersToEnd: true,
          ),
      ],
    );
  }
}
