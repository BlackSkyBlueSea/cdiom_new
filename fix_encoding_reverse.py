#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
反向修复：如果提交时 GBK 字节被当作 UTF-8 存储了
需要：乱码 UTF-8 文本 -> UTF-8 编码为字节 -> GBK 解码
"""
import subprocess
import sys
import io

# 设置标准输出为 UTF-8
if sys.platform == 'win32':
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

# 获取乱码文本（作为 UTF-8 字符串）
garbled_utf8 = """docs: 鏇存柊椤圭洰鏂囨。鍜屼唬鐮佸畬鏁存€ф娴嬫煡?
- 鏇存柊鎵€鏈?md鏂囦欢鐨勬渶鍚庢洿鏂版棩鏈熶负瀹為檯鍒涘缓/淇敼鏃堕棿
- 娣诲姞鏍稿績涓氬姟闇€姹傚垎鏋愭枃妗ｏ紙搴撳瓨绠＄悊銆佸叆搴撶鐞嗐€佸嚭搴撶鐞嗭級
- 娣诲姞浠ｇ爜瀹屾暣鎬ф娴嬫煡鎶ユ姤鍛?- 瀹屽杽README.md鏂囨。锛屾坊鍔犵涓夋柟API闆嗘垚璇存槑
- 鏇存柊鏉冮檺闂棿淇敼璇存槑銆佸苟鍙戣闂厤缃璇存槑绛夋枃妗。"""

print("=" * 70)
print("步骤1: 乱码文本（当前 UTF-8 显示）")
print("=" * 70)
print(garbled_utf8)
print()

print("=" * 70)
print("步骤2: 将 UTF-8 文本编码为 UTF-8 字节（这些字节应该是原始的 GBK 字节）")
print("=" * 70)

# 将 UTF-8 文本编码为 UTF-8 字节
utf8_bytes = garbled_utf8.encode('utf-8', errors='replace')
print(f"字节长度: {len(utf8_bytes)}")
print(f"前50字节（十六进制）: {utf8_bytes[:50].hex()}")
print()

print("=" * 70)
print("步骤3: 用 GBK 解码这些字节")
print("=" * 70)

try:
    # 用 GBK 解码这些字节
    fixed_text = utf8_bytes.decode('gbk', errors='replace')
    print("修复后的提交信息：")
    print("-" * 70)
    print(fixed_text)
    print("-" * 70)
    
    # 检查是否包含正常的中文
    chinese_chars = [c for c in fixed_text if '\u4e00' <= c <= '\u9fff']
    print(f"\n包含 {len(chinese_chars)} 个中文字符")
    if len(chinese_chars) > 50:
        print("✓ 看起来修复成功了！")
        print("\n前20个中文字符:", ''.join(chinese_chars[:20]))
    
except Exception as e:
    print(f"解码失败: {e}")
    import traceback
    traceback.print_exc()

