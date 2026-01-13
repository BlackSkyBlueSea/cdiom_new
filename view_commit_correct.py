#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
正确查看 Git 提交信息的工具
尝试用不同的编码解码，找到最可能正确的显示方式
"""
import subprocess
import sys
import io

# 设置标准输出为 UTF-8
if sys.platform == 'win32':
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

def main():
    print("=" * 70)
    print("Git 提交信息查看工具")
    print("=" * 70)
    print()
    
    # 获取原始字节
    result = subprocess.run(
        ['git', 'log', '-1', '--format=%H%n%an <%ae>%n%ad%n%s%n%b'],
        capture_output=True
    )
    
    raw_bytes = result.stdout
    
    # 分离元数据和提交信息
    lines = raw_bytes.split(b'\n', 3)
    if len(lines) >= 4:
        metadata_bytes = b'\n'.join(lines[:3])
        commit_msg_bytes = lines[3] if len(lines) > 3 else b''
    else:
        metadata_bytes = raw_bytes
        commit_msg_bytes = b''
    
    # 显示元数据（通常不会有编码问题）
    print("提交元数据：")
    print("-" * 70)
    try:
        print(metadata_bytes.decode('utf-8', errors='replace'))
    except:
        print(metadata_bytes.decode('latin1', errors='replace'))
    print()
    
    # 尝试不同的编码解码提交信息
    print("提交信息（尝试不同编码）：")
    print("-" * 70)
    
    encodings = [
        ('utf-8', 'UTF-8（当前显示）'),
        ('gbk', 'GBK（如果提交时使用）'),
        ('gb2312', 'GB2312'),
        ('gb18030', 'GB18030'),
    ]
    
    for encoding, name in encodings:
        try:
            decoded = commit_msg_bytes.decode(encoding, errors='replace')
            chinese_count = sum(1 for c in decoded if '\u4e00' <= c <= '\u9fff')
            print(f"\n{name}:")
            print(f"  中文字符数: {chinese_count}")
            if chinese_count > 0:
                print(f"  内容预览: {decoded[:100]}...")
        except Exception as e:
            print(f"\n{name}: 解码失败 - {e}")
    
    print()
    print("=" * 70)
    print("说明：")
    print("  - 如果所有编码都显示乱码，说明提交信息在存储时已损坏")
    print("  - 建议使用 git commit --amend 修改提交信息")
    print("=" * 70)

if __name__ == '__main__':
    main()

