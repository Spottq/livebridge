import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:livebridge/screens/redesign/rules_network_connections_screen.dart';
import 'package:livebridge/widgets/redesign/lb_list_component.dart';

void main() {
  const channel = MethodChannel('livebridge/platform');
  final binding = TestWidgetsFlutterBinding.ensureInitialized();
  bool hide = false;
  bool saveSucceeds = true;
  final writes = <bool>[];
  setUp(() {
    hide = false;
    saveSucceeds = true;
    writes.clear();
    binding.defaultBinaryMessenger.setMockMethodCallHandler(channel, (
      call,
    ) async {
      switch (call.method) {
        case 'getNetworkSpeedMinThresholdBytesPerSecond':
          return 0;
        case 'getNetworkSpeedNotificationColorArgb':
          return 0xFF0F766E;
        case 'getNetworkSpeedDisplayMode':
          return 'total';
        case 'getNetworkSpeedHideWhenLocked':
          return hide;
        case 'setNetworkSpeedHideWhenLocked':
          final value = (call.arguments as Map)['value'] as bool;
          writes.add(value);
          if (saveSucceeds) hide = value;
          return saveSucceeds;
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
      .singleWhere((item) => item.title == 'Hide speed when locked');
  testWidgets('speed hiding defaults off and can be changed independently', (
    tester,
  ) async {
    await tester.pumpWidget(
      const MaterialApp(home: RulesNetworkConnectionsScreen()),
    );
    await tester.pumpAndSettle();
    expect(item(tester).toggleValue, isFalse);
    item(tester).onToggle!(true);
    await tester.pumpAndSettle();
    expect(item(tester).toggleValue, isTrue);
    item(tester).onToggle!(false);
    await tester.pumpAndSettle();
    expect(item(tester).toggleValue, isFalse);
    expect(writes, [true, false]);
  });
  testWidgets('failed speed setting save preserves the previous value', (
    tester,
  ) async {
    saveSucceeds = false;
    await tester.pumpWidget(
      const MaterialApp(home: RulesNetworkConnectionsScreen()),
    );
    await tester.pumpAndSettle();
    item(tester).onToggle!(true);
    await tester.pumpAndSettle();
    expect(item(tester).toggleValue, isFalse);
    expect(hide, isFalse);
    expect(item(tester).enabled, isTrue);
    expect(
      find.text('Could not save the setting. Please try again.'),
      findsOneWidget,
    );
  });
}
