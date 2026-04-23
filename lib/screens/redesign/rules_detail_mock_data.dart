import 'package:flutter/material.dart';

enum LbRulesConversionMode {
  allApps,
  onlySelected,
  excludeSelected,
}

class LbRulesScopeAppItem {
  const LbRulesScopeAppItem({
    required this.id,
    required this.name,
    required this.avatarLabel,
    required this.avatarBackground,
    required this.avatarForeground,
    required this.enabled,
  });

  final String id;
  final String name;
  final String avatarLabel;
  final Color avatarBackground;
  final Color avatarForeground;
  final bool enabled;
}

const List<LbRulesScopeAppItem> lbRulesPrimaryApps = <LbRulesScopeAppItem>[
  LbRulesScopeAppItem(
    id: '2gis',
    name: '2GIS',
    avatarLabel: '2',
    avatarBackground: Color(0xFFFFC12D),
    avatarForeground: Color(0xFF1A6BFF),
    enabled: true,
  ),
  LbRulesScopeAppItem(
    id: 'hiddify',
    name: 'Hiddify',
    avatarLabel: 'H',
    avatarBackground: Color(0xFFE8ECFF),
    avatarForeground: Color(0xFF5B6DE1),
    enabled: true,
  ),
  LbRulesScopeAppItem(
    id: 'alfa',
    name: 'Alfa-Bank',
    avatarLabel: 'A',
    avatarBackground: Color(0xFFFF3B30),
    avatarForeground: Color(0xFFFFFFFF),
    enabled: true,
  ),
  LbRulesScopeAppItem(
    id: 'azbuka',
    name: 'Azbuka vkusa',
    avatarLabel: 'A',
    avatarBackground: Color(0xFF1C4C2E),
    avatarForeground: Color(0xFF69D63D),
    enabled: true,
  ),
  LbRulesScopeAppItem(
    id: 'messages',
    name: 'Messages',
    avatarLabel: 'M',
    avatarBackground: Color(0xFF35D96B),
    avatarForeground: Color(0xFFFFFFFF),
    enabled: false,
  ),
  LbRulesScopeAppItem(
    id: 'wechat',
    name: 'WeChat',
    avatarLabel: 'W',
    avatarBackground: Color(0xFF45D500),
    avatarForeground: Color(0xFFFFFFFF),
    enabled: false,
  ),
];

const List<LbRulesScopeAppItem> lbRulesSystemApps = <LbRulesScopeAppItem>[
  LbRulesScopeAppItem(
    id: 'phone',
    name: 'Phone',
    avatarLabel: 'P',
    avatarBackground: Color(0xFF6CA7FF),
    avatarForeground: Color(0xFFFFFFFF),
    enabled: false,
  ),
  LbRulesScopeAppItem(
    id: 'system',
    name: 'Android System',
    avatarLabel: 'S',
    avatarBackground: Color(0xFF70817D),
    avatarForeground: Color(0xFFFFFFFF),
    enabled: false,
  ),
];
