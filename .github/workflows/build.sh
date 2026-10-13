#!/bin/bash
echo "=== Préparation du projet Koshibar-Tun ==="
export ANDROID_HOME=$HOME/android-sdk
mkdir -p $ANDROID_HOME

echo "=== Compilation de l'APK en cours ==="
gradle assembleDebug --stacktrace
