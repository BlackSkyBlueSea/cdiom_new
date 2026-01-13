#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
修复 Git 提交信息的编码问题
如果提交信息是用 GBK 存储的，但现在显示为 UTF-8 乱码，
需要将乱码文本重新编码为字节，然后用 GBK 解码
"""
import subprocess
import sys
import io

# 设置标准输出为 UTF-8
if sys.platform == 'win32':
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

# 获取当前显示的乱码文本（作为 UTF-8 字符串）
result = subprocess.run(
    ['git', 'log', '-1', '--format=%H%n%an <%ae>%n%ad%n%s%n%b'],
    capture_output=True,
    encoding='utf-8',
    errors='replace'
)

garbled_text = result.stdout

print("=" * 70)
print("原始乱码文本（UTF-8 显示）:")
print("=" * 70)
print(garbled_text)
print()

# 方法：将乱码文本按 UTF-8 编码回字节，然后用 GBK 解码
print("=" * 70)
print("尝试修复：将 UTF-8 乱码文本 -> UTF-8 字节 -> GBK 解码")
print("=" * 70)
print()

try:
    # 乱码文本已经是 UTF-8 字符串，将其编码为 UTF-8 字节
    # 这些字节实际上应该是原始的 GBK 字节
    utf8_bytes = garbled_text.encode('utf-8', errors='replace')
    
    # 用 GBK 解码这些字节
    fixed_text = utf8_bytes.decode('gbk', errors='replace')
    
    print("修复后的提交信息：")
    print("-" * 70)
    print(fixed_text)
    print("-" * 70)
    
except Exception as e:
    print(f"修复失败: {e}")
    import traceback
    traceback.print_exc()

