import '../models/oath_mismatch.dart';

class OathChecker {
  static OathMismatch? findFirstMismatch(
    String typed,
    String expected,
  ) {
    final minimum =
        typed.length < expected.length
            ? typed.length
            : expected.length;

    for (var i = 0; i < minimum; i++) {
      if (typed[i] != expected[i]) {
        return OathMismatch(
          position: i,
          expected: expected[i],
          actual: typed[i],
        );
      }
    }

    // User typed more than expected.
    if (typed.length > expected.length) {
      return OathMismatch(
        position: expected.length,
        expected: '',
        actual: typed[expected.length],
      );
    }

    // User hasn't finished yet.
    if (typed.length < expected.length) {
      return OathMismatch(
        position: typed.length,
        expected: expected[typed.length],
        actual: '',
      );
    }

    // Exact match.
    return null;
  }

  static bool isComplete(
    String typed,
    String expected,
  ) {
    return typed == expected;
  }

  static String displayCharacter(String character) {
    if (character.isEmpty) {
      return 'nothing';
    }

    if (character == '\n') {
      return '↵';
    }

    if (character == ' ') {
      return 'space';
    }

    return "'$character'";
  }
}