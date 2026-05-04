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
    if (isPtBr) return ptBr ?? en;
    if (isZhHant) return zhHant ?? zhHans ?? en;
    if (isZhHans) return zhHans ?? zhHant ?? en;
    return en;
  }

  static AppStrings of(BuildContext context) {
    return AppStrings(locale: Localizations.localeOf(context));
  }

  String get refresh => tr(
    en: 'Refresh',
    ru: 'Обновить',
    tr: 'Yenile',
    ptBr: 'Atualizar',
    zhHans: '刷新',
    zhHant: '重新整理',
  );

  String get permissionGranted => tr(
    en: 'Notification permission granted.',
    ru: 'Разрешение на уведомления выдано.',
    tr: 'Bildirim izni verildi.',
    ptBr: 'Permissão de notificações concedida.',
    zhHans: '通知权限已授予。',
    zhHant: '通知權限已授予。',
  );

  String get permissionDenied => tr(
    en: 'Notification permission was not granted.',
    ru: 'Разрешение на уведомления не выдано.',
    tr: 'Bildirim izni verilmedi.',
    ptBr: 'Permissão de notificações não concedida.',
    zhHans: '未授予通知权限。',
    zhHant: '未授予通知權限。',
  );

  String get listenerUnavailable => tr(
    en: 'Unable to open Listener settings on this device.',
    ru: 'Не удалось открыть настройки Listener.',
    tr: 'Bu cihazda Listener ayarları açılamıyor.',
    ptBr:
        'Não foi possível abrir as configurações do Listener neste dispositivo.',
    zhHans: '此设备无法打开监听器设置。',
    zhHant: '此裝置無法開啟監聽器設定。',
  );

  String get notificationsUnavailable => tr(
    en: 'Unable to open app notification settings.',
    ru: 'Не удалось открыть настройки уведомлений.',
    tr: 'Uygulama bildirim ayarları açılamıyor.',
    ptBr: 'Não foi possível abrir as configurações de notificação do app.',
    zhHans: '无法打开应用通知设置。',
    zhHant: '無法開啟應用通知設定。',
  );

  String get liveUpdatesUnavailable => tr(
    en: 'Unable to open Live Updates settings on this device.',
    ru: 'Не удалось открыть настройки Live Updates.',
    tr: 'Bu cihazda Live Updates ayarları açılamıyor.',
    ptBr:
        'Não foi possível abrir as configurações de Live Updates neste dispositivo.',
    zhHans: '此设备无法打开 Live Updates 设置。',
    zhHant: '此裝置無法開啟 Live Updates 設定。',
  );

  String get githubOpenFailed => tr(
    en: 'Unable to open GitHub link.',
    ru: 'Не удалось открыть ссылку GitHub.',
    tr: 'GitHub bağlantısı açılamıyor.',
    ptBr: 'Não foi possível abrir o link do GitHub.',
    zhHans: '无法打开 GitHub 链接。',
    zhHant: '無法開啟 GitHub 連結。',
  );

  String get linkOpenFailed => tr(
    en: 'Unable to open link.',
    ru: 'Не удалось открыть ссылку.',
    tr: 'Bağlantı açılamıyor.',
    ptBr: 'Não foi possível abrir o link.',
    zhHans: '无法打开链接。',
    zhHant: '無法開啟連結。',
  );

  String get updateCheckFailed => tr(
    en: 'Unable to check updates. Try disabling VPN.',
    ru: 'Не удалось проверить обновления. Попробуйте отключить VPN.',
    tr: 'Güncellemeler denetlenemiyor. VPN\'i kapatmayı deneyin.',
    ptBr: 'Não foi possível verificar atualizações. Tente desativar a VPN.',
    zhHans: '无法检查更新。请尝试关闭 VPN。',
    zhHant: '無法檢查更新。請嘗試關閉 VPN。',
  );

  String get dictionaryEmpty => tr(
    en: 'Dictionary is empty or invalid.',
    ru: 'Словарь пустой или поврежден.',
    tr: 'Sözlük boş veya geçersiz.',
    ptBr: 'O dicionário está vazio ou inválido.',
    zhHans: '词典为空或无效。',
    zhHant: '字典為空或無效。',
  );

  String get dictionaryUpdateDone => tr(
    en: 'Dictionary updated from GitHub.',
    ru: 'Словарь обновлен из GitHub.',
    tr: 'Sözlük GitHub\'dan güncellendi.',
    ptBr: 'Dicionário atualizado do GitHub.',
    zhHans: '词典已从 GitHub 更新。',
    zhHant: '字典已從 GitHub 更新。',
  );

  String get dictionaryInvalid => tr(
    en: 'Invalid dictionary JSON.',
    ru: 'Невалидный JSON словаря.',
    tr: 'Geçersiz sözlük JSON\'u.',
    ptBr: 'JSON do dicionário inválido.',
    zhHans: '词典 JSON 无效。',
    zhHant: '字典 JSON 無效。',
  );

  String get dictionaryUpdateFailed => tr(
    en: 'Failed to update dictionary from GitHub.',
    ru: 'Не удалось обновить словарь из GitHub.',
    tr: 'Sözlük GitHub\'dan güncellenemedi.',
    ptBr: 'Falha ao atualizar o dicionário do GitHub.',
    zhHans: '从 GitHub 更新词典失败。',
    zhHant: '從 GitHub 更新字典失敗。',
  );

  String get dictionaryTitle => tr(
    en: 'Dictionary',
    ru: 'Словарь',
    tr: 'Sözlük',
    ptBr: 'Dicionário',
    zhHans: '词典',
    zhHant: '字典',
  );

  String get dictionaryManageSubtitle => tr(
    en: 'tap to manage',
    ru: 'нажмите для управления',
    tr: 'yönetmek için açın',
    ptBr: 'toque para gerenciar',
    zhHans: '点按以管理',
    zhHant: '點按以管理',
  );

  String get dictionaryLanguagesTitle => tr(
    en: 'Dictionary languages',
    ru: 'Языки словаря',
    tr: 'Sözlük dilleri',
    ptBr: 'Idiomas do dicionário',
    zhHans: '词典语言',
    zhHant: '字典語言',
  );

  String get dictionaryLanguagesSubtitle => tr(
    en: 'tap to choose',
    ru: 'нажмите для выбора',
    tr: 'seçmek için açın',
    ptBr: 'toque para escolher',
    zhHans: '点按以选择',
    zhHant: '點按以選擇',
  );

  String get dictionaryLanguagesPickerTitle => tr(
    en: 'Select languages for conversion',
    ru: 'Выберите языки для конвертации',
    tr: 'Dönüştürme için dilleri seçin',
    ptBr: 'Selecione idiomas para conversão',
    zhHans: '选择用于转换的语言',
    zhHant: '選擇用於轉換的語言',
  );

  String get dictionaryUpdateAction => tr(
    en: 'Update dictionaries',
    ru: 'Обновить словари',
    tr: 'Sözlükleri güncelle',
    ptBr: 'Atualizar dicionários',
    zhHans: '更新词典',
    zhHant: '更新字典',
  );

  String get dictionaryUpdateDescription => tr(
    en: 'downloads the latest parser dictionaries for enabled languages',
    ru: 'загружает свежие словари парсинга для включенных языков',
    tr: 'etkin diller için en güncel ayrıştırma sözlüklerini indirir',
    ptBr:
        'baixa os dicionários de análise mais recentes para os idiomas ativados',
    zhHans: '下载已启用语言的最新解析词典',
    zhHant: '下載已啟用語言的最新解析字典',
  );

  String get dictionaryEditorTitle => tr(
    en: 'Dictionary editor',
    ru: 'Редактор словаря',
    tr: 'Sözlük düzenleyici',
    ptBr: 'Editor de dicionário',
    zhHans: '词典编辑器',
    zhHant: '字典編輯器',
  );

  String get dictionaryEditorDescription => tr(
    en: 'in-app dictionary editing will be added later',
    ru: 'редактирование словарей внутри приложения появится позже',
    tr: 'uygulama içi sözlük düzenleme daha sonra eklenecek',
    ptBr: 'a edição de dicionários dentro do app será adicionada depois',
    zhHans: '应用内词典编辑会稍后添加',
    zhHant: '應用程式內字典編輯稍後會加入',
  );

  String get dictionaryComingSoon => tr(
    en: '(coming soon)',
    ru: '(скоро)',
    tr: '(yakında)',
    ptBr: '(em breve)',
    zhHans: '（即将推出）',
    zhHant: '（即將推出）',
  );

  String get navHome => tr(
    en: 'Home',
    ru: 'Домой',
    tr: 'Ana sayfa',
    ptBr: 'Início',
    zhHans: '主页',
    zhHant: '首頁',
  );

  String get navRules => tr(
    en: 'Rules',
    ru: 'Правила',
    tr: 'Kurallar',
    ptBr: 'Regras',
    zhHans: '规则',
    zhHant: '規則',
  );

  String get navSettings => tr(
    en: 'Settings',
    ru: 'Настройки',
    tr: 'Ayarlar',
    ptBr: 'Configurações',
    zhHans: '设置',
    zhHant: '設定',
  );

  String get redesignRulesTitle => tr(
    en: 'Rules',
    ru: 'Правила',
    tr: 'Kurallar',
    ptBr: 'Regras',
    zhHans: '规则',
    zhHant: '規則',
  );

  String get appConfigTitle => tr(
    en: 'App config',
    ru: 'Настройки приложения',
    tr: 'Uygulama ayarları',
    ptBr: 'Configurações do app',
    zhHans: '应用设置',
    zhHant: '應用設定',
  );

  String get appLanguageTitle => tr(
    en: 'App language',
    ru: 'Язык приложения',
    tr: 'Uygulama dili',
    ptBr: 'Idioma do app',
    zhHans: '应用语言',
    zhHant: '應用語言',
  );

  String get appLanguagePickerTitle => tr(
    en: 'Choose app language',
    ru: 'Выберите язык приложения',
    tr: 'Uygulama dilini seçin',
    ptBr: 'Escolha o idioma do app',
    zhHans: '选择应用语言',
    zhHant: '選擇應用語言',
  );

  String get appLanguageSystem => tr(
    en: 'Auto',
    ru: 'Автовыбор',
    tr: 'Otomatik',
    ptBr: 'Automático',
    zhHans: '自动',
    zhHant: '自動',
  );

  String get brandSpecificTitle => tr(
    en: 'Brand-specific',
    ru: 'Brand-specific',
    tr: 'Markaya özel',
    ptBr: 'Específico da marca',
    zhHans: '品牌特定',
    zhHant: '品牌特定',
  );

  String get appUpdatesTitle => tr(
    en: 'App updates',
    ru: 'Обновления приложения',
    tr: 'Uygulama güncellemeleri',
    ptBr: 'Atualizações do app',
    zhHans: '应用更新',
    zhHant: '應用更新',
  );

  String get statusRunning => tr(
    en: 'Running',
    ru: 'Запущен',
    tr: 'Çalışıyor',
    ptBr: 'Em execução',
    zhHans: '正在运行',
    zhHant: '正在執行',
  );

  String get statusDisabled => tr(
    en: 'LiveBridge is disabled',
    ru: 'LiveBridge выключен',
    tr: 'LiveBridge devre dışı',
    ptBr: 'LiveBridge está desativado',
    zhHans: 'LiveBridge 已关闭',
    zhHant: 'LiveBridge 已關閉',
  );

  String get statusByPrefix => tr(
    en: 'by ',
    ru: 'by ',
    tr: 'by ',
    ptBr: 'por ',
    zhHans: 'by ',
    zhHant: 'by ',
  );

  String get discussTitle => tr(
    en: 'Discuss',
    ru: 'Discuss',
    tr: 'Tartış',
    ptBr: 'Discutir',
    zhHans: '讨论',
    zhHant: '討論',
  );

  String get discussSubtitle => tr(
    en: 'telegram topics',
    ru: 'telegram topics',
    tr: 'telegram konuları',
    ptBr: 'tópicos do Telegram',
    zhHans: 'telegram 话题',
    zhHant: 'telegram 話題',
  );

  String get rulesModeAllApps => tr(
    en: 'all apps',
    ru: 'все приложения',
    tr: 'tüm uygulamalar',
    ptBr: 'todos os apps',
    zhHans: '所有应用',
    zhHant: '所有應用程式',
  );

  String get rulesModeOnlySelected => tr(
    en: 'only selected',
    ru: 'только выбранные',
    tr: 'yalnızca seçilenler',
    ptBr: 'somente selecionados',
    zhHans: '仅已选择',
    zhHant: '僅已選取',
  );

  String get rulesModeExcludeSelected => tr(
    en: 'exclude selected',
    ru: 'исключая выбранные',
    tr: 'seçilenleri hariç tut',
    ptBr: 'excluir selecionados',
    zhHans: '排除已选择',
    zhHant: '排除已選取',
  );

  String get permissionCheckRequired => tr(
    en: 'check required',
    ru: 'требуется проверка',
    tr: 'kontrol gerekli',
    ptBr: 'verificação necessária',
    zhHans: '需要检查',
    zhHant: '需要檢查',
  );

  String get permissionsAllSet => tr(
    en: 'all set',
    ru: 'всё хорошо',
    tr: 'hazır',
    ptBr: 'tudo certo',
    zhHans: '已就绪',
    zhHant: '已就緒',
  );

  String get versionTapToUpdate => tr(
    en: 'tap to update',
    ru: 'нажмите для обновления',
    tr: 'güncellemek için dokunun',
    ptBr: 'toque para atualizar',
    zhHans: '点按更新',
    zhHant: '點按更新',
  );

  String get versionLatestVersion => tr(
    en: 'latest version',
    ru: 'последняя версия',
    tr: 'son sürüm',
    ptBr: 'versão mais recente',
    zhHans: '最新版本',
    zhHant: '最新版本',
  );

  String get recentConversions => tr(
    en: 'Recent conversions',
    ru: 'Последние конвертации',
    tr: 'Son dönüştürmeler',
    ptBr: 'Conversões recentes',
    zhHans: '最近转换',
    zhHant: '最近轉換',
  );

  String get noConversionsYet => tr(
    en: 'no conversions yet',
    ru: 'конвертаций пока нет',
    tr: 'henüz dönüştürme yok',
    ptBr: 'nenhuma conversão ainda',
    zhHans: '暂无转换',
    zhHant: '暫無轉換',
  );

  String get conversionLogDisabled => tr(
    en: 'conversion log is disabled',
    ru: 'лог конвертаций выключен',
    tr: 'dönüştürme günlüğü kapalı',
    ptBr: 'o log de conversões está desativado',
    zhHans: '转换日志已关闭',
    zhHant: '轉換記錄已關閉',
  );

  String get enable => tr(
    en: 'enable',
    ru: 'включить',
    tr: 'etkinleştir',
    ptBr: 'ativar',
    zhHans: '启用',
    zhHant: '啟用',
  );

  String get payloadCopied => tr(
    en: 'Payload copied',
    ru: 'Payload скопирован',
    tr: 'Payload kopyalandı',
    ptBr: 'Payload copiado',
    zhHans: 'Payload 已复制',
    zhHant: 'Payload 已複製',
  );

  String get progressTitle => tr(
    en: 'Progress',
    ru: 'Прогресс',
    tr: 'İlerleme',
    ptBr: 'Barras de Progresso (Downloads/Mídia)',
    zhHans: '进度',
    zhHant: '進度',
  );

  String get nativeProgressTitle => tr(
    en: 'Native progress',
    ru: 'Нативный прогресс',
    tr: 'Yerel ilerleme',
    ptBr: 'Barras de progresso do sistema',
    zhHans: '原生进度',
    zhHant: '原生進度',
  );

  String get otpCodesTitle => tr(
    en: 'OTP codes',
    ru: 'OTP-коды',
    tr: 'OTP kodları',
    ptBr: 'Códigos OTP',
    zhHans: 'OTP 验证码',
    zhHant: 'OTP 驗證碼',
  );

  String get autoCopyCodeTitle => tr(
    en: 'Auto-copy code',
    ru: 'Автокопирование кода',
    tr: 'Kodu otomatik kopyala',
    ptBr: 'Copiar código automaticamente',
    zhHans: '自动复制验证码',
    zhHant: '自動複製驗證碼',
  );

  String get smartConversionTitle => tr(
    en: 'Smart conversion',
    ru: 'Умная конвертация',
    tr: 'Akıllı dönüştürme',
    ptBr: 'Conversão inteligente',
    zhHans: '智能转换',
    zhHant: '智慧轉換',
  );

  String get taxiTitle => tr(
    en: 'Taxi',
    ru: 'Такси',
    tr: 'Taksi',
    ptBr: 'Táxi',
    zhHans: '打车',
    zhHant: '叫車',
  );

  String get deliveriesTitle => tr(
    en: 'Deliveries',
    ru: 'Доставки',
    tr: 'Teslimatlar',
    ptBr: 'Entregas',
    zhHans: '外卖',
    zhHant: '外送',
  );

  String get removeOriginalMessageTitle => tr(
    en: 'Remove original message',
    ru: 'Удалять исходное уведомление',
    tr: 'Orijinal bildirimi kaldır',
    ptBr: 'Remover notificação original (Evita notificações duplicadas)',
    zhHans: '移除原始通知',
    zhHant: '移除原始通知',
  );

  String get experimentalSuffix => tr(
    en: '(exp)',
    ru: '(exp)',
    tr: '(deneysel)',
    ptBr: '(exp)',
    zhHans: '（实验）',
    zhHant: '（實驗）',
  );

  String get allAppsTitle => tr(
    en: 'All apps',
    ru: 'Все приложения',
    tr: 'Tüm uygulamalar',
    ptBr: 'Todos os apps',
    zhHans: '所有应用',
    zhHant: '所有應用程式',
  );

  String get onlySelectedTitle => tr(
    en: 'Only selected',
    ru: 'Только выбранные',
    tr: 'Yalnızca seçilenler',
    ptBr: 'Somente selecionados',
    zhHans: '仅已选择',
    zhHant: '僅已選取',
  );

  String get excludeSelectedTitle => tr(
    en: 'Exclude selected',
    ru: 'Исключить выбранные',
    tr: 'Seçilenleri hariç tut',
    ptBr: 'Excluir selecionados',
    zhHans: '排除已选择',
    zhHant: '排除已選取',
  );

  String get conversionModeTitle => tr(
    en: 'Conversion mode',
    ru: 'Режим конвертации',
    tr: 'Dönüştürme modu',
    ptBr: 'Modo de conversão',
    zhHans: '转换模式',
    zhHant: '轉換模式',
  );

  String get selectedAppsTitle => tr(
    en: 'Selected apps',
    ru: 'Приложения',
    tr: 'Seçili uygulamalar',
    ptBr: 'Apps selecionados',
    zhHans: '已选择应用',
    zhHant: '已選取應用程式',
  );

  String get showSystem => tr(
    en: 'show system',
    ru: 'показать системные',
    tr: 'sistem uygulamalarını göster',
    ptBr: 'mostrar aplicativos do sistema',
    zhHans: '显示系统',
    zhHant: '顯示系統',
  );

  String get hideSystem => tr(
    en: 'hide system',
    ru: 'скрыть системные',
    tr: 'sistem uygulamalarını gizle',
    ptBr: 'ocultar aplicativos do sistema',
    zhHans: '隐藏系统',
    zhHant: '隱藏系統',
  );

  String get networkConnectionsTitle => tr(
    en: 'Network & Connections',
    ru: 'Сеть и подключения',
    tr: 'Ağ ve Bağlantılar',
    ptBr: 'Rede e conexões',
    zhHans: '网络与连接',
    zhHant: '網路與連線',
  );

  String get vpnsTitle => tr(
    en: 'VPNs',
    ru: 'VPN',
    tr: 'VPN\'ler',
    ptBr: 'VPNs',
    zhHans: 'VPN',
    zhHant: 'VPN',
  );

  String get externalDevicesTitle => tr(
    en: 'External devices',
    ru: 'Внешние устройства',
    tr: 'Harici cihazlar',
    ptBr: 'Dispositivos externos',
    zhHans: '外接设备',
    zhHant: '外接裝置',
  );

  String get ignoreDebuggingDevicesTitle => tr(
    en: 'Ignore debugging devices',
    ru: 'Игнорировать отладочные устройства',
    tr: 'Hata ayıklama cihazlarını yok say',
    ptBr: 'Ignorar dispositivos de depuração',
    zhHans: '忽略调试设备',
    zhHant: '忽略偵錯裝置',
  );

  String get networkSpeedThresholdRedesignTitle => tr(
    en: 'Network speed threshold',
    ru: 'Порог скорости сети',
    tr: 'Ağ hızı eşiği',
    ptBr: 'Limite de velocidade de rede',
    zhHans: '网速阈值',
    zhHant: '網速門檻',
  );

  String get miscellaneousTitle => tr(
    en: 'Miscellaneous',
    ru: 'Разное',
    tr: 'Diğer',
    ptBr: 'Diversos',
    zhHans: '其他',
    zhHant: '其他',
  );

  String get navigationMapsTitle => tr(
    en: 'Navigation (maps)',
    ru: 'Навигация (карты)',
    tr: 'Navigasyon (haritalar)',
    ptBr: 'Navegação (mapas)',
    zhHans: '导航（地图）',
    zhHant: '導航（地圖）',
  );

  String get mediaPlaybackRedesignTitle => tr(
    en: 'Media playback',
    ru: 'Медиа',
    tr: 'Medya oynatma',
    ptBr: 'Reprodução de mídia',
    zhHans: '媒体播放',
    zhHant: '媒體播放',
  );

  String get callsTitle => tr(
    en: 'Calls',
    ru: 'Звонки',
    tr: 'Aramalar',
    ptBr: 'Chamadas',
    zhHans: '通话',
    zhHant: '通話',
  );

  String get showMediaOnLockTitle => tr(
    en: 'Show media on lockscreen',
    ru: 'Медиа на экране блокировки',
    tr: 'Kilit ekranında medyayı göster',
    ptBr: 'Mostrar apenas na tela de bloqueio',
    zhHans: '在锁屏显示媒体',
    zhHant: '在鎖定畫面顯示媒體',
  );

  String get useSymbolsInMediaPlayerTitle => tr(
    en: 'Use symbols in media player',
    ru: 'Символы в медиаплеере',
    tr: 'Medya oynatıcıda semboller kullan',
    ptBr: 'Usar símbolos no reprodutor de mídia',
    zhHans: '在媒体播放器中使用符号',
    zhHant: '在媒體播放器中使用符號',
  );

  String get weatherBroadcastsTitle => tr(
    en: 'Weather broadcasts',
    ru: 'Прогнозы погоды',
    tr: 'Hava durumu bildirimleri',
    ptBr: 'Alertas de clima',
    zhHans: '天气播报',
    zhHant: '天氣播報',
  );

  String get bypassTitle => tr(
    en: 'Bypass',
    ru: 'Bypass',
    tr: 'Bypass',
    ptBr: 'Forçar notificações ao vivo',
    zhHans: '绕过',
    zhHant: '繞過',
  );

  String get perAppSettingsTitle => tr(
    en: 'Per-app settings',
    ru: 'Настройки приложений',
    tr: 'Uygulama bazlı ayarlar',
    ptBr: 'Configurações por app',
    zhHans: '按应用设置',
    zhHant: '各應用設定',
  );

  String get defaultsTitle => tr(
    en: 'Defaults',
    ru: 'По умолчанию',
    tr: 'Varsayılanlar',
    ptBr: 'Padrões',
    zhHans: '默认值',
    zhHant: '預設值',
  );

  String get defaultsSubtitle => tr(
    en: 'tap to change default behavior',
    ru: 'нажмите, чтобы изменить поведение',
    tr: 'varsayılan davranışı değiştirmek için dokunun',
    ptBr: 'toque para alterar o comportamento padrão',
    zhHans: '点按更改默认行为',
    zhHant: '點按變更預設行為',
  );

  String get appsListTitle => tr(
    en: 'Apps list',
    ru: 'Список приложений',
    tr: 'Uygulama listesi',
    ptBr: 'Lista de apps',
    zhHans: '应用列表',
    zhHant: '應用程式清單',
  );

  String get exportLabel => tr(
    en: 'Export',
    ru: 'Экспорт',
    tr: 'Dışa aktar',
    ptBr: 'Exportar',
    zhHans: '导出',
    zhHant: '匯出',
  );

  String get importLabel => tr(
    en: 'Import',
    ru: 'Импорт',
    tr: 'İçe aktar',
    ptBr: 'Importar',
    zhHans: '导入',
    zhHant: '匯入',
  );

  String get titleSourceTitle => tr(
    en: 'Title source',
    ru: 'Источник заголовка',
    tr: 'Başlık kaynağı',
    ptBr: 'Origem do título',
    zhHans: '标题来源',
    zhHant: '標題來源',
  );

  String get contentSourceTitle => tr(
    en: 'Content source',
    ru: 'Источник контента',
    tr: 'İçerik kaynağı',
    ptBr: 'Origem do conteúdo',
    zhHans: '内容来源',
    zhHant: '內容來源',
  );

  String get notificationTitleOption => tr(
    en: 'Notification title',
    ru: 'Заголовок уведомления',
    tr: 'Bildirim başlığı',
    ptBr: 'Título da notificação',
    zhHans: '通知标题',
    zhHant: '通知標題',
  );

  String get appTitleOption => tr(
    en: 'App title',
    ru: 'Название приложения',
    tr: 'Uygulama başlığı',
    ptBr: 'Título do app',
    zhHans: '应用标题',
    zhHant: '應用標題',
  );

  String get notificationTextOption => tr(
    en: 'Notification text',
    ru: 'Текст уведомления',
    tr: 'Bildirim metni',
    ptBr: 'Texto da notificação',
    zhHans: '通知文本',
    zhHant: '通知文字',
  );

  String get appUpdateNewVersionTitle => tr(
    en: 'New version available',
    ru: 'Доступна новая версия',
    tr: 'Yeni sürüm mevcut',
    ptBr: 'Nova versão disponível',
    zhHans: '有新版本可用',
    zhHant: '有新版本可用',
  );

  String get appUpdateCheckingTitle => tr(
    en: 'Checking for updates',
    ru: 'Проверяем обновления',
    tr: 'Güncellemeler denetleniyor',
    ptBr: 'Verificando atualizações',
    zhHans: '正在检查更新',
    zhHant: '正在檢查更新',
  );

  String get appUpdateAllSetTitle => tr(
    en: 'You’re all set',
    ru: 'Всё хорошо',
    tr: 'Her şey hazır',
    ptBr: 'Tudo pronto',
    zhHans: '已是最新',
    zhHant: '已是最新',
  );

  String get appUpdateDownloadsSubtitle => tr(
    en: 'tap to go to downloads',
    ru: 'перейти к загрузке',
    tr: 'indirmelere gitmek için dokunun',
    ptBr: 'toque para ir aos downloads',
    zhHans: '点按前往下载',
    zhHant: '點按前往下載',
  );

  String get appUpdatePleaseWaitSubtitle => tr(
    en: 'please wait a moment',
    ru: 'подождите немного',
    tr: 'lütfen biraz bekleyin',
    ptBr: 'aguarde um momento',
    zhHans: '请稍等',
    zhHant: '請稍候',
  );

  String get appUpdateLatestSubtitle => tr(
    en: 'latest version already',
    ru: 'установлена последняя версия',
    tr: 'zaten son sürüm',
    ptBr: 'já está na versão mais recente',
    zhHans: '已经是最新版本',
    zhHant: '已是最新版本',
  );

  String get appUpdateLogTitle => tr(
    en: 'What\'s new',
    ru: 'Что нового',
    tr: 'Güncelleme günlüğü',
    ptBr: 'Registro de atualização',
    zhHans: '更新日志',
    zhHant: '更新紀錄',
  );

  String get appUpdateLogLoading => tr(
    en: 'loading update log...',
    ru: 'загружаем список изменений...',
    tr: 'güncelleme günlüğü yükleniyor...',
    ptBr: 'carregando registro de atualização...',
    zhHans: '正在加载更新日志...',
    zhHant: '正在載入更新紀錄...',
  );

  String get appUpdateLogUnavailable => tr(
    en: 'update log is not available',
    ru: 'список изменений недоступен',
    tr: 'güncelleme günlüğü mevcut değil',
    ptBr: 'registro de atualização indisponível',
    zhHans: '更新日志不可用',
    zhHant: '更新紀錄無法使用',
  );

  String get visitProjectPageTitle => tr(
    en: 'Visit project page',
    ru: 'Открыть страницу проекта',
    tr: 'Proje sayfasını aç',
    ptBr: 'Abrir página do projeto',
    zhHans: '访问项目页面',
    zhHant: '前往專案頁面',
  );

  String get visitGithubTitle => tr(
    en: 'Visit GitHub',
    ru: 'Открыть GitHub',
    tr: 'GitHub\'ı aç',
    ptBr: 'Abrir GitHub',
    zhHans: '访问 GitHub',
    zhHant: '前往 GitHub',
  );

  String get updateProfileNewVersionTitle => tr(
    en: 'New version available',
    ru: 'Доступна новая версия',
    tr: 'Yeni sürüm mevcut',
    ptBr: 'Nova versão disponível',
    zhHans: '有新版本可用',
    zhHant: '有新版本可用',
  );

  String updateProfileVersionSubtitle(String current, String latest) => tr(
    en: '$current -> $latest | tap to see',
    ru: '$current -> $latest | посмотреть',
    tr: '$current -> $latest | görmek için dokunun',
    ptBr: '$current -> $latest | toque para ver',
    zhHans: '$current -> $latest | 点按查看',
    zhHant: '$current -> $latest | 點按查看',
  );

  String get updateProfileAvailableSubtitle => tr(
    en: 'update available | tap to see',
    ru: 'доступно обновление | посмотреть',
    tr: 'güncelleme mevcut | görmek için dokunun',
    ptBr: 'atualização disponível | toque para ver',
    zhHans: '有可用更新 | 点按查看',
    zhHant: '有可用更新 | 點按查看',
  );

  String get updateProfileOpenSubtitle => tr(
    en: 'tap to open update settings',
    ru: 'нажмите для настройки',
    tr: 'güncelleme ayarlarını açmak için dokunun',
    ptBr: 'toque para abrir ajustes de atualização',
    zhHans: '点按打开更新设置',
    zhHant: '點按開啟更新設定',
  );

  String get conversionLogTitle => tr(
    en: 'Conversion log',
    ru: 'Лог конвертаций',
    tr: 'Dönüştürme günlüğü',
    ptBr: 'Log de conversões',
    zhHans: '转换日志',
    zhHant: '轉換記錄',
  );

  String get logLengthTitle => tr(
    en: 'Log length',
    ru: 'Размер лога',
    tr: 'Günlük boyutu',
    ptBr: 'Tamanho do log',
    zhHans: '日志大小',
    zhHant: '記錄大小',
  );

  String get xiaomiHyperIslandTitle => tr(
    en: 'Xiaomi HyperIsland',
    ru: 'Xiaomi HyperIsland',
    tr: 'Xiaomi HyperIsland',
    ptBr: 'Xiaomi HyperIsland',
    zhHans: '小米 HyperIsland',
    zhHant: '小米 HyperIsland',
  );

  String get lengthTitle => tr(
    en: 'Length',
    ru: 'Длина',
    tr: 'Uzunluk',
    ptBr: 'Tamanho',
    zhHans: '长度',
    zhHant: '長度',
  );

  String get otpDedupTitle => tr(
    en: 'OTP dedup',
    ru: 'OTP dedup',
    tr: 'OTP tekilleştirme',
    ptBr: 'Desduplicação de OTP',
    zhHans: 'OTP 去重',
    zhHant: 'OTP 去重',
  );

  String get smartConversionDedupTitle => tr(
    en: 'Smart conversion dedup',
    ru: 'Smart conversion dedup',
    tr: 'Akıllı dönüştürme tekilleştirme',
    ptBr: 'Desduplicação da conversão inteligente',
    zhHans: '智能转换去重',
    zhHant: '智慧轉換去重',
  );

  String get animatedIslandRedesignTitle => tr(
    en: 'Animated Island',
    ru: 'Анимированный остров',
    tr: 'Animasyonlu ada',
    ptBr: 'Ilha animada',
    zhHans: '动态岛动画',
    zhHant: '動態島動畫',
  );

  String get updateFrequencyTitle => tr(
    en: 'Update frequency',
    ru: 'Частота обновления',
    tr: 'Güncelleme sıklığı',
    ptBr: 'Frequência de atualização',
    zhHans: '更新频率',
    zhHant: '更新頻率',
  );

  String get copyDebugJsonTitle => tr(
    en: 'Copy debug JSON',
    ru: 'Скопировать debug JSON',
    tr: 'Debug JSON\'unu kopyala',
    ptBr: 'Copiar JSON de debug',
    zhHans: '复制调试 JSON',
    zhHant: '複製偵錯 JSON',
  );

  String get copyDebugJsonDescription => tr(
    en: 'copies device, permission, settings, and rules state for bug reports',
    ru: 'копирует состояние устройства, разрешений, настроек и правил для issue',
    tr: 'hata raporları için cihaz, izin, ayar ve kural durumunu kopyalar',
    ptBr:
        'copia o estado do dispositivo, permissões, configurações e regras para reportar bugs',
    zhHans: '复制用于问题报告的设备、权限、设置和规则状态',
    zhHant: '複製用於問題回報的裝置、權限、設定與規則狀態',
  );

  String get openGithubPageTitle => tr(
    en: 'Open GitHub page',
    ru: 'Открыть GitHub',
    tr: 'GitHub sayfasını aç',
    ptBr: 'Abrir página do GitHub',
    zhHans: '打开 GitHub 页面',
    zhHant: '開啟 GitHub 頁面',
  );

  String get openGithubPageDescription => tr(
    en: 'opens the GitHub issue page for reporting bugs',
    ru: 'открывает страницу GitHub Issues для багрепорта',
    tr: 'hata bildirmek için GitHub Issues sayfasını açar',
    ptBr: 'abre a página de Issues do GitHub para reportar bugs',
    zhHans: '打开用于报告问题的 GitHub Issues 页面',
    zhHant: '開啟用於回報問題的 GitHub Issues 頁面',
  );

  String get autoCopyDebugJsonTitle => tr(
    en: 'Auto-copy debug JSON',
    ru: 'Автокопирование debug JSON',
    tr: 'Debug JSON\'unu otomatik kopyala',
    ptBr: 'Copiar JSON de debug automaticamente',
    zhHans: '自动复制调试 JSON',
    zhHant: '自動複製偵錯 JSON',
  );

  String get autoCopyDebugJsonDescription => tr(
    en: 'copies diagnostics automatically before opening GitHub',
    ru: 'автоматически копирует диагностику перед открытием GitHub',
    tr: 'GitHub açılmadan önce tanılama verilerini otomatik kopyalar',
    ptBr: 'copia os diagnósticos automaticamente antes de abrir o GitHub',
    zhHans: '打开 GitHub 前自动复制诊断信息',
    zhHant: '開啟 GitHub 前自動複製診斷資訊',
  );

  String conversionLogFrom(String appLabel) => tr(
    en: 'from $appLabel',
    ru: 'от $appLabel',
    tr: '$appLabel uygulamasından',
    ptBr: 'de $appLabel',
    zhHans: '来自 $appLabel',
    zhHant: '來自 $appLabel',
  );

  String conversionLogAt(String time) => tr(
    en: 'at $time',
    ru: 'в $time',
    tr: time,
    ptBr: 'às $time',
    zhHans: time,
    zhHant: time,
  );

  String get conversionLogEntryTitleLabel => tr(
    en: 'Title',
    ru: 'Заголовок',
    tr: 'Başlık',
    ptBr: 'Título',
    zhHans: '标题',
    zhHant: '標題',
  );

  String get payloadJsonTitle => tr(
    en: 'Payload JSON',
    ru: 'Payload JSON',
    tr: 'Payload JSON',
    ptBr: 'Payload JSON',
    zhHans: 'Payload JSON',
    zhHant: 'Payload JSON',
  );

  String get loadingApps => tr(
    en: 'loading apps...',
    ru: 'загрузка приложений...',
    tr: 'uygulamalar yükleniyor...',
    ptBr: 'carregando apps...',
    zhHans: '正在加载应用...',
    zhHant: '正在載入應用程式...',
  );

  String get searchForApps => tr(
    en: 'Search for apps...',
    ru: 'Поиск приложений...',
    tr: 'Uygulama ara...',
    ptBr: 'Buscar apps...',
    zhHans: '搜索应用...',
    zhHant: '搜尋應用程式...',
  );

  String get heroTitle => 'LiveBridge';

  String get reportBug => tr(
    en: 'Report a bug',
    ru: 'Сообщить о баге',
    tr: 'Hata bildir',
    ptBr: 'Reportar um bug',
    zhHans: '报告问题',
    zhHant: '回報問題',
  );

  String get supportLiveBridgeTitle => tr(
    en: 'Support LiveBridge',
    ru: 'Поддержать LiveBridge',
    tr: 'LiveBridge’i destekle',
    ptBr: 'Apoiar o LiveBridge',
    zhHans: '支持 LiveBridge',
    zhHant: '支持 LiveBridge',
  );

  String get supportIntroTitle => tr(
    en: 'Keep LiveBridge free',
    ru: 'LiveBridge остается бесплатным',
    tr: 'LiveBridge ücretsiz kalsın',
    ptBr: 'Mantenha o LiveBridge gratuito',
    zhHans: '让 LiveBridge 保持免费',
    zhHant: '讓 LiveBridge 保持免費',
  );

  String get supportIntroBody => tr(
    en: 'Donations are optional and never unlock features. They help cover testing devices and development time.',
    ru: 'Донаты добровольны и не открывают функций. Они помогают покрывать тестовые устройства и время разработки.',
    tr: 'Bağışlar isteğe bağlıdır ve özellik açmaz. Test cihazlarını ve geliştirme süresini destekler.',
    ptBr:
        'Doações são opcionais e não desbloqueiam recursos. Elas ajudam com aparelhos de teste e tempo de desenvolvimento.',
    zhHans: '捐赠是自愿的，不会解锁功能。它们用于测试设备和开发时间。',
    zhHant: '捐贈是自願的，不會解鎖功能。它們用於測試裝置和開發時間。',
  );

  String get supportBoostyTitle => tr(
    en: 'Boosty',
    ru: 'Boosty',
    tr: 'Boosty',
    ptBr: 'Boosty',
    zhHans: 'Boosty',
    zhHant: 'Boosty',
  );

  String get supportBoostySubtitle => tr(
    en: 'cards and recurring support',
    ru: 'карты и регулярная поддержка',
    tr: 'kartlar ve düzenli destek',
    ptBr: 'cartões e apoio recorrente',
    zhHans: '银行卡和定期支持',
    zhHant: '銀行卡和定期支持',
  );

  String get supportCryptoTitle => tr(
    en: 'Crypto',
    ru: 'Криптовалюта',
    tr: 'Kripto',
    ptBr: 'Cripto',
    zhHans: '加密货币',
    zhHant: '加密貨幣',
  );

  String get supportCryptoSubtitle => tr(
    en: 'copy wallet details',
    ru: 'скопировать реквизиты кошелька',
    tr: 'cüzdan bilgilerini kopyala',
    ptBr: 'copiar dados da carteira',
    zhHans: '复制钱包信息',
    zhHant: '複製錢包資訊',
  );

  String get supportDiscussTitle => tr(
    en: 'Discuss',
    ru: 'Discuss',
    tr: 'Tartış',
    ptBr: 'Discutir',
    zhHans: '讨论',
    zhHant: '討論',
  );

  String get supportDiscussSubtitle => tr(
    en: 'telegram topics',
    ru: 'telegram topics',
    tr: 'telegram konuları',
    ptBr: 'tópicos no telegram',
    zhHans: 'telegram 话题',
    zhHant: 'telegram 話題',
  );

  String get supportGithubTitle => tr(
    en: 'Star on GitHub',
    ru: 'Поставить звезду на GitHub',
    tr: 'GitHub',
    ptBr: 'GitHub',
    zhHans: 'GitHub',
    zhHant: 'GitHub',
  );

  String get supportGithubSubtitle => tr(
    en: 'source code and releases',
    ru: 'исходный код и релизы',
    tr: 'kaynak kod ve sürümler',
    ptBr: 'código-fonte e versões',
    zhHans: '源代码和版本发布',
    zhHant: '原始碼和版本發布',
  );

  String get supportMethodNotConfigured => tr(
    en: 'Support method is not configured yet.',
    ru: 'Способ поддержки еще не настроен.',
    tr: 'Destek yöntemi henüz yapılandırılmadı.',
    ptBr: 'O método de apoio ainda não foi configurado.',
    zhHans: '支持方式尚未配置。',
    zhHant: '支持方式尚未設定。',
  );

  String get supportCryptoCopied => tr(
    en: 'Crypto details copied',
    ru: 'Криптореквизиты скопированы',
    tr: 'Kripto bilgileri kopyalandı',
    ptBr: 'Dados de cripto copiados',
    zhHans: '加密货币信息已复制',
    zhHant: '加密貨幣資訊已複製',
  );

  String get bugReportCopied => tr(
    en: 'Diagnostics copied to clipboard. Paste it into the issue.',
    ru: 'Диагностика скопирована в буфер. Вставьте в issue.',
    tr: 'Tanılama panoya kopyalandı. Issue içine yapıştırın.',
    ptBr: 'Diagnóstico copiado para a área de transferência. Cole no issue.',
    zhHans: '诊断信息已复制到剪贴板，请粘贴到 issue 中。',
    zhHant: '診斷資訊已複製到剪貼簿，請貼到 issue。',
  );

  String get bugReportCopyFailed => tr(
    en: 'Failed to copy diagnostics.',
    ru: 'Не удалось скопировать диагностику.',
    tr: 'Tanılama kopyalanamadı.',
    ptBr: 'Falha ao copiar diagnóstico.',
    zhHans: '复制诊断信息失败。',
    zhHant: '複製診斷資訊失敗。',
  );

  String get accessTitle => tr(
    en: 'Permissions',
    ru: 'Разрешения',
    tr: 'İzinler',
    ptBr: 'Permissões',
    zhHans: '权限',
    zhHant: '權限',
  );

  String get listenerAccess => tr(
    en: 'Notification Listener access',
    ru: 'Доступ к уведомлениям',
    tr: 'Bildirim dinleyicisi erişimi',
    ptBr: 'Permitir acesso do app as notificações',
    zhHans: '通知监听访问',
    zhHant: '通知監聽存取',
  );

  String get postNotifications => tr(
    en: 'Post notifications permission',
    ru: 'Отправка уведомлений',
    tr: 'Bildirim gönderme izni',
    ptBr: 'Permita que o app envie notificações',
    zhHans: '发送通知权限',
    zhHant: '發送通知權限',
  );

  String get liveUpdatesAccess => tr(
    en: 'Live Updates promotion',
    ru: 'Разрешение на Live Updates',
    tr: 'Live Updates tanıtımı',
    ptBr: 'Permitir atualizações ao vivo',
    zhHans: 'Live Updates 推送权限',
    zhHant: 'Live Updates 推送權限',
  );

  String get settingsTitle => tr(
    en: 'Settings',
    ru: 'Настройки',
    tr: 'Ayarlar',
    ptBr: 'Configurações',
    zhHans: '设置',
    zhHant: '設定',
  );

  String get keepAliveForegroundTitle => tr(
    en: 'Alt background mode',
    ru: 'Альтернативный фоновый режим',
    tr: 'Alternatif arka plan modu',
    ptBr: 'Modo de segundo plano alternativo',
    zhHans: '备用后台模式',
    zhHant: '備用背景模式',
  );

  String get networkSpeedTitle => tr(
    en: 'Network speed',
    ru: 'Скорость сети',
    tr: 'Ağ hızı',
    ptBr: 'Velocidade da rede',
    zhHans: '网速',
    zhHant: '網速',
  );

  String get networkSpeedThresholdAlways => tr(
    en: 'Always show',
    ru: 'Показывать всегда',
    tr: 'Her zaman göster',
    ptBr: 'Sempre mostrar',
    zhHans: '始终显示',
    zhHant: '永遠顯示',
  );

  String get syncDndTitle => tr(
    en: 'Sync DnD',
    ru: 'Синхронизировать DnD',
    tr: 'DnD eşitle',
    ptBr: 'Sincronizar Não Perturbe',
    zhHans: '同步勿扰',
    zhHant: '同步勿擾',
  );

  String get preventDismissingTitle => tr(
    en: 'Prevent dismissing',
    ru: 'Запретить скрытие',
    tr: 'Bildirimi kapatmayı engelle',
    ptBr: 'Impedir fechamento',
    zhHans: '防止通知被关闭',
    zhHant: '防止通知被關閉',
  );

  String get hideLockscreenContentTitle => tr(
    en: 'Hide lockscreen content',
    ru: 'Скрывать на локскрине',
    tr: 'Kilit ekranında içeriği gizle',
    ptBr: 'Ocultar conteúdo quando a tela estiver bloqueada',
    zhHans: '隐藏锁屏内容',
    zhHant: '隱藏鎖定畫面內容',
  );

  String get disableHintsTitle => tr(
    en: 'Disable hints',
    ru: 'Отключить подсказки',
    tr: 'İpuçlarını kapat',
    ptBr: 'Desativar dicas',
    zhHans: '关闭提示',
    zhHant: '關閉提示',
  );

  String get updateChecksTitle => tr(
    en: 'Update checking',
    ru: 'Проверка обновлений',
    tr: 'Güncellemeleri denetle',
    ptBr: 'Verificação de atualizações',
    zhHans: '检查更新',
    zhHant: '檢查更新',
  );

  String get updateChecksDescription => tr(
    en: 'checks GitHub releases and shows an update card when a new version is available',
    ru: 'проверяет релизы GitHub и показывает карточку, когда доступна новая версия',
    tr: 'GitHub sürümlerini denetler ve yeni sürüm varsa güncelleme kartı gösterir',
    ptBr:
        'verifica lançamentos no GitHub e mostra um cartão quando há nova versão disponível',
    zhHans: '检查 GitHub 发布，并在有新版本时显示更新卡片',
    zhHant: '檢查 GitHub 發布版本，並在有新版本時顯示更新卡片',
  );

  String get experimentalTitle => tr(
    en: 'Experimental',
    ru: 'Экспериментальное',
    tr: 'Deneysel',
    ptBr: 'Experimental',
    zhHans: '实验功能',
    zhHant: '實驗功能',
  );

  String get aospCuttingTitle => tr(
    en: 'AOSP cutting',
    ru: 'Обрезка AOSP',
    tr: 'AOSP kırpma',
    ptBr: 'Recorte AOSP',
    zhHans: 'AOSP 裁剪',
    zhHant: 'AOSP 裁切',
  );

  String get appPresentationSettings => tr(
    en: 'Per-app behavior',
    ru: 'Поведение приложений',
    tr: 'Uygulama bazlı davranış',
    ptBr: 'Comportamento por app',
    zhHans: '按应用行为',
    zhHant: '各應用行為',
  );

  String get appPresentationLoadFailed => tr(
    en: 'Unable to load per-app settings.',
    ru: 'Не удалось загрузить настройки приложений.',
    tr: 'Uygulama bazlı ayarlar yüklenemiyor.',
    ptBr: 'Não foi possível carregar configurações por app.',
    zhHans: '无法加载按应用设置。',
    zhHant: '無法載入各應用設定。',
  );

  String get appPresentationSaveFailed => tr(
    en: 'Unable to save per-app settings.',
    ru: 'Не удалось сохранить настройки приложений.',
    tr: 'Uygulama bazlı ayarlar kaydedilemiyor.',
    ptBr: 'Não foi possível salvar configurações por app.',
    zhHans: '无法保存按应用设置。',
    zhHant: '無法儲存各應用設定。',
  );

  String get appPresentationDownloadFailed => tr(
    en: 'Failed to save settings JSON.',
    ru: 'Не удалось сохранить JSON настроек.',
    tr: 'Ayarlar JSON\'u kaydedilemedi.',
    ptBr: 'Falha ao salvar JSON de configurações.',
    zhHans: '保存设置 JSON 失败。',
    zhHant: '儲存設定 JSON 失敗。',
  );

  String get appPresentationSaved => tr(
    en: 'Settings saved to Downloads.',
    ru: 'Настройки сохранены в Загрузки.',
    tr: 'Ayarlar İndirilenler klasörüne kaydedildi.',
    ptBr: 'Configurações salvas em Downloads.',
    zhHans: '设置已保存到下载目录。',
    zhHant: '設定已儲存到下載資料夾。',
  );

  String get appPresentationUploadDone => tr(
    en: 'Per-app settings imported.',
    ru: 'Настройки приложений загружены.',
    tr: 'Uygulama bazlı ayarlar içe aktarıldı.',
    ptBr: 'Configurações por app importadas.',
    zhHans: '已导入按应用设置。',
    zhHant: '已匯入各應用設定。',
  );

  String get appPresentationUploadFailed => tr(
    en: 'Failed to import settings JSON.',
    ru: 'Не удалось загрузить JSON настроек.',
    tr: 'Ayarlar JSON\'u içe aktarılamadı.',
    ptBr: 'Falha ao importar JSON de configurações.',
    zhHans: '导入设置 JSON 失败。',
    zhHant: '匯入設定 JSON 失敗。',
  );

  String get appPresentationInvalidJson => tr(
    en: 'Invalid per-app settings JSON.',
    ru: 'Невалидный JSON настроек приложений.',
    tr: 'Geçersiz uygulama bazlı ayarlar JSON\'u.',
    ptBr: 'JSON de configurações por app inválido.',
    zhHans: '按应用设置 JSON 无效。',
    zhHant: '各應用設定 JSON 無效。',
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
    ru: 'Скачать настройки',
    tr: 'Ayarları indir',
    ptBr: 'Baixar configurações',
    zhHans: '下载设置',
    zhHant: '下載設定',
  );

  String get uploadSettings => tr(
    en: 'Upload settings',
    ru: 'Загрузить настройки',
    tr: 'Ayarları yükle',
    ptBr: 'Enviar configurações',
    zhHans: '上传设置',
    zhHant: '上傳設定',
  );

  String get save => tr(
    en: 'Save',
    ru: 'Сохранить',
    tr: 'Kaydet',
    ptBr: 'Salvar',
    zhHans: '保存',
    zhHant: '儲存',
  );

  String get appsLoadFailed => tr(
    en: 'Unable to load installed apps list.',
    ru: 'Не удалось загрузить список приложений.',
    tr: 'Yüklü uygulama listesi yüklenemiyor.',
    ptBr: 'Não foi possível carregar a lista de apps instalados.',
    zhHans: '无法加载已安装应用列表。',
    zhHant: '無法載入已安裝應用清單。',
  );

  String get appsAccessTitle => tr(
    en: 'App list access',
    ru: 'Доступ к списку приложений',
    tr: 'Uygulama listesi erişimi',
    ptBr: 'Acesso à lista de apps',
    zhHans: '应用列表访问',
    zhHant: '應用清單存取',
  );

  String get appsAccessMessage => tr(
    en: 'Allow LiveBridge to read installed apps so you can pick apps for rules?',
    ru: 'Разрешить LiveBridge читать список установленных приложений для выбора правил?',
    tr: 'Kurallar için uygulama seçebilmeniz adına LiveBridge yüklü uygulamaları okuyabilsin mi?',
    ptBr:
        'Permitir que o LiveBridge leia os apps instalados para que você possa escolher apps para as regras?',
    zhHans: '允许 LiveBridge 读取已安装应用列表，以便为规则选择应用吗？',
    zhHant: '允許 LiveBridge 讀取已安裝應用清單，以便為規則選擇應用程式嗎？',
  );

  String get appsAccessSaveFailed => tr(
    en: 'Unable to save access preference.',
    ru: 'Не удалось сохранить выбор доступа.',
    tr: 'Erişim tercihi kaydedilemiyor.',
    ptBr: 'Não foi possível salvar a preferência de acesso.',
    zhHans: '无法保存访问偏好。',
    zhHant: '無法儲存存取偏好。',
  );

  String get cancel => tr(
    en: 'Cancel',
    ru: 'Отмена',
    tr: 'İptal',
    ptBr: 'Cancelar',
    zhHans: '取消',
    zhHant: '取消',
  );

  String get allow => tr(
    en: 'Allow',
    ru: 'Разрешить',
    tr: 'İzin ver',
    ptBr: 'Permitir',
    zhHans: '允许',
    zhHant: '允許',
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
    en: 'Dismisses original clearable notifications after LiveBridge mirrors an OTP or status update.',
    ru: 'Убирает исходные смахиваемые уведомления, если LiveBridge уже показал свой OTP или статус.',
    tr: 'LiveBridge bir OTP veya durum güncellemesini yansıttıktan sonra temizlenebilir orijinal bildirimleri kapatır.',
    ptBr:
        'Dispensa notificações originais removíveis depois que o LiveBridge espelha um OTP ou uma atualização de status.',
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
    en: 'Hide weather while the device is unlocked.',
    ru: 'Скрывать погоду при разблокированном устройстве.',
    tr: 'Cihaz kilidi açıkken hava durumunu gizle.',
    ptBr: 'Ocultar clima quando o dispositivo estiver desbloqueado.',
    zhHans: '设备解锁时隐藏天气。',
    zhHant: '裝置解鎖時隱藏天氣。',
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
    en: 'Runs a dedicated ongoing notification with current network speed and surfaces it in the Now Bar.',
    ru: 'Запускает отдельное уведомление с текущей скоростью сети и выводит его в Now Bar.',
    tr: 'Geçerli ağ hızını içeren özel bir ongoing bildirim çalıştırır ve bunu Now Bar\'da gösterir.',
    ptBr:
        'Executa uma notificação ongoing dedicada com a velocidade atual da rede e a exibe na Now Bar.',
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

  String get networkSpeedUploadPrefixTitle => tr(
    en: 'Upload prefix',
    ru: 'Префикс отдачи',
    tr: 'Yükleme ön eki',
    ptBr: 'Prefixo de upload',
    zhHans: '上传前缀',
    zhHant: '上傳前綴',
  );

  String get networkSpeedDownloadPrefixTitle => tr(
    en: 'Download prefix',
    ru: 'Префикс загрузки',
    tr: 'İndirme ön eki',
    ptBr: 'Prefixo de download',
    zhHans: '下载前缀',
    zhHant: '下載前綴',
  );

  String get networkSpeedUnitTitle => tr(
    en: 'Speed unit',
    ru: 'Единица скорости',
    tr: 'Hız birimi',
    ptBr: 'Unidade de velocidade',
    zhHans: '速度单位',
    zhHant: '速度單位',
  );

  String get networkSpeedUnitAuto => tr(
    en: 'Auto',
    ru: 'Авто',
    tr: 'Otomatik',
    ptBr: 'Automático',
    zhHans: '自动',
    zhHant: '自動',
  );

  String get networkSpeedUnitBytes => 'B/s';

  String get networkSpeedUnitKilobytes => 'KB/s';

  String get networkSpeedUnitMegabytes => 'MB/s';

  String get networkSpeedUnitGigabytes => 'GB/s';

  String networkSpeedCurrentValue(String value) => tr(
    en: 'Current: "$value"',
    ru: 'Сейчас: "$value"',
    tr: 'Geçerli: "$value"',
    ptBr: 'Atual: "$value"',
    zhHans: '当前："$value"',
    zhHant: '目前："$value"',
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
    en: 'In total mode, upload speed is shown before download.',
    ru: 'В режиме общей скорости отдача будет стоять перед загрузкой.',
    tr: 'Toplam modda yükleme hızı indirmeden önce gösterilir.',
    ptBr: 'No modo total, a velocidade de upload aparece antes do download.',
    zhHans: '在总速度模式下，上传速度会显示在下载速度之前。',
    zhHant: '在總速度模式下，上傳速度會顯示在下載速度之前。',
  );

  String get networkSpeedLockscreenOnlyTitle => tr(
    en: 'Display only on lock screen',
    ru: 'Показывать только на экране блокировки',
    tr: 'Yalnızca kilit ekranında göster',
    ptBr: 'Exibir somente na tela de bloqueio',
    zhHans: '仅在锁屏显示',
    zhHant: '僅在鎖定畫面顯示',
  );

  String get networkSpeedLockscreenOnlySubtitle => tr(
    en: 'Hide the network speed chip while the device is unlocked.',
    ru: 'Скрывать индикатор скорости при разблокированном устройстве.',
    tr: 'Cihaz kilidi açıkken ağ hızı çipini gizle.',
    ptBr:
        'Ocultar o chip de velocidade da rede quando o dispositivo estiver desbloqueado.',
    zhHans: '设备解锁时隐藏网速胶囊。',
    zhHant: '裝置解鎖時隱藏網速膠囊。',
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
    en: 'Removes the pill background from network speed chips in Now Bar.',
    ru: 'Убирает плашку у чипов скорости в Now Bar.',
    tr: 'Now Bar\'daki ağ hızı çiplerinden kapsül arka planını kaldırır.',
    ptBr:
        'Remove o fundo em formato de pílula dos chips de velocidade da rede na Now Bar.',
    zhHans: '移除 Now Bar 中网速胶囊的药丸背景。',
    zhHant: '移除 Now Bar 中網速膠囊的藥丸背景。',
  );

  String get textProgressTitle => tr(
    en: 'Text progress',
    ru: 'Текстовые прогрессы',
    tr: 'Metin ilerlemesi',
    ptBr: 'Progresso baseado em texto ao invés de barra',
    zhHans: '文本进度',
    zhHant: '文字進度',
  );

  String get nativeProgressDescription => tr(
    en: 'uses Android progress from notifications when available',
    ru: 'использует прогресс из Android-уведомлений, если он есть',
    tr: 'varsa Android bildirimlerindeki ilerlemeyi kullanır',
    ptBr: 'Usa barras de progresso do Android quando disponível',
    zhHans: '可用时使用 Android 通知中的进度',
    zhHant: '可用時使用 Android 通知中的進度',
  );

  String get textProgressDescription => tr(
    en: 'detects progress from notification text like 42%',
    ru: 'определяет прогресс из текста уведомлений, например 42%',
    tr: '42% gibi bildirim metnindeki ilerlemeyi algılar',
    ptBr: 'Detecta o progresso a partir do texto da notificação. Ex: 42%',
    zhHans: '从通知文本中识别进度，例如 42%',
    zhHant: '從通知文字中識別進度，例如 42%',
  );

  String get otpCodesDescription => tr(
    en: 'detects verification codes and shows them in Live Updates',
    ru: 'находит коды подтверждения и показывает их в Live Updates',
    tr: 'doğrulama kodlarını algılar ve Live Updates içinde gösterir',
    ptBr: 'Detecta códigos de verificação e exibe como notificações ao vivo',
    zhHans: '识别验证码并在 Live Updates 中显示',
    zhHant: '識別驗證碼並在 Live Updates 中顯示',
  );

  String get autoCopyCodeDescription => tr(
    en: 'copies detected OTP codes to clipboard automatically',
    ru: 'автоматически копирует найденные OTP-коды в буфер обмена',
    tr: 'algılanan OTP kodlarını otomatik olarak panoya kopyalar',
    ptBr: 'Quando detectado, copia automaticamente os códigos OTP',
    zhHans: '自动将识别到的 OTP 验证码复制到剪贴板',
    zhHant: '自動將識別到的 OTP 驗證碼複製到剪貼簿',
  );

  String get removeOriginalMessageDescription => tr(
    en: 'tries to hide the original notification after conversion',
    ru: 'пытается скрыть исходное уведомление после конвертации',
    tr: 'dönüştürmeden sonra orijinal bildirimi gizlemeyi dener',
    ptBr: 'Tenta esconder a notificação original após a conversão',
    zhHans: '转换后尝试隐藏原始通知',
    zhHant: '轉換後嘗試隱藏原始通知',
  );

  String get taxiDescription => tr(
    en: 'shows taxi ride state as a Live Update',
    ru: 'показывает состояние поездки такси в Live Updates',
    tr: 'taksi yolculuğu durumunu Live Update olarak gösterir',
    ptBr: 'Mostra o estado da corrida de táxi com atualizações ao vivo',
    zhHans: '将打车行程状态显示为 Live Update',
    zhHant: '將計程車行程狀態顯示為 Live Update',
  );

  String get deliveriesDescription => tr(
    en: 'shows delivery progress from food and shopping apps',
    ru: 'показывает прогресс доставки из еды и магазинов',
    tr: 'yemek ve alışveriş uygulamalarındaki teslimat ilerlemesini gösterir',
    ptBr: 'Mostra o progresso da entrega de aplicativos como compras e comidas',
    zhHans: '显示外卖和购物应用的配送进度',
    zhHant: '顯示外送與購物應用程式的配送進度',
  );

  String get allAppsDescription => tr(
    en: 'converts matching notifications from every app',
    ru: 'конвертирует подходящие уведомления из всех приложений',
    tr: 'tüm uygulamalardan eşleşen bildirimleri dönüştürür',
    ptBr: 'Converte notificações recebidas de todos os apps',
    zhHans: '转换所有应用中匹配的通知',
    zhHant: '轉換所有應用程式中符合條件的通知',
  );

  String get onlySelectedDescription => tr(
    en: 'converts notifications only from apps you select',
    ru: 'конвертирует уведомления только из выбранных приложений',
    tr: 'yalnızca seçtiğiniz uygulamalardan gelen bildirimleri dönüştürür',
    ptBr: 'Converte notificação apenas dos apps que você selecionar',
    zhHans: '仅转换你选择的应用通知',
    zhHant: '僅轉換你選取的應用程式通知',
  );

  String get excludeSelectedDescription => tr(
    en: 'converts every app except the apps you select',
    ru: 'конвертирует все приложения, кроме выбранных',
    tr: 'seçtiğiniz uygulamalar dışındaki tüm uygulamaları dönüştürür',
    ptBr: 'Converte todos os aplicativos, exceto os que você selecionar',
    zhHans: '转换除所选应用之外的所有应用',
    zhHant: '轉換除所選應用程式之外的所有應用程式',
  );

  String get vpnsDescription => tr(
    en: 'shows active VPN traffic and connection state',
    ru: 'показывает трафик и состояние активного VPN',
    tr: 'aktif VPN trafiğini ve bağlantı durumunu gösterir',
    ptBr: 'mostra o tráfego da VPN ativa e o estado da conexão',
    zhHans: '显示活动 VPN 流量和连接状态',
    zhHant: '顯示作用中 VPN 流量與連線狀態',
  );

  String get externalDevicesDescription => tr(
    en: 'shows connected external devices in Live Updates',
    ru: 'показывает подключенные внешние устройства в Live Updates',
    tr: 'bağlı harici cihazları Live Updates içinde gösterir',
    ptBr: 'mostra dispositivos externos conectados em Notificações ao vivo',
    zhHans: '在 Live Updates 中显示已连接的外部设备',
    zhHant: '在 Live Updates 中顯示已連接的外部裝置',
  );

  String get ignoreDebuggingDevicesDescription => tr(
    en: 'hides ADB and debugging device notifications',
    ru: 'скрывает ADB и уведомления отладочных устройств',
    tr: 'ADB ve hata ayıklama cihazı bildirimlerini gizler',
    ptBr: 'oculta notificações de ADB e dispositivos de depuração',
    zhHans: '隐藏 ADB 和调试设备通知',
    zhHant: '隱藏 ADB 與偵錯裝置通知',
  );

  String get mediaPlaybackDescription => tr(
    en: 'shows track controls and playback status in Live Updates',
    ru: 'показывает управление треком и статус воспроизведения',
    tr: 'parça kontrollerini ve oynatma durumunu Live Updates içinde gösterir',
    ptBr:
        'mostra controles da faixa e estado da reprodução em Notificações ao vivo',
    zhHans: '在 Live Updates 中显示曲目控制和播放状态',
    zhHant: '在 Live Updates 中顯示曲目控制與播放狀態',
  );

  String get showMediaOnLockDescription => tr(
    en: 'allows media Live Updates on the lockscreen',
    ru: 'разрешает показывать медиа Live Updates на экране блокировки',
    tr: 'kilit ekranında medya Live Updates gösterilmesine izin verir',
    ptBr: 'permite Notificações ao vivo de mídia na tela de bloqueio',
    zhHans: '允许在锁屏显示媒体 Live Updates',
    zhHant: '允許在鎖定畫面顯示媒體 Live Updates',
  );

  String get useSymbolsInMediaPlayerDescription => tr(
    en: 'uses ▶, ⏸, ⏮ and ⏭ instead of text actions',
    ru: 'использует ▶, ⏸, ⏮ и ⏭ вместо текстовых действий',
    tr: 'metin eylemleri yerine ▶, ⏸, ⏮ ve ⏭ kullanır',
    ptBr: 'usa ▶, ⏸, ⏮ e ⏭ em vez de ações em texto',
    zhHans: '使用 ▶、⏸、⏮ 和 ⏭ 代替文字操作',
    zhHant: '使用 ▶、⏸、⏮ 和 ⏭ 取代文字操作',
  );

  String get callsDescription => tr(
    en: 'shows ongoing calls as Live Updates',
    ru: 'показывает активные звонки в Live Updates',
    tr: 'devam eden aramaları Live Updates olarak gösterir',
    ptBr: 'mostra chamadas em andamento como Notificações ao vivo',
    zhHans: '将正在进行的通话显示为 Live Updates',
    zhHant: '將進行中的通話顯示為 Live Updates',
  );

  String get navigationMapsDescription => tr(
    en: 'shows the direction and distance in Live Updates',
    ru: 'показывает направление и расстояние в Live Updates',
    tr: 'yönü ve mesafeyi Live Updates içinde gösterir',
    ptBr: 'mostra direção e distância em Notificações ao vivo',
    zhHans: '在 Live Updates 中显示方向和距离',
    zhHant: '在 Live Updates 中顯示方向與距離',
  );

  String get weatherBroadcastsDescription => tr(
    en: 'shows weather alerts and forecast notifications',
    ru: 'показывает погодные уведомления и прогнозы',
    tr: 'hava durumu uyarılarını ve tahmin bildirimlerini gösterir',
    ptBr: 'mostra alertas de clima e notificações de previsão',
    zhHans: '显示天气警报和预报通知',
    zhHant: '顯示天氣警報與預報通知',
  );

  String get appLanguageDescription => tr(
    en: 'changes the language used by LiveBridge UI',
    ru: 'меняет язык интерфейса LiveBridge',
    tr: 'LiveBridge arayüzünde kullanılan dili değiştirir',
    ptBr: 'altera o idioma usado pela interface do LiveBridge',
    zhHans: '更改 LiveBridge 界面使用的语言',
    zhHant: '變更 LiveBridge 介面使用的語言',
  );

  String get keepAliveForegroundDescription => tr(
    en: 'uses an alternate foreground mode for stricter firmwares',
    ru: 'использует альтернативный foreground-режим для строгих прошивок',
    tr: 'daha katı yazılımlar için alternatif ön plan modunu kullanır',
    ptBr: 'usa um modo foreground alternativo para firmwares mais restritos',
    zhHans: '为限制更严格的系统使用备用前台模式',
    zhHant: '為限制更嚴格的系統使用備用前景模式',
  );

  String get syncDndDescription => tr(
    en: 'syncs Live Updates behavior with Do Not Disturb',
    ru: 'синхронизирует поведение Live Updates с режимом Не беспокоить',
    tr: 'Live Updates davranışını Rahatsız Etmeyin ile eşitler',
    ptBr:
        'sincroniza o comportamento das Notificações ao vivo com o Não Perturbe',
    zhHans: '将 Live Updates 行为与勿扰模式同步',
    zhHant: '將 Live Updates 行為與勿擾模式同步',
  );

  String get preventDismissingDescription => tr(
    en: 'restores the LiveBridge notification after it is swiped away',
    ru: 'восстанавливает уведомление LiveBridge после свайпа',
    tr: 'LiveBridge bildirimi kaydırılıp kapatıldıktan sonra geri yükler',
    ptBr: 'restaura a notificação do LiveBridge depois que ela é dispensada',
    zhHans: '在 LiveBridge 通知被滑掉后恢复它',
    zhHant: '在 LiveBridge 通知被滑掉後恢復它',
  );

  String get hideLockscreenContentDescription => tr(
    en: 'shows Content hidden instead of notification text on the lockscreen',
    ru: 'показывает Content hidden вместо текста уведомления на локскрине',
    tr: 'kilit ekranında bildirim metni yerine Content hidden gösterir',
    ptBr:
        'mostra Content hidden em vez do texto da notificação na tela de bloqueio',
    zhHans: '在锁屏上显示 Content hidden 而不是通知文本',
    zhHant: '在鎖定畫面顯示 Content hidden 而不是通知文字',
  );

  String get disableHintsDescription => tr(
    en: 'hides info icons and hint popovers across the redesign UI',
    ru: 'скрывает иконки info и всплывающие подсказки в новом интерфейсе',
    tr: 'yeni arayüzde bilgi simgelerini ve ipucu pencerelerini gizler',
    ptBr: 'oculta ícones de informação e dicas na nova interface',
    zhHans: '隐藏新版界面中的信息图标和提示弹窗',
    zhHant: '隱藏新版介面中的資訊圖示與提示彈窗',
  );

  String get conversionLogDescription => tr(
    en: 'keeps recent converted notifications for debugging',
    ru: 'сохраняет последние конвертации для отладки',
    tr: 'hata ayıklama için son dönüştürülen bildirimleri saklar',
    ptBr: 'mantém notificações convertidas recentemente para depuração',
    zhHans: '保留最近转换的通知用于调试',
    zhHant: '保留最近轉換的通知用於偵錯',
  );

  String get logLengthDescription => tr(
    en: 'limits how much conversion log data is kept on device',
    ru: 'ограничивает объем лога конвертаций на устройстве',
    tr: 'cihazda tutulacak dönüştürme günlüğü verisi miktarını sınırlar',
    ptBr: 'limita quantos dados do log de conversões ficam no dispositivo',
    zhHans: '限制设备上保留的转换日志数据量',
    zhHant: '限制裝置上保留的轉換記錄資料量',
  );

  String get networkSpeedDescription => tr(
    en: 'shows current network traffic as a Live Update',
    ru: 'показывает текущий трафик сети как Live Update',
    tr: 'mevcut ağ trafiğini Live Update olarak gösterir',
    ptBr: 'mostra o tráfego de rede atual como Notificação ao vivo',
    zhHans: '将当前网络流量显示为 Live Update',
    zhHant: '將目前網路流量顯示為 Live Update',
  );

  String get networkSpeedThresholdDescription => tr(
    en: 'hides the network speed Live Update below this traffic level',
    ru: 'скрывает Live Update скорости сети ниже этого порога',
    tr: 'ağ hızı bu seviyenin altındaysa Live Update öğesini gizler',
    ptBr:
        'oculta a Notificação ao vivo de velocidade de rede abaixo deste nível',
    zhHans: '当网速低于此阈值时隐藏网络速度 Live Update',
    zhHant: '當網速低於此門檻時隱藏網路速度 Live Update',
  );

  String get xiaomiHyperIslandDescription => tr(
    en: 'enables Xiaomi HyperIsland-specific Live Updates behavior',
    ru: 'включает поведение Live Updates для Xiaomi HyperIsland',
    tr: 'Xiaomi HyperIsland için özel Live Updates davranışını etkinleştirir',
    ptBr:
        'ativa comportamento de Notificações ao vivo específico do Xiaomi HyperIsland',
    zhHans: '启用 Xiaomi HyperIsland 专用的 Live Updates 行为',
    zhHant: '啟用 Xiaomi HyperIsland 專用的 Live Updates 行為',
  );

  String get aospCuttingDescription => tr(
    en: 'shortens island text on AOSP-like firmwares that clip long content',
    ru: 'укорачивает текст острова на AOSP-прошивках, где длинный текст обрезается',
    tr: 'uzun içeriği kırpan AOSP benzeri yazılımlarda ada metnini kısaltır',
    ptBr:
        'encurta o texto da ilha em firmwares parecidos com AOSP que cortam conteúdo longo',
    zhHans: '在会截断长内容的类 AOSP 系统上缩短岛内文本',
    zhHant: '在會截斷長內容的類 AOSP 系統上縮短島內文字',
  );

  String get aospCuttingLengthDescription => tr(
    en: 'sets the maximum island text length for AOSP cutting',
    ru: 'задает максимальную длину текста острова для AOSP-обрезки',
    tr: 'AOSP kırpması için en uzun ada metni uzunluğunu ayarlar',
    ptBr: 'define o comprimento máximo do texto da ilha para o corte AOSP',
    zhHans: '设置 AOSP 截断模式下岛内文本的最大长度',
    zhHant: '設定 AOSP 截斷模式下島內文字的最大長度',
  );

  String get updateFrequencyDescription => tr(
    en: 'controls how often animated island text frames are refreshed',
    ru: 'задает частоту обновления кадров анимации текста острова',
    tr: 'animasyonlu ada metni karelerinin ne sıklıkta yenileneceğini ayarlar',
    ptBr:
        'controla a frequência de atualização dos quadros de texto da ilha animada',
    zhHans: '控制动画岛文本帧的刷新频率',
    zhHant: '控制動畫島文字影格的刷新頻率',
  );

  String get otpDedupDescription => tr(
    en: 'reduces repeated OTP notifications from the same source',
    ru: 'уменьшает повторы OTP-уведомлений из одного источника',
    tr: 'aynı kaynaktan gelen tekrarlı OTP bildirimlerini azaltır',
    ptBr: 'reduz notificações OTP repetidas da mesma origem',
    zhHans: '减少同一来源的重复 OTP 通知',
    zhHant: '減少同一來源的重複 OTP 通知',
  );

  String get smartConversionDedupDescription => tr(
    en: 'reduces repeated smart conversion notifications',
    ru: 'уменьшает повторы уведомлений умной конвертации',
    tr: 'tekrarlı akıllı dönüştürme bildirimlerini azaltır',
    ptBr: 'reduz notificações repetidas de conversão inteligente',
    zhHans: '减少重复的智能转换通知',
    zhHant: '減少重複的智慧轉換通知',
  );

  String get animatedIslandDescription => tr(
    en: 'adds smooth island text animations for supported conversions',
    ru: 'добавляет плавные анимации текста острова',
    tr: 'desteklenen dönüştürmeler için akıcı ada metni animasyonları ekler',
    ptBr:
        'adiciona animações suaves ao texto da ilha nas conversões compatíveis',
    zhHans: '为支持的转换添加流畅的岛内文本动画',
    zhHant: '為支援的轉換加入流暢的島內文字動畫',
  );
}
