import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'redesign/lb_slider.dart';

class NotificationColorSwatch extends StatelessWidget {
  const NotificationColorSwatch({
    super.key,
    required this.colorArgb,
    this.size = 28,
    this.selected = false,
  });

  final int colorArgb;
  final double size;
  final bool selected;

  @override
  Widget build(BuildContext context) {
    final Color color = Color(_opaqueArgb(colorArgb));

    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(color: color, shape: BoxShape.circle),
    );
  }
}

class NotificationColorPickerSheet extends StatefulWidget {
  const NotificationColorPickerSheet({
    super.key,
    required this.title,
    required this.doneLabel,
    required this.initialColorArgb,
  });

  final String title;
  final String doneLabel;
  final int initialColorArgb;

  @override
  State<NotificationColorPickerSheet> createState() =>
      _NotificationColorPickerSheetState();
}

class _NotificationColorPickerSheetState
    extends State<NotificationColorPickerSheet> {
  late final TextEditingController _hexController;
  late final FocusNode _hexFocusNode;
  late HSVColor _hsv = HSVColor.fromColor(
    Color(_opaqueArgb(widget.initialColorArgb)),
  );

  int get _selectedArgb => _opaqueArgb(_hsv.toColor().toARGB32());

  @override
  void initState() {
    super.initState();
    _hexController = TextEditingController(text: _formatColor(_selectedArgb));
    _hexFocusNode = FocusNode()..addListener(_handleHexFocusChanged);
  }

  @override
  void dispose() {
    _hexFocusNode
      ..removeListener(_handleHexFocusChanged)
      ..dispose();
    _hexController.dispose();
    super.dispose();
  }

  void _setFromHsv(HSVColor value) {
    setState(() => _hsv = value);
    _setHexText(_formatColor(_opaqueArgb(value.toColor().toARGB32())));
  }

  void _handleHexFocusChanged() {
    if (!_hexFocusNode.hasFocus) {
      _normalizeHexText();
    }
  }

  void _handleHexChanged(String value) {
    final int? color = _parseColorInput(value);
    if (color == null) {
      return;
    }
    setState(() => _hsv = HSVColor.fromColor(Color(color)));
  }

  void _normalizeHexText() {
    final int color = _parseColorInput(_hexController.text) ?? _selectedArgb;
    _setHexText(_formatColor(color));
    setState(() => _hsv = HSVColor.fromColor(Color(color)));
  }

  void _setHexText(String value) {
    if (_hexController.text == value) {
      return;
    }
    _hexController.value = TextEditingValue(
      text: value,
      selection: TextSelection.collapsed(offset: value.length),
    );
  }

  @override
  Widget build(BuildContext context) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;
    final MaterialLocalizations material = MaterialLocalizations.of(context);
    final Color selectedColor = Color(_selectedArgb);

    return SingleChildScrollView(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            children: <Widget>[
              NotificationColorSwatch(colorArgb: _selectedArgb, size: 40),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: <Widget>[
                    Text(
                      widget.title,
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                    const SizedBox(height: 2),
                    SizedBox(
                      height: 22,
                      child: TextField(
                        controller: _hexController,
                        focusNode: _hexFocusNode,
                        keyboardType: TextInputType.text,
                        textCapitalization: TextCapitalization.characters,
                        inputFormatters: <TextInputFormatter>[
                          const _HexColorInputFormatter(),
                        ],
                        style: Theme.of(context).textTheme.bodySmall?.copyWith(
                          color: colorScheme.onSurfaceVariant,
                          fontFeatures: const <FontFeature>[
                            FontFeature.tabularFigures(),
                          ],
                        ),
                        cursorColor: colorScheme.primary,
                        decoration: const InputDecoration(
                          isDense: true,
                          border: InputBorder.none,
                          contentPadding: EdgeInsets.zero,
                          counterText: '',
                        ),
                        onChanged: _handleHexChanged,
                        onSubmitted: (_) => _normalizeHexText(),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          _HueValuePalette(hsv: _hsv, onChanged: _setFromHsv),
          const SizedBox(height: 14),
          LbSlider(
            value: _hsv.hue,
            min: 0,
            max: 360,
            activeTrackColor: selectedColor,
            inactiveTrackColor: selectedColor.withValues(alpha: 0.34),
            onChanged: (double hue) {
              _setFromHsv(_hsv.withHue(hue == 360 ? 0 : hue));
            },
          ),
          const SizedBox(height: 24),
          Row(
            children: <Widget>[
              TextButton(
                onPressed: () => Navigator.of(context).maybePop(),
                child: Text(material.cancelButtonLabel),
              ),
              const Spacer(),
              FilledButton(
                onPressed: () => Navigator.of(context).pop(_selectedArgb),
                child: Text(widget.doneLabel),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _HueValuePalette extends StatelessWidget {
  const _HueValuePalette({required this.hsv, required this.onChanged});

  final HSVColor hsv;
  final ValueChanged<HSVColor> onChanged;

  void _handlePoint(Offset localPosition, Size size) {
    final double saturation = (localPosition.dx / size.width).clamp(0.0, 1.0);
    final double value = (1 - localPosition.dy / size.height).clamp(0.0, 1.0);
    onChanged(hsv.withSaturation(saturation).withValue(value));
  }

  @override
  Widget build(BuildContext context) {
    return AspectRatio(
      aspectRatio: 1.7,
      child: LayoutBuilder(
        builder: (BuildContext context, BoxConstraints constraints) {
          final Size size = constraints.biggest;
          return GestureDetector(
            onPanDown: (DragDownDetails details) {
              _handlePoint(details.localPosition, size);
            },
            onPanUpdate: (DragUpdateDetails details) {
              _handlePoint(details.localPosition, size);
            },
            child: CustomPaint(
              painter: _HueValuePalettePainter(hsv),
              child: const SizedBox.expand(),
            ),
          );
        },
      ),
    );
  }
}

class _HueValuePalettePainter extends CustomPainter {
  const _HueValuePalettePainter(this.hsv);

  final HSVColor hsv;

  @override
  void paint(Canvas canvas, Size size) {
    final Rect rect = Offset.zero & size;
    final RRect rounded = RRect.fromRectAndRadius(
      rect,
      const Radius.circular(16),
    );
    final Color hueColor = HSVColor.fromAHSV(1, hsv.hue, 1, 1).toColor();

    canvas.save();
    canvas.clipRRect(rounded);
    canvas.drawRect(
      rect,
      Paint()
        ..shader = LinearGradient(
          colors: <Color>[Colors.white, hueColor],
        ).createShader(rect),
    );
    canvas.drawRect(
      rect,
      Paint()
        ..shader = const LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: <Color>[Colors.transparent, Colors.black],
        ).createShader(rect),
    );
    canvas.restore();

    final Offset thumb = Offset(
      hsv.saturation * size.width,
      (1 - hsv.value) * size.height,
    );
    final double radius = math.min(size.shortestSide * 0.055, 12);
    canvas.drawCircle(
      thumb,
      radius + 2,
      Paint()
        ..color = Colors.black.withValues(alpha: 0.28)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 3,
    );
    canvas.drawCircle(
      thumb,
      radius,
      Paint()
        ..color = Colors.white
        ..style = PaintingStyle.stroke
        ..strokeWidth = 3,
    );
  }

  @override
  bool shouldRepaint(covariant _HueValuePalettePainter oldDelegate) {
    return oldDelegate.hsv != hsv;
  }
}

int _opaqueArgb(int colorArgb) {
  return 0xFF000000 | (colorArgb & 0x00FFFFFF);
}

String _formatColor(int colorArgb) {
  final int rgb = colorArgb & 0x00FFFFFF;
  return '#${rgb.toRadixString(16).padLeft(6, '0').toUpperCase()}';
}

int? _parseColorInput(String raw) {
  String value = raw.trim();
  if (value.startsWith('#')) {
    value = value.substring(1);
  }
  if (value.length != 6 || !RegExp(r'^[0-9a-fA-F]{6}$').hasMatch(value)) {
    return null;
  }
  return _opaqueArgb(int.parse(value, radix: 16));
}

class _HexColorInputFormatter extends TextInputFormatter {
  const _HexColorInputFormatter();

  static final RegExp _hexPattern = RegExp(r'[0-9a-fA-F]');

  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    final StringBuffer digits = StringBuffer();
    for (final int codeUnit in newValue.text.codeUnits) {
      final String character = String.fromCharCode(codeUnit);
      if (_hexPattern.hasMatch(character)) {
        digits.write(character.toUpperCase());
        if (digits.length >= 6) {
          break;
        }
      }
    }

    final String text = '#$digits';
    final int rawCursor = newValue.selection.baseOffset.clamp(
      0,
      newValue.text.length,
    );
    int cursorDigits = 0;
    for (final int codeUnit
        in newValue.text.substring(0, rawCursor).codeUnits) {
      if (_hexPattern.hasMatch(String.fromCharCode(codeUnit))) {
        cursorDigits += 1;
      }
    }
    final int cursor = (1 + cursorDigits).clamp(1, text.length);

    return TextEditingValue(
      text: text,
      selection: TextSelection.collapsed(offset: cursor),
    );
  }
}
