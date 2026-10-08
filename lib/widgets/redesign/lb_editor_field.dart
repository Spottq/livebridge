import 'package:flutter/material.dart';

import '../../theme/livebridge_tokens.dart';

/// Multiline editor using the same surfaces and typography as settings cards.
class LbEditorField extends StatelessWidget {
  const LbEditorField({
    super.key,
    required this.fieldKey,
    required this.label,
    required this.controller,
    this.enabled = true,
    this.minLines = 2,
    this.maxLines = 5,
    this.maxLength,
  });

  final Key fieldKey;
  final String label;
  final TextEditingController controller;
  final bool enabled;
  final int minLines;
  final int maxLines;
  final int? maxLength;

  @override
  Widget build(BuildContext context) {
    final palette = LbPalette.of(context);
    return Padding(
      padding: const EdgeInsets.only(bottom: LbSpacing.md),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: LbTextStyles.body.copyWith(color: palette.textSecondary),
          ),
          const SizedBox(height: LbSpacing.sm),
          DecoratedBox(
            decoration: BoxDecoration(
              color: palette.surface,
              borderRadius: BorderRadius.circular(LbRadius.card),
            ),
            child: TextField(
              key: fieldKey,
              controller: controller,
              enabled: enabled,
              minLines: minLines,
              maxLines: maxLines,
              maxLength: maxLength,
              cursorColor: palette.accent,
              style: LbTextStyles.body.copyWith(color: palette.textPrimary),
              decoration: const InputDecoration(
                border: InputBorder.none,
                enabledBorder: InputBorder.none,
                focusedBorder: InputBorder.none,
                disabledBorder: InputBorder.none,
                filled: false,
                contentPadding: EdgeInsets.all(LbSpacing.md),
              ),
              buildCounter: maxLength == null
                  ? null
                  : (
                      _, {
                      required currentLength,
                      required isFocused,
                      required maxLength,
                    }) => Text(
                      '$currentLength / $maxLength',
                      style: LbTextStyles.caption.copyWith(
                        color: palette.textSecondary,
                      ),
                    ),
            ),
          ),
        ],
      ),
    );
  }
}

class LbEditorSaveBar extends StatelessWidget {
  const LbEditorSaveBar({
    super.key,
    required this.label,
    required this.onPressed,
  });
  final String label;
  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) {
    final palette = LbPalette.of(context);
    return Align(
      alignment: Alignment.bottomCenter,
      child: SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.all(LbSpacing.screenHorizontal),
          child: Semantics(
            button: true,
            enabled: onPressed != null,
            child: Material(
              color: onPressed == null ? palette.surfaceSoft : palette.accent,
              shape: const StadiumBorder(),
              clipBehavior: Clip.antiAlias,
              child: InkWell(
                onTap: onPressed,
                child: ConstrainedBox(
                  constraints: const BoxConstraints(minHeight: 52),
                  child: SizedBox(
                    width: double.infinity,
                    child: Padding(
                      padding: const EdgeInsets.symmetric(
                        horizontal: LbSpacing.md,
                        vertical: LbSpacing.md,
                      ),
                      child: Text(
                        label,
                        textAlign: TextAlign.center,
                        style: LbTextStyles.title.copyWith(
                          color: onPressed == null
                              ? palette.textMuted
                              : LbPalette.light.textPrimary,
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
