#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
使用 GBK 编码正确显示 Git 提交信息
"""
import subprocess
import sys
import io

# 设置标准输出为 UTF-8（用于显示）
if sys.platform == 'win32':
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

def get_commit_info():
    """获取提交信息，尝试用 GBK 解码"""
    # 获取原始字节
    result = subprocess.run(
        ['git', 'log', '-1', '--format=%H%n%an <%ae>%n%ad%n%s%n%b'],
        capture_output=True
    )
    
    raw_bytes = result.stdout
    
    # 尝试用 GBK 解码
    try:
        text = raw_bytes.decode('gbk', errors='replace')
        return text, 'GBK'
    except:
        # 如果失败，尝试 UTF-8
        try:
            text = raw_bytes.decode('utf-8', errors='replace')
            return text, 'UTF-8'
        except:
            text = raw_bytes.decode('latin1', errors='replace')
            return text, 'Latin1'

def main():
    print("=" * 70)
    print("Git 提交信息（GBK 编码解码）")
    print("=" * 70)
    print()
    
    text, encoding = get_commit_info()
    
    print(f"使用编码: {encoding}")
    print()
    print(text)
    print()
    print("=" * 70)
    
    # 如果显示的还是乱码，尝试修复
    if encoding == 'UTF-8' and '鏇存柊' in text:
        print("\n检测到可能的编码问题，尝试修复...")
        print("-" * 70)
        
        # 将 UTF-8 乱码文本重新编码为字节，然后用 GBK 解码
        try:
            utf8_bytes = text.encode('utf-8', errors='replace')
            fixed_text = utf8_bytes.decode('gbk', errors='replace')
            
            # 检查修复后的文本是否包含更多正常的中文
            chinese_in_original = sum(1 for c in text if '\u4e00' <= c <= '\u9fff')
            chinese_in_fixed = sum(1 for c in fixed_text if '\u4e00' <= c <= '\u9fff')
            
            if chinese_in_fixed > chinese_in_original:
                print("修复后的提交信息：")
                print(fixed_text)
            else:
                print("修复未成功，原始文本可能已经是正确的。")
        except Exception as e:
            print(f"修复失败: {e}")

if __name__ == '__main__':
    main()

