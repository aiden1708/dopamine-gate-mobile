import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

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

  static const MethodChannel _channel =
      MethodChannel(
    'com.example.dopamine_gate/permissions',
  );

  GateStep _currentStep =
      GateStep.attention;

  List<Map<String, dynamic>> _protectedApps = [];

  bool _isLoadingApps = true;

  /*
   * Determines which part of the application
   * the user is currently viewing.
   *
   * false → Main Page
   * true  → Dopamine Gate flow
   */
  bool _isInGate = false;

  @override
  void initState() {
    super.initState();

    _loadProtectedApps();
    _checkLaunchReason();
  }

  /*
   * Checks whether Android opened Dopamine Gate
   * because a protected app was intercepted.
   *
   * Normal launch:
   *     Main Page
   *
   * Blocked-app redirect:
   *     Attention Screen
   */
  Future<void> _checkLaunchReason() async {
    try {
      final openedFromBlockedApp =
          await _channel.invokeMethod<bool>(
        'wasOpenedFromBlockedApp',
      );

      if (!mounted) {
        return;
      }

      if (openedFromBlockedApp == true) {
        setState(() {
          _isInGate = true;
          _currentStep = GateStep.attention;
        });
      }
    } on PlatformException catch (e) {
      debugPrint(
        'Failed to check launch reason: ${e.message}',
      );
    }
  }

  /*
   * Gets the protected-app list from Android.
   *
   * Flutter does not maintain its own copy of the list.
   * Android ProtectedApps.kt is the source of truth.
   */
  Future<void> _loadProtectedApps() async {
    try {
      final result =
          await _channel.invokeMethod<List<dynamic>>(
        'getProtectedApps',
      );

      if (!mounted) {
        return;
      }

      setState(() {
        _protectedApps =
            (result ?? [])
                .map(
                  (app) =>
                      Map<String, dynamic>.from(app),
                )
                .toList();

        _isLoadingApps = false;
      });
    } on PlatformException catch (e) {
      debugPrint(
        'Failed to load protected apps: ${e.message}',
      );

      if (!mounted) {
        return;
      }

      setState(() {
        _isLoadingApps = false;
      });
    }
  }

  /*
   * Enter the Gate manually.
   *
   * We may use this later for a button on the
   * Main Page.
   */
  void _openGate() {
    setState(() {
      _isInGate = true;
      _currentStep = GateStep.attention;
    });
  }

  /*
   * Move through the Gate steps.
   */
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

  /*
   * Leave the Gate and return to the Main Page.
   */
  void _exit() {
    setState(() {
      _isInGate = false;
      _currentStep = GateStep.attention;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'Dopamine Gate',
      theme: ThemeData.dark(),
      home: _buildHome(),
    );
  }

  /*
   * Decide which screen should be displayed.
   */
  Widget _buildHome() {
    if (_isInGate) {
      return GateFlow.buildStep(
        _currentStep,
        _nextStep,
        _exit,
      );
    }

    return _buildMainPage();
  }

  /*
   * Main Page
   *
   * This is what the user sees when they open
   * Dopamine Gate normally.
   */
  Widget _buildMainPage() {
    return Scaffold(
      appBar: AppBar(
        title: const Text(
          'Dopamine Gate',
        ),
      ),

      body: Padding(
        padding: const EdgeInsets.all(24),

        child: Column(
          crossAxisAlignment:
              CrossAxisAlignment.start,

          children: [

            const Text(
              'Protected Apps',

              style: TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.bold,
              ),
            ),

            const SizedBox(height: 8),

            const Text(
              'These apps are protected by '
              'Dopamine Gate.',

              style: TextStyle(
                fontSize: 16,
                color: Colors.white70,
              ),
            ),

            const SizedBox(height: 24),

            Expanded(
              child:
                  _buildProtectedAppsList(),
            ),

            const SizedBox(height: 16),
          ],
        ),
      ),
    );
  }

  /*
   * Display the protected apps returned
   * by Android.
   */
  Widget _buildProtectedAppsList() {
    if (_isLoadingApps) {
      return const Center(
        child: CircularProgressIndicator(),
      );
    }

    if (_protectedApps.isEmpty) {
      return const Center(
        child: Text(
          'No protected apps found.',
        ),
      );
    }

    return ListView.builder(
      itemCount: _protectedApps.length,

      itemBuilder: (context, index) {
        final app =
            _protectedApps[index];

        final name =
            app['name'] as String? ??
                'Unknown app';

        return Card(
          child: ListTile(

            leading: const Icon(
              Icons.shield,
            ),

            title: Text(
              name,

              style: const TextStyle(
                fontSize: 17,
                fontWeight: FontWeight.w500,
              ),
            ),

            trailing: const Icon(
              Icons.lock,
            ),
          ),
        );
      },
    );
  }
}