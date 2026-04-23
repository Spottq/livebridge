import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

import '../../theme/livebridge_tokens.dart';
import '../../utils/livebridge_haptics.dart';
import 'lb_list_component.dart';

class LbDictionaryLanguageSheetOption {
  const LbDictionaryLanguageSheetOption({
    required this.id,
    required this.label,
  });

  final String id;
  final String label;
}

class LbDictionaryLanguagesSheet extends StatefulWidget {
  const LbDictionaryLanguagesSheet({
    super.key,
    required this.title,
    required this.options,
    required this.initialEnabledIds,
    required this.onChanged,
  });

  final String title;
  final List<LbDictionaryLanguageSheetOption> options;
  final Set<String> initialEnabledIds;
  final ValueChanged<Set<String>> onChanged;

  @override
  State<LbDictionaryLanguagesSheet> createState() =>
      _LbDictionaryLanguagesSheetState();
}

class _LbDictionaryLanguagesSheetState
    extends State<LbDictionaryLanguagesSheet> {
  late Set<String> _enabledIds;

  @override
  void initState() {
    super.initState();
    _enabledIds = widget.initialEnabledIds.toSet();
  }

  Future<void> _setLanguageEnabled(String id, bool enabled) async {
    final Set<String> nextIds = _enabledIds.toSet();
    if (enabled) {
      nextIds.add(id);
    } else {
      nextIds.remove(id);
    }
    if (setEquals(_enabledIds, nextIds)) {
      return;
    }
    setState(() => _enabledIds = nextIds);
    widget.onChanged(nextIds);
  }

  @override
  Widget build(BuildContext context) {
    final LbPalette palette = LbPalette.of(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: <Widget>[
        Text(
          widget.title,
          style: LbTextStyles.title.copyWith(color: palette.textPrimary),
        ),
        const SizedBox(height: LbSpacing.md),
        LbListComponent(
          items: widget.options.map((LbDictionaryLanguageSheetOption option) {
            final bool enabled = _enabledIds.contains(option.id);
            return LbListItemData(
              title: option.label,
              showChevron: false,
              toggleValue: enabled,
              onToggle: (bool value) {
                unawaited(_setLanguageEnabled(option.id, value));
              },
              onTap: () {
                final bool nextValue = !enabled;
                unawaited(LiveBridgeHaptics.toggle(nextValue));
                unawaited(_setLanguageEnabled(option.id, nextValue));
              },
            );
          }).toList(),
          rowHeight: LbSpacing.recentRowHeight,
          extendDividersToEnd: true,
        ),
      ],
    );
  }
}
