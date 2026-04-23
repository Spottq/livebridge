import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:flutter/material.dart';

import '../../l10n/app_strings.dart';
import '../../platform/livebridge_platform.dart';
import '../../theme/livebridge_tokens.dart';
import '../../utils/livebridge_haptics.dart';
import '../../widgets/redesign/lb_defaults_card.dart';
import '../../widgets/redesign/lb_detail_screen.dart';
import '../../widgets/redesign/lb_dictionary_languages_sheet.dart';
import '../../widgets/redesign/lb_icon.dart';
import '../../widgets/redesign/lb_list_component.dart';
import '../../widgets/redesign/lb_modal_bottom_sheet.dart';
import '../../widgets/redesign/lb_toast.dart';
import 'dictionary_runtime.dart';

class RulesDictionaryScreen extends StatefulWidget {
  const RulesDictionaryScreen({super.key});

  @override
  State<RulesDictionaryScreen> createState() => _RulesDictionaryScreenState();
}

class _RulesDictionaryScreenState extends State<RulesDictionaryScreen> {
  static const JsonEncoder _jsonEncoder = JsonEncoder.withIndent('  ');

  Set<String> _enabledLanguageIds = lbDictionaryLanguages
      .map((DictionaryLanguageOption option) => option.id)
      .toSet();
  bool _isUpdating = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      unawaited(_loadState());
    });
  }

  Future<void> _loadState() async {
    try {
      final List<String> enabledLanguageIds =
          await LiveBridgePlatform.getParserDictionaryEnabledLanguages();
      if (!mounted) {
        return;
      }
      setState(() {
        _enabledLanguageIds = lbNormalizeDictionaryLanguageIds(
          enabledLanguageIds,
        );
      });
    } catch (_) {}
  }

  Future<void> _setEnabledLanguageIds(Set<String> nextIds) async {
    final Set<String> normalized = lbNormalizeDictionaryLanguageIds(nextIds);
    setState(() => _enabledLanguageIds = normalized);
    await LiveBridgePlatform.setParserDictionaryEnabledLanguages(
      normalized.toList(),
    );
  }

  Future<void> _openLanguageSheet() async {
    final AppStrings strings = AppStrings.of(context);
    await showLbModalBottomSheet<void>(
      context: context,
      builder: (BuildContext context) => LbDictionaryLanguagesSheet(
        title: strings.dictionaryLanguagesPickerTitle,
        options: lbDictionaryLanguages
            .map(
              (DictionaryLanguageOption option) =>
                  LbDictionaryLanguageSheetOption(
                    id: option.id,
                    label: option.label,
                  ),
            )
            .toList(),
        initialEnabledIds: _enabledLanguageIds,
        onChanged: (Set<String> value) {
          unawaited(_setEnabledLanguageIds(value));
        },
      ),
    );
  }

  Future<void> _updateDictionaries() async {
    if (_isUpdating) {
      return;
    }
    unawaited(LiveBridgeHaptics.selection());
    setState(() => _isUpdating = true);
    final AppStrings strings = AppStrings.of(context);
    final HttpClient client = HttpClient()
      ..connectionTimeout = const Duration(seconds: 8);

    try {
      for (final DictionaryLanguageOption option in lbDictionaryLanguages) {
        final String raw = await _fetchDictionary(
          client: client,
          option: option,
        );
        final dynamic decoded = jsonDecode(raw);
        if (decoded is! Map) {
          if (mounted) {
            showLbToast(context, message: strings.dictionaryInvalid);
          }
          return;
        }
        final bool saved =
            await LiveBridgePlatform.setParserDictionaryLanguageOverride(
              languageId: option.id,
              value: _jsonEncoder.convert(Map<String, dynamic>.from(decoded)),
            );
        if (!saved) {
          throw const HttpException('dictionary_update_failed');
        }
      }

      if (!mounted) {
        return;
      }
      showLbToast(
        context,
        message: strings.dictionaryUpdateDone,
        icon: LbIconSymbol.cloudArrowDown,
      );
    } catch (_) {
      if (mounted) {
        showLbToast(context, message: strings.dictionaryUpdateFailed);
      }
    } finally {
      client.close(force: true);
      if (mounted) {
        setState(() => _isUpdating = false);
      }
    }
  }

  Future<String> _fetchDictionary({
    required HttpClient client,
    required DictionaryLanguageOption option,
  }) async {
    final HttpClientRequest request = await client.getUrl(
      Uri.parse(lbDictionaryRemoteUrl(option)),
    );
    request.headers.set(HttpHeaders.acceptHeader, 'application/json');
    request.headers.set(
      HttpHeaders.userAgentHeader,
      'LiveBridge/dictionary-update',
    );

    final HttpClientResponse response = await request.close();
    if (response.statusCode != HttpStatus.ok) {
      throw const HttpException('dictionary_update_failed');
    }

    final String raw = (await utf8.decoder.bind(response).join()).trim();
    if (raw.isEmpty) {
      throw const FormatException('dictionary_empty');
    }
    return raw;
  }

  @override
  Widget build(BuildContext context) {
    final AppStrings strings = AppStrings.of(context);
    final LbPalette palette = LbPalette.of(context);

    return LbDetailScreen(
      title: strings.dictionaryTitle,
      children: <Widget>[
        LbDefaultsCard(
          title: strings.dictionaryLanguagesTitle,
          subtitle: strings.dictionaryLanguagesSubtitle,
          icon: LbIconSymbol.translate,
          iconColor: palette.textPrimary,
          onTap: _openLanguageSheet,
        ),
        const SizedBox(height: LbSpacing.md),
        LbListComponent(
          items: <LbListItemData>[
            LbListItemData(
              title: strings.dictionaryUpdateAction,
              showChevron: false,
              trailingIcon: _isUpdating ? null : LbIconSymbol.downloadThree,
              trailingIconColor: palette.textPrimary,
              trailingWidget: _isUpdating
                  ? SizedBox.square(
                      dimension: 22,
                      child: CircularProgressIndicator(
                        strokeWidth: 2.2,
                        valueColor: AlwaysStoppedAnimation<Color>(
                          palette.accent,
                        ),
                      ),
                    )
                  : null,
              trailingWidgetWidth: 22,
              onTap: _isUpdating ? null : _updateDictionaries,
            ),
            LbListItemData(
              title: strings.dictionaryEditorTitle,
              titleSuffix: strings.dictionaryComingSoon,
              showChevron: false,
              trailingIcon: LbIconSymbol.quillPen,
              trailingIconColor: palette.textMuted,
              enabled: false,
            ),
          ],
          rowHeight: LbSpacing.recentRowHeight,
        ),
      ],
    );
  }
}
