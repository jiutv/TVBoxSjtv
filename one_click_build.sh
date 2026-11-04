#!/bin/bash
# 一键编译脚本 —— 给用户跑的
cd /workspace || cd "$(dirname "$0")"

echo "=== 1. 环境检测 ==="
echo "JAVA_HOME=${JAVA_HOME:-未设置}"
echo "ANDROID_HOME=${ANDROID_HOME:-未设置}"
echo "local.properties:"
cat local.properties 2>/dev/null || echo "(不存在)"
echo ""

echo "=== 2. 清理 ==="
rm -rf app/build
echo ""

echo "=== 3. 编译 assembleJavaDebug ==="
timeout 600 ./gradlew :app:assembleJavaDebug 2>&1 | tee /tmp/build.log
EXIT_CODE=${PIPESTATUS[0]}
echo ""

echo "=== 4. 编译结果 (exit=$EXIT_CODE) ==="
if ls app/build/outputs/apk/*/debug/*.apk 2>/dev/null; then
    echo "✅ 成功！"
    mkdir -p apk
    cp app/build/outputs/apk/*/debug/*.apk apk/
    ls -lh apk/
else
    echo "❌ 失败"
    echo ""
    echo "=== 关键错误 ==="
    grep -E "^e:|FAILED|BUILD FAILED|error:|Exception|FAILURE" /tmp/build.log | head -30
    echo ""
    echo "=== 最后 50 行 ==="
    tail -50 /tmp/build.log
fi
