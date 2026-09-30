import 'dart:async';

import 'package:flutter/material.dart';


class TransitionButton extends StatefulWidget {
  final String text;
  final VoidCallback onComplete;
  final int seconds;

  const TransitionButton({
    super.key,
    required this.text,
    required this.onComplete,
    this.seconds = 5,
  });

  @override
  State<TransitionButton> createState() =>
      _TransitionButtonState();
}

class _TransitionButtonState extends State<TransitionButton> {
  Timer? _timer;

  int _remaining = 0;
  bool _transitioning = false;

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  void _startTransition() {
    if (_transitioning) {
      return;
    }

    setState(() {
      _transitioning = true;
      _remaining = widget.seconds;
    });

    _timer = Timer.periodic(
      const Duration(seconds: 1),
      (_) {
        if (!mounted) {
          return;
        }

        if (_remaining <= 1) {
          _timer?.cancel();

          setState(() {
            _remaining = 0;
          });

          widget.onComplete();

          return;
        }

        setState(() {
          _remaining--;
        });
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final text = _transitioning
        ? 'Continuing in $_remaining...'
        : widget.text;

    return SizedBox(
      width: double.infinity,
      height: 54,
      child: ElevatedButton(
        onPressed: _transitioning
            ? null
            : _startTransition,
        child: Text(
          text,
          style: const TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.bold,
          ),
        ),
      ),
    );
  }
}