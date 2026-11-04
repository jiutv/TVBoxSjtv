#!/bin/bash
# 诊断脚本 —— 跑完整编译看错误
set -e
cd /workspace || cd "$(dirname "$0")/../.."

echo "=== 清理 + 编译 ==="
rm -rf app/build
export JAVA_HOME=${JAVA_HOME:-/root/.local/share/mise/installs/java/17.0.20+101}
export ANDROID_HOME=${ANDROID_HOME:-/root/android-sdk}
echo "sdk.dir=$ANDROID_HOME" > local.properties

./gradlew :app:assembleJavaDebug --stacktrace --info 2>&1 | tee /tmp/build.log

echo ""
echo "=== 编译结果 ==="
if ls app/build/outputs/apk/java/debug/*.apk 2>/dev/null; then
    echo "✅ APK 生成成功！"
    cp app/build/outputs/apk/java/debug/*.apk /tmp/
else
    echo "❌ APK 没生成"
    echo ""
    echo "=== 错误日志(最后 80 行) ==="
    tail -80 /tmp/build.log
fi
