class PromotedNotificationAccess {
  const PromotedNotificationAccess({
    required this.status,
    this.apiAvailable = false,
    this.settingsAvailable = false,
  });

  factory PromotedNotificationAccess.fromMap(Map<Object?, Object?>? value) {
    return PromotedNotificationAccess(
      status: value?['status'] as String? ?? 'unknown',
      apiAvailable: value?['apiAvailable'] == true,
      settingsAvailable: value?['settingsAvailable'] == true,
    );
  }

  final String status;
  final bool apiAvailable;
  final bool settingsAvailable;
  bool get granted => status == 'granted';
  bool get needsPermission => status == 'denied';
  Map<String, Object> toMap() => {
    'status': status,
    'api_available': apiAvailable,
    'settings_available': settingsAvailable,
  };
}
