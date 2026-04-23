import 'dart:async';

import 'package:flutter/material.dart';

import '../../l10n/app_strings.dart';
import '../../models/app_models.dart';
import '../../platform/livebridge_platform.dart';
import '../../theme/livebridge_tokens.dart';
import '../../utils/livebridge_haptics.dart';
import '../../widgets/redesign/lb_detail_screen.dart';
import '../../widgets/redesign/lb_icon.dart';
import '../../widgets/redesign/lb_list_component.dart';
import '../../widgets/redesign/lb_toast.dart';

class SettingsPermissionsScreen extends StatefulWidget {
  const SettingsPermissionsScreen({super.key});

  @override
  State<SettingsPermissionsScreen> createState() =>
      _SettingsPermissionsScreenState();
}

class _SettingsPermissionsScreenState extends State<SettingsPermissionsScreen>
    with WidgetsBindingObserver {
  bool _listenerEnabled = false;
  bool _notificationsGranted = false;
  bool _canPostPromoted = false;
  bool _hidePromotedAccess = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      unawaited(_loadState());
    });
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      unawaited(_loadState());
    }
  }

  void _snack(String value) {
    if (!mounted) {
      return;
    }
    showLbToast(context, message: value);
  }

  Future<void> _loadState() async {
    try {
      final bool listenerEnabled =
          await LiveBridgePlatform.isNotificationListenerEnabled();
      final bool notificationsGranted =
          await LiveBridgePlatform.isNotificationPermissionGranted();
      final bool canPostPromoted =
          await LiveBridgePlatform.canPostPromotedNotifications();
      final DeviceInfo deviceInfo = await LiveBridgePlatform.getDeviceInfo();

      if (!mounted) {
        return;
      }

      setState(() {
        _listenerEnabled = listenerEnabled;
        _notificationsGranted = notificationsGranted;
        _canPostPromoted = canPostPromoted;
        _hidePromotedAccess = deviceInfo.shouldHideLiveUpdatesPromotion;
      });
    } catch (_) {}
  }

  Future<void> _requestNotificationPermission() async {
    unawaited(LiveBridgeHaptics.confirm());
    final bool granted =
        await LiveBridgePlatform.requestNotificationPermission();
    if (!mounted) {
      return;
    }
    final AppStrings strings = AppStrings.of(context);
    _snack(granted ? strings.permissionGranted : strings.permissionDenied);
    await _loadState();
  }

  Future<void> _openListenerSettings() async {
    unawaited(LiveBridgeHaptics.openSurface());
    final bool opened =
        await LiveBridgePlatform.openNotificationListenerSettings();
    if (!mounted || opened) {
      return;
    }
    _snack(AppStrings.of(context).listenerUnavailable);
  }

  Future<void> _openAppNotificationSettings() async {
    unawaited(LiveBridgeHaptics.openSurface());
    final bool opened = await LiveBridgePlatform.openAppNotificationSettings();
    if (!mounted || opened) {
      return;
    }
    _snack(AppStrings.of(context).notificationsUnavailable);
  }

  Future<void> _openPromotedSettings() async {
    unawaited(LiveBridgeHaptics.openSurface());
    final bool opened =
        await LiveBridgePlatform.openPromotedNotificationSettings();
    if (!mounted || opened) {
      return;
    }
    _snack(AppStrings.of(context).liveUpdatesUnavailable);
  }

  LbListItemData _buildPermissionItem({
    required String title,
    required bool enabled,
    required VoidCallback onTap,
  }) {
    return LbListItemData(
      title: title,
      trailingIcon: enabled ? null : LbIconSymbol.alertOctagonFilled,
      trailingIconColor: enabled ? null : LbPalette.of(context).warning,
      onTap: onTap,
    );
  }

  @override
  Widget build(BuildContext context) {
    final AppStrings strings = AppStrings.of(context);

    final List<LbListItemData> permissionItems = <LbListItemData>[
      _buildPermissionItem(
        title: strings.listenerAccess,
        enabled: _listenerEnabled,
        onTap: () {
          unawaited(_openListenerSettings());
        },
      ),
      _buildPermissionItem(
        title: strings.postNotifications,
        enabled: _notificationsGranted,
        onTap: () {
          if (_notificationsGranted) {
            unawaited(_openAppNotificationSettings());
          } else {
            unawaited(_requestNotificationPermission());
          }
        },
      ),
      if (!_hidePromotedAccess)
        _buildPermissionItem(
          title: strings.liveUpdatesAccess,
          enabled: _canPostPromoted,
          onTap: () {
            unawaited(_openPromotedSettings());
          },
        ),
    ];

    return LbDetailScreen(
      title: strings.accessTitle,
      children: <Widget>[
        LbListComponent(
          items: permissionItems,
          rowHeight: LbSpacing.recentRowHeight,
          extendDividersToEnd: true,
        ),
      ],
    );
  }
}
