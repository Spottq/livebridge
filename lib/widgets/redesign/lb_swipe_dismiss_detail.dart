import 'package:flutter/material.dart';

class LbSwipeDismissDetail extends StatefulWidget {
  const LbSwipeDismissDetail({
    super.key,
    required this.child,
  });

  final Widget child;

  @override
  State<LbSwipeDismissDetail> createState() => _LbSwipeDismissDetailState();
}

class _LbSwipeDismissDetailState extends State<LbSwipeDismissDetail>
    with SingleTickerProviderStateMixin {
  static const double _dismissThreshold = 0.22;
  static const double _dismissVelocity = 900;

  late final AnimationController _controller;
  Animation<double>? _settleAnimation;
  double _dragOffset = 0;
  bool _isDragging = false;
  bool _isSettling = false;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 180),
    )..addListener(() {
      final Animation<double>? animation = _settleAnimation;
      if (animation == null) {
        return;
      }
      setState(() => _dragOffset = animation.value);
    });
    _controller.addStatusListener((AnimationStatus status) {
      if (status != AnimationStatus.completed || !_isSettling) {
        return;
      }
      _isSettling = false;
      _settleAnimation = null;
    });
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _animateBack() {
    _isSettling = true;
    _settleAnimation = Tween<double>(
      begin: _dragOffset,
      end: 0,
    ).animate(CurvedAnimation(parent: _controller, curve: Curves.easeOutCubic));
    _controller.forward(from: 0);
  }

  void _handleDragStart(DragStartDetails details) {
    _controller.stop();
    _settleAnimation = null;
    _isSettling = false;
    _isDragging = true;
  }

  void _handleDragUpdate(DragUpdateDetails details) {
    if (!_isDragging) {
      return;
    }
    setState(() {
      _dragOffset = (_dragOffset + details.delta.dx).clamp(0, double.infinity);
    });
  }

  void _handleDragEnd(DragEndDetails details) {
    if (!_isDragging) {
      return;
    }
    _isDragging = false;

    final double width = MediaQuery.sizeOf(context).width;
    final double progress = width <= 0 ? 0 : _dragOffset / width;
    final double velocity = details.primaryVelocity ?? 0;

    if (progress >= _dismissThreshold || velocity >= _dismissVelocity) {
      Navigator.of(context).maybePop();
      return;
    }

    _animateBack();
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      behavior: HitTestBehavior.translucent,
      onHorizontalDragStart: _handleDragStart,
      onHorizontalDragUpdate: _handleDragUpdate,
      onHorizontalDragEnd: _handleDragEnd,
      onHorizontalDragCancel: () {
        if (_isDragging) {
          _isDragging = false;
          _animateBack();
        }
      },
      child: Transform.translate(
        offset: Offset(_dragOffset, 0),
        child: widget.child,
      ),
    );
  }
}
