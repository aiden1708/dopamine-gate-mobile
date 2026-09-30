import 'package:flutter/material.dart';

import 'flow/gate_flow.dart';
import 'models/gate_step.dart';

void main() {
  runApp(const DopamineGateApp());
}

class DopamineGateApp extends StatefulWidget {
  const DopamineGateApp({super.key});

  @override
  State<DopamineGateApp> createState() =>
      _DopamineGateAppState();
}

class _DopamineGateAppState
    extends State<DopamineGateApp> {

  GateStep _currentStep = GateStep.attention;

  void _nextStep() {
    setState(() {
      switch (_currentStep) {
        case GateStep.attention:
          _currentStep = GateStep.agreement;
          break;

        case GateStep.agreement:
          _currentStep = GateStep.oath;
          break;

        case GateStep.oath:
          _exit();
          break;
      }
    });
  }

  void _exit() {
    // Temporary.
    //
    // We'll later connect this to the Android
    // launcher/enforcement layer.
    debugPrint('Gate exited.');
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'Dopamine Gate',
      theme: ThemeData.dark(),
      home: GateFlow.buildStep(
        _currentStep,
        _nextStep,
        _exit,
      ),
    );
  }
}