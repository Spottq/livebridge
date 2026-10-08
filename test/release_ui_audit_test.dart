import 'dart:convert';
import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:livebridge/l10n/app_locale_controller.dart';
import 'package:livebridge/screens/redesign/dictionary_word_editor_screen.dart';
import 'package:livebridge/screens/redesign/settings_source_channels_screen.dart';
import 'package:livebridge/screens/redesign/settings_text_filters_screen.dart';
import 'package:livebridge/theme/livebridge_tokens.dart';
import 'package:livebridge/widgets/redesign/lb_detail_screen.dart';
import 'package:livebridge/widgets/redesign/lb_editor_field.dart';

void main() {
  final binding = TestWidgetsFlutterBinding.ensureInitialized();
  const channel = MethodChannel('livebridge/platform');
  final screens = <String, Widget>{
    'channels': const SettingsSourceChannelsScreen(),
    'filters': const SettingsTextFiltersScreen(),
    'dictionary': const DictionaryWordEditorScreen(),
  };
  setUpAll(() async {
    final font = FontLoader('SfProRounded')
      ..addFont(rootBundle.load('assets/fonts/SF-Compact-Rounded-Medium.otf'));
    await font.load();
    final icons = FontLoader('packages/ming_cute_icons/MingCute')
      ..addFont(
        rootBundle.load('packages/ming_cute_icons/lib/fonts/MingCute.ttf'),
      );
    await icons.load();
  });
  setUp(() {
    binding.defaultBinaryMessenger.setMockMethodCallHandler(channel, (
      call,
    ) async {
      switch (call.method) {
        case 'getSourceChannels':
          return jsonEncode([
            {
              'packageName': 'com.exteragram.messenger',
              'channelId': 'private_123',
              'name': 'Default',
              'appLabel': 'extraGram',
              'enabled': true,
            },
            {
              'packageName': 'com.google.android.googlequicksearchbox',
              'channelId': '74',
              'name': 'News',
              'appLabel': 'Google',
              'enabled': true,
            },
            {
              'packageName': 'com.google.android.youtube',
              'channelId': '1',
              'name': 'Suggested',
              'appLabel': 'YouTube',
              'enabled': false,
            },
            {
              'packageName': 'com.zhiliaoapp.musically',
              'channelId': 'im_push_associated_4',
              'name': 'Direct messages',
              'appLabel': 'TikTok',
              'enabled': true,
            },
          ]);
        case 'getInstalledApps':
          return <Map<String, dynamic>>[];
        case 'getNotificationTextFilters':
          return '{}';
        case 'getDictionaryWordAdditions':
          return '{}';
        default:
          return false;
      }
    });
  });
  tearDown(
    () =>
        binding.defaultBinaryMessenger.setMockMethodCallHandler(channel, null),
  );
  for (final screen in screens.entries) {
    for (final brightness in Brightness.values) {
      testWidgets('${screen.key} uses app design in ${brightness.name}', (
        tester,
      ) async {
        tester.view.physicalSize = const Size(390, 844);
        tester.view.devicePixelRatio = 1;
        addTearDown(tester.view.resetPhysicalSize);
        addTearDown(tester.view.resetDevicePixelRatio);
        await tester.pumpWidget(
          MaterialApp(
            theme: brightness == Brightness.dark
                ? LbAppTheme.dark()
                : LbAppTheme.light(),
            home: screen.value,
          ),
        );
        await tester.pumpAndSettle();
        expect(find.byType(LbDetailScreen), findsOneWidget);
        expect(find.byType(AppBar), findsNothing);
        expect(find.byType(SwitchListTile), findsNothing);
        expect(find.byType(FilledButton), findsNothing);
        expect(tester.takeException(), isNull);
        if (const bool.fromEnvironment('GENERATE_RELEASE_PREVIEWS')) {
          await expectLater(
            find.byType(LbDetailScreen),
            matchesGoldenFile(
              '/tmp/livebridge-release-previews/${screen.key}-${brightness.name}.png',
            ),
          );
        }
      });
    }
    testWidgets('${screen.key} supports narrow screens and large German text', (
      tester,
    ) async {
      tester.view.physicalSize = const Size(320, 568);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      await tester.pumpWidget(
        MaterialApp(
          locale: const Locale('de'),
          supportedLocales: supportedAppLocales(),
          localizationsDelegates: const [
            GlobalMaterialLocalizations.delegate,
            GlobalWidgetsLocalizations.delegate,
            GlobalCupertinoLocalizations.delegate,
          ],
          theme: LbAppTheme.light(),
          builder: (context, child) => MediaQuery(
            data: MediaQuery.of(
              context,
            ).copyWith(textScaler: const TextScaler.linear(1.6)),
            child: child!,
          ),
          home: screen.value,
        ),
      );
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
    });
  }
  testWidgets('editor save remains reachable while keyboard is open', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(390, 844);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    addTearDown(tester.view.resetViewInsets);
    await tester.pumpWidget(
      MaterialApp(
        theme: LbAppTheme.dark(),
        home: const SettingsTextFiltersScreen(),
      ),
    );
    await tester.pumpAndSettle();
    tester.view.viewInsets = const FakeViewPadding(bottom: 300);
    await tester.pumpAndSettle();
    final button = find.descendant(
      of: find.byType(LbEditorSaveBar),
      matching: find.byType(InkWell),
    );
    expect(tester.getBottomRight(button).dy, lessThanOrEqualTo(544));
    expect(tester.takeException(), isNull);
  });
  test('secondary text remains readable in both themes', () {
    for (final palette in [LbPalette.light, LbPalette.dark]) {
      final foreground = palette.textSecondary.computeLuminance();
      for (final background in [palette.background, palette.surface]) {
        final luminance = background.computeLuminance();
        final ratio =
            (math.max(foreground, luminance) + 0.05) /
            (math.min(foreground, luminance) + 0.05);
        expect(ratio, greaterThanOrEqualTo(4.5));
      }
    }
  });
}
