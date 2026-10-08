import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:livebridge/models/app_models.dart';
import 'package:livebridge/screens/redesign/settings_permissions_screen.dart';
import 'package:livebridge/widgets/redesign/lb_app_presentation_editor_sheet.dart';
import 'package:livebridge/widgets/redesign/lb_list_component.dart';
import 'package:livebridge/widgets/redesign/lb_modal_bottom_sheet.dart';

void main() {
  const MethodChannel channel = MethodChannel('livebridge/platform');
  final TestWidgetsFlutterBinding binding =
      TestWidgetsFlutterBinding.ensureInitialized();
  String manufacturer = 'vivo';
  String brand = 'vivo';
  String promotedStatus = 'denied';
  bool promotionSettingsAvailable = true;

  setUp(() {
    manufacturer = 'vivo';
    brand = 'vivo';
    promotedStatus = 'denied';
    promotionSettingsAvailable = true;
    binding.defaultBinaryMessenger.setMockMethodCallHandler(channel, (
      MethodCall call,
    ) async {
      switch (call.method) {
        case 'getAppPresentationOverrides':
          return '{}';
        case 'getInstalledApps':
          return <Map<String, Object>>[
            <String, Object>{
              'packageName': 'com.example.app',
              'label': 'Example app',
              'isSystem': false,
            },
          ];
        case 'getDeviceInfo':
          return <String, String>{'manufacturer': manufacturer, 'brand': brand};
        case 'getPromotedNotificationAccess':
          return {
            'status': promotedStatus,
            'apiAvailable': true,
            'settingsAvailable': promotionSettingsAvailable,
          };
        case 'canPostPromotedNotifications':
          return false;
        default:
          return true;
      }
    });
  });

  tearDown(() {
    binding.defaultBinaryMessenger.setMockMethodCallHandler(channel, null);
  });

  for (final String vendor in <String>['vivo', 'iQOO']) {
    testWidgets('$vendor does not show an unavailable promotion prerequisite', (
      WidgetTester tester,
    ) async {
      manufacturer = vendor;
      brand = vendor;
      await tester.pumpWidget(
        const MaterialApp(home: SettingsPermissionsScreen()),
      );
      await tester.pumpAndSettle();
      final LbListComponent list = tester.widget(find.byType(LbListComponent));
      expect(list.items, hasLength(2));
      expect(
        list.items.every((LbListItemData i) => i.trailingIcon == null),
        isTrue,
      );
    });
  }

  testWidgets('other vendors retain the missing promotion warning', (
    WidgetTester tester,
  ) async {
    manufacturer = 'asus';
    brand = 'asus';
    await tester.pumpWidget(
      const MaterialApp(home: SettingsPermissionsScreen()),
    );
    await tester.pumpAndSettle();
    final LbListComponent list = tester.widget(find.byType(LbListComponent));
    expect(list.items, hasLength(3));
    expect(list.items.last.trailingIcon, isNotNull);
  });

  for (final status in ['unavailable', 'unknown']) {
    testWidgets(
      'ASUS $status shows information without a false permission warning',
      (tester) async {
        manufacturer = 'asus';
        brand = 'asus';
        promotedStatus = status;
        promotionSettingsAvailable = false;
        await tester.pumpWidget(
          const MaterialApp(home: SettingsPermissionsScreen()),
        );
        await tester.pumpAndSettle();
        final list = tester.widget<LbListComponent>(
          find.byType(LbListComponent),
        );
        expect(list.items, hasLength(3));
        final promotion = list.items.last;
        expect(promotion.trailingIcon, isNull);
        expect(promotion.onTap, isNull);
        expect(promotion.showChevron, isFalse);
        expect(promotion.subtitle, isNotNull);
        expect(promotion.description, contains('not an overlay permission'));
      },
    );
  }
  testWidgets(
    'granted promotion access remains granted without a settings page',
    (tester) async {
      manufacturer = 'asus';
      brand = 'asus';
      promotedStatus = 'granted';
      promotionSettingsAvailable = false;
      await tester.pumpWidget(
        const MaterialApp(home: SettingsPermissionsScreen()),
      );
      await tester.pumpAndSettle();
      final item = tester
          .widget<LbListComponent>(find.byType(LbListComponent))
          .items
          .last;
      expect(item.trailingIcon, isNull);
      expect(item.subtitle, isNull);
    },
  );

  test('vivo detection uses vendor identity rather than a model substring', () {
    const DeviceInfo device = DeviceInfo(
      manufacturer: 'Example',
      brand: 'Example',
      marketName: 'vivo edition',
      model: 'vivo edition',
    );
    expect(device.shouldHideLiveUpdatesPromotion, isFalse);
  });

  for (final bool perApp in <bool>[false, true]) {
    testWidgets(
      'current ${perApp ? 'per-app' : 'default'} editor scrolls to the last option',
      (WidgetTester tester) async {
        tester.view.physicalSize = const Size(320, 360);
        tester.view.devicePixelRatio = 1;
        addTearDown(tester.view.resetPhysicalSize);
        addTearDown(tester.view.resetDevicePixelRatio);
        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: Builder(
                builder: (BuildContext context) => TextButton(
                  onPressed: () => showLbModalBottomSheet<void>(
                    context: context,
                    builder: (_) => LbAppPresentationEditorSheet(
                      title: perApp ? 'Example app' : 'Defaults',
                      app: perApp
                          ? const InstalledApp(
                              packageName: 'com.example.app',
                              label: 'Example app',
                            )
                          : null,
                      value: const AppPresentationOverride(),
                      onChanged: (_) {},
                    ),
                  ),
                  child: const Text('Open'),
                ),
              ),
            ),
          ),
        );
        await tester.tap(find.text('Open'));
        await tester.pumpAndSettle();
        expect(tester.takeException(), isNull);
        final Finder scroll = find.byType(SingleChildScrollView);
        final ScrollableState state = tester.state(
          find.descendant(of: scroll, matching: find.byType(Scrollable)),
        );
        expect(state.position.maxScrollExtent, greaterThan(0));
        await tester.drag(scroll, const Offset(0, -600));
        await tester.pumpAndSettle();
        expect(
          state.position.pixels,
          closeTo(state.position.maxScrollExtent, 1),
        );
        expect(find.byType(LbListComponent).last.hitTestable(), findsOneWidget);
      },
    );
  }
}
