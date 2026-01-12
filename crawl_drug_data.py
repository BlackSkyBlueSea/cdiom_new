#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
药品信息爬虫脚本
爬取 https://data.pharnexcloud.com/6/table/159 网站的药品信息
生成 SQL INSERT 语句用于导入到 drug_info 表
"""

import requests
from bs4 import BeautifulSoup
import json
import re
import time
from datetime import datetime, timedelta
import random

# 设置请求头，模拟浏览器
HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
    'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
    'Accept-Encoding': 'gzip, deflate, br',
    'Connection': 'keep-alive',
    'Upgrade-Insecure-Requests': '1',
}

# 常用药品列表（如果网站无法直接爬取，使用这些常见药品数据）
COMMON_DRUGS = [
    {"name": "阿莫西林胶囊", "form": "胶囊", "spec": "0.25g*24粒", "manufacturer": "华北制药股份有限公司"},
    {"name": "头孢克肟胶囊", "form": "胶囊", "spec": "0.1g*12粒", "manufacturer": "齐鲁制药有限公司"},
    {"name": "布洛芬缓释胶囊", "form": "胶囊", "spec": "0.3g*20粒", "manufacturer": "中美天津史克制药有限公司"},
    {"name": "对乙酰氨基酚片", "form": "片剂", "spec": "0.5g*10片", "manufacturer": "华润三九医药股份有限公司"},
    {"name": "复方甘草片", "form": "片剂", "spec": "24片", "manufacturer": "北京同仁堂股份有限公司"},
    {"name": "板蓝根颗粒", "form": "颗粒剂", "spec": "10g*20袋", "manufacturer": "广州白云山和记黄埔中药有限公司"},
    {"name": "感冒灵颗粒", "form": "颗粒剂", "spec": "10g*9袋", "manufacturer": "华润三九医药股份有限公司"},
    {"name": "双氯芬酸钠缓释片", "form": "片剂", "spec": "75mg*10片", "manufacturer": "北京诺华制药有限公司"},
    {"name": "奥美拉唑肠溶胶囊", "form": "胶囊", "spec": "20mg*14粒", "manufacturer": "阿斯利康制药有限公司"},
    {"name": "蒙脱石散", "form": "散剂", "spec": "3g*10袋", "manufacturer": "博福-益普生制药有限公司"},
]

def generate_national_code(index):
    """生成国家本位码（模拟）"""
    return f"869{str(index).zfill(13)}"

def generate_trace_code(index):
    """生成药品追溯码（模拟）"""
    return f"TR{str(index).zfill(10)}"

def generate_product_code(index):
    """生成商品码（模拟）"""
    return f"PC{str(index).zfill(8)}"

def escape_sql_string(value):
    """转义 SQL 字符串"""
    if value is None:
        return 'NULL'
    if isinstance(value, str):
        # 转义单引号
        value = value.replace("'", "''")
        return f"'{value}'"
    return str(value)

def crawl_pharnexcloud():
    """爬取 pharnexcloud 网站数据"""
    url = "https://data.pharnexcloud.com/6/table/159"
    drugs = []
    
    try:
        print(f"正在访问: {url}")
        response = requests.get(url, headers=HEADERS, timeout=30)
        response.encoding = 'utf-8'
        
        if response.status_code == 200:
            soup = BeautifulSoup(response.text, 'html.parser')
            
            # 尝试查找表格数据
            tables = soup.find_all('table')
            if tables:
                print(f"找到 {len(tables)} 个表格")
                for table in tables:
                    rows = table.find_all('tr')
                    for row in rows[1:]:  # 跳过表头
                        cells = row.find_all(['td', 'th'])
                        if len(cells) >= 3:
                            drug_data = {
                                'name': cells[0].get_text(strip=True) if len(cells) > 0 else '',
                                'form': cells[1].get_text(strip=True) if len(cells) > 1 else '',
                                'spec': cells[2].get_text(strip=True) if len(cells) > 2 else '',
                                'manufacturer': cells[3].get_text(strip=True) if len(cells) > 3 else '',
                            }
                            if drug_data['name']:
                                drugs.append(drug_data)
            
            # 如果没找到表格，尝试查找 JSON 数据
            if not drugs:
                # 查找可能的 JSON 数据
                json_pattern = re.compile(r'\{.*?"data".*?\}', re.DOTALL)
                matches = json_pattern.findall(response.text)
                for match in matches:
                    try:
                        data = json.loads(match)
                        if 'data' in data and isinstance(data['data'], list):
                            drugs.extend(data['data'])
                    except:
                        pass
            
            print(f"成功爬取 {len(drugs)} 条药品数据")
        else:
            print(f"请求失败，状态码: {response.status_code}")
            
    except Exception as e:
        print(f"爬取网站数据时出错: {str(e)}")
        print("将使用预设的常用药品数据...")
    
    return drugs

def get_common_drugs():
    """获取常用药品列表（如果爬取失败，使用预设数据）"""
    # 尝试爬取网站
    drugs = crawl_pharnexcloud()
    
    # 如果爬取失败或数据不足，使用预设数据
    if len(drugs) < 50:
        print("使用预设的常用药品数据...")
        drugs = COMMON_DRUGS.copy()
        
        # 扩展常用药品列表到100种
        common_drug_names = [
            "阿司匹林肠溶片", "氨咖黄敏胶囊", "复方氨酚烷胺片", "维C银翘片",
            "999感冒灵颗粒", "连花清瘟胶囊", "蒲地蓝消炎片", "蓝芩口服液",
            "急支糖浆", "川贝枇杷膏", "蛇胆川贝液", "强力枇杷露",
            "头孢拉定胶囊", "头孢氨苄胶囊", "头孢呋辛酯片", "头孢地尼胶囊",
            "阿奇霉素片", "罗红霉素胶囊", "克拉霉素片", "左氧氟沙星片",
            "诺氟沙星胶囊", "环丙沙星片", "甲硝唑片", "替硝唑片",
            "甲硝唑阴道泡腾片", "克霉唑栓", "咪康唑栓", "制霉菌素片",
            "复方甘草口服溶液", "氨溴索口服溶液", "乙酰半胱氨酸泡腾片", "桉柠蒎肠溶软胶囊",
            "沙丁胺醇气雾剂", "布地奈德气雾剂", "异丙托溴铵气雾剂", "茶碱缓释片",
            "孟鲁司特钠片", "氯雷他定片", "西替利嗪片", "依巴斯汀片",
            "马来酸氯苯那敏片", "赛庚啶片", "酮替芬片", "富马酸酮替芬片",
            "奥美拉唑肠溶片", "兰索拉唑肠溶片", "雷贝拉唑钠肠溶片", "泮托拉唑钠肠溶片",
            "多潘立酮片", "莫沙必利片", "西沙必利片", "甲氧氯普胺片",
            "多酶片", "复方消化酶胶囊", "双歧杆菌三联活菌胶囊", "枯草杆菌二联活菌颗粒",
            "蒙脱石散", "洛哌丁胺胶囊", "聚乙二醇4000散", "乳果糖口服溶液",
            "开塞露", "甘油栓", "酚酞片", "比沙可啶肠溶片",
            "复方地芬诺酯片", "消旋卡多曲颗粒", "双歧杆菌活菌胶囊", "酪酸梭菌活菌片",
            "美沙拉秦肠溶片", "柳氮磺吡啶肠溶片", "奥沙拉秦钠胶囊", "巴柳氮钠片",
            "甲氨蝶呤片", "来氟米特片", "羟氯喹片", "硫唑嘌呤片",
            "环孢素软胶囊", "他克莫司胶囊", "吗替麦考酚酯胶囊", "来氟米特片",
            "甲泼尼龙片", "泼尼松片", "地塞米松片", "氢化可的松片",
            "布洛芬片", "双氯芬酸钠片", "吲哚美辛片", "美洛昔康片",
            "塞来昔布胶囊", "依托考昔片", "帕瑞昔布钠", "对乙酰氨基酚片",
            "曲马多缓释片", "可待因片", "吗啡片", "芬太尼透皮贴剂",
            "卡马西平片", "苯妥英钠片", "丙戊酸钠片", "拉莫三嗪片",
            "左乙拉西坦片", "托吡酯片", "加巴喷丁胶囊", "奥卡西平片",
        ]
        
        forms = ["片剂", "胶囊", "颗粒剂", "口服液", "注射液", "软膏", "栓剂", "气雾剂"]
        manufacturers = [
            "华北制药股份有限公司", "齐鲁制药有限公司", "中美天津史克制药有限公司",
            "华润三九医药股份有限公司", "北京同仁堂股份有限公司", "广州白云山和记黄埔中药有限公司",
            "北京诺华制药有限公司", "阿斯利康制药有限公司", "博福-益普生制药有限公司",
            "辉瑞制药有限公司", "拜耳医药保健有限公司", "葛兰素史克制药有限公司",
        ]
        
        for i, name in enumerate(common_drug_names[:100-len(drugs)]):
            drugs.append({
                'name': name,
                'form': random.choice(forms),
                'spec': f"{random.choice(['0.1g', '0.25g', '0.5g', '10mg', '20mg'])}*{random.choice([10, 12, 20, 24])}{random.choice(['片', '粒', '袋', '支'])}",
                'manufacturer': random.choice(manufacturers),
            })
    
    return drugs[:100]  # 只取前100种

def generate_approval_number():
    """生成模拟的批准文号"""
    prefixes = ["国药准字H", "国药准字Z", "国药准字S"]
    return f"{random.choice(prefixes)}{random.randint(100000000, 999999999)}"

def determine_storage_requirement(drug_name):
    """根据药品名称判断存储要求"""
    if any(keyword in drug_name for keyword in ["注射", "输液", "生物", "疫苗"]):
        return "2-8℃冷藏"
    elif any(keyword in drug_name for keyword in ["胶囊", "软胶囊"]):
        return "密封，置阴凉干燥处"
    else:
        return "密封，置干燥处"

def determine_is_special(drug_name):
    """判断是否为特殊药品"""
    special_keywords = ["吗啡", "可待因", "芬太尼", "杜冷丁", "阿片", "罂粟", "麻黄", "可卡因"]
    return 1 if any(keyword in drug_name for keyword in special_keywords) else 0

def generate_sql_insert(drugs):
    """生成 SQL INSERT 语句"""
    sql_statements = []
    sql_statements.append("-- ============================================")
    sql_statements.append("-- 药品信息数据导入脚本")
    sql_statements.append(f"-- 生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    sql_statements.append(f"-- 数据条数: {len(drugs)}")
    sql_statements.append("-- ============================================")
    sql_statements.append("")
    sql_statements.append("USE cdiom_db;")
    sql_statements.append("")
    sql_statements.append("-- 开始事务")
    sql_statements.append("START TRANSACTION;")
    sql_statements.append("")
    
    for index, drug in enumerate(drugs, 1):
        national_code = generate_national_code(index)
        trace_code = generate_trace_code(index)
        product_code = generate_product_code(index)
        drug_name = drug.get('name', f'药品{index}')
        dosage_form = drug.get('form', '片剂')
        specification = drug.get('spec', '')
        manufacturer = drug.get('manufacturer', '')
        approval_number = generate_approval_number()
        storage_requirement = determine_storage_requirement(drug_name)
        is_special = determine_is_special(drug_name)
        
        # 生成有效期（未来1-3年）
        expiry_days = random.randint(365, 1095)
        expiry_date = (datetime.now() + timedelta(days=expiry_days)).strftime('%Y-%m-%d')
        
        # 单位
        if '片' in specification or '片剂' in dosage_form:
            unit = '片'
        elif '粒' in specification or '胶囊' in dosage_form:
            unit = '粒'
        elif '袋' in specification or '颗粒' in dosage_form:
            unit = '袋'
        elif '支' in specification or '注射' in dosage_form:
            unit = '支'
        else:
            unit = '盒'
        
        sql = f"""INSERT INTO drug_info (
    national_code,
    trace_code,
    product_code,
    drug_name,
    dosage_form,
    specification,
    approval_number,
    manufacturer,
    supplier_name,
    supplier_id,
    expiry_date,
    is_special,
    storage_requirement,
    storage_location,
    unit,
    description,
    create_by,
    deleted
) VALUES (
    {escape_sql_string(national_code)},
    {escape_sql_string(trace_code)},
    {escape_sql_string(product_code)},
    {escape_sql_string(drug_name)},
    {escape_sql_string(dosage_form)},
    {escape_sql_string(specification)},
    {escape_sql_string(approval_number)},
    {escape_sql_string(manufacturer)},
    NULL,
    NULL,
    {escape_sql_string(expiry_date)},
    {is_special},
    {escape_sql_string(storage_requirement)},
    NULL,
    {escape_sql_string(unit)},
    NULL,
    NULL,
    0
);"""
        sql_statements.append(sql)
        sql_statements.append("")
    
    sql_statements.append("-- 提交事务")
    sql_statements.append("COMMIT;")
    sql_statements.append("")
    sql_statements.append("-- 数据导入完成！")
    
    return "\n".join(sql_statements)

def main():
    """主函数"""
    print("=" * 60)
    print("药品信息爬虫脚本")
    print("=" * 60)
    print()
    
    # 获取药品数据
    print("正在获取药品数据...")
    drugs = get_common_drugs()
    print(f"成功获取 {len(drugs)} 种药品信息")
    print()
    
    # 生成 SQL 脚本
    print("正在生成 SQL 脚本...")
    sql_content = generate_sql_insert(drugs)
    
    # 保存到文件
    output_file = "drug_info_insert.sql"
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(sql_content)
    
    print(f"SQL 脚本已生成: {output_file}")
    print(f"共 {len(drugs)} 条药品数据")
    print()
    print("=" * 60)
    print("完成！")
    print("=" * 60)
    print()
    print("使用说明：")
    print(f"1. 打开 MySQL Workbench")
    print(f"2. 连接到 cdiom_db 数据库")
    print(f"3. 打开并执行 {output_file} 文件")
    print()

if __name__ == "__main__":
    main()




