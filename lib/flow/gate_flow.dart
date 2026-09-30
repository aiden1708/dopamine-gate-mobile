import 'package:flutter/material.dart';

import '../models/gate_step.dart';
import '../screens/attention_screen.dart';
import '../screens/agreement_screen.dart';
import '../screens/oath_screen.dart';

class GateFlow {
  static Widget buildStep(
    GateStep step,
    VoidCallback onComplete,
    VoidCallback onExit,
  ) {
    switch (step) {
      case GateStep.attention:
        return AttentionScreen(
          onContinue: onComplete,
          onExit: onExit,
        );

      case GateStep.agreement:
        return AgreementScreen(
          onContinue: onComplete,
          onExit: onExit,
        );

      case GateStep.oath:
        return OathScreen(
          onComplete: onComplete,
          onExit: onExit,
        );
    }
  }
}