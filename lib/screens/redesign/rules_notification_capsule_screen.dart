import 'dart:async';

import 'package:flutter/material.dart';

import '../../l10n/app_strings.dart';
import '../../models/app_models.dart';
import '../../platform/livebridge_platform.dart';
import '../../theme/livebridge_tokens.dart';
import '../../utils/livebridge_haptics.dart';
import '../../widgets/redesign/lb_apps_loading_state.dart';
import '../../widgets/redesign/lb_detail_screen.dart';
import '../../widgets/redesign/lb_installed_app_avatar.dart';
import '../../widgets/redesign/lb_list_component.dart';
import '../../widgets/redesign/lb_search_pill.dart';
import '../../widgets/redesign/lb_selection_indicator.dart';
import '../../widgets/redesign/lb_toast.dart';
import 'rules_runtime.dart';

class RulesNotificationCapsuleScreen extends StatefulWidget {
  const RulesNotificationCapsuleScreen({super.key});

  @override
  State<RulesNotificationCapsuleScreen> createState() =>
      _RulesNotificationCapsuleScreenState();
}

class _RulesNotificationCapsuleScreenState
    extends State<RulesNotificationCapsuleScreen> {
  bool _capsuleEnabled = false;
  bool _smartCapsuleEnabled = false;
  bool _clearActionEnabled = false;
  bool _showSystemApps = false;
  bool _isSearchExpanded = false;
  bool _isAppsLoading = true;
  String _searchQuery = '';
  NotificationCapsuleMode _capsuleMode = NotificationCapsuleMode.general;
  Set<String> _excludedPackages = <String>{};
  List<InstalledApp> _apps = const <InstalledApp>[];
  int _appsLoadRequestId = 0;
  late final TextEditingController _searchController;
  late final FocusNode _searchFocusNode;

  @override
  void initState() {
    super.initState();
    _searchController = TextEditingController()
      ..addListener(() {
        final String nextQuery = _searchController.text;
        if (nextQuery == _searchQuery || !mounted) {
          return;
        }
        setState(() => _searchQuery = nextQuery);
      });
    _searchFocusNode = FocusNode();
    unawaited(_loadPrimaryState());
    WidgetsBinding.instance.addPostFrameCallback((_) {
      unawaited(_loadApps());
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    _searchFocusNode.dispose();
    super.dispose();
  }

  void _snack(String value) {
    if (!mounted) {
      return;
    }
    showLbToast(context, message: value);
  }

  Future<void> _loadPrimaryState() async {
    try {
      final Future<bool> capsuleEnabledFuture =
          LiveBridgePlatform.getSmartNotificationCapsuleEnabled();
      final Future<bool> smartCapsuleEnabledFuture =
          LiveBridgePlatform.getNotificationCapsuleSmartEnabled();
      final Future<String> capsuleModeFuture =
          LiveBridgePlatform.getNotificationCapsuleMode();
      final Future<bool> clearActionEnabledFuture =
          LiveBridgePlatform.getNotificationCapsuleClearActionEnabled();
      final Future<String> excludedRulesFuture =
          LiveBridgePlatform.getNotificationCapsuleExcludedPackageRules();

      final bool capsuleEnabled = await capsuleEnabledFuture;
      final bool smartCapsuleEnabled = await smartCapsuleEnabledFuture;
      final NotificationCapsuleMode capsuleMode =
          NotificationCapsuleModeId.from(await capsuleModeFuture);
      final bool clearActionEnabled = await clearActionEnabledFuture;
      final Set<String> excludedPackages = lbParsePackageRules(
        await excludedRulesFuture,
      );

      if (!mounted) {
        return;
      }

      setState(() {
        _capsuleEnabled = capsuleEnabled;
        _smartCapsuleEnabled = smartCapsuleEnabled;
        _capsuleMode = capsuleMode;
        _clearActionEnabled = clearActionEnabled;
        _excludedPackages = excludedPackages;
      });
    } catch (_) {}
  }

  Future<void> _loadApps() async {
    final int requestId = ++_appsLoadRequestId;
    final DateTime startedAt = DateTime.now();
    if (mounted) {
      setState(() => _isAppsLoading = true);
    }

    try {
      List<InstalledApp> apps = const <InstalledApp>[];
      if (mounted && await lbEnsureAppListAccess(context)) {
        apps = lbSortInstalledApps(await LiveBridgePlatform.getInstalledApps());
      }

      if (!mounted || requestId != _appsLoadRequestId) {
        return;
      }

      await lbWaitForMinimumAppsLoading(startedAt: startedAt);

      if (!mounted || requestId != _appsLoadRequestId) {
        return;
      }

      setState(() {
        _apps = apps;
        _isAppsLoading = false;
      });
    } catch (_) {
      if (!mounted || requestId != _appsLoadRequestId) {
        return;
      }
      await lbWaitForMinimumAppsLoading(startedAt: startedAt);
      if (!mounted || requestId != _appsLoadRequestId) {
        return;
      }
      setState(() => _isAppsLoading = false);
      _snack(AppStrings.of(context).appsLoadFailed);
    }
  }

  Future<void> _setCapsuleEnabled(bool value) async {
    if (value == _capsuleEnabled) {
      return;
    }
    setState(() => _capsuleEnabled = value);
    await LiveBridgePlatform.setSmartNotificationCapsuleEnabled(value);
  }

  Future<void> _setSmartCapsuleEnabled(bool value) async {
    if (value == _smartCapsuleEnabled) {
      return;
    }
    setState(() => _smartCapsuleEnabled = value);
    await LiveBridgePlatform.setNotificationCapsuleSmartEnabled(value);
  }

  Future<void> _setCapsuleMode(NotificationCapsuleMode value) async {
    if (value == _capsuleMode) {
      return;
    }
    setState(() => _capsuleMode = value);
    await LiveBridgePlatform.setNotificationCapsuleMode(value.id);
  }

  Future<void> _setClearActionEnabled(bool value) async {
    if (value == _clearActionEnabled) {
      return;
    }
    setState(() => _clearActionEnabled = value);
    await LiveBridgePlatform.setNotificationCapsuleClearActionEnabled(value);
  }

  Future<void> _setAppExcluded(String packageName, bool value) async {
    final String normalized = lbNormalizePackageName(packageName);
    final Set<String> next = Set<String>.from(_excludedPackages);
    if (value) {
      next.add(normalized);
    } else {
      next.remove(normalized);
    }
    setState(() => _excludedPackages = next);
    await LiveBridgePlatform.setNotificationCapsuleExcludedPackageRules(
      lbEncodePackageRules(next),
    );
  }

  void _toggleSystemApps() {
    unawaited(LiveBridgeHaptics.selection());
    setState(() => _showSystemApps = !_showSystemApps);
  }

  void _openSearch() {
    if (_isSearchExpanded) {
      return;
    }
    unawaited(LiveBridgeHaptics.selection());
    setState(() => _isSearchExpanded = true);
    Future<void>.delayed(const Duration(milliseconds: 250), () {
      if (!mounted || !_isSearchExpanded) {
        return;
      }
      _searchFocusNode.requestFocus();
    });
  }

  void _closeSearch() {
    if (!_isSearchExpanded && _searchQuery.isEmpty) {
      return;
    }
    unawaited(LiveBridgeHaptics.selection());
    _searchFocusNode.unfocus();
    _searchController.clear();
    setState(() {
      _isSearchExpanded = false;
      _searchQuery = '';
    });
  }

  List<LbListItemData> _buildAppItems() {
    final List<InstalledApp> visibleApps = lbBuildVisibleApps(
      apps: _apps,
      selectedPackages: _excludedPackages,
      showSystemApps: _showSystemApps,
      searchQuery: _searchQuery,
    );

    return visibleApps.map((InstalledApp app) {
      final bool isExcluded = _excludedPackages.contains(
        lbNormalizePackageName(app.packageName),
      );
      return LbListItemData(
        title: app.label,
        leadingChild: LbInstalledAppAvatar(
          app: app,
          size: LbSpacing.recentAvatarSize,
        ),
        showChevron: false,
        toggleValue: isExcluded,
        onToggle: (bool value) {
          unawaited(_setAppExcluded(app.packageName, value));
        },
        onTap: () {
          final bool nextValue = !isExcluded;
          unawaited(LiveBridgeHaptics.toggle(nextValue));
          unawaited(_setAppExcluded(app.packageName, nextValue));
        },
      );
    }).toList();
  }

  Widget _buildAppsSection(LbPalette palette) {
    if (_isAppsLoading) {
      return const LbAppsLoadingState();
    }
    if (_apps.isEmpty) {
      return Padding(
        padding: const EdgeInsets.only(top: LbSpacing.lg),
        child: Text(
          AppStrings.of(context).appsLoadFailed,
          style: LbTextStyles.body.copyWith(color: palette.textSecondary),
        ),
      );
    }
    return LbListComponent(
      key: ValueKey<Object>((_showSystemApps, _searchQuery)),
      items: _buildAppItems(),
      rowHeight: LbSpacing.recentRowHeight,
      leadingSize: LbSpacing.recentAvatarSize,
      leadingGap: LbSpacing.recentAvatarGap,
      extendDividersToEnd: true,
    );
  }

  @override
  Widget build(BuildContext context) {
    final AppStrings strings = AppStrings.of(context);
    final LbPalette palette = LbPalette.of(context);
    final bool showModePicker = !_smartCapsuleEnabled;

    final List<LbListItemData> settingsItems = <LbListItemData>[
      LbListItemData(
        title: strings.smartNotificationCapsuleTitle,
        description: strings.smartNotificationCapsuleSubtitle,
        showChevron: false,
        toggleValue: _capsuleEnabled,
        onToggle: (bool value) {
          unawaited(_setCapsuleEnabled(value));
        },
        onTap: () {
          final bool nextValue = !_capsuleEnabled;
          unawaited(LiveBridgeHaptics.toggle(nextValue));
          unawaited(_setCapsuleEnabled(nextValue));
        },
      ),
      LbListItemData(
        title: strings.notificationCapsuleSmartTitle,
        description: strings.notificationCapsuleSmartDescription,
        showChevron: false,
        enabled: _capsuleEnabled,
        toggleValue: _smartCapsuleEnabled,
        onToggle: _capsuleEnabled
            ? (bool value) {
                unawaited(_setSmartCapsuleEnabled(value));
              }
            : null,
        onTap: _capsuleEnabled
            ? () {
                final bool nextValue = !_smartCapsuleEnabled;
                unawaited(LiveBridgeHaptics.toggle(nextValue));
                unawaited(_setSmartCapsuleEnabled(nextValue));
              }
            : null,
      ),
      LbListItemData(
        title: strings.notificationCapsuleClearActionTitle,
        description: strings.notificationCapsuleClearActionDescription,
        showChevron: false,
        enabled: _capsuleEnabled,
        toggleValue: _clearActionEnabled,
        onToggle: _capsuleEnabled
            ? (bool value) {
                unawaited(_setClearActionEnabled(value));
              }
            : null,
        onTap: _capsuleEnabled
            ? () {
                final bool nextValue = !_clearActionEnabled;
                unawaited(LiveBridgeHaptics.toggle(nextValue));
                unawaited(_setClearActionEnabled(nextValue));
              }
            : null,
      ),
    ];

    final List<LbListItemData> modeItems = <LbListItemData>[
      LbListItemData(
        title: strings.notificationCapsuleModeGeneralTitle,
        description: strings.notificationCapsuleModeGeneralDescription,
        showChevron: false,
        enabled: _capsuleEnabled,
        trailingWidget: LbSelectionIndicator(
          selected: _capsuleMode == NotificationCapsuleMode.general,
        ),
        trailingWidgetWidth: LbSpacing.listAccessoryWidth,
        onTap: _capsuleEnabled
            ? () {
                unawaited(LiveBridgeHaptics.selection());
                unawaited(_setCapsuleMode(NotificationCapsuleMode.general));
              }
            : null,
      ),
      LbListItemData(
        title: strings.notificationCapsuleModePerAppTitle,
        description: strings.notificationCapsuleModePerAppDescription,
        showChevron: false,
        enabled: _capsuleEnabled,
        trailingWidget: LbSelectionIndicator(
          selected: _capsuleMode == NotificationCapsuleMode.perApp,
        ),
        trailingWidgetWidth: LbSpacing.listAccessoryWidth,
        onTap: _capsuleEnabled
            ? () {
                unawaited(LiveBridgeHaptics.selection());
                unawaited(_setCapsuleMode(NotificationCapsuleMode.perApp));
              }
            : null,
      ),
    ];

    return LbDetailScreen(
      title: strings.smartNotificationCapsuleTitle,
      floatingBottom: _isAppsLoading
          ? null
          : LbSearchPill(
              expanded: _isSearchExpanded,
              controller: _searchController,
              focusNode: _searchFocusNode,
              onOpen: _openSearch,
              onClose: _closeSearch,
            ),
      floatingBottomReservedHeight: _isAppsLoading
          ? 0
          : LbSpacing.searchPillReservedHeight,
      children: <Widget>[
        LbListComponent(
          items: settingsItems,
          rowHeight: LbSpacing.recentRowHeight,
          extendDividersToEnd: true,
        ),
        if (showModePicker) ...<Widget>[
          const SizedBox(height: LbSpacing.detailSectionGap),
          Text(
            strings.notificationCapsuleDisplayModeTitle,
            style: LbTextStyles.title.copyWith(color: palette.textSecondary),
          ),
          const SizedBox(height: LbSpacing.recentSectionGap),
          LbListComponent(
            items: modeItems,
            rowHeight: LbSpacing.recentRowHeight,
            extendDividersToEnd: true,
          ),
        ],
        const SizedBox(height: LbSpacing.detailSectionGap),
        Row(
          children: <Widget>[
            Expanded(
              child: Text(
                strings.notificationCapsuleExcludedAppsTitle,
                style: LbTextStyles.title.copyWith(
                  color: palette.textSecondary,
                ),
              ),
            ),
            GestureDetector(
              onTap: _toggleSystemApps,
              behavior: HitTestBehavior.opaque,
              child: Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: LbSpacing.xs,
                  vertical: LbSpacing.xs,
                ),
                child: Text(
                  _showSystemApps ? strings.hideSystem : strings.showSystem,
                  style: LbTextStyles.body.copyWith(color: palette.link),
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: LbSpacing.recentSectionGap),
        AnimatedSwitcher(
          duration: const Duration(milliseconds: 220),
          switchInCurve: Curves.easeOutCubic,
          switchOutCurve: Curves.easeInCubic,
          layoutBuilder: (Widget? currentChild, List<Widget> previousChildren) {
            return Stack(
              alignment: Alignment.topCenter,
              children: <Widget>[
                ...previousChildren,
                if (currentChild != null) currentChild,
              ],
            );
          },
          child: _buildAppsSection(palette),
        ),
      ],
    );
  }
}
