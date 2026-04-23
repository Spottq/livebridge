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
    ptBr: 'Dicionário atualizado pelo GitHub.',
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
    ptBr: 'Falha ao atualizar o dicionário pelo GitHub.',
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
    tr: 'yönetmek için dokunun',
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
    tr: 'seçmek için dokunun',
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

  String get dictionaryEditorTitle => tr(
    en: 'Dictionary editor',
    ru: 'Редактор словаря',
    tr: 'Sözlük düzenleyici',
    ptBr: 'Editor de dicionário',
    zhHans: '词典编辑器',
    zhHant: '字典編輯器',
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
    tr: 'Uygulama yapılandırması',
    ptBr: 'Configuração do app',
    zhHans: '应用配置',
    zhHant: '應用配置',
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
    en: 'LiveBridge is running',
    ru: 'LiveBridge запущен',
    tr: 'LiveBridge çalışıyor',
    ptBr: 'LiveBridge está em execução',
    zhHans: 'LiveBridge 正在运行',
    zhHant: 'LiveBridge 正在執行',
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
    ptBr: 'tópicos no Telegram',
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
    ptBr: 'Progresso',
    zhHans: '进度',
    zhHant: '進度',
  );

  String get nativeProgressTitle => tr(
    en: 'Native progress',
    ru: 'Нативный прогресс',
    tr: 'Yerel ilerleme',
    ptBr: 'Progresso nativo',
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
    ptBr: 'Remover mensagem original',
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
    ptBr: 'mostrar sistema',
    zhHans: '显示系统',
    zhHant: '顯示系統',
  );

  String get hideSystem => tr(
    en: 'hide system',
    ru: 'скрыть системные',
    tr: 'sistem uygulamalarını gizle',
    ptBr: 'ocultar sistema',
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

  String get weatherBroadcastsTitle => tr(
    en: 'Weather broadcasts',
    ru: 'Погодные уведомления',
    tr: 'Hava durumu bildirimleri',
    ptBr: 'Alertas de clima',
    zhHans: '天气播报',
    zhHant: '天氣播報',
  );

  String get bypassTitle => tr(
    en: 'Bypass',
    ru: 'Bypass',
    tr: 'Bypass',
    ptBr: 'Bypass',
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
    ptBr: 'Tudo certo',
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
    ptBr: 'Deduplicação de OTP',
    zhHans: 'OTP 去重',
    zhHant: 'OTP 去重',
  );

  String get smartConversionDedupTitle => tr(
    en: 'Smart conversion dedup',
    ru: 'Smart conversion dedup',
    tr: 'Akıllı dönüştürme tekilleştirme',
    ptBr: 'Deduplicação da conversão inteligente',
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

  String get openGithubPageTitle => tr(
    en: 'Open GitHub page',
    ru: 'Открыть GitHub',
    tr: 'GitHub sayfasını aç',
    ptBr: 'Abrir página do GitHub',
    zhHans: '打开 GitHub 页面',
    zhHant: '開啟 GitHub 頁面',
  );

  String get autoCopyDebugJsonTitle => tr(
    en: 'Auto-copy debug JSON',
    ru: 'Автокопирование debug JSON',
    tr: 'Debug JSON\'unu otomatik kopyala',
    ptBr: 'Copiar JSON de debug automaticamente',
    zhHans: '自动复制调试 JSON',
    zhHant: '自動複製偵錯 JSON',
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
    ptBr: 'Acesso ao Notification Listener',
    zhHans: '通知监听访问',
    zhHant: '通知監聽存取',
  );

  String get postNotifications => tr(
    en: 'Post notifications permission',
    ru: 'Отправка уведомлений',
    tr: 'Bildirim gönderme izni',
    ptBr: 'Permissão para enviar notificações',
    zhHans: '发送通知权限',
    zhHant: '發送通知權限',
  );

  String get liveUpdatesAccess => tr(
    en: 'Live Updates promotion',
    ru: 'Разрешение на Live Updates',
    tr: 'Live Updates tanıtımı',
    ptBr: 'Permissão para Live Updates',
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
    ptBr: 'Modo alternativo em segundo plano',
    zhHans: '替代后台模式',
    zhHant: '替代背景模式',
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
    tr: 'Kapatmayı engelle',
    ptBr: 'Impedir dispensa',
    zhHans: '防止被关闭',
    zhHant: '防止被關閉',
  );

  String get updateChecksTitle => tr(
    en: 'Update checking',
    ru: 'Проверка обновлений',
    tr: 'Güncelleme denetimi',
    ptBr: 'Verificação de atualizações',
    zhHans: '检查更新',
    zhHant: '檢查更新',
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
  );

  String get appPresentationTextSourceLabel => tr(
    en: 'Island text source',
    ru: 'Источник текста для острова',
  );

  String get appPresentationIconSourceLabel => tr(
    en: 'Icon source',
    ru: 'Источник иконки',
  );

  String get appPresentationTextTitle => tr(
    en: 'Notification title',
    ru: 'Заголовок уведомления',
  );

  String get appPresentationTextNotification => tr(
    en: 'Notification text',
    ru: 'Текст уведомления',
  );

  String get appPresentationIconNotification => tr(
    en: 'Notification icon',
    ru: 'Иконка уведомления',
  );

  String get appPresentationIconApp => tr(
    en: 'Application icon',
    ru: 'Иконка приложения',
  );

  String get searchAppHint => tr(
    en: 'Search by app or package',
    ru: 'Поиск по названию или пакету',
  );

  String get showSystemApps => tr(
    en: 'Show system applications',
    ru: 'Показать системные приложения',
  );

  String get hideSystemApps => tr(
    en: 'Hide system applications',
    ru: 'Скрыть системные приложения',
  );

  String get resetToDefault => tr(
    en: 'Reset to default',
    ru: 'Сбросить к стандарту',
  );

  String get notificationDedupTitle => tr(
    en: 'Notification dedup',
    ru: 'Удаление дублей уведомлений',
  );

  String get notificationDedupSubtitle => tr(
    en: 'Dismisses original clearable notifications after LiveBridge mirrors an OTP or status update.',
    ru: 'Убирает исходные смахиваемые уведомления, если LiveBridge уже показал свой OTP или статус.',
  );

  String get notificationDedupModeLabel => tr(
    en: 'Dedup mode',
    ru: 'Режим удаления дублей',
  );

  String get notificationDedupModeOtpOnly => tr(
    en: 'OTP only',
    ru: 'Только OTP',
  );

  String get notificationDedupModeOtpStatus => tr(
    en: 'OTP and statuses',
    ru: 'OTP и статусы',
  );

  String get samsungRemoteParserTitle => tr(
    en: 'Samsung RemoteViews reparser',
    ru: 'Samsung RemoteViews репарсер',
  );

  String get samsungRemoteParserSubtitle => tr(
    en: 'Uses Samsung ongoingActivity extras and RemoteViews for improved parsing on One UI.',
    ru: 'Использует Samsung ongoingActivity extras и RemoteViews для более точного парсинга уведомлений.',
  );

  String get smartNavigationSubtitle => tr(
    en: 'Navigation notification detection.',
    ru: 'Распознавание уведомлений навигации.',
  );

  String get smartMediaPlaybackSubtitle => tr(
    en: 'Converts media playback notifications into Live. On some OEMs this may duplicate native media UI.',
    ru: 'Преобразует уведомления медиаплеера в Live. На некоторых OEM может дублировать нативный плеер.',
  );

  String get smartWeatherSubtitle => tr(
    en: 'Weather notification detection (temperature in island).',
    ru: 'Распознавание погодных уведомлений (температура в острове).',
  );

  String get smartWeatherLockscreenOnlyTitle => tr(
    en: 'Display only on lock screen',
    ru: 'Показывать только на экране блокировки',
  );

  String get smartWeatherLockscreenOnlySubtitle => tr(
    en: 'Hide weather while the device is unlocked.',
    ru: 'Скрывать погоду при разблокированном устройстве.',
  );

  String get smartFlashlightTitle => tr(
    en: 'Flashlight',
    ru: 'Фонарик',
  );

  String get smartFlashlightSubtitle => tr(
    en: 'Creates a LiveBridge flashlight notification and mirrors the system flashlight state into Now Bar.',
    ru: 'Создаёт уведомление фонарика LiveBridge и отражает системное состояние фонарика в Now Bar.',
  );

  String get smartFlashlightUnsupportedSubtitle => tr(
    en: 'This device can enable the flashlight, but it does not expose 5 separate brightness levels.',
    ru: 'Устройство включает фонарик, но не даёт 5 отдельных уровней яркости.',
  );

  String get smartFlashlightUnavailableSubtitle => tr(
    en: 'This device does not expose a usable flashlight.',
    ru: 'На этом устройстве нет доступного фонарика.',
  );

  String get smartVpnSubtitle => tr(
    en: 'Shows incoming/outgoing traffic speed in *b/s format.',
    ru: 'Показывает входящий/исходящий трафик в формате *b/s.',
  );

  String get smartExternalDevicesSubtitle => tr(
    en: 'Shows connected/connecting status and device name in island.',
    ru: 'Показывает статус подключения и имя устройства в острове.',
  );

  String get smartExternalDevicesIgnoreDebuggingSubtitle => tr(
    en: 'Skip Live updates for USB debugging, wireless debugging, ADB, and similar system notifications.',
    ru: 'Не показывать Live для USB debugging, wireless debugging, ADB и похожих системных уведомлений.',
  );

  String get networkSpeedEnabledTitle => tr(
    en: 'Show network speed in Now Bar',
    ru: 'Показывать скорость интернета в Now Bar',
  );

  String get networkSpeedEnabledSubtitle => tr(
    en: 'Runs a dedicated ongoing notification with current network speed and surfaces it in the Now Bar.',
    ru: 'Запускает отдельное уведомление с текущей скоростью сети и выводит его в Now Bar.',
  );

  String get networkSpeedThresholdTitle => tr(
    en: 'Minimum speed to show',
    ru: 'Минимальная скорость для показа',
  );

  String get networkSpeedThresholdSubtitle => tr(
    en: 'The live element appears when combined download and upload reach this threshold.',
    ru: 'Live-индикатор появится, когда суммарная скорость загрузки и отдачи достигнет этого порога.',
  );

  String get networkSpeedDisplayContentTitle => tr(
    en: 'Display content',
    ru: 'Отображаемый контент',
  );

  String get networkSpeedDisplayModeTotal => tr(
    en: 'Total speed',
    ru: 'Общая скорость',
  );

  String get networkSpeedDisplayModeUpload => tr(
    en: 'Upload only',
    ru: 'Только отдача',
  );

  String get networkSpeedDisplayModeDownload => tr(
    en: 'Download only',
    ru: 'Только загрузка',
  );

  String get networkSpeedUploadPrefixTitle => tr(
    en: 'Upload prefix',
    ru: 'Префикс отдачи',
  );

  String get networkSpeedDownloadPrefixTitle => tr(
    en: 'Download prefix',
    ru: 'Префикс загрузки',
  );

  String get networkSpeedUnitTitle => tr(
    en: 'Speed unit',
    ru: 'Единица скорости',
  );

  String get networkSpeedUnitAuto => tr(
    en: 'Auto',
    ru: 'Авто',
  );

  String get networkSpeedUnitBytes => 'B/s';

  String get networkSpeedUnitKilobytes => 'KB/s';

  String get networkSpeedUnitMegabytes => 'MB/s';

  String get networkSpeedUnitGigabytes => 'GB/s';

  String networkSpeedCurrentValue(String value) => tr(
    en: 'Current: "$value"',
    ru: 'Сейчас: "$value"',
  );

  String get networkSpeedPrioritizeUploadTitle => tr(
    en: 'Prioritize upload speed',
    ru: 'Сначала показывать отдачу',
  );

  String get networkSpeedPrioritizeUploadSubtitle => tr(
    en: 'In total mode, upload speed is shown before download.',
    ru: 'В режиме общей скорости отдача будет стоять перед загрузкой.',
  );

  String get networkSpeedLockscreenOnlyTitle => tr(
    en: 'Display only on lock screen',
    ru: 'Показывать только на экране блокировки',
  );

  String get networkSpeedLockscreenOnlySubtitle => tr(
    en: 'Hide the network speed chip while the device is unlocked.',
    ru: 'Скрывать индикатор скорости при разблокированном устройстве.',
  );

  String get networkSpeedDisableChipBackgroundTitle => tr(
    en: 'Disable chip background',
    ru: 'Отключить фон чипов',
  );

  String get networkSpeedDisableChipBackgroundSubtitle => tr(
    en: 'Removes the pill background from network speed chips in Now Bar.',
    ru: 'Убирает плашку у чипов скорости в Now Bar.',
  );

  String get textProgressTitle => tr(
    en: 'Text progress',
    ru: 'Текстовые прогрессы',
    tr: 'Metin ilerlemesi',
    ptBr: 'Progresso por texto',
    zhHans: '文本进度',
    zhHant: '文字進度',
  );
}
