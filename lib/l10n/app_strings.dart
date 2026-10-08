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

  bool get isKo => locale.languageCode.toLowerCase().startsWith('ko');

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
    String? es,
    String? de,
    String? tr,
    String? ptBr,
    String? zhHans,
    String? zhHant,
    String? ko,
  }) {
    if (locale.languageCode == 'es') return es ?? en;
    if (locale.languageCode == 'de') return de ?? en;
    if (isRu) return ru;
    if (isTr) return tr ?? en;
    if (isPtBr) return ptBr ?? en;
    if (isZhHant) return zhHant ?? zhHans ?? en;
    if (isZhHans) return zhHans ?? zhHant ?? en;
    if (isKo) return ko ?? en;
    return en;
  }

  String get promotionUnavailableStatus => tr(
    en: 'Unavailable on this firmware',
    es: 'No disponible en este firmware',
    de: 'In dieser Firmware nicht verfügbar',
    ru: 'Недоступно в этой прошивке',
  );
  String get promotionUnknownStatus => tr(
    en: 'Could not check access',
    es: 'No se pudo comprobar el acceso',
    de: 'Zugriff konnte nicht geprüft werden',
    ru: 'Не удалось проверить доступ',
  );
  String get promotionUnavailableHelp => tr(
    en: 'the system does not expose a working Live Updates permission check or settings page. This is not an overlay permission. Notification conversion can stay enabled, but the firmware decides whether to show a live capsule',
    es: 'el sistema no ofrece una comprobación de permisos ni una página de ajustes funcional para Live Updates. Este permiso no sirve para mostrar elementos sobre otras aplicaciones. La conversión de notificaciones puede seguir activada, pero el firmware decide si muestra una cápsula',
    de: 'das System bietet keine funktionierende Berechtigungsprüfung oder Einstellungsseite für Live Updates. Dies ist keine Berechtigung zum Einblenden über anderen Apps. Die Umwandlung von Benachrichtigungen kann aktiviert bleiben; ob eine Kapsel angezeigt wird, entscheidet die Firmware',
    ru: 'система не предоставляет рабочую проверку или настройку доступа к Live Updates. Это не разрешение на показ поверх других приложений. Конвертация уведомлений может оставаться включённой, но показ капсулы зависит от прошивки',
  );
  String get networkSpeedHideWhenLocked => tr(
    en: 'Hide speed when locked',
    es: 'Ocultar velocidad al bloquear',
    de: 'Geschwindigkeit bei Sperre ausblenden',
    ru: 'Скрывать скорость при блокировке',
  );
  String get networkSpeedHideWhenLockedHelp => tr(
    en: 'hide the speed capsule while the screen is off or locked. It returns after unlocking. The background service notification stays in the notification panel',
    es: 'oculta la cápsula de velocidad cuando la pantalla está apagada o bloqueada. Vuelve a aparecer al desbloquear. La notificación del servicio en segundo plano permanece en el panel de notificaciones',
    de: 'blendet die Geschwindigkeitskapsel bei ausgeschaltetem oder gesperrtem Bildschirm aus. Nach dem Entsperren erscheint sie wieder. Die Benachrichtigung des Hintergrunddienstes bleibt in der Benachrichtigungsleiste',
    ru: 'скрывать капсулу скорости при выключенном или заблокированном экране. После разблокировки она возвращается. Уведомление фоновой службы остаётся в шторке',
  );

  String get textFiltersTitle => tr(
    en: 'Notification text filters',
    es: 'Filtros de texto de notificaciones',
    de: 'Textfilter für Benachrichtigungen',
    ru: 'Фильтры по тексту уведомлений',
  );
  String get textFiltersDescription => tr(
    en: 'choose notifications by words in their title or text. Excluded notifications keep their originals',
    es: 'selecciona notificaciones según las palabras de su título o texto. Las notificaciones excluidas conservan sus originales',
    de: 'wählt Benachrichtigungen anhand von Wörtern im Titel oder Text aus. Die Originale ausgeschlossener Benachrichtigungen bleiben erhalten',
    ru: 'выбирайте уведомления по словам в заголовке или тексте. Оригиналы исключённых уведомлений сохраняются',
  );
  String get filterTemplateTitle => tr(
    en: 'Custom island text (optional)',
    es: 'Texto personalizado de la cápsula (opcional)',
    de: 'Eigener Kapseltext (optional)',
    ru: 'Свой текст на островке (необязательно)',
  );
  String get filterTemplateHelp => tr(
    en: 'Use {title}, {text}, and {app}, or write your own text. Empty app templates use the global template. Call controls, media players and OTP codes keep their original presentation.',
    es: 'Usa {title}, {text} y {app}, o escribe tu propio texto. Si la plantilla de una aplicación está vacía, se usa la global. Las llamadas, los reproductores y los códigos OTP conservan su presentación original.',
    de: 'Verwende {title}, {text} und {app} oder eigenen Text. Leere App-Vorlagen verwenden die globale Vorlage. Anrufe, Medienplayer und OTP-Codes behalten ihre ursprüngliche Darstellung.',
    ru: 'Используйте {title}, {text} и {app} или напишите свой текст. Пустой шаблон приложения использует общий шаблон. Звонки, плеер и OTP сохраняют своё отображение.',
  );
  String get filterAllApps => tr(
    en: 'All apps',
    es: 'Todas las aplicaciones',
    de: 'Alle Apps',
    ru: 'Все приложения',
  );
  String get filterAllowWords => tr(
    en: 'Must contain (one word or phrase per line)',
    es: 'Debe contener (una palabra o frase por línea)',
    de: 'Muss enthalten (ein Wort oder Ausdruck pro Zeile)',
    ru: 'Должно содержать (слово или фраза на строку)',
  );
  String get filterDenyWords => tr(
    en: 'Do not convert if it contains',
    es: 'No convertir si contiene',
    de: 'Nicht umwandeln, wenn enthalten',
    ru: 'Не конвертировать, если содержит',
  );
  String get filterMatchAll => tr(
    en: 'Require all allowed phrases',
    es: 'Exigir todas las frases permitidas',
    de: 'Alle erlaubten Ausdrücke verlangen',
    ru: 'Требовать все разрешённые фразы',
  );
  String get filterHelp => tr(
    en: 'matching ignores case. Exclusions win. Leave the allowed list empty to allow any text. Global and app filters both apply, including always-convert apps',
    es: 'no se distingue entre mayúsculas y minúsculas. Las exclusiones tienen prioridad. Deja vacía la lista de palabras permitidas para aceptar cualquier texto. Se aplican tanto los filtros globales como los de cada aplicación, incluso a las aplicaciones que siempre se convierten',
    de: 'Groß- und Kleinschreibung wird ignoriert. Ausschlüsse haben Vorrang. Eine leere Liste erlaubter Ausdrücke lässt jeden Text zu. Globale Filter und App-Filter gelten gemeinsam, auch für Apps mit erzwungener Umwandlung',
    ru: 'регистр не учитывается. Запрет имеет приоритет. Пустой разрешённый список пропускает любой текст. Общий фильтр и фильтр приложения действуют вместе, в том числе для «всегда конвертировать»',
  );
  String get wordEditorHelp => tr(
    en: 'Add recognition words and app hints, one per line. Built-in dictionaries and imported rules are kept. Remove your additions here to return to built-in recognition.',
    es: 'Añade palabras de reconocimiento e identificadores de aplicaciones, uno por línea. Se conservan los diccionarios integrados y las reglas importadas. Elimina tus añadidos para volver al reconocimiento original.',
    de: 'Füge Erkennungswörter und App-Kennungen hinzu, jeweils eines pro Zeile. Integrierte Wörterbücher und importierte Regeln bleiben erhalten. Entferne deine Ergänzungen, um zur ursprünglichen Erkennung zurückzukehren.',
    ru: 'Добавляйте слова распознавания и подсказки приложений, по одному на строку. Встроенные словари и импортированные правила сохраняются. Удалите добавленные слова, чтобы вернуть исходное распознавание.',
  );
  String get editorClear => tr(
    en: 'Clear additions',
    es: 'Borrar añadidos',
    de: 'Ergänzungen löschen',
    ru: 'Очистить добавления',
  );
  String get editorLimit => tr(
    en: 'Use at most 100 entries per field, up to 200 characters each.',
    es: 'Máximo de 100 entradas por campo, de hasta 200 caracteres cada una.',
    de: 'Höchstens 100 Einträge pro Feld mit jeweils bis zu 200 Zeichen.',
    ru: 'Не более 100 строк в поле, до 200 символов в каждой.',
  );
  String dictionaryWordField(String key) {
    const labels = <String, List<String>>{
      'otp_strong_triggers': [
        'Verification code words',
        'Слова для кодов подтверждения',
        'Palabras para códigos de verificación',
        'Wörter für Bestätigungscodes',
      ],
      'progress_words': [
        'Progress words',
        'Слова прогресса',
        'Palabras de progreso',
        'Fortschrittswörter',
      ],
      'weather_words': [
        'Weather words',
        'Слова о погоде',
        'Palabras meteorológicas',
        'Wetterwörter',
      ],
      'weather_package_hints': [
        'Weather app hints',
        'Подсказки погодных приложений',
        'Identificadores de aplicaciones meteorológicas',
        'Kennungen für Wetter-Apps',
      ],
      'known_navigation_packages': [
        'Navigation app packages',
        'Пакеты навигационных приложений',
        'Paquetes de aplicaciones de navegación',
        'Pakete von Navigations-Apps',
      ],
      'navigation_package_markers': [
        'Navigation app hints',
        'Подсказки навигационных приложений',
        'Identificadores de aplicaciones de navegación',
        'Kennungen für Navigations-Apps',
      ],
      'vpn_package_markers': [
        'VPN app hints',
        'Подсказки VPN-приложений',
        'Identificadores de aplicaciones VPN',
        'Kennungen für VPN-Apps',
      ],
      'order_context_hints': [
        'Order words',
        'Слова о заказах',
        'Palabras de contexto de pedidos',
        'Wörter zum Bestellkontext',
      ],
      'food_words': [
        'Food delivery words',
        'Слова о доставке еды',
        'Palabras de entrega de comida',
        'Wörter für Essenslieferungen',
      ],
      'food_packages': [
        'Food delivery app hints',
        'Подсказки приложений доставки еды',
        'Identificadores de aplicaciones de entrega de comida',
        'Kennungen für Essensliefer-Apps',
      ],
      'taxi_words': [
        'Taxi words',
        'Слова о такси',
        'Palabras de taxi',
        'Taxi-Wörter',
      ],
      'taxi_packages': [
        'Taxi app hints',
        'Подсказки приложений такси',
        'Identificadores de aplicaciones de taxi',
        'Kennungen für Taxi-Apps',
      ],
    };
    final index = locale.languageCode == 'es'
        ? 2
        : locale.languageCode == 'de'
        ? 3
        : isRu
        ? 1
        : 0;
    return labels[key]?[index] ?? key;
  }

  String get settingsSaveError => tr(
    en: "Could not save the setting. Please try again.",
    es: 'No se pudo guardar el ajuste. Inténtalo de nuevo.',
    de: 'Die Einstellung konnte nicht gespeichert werden. Bitte erneut versuchen.',
    ru: "Не удалось сохранить настройку. Попробуйте ещё раз.",
    tr: "Ayar kaydedilemedi. Lütfen tekrar deneyin.",
    ptBr: "Não foi possível salvar a configuração. Tente novamente.",
    zhHans: "无法保存设置，请重试。",
    zhHant: "無法儲存設定，請重試。",
    ko: "설정을 저장하지 못했습니다. 다시 시도하세요.",
  );
  String get hideFromRecentsTitle => tr(
    en: "Hide from recent apps",
    es: 'Ocultar de aplicaciones recientes',
    de: 'Aus zuletzt verwendeten Apps ausblenden',
    ru: "Скрывать из недавних приложений",
    tr: "Son uygulamalardan gizle",
    ptBr: "Ocultar dos apps recentes",
    zhHans: "在最近使用的应用中隐藏",
    zhHant: "在最近使用的應用程式中隱藏",
    ko: "최근 앱에서 숨기기",
  );
  String get hideFromRecentsDescription => tr(
    en: "hide LiveBridge in the app switcher. This does not keep it running in the background",
    es: 'oculta LiveBridge en la lista de aplicaciones recientes. Esto no garantiza que siga funcionando en segundo plano',
    de: 'blendet LiveBridge in der Übersicht zuletzt verwendeter Apps aus. Dies hält die App nicht im Hintergrund aktiv',
    ru: "скрывать LiveBridge в списке недавних приложений. Это не защищает от остановки в фоне",
    tr: "LiveBridge uygulamasını son uygulamalar listesinden gizler. Arka planda çalışmasını garanti etmez",
    ptBr:
        "oculta o LiveBridge nos apps recentes. Isso não garante sua execução em segundo plano",
    zhHans: "在最近使用的应用列表中隐藏 LiveBridge。这不能保证后台运行。",
    zhHant: "在最近使用的應用程式列表中隱藏 LiveBridge。這不能保證背景執行。",
    ko: "최근 앱 목록에서 LiveBridge를 숨깁니다. 백그라운드 실행을 보장하지는 않습니다.",
  );
  String get sourceChannelsTitle => tr(
    en: "Source notification channels",
    es: 'Canales de notificación de origen',
    de: 'Kanäle der Quellbenachrichtigungen',
    ru: "Каналы исходных уведомлений",
    tr: "Kaynak bildirim kanalları",
    ptBr: "Canais de notificação de origem",
    zhHans: "源通知渠道",
    zhHant: "來源通知管道",
    ko: "원본 알림 채널",
  );
  String get sourceChannelsDescription => tr(
    en: "Choose which channels LiveBridge can convert. Original notifications are unaffected.",
    es: 'Elige qué canales puede convertir LiveBridge. Las notificaciones originales se conservan.',
    de: 'Wähle aus, welche Kanäle LiveBridge umwandeln darf. Die Originalbenachrichtigungen bleiben erhalten.',
    ru: "Выберите, какие каналы LiveBridge может конвертировать. Исходные уведомления сохраняются.",
    tr: "LiveBridge uygulamasının dönüştürebileceği kanalları seçin. Orijinal bildirimler etkilenmez.",
    ptBr:
        "Escolha quais canais o LiveBridge pode converter. As notificações originais não são afetadas.",
    zhHans: "选择 LiveBridge 可以转换的渠道。原始通知不受影响。",
    zhHant: "選擇 LiveBridge 可以轉換的管道。原始通知不受影響。",
    ko: "LiveBridge가 변환할 채널을 선택하세요. 원본 알림에는 영향을 주지 않습니다.",
  );

  String get sourceChannelsHint => tr(
    en: "choose which channels LiveBridge can convert. Original notifications are unaffected",
    es: 'elige qué canales puede convertir LiveBridge. Las notificaciones originales se conservan',
    de: 'wähle aus, welche Kanäle LiveBridge umwandeln darf. Die Originalbenachrichtigungen bleiben erhalten',
    ru: "выберите, какие каналы LiveBridge может конвертировать. Исходные уведомления сохраняются",
    tr: "LiveBridge uygulamasının dönüştürebileceği kanalları seçin. Orijinal bildirimler etkilenmez",
    ptBr:
        "escolha quais canais o LiveBridge pode converter. As notificações originais não são afetadas",
    zhHans: "选择 LiveBridge 可以转换的渠道。原始通知不受影响。",
    zhHant: "選擇 LiveBridge 可以轉換的管道。原始通知不受影響。",
    ko: "LiveBridge가 변환할 채널을 선택하세요. 원본 알림에는 영향을 주지 않습니다.",
  );
  String get sourceChannelsEmpty => tr(
    en: "Channels appear after their apps post notifications while notification access is enabled. Refresh this list after receiving a notification.",
    es: 'Los canales aparecen cuando sus aplicaciones envían notificaciones con el acceso a notificaciones activado. Actualiza esta lista después de recibir una notificación.',
    de: 'Kanäle erscheinen, sobald ihre Apps bei aktiviertem Benachrichtigungszugriff eine Benachrichtigung senden. Aktualisiere die Liste nach dem Empfang einer Benachrichtigung.',
    ru: "Каналы появятся после поступления уведомлений при включённом доступе к ним. Получите уведомление и обновите список.",
    tr: "Bildirim erişimi açıkken uygulamalar bildirim gönderdiğinde kanallar görünür. Bildirim aldıktan sonra listeyi yenileyin.",
    ptBr:
        "Os canais aparecem quando os apps enviam notificações com o acesso ativado. Atualize a lista após receber uma notificação.",
    zhHans: "启用通知访问权限后，应用发送通知时会出现渠道。收到通知后请刷新列表。",
    zhHant: "啟用通知存取權後，應用程式傳送通知時會出現管道。收到通知後請重新整理列表。",
    ko: "알림 접근 권한이 켜진 상태에서 앱이 알림을 보내면 채널이 표시됩니다. 알림을 받은 뒤 목록을 새로고침하세요.",
  );
  String get sourceChannelsSearch => tr(
    en: "Search apps and channels",
    es: 'Buscar aplicaciones y canales',
    de: 'Apps und Kanäle suchen',
    ru: "Поиск приложений и каналов",
    tr: "Uygulama ve kanal ara",
    ptBr: "Buscar apps e canais",
    zhHans: "搜索应用和渠道",
    zhHant: "搜尋應用程式和管道",
    ko: "앱 및 채널 검색",
  );
  String get sourceChannelsError => tr(
    en: "Could not load or save channels. Please try again.",
    es: 'No se pudieron cargar o guardar los canales. Inténtalo de nuevo.',
    de: 'Kanäle konnten nicht geladen oder gespeichert werden. Bitte erneut versuchen.',
    ru: "Не удалось загрузить или сохранить каналы. Попробуйте ещё раз.",
    tr: "Kanallar yüklenemedi veya kaydedilemedi. Lütfen tekrar deneyin.",
    ptBr: "Não foi possível carregar ou salvar os canais. Tente novamente.",
    zhHans: "无法加载或保存渠道，请重试。",
    zhHant: "無法載入或儲存管道，請重試。",
    ko: "채널을 불러오거나 저장하지 못했습니다. 다시 시도하세요.",
  );
  String get sourceChannelsRefresh => tr(
    en: "Refresh",
    es: 'Actualizar',
    de: 'Aktualisieren',
    ru: "Обновить",
    tr: "Yenile",
    ptBr: "Atualizar",
    zhHans: "刷新",
    zhHant: "重新整理",
    ko: "새로고침",
  );

  static AppStrings of(BuildContext context) {
    return AppStrings(locale: Localizations.localeOf(context));
  }

  String get refresh => tr(
    en: 'Refresh',
    es: 'Actualizar',
    de: 'Aktualisieren',
    ru: 'Обновить',
    tr: 'Yenile',
    ptBr: 'Atualizar',
    zhHans: '刷新',
    zhHant: '重新整理',
    ko: '새로고침',
  );

  String get permissionGranted => tr(
    en: 'Notification permission granted.',
    es: 'Permiso de notificaciones concedido.',
    de: 'Benachrichtigungsberechtigung erteilt.',
    ru: 'Разрешение на уведомления выдано.',
    tr: 'Bildirim izni verildi.',
    ptBr: 'Permissão de notificações concedida.',
    zhHans: '通知权限已授予。',
    zhHant: '通知權限已授予。',
    ko: '알림 권한이 부여되었습니다.',
  );

  String get permissionDenied => tr(
    en: 'Notification permission was not granted.',
    es: 'No se concedió el permiso de notificaciones.',
    de: 'Benachrichtigungsberechtigung nicht erteilt.',
    ru: 'Разрешение на уведомления не выдано.',
    tr: 'Bildirim izni verilmedi.',
    ptBr: 'Permissão de notificações não concedida.',
    zhHans: '未授予通知权限。',
    zhHant: '未授予通知權限。',
    ko: '알림 권한이 부여되지 않았습니다.',
  );

  String get listenerUnavailable => tr(
    en: 'Unable to open Listener settings on this device.',
    es: 'No se pueden abrir los ajustes de acceso a notificaciones en este dispositivo.',
    de: 'Die Einstellungen für den Benachrichtigungszugriff können auf diesem Gerät nicht geöffnet werden.',
    ru: 'Не удалось открыть настройки Listener.',
    tr: 'Bu cihazda Listener ayarları açılamıyor.',
    ptBr:
        'Não foi possível abrir as configurações do Listener neste dispositivo.',
    zhHans: '此设备无法打开监听器设置。',
    zhHant: '此裝置無法開啟監聽器設定。',
    ko: '기기의 알림 읽기 설정을 열 수 없습니다.',
  );

  String get notificationsUnavailable => tr(
    en: 'Unable to open app notification settings.',
    es: 'No se pueden abrir los ajustes de notificaciones de la aplicación.',
    de: 'Die Benachrichtigungseinstellungen der App können nicht geöffnet werden.',
    ru: 'Не удалось открыть настройки уведомлений.',
    tr: 'Uygulama bildirim ayarları açılamıyor.',
    ptBr: 'Não foi possível abrir as configurações de notificação do app.',
    zhHans: '无法打开应用通知设置。',
    zhHant: '無法開啟應用通知設定。',
    ko: '앱 알림 설정을 열 수 없습니다.',
  );

  String get liveUpdatesUnavailable => tr(
    en: 'Unable to open Live Updates settings on this device.',
    es: 'No se pueden abrir los ajustes de Live Updates en este dispositivo.',
    de: 'Die Live-Updates-Einstellungen können auf diesem Gerät nicht geöffnet werden.',
    ru: 'Не удалось открыть настройки Live Updates.',
    tr: 'Bu cihazda Live Updates ayarları açılamıyor.',
    ptBr:
        'Não foi possível abrir as configurações de Live Updates neste dispositivo.',
    zhHans: '此设备无法打开 Live Updates 设置。',
    zhHant: '此裝置無法開啟 Live Updates 設定。',
    ko: '기기의 Live Updates 설정을 열 수 없습니다.',
  );

  String get githubOpenFailed => tr(
    en: 'Unable to open GitHub link.',
    es: 'No se puede abrir el enlace de GitHub.',
    de: 'Der GitHub-Link kann nicht geöffnet werden.',
    ru: 'Не удалось открыть ссылку GitHub.',
    tr: 'GitHub bağlantısı açılamıyor.',
    ptBr: 'Não foi possível abrir o link do GitHub.',
    zhHans: '无法打开 GitHub 链接。',
    zhHant: '無法開啟 GitHub 連結。',
    ko: 'GitHub 링크를 열 수 없습니다',
  );

  String get linkOpenFailed => tr(
    en: 'Unable to open link.',
    es: 'No se puede abrir el enlace.',
    de: 'Der Link kann nicht geöffnet werden.',
    ru: 'Не удалось открыть ссылку.',
    tr: 'Bağlantı açılamıyor.',
    ptBr: 'Não foi possível abrir o link.',
    zhHans: '无法打开链接。',
    zhHant: '無法開啟連結。',
    ko: '링크를 열 수 없습니다.',
  );

  String get updateCheckFailed => tr(
    en: 'Unable to check updates. Try disabling VPN.',
    es: 'No se pudieron buscar actualizaciones. Prueba a desactivar la VPN.',
    de: 'Updates konnten nicht geprüft werden. Versuche, die VPN zu deaktivieren.',
    ru: 'Не удалось проверить обновления. Попробуйте отключить VPN.',
    tr: 'Güncellemeler denetlenemiyor. VPN\'i kapatmayı deneyin.',
    ptBr: 'Não foi possível verificar atualizações. Tente desativar a VPN.',
    zhHans: '无法检查更新。请尝试关闭 VPN。',
    zhHant: '無法檢查更新。請嘗試關閉 VPN。',
    ko: '업데이트를 확인할 수 없습니다. VPN을 비활성화 해보세요.',
  );

  String get dictionaryEmpty => tr(
    en: 'Dictionary is empty or invalid.',
    es: 'El diccionario está vacío o no es válido.',
    de: 'Das Wörterbuch ist leer oder ungültig.',
    ru: 'Словарь пустой или поврежден.',
    tr: 'Sözlük boş veya geçersiz.',
    ptBr: 'O dicionário está vazio ou inválido.',
    zhHans: '词典为空或无效。',
    zhHant: '字典為空或無效。',
    ko: '사전이 비어있거나 문제가 있습니다.',
  );

  String get dictionaryUpdateDone => tr(
    en: 'Dictionary updated from GitHub.',
    es: 'Diccionario actualizado desde GitHub.',
    de: 'Wörterbuch von GitHub aktualisiert.',
    ru: 'Словарь обновлен из GitHub.',
    tr: 'Sözlük GitHub\'dan güncellendi.',
    ptBr: 'Dicionário atualizado do GitHub.',
    zhHans: '词典已从 GitHub 更新。',
    zhHant: '字典已從 GitHub 更新。',
    ko: 'GitHub를 통해 사전을 업데이트했습니다.',
  );

  String get dictionaryInvalid => tr(
    en: 'Invalid dictionary JSON.',
    es: 'El JSON del diccionario no es válido.',
    de: 'Ungültiges Wörterbuch-JSON.',
    ru: 'Невалидный JSON словаря.',
    tr: 'Geçersiz sözlük JSON\'u.',
    ptBr: 'JSON do dicionário inválido.',
    zhHans: '词典 JSON 无效。',
    zhHant: '字典 JSON 無效。',
    ko: '사전 JSON에 문제가 있습니다.',
  );

  String get dictionaryUpdateFailed => tr(
    en: 'Failed to update dictionary from GitHub.',
    es: 'No se pudo actualizar el diccionario desde GitHub.',
    de: 'Das Wörterbuch konnte nicht von GitHub aktualisiert werden.',
    ru: 'Не удалось обновить словарь из GitHub.',
    tr: 'Sözlük GitHub\'dan güncellenemedi.',
    ptBr: 'Falha ao atualizar o dicionário do GitHub.',
    zhHans: '从 GitHub 更新词典失败。',
    zhHant: '從 GitHub 更新字典失敗。',
    ko: 'GitHub에서 사전을 업데이트할 수 없습니다.',
  );

  String get dictionaryTitle => tr(
    en: 'Dictionary',
    es: 'Diccionario',
    de: 'Wörterbuch',
    ru: 'Словарь',
    tr: 'Sözlük',
    ptBr: 'Dicionário',
    zhHans: '词典',
    zhHant: '字典',
    ko: '사전',
  );

  String get dictionaryManageSubtitle => tr(
    en: 'tap to manage',
    es: 'pulsa para gestionar',
    de: 'zum Verwalten tippen',
    ru: 'нажмите для управления',
    tr: 'yönetmek için açın',
    ptBr: 'toque para gerenciar',
    zhHans: '点按以管理',
    zhHant: '點按以管理',
    ko: '눌러서 관리하기',
  );

  String get dictionaryLanguagesTitle => tr(
    en: 'Dictionary languages',
    es: 'Idiomas del diccionario',
    de: 'Wörterbuchsprachen',
    ru: 'Языки словаря',
    tr: 'Sözlük dilleri',
    ptBr: 'Idiomas do dicionário',
    zhHans: '词典语言',
    zhHant: '字典語言',
    ko: '사전 언어',
  );

  String get dictionaryLanguagesSubtitle => tr(
    en: 'tap to choose',
    es: 'pulsa para elegir',
    de: 'zum Auswählen tippen',
    ru: 'нажмите для выбора',
    tr: 'seçmek için açın',
    ptBr: 'toque para escolher',
    zhHans: '点按以选择',
    zhHant: '點按以選擇',
    ko: '눌러서 선택하기',
  );

  String get dictionaryLanguagesPickerTitle => tr(
    en: 'Select languages for conversion',
    es: 'Seleccionar idiomas para la conversión',
    de: 'Sprachen für die Umwandlung auswählen',
    ru: 'Выберите языки для конвертации',
    tr: 'Dönüştürme için dilleri seçin',
    ptBr: 'Selecione idiomas para conversão',
    zhHans: '选择用于转换的语言',
    zhHant: '選擇用於轉換的語言',
    ko: '변환용 언어 선택',
  );

  String get dictionaryUpdateAction => tr(
    en: 'Update dictionaries',
    es: 'Actualizar diccionarios',
    de: 'Wörterbücher aktualisieren',
    ru: 'Обновить словари',
    tr: 'Sözlükleri güncelle',
    ptBr: 'Atualizar dicionários',
    zhHans: '更新词典',
    zhHant: '更新字典',
    ko: '사전 업데이트',
  );

  String get dictionaryUpdateDescription => tr(
    en: 'downloads the latest parser dictionaries for enabled languages',
    es: 'descarga los últimos diccionarios de reconocimiento para los idiomas activados',
    de: 'lädt die neuesten Erkennungswörterbücher für aktivierte Sprachen herunter',
    ru: 'загружает свежие словари парсинга для включенных языков',
    tr: 'etkin diller için en güncel ayrıştırma sözlüklerini indirir',
    ptBr:
        'baixa os dicionários de análise mais recentes para os idiomas ativados',
    zhHans: '下载已启用语言的最新解析词典',
    zhHant: '下載已啟用語言的最新解析字典',
    ko: '활성화된 언어들의 최신 파싱 사전을 다운로드합니다.',
  );

  String get dictionaryEditorTitle => tr(
    en: 'Dictionary editor',
    es: 'Editor de diccionarios',
    de: 'Wörterbucheditor',
    ru: 'Редактор словаря',
    tr: 'Sözlük düzenleyici',
    ptBr: 'Editor de dicionário',
    zhHans: '词典编辑器',
    zhHant: '字典編輯器',
    ko: '사전 편집기',
  );

  String get dictionaryEditorDescription => tr(
    en: 'in-app dictionary editing will be added later',
    es: 'la edición de diccionarios en la aplicación se añadirá más adelante',
    de: 'die Wörterbuchbearbeitung in der App wird später ergänzt',
    ru: 'редактирование словарей внутри приложения появится позже',
    tr: 'uygulama içi sözlük düzenleme daha sonra eklenecek',
    ptBr: 'a edição de dicionários dentro do app será adicionada depois',
    zhHans: '应用内词典编辑会稍后添加',
    zhHant: '應用程式內字典編輯稍後會加入',
    ko: '앱 내 사전 편집기는 지원 예정입니다.',
  );

  String get dictionaryComingSoon => tr(
    en: '(coming soon)',
    es: '(próximamente)',
    de: '(demnächst)',
    ru: '(скоро)',
    tr: '(yakında)',
    ptBr: '(em breve)',
    zhHans: '（即将推出）',
    zhHant: '（即將推出）',
    ko: '(지원 예정)',
  );

  String get navHome => tr(
    en: 'Home',
    es: 'Inicio',
    de: 'Start',
    ru: 'Домой',
    tr: 'Ana sayfa',
    ptBr: 'Início',
    zhHans: '主页',
    zhHant: '首頁',
    ko: '홈',
  );

  String get navRules => tr(
    en: 'Rules',
    es: 'Reglas',
    de: 'Regeln',
    ru: 'Правила',
    tr: 'Kurallar',
    ptBr: 'Regras',
    zhHans: '规则',
    zhHant: '規則',
    ko: '규칙',
  );

  String get navSettings => tr(
    en: 'Settings',
    es: 'Ajustes',
    de: 'Einstellungen',
    ru: 'Настройки',
    tr: 'Ayarlar',
    ptBr: 'Configurações',
    zhHans: '设置',
    zhHant: '設定',
    ko: '설정',
  );

  String get redesignRulesTitle => tr(
    en: 'Rules',
    es: 'Reglas',
    de: 'Regeln',
    ru: 'Правила',
    tr: 'Kurallar',
    ptBr: 'Regras',
    zhHans: '规则',
    zhHant: '規則',
    ko: '규칙',
  );

  String get appConfigTitle => tr(
    en: 'App config',
    es: 'Configuración de la aplicación',
    de: 'App-Konfiguration',
    ru: 'Настройки приложения',
    tr: 'Uygulama ayarları',
    ptBr: 'Configurações do app',
    zhHans: '应用设置',
    zhHant: '應用設定',
    ko: '앱 설정',
  );

  String get backupRestoreTitle => tr(
    en: 'Backup & Restore',
    es: 'Copia de seguridad y restauración',
    de: 'Sichern und Wiederherstellen',
    ru: 'Backup & Restore',
    tr: 'Yedekleme ve geri yükleme',
    ptBr: 'Backup e restauração',
    zhHans: '备份与恢复',
    zhHant: '備份與還原',
    ko: '백업 & 복구',
  );

  String get exportLiveBridgeSettingsTitle => tr(
    en: 'Export LiveBridge settings',
    es: 'Exportar ajustes de LiveBridge',
    de: 'LiveBridge-Einstellungen exportieren',
    ru: 'Экспорт настроек LiveBridge',
    tr: 'LiveBridge ayarlarını dışa aktar',
    ptBr: 'Exportar configurações do LiveBridge',
    zhHans: '导出 LiveBridge 设置',
    zhHant: '匯出 LiveBridge 設定',
    ko: 'LiveBridge 설정 내보내기',
  );

  String get importLiveBridgeSettingsTitle => tr(
    en: 'Import LiveBridge settings',
    es: 'Importar ajustes de LiveBridge',
    de: 'LiveBridge-Einstellungen importieren',
    ru: 'Импорт настроек LiveBridge',
    tr: 'LiveBridge ayarlarını içe aktar',
    ptBr: 'Importar configurações do LiveBridge',
    zhHans: '导入 LiveBridge 设置',
    zhHant: '匯入 LiveBridge 設定',
    ko: 'LiveBridge 설정 불러오기',
  );

  String get importFromDebugTitle => tr(
    en: 'Import from debug JSON',
    es: 'Importar desde JSON de diagnóstico',
    de: 'Aus Diagnose-JSON importieren',
    ru: 'Импорт из debug JSON',
    tr: 'Debug JSON’dan içe aktar',
    ptBr: 'Importar do debug JSON',
    zhHans: '从 debug JSON 导入',
    zhHant: '從 debug JSON 匯入',
    ko: 'debug JSON에서 불러오기',
  );

  String get liveBridgeSettingsExported => tr(
    en: 'LiveBridge settings exported.',
    es: 'Ajustes de LiveBridge exportados.',
    de: 'LiveBridge-Einstellungen exportiert.',
    ru: 'Настройки LiveBridge экспортированы.',
    tr: 'LiveBridge ayarları dışa aktarıldı.',
    ptBr: 'Configurações do LiveBridge exportadas.',
    zhHans: 'LiveBridge 设置已导出。',
    zhHant: 'LiveBridge 設定已匯出。',
    ko: 'LiveBridge 설정을 내보냈습니다.',
  );

  String get liveBridgeSettingsExportFailed => tr(
    en: 'Failed to export LiveBridge settings.',
    es: 'No se pudieron exportar los ajustes de LiveBridge.',
    de: 'LiveBridge-Einstellungen konnten nicht exportiert werden.',
    ru: 'Не удалось экспортировать настройки LiveBridge.',
    tr: 'LiveBridge ayarları dışa aktarılamadı.',
    ptBr: 'Falha ao exportar configurações do LiveBridge.',
    zhHans: '导出 LiveBridge 设置失败。',
    zhHant: '匯出 LiveBridge 設定失敗。',
    ko: 'LiveBridge 설정을 내보낼 수 없습니다.',
  );

  String get liveBridgeSettingsImported => tr(
    en: 'LiveBridge settings imported.',
    es: 'Ajustes de LiveBridge importados.',
    de: 'LiveBridge-Einstellungen importiert.',
    ru: 'Настройки LiveBridge импортированы.',
    tr: 'LiveBridge ayarları içe aktarıldı.',
    ptBr: 'Configurações do LiveBridge importadas.',
    zhHans: 'LiveBridge 设置已导入。',
    zhHant: 'LiveBridge 設定已匯入。',
    ko: 'LiveBridge 설정을 불러왔습니다.',
  );

  String get liveBridgeSettingsImportFailed => tr(
    en: 'Failed to import LiveBridge settings.',
    es: 'No se pudieron importar los ajustes de LiveBridge.',
    de: 'LiveBridge-Einstellungen konnten nicht importiert werden.',
    ru: 'Не удалось импортировать настройки LiveBridge.',
    tr: 'LiveBridge ayarları içe aktarılamadı.',
    ptBr: 'Falha ao importar configurações do LiveBridge.',
    zhHans: '导入 LiveBridge 设置失败。',
    zhHant: '匯入 LiveBridge 設定失敗。',
    ko: 'LiveBridge 설정을 불러올 수 없습니다.',
  );

  String get copyOldDebugJsonFirst => tr(
    en: 'please copy your old debug JSON first',
    es: 'copia primero tu antiguo JSON de diagnóstico',
    de: 'kopiere zuerst dein bisheriges Diagnose-JSON',
    ru: 'сначала скопируйте старый debug JSON',
    tr: 'lütfen önce eski debug JSON’unuzu kopyalayın',
    ptBr: 'copie seu debug JSON antigo primeiro',
    zhHans: '请先复制旧的 debug JSON',
    zhHant: '請先複製舊的 debug JSON',
    ko: '구버전 debug JSON을 먼저 복사해주세요',
  );

  String get appLanguageTitle => tr(
    en: 'App language',
    es: 'Idioma de la aplicación',
    de: 'App-Sprache',
    ru: 'Язык приложения',
    tr: 'Uygulama dili',
    ptBr: 'Idioma do app',
    zhHans: '应用语言',
    zhHant: '應用語言',
    ko: '앱 언어',
  );

  String get appLanguagePickerTitle => tr(
    en: 'Choose app language',
    es: 'Elegir idioma de la aplicación',
    de: 'App-Sprache auswählen',
    ru: 'Выберите язык приложения',
    tr: 'Uygulama dilini seçin',
    ptBr: 'Escolha o idioma do app',
    zhHans: '选择应用语言',
    zhHant: '選擇應用語言',
    ko: '앱 언어 선택',
  );

  String get appLanguageSystem => tr(
    en: 'Auto',
    es: 'Automático',
    de: 'Automatisch',
    ru: 'Автовыбор',
    tr: 'Otomatik',
    ptBr: 'Automático',
    zhHans: '自动',
    zhHant: '自動',
    ko: '자동',
  );

  String get brandSpecificTitle => tr(
    en: 'Brand-specific',
    es: 'Ajustes del fabricante',
    de: 'Herstellerspezifisch',
    ru: 'Brand-specific',
    tr: 'Markaya özel',
    ptBr: 'Específico da marca',
    zhHans: '品牌特定',
    zhHant: '品牌特定',
    ko: '브랜드별 기능',
  );

  String get appUpdatesTitle => tr(
    en: 'App updates',
    es: 'Actualizaciones de la aplicación',
    de: 'App-Updates',
    ru: 'Обновления приложения',
    tr: 'Uygulama güncellemeleri',
    ptBr: 'Atualizações do app',
    zhHans: '应用更新',
    zhHant: '應用更新',
    ko: '앱 업데이트',
  );

  String get statusRunning => tr(
    en: 'Running',
    es: 'En funcionamiento',
    de: 'Aktiv',
    ru: 'Запущен',
    tr: 'Çalışıyor',
    ptBr: 'Em execução',
    zhHans: '正在运行',
    zhHant: '正在執行',
    ko: '실행 중',
  );

  String get statusDisabled => tr(
    en: 'LiveBridge is disabled',
    es: 'LiveBridge está desactivado',
    de: 'LiveBridge ist deaktiviert',
    ru: 'LiveBridge выключен',
    tr: 'LiveBridge devre dışı',
    ptBr: 'LiveBridge está desativado',
    zhHans: 'LiveBridge 已关闭',
    zhHant: 'LiveBridge 已關閉',
    ko: 'LiveBridge 꺼짐',
  );

  String get statusByPrefix => tr(
    en: 'by ',
    es: 'por ',
    de: 'von ',
    ru: 'by ',
    tr: 'by ',
    ptBr: 'por ',
    zhHans: '由',
    zhHant: '由',
    ko: '개발자: ',
  );

  String get discussTitle => tr(
    en: 'Discuss',
    es: 'Comunidad',
    de: 'Austausch',
    ru: 'Discuss',
    tr: 'Tartış',
    ptBr: 'Discutir',
    zhHans: '讨论',
    zhHant: '討論',
    ko: '커뮤니티',
  );

  String get discussSubtitle => tr(
    en: 'telegram topics',
    es: 'temas de Telegram',
    de: 'Telegram-Themen',
    ru: 'telegram topics',
    tr: 'telegram konuları',
    ptBr: 'tópicos do Telegram',
    zhHans: 'telegram 话题',
    zhHant: 'telegram 話題',
    ko: '텔레그램 주제',
  );

  String get rulesModeAllApps => tr(
    en: 'all apps',
    es: 'todas las aplicaciones',
    de: 'alle Apps',
    ru: 'все приложения',
    tr: 'tüm uygulamalar',
    ptBr: 'todos os apps',
    zhHans: '所有应用',
    zhHant: '所有應用程式',
    ko: '모든 앱',
  );

  String get rulesModeOnlySelected => tr(
    en: 'only selected',
    es: 'solo las seleccionadas',
    de: 'nur ausgewählte',
    ru: 'только выбранные',
    tr: 'yalnızca seçilenler',
    ptBr: 'somente selecionados',
    zhHans: '仅已选择',
    zhHant: '僅已選取',
    ko: '선택한 앱만 포함',
  );

  String get rulesModeExcludeSelected => tr(
    en: 'exclude selected',
    es: 'excluir las seleccionadas',
    de: 'ausgewählte ausschließen',
    ru: 'исключая выбранные',
    tr: 'seçilenleri hariç tut',
    ptBr: 'excluir selecionados',
    zhHans: '排除已选择',
    zhHant: '排除已選取',
    ko: '선택합 앱만 제외',
  );

  String get permissionCheckRequired => tr(
    en: 'check required',
    es: 'requiere comprobación',
    de: 'Prüfung erforderlich',
    ru: 'требуется проверка',
    tr: 'kontrol gerekli',
    ptBr: 'verificação necessária',
    zhHans: '需要检查',
    zhHant: '需要檢查',
    ko: '확인 필요',
  );

  String get permissionsAllSet => tr(
    en: 'all set',
    es: 'todo listo',
    de: 'alles bereit',
    ru: 'всё хорошо',
    tr: 'hazır',
    ptBr: 'tudo certo',
    zhHans: '已就绪',
    zhHant: '已就緒',
    ko: '정상',
  );

  String get versionTapToUpdate => tr(
    en: 'tap to update',
    es: 'pulsa para actualizar',
    de: 'zum Aktualisieren tippen',
    ru: 'нажмите для обновления',
    tr: 'güncellemek için dokunun',
    ptBr: 'toque para atualizar',
    zhHans: '点按更新',
    zhHant: '點按更新',
    ko: '눌러서 업데이트',
  );

  String get versionLatestVersion => tr(
    en: 'latest version',
    es: 'última versión',
    de: 'neueste Version',
    ru: 'последняя версия',
    tr: 'son sürüm',
    ptBr: 'versão mais recente',
    zhHans: '最新版本',
    zhHant: '最新版本',
    ko: '최신 버전',
  );

  String get recentConversions => tr(
    en: 'Recent conversions',
    es: 'Conversiones recientes',
    de: 'Letzte Umwandlungen',
    ru: 'Последние конвертации',
    tr: 'Son dönüştürmeler',
    ptBr: 'Conversões recentes',
    zhHans: '最近转换',
    zhHant: '最近轉換',
    ko: '최근 변환 기록',
  );

  String get noConversionsYet => tr(
    en: 'no conversions yet',
    es: 'aún no hay conversiones',
    de: 'noch keine Umwandlungen',
    ru: 'конвертаций пока нет',
    tr: 'henüz dönüştürme yok',
    ptBr: 'nenhuma conversão ainda',
    zhHans: '暂无转换',
    zhHant: '暫無轉換',
    ko: '변환 내역 없음',
  );

  String get conversionLogDisabled => tr(
    en: 'conversion log is disabled',
    es: 'el registro de conversiones está desactivado',
    de: 'das Umwandlungsprotokoll ist deaktiviert',
    ru: 'лог конвертаций выключен',
    tr: 'dönüştürme günlüğü kapalı',
    ptBr: 'o log de conversões está desativado',
    zhHans: '转换日志已关闭',
    zhHant: '轉換記錄已關閉',
    ko: '변환 내역이 꺼져있음',
  );

  String get enable => tr(
    en: 'enable',
    es: 'activar',
    de: 'aktivieren',
    ru: 'включить',
    tr: 'etkinleştir',
    ptBr: 'ativar',
    zhHans: '启用',
    zhHant: '啟用',
    ko: '활성화',
  );

  String get payloadCopied => tr(
    en: 'Payload copied',
    es: 'Datos copiados',
    de: 'Daten kopiert',
    ru: 'Payload скопирован',
    tr: 'Payload kopyalandı',
    ptBr: 'Payload copiado',
    zhHans: 'Payload 已复制',
    zhHant: 'Payload 已複製',
    ko: 'Payload 복사됨',
  );

  String get progressTitle => tr(
    en: 'Progress',
    es: 'Progreso',
    de: 'Fortschritt',
    ru: 'Прогресс',
    tr: 'İlerleme',
    ptBr: 'Barras de Progresso (Downloads/Mídia)',
    zhHans: '进度',
    zhHant: '進度',
    ko: '진행률',
  );

  String get nativeProgressTitle => tr(
    en: 'Native progress',
    es: 'Progreso nativo',
    de: 'Nativer Fortschritt',
    ru: 'Нативный прогресс',
    tr: 'Yerel ilerleme',
    ptBr: 'Barras de progresso do sistema',
    zhHans: '原生进度',
    zhHant: '原生進度',
    ko: '네이티브 진행률',
  );

  String get otpCodesTitle => tr(
    en: 'OTP codes',
    es: 'Códigos OTP',
    de: 'OTP-Codes',
    ru: 'OTP-коды',
    tr: 'OTP kodları',
    ptBr: 'Códigos OTP',
    zhHans: 'OTP 验证码',
    zhHant: 'OTP 驗證碼',
    ko: 'OTP 코드',
  );

  String get autoCopyCodeTitle => tr(
    en: 'Auto-copy code',
    es: 'Copiar código automáticamente',
    de: 'Code automatisch kopieren',
    ru: 'Автокопирование кода',
    tr: 'Kodu otomatik kopyala',
    ptBr: 'Copiar código automaticamente',
    zhHans: '自动复制验证码',
    zhHant: '自動複製驗證碼',
    ko: '코드 자동 복사',
  );

  String get smartConversionTitle => tr(
    en: 'Smart conversion',
    es: 'Conversión inteligente',
    de: 'Intelligente Umwandlung',
    ru: 'Умная конвертация',
    tr: 'Akıllı dönüştürme',
    ptBr: 'Conversão inteligente',
    zhHans: '智能转换',
    zhHant: '智慧轉換',
    ko: '지능형 변환',
  );

  String get taxiTitle => tr(
    en: 'Taxi',
    es: 'Taxi',
    de: 'Taxi',
    ru: 'Такси',
    tr: 'Taksi',
    ptBr: 'Táxi',
    zhHans: '打车',
    zhHant: '叫車',
    ko: '택시',
  );

  String get deliveriesTitle => tr(
    en: 'Deliveries',
    es: 'Entregas',
    de: 'Lieferungen',
    ru: 'Доставки',
    tr: 'Teslimatlar',
    ptBr: 'Entregas',
    zhHans: '外卖',
    zhHant: '外送',
    ko: '배달',
  );

  String get removeOriginalMessageTitle => tr(
    en: 'Remove original message',
    es: 'Eliminar mensaje original',
    de: 'Originalnachricht entfernen',
    ru: 'Удалять исходное уведомление',
    tr: 'Orijinal bildirimi kaldır',
    ptBr: 'Remover notificação original (Evita notificações duplicadas)',
    zhHans: '移除原始通知',
    zhHant: '移除原始通知',
    ko: '기존 메시지 제거',
  );

  String get experimentalSuffix => tr(
    en: '(exp)',
    es: '(exp.)',
    de: '(exp.)',
    ru: '(exp)',
    tr: '(deneysel)',
    ptBr: '(exp)',
    zhHans: '（实验）',
    zhHant: '（實驗）',
    ko: '(불안정)',
  );

  String get allAppsTitle => tr(
    en: 'All apps',
    es: 'Todas las aplicaciones',
    de: 'Alle Apps',
    ru: 'Все приложения',
    tr: 'Tüm uygulamalar',
    ptBr: 'Todos os apps',
    zhHans: '所有应用',
    zhHant: '所有應用程式',
    ko: '모든 앱',
  );

  String get onlySelectedTitle => tr(
    en: 'Only selected',
    es: 'Solo las seleccionadas',
    de: 'Nur ausgewählte',
    ru: 'Только выбранные',
    tr: 'Yalnızca seçilenler',
    ptBr: 'Somente selecionados',
    zhHans: '仅已选择',
    zhHant: '僅已選取',
    ko: '선택한 앱만 포함',
  );

  String get excludeSelectedTitle => tr(
    en: 'Exclude selected',
    es: 'Excluir las seleccionadas',
    de: 'Ausgewählte ausschließen',
    ru: 'Исключить выбранные',
    tr: 'Seçilenleri hariç tut',
    ptBr: 'Excluir selecionados',
    zhHans: '排除已选择',
    zhHant: '排除已選取',
    ko: '선택한 앱만 제외',
  );

  String get conversionModeTitle => tr(
    en: 'Conversion mode',
    es: 'Modo de conversión',
    de: 'Umwandlungsmodus',
    ru: 'Режим конвертации',
    tr: 'Dönüştürme modu',
    ptBr: 'Modo de conversão',
    zhHans: '转换模式',
    zhHant: '轉換模式',
    ko: '변환 모드',
  );

  String get selectedAppsTitle => tr(
    en: 'Selected apps',
    es: 'Aplicaciones seleccionadas',
    de: 'Ausgewählte Apps',
    ru: 'Приложения',
    tr: 'Seçili uygulamalar',
    ptBr: 'Apps selecionados',
    zhHans: '已选择应用',
    zhHant: '已選取應用程式',
    ko: '선택한 앱',
  );

  String get showSystem => tr(
    en: 'show system',
    es: 'mostrar del sistema',
    de: 'System-Apps anzeigen',
    ru: 'показать системные',
    tr: 'sistem uygulamalarını göster',
    ptBr: 'mostrar aplicativos do sistema',
    zhHans: '显示系统',
    zhHant: '顯示系統',
    ko: '시스템 앱 포함',
  );

  String get hideSystem => tr(
    en: 'hide system',
    es: 'ocultar del sistema',
    de: 'System-Apps ausblenden',
    ru: 'скрыть системные',
    tr: 'sistem uygulamalarını gizle',
    ptBr: 'ocultar aplicativos do sistema',
    zhHans: '隐藏系统',
    zhHant: '隱藏系統',
    ko: '시스템 앱 제외',
  );

  String get networkConnectionsTitle => tr(
    en: 'Network & Connections',
    es: 'Red y conexiones',
    de: 'Netzwerk und Verbindungen',
    ru: 'Сеть и подключения',
    tr: 'Ağ ve Bağlantılar',
    ptBr: 'Rede e conexões',
    zhHans: '网络与连接',
    zhHant: '網路與連線',
    ko: '네트워크 및 연결',
  );

  String get vpnsTitle => tr(
    en: 'VPNs',
    es: 'VPN',
    de: 'VPNs',
    ru: 'VPN',
    tr: 'VPN\'ler',
    ptBr: 'VPNs',
    zhHans: 'VPN',
    zhHant: 'VPN',
    ko: 'VPN',
  );

  String get externalDevicesTitle => tr(
    en: 'External devices',
    es: 'Dispositivos externos',
    de: 'Externe Geräte',
    ru: 'Внешние устройства',
    tr: 'Harici cihazlar',
    ptBr: 'Dispositivos externos',
    zhHans: '外接设备',
    zhHant: '外接裝置',
    ko: '외부 장치',
  );

  String get ignoreDebuggingDevicesTitle => tr(
    en: 'Ignore debugging devices',
    es: 'Ignorar dispositivos de depuración',
    de: 'Debugging-Geräte ignorieren',
    ru: 'Игнорировать отладочные устройства',
    tr: 'Hata ayıklama cihazlarını yok say',
    ptBr: 'Ignorar dispositivos de depuração',
    zhHans: '忽略调试设备',
    zhHant: '忽略偵錯裝置',
    ko: '디버깅 기기 제외',
  );

  String get networkSpeedThresholdRedesignTitle => tr(
    en: 'Network speed threshold',
    es: 'Umbral de velocidad de red',
    de: 'Schwellenwert für die Netzwerkgeschwindigkeit',
    ru: 'Порог скорости сети',
    tr: 'Ağ hızı eşiği',
    ptBr: 'Limite de velocidade de rede',
    zhHans: '网速阈值',
    zhHant: '網速門檻',
    ko: '네트워크 속도 기준',
  );

  String get miscellaneousTitle => tr(
    en: 'Miscellaneous',
    es: 'Otros',
    de: 'Sonstiges',
    ru: 'Разное',
    tr: 'Diğer',
    ptBr: 'Diversos',
    zhHans: '其他',
    zhHant: '其他',
    ko: '기타',
  );

  String get navigationMapsTitle => tr(
    en: 'Navigation (maps)',
    es: 'Navegación (mapas)',
    de: 'Navigation (Karten)',
    ru: 'Навигация (карты)',
    tr: 'Navigasyon (haritalar)',
    ptBr: 'Navegação (mapas)',
    zhHans: '导航（地图）',
    zhHant: '導航（地圖）',
    ko: '내비게이션 (지도)',
  );

  String get mediaPlaybackRedesignTitle => tr(
    en: 'Media playback',
    es: 'Reproducción multimedia',
    de: 'Medienwiedergabe',
    ru: 'Медиа',
    tr: 'Medya oynatma',
    ptBr: 'Reprodução de mídia',
    zhHans: '媒体播放',
    zhHant: '媒體播放',
    ko: '미디어 재생',
  );

  String get callsTitle => tr(
    en: 'Calls',
    es: 'Llamadas',
    de: 'Anrufe',
    ru: 'Звонки',
    tr: 'Aramalar',
    ptBr: 'Chamadas',
    zhHans: '通话',
    zhHant: '通話',
    ko: '전화',
  );

  String get showMediaOnLockTitle => tr(
    en: 'Show media on lockscreen',
    es: 'Mostrar multimedia en la pantalla de bloqueo',
    de: 'Medien auf dem Sperrbildschirm anzeigen',
    ru: 'Медиа на экране блокировки',
    tr: 'Kilit ekranında medyayı göster',
    ptBr: 'Mostrar apenas na tela de bloqueio',
    zhHans: '在锁屏显示媒体',
    zhHant: '在鎖定畫面顯示媒體',
    ko: '잠금화면에 미디어 표시',
  );

  String get useSymbolsInMediaPlayerTitle => tr(
    en: 'Use symbols in media player',
    es: 'Usar símbolos en el reproductor',
    de: 'Symbole im Medienplayer verwenden',
    ru: 'Символы в медиаплеере',
    tr: 'Medya oynatıcıda semboller kullan',
    ptBr: 'Usar símbolos no reprodutor de mídia',
    zhHans: '在媒体播放器中使用符号',
    zhHant: '在媒體播放器中使用符號',
    ko: '미디어 플레이어에 기호 사용하기',
  );

  String get weatherBroadcastsTitle => tr(
    en: 'Weather broadcasts',
    es: 'Avisos meteorológicos',
    de: 'Wetterbenachrichtigungen',
    ru: 'Прогнозы погоды',
    tr: 'Hava durumu bildirimleri',
    ptBr: 'Alertas de clima',
    zhHans: '天气播报',
    zhHant: '天氣播報',
    ko: '일기예보',
  );

  String get bypassTitle => tr(
    en: 'Always convert',
    es: 'Convertir siempre',
    de: 'Immer umwandeln',
    ru: 'Всегда конвертировать',
    tr: 'Her zaman dönüştür',
    ptBr: 'Sempre converter',
    zhHans: '始终转换',
    zhHant: '一律轉換',
    ko: '항상 변환',
  );

  String get bypassDescription => tr(
    en: 'converts selected apps even when no rule matches',
    es: 'convierte las aplicaciones seleccionadas aunque no coincidan con ninguna regla',
    de: 'wandelt Benachrichtigungen ausgewählter Apps auch ohne passende Regel um',
    ru: 'конвертирует выбранные приложения, даже если правила не совпали',
    tr: 'hiçbir kural eşleşmese bile seçili uygulamaları dönüştürür',
    ptBr: 'converte os apps selecionados mesmo sem uma regra correspondente',
    zhHans: '即使没有匹配规则，也转换所选应用的通知',
    zhHant: '即使沒有符合規則，也轉換所選應用程式的通知',
    ko: '일치하는 규칙이 없어도 선택한 앱의 알림을 변환합니다',
  );

  String get perAppSettingsTitle => tr(
    en: 'Per-app settings',
    es: 'Ajustes por aplicación',
    de: 'Einstellungen pro App',
    ru: 'Настройки приложений',
    tr: 'Uygulama bazlı ayarlar',
    ptBr: 'Configurações por app',
    zhHans: '按应用设置',
    zhHant: '各應用設定',
    ko: '앱 별 설정',
  );

  String get defaultsTitle => tr(
    en: 'Defaults',
    es: 'Valores predeterminados',
    de: 'Standardeinstellungen',
    ru: 'По умолчанию',
    tr: 'Varsayılanlar',
    ptBr: 'Padrões',
    zhHans: '默认值',
    zhHant: '預設值',
    ko: '기본 설정',
  );

  String get defaultsSubtitle => tr(
    en: 'tap to change default behavior',
    es: 'pulsa para cambiar el comportamiento predeterminado',
    de: 'zum Ändern des Standardverhaltens tippen',
    ru: 'нажмите, чтобы изменить поведение',
    tr: 'varsayılan davranışı değiştirmek için dokunun',
    ptBr: 'toque para alterar o comportamento padrão',
    zhHans: '点按更改默认行为',
    zhHant: '點按變更預設行為',
    ko: '기본 변환 동작 변경',
  );

  String get appsListTitle => tr(
    en: 'Apps list',
    es: 'Lista de aplicaciones',
    de: 'App-Liste',
    ru: 'Список приложений',
    tr: 'Uygulama listesi',
    ptBr: 'Lista de apps',
    zhHans: '应用列表',
    zhHant: '應用程式清單',
    ko: '앱 목록',
  );

  String get exportLabel => tr(
    en: 'Export',
    es: 'Exportar',
    de: 'Exportieren',
    ru: 'Экспорт',
    tr: 'Dışa aktar',
    ptBr: 'Exportar',
    zhHans: '导出',
    zhHant: '匯出',
    ko: '내보내기',
  );

  String get importLabel => tr(
    en: 'Import',
    es: 'Importar',
    de: 'Importieren',
    ru: 'Импорт',
    tr: 'İçe aktar',
    ptBr: 'Importar',
    zhHans: '导入',
    zhHant: '匯入',
    ko: '불러오기',
  );

  String get titleSourceTitle => tr(
    en: 'Title source',
    es: 'Origen del título',
    de: 'Titelquelle',
    ru: 'Источник заголовка',
    tr: 'Başlık kaynağı',
    ptBr: 'Origem do título',
    zhHans: '标题来源',
    zhHant: '標題來源',
    ko: '제목',
  );

  String get contentSourceTitle => tr(
    en: 'Content source',
    es: 'Origen del contenido',
    de: 'Inhaltsquelle',
    ru: 'Источник контента',
    tr: 'İçerik kaynağı',
    ptBr: 'Origem do conteúdo',
    zhHans: '内容来源',
    zhHant: '內容來源',
    ko: '내용',
  );

  String get notificationTitleOption => tr(
    en: 'Notification title',
    es: 'Título de la notificación',
    de: 'Benachrichtigungstitel',
    ru: 'Заголовок уведомления',
    tr: 'Bildirim başlığı',
    ptBr: 'Título da notificação',
    zhHans: '通知标题',
    zhHant: '通知標題',
    ko: '알림 제목',
  );

  String get appTitleOption => tr(
    en: 'App title',
    es: 'Nombre de la aplicación',
    de: 'App-Name',
    ru: 'Название приложения',
    tr: 'Uygulama başlığı',
    ptBr: 'Título do app',
    zhHans: '应用标题',
    zhHant: '應用標題',
    ko: '앱 이름',
  );

  String get notificationTextOption => tr(
    en: 'Notification text',
    es: 'Texto de la notificación',
    de: 'Benachrichtigungstext',
    ru: 'Текст уведомления',
    tr: 'Bildirim metni',
    ptBr: 'Texto da notificação',
    zhHans: '通知文本',
    zhHant: '通知文字',
    ko: '알림 텍스트',
  );

  String get appUpdateNewVersionTitle => tr(
    en: 'New version available',
    es: 'Nueva versión disponible',
    de: 'Neue Version verfügbar',
    ru: 'Доступна новая версия',
    tr: 'Yeni sürüm mevcut',
    ptBr: 'Nova versão disponível',
    zhHans: '有新版本可用',
    zhHant: '有新版本可用',
    ko: '새 버전 이용 가능',
  );

  String get appUpdateCheckingTitle => tr(
    en: 'Checking for updates',
    es: 'Buscando actualizaciones',
    de: 'Updates werden gesucht',
    ru: 'Проверяем обновления',
    tr: 'Güncellemeler denetleniyor',
    ptBr: 'Verificando atualizações',
    zhHans: '正在检查更新',
    zhHant: '正在檢查更新',
    ko: '업데이트 확인',
  );

  String get appUpdateAllSetTitle => tr(
    en: 'You’re all set',
    es: 'Todo listo',
    de: 'Alles auf dem neuesten Stand',
    ru: 'Всё хорошо',
    tr: 'Her şey hazır',
    ptBr: 'Tudo pronto',
    zhHans: '已是最新',
    zhHant: '已是最新',
    ko: '준비 완료',
  );

  String get appUpdateDownloadsSubtitle => tr(
    en: 'tap to go to downloads',
    es: 'pulsa para ir a las descargas',
    de: 'zum Öffnen der Downloads tippen',
    ru: 'перейти к загрузке',
    tr: 'indirmelere gitmek için dokunun',
    ptBr: 'toque para ir aos downloads',
    zhHans: '点按前往下载',
    zhHant: '點按前往下載',
    ko: '눌러서 다운로드',
  );

  String get appUpdatePleaseWaitSubtitle => tr(
    en: 'please wait a moment',
    es: 'espera un momento',
    de: 'bitte kurz warten',
    ru: 'подождите немного',
    tr: 'lütfen biraz bekleyin',
    ptBr: 'aguarde um momento',
    zhHans: '请稍等',
    zhHant: '請稍候',
    ko: '잠시만 기다려주세요',
  );

  String get appUpdateLatestSubtitle => tr(
    en: 'latest version already',
    es: 'ya tienes la última versión',
    de: 'bereits die neueste Version',
    ru: 'установлена последняя версия',
    tr: 'zaten son sürüm',
    ptBr: 'já está na versão mais recente',
    zhHans: '已经是最新版本',
    zhHant: '已是最新版本',
    ko: '최신 버전이 준비됨',
  );

  String get appUpdateLogTitle => tr(
    en: 'What\'s new',
    es: 'Novedades',
    de: 'Neuerungen',
    ru: 'Что нового',
    tr: 'Güncelleme günlüğü',
    ptBr: 'Registro de atualização',
    zhHans: '更新日志',
    zhHant: '更新紀錄',
    ko: '변경된 사항',
  );

  String get appUpdateLogLoading => tr(
    en: 'loading update log...',
    es: 'cargando novedades...',
    de: 'Änderungsprotokoll wird geladen...',
    ru: 'загружаем список изменений...',
    tr: 'güncelleme günlüğü yükleniyor...',
    ptBr: 'carregando registro de atualização...',
    zhHans: '正在加载更新日志...',
    zhHant: '正在載入更新紀錄...',
    ko: '업데이트 기록을 불러오는 중',
  );

  String get appUpdateLogUnavailable => tr(
    en: 'update log is not available',
    es: 'las novedades no están disponibles',
    de: 'Änderungsprotokoll nicht verfügbar',
    ru: 'список изменений недоступен',
    tr: 'güncelleme günlüğü mevcut değil',
    ptBr: 'registro de atualização indisponível',
    zhHans: '更新日志不可用',
    zhHant: '更新紀錄無法使用',
    ko: '업데이트 기록을 확인할 수 없음',
  );

  String get visitProjectPageTitle => tr(
    en: 'Visit project page',
    es: 'Visitar la página del proyecto',
    de: 'Projektseite besuchen',
    ru: 'Открыть страницу проекта',
    tr: 'Proje sayfasını aç',
    ptBr: 'Abrir página do projeto',
    zhHans: '访问项目页面',
    zhHant: '前往專案頁面',
    ko: '프로젝트 페이지 열기',
  );

  String get visitGithubTitle => tr(
    en: 'Visit GitHub',
    es: 'Visitar GitHub',
    de: 'GitHub besuchen',
    ru: 'Открыть GitHub',
    tr: 'GitHub\'ı aç',
    ptBr: 'Abrir GitHub',
    zhHans: '访问 GitHub',
    zhHant: '前往 GitHub',
    ko: 'GitHub 열기',
  );

  String get updateProfileNewVersionTitle => tr(
    en: 'New version available',
    es: 'Nueva versión disponible',
    de: 'Neue Version verfügbar',
    ru: 'Доступна новая версия',
    tr: 'Yeni sürüm mevcut',
    ptBr: 'Nova versão disponível',
    zhHans: '有新版本可用',
    zhHant: '有新版本可用',
    ko: '새 버전 이용 가능',
  );

  String updateProfileVersionSubtitle(String current, String latest) => tr(
    en: '$current -> $latest | tap to see',
    es: '$current -> $latest | pulsa para ver',
    de: '$current -> $latest | zum Anzeigen tippen',
    ru: '$current -> $latest | посмотреть',
    tr: '$current -> $latest | görmek için dokunun',
    ptBr: '$current -> $latest | toque para ver',
    zhHans: '$current -> $latest | 点按查看',
    zhHant: '$current -> $latest | 點按查看',
    ko: '$current -> $latest | 눌러서 보기',
  );

  String get updateProfileAvailableSubtitle => tr(
    en: 'update available | tap to see',
    es: 'actualización disponible | pulsa para ver',
    de: 'Update verfügbar | zum Anzeigen tippen',
    ru: 'доступно обновление | посмотреть',
    tr: 'güncelleme mevcut | görmek için dokunun',
    ptBr: 'atualização disponível | toque para ver',
    zhHans: '有可用更新 | 点按查看',
    zhHant: '有可用更新 | 點按查看',
    ko: '업데이트 이용 가능 | 눌러서 보기',
  );

  String get updateProfileOpenSubtitle => tr(
    en: 'tap to open update settings',
    es: 'pulsa para abrir los ajustes de actualización',
    de: 'zum Öffnen der Update-Einstellungen tippen',
    ru: 'нажмите для настройки',
    tr: 'güncelleme ayarlarını açmak için dokunun',
    ptBr: 'toque para abrir ajustes de atualização',
    zhHans: '点按打开更新设置',
    zhHant: '點按開啟更新設定',
    ko: '눌러서 업데이트 설정 열기',
  );

  String get conversionLogTitle => tr(
    en: 'Conversion log',
    es: 'Registro de conversiones',
    de: 'Umwandlungsprotokoll',
    ru: 'Лог конвертаций',
    tr: 'Dönüştürme günlüğü',
    ptBr: 'Log de conversões',
    zhHans: '转换日志',
    zhHant: '轉換記錄',
    ko: '변환 기록',
  );

  String get logLengthTitle => tr(
    en: 'Log length',
    es: 'Tamaño del registro',
    de: 'Protokollgröße',
    ru: 'Размер лога',
    tr: 'Günlük boyutu',
    ptBr: 'Tamanho do log',
    zhHans: '日志大小',
    zhHant: '記錄大小',
    ko: '로그 크가',
  );

  String get xiaomiHyperIslandTitle => tr(
    en: 'Xiaomi HyperIsland',
    es: 'Xiaomi HyperIsland',
    de: 'Xiaomi HyperIsland',
    ru: 'Xiaomi HyperIsland',
    tr: 'Xiaomi HyperIsland',
    ptBr: 'Xiaomi HyperIsland',
    zhHans: '小米 HyperIsland',
    zhHant: '小米 HyperIsland',
    ko: '샤오미 HyperIsland',
  );

  String get lengthTitle => tr(
    en: 'Length',
    es: 'Longitud',
    de: 'Länge',
    ru: 'Длина',
    tr: 'Uzunluk',
    ptBr: 'Tamanho',
    zhHans: '长度',
    zhHant: '長度',
    ko: '길이',
  );

  String get otpDedupTitle => tr(
    en: 'OTP dedup',
    es: 'Eliminar duplicados de OTP',
    de: 'OTP-Duplikate vermeiden',
    ru: 'OTP dedup',
    tr: 'OTP tekilleştirme',
    ptBr: 'Desduplicação de OTP',
    zhHans: 'OTP 去重',
    zhHant: 'OTP 去重',
    ko: 'OTP 중복 제거',
  );

  String get smartConversionDedupTitle => tr(
    en: 'Smart conversion dedup',
    es: 'Eliminar duplicados de conversión inteligente',
    de: 'Duplikate intelligenter Umwandlungen vermeiden',
    ru: 'Smart conversion dedup',
    tr: 'Akıllı dönüştürme tekilleştirme',
    ptBr: 'Desduplicação da conversão inteligente',
    zhHans: '智能转换去重',
    zhHant: '智慧轉換去重',
    ko: '지능형 변환 중복 제거',
  );

  String get animatedIslandRedesignTitle => tr(
    en: 'Animated Island',
    es: 'Cápsula animada',
    de: 'Animierte Kapsel',
    ru: 'Анимированный остров',
    tr: 'Animasyonlu ada',
    ptBr: 'Ilha animada',
    zhHans: '动态岛动画',
    zhHant: '動態島動畫',
    ko: '아일랜드 애니메이션',
  );

  String get updateFrequencyTitle => tr(
    en: 'Update frequency',
    es: 'Frecuencia de actualización',
    de: 'Aktualisierungsintervall',
    ru: 'Частота обновления',
    tr: 'Güncelleme sıklığı',
    ptBr: 'Frequência de atualização',
    zhHans: '更新频率',
    zhHant: '更新頻率',
    ko: '업데이트 주기',
  );

  String get copyDebugJsonTitle => tr(
    en: 'Copy debug JSON',
    es: 'Copiar JSON de diagnóstico',
    de: 'Diagnose-JSON kopieren',
    ru: 'Скопировать debug JSON',
    tr: 'Debug JSON\'unu kopyala',
    ptBr: 'Copiar JSON de debug',
    zhHans: '复制调试 JSON',
    zhHant: '複製偵錯 JSON',
    ko: 'debug JSON 복사',
  );

  String get copyDebugJsonDescription => tr(
    en: 'copies device, permission, settings, and rules state for bug reports',
    es: 'copia el estado del dispositivo, los permisos, los ajustes y las reglas para informar de errores',
    de: 'kopiert Geräteinformationen, Berechtigungen, Einstellungen und Regeln für Fehlerberichte',
    ru: 'копирует состояние устройства, разрешений, настроек и правил для issue',
    tr: 'hata raporları için cihaz, izin, ayar ve kural durumunu kopyalar',
    ptBr:
        'copia o estado do dispositivo, permissões, configurações e regras para reportar bugs',
    zhHans: '复制用于问题报告的设备、权限、设置和规则状态',
    zhHant: '複製用於問題回報的裝置、權限、設定與規則狀態',
    ko: '버그 제보를 위해 기기, 권한, 설정, 규칙 상태를 복사합니다.',
  );

  String get openGithubPageTitle => tr(
    en: 'Open GitHub page',
    es: 'Abrir página de GitHub',
    de: 'GitHub-Seite öffnen',
    ru: 'Открыть GitHub',
    tr: 'GitHub sayfasını aç',
    ptBr: 'Abrir página do GitHub',
    zhHans: '打开 GitHub 页面',
    zhHant: '開啟 GitHub 頁面',
    ko: 'GitHub 페이지 열기',
  );

  String get openGithubPageDescription => tr(
    en: 'opens the GitHub issue page for reporting bugs',
    es: 'abre la página de incidencias de GitHub para informar de errores',
    de: 'öffnet die GitHub-Issue-Seite zum Melden von Fehlern',
    ru: 'открывает страницу GitHub Issues для багрепорта',
    tr: 'hata bildirmek için GitHub Issues sayfasını açar',
    ptBr: 'abre a página de Issues do GitHub para reportar bugs',
    zhHans: '打开用于报告问题的 GitHub Issues 页面',
    zhHant: '開啟用於回報問題的 GitHub Issues 頁面',
    ko: '버그 제보를 위해 GitHub 이슈 페이지를 엽니다.',
  );

  String get autoCopyDebugJsonTitle => tr(
    en: 'Auto-copy debug JSON',
    es: 'Copiar JSON de diagnóstico automáticamente',
    de: 'Diagnose-JSON automatisch kopieren',
    ru: 'Автокопирование debug JSON',
    tr: 'Debug JSON\'unu otomatik kopyala',
    ptBr: 'Copiar JSON de debug automaticamente',
    zhHans: '自动复制调试 JSON',
    zhHant: '自動複製偵錯 JSON',
    ko: 'debug JSON 자동 복사',
  );

  String get autoCopyDebugJsonDescription => tr(
    en: 'copies diagnostics automatically before opening GitHub',
    es: 'copia los datos de diagnóstico automáticamente antes de abrir GitHub',
    de: 'kopiert Diagnosedaten automatisch vor dem Öffnen von GitHub',
    ru: 'автоматически копирует диагностику перед открытием GitHub',
    tr: 'GitHub açılmadan önce tanılama verilerini otomatik kopyalar',
    ptBr: 'copia os diagnósticos automaticamente antes de abrir o GitHub',
    zhHans: '打开 GitHub 前自动复制诊断信息',
    zhHant: '開啟 GitHub 前自動複製診斷資訊',
    ko: 'GitHub를 열기 전에 분석 내용을 자동으로 복사합니다.',
  );

  String conversionLogFrom(String appLabel) => tr(
    en: 'from $appLabel',
    es: 'de $appLabel',
    de: 'von $appLabel',
    ru: 'от $appLabel',
    tr: '$appLabel uygulamasından',
    ptBr: 'de $appLabel',
    zhHans: '来自 $appLabel',
    zhHant: '來自 $appLabel',
    ko: '출처: ',
  );

  String conversionLogAt(String time) => tr(
    en: 'at $time',
    es: 'a las $time',
    de: 'um $time',
    ru: 'в $time',
    tr: time,
    ptBr: 'às $time',
    zhHans: time,
    zhHant: time,
    ko: '시간: ',
  );

  String get conversionLogEntryTitleLabel => tr(
    en: 'Title',
    es: 'Título',
    de: 'Titel',
    ru: 'Заголовок',
    tr: 'Başlık',
    ptBr: 'Título',
    zhHans: '标题',
    zhHant: '標題',
    ko: '제목',
  );

  String get payloadJsonTitle => tr(
    en: 'Payload JSON',
    es: 'Datos JSON',
    de: 'JSON-Daten',
    ru: 'Payload JSON',
    tr: 'Payload JSON',
    ptBr: 'Payload JSON',
    zhHans: 'Payload JSON',
    zhHant: 'Payload JSON',
    ko: 'Payload JSON',
  );

  String get loadingApps => tr(
    en: 'loading apps...',
    es: 'cargando aplicaciones...',
    de: 'Apps werden geladen...',
    ru: 'загрузка приложений...',
    tr: 'uygulamalar yükleniyor...',
    ptBr: 'carregando apps...',
    zhHans: '正在加载应用...',
    zhHant: '正在載入應用程式...',
    ko: '앱을 불러오는 중...',
  );

  String get searchForApps => tr(
    en: 'Search for apps...',
    es: 'Buscar aplicaciones...',
    de: 'Apps suchen...',
    ru: 'Поиск приложений...',
    tr: 'Uygulama ara...',
    ptBr: 'Buscar apps...',
    zhHans: '搜索应用...',
    zhHant: '搜尋應用程式...',
    ko: '앱을 검색하는 중...',
  );

  String get heroTitle => 'LiveBridge';

  String get reportBug => tr(
    en: 'Report a bug',
    es: 'Informar de un error',
    de: 'Fehler melden',
    ru: 'Сообщить о баге',
    tr: 'Hata bildir',
    ptBr: 'Reportar um bug',
    zhHans: '报告问题',
    zhHant: '回報問題',
    ko: '버그 제보',
  );

  String get supportLiveBridgeTitle => tr(
    en: 'Support LiveBridge',
    es: 'Apoyar a LiveBridge',
    de: 'LiveBridge unterstützen',
    ru: 'Поддержать LiveBridge',
    tr: 'LiveBridge’i destekle',
    ptBr: 'Apoiar o LiveBridge',
    zhHans: '支持 LiveBridge',
    zhHant: '支持 LiveBridge',
    ko: 'LiveBridge 지원하기',
  );

  String get supportIntroTitle => tr(
    en: 'Keep LiveBridge free',
    es: 'Mantén LiveBridge gratuito',
    de: 'LiveBridge kostenlos halten',
    ru: 'LiveBridge остается бесплатным',
    tr: 'LiveBridge ücretsiz kalsın',
    ptBr: 'Mantenha o LiveBridge gratuito',
    zhHans: '让 LiveBridge 保持免费',
    zhHant: '讓 LiveBridge 保持免費',
    ko: 'LiveBridge를 무료로 유지하기',
  );

  String get supportIntroBody => tr(
    en: 'Donations are optional and never unlock features. They help cover testing devices and development time.',
    es: 'Las donaciones son opcionales y no desbloquean funciones. Ayudan a financiar dispositivos de prueba y tiempo de desarrollo.',
    de: 'Spenden sind freiwillig und schalten keine Funktionen frei. Sie helfen, Testgeräte und Entwicklungszeit zu finanzieren.',
    ru: 'Донаты добровольны и не открывают функций. Они помогают покрывать тестовые устройства и время разработки.',
    tr: 'Bağışlar isteğe bağlıdır ve özellik açmaz. Test cihazlarını ve geliştirme süresini destekler.',
    ptBr:
        'Doações são opcionais e não desbloqueiam recursos. Elas ajudam com aparelhos de teste e tempo de desenvolvimento.',
    zhHans: '捐赠是自愿的，不会解锁功能。它们用于测试设备和开发时间。',
    zhHant: '捐贈是自願的，不會解鎖功能。它們用於測試裝置和開發時間。',
    ko: '기부는 선택이며 별도의 기능을 해금하지 않으며 개발자의 기기 테스트 및 개발에 사용됩니다.',
  );

  String get supportBoostyTitle => tr(
    en: 'Boosty',
    es: 'Boosty',
    de: 'Boosty',
    ru: 'Boosty',
    tr: 'Boosty',
    ptBr: 'Boosty',
    zhHans: 'Boosty',
    zhHant: 'Boosty',
    ko: 'Boosty',
  );

  String get supportBoostySubtitle => tr(
    en: 'cards and recurring support',
    es: 'tarjetas y apoyo recurrente',
    de: 'Kartenzahlung und regelmäßige Unterstützung',
    ru: 'карты и регулярная поддержка',
    tr: 'kartlar ve düzenli destek',
    ptBr: 'cartões e apoio recorrente',
    zhHans: '银行卡和定期支持',
    zhHant: '銀行卡和定期支持',
    ko: '카드 및 정기 기부',
  );

  String get supportCryptoTitle => tr(
    en: 'Crypto',
    es: 'Criptomonedas',
    de: 'Kryptowährungen',
    ru: 'Криптовалюта',
    tr: 'Kripto',
    ptBr: 'Cripto',
    zhHans: '加密货币',
    zhHant: '加密貨幣',
    ko: '암호화폐',
  );

  String get supportCryptoSubtitle => tr(
    en: 'copy wallet details',
    es: 'copiar datos de la cartera',
    de: 'Wallet-Daten kopieren',
    ru: 'скопировать реквизиты кошелька',
    tr: 'cüzdan bilgilerini kopyala',
    ptBr: 'copiar dados da carteira',
    zhHans: '复制钱包信息',
    zhHant: '複製錢包資訊',
    ko: '지갑 정보 복사하기',
  );

  String get supportDiscussTitle => tr(
    en: 'Discuss',
    es: 'Comunidad',
    de: 'Austausch',
    ru: 'Discuss',
    tr: 'Tartış',
    ptBr: 'Discutir',
    zhHans: '讨论',
    zhHant: '討論',
    ko: '커뮤니티',
  );

  String get supportDiscussSubtitle => tr(
    en: 'telegram topics',
    es: 'temas de Telegram',
    de: 'Telegram-Themen',
    ru: 'telegram topics',
    tr: 'telegram konuları',
    ptBr: 'tópicos no telegram',
    zhHans: 'telegram 话题',
    zhHant: 'telegram 話題',
    ko: '탤레그램 주제',
  );

  String get supportGithubTitle => tr(
    en: 'Star on GitHub',
    es: 'Dar una estrella en GitHub',
    de: 'Stern auf GitHub vergeben',
    ru: 'Поставить звезду на GitHub',
    tr: 'GitHub',
    ptBr: 'GitHub',
    zhHans: 'GitHub',
    zhHant: 'GitHub',
    ko: 'GitHub',
  );

  String get supportGithubSubtitle => tr(
    en: 'source code and releases',
    es: 'código fuente y versiones',
    de: 'Quellcode und Releases',
    ru: 'исходный код и релизы',
    tr: 'kaynak kod ve sürümler',
    ptBr: 'código-fonte e versões',
    zhHans: '源代码和版本发布',
    zhHant: '原始碼和版本發布',
    ko: '소스 코드 및 릴리즈',
  );

  String get supportMethodNotConfigured => tr(
    en: 'Support method is not configured yet.',
    es: 'El método de apoyo aún no está configurado.',
    de: 'Diese Unterstützungsmethode ist noch nicht eingerichtet.',
    ru: 'Способ поддержки еще не настроен.',
    tr: 'Destek yöntemi henüz yapılandırılmadı.',
    ptBr: 'O método de apoio ainda não foi configurado.',
    zhHans: '支持方式尚未配置。',
    zhHant: '支持方式尚未設定。',
    ko: '기부 수단이 아직 정해지지 않았습니다.',
  );

  String get supportCryptoCopied => tr(
    en: 'Crypto details copied',
    es: 'Datos de criptomonedas copiados',
    de: 'Kryptodaten kopiert',
    ru: 'Криптореквизиты скопированы',
    tr: 'Kripto bilgileri kopyalandı',
    ptBr: 'Dados de cripto copiados',
    zhHans: '加密货币信息已复制',
    zhHant: '加密貨幣資訊已複製',
    ko: '암호화폐 지갑 정보가 복사됨',
  );

  String get bugReportCopied => tr(
    en: 'Diagnostics copied to clipboard. Paste it into the issue.',
    es: 'Diagnóstico copiado al portapapeles. Pégalo en la incidencia.',
    de: 'Diagnosedaten in die Zwischenablage kopiert. Füge sie in das Issue ein.',
    ru: 'Диагностика скопирована в буфер. Вставьте в issue.',
    tr: 'Tanılama panoya kopyalandı. Issue içine yapıştırın.',
    ptBr: 'Diagnóstico copiado para a área de transferência. Cole no issue.',
    zhHans: '诊断信息已复制到剪贴板，请粘贴到 issue 中。',
    zhHant: '診斷資訊已複製到剪貼簿，請貼到 issue。',
    ko: '분석 정보가 클립보드에 복사되었습니다. 이슈에 붙여넣어 주세요.',
  );

  String get bugReportCopyFailed => tr(
    en: 'Failed to copy diagnostics.',
    es: 'No se pudieron copiar los datos de diagnóstico.',
    de: 'Diagnosedaten konnten nicht kopiert werden.',
    ru: 'Не удалось скопировать диагностику.',
    tr: 'Tanılama kopyalanamadı.',
    ptBr: 'Falha ao copiar diagnóstico.',
    zhHans: '复制诊断信息失败。',
    zhHant: '複製診斷資訊失敗。',
    ko: '분석 정보를 복사할 수 없습니다.',
  );

  String get accessTitle => tr(
    en: 'Permissions',
    es: 'Permisos',
    de: 'Berechtigungen',
    ru: 'Разрешения',
    tr: 'İzinler',
    ptBr: 'Permissões',
    zhHans: '权限',
    zhHant: '權限',
    ko: '권한',
  );

  String get listenerAccess => tr(
    en: 'Notification Listener access',
    es: 'Acceso a notificaciones',
    de: 'Benachrichtigungszugriff',
    ru: 'Доступ к уведомлениям',
    tr: 'Bildirim dinleyicisi erişimi',
    ptBr: 'Permitir acesso do app as notificações',
    zhHans: '通知监听访问',
    zhHant: '通知監聽存取',
    ko: '알림 읽기 접근 권힌',
  );

  String get postNotifications => tr(
    en: 'Post notifications permission',
    es: 'Permiso para enviar notificaciones',
    de: 'Berechtigung zum Senden von Benachrichtigungen',
    ru: 'Отправка уведомлений',
    tr: 'Bildirim gönderme izni',
    ptBr: 'Permita que o app envie notificações',
    zhHans: '发送通知权限',
    zhHant: '發送通知權限',
    ko: '알림 권한',
  );

  String get liveUpdatesAccess => tr(
    en: 'Live Updates promotion',
    es: 'Permiso de Live Updates',
    de: 'Live-Updates-Berechtigung',
    ru: 'Разрешение на Live Updates',
    tr: 'Live Updates tanıtımı',
    ptBr: 'Permitir atualizações ao vivo',
    zhHans: 'Live Updates 推送权限',
    zhHant: 'Live Updates 推送權限',
    ko: 'Live Updates 권한',
  );

  String get settingsTitle => tr(
    en: 'Settings',
    es: 'Ajustes',
    de: 'Einstellungen',
    ru: 'Настройки',
    tr: 'Ayarlar',
    ptBr: 'Configurações',
    zhHans: '设置',
    zhHant: '設定',
    ko: '설정',
  );

  String get keepAliveForegroundTitle => tr(
    en: 'Alt background mode',
    es: 'Modo alternativo en segundo plano',
    de: 'Alternativer Hintergrundmodus',
    ru: 'Альтернативный фоновый режим',
    tr: 'Alternatif arka plan modu',
    ptBr: 'Modo de segundo plano alternativo',
    zhHans: '备用后台模式',
    zhHant: '備用背景模式',
    ko: '대체 백그라운드 모드',
  );

  String get networkSpeedTitle => tr(
    en: 'Network speed',
    es: 'Velocidad de red',
    de: 'Netzwerkgeschwindigkeit',
    ru: 'Скорость сети',
    tr: 'Ağ hızı',
    ptBr: 'Velocidade da rede',
    zhHans: '网速',
    zhHant: '網速',
    ko: '네트워크 속도',
  );

  String get networkSpeedThresholdAlways => tr(
    en: 'Always show',
    es: 'Mostrar siempre',
    de: 'Immer anzeigen',
    ru: 'Показывать всегда',
    tr: 'Her zaman göster',
    ptBr: 'Sempre mostrar',
    zhHans: '始终显示',
    zhHant: '永遠顯示',
    ko: '항상 표시',
  );

  String get syncDndTitle => tr(
    en: 'Sync DnD',
    es: 'Sincronizar con No molestar',
    de: 'Mit „Nicht stören“ synchronisieren',
    ru: 'Синхронизировать DnD',
    tr: 'DnD eşitle',
    ptBr: 'Sincronizar Não Perturbe',
    zhHans: '同步勿扰',
    zhHant: '同步勿擾',
    ko: '방해금지 모드 연동하기',
  );

  String get preventDismissingTitle => tr(
    en: 'Prevent dismissing',
    es: 'Impedir que se descarten',
    de: 'Entfernen verhindern',
    ru: 'Запретить скрытие',
    tr: 'Bildirimi kapatmayı engelle',
    ptBr: 'Impedir fechamento',
    zhHans: '防止通知被关闭',
    zhHant: '防止通知被關閉',
    ko: '지우기 방지',
  );

  String get hideLockscreenContentTitle => tr(
    en: 'Hide lockscreen content',
    es: 'Ocultar contenido en la pantalla de bloqueo',
    de: 'Inhalt auf dem Sperrbildschirm ausblenden',
    ru: 'Скрывать на локскрине',
    tr: 'Kilit ekranında içeriği gizle',
    ptBr: 'Ocultar conteúdo quando a tela estiver bloqueada',
    zhHans: '隐藏锁屏内容',
    zhHant: '隱藏鎖定畫面內容',
    ko: '잠금화면 내용 숨기기',
  );

  String get convertedNotificationSoundTitle => tr(
    en: 'Converted notification sound',
    es: 'Sonido de notificaciones convertidas',
    de: 'Ton für umgewandelte Benachrichtigungen',
    ru: 'Звук конвертированных уведомлений',
    tr: 'Dönüştürülen bildirim sesi',
    ptBr: 'Som das notificações convertidas',
    zhHans: '转换通知声音',
    zhHant: '轉換通知聲音',
    ko: '변환된 알림 소리',
  );

  String get convertedNotificationVibrationTitle => tr(
    en: 'Converted notification vibration',
    es: 'Vibración de notificaciones convertidas',
    de: 'Vibration für umgewandelte Benachrichtigungen',
    ru: 'Вибрация конвертированных уведомлений',
    tr: 'Dönüştürülen bildirim titreşimi',
    ptBr: 'Vibração das notificações convertidas',
    zhHans: '转换通知振动',
    zhHant: '轉換通知震動',
    ko: '변환된 알림 진동',
  );

  String get convertedNotificationVibrationDescription => tr(
    en: 'vibrates when a converted notification first appears; system channel settings take priority',
    es: 'vibra cuando aparece una notificación convertida por primera vez; los ajustes del canal del sistema tienen prioridad',
    de: 'vibriert beim ersten Erscheinen einer umgewandelten Benachrichtigung; die Kanaleinstellungen des Systems haben Vorrang',
    ru: 'вибрирует при первом появлении уведомления; системные настройки канала имеют приоритет',
    tr: 'dönüştürülen bildirim ilk göründüğünde titreşir; sistem kanal ayarları önceliklidir',
    ptBr:
        'vibra quando uma notificação convertida aparece; as configurações do canal têm prioridade',
    zhHans: '转换通知首次出现时振动；系统通知渠道设置优先',
    zhHant: '轉換通知首次出現時震動；系統通知管道設定優先',
    ko: '변환된 알림이 처음 표시될 때 진동합니다. 시스템 채널 설정이 우선합니다.',
  );

  String get disableHintsTitle => tr(
    en: 'Disable hints',
    es: 'Desactivar consejos',
    de: 'Hinweise deaktivieren',
    ru: 'Отключить подсказки',
    tr: 'İpuçlarını kapat',
    ptBr: 'Desativar dicas',
    zhHans: '关闭提示',
    zhHant: '關閉提示',
    ko: '힌트 숨기기',
  );

  String get updateChecksTitle => tr(
    en: 'Update checking',
    es: 'Buscar actualizaciones',
    de: 'Update-Prüfung',
    ru: 'Проверка обновлений',
    tr: 'Güncellemeleri denetle',
    ptBr: 'Verificação de atualizações',
    zhHans: '检查更新',
    zhHant: '檢查更新',
    ko: '업데이트 확인 중',
  );

  String get updateChecksDescription => tr(
    en: 'checks GitHub releases and shows an update card when a new version is available',
    es: 'busca versiones en GitHub y muestra una tarjeta cuando hay una nueva versión',
    de: 'prüft GitHub-Releases und zeigt eine Karte an, wenn eine neue Version verfügbar ist',
    ru: 'проверяет релизы GitHub и показывает карточку, когда доступна новая версия',
    tr: 'GitHub sürümlerini denetler ve yeni sürüm varsa güncelleme kartı gösterir',
    ptBr:
        'verifica lançamentos no GitHub e mostra um cartão quando há nova versão disponível',
    zhHans: '检查 GitHub 发布，并在有新版本时显示更新卡片',
    zhHant: '檢查 GitHub 發布版本，並在有新版本時顯示更新卡片',
    ko: 'GitHub 릴리즈를 확인해서 새 버전을 이용할 수 있다면 업데이트 카드를 표시합니다.',
  );

  String get experimentalTitle => tr(
    en: 'Experimental',
    es: 'Experimental',
    de: 'Experimentell',
    ru: 'Экспериментальное',
    tr: 'Deneysel',
    ptBr: 'Experimental',
    zhHans: '实验功能',
    zhHant: '實驗功能',
    ko: '실험실',
  );

  String get aospCuttingTitle => tr(
    en: 'AOSP cutting',
    es: 'Recortar texto en AOSP',
    de: 'Textkürzung für AOSP',
    ru: 'Обрезка AOSP',
    tr: 'AOSP kırpma',
    ptBr: 'Recorte AOSP',
    zhHans: 'AOSP 裁剪',
    zhHant: 'AOSP 裁切',
    ko: 'AOSP 자르기',
  );

  String get appPresentationSettings => tr(
    en: 'Per-app behavior',
    es: 'Comportamiento por aplicación',
    de: 'Verhalten pro App',
    ru: 'Поведение приложений',
    tr: 'Uygulama bazlı davranış',
    ptBr: 'Comportamento por app',
    zhHans: '按应用行为',
    zhHant: '各應用行為',
    ko: '앱별 동작',
  );

  String get appPresentationLoadFailed => tr(
    en: 'Unable to load per-app settings.',
    es: 'No se pudieron cargar los ajustes por aplicación.',
    de: 'App-spezifische Einstellungen konnten nicht geladen werden.',
    ru: 'Не удалось загрузить настройки приложений.',
    tr: 'Uygulama bazlı ayarlar yüklenemiyor.',
    ptBr: 'Não foi possível carregar configurações por app.',
    zhHans: '无法加载按应用设置。',
    zhHant: '無法載入各應用設定。',
    ko: '앱별 동작을 불러올 수 없습니다.',
  );

  String get appPresentationSaveFailed => tr(
    en: 'Unable to save per-app settings.',
    es: 'No se pudieron guardar los ajustes por aplicación.',
    de: 'App-spezifische Einstellungen konnten nicht gespeichert werden.',
    ru: 'Не удалось сохранить настройки приложений.',
    tr: 'Uygulama bazlı ayarlar kaydedilemiyor.',
    ptBr: 'Não foi possível salvar configurações por app.',
    zhHans: '无法保存按应用设置。',
    zhHant: '無法儲存各應用設定。',
    ko: '앱별 동작을 저장할 수 없습니다.',
  );

  String get appPresentationDownloadFailed => tr(
    en: 'Failed to save settings JSON.',
    es: 'No se pudo guardar el JSON de ajustes.',
    de: 'Einstellungs-JSON konnte nicht gespeichert werden.',
    ru: 'Не удалось сохранить JSON настроек.',
    tr: 'Ayarlar JSON\'u kaydedilemedi.',
    ptBr: 'Falha ao salvar JSON de configurações.',
    zhHans: '保存设置 JSON 失败。',
    zhHant: '儲存設定 JSON 失敗。',
    ko: '설정 JSON을 저장할 수 없습니다.',
  );

  String get appPresentationSaved => tr(
    en: 'Settings saved to Downloads.',
    es: 'Ajustes guardados en Descargas.',
    de: 'Einstellungen unter Downloads gespeichert.',
    ru: 'Настройки сохранены в Загрузки.',
    tr: 'Ayarlar İndirilenler klasörüne kaydedildi.',
    ptBr: 'Configurações salvas em Downloads.',
    zhHans: '设置已保存到下载目录。',
    zhHant: '設定已儲存到下載資料夾。',
    ko: '설정을 Downloads 폴더에 저장했습니다.',
  );

  String get appPresentationUploadDone => tr(
    en: 'Per-app settings imported.',
    es: 'Ajustes por aplicación importados.',
    de: 'App-spezifische Einstellungen importiert.',
    ru: 'Настройки приложений загружены.',
    tr: 'Uygulama bazlı ayarlar içe aktarıldı.',
    ptBr: 'Configurações por app importadas.',
    zhHans: '已导入按应用设置。',
    zhHant: '已匯入各應用設定。',
    ko: '앱별 설정을 성공적으로 불러왔습니다.',
  );

  String get appPresentationUploadFailed => tr(
    en: 'Failed to import settings JSON.',
    es: 'No se pudo importar el JSON de ajustes.',
    de: 'Einstellungs-JSON konnte nicht importiert werden.',
    ru: 'Не удалось загрузить JSON настроек.',
    tr: 'Ayarlar JSON\'u içe aktarılamadı.',
    ptBr: 'Falha ao importar JSON de configurações.',
    zhHans: '导入设置 JSON 失败。',
    zhHant: '匯入設定 JSON 失敗。',
    ko: '앱별 설정 JSON을 불러올 수 없습니다.',
  );

  String get appPresentationInvalidJson => tr(
    en: 'Invalid per-app settings JSON.',
    es: 'El JSON de ajustes por aplicación no es válido.',
    de: 'Ungültiges JSON für App-spezifische Einstellungen.',
    ru: 'Невалидный JSON настроек приложений.',
    tr: 'Geçersiz uygulama bazlı ayarlar JSON\'u.',
    ptBr: 'JSON de configurações por app inválido.',
    zhHans: '按应用设置 JSON 无效。',
    zhHant: '各應用設定 JSON 無效。',
    ko: '앱별 설정 JSON에 문제가 있습니다.',
  );

  String get customNotificationColorTitle => tr(
    en: 'Custom notification color',
    ru: '\u041f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u0435\u043b\u044c\u0441\u043a\u0438\u0439 \u0446\u0432\u0435\u0442 \u0443\u0432\u0435\u0434\u043e\u043c\u043b\u0435\u043d\u0438\u044f',
  );

  String get notificationColorTitle => tr(
    en: 'Notification color',
    ru: '\u0426\u0432\u0435\u0442 \u0443\u0432\u0435\u0434\u043e\u043c\u043b\u0435\u043d\u0438\u044f',
  );

  String get selectNotificationColorTitle => tr(
    en: 'Select color',
    ru: '\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u0446\u0432\u0435\u0442',
  );

  String get downloadSettings => tr(
    en: 'Download settings',
    es: 'Descargar ajustes',
    de: 'Einstellungen herunterladen',
    ru: 'Скачать настройки',
    tr: 'Ayarları indir',
    ptBr: 'Baixar configurações',
    zhHans: '下载设置',
    zhHant: '下載設定',
    ko: '다운로드 설정',
  );

  String get uploadSettings => tr(
    en: 'Upload settings',
    es: 'Cargar ajustes',
    de: 'Einstellungen hochladen',
    ru: 'Загрузить настройки',
    tr: 'Ayarları yükle',
    ptBr: 'Enviar configurações',
    zhHans: '上传设置',
    zhHant: '上傳設定',
    ko: '업로드 설정',
  );

  String get save => tr(
    en: 'Save',
    es: 'Guardar',
    de: 'Speichern',
    ru: 'Сохранить',
    tr: 'Kaydet',
    ptBr: 'Salvar',
    zhHans: '保存',
    zhHant: '儲存',
    ko: '저장',
  );

  String get appsLoadFailed => tr(
    en: 'Unable to load installed apps list.',
    es: 'No se pudo cargar la lista de aplicaciones instaladas.',
    de: 'Die Liste installierter Apps konnte nicht geladen werden.',
    ru: 'Не удалось загрузить список приложений.',
    tr: 'Yüklü uygulama listesi yüklenemiyor.',
    ptBr: 'Não foi possível carregar a lista de apps instalados.',
    zhHans: '无法加载已安装应用列表。',
    zhHant: '無法載入已安裝應用清單。',
    ko: '설치된 앱 목록을 불러올 수 없습니다.',
  );

  String get appsAccessTitle => tr(
    en: 'App list access',
    es: 'Acceso a la lista de aplicaciones',
    de: 'Zugriff auf die App-Liste',
    ru: 'Доступ к списку приложений',
    tr: 'Uygulama listesi erişimi',
    ptBr: 'Acesso à lista de apps',
    zhHans: '应用列表访问',
    zhHant: '應用清單存取',
    ko: '앱 목록 접근',
  );

  String get appsAccessMessage => tr(
    en: 'Allow LiveBridge to read installed apps so you can pick apps for rules?',
    es: '¿Permitir que LiveBridge lea las aplicaciones instaladas para seleccionarlas en las reglas?',
    de: 'Darf LiveBridge die installierten Apps lesen, damit du sie für Regeln auswählen kannst?',
    ru: 'Разрешить LiveBridge читать список установленных приложений для выбора правил?',
    tr: 'Kurallar için uygulama seçebilmeniz adına LiveBridge yüklü uygulamaları okuyabilsin mi?',
    ptBr:
        'Permitir que o LiveBridge leia os apps instalados para que você possa escolher apps para as regras?',
    zhHans: '允许 LiveBridge 读取已安装应用列表，以便为规则选择应用吗？',
    zhHant: '允許 LiveBridge 讀取已安裝應用清單，以便為規則選擇應用程式嗎？',
    ko: 'LiveBridge가 설치된 앱 목록을 읽어서 앱을 규칙에 지정하시겠어요?',
  );

  String get appsAccessSaveFailed => tr(
    en: 'Unable to save access preference.',
    es: 'No se pudo guardar la preferencia de acceso.',
    de: 'Die Zugriffseinstellung konnte nicht gespeichert werden.',
    ru: 'Не удалось сохранить выбор доступа.',
    tr: 'Erişim tercihi kaydedilemiyor.',
    ptBr: 'Não foi possível salvar a preferência de acesso.',
    zhHans: '无法保存访问偏好。',
    zhHant: '無法儲存存取偏好。',
    ko: '접근 설정을 저장할 수 없습니다.',
  );

  String get cancel => tr(
    en: 'Cancel',
    es: 'Cancelar',
    de: 'Abbrechen',
    ru: 'Отмена',
    tr: 'İptal',
    ptBr: 'Cancelar',
    zhHans: '取消',
    zhHant: '取消',
    ko: '취소',
  );

  String get allow => tr(
    en: 'Allow',
    es: 'Permitir',
    de: 'Erlauben',
    ru: 'Разрешить',
    tr: 'İzin ver',
    ptBr: 'Permitir',
    zhHans: '允许',
    zhHant: '允許',
    ko: '허용',
  );

  String get appPresentationDefaultSummary => tr(
    en: 'Default behavior',
    ru: 'Стандартное поведение',
    tr: 'Varsayılan davranış',
    ptBr: 'Comportamento padrão',
    zhHans: '默认行为',
    zhHant: '預設行為',
  );

  String get appPresentationTextSourceLabel => tr(
    en: 'Island text source',
    ru: 'Источник текста для острова',
    tr: 'Ada metni kaynağı',
    ptBr: 'Origem do texto da ilha',
    zhHans: '岛文本来源',
    zhHant: '島文字來源',
  );

  String get appPresentationIconSourceLabel => tr(
    en: 'Icon source',
    ru: 'Источник иконки',
    tr: 'Simge kaynağı',
    ptBr: 'Origem do ícone',
    zhHans: '图标来源',
    zhHant: '圖示來源',
  );

  String get appPresentationTextTitle => tr(
    en: 'Notification title',
    ru: 'Заголовок уведомления',
    tr: 'Bildirim başlığı',
    ptBr: 'Título da notificação',
    zhHans: '通知标题',
    zhHant: '通知標題',
  );

  String get appPresentationTextNotification => tr(
    en: 'Notification text',
    ru: 'Текст уведомления',
    tr: 'Bildirim metni',
    ptBr: 'Texto da notificação',
    zhHans: '通知文本',
    zhHant: '通知文字',
  );

  String get appPresentationIconNotification => tr(
    en: 'Notification icon',
    ru: 'Иконка уведомления',
    tr: 'Bildirim simgesi',
    ptBr: 'Ícone da notificação',
    zhHans: '通知图标',
    zhHant: '通知圖示',
  );

  String get appPresentationIconApp => tr(
    en: 'Application icon',
    ru: 'Иконка приложения',
    tr: 'Uygulama simgesi',
    ptBr: 'Ícone do app',
    zhHans: '应用图标',
    zhHant: '應用程式圖示',
  );

  String get searchAppHint => tr(
    en: 'Search by app or package',
    ru: 'Поиск по названию или пакету',
    tr: 'Uygulama veya paket ara',
    ptBr: 'Buscar por app ou pacote',
    zhHans: '按应用或包名搜索',
    zhHant: '依應用程式或套件搜尋',
  );

  String get showSystemApps => tr(
    en: 'Show system applications',
    ru: 'Показать системные приложения',
    tr: 'Sistem uygulamalarını göster',
    ptBr: 'Mostrar apps do sistema',
    zhHans: '显示系统应用',
    zhHant: '顯示系統應用程式',
  );

  String get hideSystemApps => tr(
    en: 'Hide system applications',
    ru: 'Скрыть системные приложения',
    tr: 'Sistem uygulamalarını gizle',
    ptBr: 'Ocultar apps do sistema',
    zhHans: '隐藏系统应用',
    zhHant: '隱藏系統應用程式',
  );

  String get resetToDefault => tr(
    en: 'Reset to default',
    ru: 'Сбросить к стандарту',
    tr: 'Varsayılana sıfırla',
    ptBr: 'Redefinir para o padrão',
    zhHans: '重置为默认',
    zhHant: '重設為預設值',
  );

  String get notificationDedupTitle => tr(
    en: 'Notification dedup',
    ru: 'Удаление дублей уведомлений',
    tr: 'Bildirim tekilleştirme',
    ptBr: 'Deduplicação de notificações',
    zhHans: '通知去重',
    zhHant: '通知去重',
  );

  String get notificationDedupSubtitle => tr(
    en: 'dismisses original clearable notifications after LiveBridge mirrors an OTP or status update',
    ru: 'убирает исходные смахиваемые уведомления, если LiveBridge уже показал свой OTP или статус',
    tr: 'LiveBridge bir OTP veya durum güncellemesini yansıttıktan sonra temizlenebilir orijinal bildirimleri kapatır',
    ptBr:
        'dispensa notificações originais removíveis depois que o LiveBridge espelha um OTP ou uma atualização de status',
    zhHans: 'LiveBridge 镜像 OTP 或状态更新后，关闭可清除的原始通知。',
    zhHant: 'LiveBridge 鏡像 OTP 或狀態更新後，關閉可清除的原始通知。',
  );

  String get notificationDedupModeLabel => tr(
    en: 'Dedup mode',
    ru: 'Режим удаления дублей',
    tr: 'Tekilleştirme modu',
    ptBr: 'Modo de deduplicação',
    zhHans: '去重模式',
    zhHant: '去重模式',
  );

  String get notificationDedupModeOtpOnly => tr(
    en: 'OTP only',
    ru: 'Только OTP',
    tr: 'Yalnızca OTP',
    ptBr: 'Somente OTP',
    zhHans: '仅 OTP',
    zhHant: '僅 OTP',
  );

  String get notificationDedupModeOtpStatus => tr(
    en: 'OTP and statuses',
    ru: 'OTP и статусы',
    tr: 'OTP ve durumlar',
    ptBr: 'OTP e status',
    zhHans: 'OTP 和状态',
    zhHant: 'OTP 與狀態',
  );

  String get samsungRemoteParserTitle => tr(
    en: 'Samsung RemoteViews reparser',
    ru: 'Samsung RemoteViews репарсер',
    tr: 'Samsung RemoteViews yeniden ayrıştırıcı',
    ptBr: 'Reanalisador Samsung RemoteViews',
    zhHans: 'Samsung RemoteViews 重新解析器',
    zhHant: 'Samsung RemoteViews 重新解析器',
  );

  String get samsungRemoteParserSubtitle => tr(
    en: 'Uses Samsung ongoingActivity extras and RemoteViews for improved parsing on One UI.',
    ru: 'Использует Samsung ongoingActivity extras и RemoteViews для более точного парсинга уведомлений.',
    tr: 'One UI üzerinde daha iyi ayrıştırma için Samsung ongoingActivity extras ve RemoteViews kullanır.',
    ptBr:
        'Usa extras ongoingActivity da Samsung e RemoteViews para melhorar a análise no One UI.',
    zhHans: '使用 Samsung ongoingActivity extras 和 RemoteViews 改进 One UI 上的解析。',
    zhHant: '使用 Samsung ongoingActivity extras 和 RemoteViews 改進 One UI 上的解析。',
  );

  String get smartNavigationSubtitle => tr(
    en: 'Navigation notification detection.',
    ru: 'Распознавание уведомлений навигации.',
    tr: 'Navigasyon bildirimi algılama.',
    ptBr: 'Detecção de notificações de navegação.',
    zhHans: '导航通知检测。',
    zhHant: '導航通知偵測。',
  );

  String get smartMediaPlaybackSubtitle => tr(
    en: 'Converts media playback notifications into Live. On some OEMs this may duplicate native media UI.',
    ru: 'Преобразует уведомления медиаплеера в Live. На некоторых OEM может дублировать нативный плеер.',
    tr: 'Medya oynatma bildirimlerini Live\'a dönüştürür. Bazı OEM\'lerde yerel medya arayüzünü çoğaltabilir.',
    ptBr:
        'Converte notificações de reprodução de mídia em Live. Em alguns OEMs, isso pode duplicar a interface nativa de mídia.',
    zhHans: '将媒体播放通知转换为 Live。在某些 OEM 上可能会重复原生媒体界面。',
    zhHant: '將媒體播放通知轉換為 Live。在某些 OEM 上可能會重複原生媒體介面。',
  );

  String get smartCallsSubtitle => tr(
    en: 'Mirrors active call notifications and shows elapsed call time in the chip.',
    ru: 'Отражает уведомления активных звонков и показывает время разговора в чипе.',
    tr: 'Etkin arama bildirimlerini yansıtır ve arama süresini çipte gösterir.',
    ptBr:
        'Espelha notificações de chamadas ativas e mostra o tempo da chamada no chip.',
    zhHans: '镜像正在进行的通话通知，并在芯片中显示通话时长。',
    zhHant: '鏡像進行中的通話通知，並在晶片中顯示通話時間。',
  );

  String get smartWeatherSubtitle => tr(
    en: 'Weather notification detection (temperature in island).',
    ru: 'Распознавание погодных уведомлений (температура в острове).',
    tr: 'Hava durumu bildirimi algılama (adada sıcaklık).',
    ptBr: 'Detecção de notificações de clima (temperatura na ilha).',
    zhHans: '天气通知检测（岛中显示温度）。',
    zhHant: '天氣通知偵測（島中顯示溫度）。',
  );

  String get smartWeatherLockscreenOnlyTitle => tr(
    en: 'Display only on lock screen',
    ru: 'Показывать только на экране блокировки',
    tr: 'Yalnızca kilit ekranında göster',
    ptBr: 'Exibir somente na tela de bloqueio',
    zhHans: '仅在锁屏显示',
    zhHant: '僅在鎖定畫面顯示',
  );

  String get smartWeatherLockscreenOnlySubtitle => tr(
    en: 'hide weather while the device is unlocked',
    ru: 'скрывать погоду при разблокированном устройстве',
    tr: 'cihaz kilidi açıkken hava durumunu gizle',
    ptBr: 'ocultar clima quando o dispositivo estiver desbloqueado',
    zhHans: '设备解锁时隐藏天气。',
    zhHant: '裝置解鎖時隱藏天氣。',
  );

  String get smartChargingInfoTitle => tr(
    en: 'Charging information',
    ru: 'Информация о зарядке',
    tr: 'Şarj bilgisi',
    ptBr: 'Informações de carregamento',
    zhHans: '充电信息',
    zhHant: '充電資訊',
    ko: '충전 정보',
  );

  String get smartChargingInfoSubtitle => tr(
    en: 'shows battery level, time until full, charging speed, and low-battery warnings in a lock screen Now Bar capsule',
    ru: 'показывает уровень батареи, время до полного заряда, скорость зарядки и предупреждение о разрядке в капсуле Now Bar на экране блокировки',
    tr: 'pil seviyesini, dolmaya kalan süreyi, şarj hızını ve düşük pil uyarılarını kilit ekranındaki Now Bar kapsülünde gösterir',
    ptBr:
        'mostra o nível da bateria, o tempo até completar, a velocidade de carregamento e alertas de bateria fraca em uma cápsula Now Bar na tela de bloqueio',
    zhHans: '在锁屏 Now Bar 胶囊中显示电量、充满剩余时间、充电速度和低电量提醒。',
    zhHant: '在鎖定畫面的 Now Bar 膠囊中顯示電量、充滿剩餘時間、充電速度和低電量提醒。',
    ko: '잠금 화면 Now Bar 캡슐에 배터리 잔량, 완충까지 남은 시간, 충전 속도와 배터리 부족 알림을 표시합니다.',
  );

  String get smartNotificationCapsuleTitle => tr(
    en: 'Notification capsule',
    ru: 'Капсула уведомлений',
    tr: 'Bildirim kapsülü',
    ptBr: 'Cápsula de notificações',
    zhHans: '通知胶囊',
    zhHant: '通知膠囊',
    ko: '알림 캡슐',
  );

  String get smartNotificationCapsuleSubtitle => tr(
    en: 'shows only the total count and app names on the lock screen',
    ru: 'показывает на экране блокировки только общее количество и названия приложений',
    tr: 'kilit ekranında yalnızca toplam sayıyı ve uygulama adlarını gösterir',
    ptBr:
        'mostra somente a contagem total e os nomes dos apps na tela de bloqueio',
    zhHans: '仅在锁屏显示总数和应用名称。',
    zhHant: '僅在鎖定畫面顯示總數和 App 名稱。',
    ko: '잠금 화면에 전체 개수와 앱 이름만 표시합니다.',
  );

  String get notificationCapsuleSmartTitle => tr(
    en: 'Smart capsule',
    ru: 'Умная капсула',
    tr: 'Akıllı kapsül',
    ptBr: 'Cápsula inteligente',
    zhHans: '智能胶囊',
    zhHant: '智慧膠囊',
    ko: '스마트 캡슐',
  );

  String get notificationCapsuleSmartDescription => tr(
    en: 'if notifications are only from one app, show that app icon, name, and count instead of the general capsule',
    ru: 'если уведомления только от одного приложения, показывает его иконку, название и количество вместо общей капсулы',
    tr: 'bildirimler tek bir uygulamadan geliyorsa genel kapsül yerine uygulama simgesini, adını ve sayısını gösterir',
    ptBr:
        'se houver notificações de apenas um app, mostra o ícone, nome e contagem do app no lugar da cápsula geral',
    zhHans: '如果通知只来自一个应用，则显示该应用图标、名称和数量，而不是通用胶囊。',
    zhHant: '如果通知只來自一個 App，則顯示該 App 圖示、名稱和數量，而不是一般膠囊。',
    ko: '알림이 한 앱에서만 온 경우 일반 캡슐 대신 앱 아이콘, 이름, 개수를 표시합니다.',
  );

  String get notificationCapsuleClearActionTitle => tr(
    en: 'Clear action',
    ru: 'Кнопка Clear',
    tr: 'Clear eylemi',
    ptBr: 'Ação Clear',
    zhHans: 'Clear 操作',
    zhHant: 'Clear 動作',
    ko: 'Clear 동작',
  );

  String get notificationCapsuleClearActionDescription => tr(
    en: 'adds a cross icon and Clear action to notification capsules',
    ru: 'добавляет крестик и действие Clear в капсулы уведомлений',
    tr: 'bildirim kapsüllerine çarpı simgesi ve Clear eylemi ekler',
    ptBr: 'adiciona um ícone de X e a ação Clear às cápsulas de notificações',
    zhHans: '为通知胶囊添加叉号图标和 Clear 操作。',
    zhHant: '為通知膠囊加入叉號圖示和 Clear 動作。',
    ko: '알림 캡슐에 X 아이콘과 Clear 동작을 추가합니다.',
  );

  String get notificationCapsuleDisplayModeTitle => tr(
    en: 'Display mode',
    ru: 'Режим отображения',
    tr: 'Görüntüleme modu',
    ptBr: 'Modo de exibição',
    zhHans: '显示模式',
    zhHant: '顯示模式',
    ko: '표시 모드',
  );

  String get notificationCapsuleModeGeneralTitle => tr(
    en: 'General capsule',
    ru: 'Общая капсула',
    tr: 'Genel kapsül',
    ptBr: 'Cápsula geral',
    zhHans: '通用胶囊',
    zhHant: '一般膠囊',
    ko: '일반 캡슐',
  );

  String get notificationCapsuleModeGeneralDescription => tr(
    en: 'show one total notification capsule like before',
    ru: 'показывать одну общую капсулу уведомлений, как раньше',
    tr: 'önceki gibi tek bir toplam bildirim kapsülü gösterir',
    ptBr: 'mostra uma única cápsula total de notificações como antes',
    zhHans: '像以前一样显示一个总通知胶囊。',
    zhHant: '像以前一樣顯示一個總通知膠囊。',
    ko: '이전처럼 전체 알림 캡슐 하나를 표시합니다.',
  );

  String get notificationCapsuleModePerAppTitle => tr(
    en: 'Capsule per app',
    ru: 'Капсула для каждого приложения',
    tr: 'Uygulama başına kapsül',
    ptBr: 'Cápsula por app',
    zhHans: '每个应用一个胶囊',
    zhHant: '每個 App 一個膠囊',
    ko: '앱별 캡슐',
  );

  String get notificationCapsuleModePerAppDescription => tr(
    en: 'create a separate capsule with icon, app name, and count for each app',
    ru: 'создавать отдельную капсулу с иконкой, названием и количеством для каждого приложения',
    tr: 'her uygulama için simge, uygulama adı ve sayı içeren ayrı bir kapsül oluşturur',
    ptBr: 'cria uma cápsula separada com ícone, nome e contagem para cada app',
    zhHans: '为每个应用创建单独胶囊，显示图标、应用名称和数量。',
    zhHant: '為每個 App 建立單獨膠囊，顯示圖示、App 名稱和數量。',
    ko: '각 앱마다 아이콘, 앱 이름, 개수가 있는 별도 캡슐을 만듭니다.',
  );

  String get notificationCapsuleExcludedAppsTitle => tr(
    en: 'Excluded apps',
    ru: 'Исключенные приложения',
    tr: 'Hariç tutulan uygulamalar',
    ptBr: 'Apps excluídos',
    zhHans: '排除的应用',
    zhHant: '排除的 App',
    ko: '제외한 앱',
  );

  String get notificationCapsuleExcludedAppsDescription => tr(
    en: 'Selected apps are not shown in notification capsules.',
    ru: 'Выбранные приложения не показываются в капсулах уведомлений.',
    tr: 'Seçilen uygulamalar bildirim kapsüllerinde gösterilmez.',
    ptBr: 'Apps selecionados não aparecem nas cápsulas de notificações.',
    zhHans: '所选应用不会显示在通知胶囊中。',
    zhHant: '所選 App 不會顯示在通知膠囊中。',
    ko: '선택한 앱은 알림 캡슐에 표시되지 않습니다.',
  );

  String get smartFlashlightTitle => tr(
    en: 'Flashlight',
    ru: 'Фонарик',
    tr: 'El feneri',
    ptBr: 'Lanterna',
    zhHans: '手电筒',
    zhHant: '手電筒',
  );

  String get smartFlashlightSubtitle => tr(
    en: 'Creates a LiveBridge flashlight notification and mirrors the system flashlight state into Now Bar.',
    ru: 'Создаёт уведомление фонарика LiveBridge и отражает системное состояние фонарика в Now Bar.',
    tr: 'Bir LiveBridge el feneri bildirimi oluşturur ve sistem el feneri durumunu Now Bar\'a yansıtır.',
    ptBr:
        'Cria uma notificação de lanterna do LiveBridge e espelha o estado da lanterna do sistema na Now Bar.',
    zhHans: '创建 LiveBridge 手电筒通知，并将系统手电筒状态镜像到 Now Bar。',
    zhHant: '建立 LiveBridge 手電筒通知，並將系統手電筒狀態鏡像到 Now Bar。',
  );

  String get smartFlashlightUnsupportedSubtitle => tr(
    en: 'This device can enable the flashlight, but it does not expose 5 separate brightness levels.',
    ru: 'Устройство включает фонарик, но не даёт 5 отдельных уровней яркости.',
    tr: 'Bu cihaz el fenerini açabiliyor, ancak 5 ayrı parlaklık seviyesi sunmuyor.',
    ptBr:
        'Este dispositivo consegue ativar a lanterna, mas não expõe 5 níveis separados de brilho.',
    zhHans: '此设备可以开启手电筒，但不提供 5 个独立亮度级别。',
    zhHant: '此裝置可以開啟手電筒，但不提供 5 個獨立亮度級別。',
  );

  String get smartFlashlightUnavailableSubtitle => tr(
    en: 'This device does not expose a usable flashlight.',
    ru: 'На этом устройстве нет доступного фонарика.',
    tr: 'Bu cihaz kullanılabilir bir el feneri sunmuyor.',
    ptBr: 'Este dispositivo não expõe uma lanterna utilizável.',
    zhHans: '此设备没有可用的手电筒。',
    zhHant: '此裝置沒有可用的手電筒。',
  );

  String get smartVpnSubtitle => tr(
    en: 'Shows incoming/outgoing traffic speed in *b/s format.',
    ru: 'Показывает входящий/исходящий трафик в формате *b/s.',
    tr: 'Gelen/giden trafik hızını *b/s biçiminde gösterir.',
    ptBr: 'Mostra a velocidade de tráfego de entrada/saída no formato *b/s.',
    zhHans: '以 *b/s 格式显示传入/传出流量速度。',
    zhHant: '以 *b/s 格式顯示傳入/傳出流量速度。',
  );

  String get smartVpnLockscreenOnlyTitle => smartWeatherLockscreenOnlyTitle;

  String get smartVpnLockscreenOnlySubtitle => tr(
    en: 'hide VPN traffic while the device is unlocked',
    ru: 'скрывать VPN-трафик при разблокированном устройстве',
    tr: 'cihaz kilidi açıkken VPN trafiğini gizle',
    ptBr: 'ocultar o tráfego VPN quando o dispositivo estiver desbloqueado',
    zhHans: '设备解锁时隐藏 VPN 流量。',
    zhHant: '裝置解鎖時隱藏 VPN 流量。',
    ko: '기기가 잠금 해제되어 있을 때 VPN 트래픽을 숨깁니다.',
  );

  String get smartExternalDevicesSubtitle => tr(
    en: 'Shows connected/connecting status and device name in island.',
    ru: 'Показывает статус подключения и имя устройства в острове.',
    tr: 'Adada bağlı/bağlanıyor durumunu ve cihaz adını gösterir.',
    ptBr:
        'Mostra o status conectado/conectando e o nome do dispositivo na ilha.',
    zhHans: '在岛中显示已连接/连接中状态和设备名称。',
    zhHant: '在島中顯示已連線/連線中狀態和裝置名稱。',
  );

  String get smartExternalDevicesIgnoreDebuggingSubtitle => tr(
    en: 'Skip Live updates for USB debugging, wireless debugging, ADB, and similar system notifications.',
    ru: 'Не показывать Live для USB debugging, wireless debugging, ADB и похожих системных уведомлений.',
    tr: 'USB debugging, wireless debugging, ADB ve benzeri sistem bildirimleri için Live güncellemelerini atla.',
    ptBr:
        'Ignora atualizações Live para USB debugging, wireless debugging, ADB e notificações de sistema semelhantes.',
    zhHans: '跳过 USB debugging、wireless debugging、ADB 及类似系统通知的 Live 更新。',
    zhHant: '略過 USB debugging、wireless debugging、ADB 及類似系統通知的 Live 更新。',
  );

  String get networkSpeedEnabledTitle => tr(
    en: 'Show network speed in Now Bar',
    ru: 'Показывать скорость интернета в Now Bar',
    tr: 'Ağ hızını Now Bar\'da göster',
    ptBr: 'Mostrar velocidade da rede na Now Bar',
    zhHans: '在 Now Bar 显示网速',
    zhHant: '在 Now Bar 顯示網速',
  );

  String get networkSpeedEnabledSubtitle => tr(
    en: 'runs a dedicated ongoing notification with current network speed and surfaces it in the Now Bar',
    ru: 'запускает отдельное уведомление с текущей скоростью сети и выводит его в Now Bar',
    tr: 'geçerli ağ hızını içeren özel bir ongoing bildirim çalıştırır ve bunu Now Bar\'da gösterir',
    ptBr:
        'executa uma notificação ongoing dedicada com a velocidade atual da rede e a exibe na Now Bar',
    zhHans: '运行一个包含当前网速的专用 ongoing 通知，并将其显示在 Now Bar。',
    zhHant: '執行一個包含目前網速的專用 ongoing 通知，並將其顯示在 Now Bar。',
  );

  String get networkSpeedThresholdTitle => tr(
    en: 'Minimum speed to show',
    ru: 'Минимальная скорость для показа',
    tr: 'Gösterilecek minimum hız',
    ptBr: 'Velocidade mínima para exibir',
    zhHans: '显示的最低速度',
    zhHant: '顯示的最低速度',
  );

  String get networkSpeedThresholdSubtitle => tr(
    en: 'The live element appears when combined download and upload reach this threshold.',
    ru: 'Live-индикатор появится, когда суммарная скорость загрузки и отдачи достигнет этого порога.',
    tr: 'Live öğesi, indirme ve yükleme toplamı bu eşiğe ulaştığında görünür.',
    ptBr:
        'O elemento Live aparece quando download e upload combinados atingem este limite.',
    zhHans: '当下载和上传合计达到此阈值时，Live 元素会出现。',
    zhHant: '當下載和上傳合計達到此門檻時，Live 元素會出現。',
  );

  String get networkSpeedDisplayContentTitle => tr(
    en: 'Display content',
    ru: 'Отображаемый контент',
    tr: 'Gösterilecek içerik',
    ptBr: 'Conteúdo exibido',
    zhHans: '显示内容',
    zhHant: '顯示內容',
  );

  String get networkSpeedDisplayModeTotal => tr(
    en: 'Total speed',
    ru: 'Общая скорость',
    tr: 'Toplam hız',
    ptBr: 'Velocidade total',
    zhHans: '总速度',
    zhHant: '總速度',
  );

  String get networkSpeedDisplayModeUpload => tr(
    en: 'Upload only',
    ru: 'Только отдача',
    tr: 'Yalnızca yükleme',
    ptBr: 'Somente upload',
    zhHans: '仅上传',
    zhHant: '僅上傳',
  );

  String get networkSpeedDisplayModeDownload => tr(
    en: 'Download only',
    ru: 'Только загрузка',
    tr: 'Yalnızca indirme',
    ptBr: 'Somente download',
    zhHans: '仅下载',
    zhHant: '僅下載',
  );

  String get networkSpeedPrioritizeUploadTitle => tr(
    en: 'Prioritize upload speed',
    ru: 'Сначала показывать отдачу',
    tr: 'Yükleme hızına öncelik ver',
    ptBr: 'Priorizar velocidade de upload',
    zhHans: '优先显示上传速度',
    zhHant: '優先顯示上傳速度',
  );

  String get networkSpeedPrioritizeUploadSubtitle => tr(
    en: 'in total mode, upload speed is shown before download',
    ru: 'в режиме общей скорости отдача будет стоять перед загрузкой',
    tr: 'toplam modda yükleme hızı indirmeden önce gösterilir',
    ptBr: 'no modo total, a velocidade de upload aparece antes do download',
    zhHans: '在总速度模式下，上传速度会显示在下载速度之前。',
    zhHant: '在總速度模式下，上傳速度會顯示在下載速度之前。',
  );

  String get networkSpeedDisableChipBackgroundTitle => tr(
    en: 'Disable chip background',
    ru: 'Отключить фон чипов',
    tr: 'Çip arka planını devre dışı bırak',
    ptBr: 'Desativar fundo do chip',
    zhHans: '禁用胶囊背景',
    zhHant: '停用膠囊背景',
  );

  String get networkSpeedDisableChipBackgroundSubtitle => tr(
    en: 'removes the pill background from network speed chips in Now Bar',
    ru: 'убирает плашку у чипов скорости в Now Bar',
    tr: 'Now Bar\'daki ağ hızı çiplerinden kapsül arka planını kaldırır',
    ptBr:
        'remove o fundo em formato de pílula dos chips de velocidade da rede na Now Bar',
    zhHans: '移除 Now Bar 中网速胶囊的药丸背景。',
    zhHant: '移除 Now Bar 中網速膠囊的藥丸背景。',
  );

  String get networkSpeedDailyUsageTitle => tr(
    en: 'Show daily internet usage',
    ru: '\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0442\u044c \u0434\u043d\u0435\u0432\u043d\u043e\u0439 \u0442\u0440\u0430\u0444\u0438\u043a',
  );

  String get networkSpeedDailyUsageSubtitle => tr(
    en: 'adds a third notification line with today\'s Wi-Fi and mobile data usage',
    ru: '\u0434\u043e\u0431\u0430\u0432\u043b\u044f\u0435\u0442 \u0442\u0440\u0435\u0442\u044c\u044e \u0441\u0442\u0440\u043e\u043a\u0443 \u0443\u0432\u0435\u0434\u043e\u043c\u043b\u0435\u043d\u0438\u044f \u0441 \u0434\u043d\u0435\u0432\u043d\u044b\u043c \u0442\u0440\u0430\u0444\u0438\u043a\u043e\u043c Wi-Fi \u0438 \u043c\u043e\u0431\u0438\u043b\u044c\u043d\u043e\u0439 \u0441\u0435\u0442\u0438',
  );

  String get textProgressTitle => tr(
    en: 'Text progress',
    es: 'Progreso en el texto',
    de: 'Fortschritt im Text',
    ru: 'Текстовые прогрессы',
    tr: 'Metin ilerlemesi',
    ptBr: 'Progresso baseado em texto ao invés de barra',
    zhHans: '文本进度',
    zhHant: '文字進度',
    ko: '텍스트 진행률',
  );

  String get nativeProgressDescription => tr(
    en: 'uses Android progress from notifications when available',
    es: 'usa el progreso de Android de las notificaciones cuando está disponible',
    de: 'verwendet den Android-Fortschritt aus Benachrichtigungen, sofern verfügbar',
    ru: 'использует прогресс из Android-уведомлений, если он есть',
    tr: 'varsa Android bildirimlerindeki ilerlemeyi kullanır',
    ptBr: 'usa barras de progresso do Android quando disponível',
    zhHans: '可用时使用 Android 通知中的进度',
    zhHant: '可用時使用 Android 通知中的進度',
    ko: '알림의 Android 진행률이 있다면 사용합니다.',
  );

  String get textProgressDescription => tr(
    en: 'detects progress from notification text like 42%',
    es: 'detecta el progreso en el texto de las notificaciones, como 42%',
    de: 'erkennt Fortschritt im Benachrichtigungstext, etwa 42 %',
    ru: 'определяет прогресс из текста уведомлений, например 42%',
    tr: '42% gibi bildirim metnindeki ilerlemeyi algılar',
    ptBr: 'detecta o progresso a partir do texto da notificação. Ex: 42%',
    zhHans: '从通知文本中识别进度，例如 42%',
    zhHant: '從通知文字中識別進度，例如 42%',
    ko: '42%와 같은 진행률을 알림에서 인식합니다.',
  );

  String get otpCodesDescription => tr(
    en: 'detects verification codes and shows them in Live Updates',
    es: 'detecta códigos de verificación y los muestra en Live Updates',
    de: 'erkennt Bestätigungscodes und zeigt sie in Live Updates an',
    ru: 'находит коды подтверждения и показывает их в Live Updates',
    tr: 'doğrulama kodlarını algılar ve Live Updates içinde gösterir',
    ptBr: 'detecta códigos de verificação e exibe como notificações ao vivo',
    zhHans: '识别验证码并在 Live Updates 中显示',
    zhHant: '識別驗證碼並在 Live Updates 中顯示',
    ko: '인증 코드를 감지해서 Live Updates에 표시합니다.',
  );

  String get autoCopyCodeDescription => tr(
    en: 'copies detected OTP codes to clipboard automatically',
    es: 'copia automáticamente los códigos OTP detectados al portapapeles',
    de: 'kopiert erkannte OTP-Codes automatisch in die Zwischenablage',
    ru: 'автоматически копирует найденные OTP-коды в буфер обмена',
    tr: 'algılanan OTP kodlarını otomatik olarak panoya kopyalar',
    ptBr: 'quando detectado, copia automaticamente os códigos OTP',
    zhHans: '自动将识别到的 OTP 验证码复制到剪贴板',
    zhHant: '自動將識別到的 OTP 驗證碼複製到剪貼簿',
    ko: 'OTP 코드를 감지해서 자동으로 클립보드에 복사합니다.',
  );

  String get removeOriginalMessageDescription => tr(
    en: 'tries to hide the original notification after conversion',
    es: 'intenta ocultar la notificación original después de convertirla',
    de: 'versucht, die Originalbenachrichtigung nach der Umwandlung auszublenden',
    ru: 'пытается скрыть исходное уведомление после конвертации',
    tr: 'dönüştürmeden sonra orijinal bildirimi gizlemeyi dener',
    ptBr: 'tenta esconder a notificação original após a conversão',
    zhHans: '转换后尝试隐藏原始通知',
    zhHant: '轉換後嘗試隱藏原始通知',
    ko: '변환 후 기존 알림을 제거합니다.',
  );

  String get taxiDescription => tr(
    en: 'shows taxi ride state as a Live Update',
    es: 'muestra el estado del viaje en taxi como Live Update',
    de: 'zeigt den Status einer Taxifahrt als Live Update an',
    ru: 'показывает состояние поездки такси в Live Updates',
    tr: 'taksi yolculuğu durumunu Live Update olarak gösterir',
    ptBr: 'mostra o estado da corrida de táxi com atualizações ao vivo',
    zhHans: '将打车行程状态显示为 Live Update',
    zhHant: '將計程車行程狀態顯示為 Live Update',
    ko: '택시 탑승 상태를 Live Updates에 표시합니다.',
  );

  String get deliveriesDescription => tr(
    en: 'shows delivery progress from food and shopping apps',
    es: 'muestra el progreso de entregas de aplicaciones de comida y compras',
    de: 'zeigt den Lieferfortschritt aus Essens- und Einkaufs-Apps an',
    ru: 'показывает прогресс доставки из еды и магазинов',
    tr: 'yemek ve alışveriş uygulamalarındaki teslimat ilerlemesini gösterir',
    ptBr: 'mostra o progresso da entrega de aplicativos como compras e comidas',
    zhHans: '显示外卖和购物应用的配送进度',
    zhHant: '顯示外送與購物應用程式的配送進度',
    ko: '배달 및 쇼핑 진행 상황을 표시합니다.',
  );

  String get allAppsDescription => tr(
    en: 'converts matching notifications from every app',
    es: 'convierte las notificaciones que cumplen las reglas de todas las aplicaciones',
    de: 'wandelt passende Benachrichtigungen aus allen Apps um',
    ru: 'конвертирует подходящие уведомления из всех приложений',
    tr: 'tüm uygulamalardan eşleşen bildirimleri dönüştürür',
    ptBr: 'converte notificações recebidas de todos os apps',
    zhHans: '转换所有应用中匹配的通知',
    zhHant: '轉換所有應用程式中符合條件的通知',
    ko: '모든 앱의 일치하는 알림을 변환합니다.',
  );

  String get onlySelectedDescription => tr(
    en: 'converts notifications only from apps you select',
    es: 'convierte notificaciones solo de las aplicaciones que selecciones',
    de: 'wandelt nur Benachrichtigungen ausgewählter Apps um',
    ru: 'конвертирует уведомления только из выбранных приложений',
    tr: 'yalnızca seçtiğiniz uygulamalardan gelen bildirimleri dönüştürür',
    ptBr: 'converte notificação apenas dos apps que você selecionar',
    zhHans: '仅转换你选择的应用通知',
    zhHant: '僅轉換你選取的應用程式通知',
    ko: '선택한 앱의 알림만 변환합니다.',
  );

  String get excludeSelectedDescription => tr(
    en: 'converts every app except the apps you select',
    es: 'convierte todas las aplicaciones excepto las que selecciones',
    de: 'wandelt alle Apps außer den ausgewählten um',
    ru: 'конвертирует все приложения, кроме выбранных',
    tr: 'seçtiğiniz uygulamalar dışındaki tüm uygulamaları dönüştürür',
    ptBr: 'converte todos os aplicativos, exceto os que você selecionar',
    zhHans: '转换除所选应用之外的所有应用',
    zhHant: '轉換除所選應用程式之外的所有應用程式',
    ko: '선택한 앱의 알림만 변환하지 않습니다.',
  );

  String get vpnsDescription => tr(
    en: 'shows active VPN traffic and connection state',
    es: 'muestra el tráfico y el estado de conexión de la VPN activa',
    de: 'zeigt Datenverkehr und Verbindungsstatus aktiver VPNs an',
    ru: 'показывает трафик и состояние активного VPN',
    tr: 'aktif VPN trafiğini ve bağlantı durumunu gösterir',
    ptBr: 'mostra o tráfego da VPN ativa e o estado da conexão',
    zhHans: '显示活动 VPN 流量和连接状态',
    zhHant: '顯示作用中 VPN 流量與連線狀態',
    ko: '활성화된 VPN의 트래픽 및 연결 상태를 표시합니다.',
  );

  String get externalDevicesDescription => tr(
    en: 'shows connected external devices in Live Updates',
    es: 'muestra dispositivos externos conectados en Live Updates',
    de: 'zeigt verbundene externe Geräte in Live Updates an',
    ru: 'показывает подключенные внешние устройства в Live Updates',
    tr: 'bağlı harici cihazları Live Updates içinde gösterir',
    ptBr: 'mostra dispositivos externos conectados em Notificações ao vivo',
    zhHans: '在 Live Updates 中显示已连接的外部设备',
    zhHant: '在 Live Updates 中顯示已連接的外部裝置',
    ko: '연결된 외부 장치를 Live Updates에 표시합니다.',
  );

  String get ignoreDebuggingDevicesDescription => tr(
    en: 'hides ADB and debugging device notifications',
    es: 'oculta notificaciones de ADB y dispositivos de depuración',
    de: 'blendet ADB- und Debugging-Gerätebenachrichtigungen aus',
    ru: 'скрывает ADB и уведомления отладочных устройств',
    tr: 'ADB ve hata ayıklama cihazı bildirimlerini gizler',
    ptBr: 'oculta notificações de ADB e dispositivos de depuração',
    zhHans: '隐藏 ADB 和调试设备通知',
    zhHant: '隱藏 ADB 與偵錯裝置通知',
    ko: 'ADB 및 디버깅 기기를 알림에서 숨깁니다.',
  );

  String get mediaPlaybackDescription => tr(
    en: 'shows track controls and playback status in Live Updates',
    es: 'muestra controles y estado de reproducción en Live Updates',
    de: 'zeigt Mediensteuerung und Wiedergabestatus in Live Updates an',
    ru: 'показывает управление треком и статус воспроизведения',
    tr: 'parça kontrollerini ve oynatma durumunu Live Updates içinde gösterir',
    ptBr:
        'mostra controles da faixa e estado da reprodução em Notificações ao vivo',
    zhHans: '在 Live Updates 中显示曲目控制和播放状态',
    zhHant: '在 Live Updates 中顯示曲目控制與播放狀態',
    ko: '미디어 제어 및 재생 상태를 Live Updates에 표시합니다.',
  );

  String get showMediaOnLockDescription => tr(
    en: 'allows media Live Updates on the lockscreen',
    es: 'permite Live Updates multimedia en la pantalla de bloqueo',
    de: 'erlaubt Medien-Live-Updates auf dem Sperrbildschirm',
    ru: 'разрешает показывать медиа Live Updates на экране блокировки',
    tr: 'kilit ekranında medya Live Updates gösterilmesine izin verir',
    ptBr: 'permite Notificações ao vivo de mídia na tela de bloqueio',
    zhHans: '允许在锁屏显示媒体 Live Updates',
    zhHant: '允許在鎖定畫面顯示媒體 Live Updates',
    ko: '미디어의 Live Updates를 잠금화면에 표시합니다.',
  );

  String get useSymbolsInMediaPlayerDescription => tr(
    en: 'uses ▶, ⏸, ⏮ and ⏭ instead of text actions',
    es: 'usa ▶, ⏸, ⏮ y ⏭ en lugar de acciones de texto',
    de: 'verwendet ▶, ⏸, ⏮ und ⏭ statt Aktionsbeschriftungen',
    ru: 'использует ▶, ⏸, ⏮ и ⏭ вместо текстовых действий',
    tr: 'metin eylemleri yerine ▶, ⏸, ⏮ ve ⏭ kullanır',
    ptBr: 'usa ▶, ⏸, ⏮ e ⏭ em vez de ações em texto',
    zhHans: '使用 ▶、⏸、⏮ 和 ⏭ 代替文字操作',
    zhHant: '使用 ▶、⏸、⏮ 和 ⏭ 取代文字操作',
    ko: '텍스트 동작 대신에 ▶, ⏸, ⏮, ⏭를 사용합니다.',
  );

  String get callsDescription => tr(
    en: 'shows ongoing calls as Live Updates',
    es: 'muestra las llamadas en curso como Live Updates',
    de: 'zeigt laufende Anrufe als Live Updates an',
    ru: 'показывает активные звонки в Live Updates',
    tr: 'devam eden aramaları Live Updates olarak gösterir',
    ptBr: 'mostra chamadas em andamento como Notificações ao vivo',
    zhHans: '将正在进行的通话显示为 Live Updates',
    zhHant: '將進行中的通話顯示為 Live Updates',
    ko: '진행 중인 통화를 Live Updates에 표시합니다.',
  );

  String get navigationMapsDescription => tr(
    en: 'shows the direction and distance in Live Updates',
    es: 'muestra la dirección y la distancia en Live Updates',
    de: 'zeigt Richtung und Entfernung in Live Updates an',
    ru: 'показывает направление и расстояние в Live Updates',
    tr: 'yönü ve mesafeyi Live Updates içinde gösterir',
    ptBr: 'mostra direção e distância em Notificações ao vivo',
    zhHans: '在 Live Updates 中显示方向和距离',
    zhHant: '在 Live Updates 中顯示方向與距離',
    ko: '방향 및 거리를 Live Updates에 표시합니다.',
  );

  String get weatherBroadcastsDescription => tr(
    en: 'shows weather alerts and forecast notifications',
    es: 'muestra avisos meteorológicos y notificaciones del pronóstico',
    de: 'zeigt Wetterwarnungen und Vorhersagebenachrichtigungen an',
    ru: 'показывает погодные уведомления и прогнозы',
    tr: 'hava durumu uyarılarını ve tahmin bildirimlerini gösterir',
    ptBr: 'mostra alertas de clima e notificações de previsão',
    zhHans: '显示天气警报和预报通知',
    zhHant: '顯示天氣警報與預報通知',
    ko: '일기예보 및 날씨를 알림에 표시합니다.',
  );

  String get appLanguageDescription => tr(
    en: 'changes the language used by LiveBridge UI',
    es: 'cambia el idioma de la interfaz de LiveBridge',
    de: 'ändert die Sprache der LiveBridge-Oberfläche',
    ru: 'меняет язык интерфейса LiveBridge',
    tr: 'LiveBridge arayüzünde kullanılan dili değiştirir',
    ptBr: 'altera o idioma usado pela interface do LiveBridge',
    zhHans: '更改 LiveBridge 界面使用的语言',
    zhHant: '變更 LiveBridge 介面使用的語言',
    ko: 'LiveBridge UI의 언어를 변경합니다.',
  );

  String get keepAliveForegroundDescription => tr(
    en: 'uses an alternate foreground mode for stricter firmwares',
    es: 'usa un modo alternativo de servicio en primer plano para firmwares más restrictivos',
    de: 'verwendet einen alternativen Vordergrundmodus für restriktivere Firmware',
    ru: 'использует альтернативный foreground-режим для строгих прошивок',
    tr: 'daha katı yazılımlar için alternatif ön plan modunu kullanır',
    ptBr: 'usa um modo foreground alternativo para firmwares mais restritos',
    zhHans: '为限制更严格的系统使用备用前台模式',
    zhHant: '為限制更嚴格的系統使用備用前景模式',
    ko: '엄격한 펌웨어를 사용하는 기기에서 안정적으로 작동하기 위해 대체 백그라운드 모드를 사용합니다.',
  );

  String get syncDndDescription => tr(
    en: 'syncs Live Updates behavior with Do Not Disturb',
    es: 'sincroniza Live Updates con No molestar',
    de: 'passt Live Updates an „Nicht stören“ an',
    ru: 'синхронизирует поведение Live Updates с режимом Не беспокоить',
    tr: 'Live Updates davranışını Rahatsız Etmeyin ile eşitler',
    ptBr:
        'sincroniza o comportamento das Notificações ao vivo com o Não Perturbe',
    zhHans: '将 Live Updates 行为与勿扰模式同步',
    zhHant: '將 Live Updates 行為與勿擾模式同步',
    ko: '방해금지 모드를 Live Updates에도 적용합니다.',
  );

  String get preventDismissingDescription => tr(
    en: 'restores the LiveBridge notification after it is swiped away',
    es: 'restaura la notificación de LiveBridge después de descartarla deslizando',
    de: 'stellt die LiveBridge-Benachrichtigung nach dem Wegwischen wieder her',
    ru: 'восстанавливает уведомление LiveBridge после свайпа',
    tr: 'LiveBridge bildirimi kaydırılıp kapatıldıktan sonra geri yükler',
    ptBr: 'restaura a notificação do LiveBridge depois que ela é dispensada',
    zhHans: '在 LiveBridge 通知被滑掉后恢复它',
    zhHant: '在 LiveBridge 通知被滑掉後恢復它',
    ko: 'LiveBridge 알림을 지웠을 때 복구합니다.',
  );

  String get hideLockscreenContentDescription => tr(
    en: 'shows Content hidden instead of notification text on the lockscreen',
    es: 'muestra «Contenido oculto» en lugar del texto de la notificación en la pantalla de bloqueo',
    de: 'zeigt „Inhalt ausgeblendet“ statt des Benachrichtigungstextes auf dem Sperrbildschirm',
    ru: 'показывает Content hidden вместо текста уведомления на локскрине',
    tr: 'kilit ekranında bildirim metni yerine Content hidden gösterir',
    ptBr:
        'mostra Content hidden em vez do texto da notificação na tela de bloqueio',
    zhHans: '在锁屏上显示 Content hidden 而不是通知文本',
    zhHant: '在鎖定畫面顯示 Content hidden 而不是通知文字',
    ko: '잠금화면에 알림을 표시할 때 내용을 숨깁니다.',
  );

  String get convertedNotificationSoundDescription => tr(
    en: 'plays a sound when a converted notification first appears',
    es: 'reproduce un sonido cuando aparece una notificación convertida por primera vez',
    de: 'spielt beim ersten Erscheinen einer umgewandelten Benachrichtigung einen Ton ab',
    ru: 'воспроизводит звук при первом появлении конвертированного уведомления',
    tr: 'dönüştürülen bir bildirim ilk göründüğünde ses çalar',
    ptBr:
        'reproduz um som quando uma notificação convertida aparece pela primeira vez',
    zhHans: '转换通知首次出现时播放声音',
    zhHant: '轉換通知首次出現時播放聲音',
    ko: '변환된 알림이 처음 표시될 때 소리를 재생합니다.',
  );

  String get disableHintsDescription => tr(
    en: 'hides info icons and hint popovers across the redesign UI',
    es: 'oculta iconos de información y consejos en la interfaz',
    de: 'blendet Infosymbole und Hinweise in der Oberfläche aus',
    ru: 'скрывает иконки info и всплывающие подсказки в новом интерфейсе',
    tr: 'yeni arayüzde bilgi simgelerini ve ipucu pencerelerini gizler',
    ptBr: 'oculta ícones de informação e dicas na nova interface',
    zhHans: '隐藏新版界面中的信息图标和提示弹窗',
    zhHant: '隱藏新版介面中的資訊圖示與提示彈窗',
    ko: '새로 디자인된 UI에서 정보 아이콘 및 힌트 팝업을 숨깁니다.',
  );

  String get conversionLogDescription => tr(
    en: 'keeps recent converted notifications for debugging',
    es: 'guarda las notificaciones convertidas recientes para el diagnóstico',
    de: 'speichert zuletzt umgewandelte Benachrichtigungen zur Fehlerdiagnose',
    ru: 'сохраняет последние конвертации для отладки',
    tr: 'hata ayıklama için son dönüştürülen bildirimleri saklar',
    ptBr: 'mantém notificações convertidas recentemente para depuração',
    zhHans: '保留最近转换的通知用于调试',
    zhHant: '保留最近轉換的通知用於偵錯',
    ko: '최근에 변환된 알림을 디버깅을 위해 남깁니다.',
  );

  String get logLengthDescription => tr(
    en: 'limits how much conversion log data is kept on device',
    es: 'limita la cantidad de datos del registro guardados en el dispositivo',
    de: 'begrenzt die auf dem Gerät gespeicherten Protokolldaten',
    ru: 'ограничивает объем лога конвертаций на устройстве',
    tr: 'cihazda tutulacak dönüştürme günlüğü verisi miktarını sınırlar',
    ptBr: 'limita quantos dados do log de conversões ficam no dispositivo',
    zhHans: '限制设备上保留的转换日志数据量',
    zhHant: '限制裝置上保留的轉換記錄資料量',
    ko: '기기에 어느 정도의 로그를 저장할 지 제한합니다.',
  );

  String get networkSpeedDescription => tr(
    en: 'shows current network traffic as a Live Update',
    es: 'muestra el tráfico de red actual como Live Update',
    de: 'zeigt den aktuellen Netzwerkverkehr als Live Update an',
    ru: 'показывает текущий трафик сети как Live Update',
    tr: 'mevcut ağ trafiğini Live Update olarak gösterir',
    ptBr: 'mostra o tráfego de rede atual como Notificação ao vivo',
    zhHans: '将当前网络流量显示为 Live Update',
    zhHant: '將目前網路流量顯示為 Live Update',
    ko: '현재 네트워크 속도를 Live Updates에 표시합니다.',
  );

  String get networkSpeedThresholdDescription => tr(
    en: 'hides the network speed Live Update below this traffic level',
    es: 'oculta el Live Update de velocidad de red por debajo de este nivel de tráfico',
    de: 'blendet das Netzwerkgeschwindigkeits-Live-Update unterhalb dieses Datenverkehrs aus',
    ru: 'скрывает Live Update скорости сети ниже этого порога',
    tr: 'ağ hızı bu seviyenin altındaysa Live Update öğesini gizler',
    ptBr:
        'oculta a Notificação ao vivo de velocidade de rede abaixo deste nível',
    zhHans: '当网速低于此阈值时隐藏网络速度 Live Update',
    zhHant: '當網速低於此門檻時隱藏網路速度 Live Update',
    ko: '네트워크 속도 제한이 기준치 미만이면 숨깁니다.',
  );

  String get xiaomiHyperIslandDescription => tr(
    en: 'enables Xiaomi HyperIsland-specific Live Updates behavior',
    es: 'activa el comportamiento de Live Updates específico de Xiaomi HyperIsland',
    de: 'aktiviert das Xiaomi-HyperIsland-spezifische Verhalten für Live Updates',
    ru: 'включает поведение Live Updates для Xiaomi HyperIsland',
    tr: 'Xiaomi HyperIsland için özel Live Updates davranışını etkinleştirir',
    ptBr:
        'ativa comportamento de Notificações ao vivo específico do Xiaomi HyperIsland',
    zhHans: '启用 Xiaomi HyperIsland 专用的 Live Updates 行为',
    zhHant: '啟用 Xiaomi HyperIsland 專用的 Live Updates 行為',
    ko: '샤오미 HyperIsland 형식의 Live Updates를 활성화합니다.',
  );

  String get aospCuttingDescription => tr(
    en: 'shortens island text on AOSP-like firmwares that clip long content',
    es: 'acorta el texto de la cápsula en firmwares basados en AOSP que recortan contenido largo',
    de: 'kürzt den Kapseltext auf AOSP-basierten Systemen, die lange Inhalte abschneiden',
    ru: 'укорачивает текст острова на AOSP-прошивках, где длинный текст обрезается',
    tr: 'uzun içeriği kırpan AOSP benzeri yazılımlarda ada metnini kısaltır',
    ptBr:
        'encurta o texto da ilha em firmwares parecidos com AOSP que cortam conteúdo longo',
    zhHans: '在会截断长内容的类 AOSP 系统上缩短岛内文本',
    zhHant: '在會截斷長內容的類 AOSP 系統上縮短島內文字',
    ko: '긴 내용을 자르는 AOSP 기반 펌웨어를 위해 텍스트를 제한합니다.',
  );

  String get aospCuttingLengthDescription => tr(
    en: 'sets the maximum island text length for AOSP cutting',
    es: 'establece la longitud máxima del texto para el recorte en AOSP',
    de: 'legt die maximale Kapseltextlänge für die AOSP-Textkürzung fest',
    ru: 'задает максимальную длину текста острова для AOSP-обрезки',
    tr: 'AOSP kırpması için en uzun ada metni uzunluğunu ayarlar',
    ptBr: 'define o comprimento máximo do texto da ilha para o corte AOSP',
    zhHans: '设置 AOSP 截断模式下岛内文本的最大长度',
    zhHant: '設定 AOSP 截斷模式下島內文字的最大長度',
    ko: 'AOSP 잘림을 막기 위해 최대 아일랜드 길이를 설정합니다.',
  );

  String get updateFrequencyDescription => tr(
    en: 'controls how often animated island text frames are refreshed',
    es: 'controla la frecuencia de actualización de los fotogramas de texto de la cápsula animada',
    de: 'legt fest, wie oft Textbilder der animierten Kapsel aktualisiert werden',
    ru: 'задает частоту обновления кадров анимации текста острова',
    tr: 'animasyonlu ada metni karelerinin ne sıklıkta yenileneceğini ayarlar',
    ptBr:
        'controla a frequência de atualização dos quadros de texto da ilha animada',
    zhHans: '控制动画岛文本帧的刷新频率',
    zhHant: '控制動畫島文字影格的刷新頻率',
    ko: '아일랜드 애니메이션의 갱신 주기를 설정합니다.',
  );

  String get otpDedupDescription => tr(
    en: 'reduces repeated OTP notifications from the same source',
    es: 'reduce las notificaciones OTP repetidas del mismo origen',
    de: 'verringert wiederholte OTP-Benachrichtigungen aus derselben Quelle',
    ru: 'уменьшает повторы OTP-уведомлений из одного источника',
    tr: 'aynı kaynaktan gelen tekrarlı OTP bildirimlerini azaltır',
    ptBr: 'reduz notificações OTP repetidas da mesma origem',
    zhHans: '减少同一来源的重复 OTP 通知',
    zhHant: '減少同一來源的重複 OTP 通知',
    ko: '동일 출처에서의 중복 OTP를 줄입니다.',
  );

  String get smartConversionDedupDescription => tr(
    en: 'reduces repeated smart conversion notifications',
    es: 'reduce las notificaciones repetidas de conversión inteligente',
    de: 'verringert wiederholte Benachrichtigungen der intelligenten Umwandlung',
    ru: 'уменьшает повторы уведомлений умной конвертации',
    tr: 'tekrarlı akıllı dönüştürme bildirimlerini azaltır',
    ptBr: 'reduz notificações repetidas de conversão inteligente',
    zhHans: '减少重复的智能转换通知',
    zhHant: '減少重複的智慧轉換通知',
    ko: '반복되는 스마트 변환 알림을 줄입니다.',
  );

  String get animatedIslandDescription => tr(
    en: 'adds smooth island text animations for supported conversions',
    es: 'añade animaciones de texto suaves a las conversiones compatibles',
    de: 'fügt unterstützten Umwandlungen flüssige Kapseltextanimationen hinzu',
    ru: 'добавляет плавные анимации текста острова',
    tr: 'desteklenen dönüştürmeler için akıcı ada metni animasyonları ekler',
    ptBr:
        'adiciona animações suaves ao texto da ilha nas conversões compatíveis',
    zhHans: '为支持的转换添加流畅的岛内文本动画',
    zhHant: '為支援的轉換加入流暢的島內文字動畫',
    ko: '지원되는 변환에서 자연스러운 아일랜드 텍스트 애니메이션을 추가합니다.',
  );
  String get wearOsLiveUpdatesTitle => tr(
    en: "Live Updates on Wear OS",
    ru: "Live Updates на Wear OS",
    es: "Live Updates en Wear OS",
    de: "Live Updates auf Wear OS",
    tr: "Wear OS’te Live Updates",
    ptBr: "Live Updates no Wear OS",
    zhHans: "Wear OS 实时更新",
    zhHant: "Wear OS 即時更新",
    ko: "Wear OS 실시간 업데이트",
  );

  String get wearOsLiveUpdatesUnavailable => tr(
    en: "Requires Android 17",
    ru: "Требуется Android 17",
    es: "Requiere Android 17",
    de: "Erfordert Android 17",
    tr: "Android 17 gerekir",
    ptBr: "Requer Android 17",
    zhHans: "需要 Android 17",
    zhHant: "需要 Android 17",
    ko: "Android 17 필요",
  );

  String get wearOsLiveUpdatesDescription => tr(
    en: "allows Android to forward converted Live Updates to your watch. Requires Android 17, Wear OS 7 and support from the watch manufacturer. Original app notifications may also appear on the watch",
    ru: "разрешает Android пересылать преобразованные Live Updates на часы. Нужны Android 17, Wear OS 7 и поддержка производителя часов. На часах также могут появляться исходные уведомления приложений",
    es: "permite a Android enviar Live Updates convertidas al reloj. Requiere Android 17, Wear OS 7 y soporte del fabricante del reloj. También pueden aparecer las notificaciones originales",
    de: "erlaubt Android, umgewandelte Live Updates an die Uhr weiterzuleiten. Erfordert Android 17, Wear OS 7 und Unterstützung des Uhrenherstellers. Die ursprünglichen Benachrichtigungen können ebenfalls auf der Uhr erscheinen",
    tr: "Android’in dönüştürülen Live Updates bildirimlerini saate iletmesine izin verir. Android 17, Wear OS 7 ve saat üreticisinin desteği gerekir. Asıl uygulama bildirimleri de saatte görünebilir",
    ptBr:
        "permite que o Android envie Live Updates convertidos ao relógio. Requer Android 17, Wear OS 7 e suporte do fabricante do relógio. As notificações originais também podem aparecer no relógio",
    zhHans:
        "允许 Android 将转换后的实时更新转发到手表。需要 Android 17、Wear OS 7 和手表厂商支持。原应用通知也可能显示在手表上。",
    zhHant:
        "允許 Android 將轉換後的即時更新轉送至手錶。需要 Android 17、Wear OS 7 及手錶廠商支援。原應用程式通知也可能顯示在手錶上。",
    ko: "Android가 변환된 실시간 업데이트를 시계로 전달하도록 허용합니다. Android 17, Wear OS 7 및 시계 제조사의 지원이 필요합니다. 원래 앱 알림도 시계에 표시될 수 있습니다.",
  );
  String get searchNoResults => tr(
    en: 'No results',
    ru: 'Ничего не найдено',
    es: 'Sin resultados',
    de: 'Keine Ergebnisse',
    tr: 'Sonuç bulunamadı',
    ptBr: 'Nenhum resultado',
    zhHans: '没有结果',
    zhHant: '沒有結果',
    ko: '검색 결과 없음',
  );
}
