import 'package:flutter/material.dart';

import '../config/gate_config.dart';
import '../models/oath_mismatch.dart';
import '../services/oath_checker.dart';
import '../widgets/gate_button.dart';

class OathScreen extends StatefulWidget {
  final VoidCallback onComplete;
  final VoidCallback onExit;

  const OathScreen({super.key, required this.onComplete, required this.onExit});

  @override
  State<OathScreen> createState() => _OathScreenState();
}

class _OathScreenState extends State<OathScreen> {
  final TextEditingController _controller = TextEditingController();

  final FocusNode _focusNode = FocusNode();

  OathMismatch? _mismatch;

  @override
  void initState() {
    super.initState();

    _controller.addListener(_checkTyping);
  }

  @override
  void dispose() {
    _controller.removeListener(_checkTyping);
    _controller.dispose();
    _focusNode.dispose();

    super.dispose();
  }

  bool get _isComplete {
    return _controller.text == oath;
  }

  void _checkTyping() {
    final typed = _controller.text;

    setState(() {
      _mismatch = OathChecker.findFirstMismatch(typed, oath);
    });
  }

  void _submit() {
    if (_isComplete) {
      widget.onComplete();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      resizeToAvoidBottomInset: true,

      body: SafeArea(
        child: LayoutBuilder(
          builder: (context, constraints) {
            return SingleChildScrollView(
              padding: const EdgeInsets.fromLTRB(20, 20, 20, 20),
              child: ConstrainedBox(
                constraints: BoxConstraints(
                  minHeight: constraints.maxHeight - 40,
                ),
                child: IntrinsicHeight(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      const SizedBox(height: 10),

                      const Text(
                        'THE OATH',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          fontSize: 28,
                          fontWeight: FontWeight.bold,
                        ),
                      ),

                      const SizedBox(height: 10),

                      const Text(
                        'Read the faint text carefully.\n'
                        'Then type it deliberately.',
                        textAlign: TextAlign.center,
                        style: TextStyle(fontSize: 15, color: Colors.grey),
                      ),

                      const SizedBox(height: 20),

                      Expanded(child: _buildOathEditor()),

                      const SizedBox(height: 14),

                      _buildStatus(),

                      const SizedBox(height: 14),

                      GateButton(
                        text: 'Continue',
                        onPressed: _isComplete ? _submit : null,
                      ),

                      const SizedBox(height: 10),

                      GateButton(
                        text: 'Exit',
                        onPressed: widget.onExit,
                        danger: true,
                      ),
                    ],
                  ),
                ),
              ),
            );
          },
        ),
      ),
    );
  }

  Widget _buildOathEditor() {
    return Container(
      width: double.infinity,
      decoration: BoxDecoration(
        color: Colors.grey.shade900,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: _borderColor(), width: 2),
      ),
      clipBehavior: Clip.antiAlias,
      child: Stack(
        children: [
          // ------------------------------------------------------
          // GHOST TEXT
          // ------------------------------------------------------

          Padding(
            padding: const EdgeInsets.all(18),
            child: IgnorePointer(child: _buildGhostText()),
          ),

          // ------------------------------------------------------
          // REAL INPUT
          // ------------------------------------------------------
          TextField(
            controller: _controller,
            focusNode: _focusNode,
            keyboardType: TextInputType.multiline,
            textInputAction: TextInputAction.newline,
            maxLines: null,
            expands: true,
            textAlignVertical: TextAlignVertical.top,

            style: const TextStyle(
              fontSize: 17,
              height: 1.55,
              fontWeight: FontWeight.normal,
              letterSpacing: 0,
              color: Colors.transparent,
            ),

            cursorColor: Colors.white,

            decoration: const InputDecoration(
              border: InputBorder.none,
              contentPadding: EdgeInsets.all(18),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildGhostText() {
  final typed = _controller.text;

  const baseStyle = TextStyle(
    fontSize: 17,
    height: 1.55,
    fontWeight: FontWeight.normal,
    letterSpacing: 0,
  );

  final spans = <TextSpan>[];

  for (var i = 0; i < oath.length; i++) {
    final expectedCharacter = oath[i];

    Color color;

    if (i >= typed.length) {
      // Not typed yet.
      color = Colors.grey.shade700;
    } else if (typed[i] == expectedCharacter) {
      // Correctly typed.
      color = Colors.amber.shade400;
    } else {
      // Wrong character.
      color = Colors.red.shade400;
    }

    spans.add(
      TextSpan(
        text: expectedCharacter,
        style: baseStyle.copyWith(
          color: color,
        ),
      ),
    );
  }

  return Align(
    alignment: Alignment.topLeft,
    child: RichText(
      text: TextSpan(
        style: baseStyle,
        children: spans,
      ),
    ),
  );
}

  Color _borderColor() {
    if (_isComplete) {
      return Colors.green.shade600;
    }

    if (_mismatch != null && _mismatch!.actual.isNotEmpty) {
      return Colors.red.shade700;
    }

    if (_controller.text.isNotEmpty) {
      return Colors.amber.shade600;
    }

    return Colors.grey.shade700;
  }

  Widget _buildStatus() {
    final typed = _controller.text;

    if (typed.isEmpty) {
      return const Text('Begin writing the oath.', textAlign: TextAlign.center);
    }

    if (_isComplete) {
      return const Text(
        '✓ The oath is complete.',
        textAlign: TextAlign.center,
        style: TextStyle(color: Colors.green, fontWeight: FontWeight.bold),
      );
    }

    final mismatch = _mismatch;

    if (mismatch == null) {
      return const SizedBox.shrink();
    }

    if (mismatch.position == typed.length) {
      final remaining = oath.length - typed.length;

      return Text(
        '✓ Correct so far — '
        '$remaining characters remaining.',
        textAlign: TextAlign.center,
        style: const TextStyle(color: Colors.green),
      );
    }

    final expected = OathChecker.displayCharacter(mismatch.expected);

    final actual = OathChecker.displayCharacter(mismatch.actual);

    return Text(
      'Mismatch at character '
      '${mismatch.position + 1}: '
      'expected $expected, got $actual',
      textAlign: TextAlign.center,
      style: TextStyle(color: Colors.red.shade300, fontWeight: FontWeight.w500),
    );
  }
}
