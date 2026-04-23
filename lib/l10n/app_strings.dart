import 'package:flutter/material.dart';

class AppStrings {
  AppStrings({required this.locale});
  final Locale locale;

  bool get isRu => locale.languageCode.toLowerCase().startsWith('ru');
  bool get isTr => locale.languageCode.toLowerCase().startsWith('tr');
  bool get isPtBr {
    final String languageCode = locale.languageCode.toLowerCase();
    final String countryCode = locale.countryCode?.toLowerCase() ?? '';
    return languageCode == 'pt' && countryCode == 'br';
  }

  bool get isZhHans {
    final String languageCode = locale.languageCode.toLowerCase();
    if (languageCode != 'zh') return false;
    final String scriptCode = locale.scriptCode?.toLowerCase() ?? '';
    final String countryCode = locale.countryCode?.toLowerCase() ?? '';
    return scriptCode == 'hans' || countryCode == 'cn' || countryCode == 'sg';
  }

  bool get isZhHant {
    final String languageCode = locale.languageCode.toLowerCase();
    if (languageCode != 'zh') return false;
    final String scriptCode = locale.scriptCode?.toLowerCase() ?? '';
    final String countryCode = locale.countryCode?.toLowerCase() ?? '';
    return scriptCode == 'hant' ||
        countryCode == 'tw' ||
        countryCode == 'hk' ||
        countryCode == 'mo';
  }

  String tr({
    required String en,
    required String ru,
    String? tr,
    String? ptBr,
    String? zhHans,
    String? zhHant,
  }) {
    if (isRu) return ru;
    if (isTr) return tr ?? en;
    if (isPtBr) return ptBr ?? _ptBrTranslations[en] ?? en;
    if (isZhHant) return zhHant ?? zhHans ?? en;
    if (isZhHans) return zhHans ?? zhHant ?? en;
    return en;
  }

  static AppStrings of(BuildContext context) {
    return AppStrings(locale: Localizations.localeOf(context));
  }

  String get refresh => tr(
    en: 'Refresh',
    ru: 'РћР±РЅРѕРІРёС‚СЊ',
    tr: 'Yenile',
    zhHans: 'е€·ж–°',
    zhHant: 'й‡Ќж–°ж•ґзђ†',
  );

  String get permissionGranted => tr(
    en: 'Notification permission granted.',
    ru: 'Р Р°Р·СЂРµС€РµРЅРёРµ РЅР° СѓРІРµРґРѕРјР»РµРЅРёСЏ РІС‹РґР°РЅРѕ.',
    tr: 'Bildirim izni verildi.',
    zhHans: 'йЂљзџҐжќѓй™ђе·ІжЋ€дє€гЂ‚',
    zhHant: 'йЂљзџҐж¬Љй™ђе·ІжЋ€дє€гЂ‚',
  );

  String get permissionDenied => tr(
    en: 'Notification permission was not granted.',
    ru: 'Р Р°Р·СЂРµС€РµРЅРёРµ РЅР° СѓРІРµРґРѕРјР»РµРЅРёСЏ РЅРµ РІС‹РґР°РЅРѕ.',
    tr: 'Bildirim izni verilmedi.',
    zhHans: 'жњЄжЋ€дє€йЂљзџҐжќѓй™ђгЂ‚',
    zhHant: 'жњЄжЋ€дє€йЂљзџҐж¬Љй™ђгЂ‚',
  );

  String get listenerUnavailable => tr(
    en: 'Unable to open Listener settings on this device.',
    ru: 'РќРµ СѓРґР°Р»РѕСЃСЊ РѕС‚РєСЂС‹С‚СЊ РЅР°СЃС‚СЂРѕР№РєРё Listener.',
    tr: 'Bu cihazda Listener ayarlarД± aГ§Д±lamД±yor.',
    zhHans: 'ж­¤и®ѕе¤‡ж— жі•ж‰“ејЂз›‘еђ¬е™Ёи®ѕзЅ®гЂ‚',
    zhHant: 'ж­¤иЈќзЅ®з„Ўжі•й–‹е•џз›ЈиЃЅе™ЁиЁ­е®љгЂ‚',
  );

  String get notificationsUnavailable => tr(
    en: 'Unable to open app notification settings.',
    ru: 'РќРµ СѓРґР°Р»РѕСЃСЊ РѕС‚РєСЂС‹С‚СЊ РЅР°СЃС‚СЂРѕР№РєРё СѓРІРµРґРѕРјР»РµРЅРёР№.',
    tr: 'Uygulama bildirim ayarlarД± aГ§Д±lamД±yor.',
    zhHans: 'ж— жі•ж‰“ејЂеє”з”ЁйЂљзџҐи®ѕзЅ®гЂ‚',
    zhHant: 'з„Ўжі•й–‹е•џж‡‰з”ЁйЂљзџҐиЁ­е®љгЂ‚',
  );

  String get liveUpdatesUnavailable => tr(
    en: 'Unable to open Live Updates settings on this device.',
    ru: 'РќРµ СѓРґР°Р»РѕСЃСЊ РѕС‚РєСЂС‹С‚СЊ РЅР°СЃС‚СЂРѕР№РєРё Live Updates.',
    tr: 'Bu cihazda Live Updates ayarlarД± aГ§Д±lamД±yor.',
    zhHans: 'ж­¤и®ѕе¤‡ж— жі•ж‰“ејЂ Live Updates и®ѕзЅ®гЂ‚',
    zhHant: 'ж­¤иЈќзЅ®з„Ўжі•й–‹е•џ Live Updates иЁ­е®љгЂ‚',
  );

  String get githubOpenFailed => tr(
    en: 'Unable to open GitHub link.',
    ru: 'РќРµ СѓРґР°Р»РѕСЃСЊ РѕС‚РєСЂС‹С‚СЊ СЃСЃС‹Р»РєСѓ GitHub.',
    tr: 'GitHub baДџlantД±sД± aГ§Д±lamД±yor.',
    zhHans: 'ж— жі•ж‰“ејЂ GitHub й“ѕжЋҐгЂ‚',
    zhHant: 'з„Ўжі•й–‹е•џ GitHub йЂЈзµђгЂ‚',
  );

  String get linkOpenFailed => tr(
    en: 'Unable to open link.',
    ru: 'РќРµ СѓРґР°Р»РѕСЃСЊ РѕС‚РєСЂС‹С‚СЊ СЃСЃС‹Р»РєСѓ.',
    tr: 'BaДџlantД± aГ§Д±lamД±yor.',
    zhHans: 'ж— жі•ж‰“ејЂй“ѕжЋҐгЂ‚',
    zhHant: 'з„Ўжі•й–‹е•џйЂЈзµђгЂ‚',
  );

  String get updateCheckFailed => tr(
    en: 'Unable to check updates. Try disabling VPN.',
    ru: 'РќРµ СѓРґР°Р»РѕСЃСЊ РїСЂРѕРІРµСЂРёС‚СЊ РѕР±РЅРѕРІР»РµРЅРёСЏ. РџРѕРїСЂРѕР±СѓР№С‚Рµ РѕС‚РєР»СЋС‡РёС‚СЊ VPN.',
    tr: 'GГјncellemeler denetlenemiyor. VPN\'i kapatmayД± deneyin.',
    zhHans: 'ж— жі•жЈЂжџҐж›ґж–°гЂ‚иЇ·е°ќиЇ•е…ій—­ VPNгЂ‚',
    zhHant: 'з„Ўжі•жЄўжџҐж›ґж–°гЂ‚и«‹е—и©¦й—њй–‰ VPNгЂ‚',
  );

  String get dictionaryEmpty => tr(
    en: 'Dictionary is empty or invalid.',
    ru: 'РЎР»РѕРІР°СЂСЊ РїСѓСЃС‚РѕР№ РёР»Рё РїРѕРІСЂРµР¶РґРµРЅ.',
    tr: 'SГ¶zlГјk boЕџ veya geГ§ersiz.',
    zhHans: 'иЇЌе…ёдёєз©єж€–ж— ж•€гЂ‚',
    zhHant: 'е­—е…ёз‚єз©єж€–з„Ўж•€гЂ‚',
  );

  String get dictionaryUpdateDone => tr(
    en: 'Dictionary updated from GitHub.',
    ru: 'РЎР»РѕРІР°СЂСЊ РѕР±РЅРѕРІР»РµРЅ РёР· GitHub.',
    tr: 'SГ¶zlГјk GitHub\'dan gГјncellendi.',
    zhHans: 'иЇЌе…ёе·Ід»Ћ GitHub ж›ґж–°гЂ‚',
    zhHant: 'е­—е…ёе·Іеѕћ GitHub ж›ґж–°гЂ‚',
  );

  String get dictionaryInvalid => tr(
    en: 'Invalid dictionary JSON.',
    ru: 'РќРµРІР°Р»РёРґРЅС‹Р№ JSON СЃР»РѕРІР°СЂСЏ.',
    tr: 'GeГ§ersiz sГ¶zlГјk JSON\'u.',
    zhHans: 'иЇЌе…ё JSON ж— ж•€гЂ‚',
    zhHant: 'е­—е…ё JSON з„Ўж•€гЂ‚',
  );

  String get dictionaryUpdateFailed => tr(
    en: 'Failed to update dictionary from GitHub.',
    ru: 'РќРµ СѓРґР°Р»РѕСЃСЊ РѕР±РЅРѕРІРёС‚СЊ СЃР»РѕРІР°СЂСЊ РёР· GitHub.',
    tr: 'SГ¶zlГјk GitHub\'dan gГјncellenemedi.',
    zhHans: 'д»Ћ GitHub ж›ґж–°иЇЌе…ёе¤±иґҐгЂ‚',
    zhHant: 'еѕћ GitHub ж›ґж–°е­—е…ёе¤±ж•—гЂ‚',
  );

  String get dictionaryTitle => tr(
    en: 'Dictionary',
    ru: 'РЎР»РѕРІР°СЂСЊ',
    tr: 'SГ¶zlГјk',
    zhHans: 'иЇЌе…ё',
    zhHant: 'е­—е…ё',
  );

  String get dictionaryManageSubtitle => tr(
    en: 'tap to manage',
    ru: 'РЅР°Р¶РјРёС‚Рµ РґР»СЏ СѓРїСЂР°РІР»РµРЅРёСЏ',
    tr: 'yГ¶netmek iГ§in dokunun',
    zhHans: 'з‚№жЊ‰д»Ґз®Ўзђ†',
    zhHant: 'й»ћжЊ‰д»Ґз®Ўзђ†',
  );

  String get dictionaryLanguagesTitle => tr(
    en: 'Dictionary languages',
    ru: 'РЇР·С‹РєРё СЃР»РѕРІР°СЂСЏ',
    tr: 'SГ¶zlГјk dilleri',
    zhHans: 'иЇЌе…ёиЇ­иЁЂ',
    zhHant: 'е­—е…ёиЄћиЁЂ',
  );

  String get dictionaryLanguagesSubtitle => tr(
    en: 'tap to choose',
    ru: 'РЅР°Р¶РјРёС‚Рµ РґР»СЏ РІС‹Р±РѕСЂР°',
    tr: 'seГ§mek iГ§in dokunun',
    zhHans: 'з‚№жЊ‰д»ҐйЂ‰ж‹©',
    zhHant: 'й»ћжЊ‰д»ҐйЃёж“‡',
  );

  String get dictionaryLanguagesPickerTitle => tr(
    en: 'Select languages for conversion',
    ru: 'Р’С‹Р±РµСЂРёС‚Рµ СЏР·С‹РєРё РґР»СЏ РєРѕРЅРІРµСЂС‚Р°С†РёРё',
    tr: 'DГ¶nГјЕџtГјrme iГ§in dilleri seГ§in',
    zhHans: 'йЂ‰ж‹©з”ЁдєЋиЅ¬жЌўзљ„иЇ­иЁЂ',
    zhHant: 'йЃёж“‡з”Ёж–јиЅ‰жЏ›зљ„иЄћиЁЂ',
  );

  String get dictionaryUpdateAction => tr(
    en: 'Update dictionaries',
    ru: 'РћР±РЅРѕРІРёС‚СЊ СЃР»РѕРІР°СЂРё',
    tr: 'SГ¶zlГјkleri gГјncelle',
    zhHans: 'ж›ґж–°иЇЌе…ё',
    zhHant: 'ж›ґж–°е­—е…ё',
  );

  String get dictionaryEditorTitle => tr(
    en: 'Dictionary editor',
    ru: 'Р РµРґР°РєС‚РѕСЂ СЃР»РѕРІР°СЂСЏ',
    tr: 'SГ¶zlГјk dГјzenleyici',
    zhHans: 'иЇЌе…ёзј–иѕ‘е™Ё',
    zhHant: 'е­—е…ёз·ЁијЇе™Ё',
  );

  String get dictionaryComingSoon => tr(
    en: '(coming soon)',
    ru: '(СЃРєРѕСЂРѕ)',
    tr: '(yakД±nda)',
    zhHans: 'пј€еЌіе°†жЋЁе‡єпј‰',
    zhHant: 'пј€еЌіе°‡жЋЁе‡єпј‰',
  );

  String get navHome =>
      tr(en: 'Home', ru: 'Р”РѕРјРѕР№', tr: 'Ana sayfa', zhHans: 'дё»йЎµ', zhHant: 'й¦–й Ѓ');

  String get navRules => tr(
    en: 'Rules',
    ru: 'РџСЂР°РІРёР»Р°',
    tr: 'Kurallar',
    zhHans: 'и§„е€™',
    zhHant: 'и¦Џе‰‡',
  );

  String get navSettings => tr(
    en: 'Settings',
    ru: 'РќР°СЃС‚СЂРѕР№РєРё',
    tr: 'Ayarlar',
    zhHans: 'и®ѕзЅ®',
    zhHant: 'иЁ­е®љ',
  );

  String get redesignRulesTitle => tr(
    en: 'Rules',
    ru: 'РџСЂР°РІРёР»Р°',
    tr: 'Kurallar',
    zhHans: 'и§„е€™',
    zhHant: 'и¦Џе‰‡',
  );

  String get appConfigTitle => tr(
    en: 'App config',
    ru: 'РќР°СЃС‚СЂРѕР№РєРё РїСЂРёР»РѕР¶РµРЅРёСЏ',
    tr: 'Uygulama yapД±landД±rmasД±',
    zhHans: 'еє”з”Ёй…ЌзЅ®',
    zhHant: 'ж‡‰з”Ёй…ЌзЅ®',
  );

  String get brandSpecificTitle => tr(
    en: 'Brand-specific',
    ru: 'Brand-specific',
    tr: 'Markaya Г¶zel',
    zhHans: 'е“Ѓз‰Њз‰№е®љ',
    zhHant: 'е“Ѓз‰Њз‰№е®љ',
  );

  String get appUpdatesTitle => tr(
    en: 'App updates',
    ru: 'РћР±РЅРѕРІР»РµРЅРёСЏ РїСЂРёР»РѕР¶РµРЅРёСЏ',
    tr: 'Uygulama gГјncellemeleri',
    zhHans: 'еє”з”Ёж›ґж–°',
    zhHant: 'ж‡‰з”Ёж›ґж–°',
  );

  String get statusRunning => tr(
    en: 'LiveBridge is running',
    ru: 'LiveBridge Р·Р°РїСѓС‰РµРЅ',
    tr: 'LiveBridge Г§alД±ЕџД±yor',
    zhHans: 'LiveBridge ж­ЈењЁиїђиЎЊ',
    zhHant: 'LiveBridge ж­ЈењЁеџ·иЎЊ',
  );

  String get statusDisabled => tr(
    en: 'LiveBridge is disabled',
    ru: 'LiveBridge РІС‹РєР»СЋС‡РµРЅ',
    tr: 'LiveBridge devre dД±ЕџД±',
    zhHans: 'LiveBridge е·Іе…ій—­',
    zhHant: 'LiveBridge е·Ій—њй–‰',
  );

  String get statusByPrefix =>
      tr(en: 'by ', ru: 'by ', tr: 'by ', zhHans: 'by ', zhHant: 'by ');

  String get discussTitle => tr(
    en: 'Discuss',
    ru: 'Discuss',
    tr: 'TartД±Еџ',
    zhHans: 'и®Ёи®є',
    zhHant: 'иЁЋи«–',
  );

  String get discussSubtitle => tr(
    en: 'telegram topics',
    ru: 'telegram topics',
    tr: 'telegram konularД±',
    zhHans: 'telegram иЇќйў',
    zhHant: 'telegram и©±йЎЊ',
  );

  String get rulesModeAllApps => tr(
    en: 'all apps',
    ru: 'РІСЃРµ РїСЂРёР»РѕР¶РµРЅРёСЏ',
    tr: 'tГјm uygulamalar',
    zhHans: 'ж‰Ђжњ‰еє”з”Ё',
    zhHant: 'ж‰Ђжњ‰ж‡‰з”ЁзЁ‹ејЏ',
  );

  String get rulesModeOnlySelected => tr(
    en: 'only selected',
    ru: 'С‚РѕР»СЊРєРѕ РІС‹Р±СЂР°РЅРЅС‹Рµ',
    tr: 'yalnД±zca seГ§ilenler',
    zhHans: 'д»…е·ІйЂ‰ж‹©',
    zhHant: 'еѓ…е·ІйЃёеЏ–',
  );

  String get rulesModeExcludeSelected => tr(
    en: 'exclude selected',
    ru: 'РёСЃРєР»СЋС‡Р°СЏ РІС‹Р±СЂР°РЅРЅС‹Рµ',
    tr: 'seГ§ilenleri hariГ§ tut',
    zhHans: 'жЋ’й™¤е·ІйЂ‰ж‹©',
    zhHant: 'жЋ’й™¤е·ІйЃёеЏ–',
  );

  String get permissionCheckRequired => tr(
    en: 'check required',
    ru: 'С‚СЂРµР±СѓРµС‚СЃСЏ РїСЂРѕРІРµСЂРєР°',
    tr: 'kontrol gerekli',
    zhHans: 'йњЂи¦ЃжЈЂжџҐ',
    zhHant: 'йњЂи¦ЃжЄўжџҐ',
  );

  String get permissionsAllSet => tr(
    en: 'all set',
    ru: 'РІСЃС‘ С…РѕСЂРѕС€Рѕ',
    tr: 'hazД±r',
    zhHans: 'е·Іе°±з»Є',
    zhHant: 'е·Іе°±з·’',
  );

  String get versionTapToUpdate => tr(
    en: 'tap to update',
    ru: 'РЅР°Р¶РјРёС‚Рµ РґР»СЏ РѕР±РЅРѕРІР»РµРЅРёСЏ',
    tr: 'gГјncellemek iГ§in dokunun',
    zhHans: 'з‚№жЊ‰ж›ґж–°',
    zhHant: 'й»ћжЊ‰ж›ґж–°',
  );

  String get versionLatestVersion => tr(
    en: 'latest version',
    ru: 'РїРѕСЃР»РµРґРЅСЏСЏ РІРµСЂСЃРёСЏ',
    tr: 'son sГјrГјm',
    zhHans: 'жњЂж–°з‰€жњ¬',
    zhHant: 'жњЂж–°з‰€жњ¬',
  );

  String get recentConversions => tr(
    en: 'Recent conversions',
    ru: 'РџРѕСЃР»РµРґРЅРёРµ РєРѕРЅРІРµСЂС‚Р°С†РёРё',
    tr: 'Son dГ¶nГјЕџtГјrmeler',
    zhHans: 'жњЂиї‘иЅ¬жЌў',
    zhHant: 'жњЂиї‘иЅ‰жЏ›',
  );

  String get noConversionsYet => tr(
    en: 'no conversions yet',
    ru: 'РєРѕРЅРІРµСЂС‚Р°С†РёР№ РїРѕРєР° РЅРµС‚',
    tr: 'henГјz dГ¶nГјЕџtГјrme yok',
    zhHans: 'жљ‚ж— иЅ¬жЌў',
    zhHant: 'жљ«з„ЎиЅ‰жЏ›',
  );

  String get conversionLogDisabled => tr(
    en: 'conversion log is disabled',
    ru: 'Р»РѕРі РєРѕРЅРІРµСЂС‚Р°С†РёР№ РІС‹РєР»СЋС‡РµРЅ',
    tr: 'dГ¶nГјЕџtГјrme gГјnlГјДџГј kapalД±',
    zhHans: 'иЅ¬жЌўж—Ґеї—е·Іе…ій—­',
    zhHant: 'иЅ‰жЏ›иЁйЊ„е·Ій—њй–‰',
  );

  String get enable => tr(
    en: 'enable',
    ru: 'РІРєР»СЋС‡РёС‚СЊ',
    tr: 'etkinleЕџtir',
    zhHans: 'еђЇз”Ё',
    zhHant: 'е•џз”Ё',
  );

  String get payloadCopied => tr(
    en: 'Payload copied',
    ru: 'Payload СЃРєРѕРїРёСЂРѕРІР°РЅ',
    tr: 'Payload kopyalandД±',
    zhHans: 'Payload е·Іе¤Ќе€¶',
    zhHant: 'Payload е·Іи¤‡иЈЅ',
  );

  String get progressTitle => tr(
    en: 'Progress',
    ru: 'РџСЂРѕРіСЂРµСЃСЃ',
    tr: 'Д°lerleme',
    zhHans: 'иї›еє¦',
    zhHant: 'йЂІеє¦',
  );

  String get nativeProgressTitle => tr(
    en: 'Native progress',
    ru: 'РќР°С‚РёРІРЅС‹Р№ РїСЂРѕРіСЂРµСЃСЃ',
    tr: 'Yerel ilerleme',
    zhHans: 'еЋџз”џиї›еє¦',
    zhHant: 'еЋџз”џйЂІеє¦',
  );

  String get otpCodesTitle => tr(
    en: 'OTP codes',
    ru: 'OTP-РєРѕРґС‹',
    tr: 'OTP kodlarД±',
    zhHans: 'OTP йЄЊиЇЃз Ѓ',
    zhHant: 'OTP й©—и­‰зўј',
  );

  String get autoCopyCodeTitle => tr(
    en: 'Auto-copy code',
    ru: 'РђРІС‚РѕРєРѕРїРёСЂРѕРІР°РЅРёРµ РєРѕРґР°',
    tr: 'Kodu otomatik kopyala',
    zhHans: 'и‡ЄеЉЁе¤Ќе€¶йЄЊиЇЃз Ѓ',
    zhHant: 'и‡Єе‹•и¤‡иЈЅй©—и­‰зўј',
  );

  String get smartConversionTitle => tr(
    en: 'Smart conversion',
    ru: 'РЈРјРЅР°СЏ РєРѕРЅРІРµСЂС‚Р°С†РёСЏ',
    tr: 'AkД±llД± dГ¶nГјЕџtГјrme',
    zhHans: 'ж™єиѓЅиЅ¬жЌў',
    zhHant: 'ж™єж…§иЅ‰жЏ›',
  );

  String get taxiTitle =>
      tr(en: 'Taxi', ru: 'РўР°РєСЃРё', tr: 'Taksi', zhHans: 'ж‰“иЅ¦', zhHant: 'еЏ«и»Љ');

  String get deliveriesTitle => tr(
    en: 'Deliveries',
    ru: 'Р”РѕСЃС‚Р°РІРєРё',
    tr: 'Teslimatlar',
    zhHans: 'е¤–еЌ–',
    zhHant: 'е¤–йЂЃ',
  );

  String get removeOriginalMessageTitle => tr(
    en: 'Remove original message',
    ru: 'РЈРґР°Р»СЏС‚СЊ РёСЃС…РѕРґРЅРѕРµ СѓРІРµРґРѕРјР»РµРЅРёРµ',
    tr: 'Orijinal bildirimi kaldД±r',
    zhHans: 'з§»й™¤еЋџе§‹йЂљзџҐ',
    zhHant: 'з§»й™¤еЋџе§‹йЂљзџҐ',
  );

  String get experimentalSuffix => tr(
    en: '(exp)',
    ru: '(exp)',
    tr: '(deneysel)',
    zhHans: 'пј€е®ћйЄЊпј‰',
    zhHant: 'пј€еЇ¦й©—пј‰',
  );

  String get allAppsTitle => tr(
    en: 'All apps',
    ru: 'Р’СЃРµ РїСЂРёР»РѕР¶РµРЅРёСЏ',
    tr: 'TГјm uygulamalar',
    zhHans: 'ж‰Ђжњ‰еє”з”Ё',
    zhHant: 'ж‰Ђжњ‰ж‡‰з”ЁзЁ‹ејЏ',
  );

  String get onlySelectedTitle => tr(
    en: 'Only selected',
    ru: 'РўРѕР»СЊРєРѕ РІС‹Р±СЂР°РЅРЅС‹Рµ',
    tr: 'YalnД±zca seГ§ilenler',
    zhHans: 'д»…е·ІйЂ‰ж‹©',
    zhHant: 'еѓ…е·ІйЃёеЏ–',
  );

  String get excludeSelectedTitle => tr(
    en: 'Exclude selected',
    ru: 'РСЃРєР»СЋС‡РёС‚СЊ РІС‹Р±СЂР°РЅРЅС‹Рµ',
    tr: 'SeГ§ilenleri hariГ§ tut',
    zhHans: 'жЋ’й™¤е·ІйЂ‰ж‹©',
    zhHant: 'жЋ’й™¤е·ІйЃёеЏ–',
  );

  String get conversionModeTitle => tr(
    en: 'Conversion mode',
    ru: 'Р РµР¶РёРј РєРѕРЅРІРµСЂС‚Р°С†РёРё',
    tr: 'DГ¶nГјЕџtГјrme modu',
    zhHans: 'иЅ¬жЌўжЁЎејЏ',
    zhHant: 'иЅ‰жЏ›жЁЎејЏ',
  );

  String get selectedAppsTitle => tr(
    en: 'Selected apps',
    ru: 'РџСЂРёР»РѕР¶РµРЅРёСЏ',
    tr: 'SeГ§ili uygulamalar',
    zhHans: 'е·ІйЂ‰ж‹©еє”з”Ё',
    zhHant: 'е·ІйЃёеЏ–ж‡‰з”ЁзЁ‹ејЏ',
  );

  String get showSystem => tr(
    en: 'show system',
    ru: 'РїРѕРєР°Р·Р°С‚СЊ СЃРёСЃС‚РµРјРЅС‹Рµ',
    tr: 'sistem uygulamalarД±nД± gГ¶ster',
    zhHans: 'жѕз¤єзі»з»џ',
    zhHant: 'йЎЇз¤єзі»зµ±',
  );

  String get hideSystem => tr(
    en: 'hide system',
    ru: 'СЃРєСЂС‹С‚СЊ СЃРёСЃС‚РµРјРЅС‹Рµ',
    tr: 'sistem uygulamalarД±nД± gizle',
    zhHans: 'йљђи—Џзі»з»џ',
    zhHant: 'йљ±и—Џзі»зµ±',
  );

  String get networkConnectionsTitle => tr(
    en: 'Network & Connections',
    ru: 'РЎРµС‚СЊ Рё РїРѕРґРєР»СЋС‡РµРЅРёСЏ',
    tr: 'AДџ ve BaДџlantД±lar',
    zhHans: 'зЅ‘з»њдёЋиїћжЋҐ',
    zhHant: 'з¶Іи·Їи€‡йЂЈз·љ',
  );

  String get vpnsTitle =>
      tr(en: 'VPNs', ru: 'VPN', tr: 'VPN\'ler', zhHans: 'VPN', zhHant: 'VPN');

  String get externalDevicesTitle => tr(
    en: 'External devices',
    ru: 'Р’РЅРµС€РЅРёРµ СѓСЃС‚СЂРѕР№СЃС‚РІР°',
    tr: 'Harici cihazlar',
    zhHans: 'е¤–жЋҐи®ѕе¤‡',
    zhHant: 'е¤–жЋҐиЈќзЅ®',
  );

  String get ignoreDebuggingDevicesTitle => tr(
    en: 'Ignore debugging devices',
    ru: 'РРіРЅРѕСЂРёСЂРѕРІР°С‚СЊ РѕС‚Р»Р°РґРѕС‡РЅС‹Рµ СѓСЃС‚СЂРѕР№СЃС‚РІР°',
    tr: 'Hata ayД±klama cihazlarД±nД± yok say',
    zhHans: 'еїЅз•Ґи°ѓиЇ•и®ѕе¤‡',
    zhHant: 'еїЅз•ҐеЃµйЊЇиЈќзЅ®',
  );

  String get networkSpeedThresholdRedesignTitle => tr(
    en: 'Network speed threshold',
    ru: 'РџРѕСЂРѕРі СЃРєРѕСЂРѕСЃС‚Рё СЃРµС‚Рё',
    tr: 'AДџ hД±zД± eЕџiДџi',
    zhHans: 'зЅ‘йЂџй€еЂј',
    zhHant: 'з¶ІйЂџй–ЂжЄ»',
  );

  String get miscellaneousTitle => tr(
    en: 'Miscellaneous',
    ru: 'Р Р°Р·РЅРѕРµ',
    tr: 'DiДџer',
    zhHans: 'е…¶д»–',
    zhHant: 'е…¶д»–',
  );

  String get navigationMapsTitle => tr(
    en: 'Navigation (maps)',
    ru: 'РќР°РІРёРіР°С†РёСЏ (РєР°СЂС‚С‹)',
    tr: 'Navigasyon (haritalar)',
    zhHans: 'еЇји€Єпј€ењ°е›ѕпј‰',
    zhHant: 'е°Ћи€Єпј€ењ°ењ–пј‰',
  );

  String get mediaPlaybackRedesignTitle => tr(
    en: 'Media playback',
    ru: 'РњРµРґРёР°',
    tr: 'Medya oynatma',
    zhHans: 'еЄ’дЅ“ж’­ж”ѕ',
    zhHant: 'еЄ’й«”ж’­ж”ѕ',
  );

  String get weatherBroadcastsTitle => tr(
    en: 'Weather broadcasts',
    ru: 'РџРѕРіРѕРґРЅС‹Рµ СѓРІРµРґРѕРјР»РµРЅРёСЏ',
    tr: 'Hava durumu bildirimleri',
    zhHans: 'е¤©ж°”ж’­жЉҐ',
    zhHant: 'е¤©ж°Јж’­е ±',
  );

  String get bypassTitle =>
      tr(en: 'Bypass', ru: 'Bypass', tr: 'Bypass', zhHans: 'з»•иї‡', zhHant: 'з№ћйЃЋ');

  String get perAppSettingsTitle => tr(
    en: 'Per-app settings',
    ru: 'РќР°СЃС‚СЂРѕР№РєРё РїСЂРёР»РѕР¶РµРЅРёР№',
    tr: 'Uygulama bazlД± ayarlar',
    zhHans: 'жЊ‰еє”з”Ёи®ѕзЅ®',
    zhHant: 'еђ„ж‡‰з”ЁиЁ­е®љ',
  );

  String get defaultsTitle => tr(
    en: 'Defaults',
    ru: 'РџРѕ СѓРјРѕР»С‡Р°РЅРёСЋ',
    tr: 'VarsayД±lanlar',
    zhHans: 'й»и®¤еЂј',
    zhHant: 'й ђиЁ­еЂј',
  );

  String get defaultsSubtitle => tr(
    en: 'tap to change default behavior',
    ru: 'РЅР°Р¶РјРёС‚Рµ, С‡С‚РѕР±С‹ РёР·РјРµРЅРёС‚СЊ РїРѕРІРµРґРµРЅРёРµ',
    tr: 'varsayД±lan davranД±ЕџД± deДџiЕџtirmek iГ§in dokunun',
    zhHans: 'з‚№жЊ‰ж›ґж”№й»и®¤иЎЊдёє',
    zhHant: 'й»ћжЊ‰и®Љж›ґй ђиЁ­иЎЊз‚є',
  );

  String get appsListTitle => tr(
    en: 'Apps list',
    ru: 'РЎРїРёСЃРѕРє РїСЂРёР»РѕР¶РµРЅРёР№',
    tr: 'Uygulama listesi',
    zhHans: 'еє”з”Ёе€—иЎЁ',
    zhHant: 'ж‡‰з”ЁзЁ‹ејЏжё…е–®',
  );

  String get exportLabel => tr(
    en: 'Export',
    ru: 'Р­РєСЃРїРѕСЂС‚',
    tr: 'DД±Еџa aktar',
    zhHans: 'еЇје‡є',
    zhHant: 'еЊЇе‡є',
  );

  String get importLabel => tr(
    en: 'Import',
    ru: 'РРјРїРѕСЂС‚',
    tr: 'Д°Г§e aktar',
    zhHans: 'еЇје…Ґ',
    zhHant: 'еЊЇе…Ґ',
  );

  String get titleSourceTitle => tr(
    en: 'Title source',
    ru: 'РСЃС‚РѕС‡РЅРёРє Р·Р°РіРѕР»РѕРІРєР°',
    tr: 'BaЕџlД±k kaynaДџД±',
    zhHans: 'ж ‡йўжќҐжєђ',
    zhHant: 'жЁ™йЎЊдѕ†жєђ',
  );

  String get contentSourceTitle => tr(
    en: 'Content source',
    ru: 'РСЃС‚РѕС‡РЅРёРє РєРѕРЅС‚РµРЅС‚Р°',
    tr: 'Д°Г§erik kaynaДџД±',
    zhHans: 'е†…е®№жќҐжєђ',
    zhHant: 'е…§е®№дѕ†жєђ',
  );

  String get notificationTitleOption => tr(
    en: 'Notification title',
    ru: 'Р—Р°РіРѕР»РѕРІРѕРє СѓРІРµРґРѕРјР»РµРЅРёСЏ',
    tr: 'Bildirim baЕџlД±ДџД±',
    zhHans: 'йЂљзџҐж ‡йў',
    zhHant: 'йЂљзџҐжЁ™йЎЊ',
  );

  String get appTitleOption => tr(
    en: 'App title',
    ru: 'РќР°Р·РІР°РЅРёРµ РїСЂРёР»РѕР¶РµРЅРёСЏ',
    tr: 'Uygulama baЕџlД±ДџД±',
    zhHans: 'еє”з”Ёж ‡йў',
    zhHant: 'ж‡‰з”ЁжЁ™йЎЊ',
  );

  String get notificationTextOption => tr(
    en: 'Notification text',
    ru: 'РўРµРєСЃС‚ СѓРІРµРґРѕРјР»РµРЅРёСЏ',
    tr: 'Bildirim metni',
    zhHans: 'йЂљзџҐж–‡жњ¬',
    zhHant: 'йЂљзџҐж–‡е­—',
  );

  String get appUpdateNewVersionTitle => tr(
    en: 'New version available',
    ru: 'Р”РѕСЃС‚СѓРїРЅР° РЅРѕРІР°СЏ РІРµСЂСЃРёСЏ',
    tr: 'Yeni sГјrГјm mevcut',
    zhHans: 'жњ‰ж–°з‰€жњ¬еЏЇз”Ё',
    zhHant: 'жњ‰ж–°з‰€жњ¬еЏЇз”Ё',
  );

  String get appUpdateCheckingTitle => tr(
    en: 'Checking for updates',
    ru: 'РџСЂРѕРІРµСЂСЏРµРј РѕР±РЅРѕРІР»РµРЅРёСЏ',
    tr: 'GГјncellemeler denetleniyor',
    zhHans: 'ж­ЈењЁжЈЂжџҐж›ґж–°',
    zhHant: 'ж­ЈењЁжЄўжџҐж›ґж–°',
  );

  String get appUpdateAllSetTitle => tr(
    en: 'YouвЂ™re all set',
    ru: 'Р’СЃС‘ С…РѕСЂРѕС€Рѕ',
    tr: 'Her Еџey hazД±r',
    zhHans: 'е·ІжЇжњЂж–°',
    zhHant: 'е·ІжЇжњЂж–°',
  );

  String get appUpdateDownloadsSubtitle => tr(
    en: 'tap to go to downloads',
    ru: 'РїРµСЂРµР№С‚Рё Рє Р·Р°РіСЂСѓР·РєРµ',
    tr: 'indirmelere gitmek iГ§in dokunun',
    zhHans: 'з‚№жЊ‰е‰ЌеѕЂдё‹иЅЅ',
    zhHant: 'й»ћжЊ‰е‰ЌеѕЂдё‹иј‰',
  );

  String get appUpdatePleaseWaitSubtitle => tr(
    en: 'please wait a moment',
    ru: 'РїРѕРґРѕР¶РґРёС‚Рµ РЅРµРјРЅРѕРіРѕ',
    tr: 'lГјtfen biraz bekleyin',
    zhHans: 'иЇ·зЁЌз­‰',
    zhHant: 'и«‹зЁЌеЂ™',
  );

  String get appUpdateLatestSubtitle => tr(
    en: 'latest version already',
    ru: 'СѓСЃС‚Р°РЅРѕРІР»РµРЅР° РїРѕСЃР»РµРґРЅСЏСЏ РІРµСЂСЃРёСЏ',
    tr: 'zaten son sГјrГјm',
    zhHans: 'е·Із»ЏжЇжњЂж–°з‰€жњ¬',
    zhHant: 'е·ІжЇжњЂж–°з‰€жњ¬',
  );

  String get visitProjectPageTitle => tr(
    en: 'Visit project page',
    ru: 'РћС‚РєСЂС‹С‚СЊ СЃС‚СЂР°РЅРёС†Сѓ РїСЂРѕРµРєС‚Р°',
    tr: 'Proje sayfasД±nД± aГ§',
    zhHans: 'и®їй—®йЎ№з›®йЎµйќў',
    zhHant: 'е‰ЌеѕЂе°€жЎ€й Ѓйќў',
  );

  String get visitGithubTitle => tr(
    en: 'Visit GitHub',
    ru: 'РћС‚РєСЂС‹С‚СЊ GitHub',
    tr: 'GitHub\'Д± aГ§',
    zhHans: 'и®їй—® GitHub',
    zhHant: 'е‰ЌеѕЂ GitHub',
  );

  String get updateProfileNewVersionTitle => tr(
    en: 'New version available',
    ru: 'Р”РѕСЃС‚СѓРїРЅР° РЅРѕРІР°СЏ РІРµСЂСЃРёСЏ',
    tr: 'Yeni sГјrГјm mevcut',
    zhHans: 'жњ‰ж–°з‰€жњ¬еЏЇз”Ё',
    zhHant: 'жњ‰ж–°з‰€жњ¬еЏЇз”Ё',
  );

  String updateProfileVersionSubtitle(String current, String latest) => tr(
    en: '$current -> $latest | tap to see',
    ru: '$current -> $latest | РїРѕСЃРјРѕС‚СЂРµС‚СЊ',
    tr: '$current -> $latest | gГ¶rmek iГ§in dokunun',
    ptBr: '$current -> $latest | toque para ver',
    zhHans: '$current -> $latest | з‚№жЊ‰жџҐзњ‹',
    zhHant: '$current -> $latest | й»ћжЊ‰жџҐзњ‹',
  );

  String get updateProfileAvailableSubtitle => tr(
    en: 'update available | tap to see',
    ru: 'РґРѕСЃС‚СѓРїРЅРѕ РѕР±РЅРѕРІР»РµРЅРёРµ | РїРѕСЃРјРѕС‚СЂРµС‚СЊ',
    tr: 'gГјncelleme mevcut | gГ¶rmek iГ§in dokunun',
    zhHans: 'жњ‰еЏЇз”Ёж›ґж–° | з‚№жЊ‰жџҐзњ‹',
    zhHant: 'жњ‰еЏЇз”Ёж›ґж–° | й»ћжЊ‰жџҐзњ‹',
  );

  String get updateProfileOpenSubtitle => tr(
    en: 'tap to open update settings',
    ru: 'РЅР°Р¶РјРёС‚Рµ РґР»СЏ РЅР°СЃС‚СЂРѕР№РєРё',
    tr: 'gГјncelleme ayarlarД±nД± aГ§mak iГ§in dokunun',
    zhHans: 'з‚№жЊ‰ж‰“ејЂж›ґж–°и®ѕзЅ®',
    zhHant: 'й»ћжЊ‰й–‹е•џж›ґж–°иЁ­е®љ',
  );

  String get conversionLogTitle => tr(
    en: 'Conversion log',
    ru: 'Р›РѕРі РєРѕРЅРІРµСЂС‚Р°С†РёР№',
    tr: 'DГ¶nГјЕџtГјrme gГјnlГјДџГј',
    zhHans: 'иЅ¬жЌўж—Ґеї—',
    zhHant: 'иЅ‰жЏ›иЁйЊ„',
  );

  String get logLengthTitle => tr(
    en: 'Log length',
    ru: 'Р Р°Р·РјРµСЂ Р»РѕРіР°',
    tr: 'GГјnlГјk boyutu',
    zhHans: 'ж—Ґеї—е¤§е°Џ',
    zhHant: 'иЁйЊ„е¤§е°Џ',
  );

  String get xiaomiHyperIslandTitle => tr(
    en: 'Xiaomi HyperIsland',
    ru: 'Xiaomi HyperIsland',
    tr: 'Xiaomi HyperIsland',
    zhHans: 'е°Џз±і HyperIsland',
    zhHant: 'е°Џз±і HyperIsland',
  );

  String get lengthTitle =>
      tr(en: 'Length', ru: 'Р”Р»РёРЅР°', tr: 'Uzunluk', zhHans: 'й•їеє¦', zhHant: 'й•·еє¦');

  String get otpDedupTitle => tr(
    en: 'OTP dedup',
    ru: 'OTP dedup',
    tr: 'OTP tekilleЕџtirme',
    zhHans: 'OTP еЋ»й‡Ќ',
    zhHant: 'OTP еЋ»й‡Ќ',
  );

  String get smartConversionDedupTitle => tr(
    en: 'Smart conversion dedup',
    ru: 'Smart conversion dedup',
    tr: 'AkД±llД± dГ¶nГјЕџtГјrme tekilleЕџtirme',
    zhHans: 'ж™єиѓЅиЅ¬жЌўеЋ»й‡Ќ',
    zhHant: 'ж™єж…§иЅ‰жЏ›еЋ»й‡Ќ',
  );

  String get animatedIslandRedesignTitle => tr(
    en: 'Animated Island',
    ru: 'РђРЅРёРјРёСЂРѕРІР°РЅРЅС‹Р№ РѕСЃС‚СЂРѕРІ',
    tr: 'Animasyonlu ada',
    zhHans: 'еЉЁжЂЃеІ›еЉЁз”»',
    zhHant: 'е‹•ж…‹еі¶е‹•з•«',
  );

  String get updateFrequencyTitle => tr(
    en: 'Update frequency',
    ru: 'Р§Р°СЃС‚РѕС‚Р° РѕР±РЅРѕРІР»РµРЅРёСЏ',
    tr: 'GГјncelleme sД±klД±ДџД±',
    zhHans: 'ж›ґж–°йў‘зЋ‡',
    zhHant: 'ж›ґж–°й »зЋ‡',
  );

  String get copyDebugJsonTitle => tr(
    en: 'Copy debug JSON',
    ru: 'РЎРєРѕРїРёСЂРѕРІР°С‚СЊ debug JSON',
    tr: 'Debug JSON\'unu kopyala',
    zhHans: 'е¤Ќе€¶и°ѓиЇ• JSON',
    zhHant: 'и¤‡иЈЅеЃµйЊЇ JSON',
  );

  String get openGithubPageTitle => tr(
    en: 'Open GitHub page',
    ru: 'РћС‚РєСЂС‹С‚СЊ GitHub',
    tr: 'GitHub sayfasД±nД± aГ§',
    zhHans: 'ж‰“ејЂ GitHub йЎµйќў',
    zhHant: 'й–‹е•џ GitHub й Ѓйќў',
  );

  String get autoCopyDebugJsonTitle => tr(
    en: 'Auto-copy debug JSON',
    ru: 'РђРІС‚РѕРєРѕРїРёСЂРѕРІР°РЅРёРµ debug JSON',
    tr: 'Debug JSON\'unu otomatik kopyala',
    zhHans: 'и‡ЄеЉЁе¤Ќе€¶и°ѓиЇ• JSON',
    zhHant: 'и‡Єе‹•и¤‡иЈЅеЃµйЊЇ JSON',
  );

  String conversionLogFrom(String appLabel) => tr(
    en: 'from $appLabel',
    ru: 'РѕС‚ $appLabel',
    tr: '$appLabel uygulamasД±ndan',
    ptBr: 'de $appLabel',
    zhHans: 'жќҐи‡Є $appLabel',
    zhHant: 'дѕ†и‡Є $appLabel',
  );

  String conversionLogAt(String time) => tr(
    en: 'at $time',
    ru: 'РІ $time',
    tr: time,
    ptBr: 'Г s $time',
    zhHans: time,
    zhHant: time,
  );

  String get conversionLogEntryTitleLabel => tr(
    en: 'Title',
    ru: 'Р—Р°РіРѕР»РѕРІРѕРє',
    tr: 'BaЕџlД±k',
    zhHans: 'ж ‡йў',
    zhHant: 'жЁ™йЎЊ',
  );

  String get payloadJsonTitle => tr(
    en: 'Payload JSON',
    ru: 'Payload JSON',
    tr: 'Payload JSON',
    zhHans: 'Payload JSON',
    zhHant: 'Payload JSON',
  );

  String get loadingApps => tr(
    en: 'loading apps...',
    ru: 'Р·Р°РіСЂСѓР·РєР° РїСЂРёР»РѕР¶РµРЅРёР№...',
    tr: 'uygulamalar yГјkleniyor...',
    zhHans: 'ж­ЈењЁеЉ иЅЅеє”з”Ё...',
    zhHant: 'ж­ЈењЁиј‰е…Ґж‡‰з”ЁзЁ‹ејЏ...',
  );

  String get searchForApps => tr(
    en: 'Search for apps...',
    ru: 'РџРѕРёСЃРє РїСЂРёР»РѕР¶РµРЅРёР№...',
    tr: 'Uygulama ara...',
    zhHans: 'жђњзґўеє”з”Ё...',
    zhHant: 'жђње°‹ж‡‰з”ЁзЁ‹ејЏ...',
  );

  String get heroTitle => 'LiveBridge';
  String get masterToggleLockedHint => isRu
      ? 'РЎРЅР°С‡Р°Р»Р° РІС‹РґР°Р№С‚Рµ РґРѕСЃС‚СѓРї Рє СѓРІРµРґРѕРјР»РµРЅРёСЏРј Рё СЂР°Р·СЂРµС€РµРЅРёРµ РЅР° СѓРІРµРґРѕРјР»РµРЅРёСЏ.'
      : 'Grant notification listener access and notifications permission first.';
  String get githubUrl => 'github.com/appsfolder/livebridge';
  String get githubReleasesUrl => 'github.com/appsfolder/livebridge/releases';
  String get downloadPageUrl => 'appsfolder.github.io/livebridge';
  String get smartExternalDevicesIgnoreDebuggingTitle =>
      isRu ? 'РРіРЅРѕСЂРёСЂРѕРІР°С‚СЊ РѕС‚Р»Р°РґРєСѓ' : 'Ignore debugging';
  String get smartExternalDevicesIgnoreDebuggingSubtitle => isRu
      ? 'РќРµ РїРѕРєР°Р·С‹РІР°С‚СЊ Live РґР»СЏ USB debugging, wireless debugging, ADB Рё РїРѕС…РѕР¶РёС… СЃРёСЃС‚РµРјРЅС‹С… СѓРІРµРґРѕРјР»РµРЅРёР№.'
      : 'Skip Live updates for USB debugging, wireless debugging, ADB, and similar system notifications.';
  String get reportBug => isRu ? 'РЎРѕРѕР±С‰РёС‚СЊ Рѕ Р±Р°РіРµ' : 'Report a bug';
  String get bugReportCopied => isRu
      ? 'Р”РёР°РіРЅРѕСЃС‚РёРєР° СЃРєРѕРїРёСЂРѕРІР°РЅР° РІ Р±СѓС„РµСЂ. Р’СЃС‚Р°РІСЊС‚Рµ РІ issue.'
      : 'Diagnostics copied to clipboard. Paste it into the issue.';
  String get bugReportCopyFailed => isRu
      ? 'РќРµ СѓРґР°Р»РѕСЃСЊ СЃРєРѕРїРёСЂРѕРІР°С‚СЊ РґРёР°РіРЅРѕСЃС‚РёРєСѓ.'
      : 'Failed to copy diagnostics.';
  String get hideWarningBanner => isRu ? 'РЎРєСЂС‹С‚СЊ' : 'Hide';
  String get backgroundWarningTitle =>
      isRu ? 'Р’Р°Р¶РЅРѕ РґР»СЏ С„РѕРЅРѕРІРѕР№ СЂР°Р±РѕС‚С‹' : 'Background mode warning';
  String backgroundWarningBody(String deviceLabel) => isRu
      ? 'Р”Р»СЏ $deviceLabel РЅСѓР¶РЅРѕ РІСЂСѓС‡РЅСѓСЋ СЂР°Р·СЂРµС€РёС‚СЊ Р°РІС‚РѕР·Р°РїСѓСЃРє Рё СЂР°Р±РѕС‚Сѓ Р±РµР· РѕРіСЂР°РЅРёС‡РµРЅРёР№ РІ С„РѕРЅРµ, РёРЅР°С‡Рµ Live Updates РјРѕРіСѓС‚ РЅРµ РїРѕСЏРІР»СЏС‚СЊСЃСЏ РёР»Рё Р·Р°РІРёСЃР°С‚СЊ.'
      : 'On $deviceLabel, allow autostart and unrestricted background activity, otherwise Live Updates may stop appearing or freeze.';
  String get accessTitle => isRu ? 'Р Р°Р·СЂРµС€РµРЅРёСЏ' : 'Permissions';
  String get accessSubtitle => isRu
      ? 'Р‘РµР· СЌС‚РёС… С‚СЂС‘С… СЂР°Р·СЂРµС€РµРЅРёР№ РєРѕРЅРІРµСЂС‚Р°С†РёСЏ Р±СѓРґРµС‚ СЂР°Р±РѕС‚Р°С‚СЊ РЅРµСЃС‚Р°Р±РёР»СЊРЅРѕ.'
      : 'Conversion reliability depends on these three permissions.';
  String get listenerAccess =>
      isRu ? 'Р”РѕСЃС‚СѓРї Рє СѓРІРµРґРѕРјР»РµРЅРёСЏРј' : 'Notification Listener access';
  String get postNotifications =>
      isRu ? 'РћС‚РїСЂР°РІРєР° СѓРІРµРґРѕРјР»РµРЅРёР№' : 'Post notifications permission';
  String get liveUpdatesAccess =>
      isRu ? 'РџСЂРѕРґРІРёР¶РµРЅРёРµ Live Updates' : 'Live Updates promotion';
  String get open => isRu ? 'РћС‚РєСЂС‹С‚СЊ' : 'Open';
  String get request => isRu ? 'Р—Р°РїСЂРѕСЃРёС‚СЊ' : 'Request';
  String get grant => isRu ? 'Р’С‹РґР°С‚СЊ' : 'Grant';
  String get manage => isRu ? 'РЈРїСЂР°РІР»СЏС‚СЊ' : 'Manage';
  String get settingsTitle => isRu ? 'РќР°СЃС‚СЂРѕР№РєРё' : 'Settings';
  String get keepAliveForegroundTitle =>
      isRu ? 'РђР»СЊС‚РµСЂРЅР°С‚РёРІРЅС‹Р№ С„РѕРЅРѕРІС‹Р№ СЂРµР¶РёРј' : 'Alt background mode';
  String get keepAliveForegroundSubtitle => isRu
      ? 'Р”РµСЂР¶РёС‚ foreground-СЃРµСЂРІРёСЃ РґР»СЏ Р±РѕР»РµРµ СЃС‚Р°Р±РёР»СЊРЅРѕР№ СЂР°Р±РѕС‚С‹ РІ С„РѕРЅРµ.'
      : 'Runs a persistent foreground service for better background stability.';
  String get keepAliveForegroundInactiveSubtitle => isRu
      ? 'Р’РєР»СЋС‡РёС‚Рµ LiveBridge, С‡С‚РѕР±С‹ СЂРµР¶РёРј РЅР°С‡Р°Р» СЂР°Р±РѕС‚Р°С‚СЊ.'
      : 'Enable the LiveBridge for this mode to take effect.';
  String get syncDndTitle => isRu ? 'РЎРёРЅС…СЂРѕРЅРёР·РёСЂРѕРІР°С‚СЊ DnD' : 'Sync DnD';
  String get syncDndSubtitle => isRu
      ? 'Р•СЃР»Рё РЅР° СЃРјР°СЂС‚С„РѕРЅРµ РІРєР»СЋС‡РµРЅ СЂРµР¶РёРј РќРµ Р±РµСЃРїРѕРєРѕРёС‚СЊ, СѓРІРµРґРѕРјР»РµРЅРёСЏ LiveBridge РЅРµ РїРѕРєР°Р·С‹РІР°СЋС‚СЃСЏ.'
      : 'When Do Not Disturb is enabled on the phone, LiveBridge notifications are hidden.';
  String get updateChecksTitle =>
      isRu ? 'РџСЂРѕРІРµСЂРєР° РѕР±РЅРѕРІР»РµРЅРёР№' : 'Update checking';
  String get updateChecksSubtitle => isRu
      ? 'РџСЂРѕРІРµСЂСЏС‚СЊ РѕР±РЅРѕРІР»РµРЅРёСЏ РїСЂРё РІС…РѕРґРµ Рё РЅРµ С‡Р°С‰Рµ РѕРґРЅРѕРіРѕ СЂР°Р·Р° РІ 6 С‡Р°СЃРѕРІ.'
      : 'Check updates on app start, and no more than once every 6 hours.';
  String get samsungRemoteParserTitle =>
      isRu ? 'Samsung RemoteViews СЂРµРїР°СЂСЃРµСЂ' : 'Samsung RemoteViews reparser';
  String get samsungRemoteParserSubtitle => isRu
      ? 'РСЃРїРѕР»СЊР·СѓРµС‚ Samsung ongoingActivity extras Рё RemoteViews РґР»СЏ Р±РѕР»РµРµ С‚РѕС‡РЅРѕРіРѕ РїР°СЂСЃРёРЅРіР° СѓРІРµРґРѕРјР»РµРЅРёР№.'
      : 'Uses Samsung ongoingActivity extras and RemoteViews for improved parsing on One UI.';
  String updateAvailableBanner(String version) => isRu
      ? 'Р”РѕСЃС‚СѓРїРЅРѕ РѕР±РЅРѕРІР»РµРЅРёРµ${version.isNotEmpty ? ': $version' : ''}'
      : 'Update available${version.isNotEmpty ? ': $version' : ''}';
  String get samsungUpdateInstallToast => isRu
      ? 'РџРѕР¶Р°Р»СѓР№СЃС‚Р°, СѓСЃС‚Р°РЅРѕРІРёС‚Рµ РІРµСЂСЃРёСЋ РґР»СЏ Samsung, Р° РЅРµ СѓРЅРёРІРµСЂСЃР°Р»СЊРЅСѓСЋ.'
      : 'Please install the Samsung build instead of the universal one.';
  String get experimentalTitle => isRu ? 'Р­РєСЃРїРµСЂРёРјРµРЅС‚Р°Р»СЊРЅРѕРµ' : 'Experimental';
  String get notificationDedupTitle =>
      isRu ? 'РЈРґР°Р»РµРЅРёРµ РґСѓР±Р»РµР№ СѓРІРµРґРѕРјР»РµРЅРёР№' : 'Notification dedup';
  String get notificationDedupSubtitle => isRu
      ? 'РЈР±РёСЂР°РµС‚ РёСЃС…РѕРґРЅС‹Рµ СЃРјР°С…РёРІР°РµРјС‹Рµ СѓРІРµРґРѕРјР»РµРЅРёСЏ, РµСЃР»Рё LiveBridge СѓР¶Рµ РїРѕРєР°Р·Р°Р» СЃРІРѕР№ OTP РёР»Рё СЃС‚Р°С‚СѓСЃ.'
      : 'Dismisses original clearable notifications after LiveBridge mirrors an OTP or status update.';
  String get notificationDedupModeLabel =>
      isRu ? 'Р РµР¶РёРј СѓРґР°Р»РµРЅРёСЏ РґСѓР±Р»РµР№' : 'Dedup mode';
  String get notificationDedupModeOtpStatus =>
      isRu ? 'OTP Рё СЃС‚Р°С‚СѓСЃС‹' : 'OTP and statuses';
  String get notificationDedupModeOtpOnly => isRu ? 'РўРѕР»СЊРєРѕ OTP' : 'OTP only';
  String get notificationDedupStatusesTitle =>
      isRu ? 'РўР°РєР¶Рµ РґР»СЏ СЃС‚Р°С‚СѓСЃРѕРІ' : 'Also dedup statuses';
  String get notificationDedupStatusesSubtitle => isRu
      ? 'Р•СЃР»Рё РІС‹РєР»СЋС‡РµРЅРѕ, СѓРґР°Р»РµРЅРёРµ РґСѓР±Р»РµР№ РїСЂРёРјРµРЅСЏРµС‚СЃСЏ С‚РѕР»СЊРєРѕ Рє OTP-РєРѕРґР°Рј.'
      : 'When disabled, dedup is applied only to OTP notifications.';
  String get animatedIslandTitle =>
      isRu ? 'РђРЅРёРјРёСЂРѕРІР°РЅРЅС‹Р№ РѕСЃС‚СЂРѕРІ' : 'Animated island';
  String get animatedIslandSubtitle => isRu
      ? 'РњРµРЅСЏРµС‚ РєРѕСЂРѕС‚РєРёР№ С‚РµРєСЃС‚ РѕСЃС‚СЂРѕРІР° РєР°Р¶РґС‹Рµ 2-3 СЃРµРєСѓРЅРґС‹ РґР»СЏ smart-СѓРІРµРґРѕРјР»РµРЅРёР№ (РјРѕР¶РµС‚ СЂР°Р±РѕС‚Р°С‚СЊ РЅРµСЃС‚Р°Р±РёР»СЊРЅРѕ).'
      : 'Rotates compact island text every 2-3 seconds for smart notifications (may be unstable).';
  String get hyperBridgeTitle => 'Xiaomi Hyper Island';
  String get hyperBridgeSubtitle => isRu
      ? 'Р”Р»СЏ Xiaomi Hyper OS 3.1 Р“Р»РѕР±Р°Р»СЊРЅРѕР№: РґРѕР±Р°РІР»СЏРµС‚ HyperOS Focus-РїР°СЂР°РјРµС‚СЂС‹ РґР»СЏ РЅР°С‚РёРІРЅРѕРіРѕ РѕСЃС‚СЂРѕРІР°.'
      : 'For Xiaomi Hyper OS 3.1 Global: injects HyperOS Focus parameters for native island behavior.';
  String get aospCuttingTitle => isRu ? 'РћР±СЂРµР·РєР° AOSP' : 'AOSP cutting';
  String get aospCuttingSubtitle => isRu
      ? 'РћР±СЂРµР·Р°С‚СЊ РёРЅС„РѕСЂРјР°С†РёСЋ РІ РѕСЃС‚СЂРѕРІРµ РґРѕ 7 СЃРёРјРІРѕР»РѕРІ РґР»СЏ РєСЂР°СЃРёРІРѕРіРѕ РѕС‚РѕР±СЂР°Р¶РµРЅРёСЏ РІ AOSP-РїСЂРѕС€РёРІРєР°С….'
      : 'Trim island text to 7 characters for cleaner rendering on AOSP ROMs.';
  String get appPresentationSettings =>
      isRu ? 'РџРѕРІРµРґРµРЅРёРµ РїСЂРёР»РѕР¶РµРЅРёР№' : 'Per-app behavior';
  String get appPresentationSubtitle => isRu
      ? 'РќР°СЃС‚СЂРѕР№С‚Рµ РёСЃС‚РѕС‡РЅРёРє С‚РµРєСЃС‚Р° Рё РёРєРѕРЅРєРё РѕС‚РґРµР»СЊРЅРѕ РґР»СЏ СЂР°Р·РЅС‹С… РїСЂРёР»РѕР¶РµРЅРёР№.'
      : 'Choose text and icon behavior separately for different applications.';
  String get appPresentationScreenTitle =>
      isRu ? 'РџРѕРІРµРґРµРЅРёРµ РїСЂРёР»РѕР¶РµРЅРёР№' : 'Per-app behavior';
  String get appPresentationLoadFailed => isRu
      ? 'РќРµ СѓРґР°Р»РѕСЃСЊ Р·Р°РіСЂСѓР·РёС‚СЊ РЅР°СЃС‚СЂРѕР№РєРё РїСЂРёР»РѕР¶РµРЅРёР№.'
      : 'Unable to load per-app settings.';
  String get appPresentationSaveFailed => isRu
      ? 'РќРµ СѓРґР°Р»РѕСЃСЊ СЃРѕС…СЂР°РЅРёС‚СЊ РЅР°СЃС‚СЂРѕР№РєРё РїСЂРёР»РѕР¶РµРЅРёР№.'
      : 'Unable to save per-app settings.';
  String get appPresentationDownloadFailed => isRu
      ? 'РќРµ СѓРґР°Р»РѕСЃСЊ СЃРѕС…СЂР°РЅРёС‚СЊ JSON РЅР°СЃС‚СЂРѕРµРє.'
      : 'Failed to save settings JSON.';
  String get appPresentationSaved =>
      isRu ? 'РќР°СЃС‚СЂРѕР№РєРё СЃРѕС…СЂР°РЅРµРЅС‹ РІ Р—Р°РіСЂСѓР·РєРё.' : 'Settings saved to Downloads.';
  String get appPresentationUploadDone =>
      isRu ? 'РќР°СЃС‚СЂРѕР№РєРё РїСЂРёР»РѕР¶РµРЅРёР№ Р·Р°РіСЂСѓР¶РµРЅС‹.' : 'Per-app settings imported.';
  String get appPresentationUploadFailed => isRu
      ? 'РќРµ СѓРґР°Р»РѕСЃСЊ Р·Р°РіСЂСѓР·РёС‚СЊ JSON РЅР°СЃС‚СЂРѕРµРє.'
      : 'Failed to import settings JSON.';
  String get appPresentationInvalidJson => isRu
      ? 'РќРµРІР°Р»РёРґРЅС‹Р№ JSON РЅР°СЃС‚СЂРѕРµРє РїСЂРёР»РѕР¶РµРЅРёР№.'
      : 'Invalid per-app settings JSON.';
  String get appPresentationDefaultSummary =>
      isRu ? 'РЎС‚Р°РЅРґР°СЂС‚РЅРѕРµ РїРѕРІРµРґРµРЅРёРµ' : 'Default behavior';
  String get appPresentationTextSourceLabel =>
      isRu ? 'РСЃС‚РѕС‡РЅРёРє С‚РµРєСЃС‚Р° РґР»СЏ РѕСЃС‚СЂРѕРІР°' : 'Island text source';
  String get appPresentationIconSourceLabel =>
      isRu ? 'РСЃС‚РѕС‡РЅРёРє РёРєРѕРЅРєРё' : 'Icon source';
  String get appPresentationTextTitle =>
      isRu ? 'Р—Р°РіРѕР»РѕРІРѕРє СѓРІРµРґРѕРјР»РµРЅРёСЏ' : 'Notification title';
  String get appPresentationTextNotification =>
      isRu ? 'РўРµРєСЃС‚ СѓРІРµРґРѕРјР»РµРЅРёСЏ' : 'Notification text';
  String get appPresentationIconNotification =>
      isRu ? 'РРєРѕРЅРєР° СѓРІРµРґРѕРјР»РµРЅРёСЏ' : 'Notification icon';
  String get appPresentationIconApp =>
      isRu ? 'РРєРѕРЅРєР° РїСЂРёР»РѕР¶РµРЅРёСЏ' : 'Application icon';
  String get downloadSettings =>
      isRu ? 'РЎРєР°С‡Р°С‚СЊ РЅР°СЃС‚СЂРѕР№РєРё' : 'Download settings';
  String get uploadSettings => isRu ? 'Р—Р°РіСЂСѓР·РёС‚СЊ РЅР°СЃС‚СЂРѕР№РєРё' : 'Upload settings';
  String get defaultLabel => isRu ? 'РџРѕ СѓРјРѕР»С‡Р°РЅРёСЋ' : 'Default';
  String get resetToDefault =>
      isRu ? 'РЎР±СЂРѕСЃРёС‚СЊ Рє СЃС‚Р°РЅРґР°СЂС‚Сѓ' : 'Reset to default';
  String get save => isRu ? 'РЎРѕС…СЂР°РЅРёС‚СЊ' : 'Save';
  String get downloadDictionary =>
      isRu ? 'РЎРєР°С‡Р°С‚СЊ СЃР»РѕРІР°СЂСЊ' : 'Download dictionary';
  String get updateDictionary =>
      isRu ? 'РћР±РЅРѕРІРёС‚СЊ СЃР»РѕРІР°СЂСЊ' : 'Update dictionary';
  String get uploadDictionary =>
      isRu ? 'Р—Р°РіСЂСѓР·РёС‚СЊ СЃР»РѕРІР°СЂСЊ' : 'Upload dictionary';
  String get resetDictionary => isRu ? 'РЎР±СЂРѕСЃРёС‚СЊ СЃР»РѕРІР°СЂСЊ' : 'Reset dictionary';
  String get pickApps => isRu ? 'Р’С‹Р±СЂР°С‚СЊ РїСЂРёР»РѕР¶РµРЅРёСЏ' : 'Pick applications';
  String get pickerTitle =>
      isRu ? 'РџСЂРёР»РѕР¶РµРЅРёСЏ РґР»СЏ РєРѕРЅРІРµСЂС‚Р°С†РёРё' : 'Choose apps for conversion';
  String get otpPickerTitle =>
      isRu ? 'РџСЂРёР»РѕР¶РµРЅРёСЏ РґР»СЏ РєРѕРґРѕРІ' : 'Choose apps for code detection';
  String get bypassPickerTitle =>
      isRu ? 'РџСЂРёР»РѕР¶РµРЅРёСЏ РѕР±С…РѕРґР°' : 'Choose apps for bypass';
  String get notificationDedupPickerTitle => isRu
      ? 'РџСЂРёР»РѕР¶РµРЅРёСЏ РґР»СЏ СѓРґР°Р»РµРЅРёСЏ РґСѓР±Р»РµР№'
      : 'Choose apps for notification dedup';
  String get applySelection => isRu ? 'РџСЂРёРјРµРЅРёС‚СЊ РІС‹Р±РѕСЂ' : 'Apply selection';
  String get searchAppHint =>
      isRu ? 'РџРѕРёСЃРє РїРѕ РЅР°Р·РІР°РЅРёСЋ РёР»Рё РїР°РєРµС‚Сѓ' : 'Search by app or package';
  String get showSystemApps =>
      isRu ? 'РџРѕРєР°Р·Р°С‚СЊ СЃРёСЃС‚РµРјРЅС‹Рµ РїСЂРёР»РѕР¶РµРЅРёСЏ' : 'Show system applications';
  String get hideSystemApps =>
      isRu ? 'РЎРєСЂС‹С‚СЊ СЃРёСЃС‚РµРјРЅС‹Рµ РїСЂРёР»РѕР¶РµРЅРёСЏ' : 'Hide system applications';
  String get appsLoadFailed => isRu
      ? 'РќРµ СѓРґР°Р»РѕСЃСЊ Р·Р°РіСЂСѓР·РёС‚СЊ СЃРїРёСЃРѕРє РїСЂРёР»РѕР¶РµРЅРёР№.'
      : 'Unable to load installed apps list.';
  String get appsAccessTitle =>
      isRu ? 'Р”РѕСЃС‚СѓРї Рє СЃРїРёСЃРєСѓ РїСЂРёР»РѕР¶РµРЅРёР№' : 'App list access';
  String get appsAccessMessage => isRu
      ? 'Р Р°Р·СЂРµС€РёС‚СЊ LiveBridge С‡РёС‚Р°С‚СЊ СЃРїРёСЃРѕРє СѓСЃС‚Р°РЅРѕРІР»РµРЅРЅС‹С… РїСЂРёР»РѕР¶РµРЅРёР№ РґР»СЏ РІС‹Р±РѕСЂР° РїСЂР°РІРёР»?'
      : 'Allow LiveBridge to read installed apps so you can pick apps for rules?';
  String get appsAccessSaveFailed => isRu
      ? 'РќРµ СѓРґР°Р»РѕСЃСЊ СЃРѕС…СЂР°РЅРёС‚СЊ РІС‹Р±РѕСЂ РґРѕСЃС‚СѓРїР°.'
      : 'Unable to save access preference.';
  String get cancel => isRu ? 'РћС‚РјРµРЅР°' : 'Cancel';
  String get allow => isRu ? 'Р Р°Р·СЂРµС€РёС‚СЊ' : 'Allow';
  String selectedAppsCount(int value) =>
      isRu ? 'Р’С‹Р±СЂР°РЅРѕ РїСЂРёР»РѕР¶РµРЅРёР№: $value' : 'Selected apps: $value';
  String get noAppsSelected =>
      isRu ? 'РџСЂРёР»РѕР¶РµРЅРёСЏ РЅРµ РІС‹Р±СЂР°РЅС‹' : 'No applications selected';

  String get rulesTitle => isRu ? 'Р РµР¶РёРј РєРѕРЅРІРµСЂС‚Р°С†РёРё' : 'Conversion behavior';
  String get rulesSubtitle => isRu
      ? 'РќР°СЃС‚СЂРѕР№С‚Рµ, С‡С‚Рѕ РёРјРµРЅРЅРѕ РїСЂРµРІСЂР°С‰Р°С‚СЊ РІ Live Updates.'
      : 'Define what should be converted into Live Updates.';
  String get modeLabel => isRu ? 'Р РµР¶РёРј СЂР°Р±РѕС‚С‹' : 'Application mode';
  String get modeAll => isRu ? 'Р’СЃРµ РїСЂРёР»РѕР¶РµРЅРёСЏ' : 'All applications';
  String get modeInclude =>
      isRu ? 'РўРѕР»СЊРєРѕ СѓРєР°Р·Р°РЅРЅС‹Рµ' : 'Only listed applications';
  String get modeExclude =>
      isRu ? 'РСЃРєР»СЋС‡РёС‚СЊ СѓРєР°Р·Р°РЅРЅС‹Рµ' : 'Exclude listed applications';
  String get pickAppsHint => isRu
      ? 'РЎРїРёСЃРѕРє РёСЃРїРѕР»СЊР·СѓРµС‚СЃСЏ С‚РѕР»СЊРєРѕ РІ СЂРµР¶РёРјР°С… "РўРѕР»СЊРєРѕ СѓРєР°Р·Р°РЅРЅС‹Рµ" РёР»Рё "РСЃРєР»СЋС‡РёС‚СЊ".'
      : 'Selected app list is used only for include/exclude modes.';
  String get bypassRulesTitle => isRu ? 'РџСЂРёР»РѕР¶РµРЅРёСЏ РѕР±С…РѕРґР°' : 'Bypass apps';
  String get bypassRulesSubtitle => isRu
      ? 'РџСЂРёР»РѕР¶РµРЅРёСЏ РёР· СЃРїРёСЃРєР° РІСЃРµРіРґР° РєРѕРЅРІРµСЂС‚РёСЂСѓСЋС‚СЃСЏ РІ Live РЅРµР·Р°РІРёСЃРёРјРѕ РѕС‚ РѕСЃС‚Р°Р»СЊРЅС‹С… РЅР°СЃС‚СЂРѕРµРє.'
      : 'Listed apps are always converted to Live independently of settings.';
  String get saveRules => isRu ? 'РЎРѕС…СЂР°РЅРёС‚СЊ' : 'Save';

  String get smartDetectionTitle =>
      isRu ? 'РЈРјРЅРѕРµ СЂР°СЃРїРѕР·РЅР°РІР°РЅРёРµ' : 'Smart status detection';
  String get smartCardTitle =>
      isRu ? 'РЈРјРЅРѕРµ РїСЂРµРѕР±СЂР°Р·РѕРІР°РЅРёРµ' : 'Smart conversion';
  String get smartCardSubtitle => isRu
      ? 'РџСЂРµРѕР±СЂР°Р·РѕРІР°РЅРёРµ С‚РµРєСЃС‚РѕРІС‹С… СЌС‚Р°РїРѕРІ РІ РѕРґРёРЅ Live-РїСЂРѕРіСЂРµСЃСЃ.'
      : 'Converts text-only stage updates into one Live progress flow.';
  String get smartDetectionSubtitle => isRu
      ? 'РџСЂРµРѕР±СЂР°Р·СѓРµС‚ С‚РµРєСЃС‚РѕРІС‹Рµ СЃС‚Р°С‚СѓСЃС‹ РµРґС‹, С‚Р°РєСЃРё Рё РЅР°РІРёРіР°С†РёРё РІ РµРґРёРЅС‹Р№ Live-РїСЂРѕРіСЂРµСЃСЃ.'
      : 'Converts text-only food/taxi/navigation status notifications into a single Live.';
  String get smartMediaPlaybackTitle =>
      isRu ? 'Р’РѕСЃРїСЂРѕРёР·РІРµРґРµРЅРёРµ РјРµРґРёР°' : 'Media Playback';
  String get smartMediaPlaybackSubtitle => isRu
      ? 'РџСЂРµРѕР±СЂР°Р·СѓРµС‚ СѓРІРµРґРѕРјР»РµРЅРёСЏ РјРµРґРёР°РїР»РµРµСЂР° РІ Live. РќР° РЅРµРєРѕС‚РѕСЂС‹С… OEM РјРѕР¶РµС‚ РґСѓР±Р»РёСЂРѕРІР°С‚СЊ РЅР°С‚РёРІРЅС‹Р№ РїР»РµРµСЂ.'
      : 'Converts media playback notifications into Live. On some OEMs this may duplicate native media UI.';
  String get smartNavigationTitle =>
      isRu ? 'РќР°РІРёРіР°С†РёСЏ (РєР°СЂС‚С‹)' : 'Navigation (maps)';
  String get smartNavigationSubtitle => isRu
      ? 'Р Р°СЃРїРѕР·РЅР°РІР°РЅРёРµ СѓРІРµРґРѕРјР»РµРЅРёР№ РЅР°РІРёРіР°С†РёРё.'
      : 'Navigation notification detection.';
  String get smartWeatherTitle => isRu ? 'РџРѕРіРѕРґР°' : 'Weather';
  String get smartWeatherSubtitle => isRu
      ? 'Р Р°СЃРїРѕР·РЅР°РІР°РЅРёРµ РїРѕРіРѕРґРЅС‹С… СѓРІРµРґРѕРјР»РµРЅРёР№ (С‚РµРјРїРµСЂР°С‚СѓСЂР° РІ РѕСЃС‚СЂРѕРІРµ).'
      : 'Weather notification detection (temperature in island).';
  String get smartWeatherLockscreenOnlyTitle => isRu
      ? 'РџРѕРєР°Р·С‹РІР°С‚СЊ С‚РѕР»СЊРєРѕ РЅР° СЌРєСЂР°РЅРµ Р±Р»РѕРєРёСЂРѕРІРєРё'
      : 'Display only on lock screen';
  String get smartWeatherLockscreenOnlySubtitle => '';
  String get smartExternalDevicesTitle =>
      isRu ? 'Р’РЅРµС€РЅРёРµ СѓСЃС‚СЂРѕР№СЃС‚РІР°' : 'External devices';
  String get smartExternalDevicesSubtitle => isRu
      ? 'РџРѕРєР°Р·С‹РІР°РµС‚ СЃС‚Р°С‚СѓСЃ РїРѕРґРєР»СЋС‡РµРЅРёСЏ Рё РёРјСЏ СѓСЃС‚СЂРѕР№СЃС‚РІР° РІ РѕСЃС‚СЂРѕРІРµ.'
      : 'Shows connected/connecting status and device name in island.';
  String get smartVpnTitle => isRu ? 'VPN-СЃРµСЂРІРёСЃС‹' : 'VPN services';
  String get smartVpnSubtitle => isRu
      ? 'РџРѕРєР°Р·С‹РІР°РµС‚ РІС…РѕРґСЏС‰РёР№/РёСЃС…РѕРґСЏС‰РёР№ С‚СЂР°С„РёРє РІ С„РѕСЂРјР°С‚Рµ *b/s.'
      : 'Shows incoming/outgoing traffic speed in *b/s format.';
  String get smartFlashlightTitle => isRu ? 'Р¤РѕРЅР°СЂРёРє' : 'Flashlight';
  String get smartFlashlightSubtitle => isRu
      ? 'РЎРѕР·РґР°С‘С‚ С‚РµСЃС‚РѕРІРѕРµ СѓРІРµРґРѕРјР»РµРЅРёРµ Now Bar СЃ 5-С‚РѕС‡РµС‡РЅС‹Рј СѓРїСЂР°РІР»РµРЅРёРµРј СЏСЂРєРѕСЃС‚СЊСЋ С„РѕРЅР°СЂРёРєР°.'
      : 'Creates a test Now Bar notification with a 5-point flashlight brightness control.';
  String get smartFlashlightUnsupportedSubtitle => isRu
      ? 'РЈСЃС‚СЂРѕР№СЃС‚РІРѕ РІРєР»СЋС‡Р°РµС‚ С„РѕРЅР°СЂРёРє, РЅРѕ РЅРµ РґР°С‘С‚ 5 РѕС‚РґРµР»СЊРЅС‹С… СѓСЂРѕРІРЅРµР№ СЏСЂРєРѕСЃС‚Рё.'
      : 'This device can enable the flashlight, but it does not expose 5 separate brightness levels.';
  String get smartFlashlightUnavailableSubtitle => isRu
      ? 'РќР° СЌС‚РѕРј СѓСЃС‚СЂРѕР№СЃС‚РІРµ РЅРµС‚ РґРѕСЃС‚СѓРїРЅРѕРіРѕ С„РѕРЅР°СЂРёРєР°.'
      : 'This device does not expose a usable flashlight.';
  String smartFlashlightBrightnessLabel(int level) => isRu
      ? 'РЇСЂРєРѕСЃС‚СЊ ${level.clamp(1, 5)}/5'
      : 'Brightness ${level.clamp(1, 5)}/5';
  String get smartFlashlightLevelSelectorHint => isRu
      ? 'РџСЂР°РІР°СЏ С‚РѕС‡РєР° = РјР°РєСЃРёРјР°Р»СЊРЅР°СЏ СЏСЂРєРѕСЃС‚СЊ.'
      : 'The rightmost point is maximum brightness.';
  String get smartFlashlightFallbackWarning => isRu
      ? 'РџРѕРєР°Р·Р°РЅ fallback: С‚РѕС‡РєРё РѕС‚РєР»СЋС‡РµРЅС‹, РїРѕС‚РѕРјСѓ С‡С‚Рѕ СѓСЃС‚СЂРѕР№СЃС‚РІРѕ РЅРµ РїРѕРґРґРµСЂР¶РёРІР°РµС‚ 5 СѓСЂРѕРІРЅРµР№ СЏСЂРєРѕСЃС‚Рё С„РѕРЅР°СЂРёРєР°.'
      : 'Fallback mode: the dots are disabled because this device does not support 5 flashlight brightness levels.';
  String get networkSpeedCardTitle =>
      isRu ? 'РЎРєРѕСЂРѕСЃС‚СЊ РёРЅС‚РµСЂРЅРµС‚Р°' : 'Network speed';
  String get networkSpeedEnabledTitle => isRu
      ? 'РџРѕРєР°Р·С‹РІР°С‚СЊ СЃРєРѕСЂРѕСЃС‚СЊ РёРЅС‚РµСЂРЅРµС‚Р° РІ Now Bar'
      : 'Show network speed in Now Bar';
  String get networkSpeedEnabledSubtitle => isRu
      ? 'Р—Р°РїСѓСЃРєР°РµС‚ РѕС‚РґРµР»СЊРЅРѕРµ СѓРІРµРґРѕРјР»РµРЅРёРµ СЃ С‚РµРєСѓС‰РµР№ СЃРєРѕСЂРѕСЃС‚СЊСЋ СЃРµС‚Рё Рё РІС‹РІРѕРґРёС‚ РµРіРѕ РІ Now Bar.'
      : 'Runs a dedicated ongoing notification with current network speed and surfaces it in the Now Bar.';
  String get networkSpeedThresholdTitle =>
      isRu ? 'РњРёРЅРёРјР°Р»СЊРЅР°СЏ СЃРєРѕСЂРѕСЃС‚СЊ РґР»СЏ РїРѕРєР°Р·Р°' : 'Minimum speed to show';
  String get networkSpeedThresholdSubtitle => isRu
      ? 'Live-РёРЅРґРёРєР°С‚РѕСЂ РїРѕСЏРІРёС‚СЃСЏ, РєРѕРіРґР° СЃСѓРјРјР°СЂРЅР°СЏ СЃРєРѕСЂРѕСЃС‚СЊ Р·Р°РіСЂСѓР·РєРё Рё РѕС‚РґР°С‡Рё РґРѕСЃС‚РёРіРЅРµС‚ СЌС‚РѕРіРѕ РїРѕСЂРѕРіР°.'
      : 'The live element appears when combined download and upload reach this threshold.';
  String get networkSpeedThresholdAlways =>
      isRu ? 'РџРѕРєР°Р·С‹РІР°С‚СЊ РІСЃРµРіРґР°' : 'Always show';
  String get networkSpeedDisplayContentTitle =>
      isRu ? 'РћС‚РѕР±СЂР°Р¶Р°РµРјС‹Р№ РєРѕРЅС‚РµРЅС‚' : 'Display content';
  String get networkSpeedDisplayModeTotal =>
      isRu ? 'РћР±С‰Р°СЏ СЃРєРѕСЂРѕСЃС‚СЊ' : 'Total speed';
  String get networkSpeedDisplayModeUpload =>
      isRu ? 'РўРѕР»СЊРєРѕ РѕС‚РґР°С‡Р°' : 'Upload only';
  String get networkSpeedDisplayModeDownload =>
      isRu ? 'РўРѕР»СЊРєРѕ Р·Р°РіСЂСѓР·РєР°' : 'Download only';
  String get networkSpeedUploadPrefixTitle =>
      isRu ? 'РџСЂРµС„РёРєСЃ РѕС‚РґР°С‡Рё' : 'Upload prefix';
  String get networkSpeedDownloadPrefixTitle =>
      isRu ? 'РџСЂРµС„РёРєСЃ Р·Р°РіСЂСѓР·РєРё' : 'Download prefix';
  String get networkSpeedUnitTitle => isRu ? 'Р•РґРёРЅРёС†Р° СЃРєРѕСЂРѕСЃС‚Рё' : 'Speed unit';
  String get networkSpeedUnitAuto => isRu ? 'РђРІС‚Рѕ' : 'Auto';
  String get networkSpeedUnitBytes => isRu ? 'B/s' : 'B/s';
  String get networkSpeedUnitKilobytes => isRu ? 'KB/s' : 'KB/s';
  String get networkSpeedUnitMegabytes => isRu ? 'MB/s' : 'MB/s';
  String get networkSpeedUnitGigabytes => isRu ? 'GB/s' : 'GB/s';
  String networkSpeedCurrentValue(String value) =>
      isRu ? 'РЎРµР№С‡Р°СЃ: "$value"' : 'Current: "$value"';
  String get networkSpeedPrioritizeUploadTitle =>
      isRu ? 'РЎРЅР°С‡Р°Р»Р° РїРѕРєР°Р·С‹РІР°С‚СЊ РѕС‚РґР°С‡Сѓ' : 'Prioritize upload speed';
  String get networkSpeedPrioritizeUploadSubtitle => isRu
      ? 'Р’ СЂРµР¶РёРјРµ РѕР±С‰РµР№ СЃРєРѕСЂРѕСЃС‚Рё РѕС‚РґР°С‡Р° Р±СѓРґРµС‚ СЃС‚РѕСЏС‚СЊ РїРµСЂРµРґ Р·Р°РіСЂСѓР·РєРѕР№.'
      : 'In total mode, upload speed is shown before download.';
  String get networkSpeedLockscreenOnlyTitle => isRu
      ? 'РџРѕРєР°Р·С‹РІР°С‚СЊ С‚РѕР»СЊРєРѕ РЅР° СЌРєСЂР°РЅРµ Р±Р»РѕРєРёСЂРѕРІРєРё'
      : 'Display only on lock screen';
  String get networkSpeedLockscreenOnlySubtitle => isRu
      ? 'РџСЂРё СЂР°Р·Р±Р»РѕРєРёСЂРѕРІР°РЅРЅРѕРј СЌРєСЂР°РЅРµ РёРЅРґРёРєР°С‚РѕСЂ СЃРєРѕСЂРѕСЃС‚Рё РїРѕР»РЅРѕСЃС‚СЊСЋ СЃРєСЂС‹РІР°РµС‚СЃСЏ Рё РІРѕР·РІСЂР°С‰Р°РµС‚СЃСЏ С‚РѕР»СЊРєРѕ РЅР° СЌРєСЂР°РЅРµ Р±Р»РѕРєРёСЂРѕРІРєРё.'
      : 'When the device is unlocked, network speed is hidden completely and returns only on the lock screen.';
  String get networkSpeedDisableChipBackgroundTitle =>
      isRu ? 'РћС‚РєР»СЋС‡РёС‚СЊ С„РѕРЅ С‡РёРїРѕРІ' : 'Disable chip background';
  String get networkSpeedDisableChipBackgroundSubtitle => isRu
      ? 'РЈР±РёСЂР°РµС‚ РїР»Р°С€РєСѓ Сѓ С‡РёРїРѕРІ СЃРєРѕСЂРѕСЃС‚Рё РІ Now Bar, РЅРѕ РѕСЃС‚Р°РІР»СЏРµС‚ СЃС‚Р°С‚РёС‡РЅС‹Р№ С†РІРµС‚ Сѓ РёРєРѕРЅРєРё РІ СЂР°Р·РІРµСЂРЅСѓС‚РѕРј СѓРІРµРґРѕРјР»РµРЅРёРё.'
      : 'Removes the pill background from the network speed chips in Now Bar while keeping a fixed accent behind the icon in the expanded notification.';
  String get smartNavigationDisabledSubtitle => isRu
      ? 'РЎРЅР°С‡Р°Р»Р° РІРєР»СЋС‡РёС‚Рµ СѓРјРЅРѕРµ СЂР°СЃРїРѕР·РЅР°РІР°РЅРёРµ.'
      : 'Enable smart status detection first.';
  String get smartDetectionDisabledSubtitle => isRu
      ? 'РћС‚РєР»СЋС‡РµРЅРѕ РІ СЂРµР¶РёРјРµ "РќР°С‚РёРІРЅС‹Р№ РїСЂРѕРіСЂРµСЃСЃ".'
      : 'Disabled while "Native progress" mode is enabled.';
  String get conflictingModesHint => isRu
      ? 'Р§С‚РѕР±С‹ СЂР°Р±РѕС‚Р°Р»Рё С‚РµРєСЃС‚РѕРІС‹Рµ СЃС‚Р°С‚СѓСЃС‹, РѕС‚РєР»СЋС‡РёС‚Рµ СЂРµР¶РёРј "РќР°С‚РёРІРЅС‹Р№ РїСЂРѕРіСЂРµСЃСЃ".'
      : 'Turn off "Native progress" mode to enable food/taxi/navigation text status recognition.';
  String get onlyProgressTitle =>
      isRu ? 'РќР°С‚РёРІРЅС‹Р№ РїСЂРѕРіСЂРµСЃСЃ' : 'Native progress';
  String get onlyProgressSubtitle => isRu
      ? 'Р•СЃР»Рё РІРєР»СЋС‡РµРЅРѕ, РєРѕРЅРІРµСЂС‚РёСЂСѓСЋС‚СЃСЏ С‚РѕР»СЊРєРѕ СѓРІРµРґРѕРјР»РµРЅРёСЏ СЃ СЃРёСЃС‚РµРјРЅС‹Рј РїСЂРѕРіСЂРµСЃСЃР±Р°СЂРѕРј.'
      : 'When enabled, only notifications with a system progress bar are converted.';
  String get textProgressTitle =>
      isRu ? 'РўРµРєСЃС‚РѕРІС‹Рµ РїСЂРѕРіСЂРµСЃСЃС‹' : 'Text progress';
  String get textProgressSubtitle => isRu
      ? 'Р•СЃР»Рё РІ С‚РµРєСЃС‚Рµ РµСЃС‚СЊ %, Рё СЌС‚Рѕ РЅРµ СЃРєРёРґРєР°/Р°РєС†РёСЏ, СЃС‡РёС‚Р°С‚СЊ РєР°Рє РїСЂРѕРіСЂРµСЃСЃ Рё РѕР±РЅРѕРІР»СЏС‚СЊ РѕСЃС‚СЂРѕРІ.'
      : 'If text contains % and it is not discount-related, treat it as progress and update island.';

  String get blockedTitle =>
      isRu ? 'AOSP РїРѕРґРґРµСЂР¶РёРІР°РµС‚СЃСЏ С‡Р°СЃС‚РёС‡РЅРѕ' : 'AOSP is partially supported';
  String get blockedSubtitle => isRu
      ? 'LiveBridge РїР»РѕС…Рѕ СЂР°Р±РѕС‚Р°РµС‚ РЅР° СѓСЃС‚СЂРѕР№СЃС‚РІР°С… СЃ AOSP. РњРѕР¶РµС‚Рµ РїСЂРѕРґРѕР»Р¶РёС‚СЊ, РЅРѕ Р·Р° РїРѕСЃР»РµРґСЃС‚РІРёСЏ СЏ РЅРµ РѕС‚РІРµС‡Р°СЋ.'
      : 'LiveBridge is not designed for AOSP. You can continue, but i am not responsible for any bugs.';
  String get blockedBypassAction =>
      isRu ? 'Р’СЃРµ СЂР°РІРЅРѕ РїСЂРѕРґРѕР»Р¶РёС‚СЊ' : 'Continue anyway';
  String get blockedBypassSaveFailed =>
      isRu ? 'РќРµ СѓРґР°Р»РѕСЃСЊ СЃРѕС…СЂР°РЅРёС‚СЊ РІС‹Р±РѕСЂ.' : 'Unable to save your choice.';






  String get networkSpeedTitle => tr(
    en: 'Network speed',
    ru: 'РЎРєРѕСЂРѕСЃС‚СЊ СЃРµС‚Рё',
    tr: 'AДџ hД±zД±',
    zhHans: 'зЅ‘йЂџ',
    zhHant: 'з¶ІйЂџ',
  );



  String get preventDismissingTitle => tr(
    en: 'Prevent dismissing',
    ru: 'Р—Р°РїСЂРµС‚РёС‚СЊ СЃРєСЂС‹С‚РёРµ',
    tr: 'KapatmayД± engelle',
    zhHans: 'йІж­ўиў«е…ій—­',
    zhHant: 'йІж­ўиў«й—њй–‰',
  );





















}

const Map<String, String> _ptBrTranslations = <String, String>{
  'Refresh': 'Atualizar',
  'Notification permission granted.': 'PermissГЈo de notificaГ§Гµes concedida.',
  'Notification permission was not granted.':
      'PermissГЈo de notificaГ§Гµes nГЈo concedida.',
  'Unable to open Listener settings on this device.':
      'NГЈo foi possГ­vel abrir as configuraГ§Гµes do Listener neste dispositivo.',
  'Unable to open app notification settings.':
      'NГЈo foi possГ­vel abrir as configuraГ§Гµes de notificaГ§ГЈo do app.',
  'Unable to open Live Updates settings on this device.':
      'NГЈo foi possГ­vel abrir as configuraГ§Гµes de Live Updates neste dispositivo.',
  'Unable to open GitHub link.': 'NГЈo foi possГ­vel abrir o link do GitHub.',
  'Unable to open link.': 'NГЈo foi possГ­vel abrir o link.',
  'Unable to check updates. Try disabling VPN.':
      'NГЈo foi possГ­vel verificar atualizaГ§Гµes. Tente desativar a VPN.',
  'Dictionary is empty or invalid.': 'O dicionГЎrio estГЎ vazio ou invГЎlido.',
  'Dictionary updated from GitHub.': 'DicionГЎrio atualizado pelo GitHub.',
  'Invalid dictionary JSON.': 'JSON do dicionГЎrio invГЎlido.',
  'Failed to update dictionary from GitHub.':
      'Falha ao atualizar o dicionГЎrio pelo GitHub.',
  'Dictionary': 'DicionГЎrio',
  'tap to manage': 'toque para gerenciar',
  'Dictionary languages': 'Idiomas do dicionГЎrio',
  'tap to choose': 'toque para escolher',
  'Select languages for conversion': 'Selecione idiomas para conversГЈo',
  'Update dictionaries': 'Atualizar dicionГЎrios',
  'Dictionary editor': 'Editor de dicionГЎrio',
  '(coming soon)': '(em breve)',
  'Home': 'InГ­cio',
  'Rules': 'Regras',
  'Settings': 'ConfiguraГ§Гµes',
  'App config': 'ConfiguraГ§ГЈo do app',
  'Brand-specific': 'EspecГ­fico da marca',
  'App updates': 'AtualizaГ§Гµes do app',
  'LiveBridge is running': 'LiveBridge estГЎ em execuГ§ГЈo',
  'LiveBridge is disabled': 'LiveBridge estГЎ desativado',
  'by ': 'por ',
  'Discuss': 'Discutir',
  'telegram topics': 'tГіpicos no Telegram',
  'all apps': 'todos os apps',
  'only selected': 'somente selecionados',
  'exclude selected': 'excluir selecionados',
  'check required': 'verificaГ§ГЈo necessГЎria',
  'all set': 'tudo certo',
  'tap to update': 'toque para atualizar',
  'latest version': 'versГЈo mais recente',
  'Recent conversions': 'ConversГµes recentes',
  'no conversions yet': 'nenhuma conversГЈo ainda',
  'conversion log is disabled': 'o log de conversГµes estГЎ desativado',
  'enable': 'ativar',
  'Payload copied': 'Payload copiado',
  'Progress': 'Progresso',
  'Native progress': 'Progresso nativo',
  'OTP codes': 'CГіdigos OTP',
  'Auto-copy code': 'Copiar cГіdigo automaticamente',
  'Smart conversion': 'ConversГЈo inteligente',
  'Taxi': 'TГЎxi',
  'Deliveries': 'Entregas',
  'Remove original message': 'Remover mensagem original',
  '(exp)': '(exp)',
  'All apps': 'Todos os apps',
  'Only selected': 'Somente selecionados',
  'Exclude selected': 'Excluir selecionados',
  'Conversion mode': 'Modo de conversГЈo',
  'Selected apps': 'Apps selecionados',
  'show system': 'mostrar sistema',
  'hide system': 'ocultar sistema',
  'Network & Connections': 'Rede e conexГµes',
  'VPNs': 'VPNs',
  'External devices': 'Dispositivos externos',
  'Ignore debugging devices': 'Ignorar dispositivos de depuraГ§ГЈo',
  'Network speed threshold': 'Limite de velocidade de rede',
  'Miscellaneous': 'Diversos',
  'Navigation (maps)': 'NavegaГ§ГЈo (mapas)',
  'Media playback': 'ReproduГ§ГЈo de mГ­dia',
  'Weather broadcasts': 'Alertas de clima',
  'Bypass': 'Bypass',
  'Per-app settings': 'ConfiguraГ§Гµes por app',
  'Defaults': 'PadrГµes',
  'tap to change default behavior': 'toque para alterar o comportamento padrГЈo',
  'Apps list': 'Lista de apps',
  'Export': 'Exportar',
  'Import': 'Importar',
  'Title source': 'Origem do tГ­tulo',
  'Content source': 'Origem do conteГєdo',
  'Notification title': 'TГ­tulo da notificaГ§ГЈo',
  'App title': 'TГ­tulo do app',
  'Notification text': 'Texto da notificaГ§ГЈo',
  'New version available': 'Nova versГЈo disponГ­vel',
  'Checking for updates': 'Verificando atualizaГ§Гµes',
  'YouвЂ™re all set': 'Tudo certo',
  'tap to go to downloads': 'toque para ir aos downloads',
  'please wait a moment': 'aguarde um momento',
  'latest version already': 'jГЎ estГЎ na versГЈo mais recente',
  'Visit project page': 'Abrir pГЎgina do projeto',
  'Visit GitHub': 'Abrir GitHub',
  'update available | tap to see': 'atualizaГ§ГЈo disponГ­vel | toque para ver',
  'tap to open update settings': 'toque para abrir ajustes de atualizaГ§ГЈo',
  'Conversion log': 'Log de conversГµes',
  'Log length': 'Tamanho do log',
  'Xiaomi HyperIsland': 'Xiaomi HyperIsland',
  'Length': 'Tamanho',
  'OTP dedup': 'DeduplicaГ§ГЈo de OTP',
  'Smart conversion dedup': 'DeduplicaГ§ГЈo da conversГЈo inteligente',
  'Animated Island': 'Ilha animada',
  'Update frequency': 'FrequГЄncia de atualizaГ§ГЈo',
  'Copy debug JSON': 'Copiar JSON de debug',
  'Open GitHub page': 'Abrir pГЎgina do GitHub',
  'Auto-copy debug JSON': 'Copiar JSON de debug automaticamente',
  'Title': 'TГ­tulo',
  'Payload JSON': 'Payload JSON',
  'loading apps...': 'carregando apps...',
  'Search for apps...': 'Buscar apps...',
  'Report a bug': 'Reportar um bug',
  'Diagnostics copied to clipboard. Paste it into the issue.':
      'DiagnГіstico copiado para a ГЎrea de transferГЄncia. Cole no issue.',
  'Failed to copy diagnostics.': 'Falha ao copiar diagnГіstico.',
  'Permissions': 'PermissГµes',
  'Notification Listener access': 'Acesso ao Notification Listener',
  'Post notifications permission': 'PermissГЈo para enviar notificaГ§Гµes',
  'Live Updates promotion': 'PermissГЈo para Live Updates',
  'Alt background mode': 'Modo alternativo em segundo plano',
  'Network speed': 'Velocidade da rede',
  'Always show': 'Sempre mostrar',
  'Sync DnD': 'Sincronizar NГЈo Perturbe',
  'Prevent dismissing': 'Impedir dispensa',
  'Update checking': 'VerificaГ§ГЈo de atualizaГ§Гµes',
  'Experimental': 'Experimental',
  'AOSP cutting': 'Recorte AOSP',
  'Per-app behavior': 'Comportamento por app',
  'Unable to load per-app settings.':
      'NГЈo foi possГ­vel carregar configuraГ§Гµes por app.',
  'Unable to save per-app settings.':
      'NГЈo foi possГ­vel salvar configuraГ§Гµes por app.',
  'Failed to save settings JSON.': 'Falha ao salvar JSON de configuraГ§Гµes.',
  'Settings saved to Downloads.': 'ConfiguraГ§Гµes salvas em Downloads.',
  'Per-app settings imported.': 'ConfiguraГ§Гµes por app importadas.',
  'Failed to import settings JSON.': 'Falha ao importar JSON de configuraГ§Гµes.',
  'Invalid per-app settings JSON.': 'JSON de configuraГ§Гµes por app invГЎlido.',
  'Download settings': 'Baixar configuraГ§Гµes',
  'Upload settings': 'Enviar configuraГ§Гµes',
  'Save': 'Salvar',
  'Unable to load installed apps list.':
      'NГЈo foi possГ­vel carregar a lista de apps instalados.',
  'App list access': 'Acesso Г  lista de apps',
  'Allow LiveBridge to read installed apps so you can pick apps for rules?':
      'Permitir que o LiveBridge leia os apps instalados para que vocГЄ possa escolher apps para as regras?',
  'Unable to save access preference.':
      'NГЈo foi possГ­vel salvar a preferГЄncia de acesso.',
  'Cancel': 'Cancelar',
  'Allow': 'Permitir',
  'Text progress': 'Progresso por texto',
};

