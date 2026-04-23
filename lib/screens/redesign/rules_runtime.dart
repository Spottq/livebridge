import 'package:flutter/material.dart';

import '../../l10n/app_strings.dart';
import '../../models/app_models.dart';
import '../../platform/livebridge_platform.dart';
import '../../utils/livebridge_haptics.dart';
import '../../widgets/redesign/lb_toast.dart';

enum LbRulesConversionMode { allApps, onlySelected, excludeSelected }

const Duration lbAppsLoadingMinimumDuration = Duration(milliseconds: 500);

String lbNormalizePackageName(String value) => value.trim().toLowerCase();

Set<String> lbParsePackageRules(String value) {
  return value
      .split(RegExp(r'[,\n;\s]+'))
      .map(lbNormalizePackageName)
      .where((String item) => item.isNotEmpty)
      .toSet();
}

String lbEncodePackageRules(Iterable<String> packageNames) {
  final List<String> values = packageNames
      .map(lbNormalizePackageName)
      .where((String item) => item.isNotEmpty)
      .toSet()
      .toList()
    ..sort();
  return values.join('\n');
}

LbRulesConversionMode lbConversionModeFromPackageMode(PackageMode mode) {
  switch (mode) {
    case PackageMode.all:
      return LbRulesConversionMode.allApps;
    case PackageMode.include:
      return LbRulesConversionMode.onlySelected;
    case PackageMode.exclude:
      return LbRulesConversionMode.excludeSelected;
  }
}

PackageMode lbPackageModeFromConversionMode(LbRulesConversionMode mode) {
  switch (mode) {
    case LbRulesConversionMode.allApps:
      return PackageMode.all;
    case LbRulesConversionMode.onlySelected:
      return PackageMode.include;
    case LbRulesConversionMode.excludeSelected:
      return PackageMode.exclude;
  }
}

List<InstalledApp> lbSortInstalledApps(Iterable<InstalledApp> apps) {
  final List<InstalledApp> sorted = apps.toList();
  sorted.sort((InstalledApp left, InstalledApp right) {
    final int labelCompare = left.label.toLowerCase().compareTo(
      right.label.toLowerCase(),
    );
    if (labelCompare != 0) {
      return labelCompare;
    }
    return left.packageName.toLowerCase().compareTo(
      right.packageName.toLowerCase(),
    );
  });
  return sorted;
}

List<InstalledApp> lbBuildVisibleApps({
  required List<InstalledApp> apps,
  Set<String> selectedPackages = const <String>{},
  required bool showSystemApps,
  String searchQuery = '',
}) {
  final String normalizedQuery = searchQuery.trim().toLowerCase();
  final List<InstalledApp> selectedApps = <InstalledApp>[];
  final List<InstalledApp> regularApps = <InstalledApp>[];

  for (final InstalledApp app in lbSortInstalledApps(apps)) {
    final bool selected = selectedPackages.contains(
      lbNormalizePackageName(app.packageName),
    );
    if (!showSystemApps && app.isSystem && !selected) {
      continue;
    }
    if (normalizedQuery.isNotEmpty &&
        !app.label.toLowerCase().contains(normalizedQuery) &&
        !app.packageName.toLowerCase().contains(normalizedQuery)) {
      continue;
    }
    if (selected) {
      selectedApps.add(app);
    } else {
      regularApps.add(app);
    }
  }

  return <InstalledApp>[...selectedApps, ...regularApps];
}

Future<void> lbWaitForMinimumAppsLoading({
  required DateTime startedAt,
  Duration minimum = lbAppsLoadingMinimumDuration,
}) async {
  final Duration elapsed = DateTime.now().difference(startedAt);
  if (elapsed >= minimum) {
    return;
  }
  await Future<void>.delayed(minimum - elapsed);
}

Future<bool> lbEnsureAppListAccess(BuildContext context) async {
  final bool alreadyGranted = await LiveBridgePlatform.getAppListAccessGranted();
  if (alreadyGranted) {
    return true;
  }
  if (!context.mounted) {
    return false;
  }

  final AppStrings strings = AppStrings.of(context);
  final bool granted =
          await showDialog<bool>(
            context: context,
            builder: (BuildContext dialogContext) {
              return AlertDialog(
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(24),
                ),
                title: Text(strings.appsAccessTitle),
                content: Text(strings.appsAccessMessage),
                actions: <Widget>[
                  TextButton(
                    onPressed: () {
                      LiveBridgeHaptics.selection();
                      Navigator.of(dialogContext).pop(false);
                    },
                    child: Text(strings.cancel),
                  ),
                  FilledButton(
                    onPressed: () {
                      LiveBridgeHaptics.confirm();
                      Navigator.of(dialogContext).pop(true);
                    },
                    child: Text(strings.allow),
                  ),
                ],
              );
            },
          ) ??
      false;

  if (!granted) {
    return false;
  }

  final bool saved = await LiveBridgePlatform.setAppListAccessGranted(true);
  if (!saved && context.mounted) {
    showLbToast(context, message: strings.appsAccessSaveFailed);
  }
  return true;
}
