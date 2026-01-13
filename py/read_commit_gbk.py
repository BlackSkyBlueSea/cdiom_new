#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
读取 Git 提交信息（GBK 编码）
"""
import subprocess
import sys

# 获取提交信息的原始字节
result = subprocess.run(
    ['git', 'log', '-1', '--format=%H%n%an <%ae>%n%ad%n%s%n%b'],
    capture_output=True
)

# 尝试用 GBK 解码
try:
    # 先尝试 GBK
    text = result.stdout.decode('gbk')
    print(text)
except UnicodeDecodeError:
    # 如果 GBK 失败，尝试 UTF-8
    try:
        text = result.stdout.decode('utf-8')
        print(text)
    except UnicodeDecodeError:
        # 最后尝试 latin1（不会失败，但可能显示不正确）
        text = result.stdout.decode('latin1', errors='replace')
        print(text)

