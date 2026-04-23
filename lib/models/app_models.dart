import 'dart:typed_data';

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

const String kDefaultNetworkSpeedUploadPrefix = '\u25B2 ';
const String kDefaultNetworkSpeedDownloadPrefix = '\u25BC ';

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

enum NetworkSpeedUnit { auto, bytes, kilobytes, megabytes, gigabytes }

const List<NetworkSpeedUnit> kNetworkSpeedUnitValues = <NetworkSpeedUnit>[
  NetworkSpeedUnit.auto,
  NetworkSpeedUnit.bytes,
  NetworkSpeedUnit.kilobytes,
  NetworkSpeedUnit.megabytes,
  NetworkSpeedUnit.gigabytes,
];

extension NetworkSpeedUnitId on NetworkSpeedUnit {
  String get id {
    switch (this) {
      case NetworkSpeedUnit.auto:
        return 'auto';
      case NetworkSpeedUnit.bytes:
        return 'b';
      case NetworkSpeedUnit.kilobytes:
        return 'kb';
      case NetworkSpeedUnit.megabytes:
        return 'mb';
      case NetworkSpeedUnit.gigabytes:
        return 'gb';
    }
  }

  static NetworkSpeedUnit from(String? value) {
    switch (value) {
      case 'b':
        return NetworkSpeedUnit.bytes;
      case 'kb':
        return NetworkSpeedUnit.kilobytes;
      case 'mb':
        return NetworkSpeedUnit.megabytes;
      case 'gb':
        return NetworkSpeedUnit.gigabytes;
      default:
        return NetworkSpeedUnit.auto;
    }
  }
}

class NetworkSpeedUnitSelection {
  const NetworkSpeedUnitSelection._();

  static Set<NetworkSpeedUnit> parse(String? raw) {
    final Set<NetworkSpeedUnit> selected = <NetworkSpeedUnit>{};
    for (final String token in (raw ?? '').split(',')) {
      final NetworkSpeedUnit? unit = tryParse(token.trim());
      if (unit != null) {
        selected.add(unit);
      }
    }
    return selected;
  }

  static String encode(Iterable<NetworkSpeedUnit> units) {
    final Set<NetworkSpeedUnit> selected = units.toSet();
    if (selected.isEmpty) {
      return '';
    }
    if (selected.contains(NetworkSpeedUnit.auto)) {
      return NetworkSpeedUnit.auto.id;
    }
    return kNetworkSpeedUnitValues
        .where(
          (NetworkSpeedUnit unit) =>
              unit != NetworkSpeedUnit.auto && selected.contains(unit),
        )
        .map((NetworkSpeedUnit unit) => unit.id)
        .join(',');
  }

  static bool usesAuto(Set<NetworkSpeedUnit> units) {
    return units.isEmpty || units.contains(NetworkSpeedUnit.auto);
  }

  static NetworkSpeedUnit? tryParse(String? value) {
    switch (value) {
      case 'auto':
        return NetworkSpeedUnit.auto;
      case 'b':
        return NetworkSpeedUnit.bytes;
      case 'kb':
        return NetworkSpeedUnit.kilobytes;
      case 'mb':
        return NetworkSpeedUnit.megabytes;
      case 'gb':
        return NetworkSpeedUnit.gigabytes;
      default:
        return null;
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
  });

  final AppCompactTextSource compactTextSource;
  final AppNotificationIconSource iconSource;
  final AppPresentationTitleSource? titleSource;
  final AppPresentationContentSource? contentSource;
  final bool removeOriginalMessage;

  bool get usesExplicitSources => titleSource != null || contentSource != null;

  AppPresentationTitleSource get effectiveTitleSource =>
      titleSource ?? AppPresentationTitleSource.notificationTitle;

  AppPresentationContentSource get effectiveContentSource =>
      contentSource ?? AppPresentationContentSource.notificationText;

  bool get isDefault =>
      iconSource == AppNotificationIconSource.app &&
      compactTextSource == AppCompactTextSource.title &&
      effectiveTitleSource == AppPresentationTitleSource.notificationTitle &&
      effectiveContentSource == AppPresentationContentSource.notificationText &&
      !removeOriginalMessage;

  bool get isEffectiveDefault => isDefault;

  AppPresentationOverride copyWith({
    AppCompactTextSource? compactTextSource,
    AppNotificationIconSource? iconSource,
    AppPresentationTitleSource? titleSource,
    AppPresentationContentSource? contentSource,
    bool? removeOriginalMessage,
  }) {
    return AppPresentationOverride(
      compactTextSource: compactTextSource ?? this.compactTextSource,
      iconSource: iconSource ?? this.iconSource,
      titleSource: titleSource ?? this.titleSource,
      contentSource: contentSource ?? this.contentSource,
      removeOriginalMessage:
          removeOriginalMessage ?? this.removeOriginalMessage,
    );
  }

  Map<String, String> toJsonEntry() {
    final Map<String, String> payload = <String, String>{
      'icon_source': iconSource.id,
    };
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
    );
  }
}
