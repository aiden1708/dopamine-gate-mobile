class OathMismatch {
  final int position;
  final String expected;
  final String actual;

  const OathMismatch({
    required this.position,
    required this.expected,
    required this.actual,
  });
}