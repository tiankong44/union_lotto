#!/bin/bash
# 可执行 JAR 文件路径
JAR_PATH=/usr/local/soft/java/workday/lotto.jar
# 检查进程是否正在运行
PROCESS_ID=$(pgrep -f "${JAR_PATH}")
# 如果进程未运行，则启动
if [ -z "${PROCESS_ID}" ]; then
  echo "$(date '+%Y-%m-%d %H:%M:%S') Process not running, starting..."
  cd /usr/local/soft/java/workday/
  sh start.sh
else
  echo "$(date '+%Y-%m-%d %H:%M:%S') Process running with PID ${PROCESS_ID}"
fi
