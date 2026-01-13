#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
读取 Git 提交对象的原始内容并用 GBK 解码
"""
import subprocess
import sys
import io

# 设置标准输出为 UTF-8
if sys.platform == 'win32':
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

# 获取提交对象的原始内容（二进制）
result = subprocess.run(
    ['git', 'cat-file', '-p', 'HEAD'],
    capture_output=True
)

raw_bytes = result.stdout

# 尝试用 GBK 解码
print("=" * 60)
print("尝试用 GBK 编码解码提交信息")
print("=" * 60)
print()

# 尝试不同的编码
encodings_to_try = [
    ('gbk', 'GBK'),
    ('gb2312', 'GB2312'),
    ('gb18030', 'GB18030'),
    ('utf-8', 'UTF-8'),
]

for encoding, name in encodings_to_try:
    try:
        decoded_text = raw_bytes.decode(encoding, errors='replace')
        # 检查是否包含中文字符
        has_chinese = any('\u4e00' <= char <= '\u9fff' for char in decoded_text)
        if has_chinese or encoding == 'utf-8':
            print(f"使用 {name} 编码解码结果：")
            print("-" * 60)
            print(decoded_text)
            print("-" * 60)
            if has_chinese:
                break
    except Exception as e:
        print(f"{name} 解码失败: {e}")
        continue

