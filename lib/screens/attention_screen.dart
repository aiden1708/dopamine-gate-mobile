import 'package:flutter/material.dart';

import '../widgets/gate_button.dart';
import '../widgets/transition_button.dart';

class AttentionScreen extends StatelessWidget {
  final VoidCallback onContinue;
  final VoidCallback onExit;

  const AttentionScreen({
    super.key,
    required this.onContinue,
    required this.onExit,
  });

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(28),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const SizedBox(height: 30),

              const Text(
                "Don't be impatient.",
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 30,
                  fontWeight: FontWeight.bold,
                ),
              ),

              const SizedBox(height: 12),

              const Text(
                "Read carefully. "
                "This is not something to click through.",
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 16,
                ),
              ),

              const Spacer(),

              const Text(
                "Is your attention here?",
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.w500,
                ),
              ),

              const SizedBox(height: 20),

              const Text(
                "Good.\n\n"
                "Then take a moment before you continue.\n\n"
                "Take a deep breath.",
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 18,
                  height: 1.6,
                ),
              ),

              const Spacer(),

              TransitionButton(
                text: "Yes, I'm here",
                onComplete: onContinue,
              ),

              const SizedBox(height: 12),

              GateButton(
                text: "No — exit",
                onPressed: onExit,
                danger: true,
              ),

              const SizedBox(height: 20),
            ],
          ),
        ),
      ),
    );
  }
}