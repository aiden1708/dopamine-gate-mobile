import 'package:flutter/material.dart';

class GateButton extends StatelessWidget {
  final String text;
  final VoidCallback? onPressed;
  final bool danger;

  const GateButton({
    super.key,
    required this.text,
    required this.onPressed,
    this.danger = false,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: double.infinity,
      height: 54,
      child: ElevatedButton(
        onPressed: onPressed,
        style: ElevatedButton.styleFrom(
          backgroundColor:
              danger ? Colors.red.shade900 : Colors.grey.shade200,
          foregroundColor:
              danger ? Colors.white : Colors.black,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
        ),
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