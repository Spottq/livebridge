import 'package:flutter/material.dart';

import '../../l10n/app_strings.dart';
import '../../theme/livebridge_tokens.dart';
import '../../widgets/redesign/lb_detail_screen.dart';
import '../../widgets/redesign/lb_incremental_list_component.dart';
import '../../widgets/redesign/lb_list_component.dart';
import '../../widgets/redesign/lb_search_pill.dart';
import '../../widgets/redesign/lb_selection_indicator.dart';

class EditorAppScopeScreen extends StatefulWidget {
  const EditorAppScopeScreen({
    super.key,
    required this.options,
    required this.selected,
  });
  final Map<String, String> options;
  final String selected;
  @override
  State<EditorAppScopeScreen> createState() => _EditorAppScopeScreenState();
}

class _EditorAppScopeScreenState extends State<EditorAppScopeScreen> {
  final _search = TextEditingController();
  final _focus = FocusNode();
  String _query = '';
  bool _expanded = false;
  @override
  void initState() {
    super.initState();
    _search.addListener(
      () => setState(() => _query = _search.text.trim().toLowerCase()),
    );
  }

  @override
  void dispose() {
    _search.dispose();
    _focus.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final s = AppStrings.of(context);
    final apps =
        widget.options.entries
            .where(
              (app) => '${app.key} ${app.value}'.toLowerCase().contains(_query),
            )
            .toList()
          ..sort((a, b) => a.value.compareTo(b.value));
    final options = <MapEntry<String, String>>[
      if (_query.isEmpty || s.filterAllApps.toLowerCase().contains(_query))
        MapEntry('*', s.filterAllApps),
      ...apps,
    ];
    return LbDetailScreen(
      title: s.appsListTitle,
      floatingBottom: LbSearchPill(
        expanded: _expanded,
        controller: _search,
        focusNode: _focus,
        onOpen: () {
          setState(() => _expanded = true);
          _focus.requestFocus();
        },
        onClose: () {
          _focus.unfocus();
          _search.clear();
          setState(() => _expanded = false);
        },
      ),
      floatingBottomReservedHeight: LbSpacing.searchPillReservedHeight,
      children: [
        if (options.isEmpty)
          Text(
            s.searchNoResults,
            style: LbTextStyles.body.copyWith(
              color: LbPalette.of(context).textSecondary,
            ),
          )
        else
          LbIncrementalListComponent(
            key: ValueKey(_query),
            rowHeight: LbSpacing.recentRowHeight,
            items: options
                .map(
                  (app) => LbListItemData(
                    title: app.value,
                    description: app.key == '*' ? null : app.key,
                    showChevron: false,
                    trailingWidget: LbSelectionIndicator(
                      selected: app.key == widget.selected,
                    ),
                    trailingWidgetWidth: LbSpacing.selectionIndicatorSize,
                    onTap: () => Navigator.of(context).pop(app.key),
                  ),
                )
                .toList(),
          ),
      ],
    );
  }
}
