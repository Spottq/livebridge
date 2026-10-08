import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:livebridge/l10n/app_locale_controller.dart';
import 'package:livebridge/l10n/app_strings.dart';
import 'package:livebridge/screens/redesign/settings_app_config_screen.dart';
import 'package:livebridge/widgets/redesign/lb_list_component.dart';

void main() {
  final binding = TestWidgetsFlutterBinding.ensureInitialized();
  const channel = MethodChannel('livebridge/platform');
  String storedLanguage = 'system';
  final calls = <String>[];
  setUp(() {
    storedLanguage = 'system';
    calls.clear();
    appLocaleOverrideNotifier.value = null;
    binding.defaultBinaryMessenger.setMockMethodCallHandler(channel, (
      call,
    ) async {
      calls.add(call.method);
      switch (call.method) {
        case 'getAppLanguageTag':
          return storedLanguage;
        case 'setAppLanguageTag':
          storedLanguage = (call.arguments as Map)['value'] as String;
          return true;
        case 'getConversionLogMaxBytes':
          return 5 * 1024 * 1024;
        default:
          return false;
      }
    });
  });
  tearDown(() {
    binding.defaultBinaryMessenger.setMockMethodCallHandler(channel, null);
    appLocaleOverrideNotifier.value = null;
  });

  test('Spanish and German locales and manual choices are supported', () {
    expect(
      supportedAppLocales(),
      containsAll([const Locale('es'), const Locale('de')]),
    );
    expect(localeForAppLanguageId('ES'), const Locale('es'));
    expect(localeForAppLanguageId('de'), const Locale('de'));
    expect(appLanguageOptionForId('es').label, 'Español');
    expect(appLanguageOptionForId('de').label, 'Deutsch');
    expect(localeForAppLanguageId('system'), isNull);
  });

  test(
    'regional locales translate placeholders without altering their values',
    () {
      final es = AppStrings(locale: const Locale('es', 'MX'));
      final de = AppStrings(locale: const Locale('de', 'AT'));
      expect(
        es.updateProfileVersionSubtitle('2.1.4', '2.1.5'),
        '2.1.4 -> 2.1.5 | pulsa para ver',
      );
      expect(de.conversionLogFrom('Example'), 'von Example');
      expect(es.conversionLogAt('12:30'), 'a las 12:30');
      for (final strings in [es, de]) {
        expect(strings.filterTemplateHelp, contains('{title}'));
        expect(strings.filterTemplateHelp, contains('{text}'));
        expect(strings.filterTemplateHelp, contains('{app}'));
      }
      expect(es.dictionaryWordField('taxi_words'), 'Palabras de taxi');
      expect(de.dictionaryWordField('taxi_words'), 'Taxi-Wörter');
    },
  );

  for (final language in ['es', 'de']) {
    testWidgets(
      'language picker applies and persists $language without changing parser dictionaries',
      (tester) async {
        tester.view.physicalSize = const Size(320, 568);
        tester.view.devicePixelRatio = 1;
        addTearDown(tester.view.resetPhysicalSize);
        addTearDown(tester.view.resetDevicePixelRatio);
        await tester.pumpWidget(
          ValueListenableBuilder<Locale?>(
            valueListenable: appLocaleOverrideNotifier,
            builder: (context, locale, _) => MaterialApp(
              locale: locale,
              supportedLocales: supportedAppLocales(),
              localizationsDelegates: const [
                GlobalMaterialLocalizations.delegate,
                GlobalWidgetsLocalizations.delegate,
                GlobalCupertinoLocalizations.delegate,
              ],
              home: const SettingsAppConfigScreen(),
            ),
          ),
        );
        await tester.pumpAndSettle();
        final languageRow = tester
            .widgetList<LbListComponent>(find.byType(LbListComponent))
            .expand((list) => list.items)
            .singleWhere((item) => item.title == 'App language');
        languageRow.onTap!();
        await tester.pumpAndSettle();
        await tester.tap(find.text(language == 'es' ? 'Español' : 'Deutsch'));
        await tester.pumpAndSettle();
        expect(storedLanguage, language);
        expect(appLocaleOverrideNotifier.value, Locale(language));
        expect(
          find.text(
            language == 'es' ? 'Idioma de la aplicación' : 'App-Sprache',
          ),
          findsOneWidget,
        );
        expect(tester.takeException(), isNull);
        expect(
          calls.any(
            (call) =>
                call.startsWith('setParser') ||
                call.startsWith('setDictionary'),
          ),
          isFalse,
        );
        appLocaleOverrideNotifier.value = null;
        await loadAppLocalePreference();
        expect(appLocaleOverrideNotifier.value, Locale(language));
        await setAppLocalePreference('system');
        expect(storedLanguage, 'system');
        expect(appLocaleOverrideNotifier.value, isNull);
        await tester.pumpAndSettle();
        expect(find.text('App language'), findsOneWidget);
      },
    );
  }
}
