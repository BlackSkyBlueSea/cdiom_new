#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
最终修复方案：根据用户提供的乱码文本，尝试正确的编码转换
"""
import sys
import io

# 设置标准输出为 UTF-8
if sys.platform == 'win32':
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

# 用户提供的乱码文本（UTF-8 显示）
garbled_text = """docs: 鏇存柊椤圭洰鏂囨。鍜屼唬鐮佸畬鏁存€ф娴嬫煡?
- 鏇存柊鎵€鏈?md鏂囦欢鐨勬渶鍚庢洿鏂版棩鏈熶负瀹為檯鍒涘缓/淇敼鏃堕棿
- 娣诲姞鏍稿績涓氬姟闇€姹傚垎鏋愭枃妗ｏ紙搴撳瓨绠＄悊銆佸叆搴撶鐞嗐€佸嚭搴撶鐞嗭級
- 娣诲姞浠ｇ爜瀹屾暣鎬ф娴嬫煡鎶ユ姤鍛?- 瀹屽杽README.md鏂囨。锛屾坊鍔犵涓夋柟API闆嗘垚璇存槑
- 鏇存柊鏉冮檺闂棿淇敼璇存槑銆佸苟鍙戣闂厤缃璇存槑绛夋枃妗。"""

print("=" * 70)
print("分析：如果提交时使用 GBK 编码，但现在显示为 UTF-8 乱码")
print("=" * 70)
print()

print("原始乱码文本：")
print(garbled_text)
print()

print("=" * 70)
print("尝试修复方法：")
print("  1. 将乱码文本（UTF-8 字符串）编码为 UTF-8 字节")
print("  2. 这些字节应该是原始的 GBK 字节")
print("  3. 用 GBK 解码这些字节")
print("=" * 70)
print()

try:
    # 步骤1: UTF-8 文本 -> UTF-8 字节
    utf8_bytes = garbled_text.encode('utf-8', errors='replace')
    print(f"步骤1完成: 得到 {len(utf8_bytes)} 字节")
    
    # 步骤2: 用 GBK 解码
    gbk_decoded = utf8_bytes.decode('gbk', errors='replace')
    print("步骤2完成: 用 GBK 解码")
    print()
    
    print("修复后的提交信息：")
    print("-" * 70)
    print(gbk_decoded)
    print("-" * 70)
    
    # 检查中文字符
    chinese_chars = [c for c in gbk_decoded if '\u4e00' <= c <= '\u9fff']
    print(f"\n包含 {len(chinese_chars)} 个中文字符")
    
    if len(chinese_chars) > 50:
        print("前30个中文字符:", ''.join(chinese_chars[:30]))
        print("\n注意：如果这些字符看起来仍然不正常，")
        print("说明提交信息在存储时就已经损坏，无法完全恢复。")
        print("建议使用 git commit --amend 修改提交信息。")
    
except Exception as e:
    print(f"修复失败: {e}")
    import traceback
    traceback.print_exc()

print()
print("=" * 70)
print("如果修复失败，建议：")
print("  1. 使用 git commit --amend 修改提交信息")
print("  2. 确保 Git 配置使用 UTF-8:")
print("     git config --global i18n.commitencoding utf-8")
print("     git config --global i18n.logoutputencoding utf-8")
print("=" * 70)

