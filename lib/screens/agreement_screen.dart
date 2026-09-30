import 'package:flutter/material.dart';

import '../widgets/gate_button.dart';
import '../widgets/transition_button.dart';

class AgreementScreen extends StatelessWidget {
  final VoidCallback onContinue;
  final VoidCallback onExit;

  const AgreementScreen({
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
                "Slow down.",
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 30,
                  fontWeight: FontWeight.bold,
                ),
              ),

              const SizedBox(height: 16),

              const Text(
                "You are about to enter an application "
                "that may easily consume more of your attention "
                "than you intended.",
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 17,
                  height: 1.5,
                ),
              ),

              const Spacer(),

              const Text(
                "Are you ready to continue deliberately?",
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 22,
                  fontWeight: FontWeight.w500,
                ),
              ),

              const Spacer(),

              TransitionButton(
                text: "Yes, continue",
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