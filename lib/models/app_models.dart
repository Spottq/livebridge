import 'dart:typed_data';

const int defaultNotificationColorArgb = 0xFF0F766E;

const Object _copyUnset = Object();

class InstalledApp {
  const InstalledApp({
    required this.packageName,
    required this.label,
    this.icon,
    this.isSystem = false,
  });

  final String packageName;
  final String label;
  final Uint8List? icon;
  final bool isSystem;
}

class DeviceInfo {
  const DeviceInfo({
    required this.manufacturer,
    required this.brand,
    required this.marketName,
    required this.model,
    this.rawModel = '',
    this.product = '',
    this.device = '',
    this.board = '',
    this.hardware = '',
    this.bootloader = '',
    this.host = '',
    this.id = '',
    this.tags = '',
    this.type = '',
    this.user = '',
    this.fingerprint = '',
    this.display = '',
  });

  final String manufacturer;
  final String brand;
  final String marketName;
  final String model;
  final String rawModel;
  final String product;
  final String device;
  final String board;
  final String hardware;
  final String bootloader;
  final String host;
  final String id;
  final String tags;
  final String type;
  final String user;
  final String fingerprint;
  final String display;

  bool get isPixel {
    final String all = '$manufacturer $brand $marketName $model'.toLowerCase();
    return all.contains('google') || all.contains('pixel');
  }

  bool get isSamsung {
    final String all = '$manufacturer $brand'.toLowerCase();
    return all.contains('samsung');
  }

  bool get isAospDevice {
    final String all =
        '$manufacturer $brand $marketName $model $rawModel $product $device '
                '$board $hardware $bootloader $host $id $tags $type $user '
                '$fingerprint $display'
            .toLowerCase();
    final bool hasCustomBuildKeys =
        tags.toLowerCase().contains('test-keys') ||
        tags.toLowerCase().contains('dev-keys');
    const List<String> customRomMarkers = <String>[
      'lineage',
      'evolution',
      'evox',
      'crdroid',
      'pixelos',
      'graphene',
      'calyx',
      'arrowos',
      'risingos',
      'yaap',
      'derpfest',
      'paranoid',
      'aospa',
      'omnirom',
      'omni',
      'resurrection',
      'superior',
      'cherish',
      'sparkos',
      'elixir',
      'hentaios',
      'aicp',
      'iodГ©',
      'iode',
      'aosp',
    ];
    return isPixel ||
        hasCustomBuildKeys ||
        all.contains('nothing') ||
        all.contains('motorola') ||
        customRomMarkers.any(all.contains);
  }

  bool get shouldHideLiveUpdatesPromotion => isSamsung || isAospDevice;

  String get label {
    if (marketName.isNotEmpty) return marketName;
    if (model.isNotEmpty) return model;
    if (brand.isNotEmpty) return brand;
    if (manufacturer.isNotEmpty) return manufacturer;
    return 'device';
  }
}

class FlashlightCapability {
  const FlashlightCapability({
    this.available = false,
    this.supportsStrengthControl = false,
    this.supportsFiveLevels = false,
    this.maxStrengthLevel = 0,
  });

  final bool available;
  final bool supportsStrengthControl;
  final bool supportsFiveLevels;
  final int maxStrengthLevel;

  bool get supportsInteractiveLevels => available && supportsFiveLevels;
  bool get hasFallbackWarning => available && !supportsFiveLevels;

  factory FlashlightCapability.fromMap(Map<String, dynamic> map) {
    return FlashlightCapability(
      available: map['available'] == true,
      supportsStrengthControl: map['supportsStrengthControl'] == true,
      supportsFiveLevels: map['supportsFiveLevels'] == true,
      maxStrengthLevel: (map['maxStrengthLevel'] as num?)?.toInt() ?? 0,
    );
  }
}

enum PackageMode { all, include, exclude }

extension PackageModeId on PackageMode {
  String get id {
    switch (this) {
      case PackageMode.all:
        return 'all';
      case PackageMode.include:
        return 'include';
      case PackageMode.exclude:
        return 'exclude';
    }
  }

  static PackageMode from(String value) {
    switch (value) {
      case 'include':
        return PackageMode.include;
      case 'exclude':
        return PackageMode.exclude;
      default:
        return PackageMode.all;
    }
  }
}

enum NetworkSpeedDisplayMode { total, upload, download }

enum NotificationCapsuleMode { general, perApp }

extension NetworkSpeedDisplayModeId on NetworkSpeedDisplayMode {
  String get id {
    switch (this) {
      case NetworkSpeedDisplayMode.total:
        return 'total';
      case NetworkSpeedDisplayMode.upload:
        return 'upload';
      case NetworkSpeedDisplayMode.download:
        return 'download';
    }
  }

  static NetworkSpeedDisplayMode from(String? value) {
    switch (value) {
      case 'upload':
        return NetworkSpeedDisplayMode.upload;
      case 'download':
        return NetworkSpeedDisplayMode.download;
      default:
        return NetworkSpeedDisplayMode.total;
    }
  }
}

extension NotificationCapsuleModeId on NotificationCapsuleMode {
  String get id {
    switch (this) {
      case NotificationCapsuleMode.general:
        return 'general';
      case NotificationCapsuleMode.perApp:
        return 'per_app';
    }
  }

  static NotificationCapsuleMode from(String? value) {
    switch (value) {
      case 'per_app':
        return NotificationCapsuleMode.perApp;
      default:
        return NotificationCapsuleMode.general;
    }
  }
}

enum NotificationDedupMode { otpStatus, otpOnly }

extension NotificationDedupModeId on NotificationDedupMode {
  String get id {
    switch (this) {
      case NotificationDedupMode.otpStatus:
        return 'otp_status';
      case NotificationDedupMode.otpOnly:
        return 'otp_only';
    }
  }

  static NotificationDedupMode from(String? value) {
    switch (value) {
      case 'otp_only':
        return NotificationDedupMode.otpOnly;
      default:
        return NotificationDedupMode.otpStatus;
    }
  }
}

enum AppCompactTextSource { title, text }

extension AppCompactTextSourceId on AppCompactTextSource {
  String get id {
    switch (this) {
      case AppCompactTextSource.title:
        return 'title';
      case AppCompactTextSource.text:
        return 'text';
    }
  }

  static AppCompactTextSource from(String? value) {
    switch (value) {
      case 'text':
        return AppCompactTextSource.text;
      default:
        return AppCompactTextSource.title;
    }
  }
}

enum AppNotificationIconSource { notification, app }

extension AppNotificationIconSourceId on AppNotificationIconSource {
  String get id {
    switch (this) {
      case AppNotificationIconSource.notification:
        return 'notification';
      case AppNotificationIconSource.app:
        return 'app';
    }
  }

  static AppNotificationIconSource from(String? value) {
    switch (value) {
      case 'notification':
        return AppNotificationIconSource.notification;
      default:
        return AppNotificationIconSource.app;
    }
  }
}

enum AppPresentationTitleSource { notificationTitle, appTitle }

extension AppPresentationTitleSourceId on AppPresentationTitleSource {
  String get id {
    switch (this) {
      case AppPresentationTitleSource.notificationTitle:
        return 'notification_title';
      case AppPresentationTitleSource.appTitle:
        return 'app_title';
    }
  }

  static AppPresentationTitleSource? tryParse(String? value) {
    switch (value) {
      case 'notification_title':
        return AppPresentationTitleSource.notificationTitle;
      case 'app_title':
        return AppPresentationTitleSource.appTitle;
      default:
        return null;
    }
  }
}

enum AppPresentationContentSource { notificationText, notificationTitle }

extension AppPresentationContentSourceId on AppPresentationContentSource {
  String get id {
    switch (this) {
      case AppPresentationContentSource.notificationText:
        return 'notification_text';
      case AppPresentationContentSource.notificationTitle:
        return 'notification_title';
    }
  }

  static AppPresentationContentSource? tryParse(String? value) {
    switch (value) {
      case 'notification_text':
        return AppPresentationContentSource.notificationText;
      case 'notification_title':
        return AppPresentationContentSource.notificationTitle;
      default:
        return null;
    }
  }
}

class AppPresentationOverride {
  const AppPresentationOverride({
    this.compactTextSource = AppCompactTextSource.title,
    this.iconSource = AppNotificationIconSource.app,
    this.titleSource,
    this.contentSource,
    this.removeOriginalMessage = false,
    this.notificationColorArgb,
    this.notificationColorEnabled = false,
  });

  final AppCompactTextSource compactTextSource;
  final AppNotificationIconSource iconSource;
  final AppPresentationTitleSource? titleSource;
  final AppPresentationContentSource? contentSource;
  final bool removeOriginalMessage;
  final int? notificationColorArgb;
  final bool notificationColorEnabled;

  bool get usesExplicitSources => titleSource != null || contentSource != null;

  int? get effectiveNotificationColorArgb =>
      notificationColorEnabled ? notificationColorArgb : null;

  AppPresentationTitleSource get effectiveTitleSource =>
      titleSource ?? AppPresentationTitleSource.notificationTitle;

  AppPresentationContentSource get effectiveContentSource =>
      contentSource ?? AppPresentationContentSource.notificationText;

  bool get isDefault =>
      iconSource == AppNotificationIconSource.app &&
      compactTextSource == AppCompactTextSource.title &&
      effectiveTitleSource == AppPresentationTitleSource.notificationTitle &&
      effectiveContentSource == AppPresentationContentSource.notificationText &&
      !removeOriginalMessage &&
      notificationColorArgb == null &&
      !notificationColorEnabled;

  bool get isEffectiveDefault => isDefault;

  AppPresentationOverride copyWith({
    AppCompactTextSource? compactTextSource,
    AppNotificationIconSource? iconSource,
    AppPresentationTitleSource? titleSource,
    AppPresentationContentSource? contentSource,
    bool? removeOriginalMessage,
    Object? notificationColorArgb = _copyUnset,
    bool? notificationColorEnabled,
  }) {
    return AppPresentationOverride(
      compactTextSource: compactTextSource ?? this.compactTextSource,
      iconSource: iconSource ?? this.iconSource,
      titleSource: titleSource ?? this.titleSource,
      contentSource: contentSource ?? this.contentSource,
      removeOriginalMessage:
          removeOriginalMessage ?? this.removeOriginalMessage,
      notificationColorArgb: identical(notificationColorArgb, _copyUnset)
          ? this.notificationColorArgb
          : notificationColorArgb as int?,
      notificationColorEnabled:
          notificationColorEnabled ?? this.notificationColorEnabled,
    );
  }

  Map<String, String> toJsonEntry() {
    final Map<String, String> payload = <String, String>{
      'icon_source': iconSource.id,
    };
    final int? color = notificationColorArgb;
    if (color != null) {
      payload['notification_color'] = _formatNotificationColor(color);
      if (!notificationColorEnabled) {
        payload['notification_color_enabled'] = 'false';
      }
    }
    if (removeOriginalMessage) {
      payload['remove_original_message'] = 'true';
    }
    if (usesExplicitSources) {
      payload['title_source'] = effectiveTitleSource.id;
      payload['content_source'] = effectiveContentSource.id;
    } else {
      payload['compact_text'] = compactTextSource.id;
    }
    return payload;
  }

  static AppPresentationOverride fromJsonEntry(Map<String, dynamic> json) {
    final AppPresentationTitleSource? titleSource =
        AppPresentationTitleSourceId.tryParse(json['title_source'] as String?);
    final AppPresentationContentSource? contentSource =
        AppPresentationContentSourceId.tryParse(
          json['content_source'] as String?,
        );

    final int? notificationColor = _parseNotificationColor(
      json['notification_color'],
    );
    final bool notificationColorEnabled = notificationColor != null
        ? _parseNotificationColorEnabled(json['notification_color_enabled'])
        : false;

    return AppPresentationOverride(
      compactTextSource: (titleSource != null || contentSource != null)
          ? AppCompactTextSource.title
          : AppCompactTextSourceId.from(json['compact_text'] as String?),
      iconSource: AppNotificationIconSourceId.from(
        json['icon_source'] as String?,
      ),
      titleSource: titleSource,
      contentSource: contentSource,
      removeOriginalMessage:
          json['remove_original_message'] == true ||
          json['remove_original_message'] == 'true',
      notificationColorArgb: notificationColor,
      notificationColorEnabled: notificationColorEnabled,
    );
  }

  static bool _parseNotificationColorEnabled(dynamic raw) {
    if (raw == null) {
      return true;
    }
    if (raw is bool) {
      return raw;
    }
    final String value = raw.toString().trim().toLowerCase();
    return value != 'false' && value != '0' && value != 'off';
  }

  static int? _parseNotificationColor(dynamic raw) {
    if (raw is num) {
      return 0xFF000000 | (raw.toInt() & 0x00FFFFFF);
    }
    if (raw is! String) {
      return null;
    }

    String value = raw.trim();
    if (value.isEmpty) {
      return null;
    }
    if (value.startsWith('#')) {
      value = value.substring(1);
    } else if (value.toLowerCase().startsWith('0x')) {
      value = value.substring(2);
    }
    if (value.length == 8) {
      value = value.substring(2);
    }
    if (value.length != 6 || !RegExp(r'^[0-9a-fA-F]{6}$').hasMatch(value)) {
      return null;
    }

    return 0xFF000000 | int.parse(value, radix: 16);
  }

  static String _formatNotificationColor(int argb) {
    final int rgb = argb & 0x00FFFFFF;
    return '#${rgb.toRadixString(16).padLeft(6, '0').toUpperCase()}';
  }
}
