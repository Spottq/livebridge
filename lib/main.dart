import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_localizations/flutter_localizations.dart';

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
  runApp(const LiveBridgeApp());
}

class LiveBridgeApp extends StatelessWidget {
  const LiveBridgeApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'LiveBridge',
      debugShowCheckedModeBanner: false,
      supportedLocales: const <Locale>[
        Locale('en'),
        Locale('ru'),
        Locale('tr'),
        Locale('pt', 'BR'),
        Locale.fromSubtags(languageCode: 'zh', scriptCode: 'Hans'),
        Locale.fromSubtags(languageCode: 'zh', scriptCode: 'Hant'),
      ],
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
  }
}
