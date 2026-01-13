#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
最终方案：直接读取 Git 对象的原始字节，尝试所有可能的编码组合
"""
import subprocess
import sys
import io

# 设置标准输出为 UTF-8
if sys.platform == 'win32':
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

# 获取提交对象的原始字节
result = subprocess.run(
    ['git', 'cat-file', '-p', 'HEAD'],
    capture_output=True
)

raw_bytes = result.stdout

# 找到提交信息部分（在空行之后）
commit_msg_start = raw_bytes.find(b'\n\n') + 2
if commit_msg_start > 1:
    commit_msg_bytes = raw_bytes[commit_msg_start:]
else:
    commit_msg_bytes = raw_bytes

print("=" * 70)
print("提交信息的原始字节（前100字节的十六进制）:")
print("=" * 70)
print(commit_msg_bytes[:100].hex())
print()

# 尝试不同的编码
encodings = [
    ('gbk', 'GBK'),
    ('gb2312', 'GB2312'),
    ('gb18030', 'GB18030'),
    ('utf-8', 'UTF-8'),
    ('big5', 'Big5'),
]

print("=" * 70)
print("尝试不同编码解码:")
print("=" * 70)
print()

best_result = None
best_encoding = None
max_chinese_chars = 0

for encoding, name in encodings:
    try:
        decoded = commit_msg_bytes.decode(encoding, errors='replace')
        # 统计中文字符数量
        chinese_count = sum(1 for c in decoded if '\u4e00' <= c <= '\u9fff')
        
        print(f"{name} 编码: {chinese_count} 个中文字符")
        if chinese_count > max_chinese_chars:
            max_chinese_chars = chinese_count
            best_result = decoded
            best_encoding = name
        
        # 如果中文字符足够多，显示结果
        if chinese_count > 10:
            print(f"  -> 可能是正确的编码！")
            print(f"  解码结果: {decoded[:200]}...")
            print()
    except Exception as e:
        print(f"{name} 编码: 解码失败 - {e}")
        continue

print()
print("=" * 70)
print(f"最佳结果（使用 {best_encoding} 编码，{max_chinese_chars} 个中文字符）:")
print("=" * 70)
if best_result:
    print(best_result)
else:
    print("未能找到合适的编码")

