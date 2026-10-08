import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:livebridge/screens/redesign/dictionary_word_editor_screen.dart';
import 'package:livebridge/screens/redesign/editor_word_lists.dart';
import 'package:livebridge/screens/redesign/settings_text_filters_screen.dart';
import 'package:livebridge/widgets/redesign/lb_editor_field.dart';
import 'package:livebridge/widgets/redesign/lb_list_component.dart';

void main() {
  const channel = MethodChannel('livebridge/platform');
  final binding = TestWidgetsFlutterBinding.ensureInitialized();
  late Map<String, dynamic> filters;
  late Map<String, dynamic> additions;
  bool failSave = false;
  int wordWrites = 0;
  int appReads = 0;
  bool appsConsent = true;

  setUp(() {
    filters = {
      '*': {
        'allow': [],
        'deny': ['ads'],
        'all': false,
      },
      'app.one': {
        'allow': ['Order'],
        'deny': [],
        'all': false,
      },
    };
    additions = {
      'otp_strong_triggers': ['secret code'],
      'food_words': ['meal'],
    };
    failSave = false;
    wordWrites = 0;
    appReads = 0;
    appsConsent = true;
    binding.defaultBinaryMessenger.setMockMethodCallHandler(channel, (
      call,
    ) async {
      switch (call.method) {
        case 'getNotificationTextFilters':
          return jsonEncode(filters);
        case 'getSourceChannels':
          return '[]';
        case 'getAppListAccessGranted':
          return appsConsent;
        case 'getInstalledApps':
          appReads++;
          return [
            {'packageName': 'app.one', 'label': 'First'},
          ];
        case 'setNotificationTextFilters':
          if (failSave) return false;
          filters = Map<String, dynamic>.from(
            jsonDecode((call.arguments as Map)['value'] as String) as Map,
          );
          return true;
        case 'getDictionaryWordAdditions':
          return jsonEncode(additions);
        case 'setDictionaryWordAdditions':
          wordWrites++;
          if (failSave) return false;
          additions = Map<String, dynamic>.from(
            jsonDecode((call.arguments as Map)['value'] as String) as Map,
          );
          return true;
        default:
          return false;
      }
    });
  });
  tearDown(
    () =>
        binding.defaultBinaryMessenger.setMockMethodCallHandler(channel, null),
  );

  test(
    'word lists trim, ignore blank lines, deduplicate and enforce limits',
    () {
      expect(editorWords(' order\n\nready\norder '), ['order', 'ready']);
      expect(validEditorWords(List.generate(101, (i) => '$i')), isFalse);
      expect(validEditorWords(['x' * 201]), isFalse);
    },
  );

  testWidgets('global draft survives switching to per-app filters', (
    tester,
  ) async {
    await tester.pumpWidget(
      const MaterialApp(home: SettingsTextFiltersScreen()),
    );
    await tester.pumpAndSettle();
    await tester.enterText(
      find.byKey(const ValueKey('denyWords')),
      'ads\npromo',
    );
    final scope = tester
        .widgetList<LbListComponent>(find.byType(LbListComponent))
        .expand((list) => list.items)
        .singleWhere((item) => item.title == 'All apps');
    scope.onTap!();
    await tester.pumpAndSettle();
    await tester.tap(find.text('First').last);
    await tester.pumpAndSettle();
    expect(
      tester
          .widget<TextField>(find.byKey(const ValueKey('allowWords')))
          .controller!
          .text,
      'Order',
    );
    await tester.enterText(
      find.byKey(const ValueKey('allowWords')),
      'order\nready',
    );
    tester
        .widgetList<LbListComponent>(find.byType(LbListComponent))
        .expand((list) => list.items)
        .singleWhere((item) => item.toggleValue != null)
        .onToggle!(true);
    await tester.pumpAndSettle();

    await tester.tap(
      find.descendant(
        of: find.byType(LbEditorSaveBar),
        matching: find.byType(InkWell),
      ),
    );
    await tester.pumpAndSettle();
    expect(filters['*']['deny'], ['ads', 'promo']);
    expect(filters['app.one']['allow'], ['order', 'ready']);
    expect(filters['app.one']['all'], true);
    expect(tester.takeException(), isNull);
    await tester.pump(const Duration(seconds: 3));
    await tester.pumpAndSettle();
  });

  testWidgets(
    'word editor saves personal words without a JSON editor or touching other settings',
    (tester) async {
      await tester.pumpWidget(
        const MaterialApp(home: DictionaryWordEditorScreen()),
      );
      await tester.pumpAndSettle();
      expect(
        tester
            .widget<TextField>(
              find.byKey(const ValueKey('otp_strong_triggers')),
            )
            .controller!
            .text,
        'secret code',
      );
      await tester.enterText(
        find.byKey(const ValueKey('otp_strong_triggers')),
        'secret code\nlogin token',
      );

      await tester.tap(
        find.descendant(
          of: find.byType(LbEditorSaveBar),
          matching: find.byType(InkWell),
        ),
      );
      await tester.pumpAndSettle();
      expect(additions['otp_strong_triggers'], ['secret code', 'login token']);
      expect(additions['food_words'], ['meal']);
      expect(filters['app.one']['allow'], ['Order']);
      expect(wordWrites, 1);
      expect(tester.takeException(), isNull);
      await tester.pump(const Duration(seconds: 3));
      await tester.pumpAndSettle();
      await tester.pump(const Duration(seconds: 3));
      await tester.pumpAndSettle();
    },
  );

  testWidgets(
    'failed dictionary save reports an error and preserves stored additions',
    (tester) async {
      failSave = true;
      await tester.pumpWidget(
        const MaterialApp(home: DictionaryWordEditorScreen()),
      );
      await tester.pumpAndSettle();
      await tester.enterText(
        find.byKey(const ValueKey('otp_strong_triggers')),
        'new term',
      );

      await tester.tap(
        find.descendant(
          of: find.byType(LbEditorSaveBar),
          matching: find.byType(InkWell),
        ),
      );
      await tester.pumpAndSettle();
      expect(additions['otp_strong_triggers'], ['secret code']);
      expect(
        find.text('Could not save the setting. Please try again.'),
        findsOneWidget,
      );
      await tester.pump(const Duration(seconds: 3));
      await tester.pumpAndSettle();
    },
  );
  testWidgets('custom island template is saved without adding filter words', (
    tester,
  ) async {
    await tester.pumpWidget(
      const MaterialApp(home: SettingsTextFiltersScreen()),
    );
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(
      find.byKey(const ValueKey('textTemplate')),
      200,
      scrollable: find.byType(Scrollable).first,
    );
    await tester.enterText(
      find.byKey(const ValueKey('textTemplate')),
      '{app}: {title}',
    );
    await tester.tap(
      find.descendant(
        of: find.byType(LbEditorSaveBar),
        matching: find.byType(InkWell),
      ),
    );
    await tester.pumpAndSettle();
    expect(filters['*']['template'], '{app}: {title}');
    expect(filters['*']['deny'], ['ads']);
    expect(filters['app.one']['allow'], ['Order']);
    expect(tester.takeException(), isNull);
    await tester.pump(const Duration(seconds: 3));
    await tester.pumpAndSettle();
  });
  testWidgets(
    'filter editor respects installed app access and uses app consent sheet',
    (tester) async {
      appsConsent = false;
      await tester.pumpWidget(
        const MaterialApp(home: SettingsTextFiltersScreen()),
      );
      await tester.pumpAndSettle();
      expect(appReads, 0);
      tester
          .widgetList<LbListComponent>(find.byType(LbListComponent))
          .expand((list) => list.items)
          .singleWhere((item) => item.title == 'All apps')
          .onTap!();
      await tester.pumpAndSettle();
      expect(find.byType(AlertDialog), findsNothing);
      final cancel = tester
          .widgetList<LbListComponent>(find.byType(LbListComponent))
          .expand((list) => list.items)
          .singleWhere((item) => item.title == 'Cancel');
      cancel.onTap!();
      await tester.pumpAndSettle();
      expect(appReads, 0);
      expect(find.text('app.one'), findsOneWidget);
      expect(tester.takeException(), isNull);
    },
  );
}
