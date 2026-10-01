### Logs

adb logcat -c
adb logcat | grep -E "ActivityTaskManager|DopamineGate|dopamine_gate_mobile"

## Clean the previous version, compile and install

flutter clean                                               
flutter pub get           
flutter build apk
adb install build/app/outputs/flutter-apk/app-release.apk