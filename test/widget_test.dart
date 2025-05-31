// This is a basic Flutter widget test.
//
// To perform an interaction with a widget in your test, use the WidgetTester
// utility in the flutter_test package. For example, you can send tap and scroll
// gestures. You can also use WidgetTester to find child widgets in the widget
// tree, read text, and verify that the values of widget properties are correct.

// import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:digital_wellbeing_app/main.dart';

void main() {
  testWidgets('Digital Wellbeing app smoke test', (WidgetTester tester) async {
    // Build our app and trigger a frame.
    await tester.pumpWidget(const DigitalWellbeingApp());

    // Verify that the app starts with the main screen
    expect(find.text('Digital Wellbeing'), findsOneWidget);
    expect(find.text('Balance your digital life'), findsOneWidget);

    // Verify that the bottom navigation works
    expect(find.text('Dashboard'), findsOneWidget);
    expect(find.text('Stats'), findsOneWidget);
    expect(find.text('Settings'), findsOneWidget);
  });
}