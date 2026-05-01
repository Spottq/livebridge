import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_localizations/flutter_localizations.dart';

import 'l10n/app_locale_controller.dart';
import 'screens/redesign/home_redesign_screen.dart';
import 'theme/livebridge_tokens.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await SystemChrome.setPreferredOrientations(const <DeviceOrientation>[
    DeviceOrientation.portraitUp,
  ]);
  final Brightness platformBrightness =
      WidgetsBinding.instance.platformDispatcher.platformBrightness;
  SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
  SystemChrome.setSystemUIOverlayStyle(
    LbAppTheme.overlayStyle(platformBrightness),
  );
  await loadAppLocalePreference();
  runApp(const LiveBridgeApp());
}

class LiveBridgeApp extends StatelessWidget {
  const LiveBridgeApp({super.key});

  @override
  Widget build(BuildContext context) {
    return ValueListenableBuilder<Locale?>(
      valueListenable: appLocaleOverrideNotifier,
      builder: (BuildContext context, Locale? locale, _) {
        return MaterialApp(
          title: 'LiveBridge',
          debugShowCheckedModeBanner: false,
          locale: locale,
          supportedLocales: supportedAppLocales(),
          localizationsDelegates: const <LocalizationsDelegate<dynamic>>[
            GlobalMaterialLocalizations.delegate,
            GlobalWidgetsLocalizations.delegate,
            GlobalCupertinoLocalizations.delegate,
          ],
          theme: LbAppTheme.light(),
          darkTheme: LbAppTheme.dark(),
          themeMode: ThemeMode.system,
          home: const HomeRedesignScreen(),
        );
      },
    );
  }
}
