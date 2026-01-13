#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
将乱码的提交信息用 GBK 编码正确解码
"""
import subprocess
import sys

# 方法1: 直接读取原始字节，尝试 GBK 解码
print("=" * 50)
print("方法1: 直接读取 Git 原始输出（GBK 解码）")
print("=" * 50)

result = subprocess.run(
    ['git', 'log', '-1', '--format=%H%n%an <%ae>%n%ad%n%s%n%b'],
    capture_output=True
)

# 尝试不同的编码
encodings = ['gbk', 'gb2312', 'gb18030', 'utf-8', 'latin1']

for encoding in encodings:
    try:
        text = result.stdout.decode(encoding, errors='strict')
        # 检查是否包含常见的中文字符
        if any('\u4e00' <= char <= '\u9fff' for char in text):
            print(f"\n使用 {encoding} 编码成功解码：\n")
            print(text)
            break
    except (UnicodeDecodeError, UnicodeEncodeError):
        continue
else:
    print("\n方法1失败，尝试方法2...\n")
    
    # 方法2: 将当前乱码文本重新编码
    print("=" * 50)
    print("方法2: 将乱码文本重新编码为字节后 GBK 解码")
    print("=" * 50)
    
    # 获取乱码文本
    result_utf8 = subprocess.run(
        ['git', 'log', '-1', '--format=%H%n%an <%ae>%n%ad%n%s%n%b'],
        capture_output=True,
        encoding='utf-8',
        errors='replace'
    )
    
    garbled_text = result_utf8.stdout
    
    # 将乱码文本按 UTF-8 编码回字节，然后用 GBK 解码
    try:
        # 乱码文本 -> UTF-8 字节 -> GBK 解码
        bytes_data = garbled_text.encode('utf-8', errors='replace')
        decoded_text = bytes_data.decode('gbk', errors='replace')
        print("\n解码结果：\n")
        print(decoded_text)
    except Exception as e:
        print(f"解码失败: {e}")

