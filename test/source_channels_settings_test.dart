import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:livebridge/screens/redesign/settings_source_channels_screen.dart';
import 'package:livebridge/widgets/redesign/lb_list_component.dart';
import 'package:livebridge/widgets/redesign/lb_search_pill.dart';

void main() {
  const channel = MethodChannel('livebridge/platform');
  final binding = TestWidgetsFlutterBinding.ensureInitialized();
  final writes = <Map>[];
  bool failSave = false;
  bool largeCatalog = false;
  Completer<bool>? pendingSave;

  setUp(() {
    failSave = false;
    largeCatalog = false;
    pendingSave = null;
    writes.clear();
    binding.defaultBinaryMessenger.setMockMethodCallHandler(channel, (
      call,
    ) async {
      if (call.method == 'getSourceChannels') {
        return jsonEncode(
          largeCatalog
              ? List.generate(
                  80,
                  (index) => {
                    'packageName': 'app.large',
                    'channelId': '$index',
                    'name': 'Channel $index',
                    'appLabel': 'Large',
                    'enabled': true,
                  },
                )
              : [
                  {
                    'packageName': 'app.first',
                    'channelId': 'ads',
                    'name': 'Offers',
                    'appLabel': 'First',
                    'enabled': true,
                  },
                  {
                    'packageName': 'app.first',
                    'channelId': 'chat',
                    'name': 'Messages',
                    'appLabel': 'First',
                    'enabled': true,
                  },
                  {
                    'packageName': 'app.second',
                    'channelId': 'ads',
                    'name': 'Offers',
                    'appLabel': 'Second',
                    'enabled': false,
                  },
                ],
        );
      }
      if (call.method == 'setSourceChannelEnabled') {
        writes.add(call.arguments as Map);
        if (failSave) throw PlatformException(code: 'failed');
        if (pendingSave != null) return pendingSave!.future;
        return true;
      }
      return false;
    });
  });

  tearDown(
    () =>
        binding.defaultBinaryMessenger.setMockMethodCallHandler(channel, null),
  );

  List<LbListItemData> rows(WidgetTester tester) => tester
      .widgetList<LbListComponent>(find.byType(LbListComponent))
      .expand((list) => list.items)
      .toList();

  testWidgets(
    'channel exclusion targets one app and channel without changing siblings',
    (tester) async {
      await tester.pumpWidget(
        const MaterialApp(home: SettingsSourceChannelsScreen()),
      );
      await tester.pumpAndSettle();
      rows(tester).first.onToggle!(false);
      await tester.pumpAndSettle();
      expect(writes.single, {
        'packageName': 'app.first',
        'channelId': 'ads',
        'enabled': false,
      });
      expect(rows(tester).map((w) => w.toggleValue).toList(), [
        false,
        true,
        false,
      ]);
    },
  );

  testWidgets('failed save keeps channel enabled and displays an error', (
    tester,
  ) async {
    failSave = true;
    await tester.pumpWidget(
      const MaterialApp(home: SettingsSourceChannelsScreen()),
    );
    await tester.pumpAndSettle();
    rows(tester).first.onToggle!(false);
    await tester.pumpAndSettle();
    expect(rows(tester).first.toggleValue, isTrue);
    expect(
      find.text('Could not load or save channels. Please try again.'),
      findsOneWidget,
    );
    expect(tester.takeException(), isNull);
    await tester.pump(const Duration(seconds: 3));
    await tester.pumpAndSettle();
  });
  testWidgets('channels keep technical IDs out of the list and searchable', (
    tester,
  ) async {
    await tester.pumpWidget(
      const MaterialApp(home: SettingsSourceChannelsScreen()),
    );
    await tester.pumpAndSettle();
    expect(find.text('app.first'), findsNothing);
    expect(find.text('ads'), findsNothing);
    expect(find.byType(SwitchListTile), findsNothing);
    tester.widget<LbSearchPill>(find.byType(LbSearchPill)).controller.text =
        'app.second';
    await tester.pumpAndSettle();
    expect(rows(tester).length, 1);
    expect(rows(tester).single.subtitle, 'Second');
    tester.widget<LbSearchPill>(find.byType(LbSearchPill)).controller.text =
        'unmatched';
    await tester.pumpAndSettle();
    expect(find.text('No results'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
  testWidgets(
    'long channel list loads incrementally and last channel can be saved',
    (tester) async {
      largeCatalog = true;
      await tester.pumpWidget(
        const MaterialApp(home: SettingsSourceChannelsScreen()),
      );
      await tester.pumpAndSettle();
      expect(rows(tester).length, 32);
      await tester.scrollUntilVisible(
        find.text('Channel 79'),
        500,
        scrollable: find.byType(Scrollable).first,
      );
      await tester.pumpAndSettle();
      rows(tester).last.onToggle!(false);
      await tester.pumpAndSettle();
      expect(writes.single, {
        'packageName': 'app.large',
        'channelId': '79',
        'enabled': false,
      });
      expect(rows(tester).first.toggleValue, isTrue);
      expect(rows(tester).last.toggleValue, isFalse);
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets('pending channel save cannot be submitted twice', (tester) async {
    pendingSave = Completer<bool>();
    await tester.pumpWidget(
      const MaterialApp(home: SettingsSourceChannelsScreen()),
    );
    await tester.pumpAndSettle();
    rows(tester).first.onToggle!(false);
    rows(tester).first.onToggle!(true);
    await tester.pump();
    expect(rows(tester).first.enabled, isFalse);
    expect(writes.length, 1);
    pendingSave!.complete(true);
    await tester.pumpAndSettle();
    expect(rows(tester).first.toggleValue, isFalse);
    expect(rows(tester).first.enabled, isTrue);
  });
}
