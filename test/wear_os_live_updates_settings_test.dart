import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:livebridge/screens/redesign/settings_app_config_screen.dart';
import 'package:livebridge/widgets/redesign/lb_list_component.dart';

void main() {
  const channel = MethodChannel('livebridge/platform');
  final binding = TestWidgetsFlutterBinding.ensureInitialized();
  bool available = false;
  bool enabled = false;
  bool saveSucceeds = true;
  final writes = <bool>[];
  setUp(() {
    available = false;
    enabled = false;
    saveSucceeds = true;
    writes.clear();
    binding.defaultBinaryMessenger.setMockMethodCallHandler(channel, (
      call,
    ) async {
      switch (call.method) {
        case 'isWearOsLiveUpdatesAvailable':
          return available;
        case 'getWearOsLiveUpdatesEnabled':
          return enabled;
        case 'setWearOsLiveUpdatesEnabled':
          final value = (call.arguments as Map)['value'] as bool;
          writes.add(value);
          if (saveSucceeds) enabled = value;
          return saveSucceeds;
        case 'getAppLanguageTag':
          return 'system';
        case 'getConversionLogMaxBytes':
          return 5 * 1024 * 1024;
        default:
          return false;
      }
    });
  });
  tearDown(
    () =>
        binding.defaultBinaryMessenger.setMockMethodCallHandler(channel, null),
  );
  LbListItemData item(WidgetTester tester) => tester
      .widgetList<LbListComponent>(find.byType(LbListComponent))
      .expand((list) => list.items)
      .singleWhere((item) => item.title == 'Live Updates on Wear OS');
  Future<void> open(WidgetTester tester) async {
    await tester.pumpWidget(const MaterialApp(home: SettingsAppConfigScreen()));
    await tester.pumpAndSettle();
  }

  testWidgets(
    'older Android disables toggle even with an imported enabled preference',
    (tester) async {
      enabled = true;
      await open(tester);
      expect(item(tester).enabled, isFalse);
      expect(item(tester).toggleValue, isFalse);
      expect(item(tester).subtitle, 'Requires Android 17');
      expect(item(tester).onToggle, isNull);
      expect(item(tester).onTap, isNull);
      expect(writes, isEmpty);
    },
  );
  testWidgets('Android 17 can enable disable and reload preference', (
    tester,
  ) async {
    available = true;
    await open(tester);
    expect(item(tester).enabled, isTrue);
    expect(item(tester).toggleValue, isFalse);
    item(tester).onToggle!(true);
    await tester.pumpAndSettle();
    expect(item(tester).toggleValue, isTrue);
    await tester.pumpWidget(const SizedBox());
    await open(tester);
    expect(item(tester).toggleValue, isTrue);
    item(tester).onToggle!(false);
    await tester.pumpAndSettle();
    expect(item(tester).toggleValue, isFalse);
    expect(writes, [true, false]);
  });
  testWidgets('failed save preserves preference and re-enables toggle', (
    tester,
  ) async {
    available = true;
    saveSucceeds = false;
    await open(tester);
    item(tester).onToggle!(true);
    await tester.pumpAndSettle();
    expect(item(tester).toggleValue, isFalse);
    expect(item(tester).enabled, isTrue);
    expect(enabled, isFalse);
    expect(
      find.text('Could not save the setting. Please try again.'),
      findsOneWidget,
    );
    await tester.pump(const Duration(seconds: 3));
    await tester.pumpAndSettle();
  });
}
