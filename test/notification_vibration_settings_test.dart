import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:livebridge/screens/redesign/settings_app_config_screen.dart';
import 'package:livebridge/widgets/redesign/lb_list_component.dart';

void main() {
  const channel = MethodChannel('livebridge/platform');
  final binding = TestWidgetsFlutterBinding.ensureInitialized();
  bool failSave = false;
  bool vibrationEnabled = false;
  bool hideRecents = false;
  final writes = <bool>[];

  setUp(() {
    failSave = false;
    vibrationEnabled = false;
    hideRecents = false;
    writes.clear();
    binding.defaultBinaryMessenger.setMockMethodCallHandler(channel, (
      call,
    ) async {
      switch (call.method) {
        case 'getHideFromRecentsEnabled':
          return hideRecents;
        case 'setHideFromRecentsEnabled':
          hideRecents = (call.arguments as Map)['value'] as bool;
          return true;
        case 'getConvertedNotificationVibrationEnabled':
          return vibrationEnabled;
        case 'setConvertedNotificationVibrationEnabled':
          if (failSave) return false;
          vibrationEnabled = (call.arguments as Map)['value'] as bool;
          writes.add(vibrationEnabled);
          return true;
        case 'getConversionLogMaxBytes':
          return 5 * 1024 * 1024;
        case 'getAppLanguageTag':
          return 'system';
        default:
          return false;
      }
    });
  });

  tearDown(() {
    binding.defaultBinaryMessenger.setMockMethodCallHandler(channel, null);
  });

  LbListItemData vibrationItem(WidgetTester tester) => tester
      .widgetList<LbListComponent>(find.byType(LbListComponent))
      .expand((list) => list.items)
      .singleWhere((item) => item.title == 'Converted notification vibration');

  testWidgets(
    'vibration defaults off and can be enabled and disabled independently',
    (tester) async {
      await tester.pumpWidget(
        const MaterialApp(home: SettingsAppConfigScreen()),
      );
      await tester.pumpAndSettle();
      expect(vibrationItem(tester).toggleValue, isFalse);
      vibrationItem(tester).onToggle!(true);
      await tester.pumpAndSettle();
      expect(vibrationItem(tester).toggleValue, isTrue);
      vibrationItem(tester).onToggle!(false);
      await tester.pumpAndSettle();
      expect(vibrationItem(tester).toggleValue, isFalse);
      expect(writes, [true, false]);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets('saved vibration setting is loaded when reopening settings', (
    tester,
  ) async {
    vibrationEnabled = true;
    await tester.pumpWidget(const MaterialApp(home: SettingsAppConfigScreen()));
    await tester.pumpAndSettle();
    expect(vibrationItem(tester).toggleValue, isTrue);
    expect(writes, isEmpty);
  });
  testWidgets(
    'hide from recent apps defaults off and saved state is restored',
    (tester) async {
      await tester.pumpWidget(
        const MaterialApp(home: SettingsAppConfigScreen()),
      );
      await tester.pumpAndSettle();
      LbListItemData item() => tester
          .widgetList<LbListComponent>(find.byType(LbListComponent))
          .expand((list) => list.items)
          .singleWhere((item) => item.title == 'Hide from recent apps');
      expect(item().toggleValue, isFalse);
      item().onToggle!(true);
      await tester.pumpAndSettle();
      expect(hideRecents, isTrue);
      expect(item().toggleValue, isTrue);
      await tester.pumpWidget(const SizedBox());
      await tester.pumpWidget(
        const MaterialApp(home: SettingsAppConfigScreen()),
      );
      await tester.pumpAndSettle();
      expect(item().toggleValue, isTrue);
    },
  );
  testWidgets(
    'vibration save failure keeps previous state and shows app toast',
    (tester) async {
      failSave = true;
      await tester.pumpWidget(
        const MaterialApp(home: SettingsAppConfigScreen()),
      );
      await tester.pumpAndSettle();
      vibrationItem(tester).onToggle!(true);
      await tester.pumpAndSettle();
      expect(vibrationItem(tester).toggleValue, isFalse);
      expect(vibrationItem(tester).enabled, isTrue);
      expect(vibrationEnabled, isFalse);
      expect(
        find.text('Could not save the setting. Please try again.'),
        findsOneWidget,
      );
      await tester.pump(const Duration(seconds: 3));
      await tester.pumpAndSettle();
    },
  );
}
