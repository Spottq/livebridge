import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:livebridge/screens/redesign/settings_app_config_screen.dart';
import 'package:livebridge/widgets/redesign/lb_list_component.dart';

void main() {
  const channel = MethodChannel('livebridge/platform');
  final binding = TestWidgetsFlutterBinding.ensureInitialized();
  bool hideRecents = false;

  setUp(() {
    hideRecents = false;
    binding.defaultBinaryMessenger.setMockMethodCallHandler(channel, (
      call,
    ) async {
      switch (call.method) {
        case 'getHideFromRecentsEnabled':
          return hideRecents;
        case 'setHideFromRecentsEnabled':
          hideRecents = (call.arguments as Map)['value'] as bool;
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

  Iterable<String> itemTitles(WidgetTester tester) => tester
      .widgetList<LbListComponent>(find.byType(LbListComponent))
      .expand((list) => list.items)
      .map((item) => item.title);

  testWidgets('converted notification sound and vibration options are gone', (
    tester,
  ) async {
    await tester.pumpWidget(const MaterialApp(home: SettingsAppConfigScreen()));
    await tester.pumpAndSettle();
    final titles = itemTitles(tester).toList();
    expect(titles, contains('Hide from recent apps'));
    expect(titles, isNot(contains('Converted notification sound')));
    expect(titles, isNot(contains('Converted notification vibration')));
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
}
