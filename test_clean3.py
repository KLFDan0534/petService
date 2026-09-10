import re

def clean_text(text: str) -> str:
    """清洗单页 OCR 文本"""
    # 1. 去除水印
    text = re.sub(r'www\.kuke99\.com', '', text)
    text = re.sub(r'Be www\.kuke99\.com', '', text)
    text = re.sub(r'www\.kuke99\.com _', '', text)
    
    # 2. 去除页眉/页脚干扰行 - 逐行处理更精确
    lines = text.split('\n')
    filtered_lines = []
    for line in lines:
        stripped = line.strip()
        # 跳过空行（保留空行用于段落分隔）
        if not stripped:
            filtered_lines.append(line)
            continue
        # 跳过页码标记行
        if re.match(r'^=== 第 \d+ 页 ===$', stripped):
            continue
        # 跳过纯符号行 (=, |, -, ~ 等)
        if re.match(r'^[\s\-_=~|*]{1,3}$', stripped):
            continue
        # 跳过短乱码行（如 "cuz svac | RAKE"）
        if len(stripped) < 30 and re.match(r'^[a-z]{3,}\s+[a-z]{3,}\s+\|', stripped):
            continue
        # 跳过只包含大写字母和空格|符号的长行（无实际内容）
        if len(stripped) > 20 and re.match(r'^[A-Z\s\|]{15,}$', stripped):
            continue
        # 关键：检测包含两个 "单词:数字" 模式的行（页眉特征）
        # 如 "Jatt: 566492930 ... Aqaitt: 697729176"
        colon_digit_count = len(re.findall(r'\w+:\s*\d+', stripped))
        if colon_digit_count >= 2:
            continue
        # 检测以 "大写词组 单词:数字" 开头的行
        if re.match(r'^[A-Z]{2,}(\s+[A-Z]{2,}){2,}\s+\w+:\s*\d+', stripped):
            continue
        # 新增：跳过含有 "|" 分隔符的混合大小写乱码行
        # 如 "A KB | estawsmzoo08s - 8"、"~ KoA | GHAMBI2Z000M - iB"
        # 特征：包含 "|" 且两边都是乱码词（非正常句子）
        if '|' in stripped:
            parts = [p.strip() for p in stripped.split('|')]
            # 如果分隔后每部分都很短且不像正常句子
            if len(parts) >= 2:
                # 检查是否像乱码：每部分要么全大写，要么混合大小写无空格，要么含数字
                is_garbage = True
                for p in parts:
                    # 正常句子特征：有小写字母、有空格、长度适中
                    if len(p) > 10 and re.search(r'[a-z]{3,}', p) and ' ' in p:
                        is_garbage = False
                        break
                if is_garbage and len(stripped) < 60:
                    continue
        # 新增：跳过以 ~ _ = 开头的短行
        if re.match(r'^[_=~]\S*', stripped) and len(stripped) < 30:
            continue
        # 新增：跳过类似 "mR BB | SHAY MI2000 - HB" 的模式
        # 特征：大小写混合 + | + 大写缩写 + 数字
        if re.match(r'^[A-Za-z]{1,3}\s+[A-Za-z]{1,3}\s+\|', stripped) and len(stripped) < 50:
            continue
        # 新增：跳过全大写缩写加点号的乱码行
        # 如 "BG. RHEE AA SEM rs BEY IAT 1 RTE fe ?? BAB AB IE SK EH"
        if re.match(r'^[A-Z]{2,}\.\s+[A-Z]{2,}', stripped) and len(stripped) > 20:
            # 检查是否包含大量大写缩写（无正常单词）
            words = stripped.split()
            upper_abbrev = sum(1 for w in words if re.match(r'^[A-Z]{2,}\.?$', w))
            if upper_abbrev >= len(words) * 0.6:
                continue
        # 新增：跳过类似 "Al. Fee TT TS A a ee RE" 的乱码行
        # 以 "Xx. " 开头，后面跟大量短词（长度<=3），明显是乱码
        if re.match(r'^[A-Za-z]{1,3}\.\s+', stripped) and len(stripped) < 60:
            words = stripped.split()
            if len(words) >= 4:
                # 统计短词（长度<=3）和全大写词的比例
                short_or_upper = sum(1 for w in words[1:] if len(w) <= 3 or re.match(r'^[A-Z]{2,}$', w))
                if short_or_upper >= len(words) * 0.6:
                    continue
        filtered_lines.append(line)
    
    text = '\n'.join(filtered_lines)
    
    # 3. 修复常见 OCR 识别错误
    replacements = {
        '�?': '',           # 常见乱码字符
        '�?': '',
        '': '•',
        '': '',
        '': '',
        '①': '1.',
        '②': '2.',
        '③': '3.',
        '④': '4.',
        '⑤': '5.',
        '⑥': '6.',
        '⑦': '7.',
        '⑧': '8.',
        '⑨': '9.',
        '⑩': '10.',
        'A．': 'A.',
        'B．': 'B.',
        'C．': 'C.',
        'D．': 'D.',
        '．': '.',
        '、': '.',
        '（': '(',
        '）': ')',
        '——': '—',
        '…': '...',
        '“': '"',
        '”': '"',
        '‘': "'",
        '’': "'",
        '﹖': '?',
        '﹗': '!',
        '；': ';',
        '：': ':',
        '《': '<',
        '》': '>',
        '|': 'I',  # OCR 常把 I 识别为 |
        '©': '',   # 版权符号
        '©': '',
    }
    for k, v in replacements.items():
        text = text.replace(k, v)
    
    # 4. 完形填空空白编号标准化：将行内的数字编号转为 [1] [2] 格式，方便填写
    lines = text.split('\n')
    processed_lines = []
    for line in lines:
        stripped = line.strip()
        # 如果是题目行（以数字+点+空格+大写字母开头），不处理
        if re.match(r'^\d+\.\s+[A-D]\.', stripped):
            processed_lines.append(line)
            continue
        # 如果是选项行
        if re.match(r'^[A-D]\.\s', stripped):
            processed_lines.append(line)
            continue
        # 如果是 Cloze 题目行（如 "1. A. grass"）
        if re.match(r'^\d+\.\s+[A-D]\.', stripped):
            processed_lines.append(line)
            continue
        # 其他行：将行内的空白编号标准化
        # 但保护 "Cloze X" 等标题
        if not re.match(r'^Cloze\s+\d+', stripped):
            # 将 " 1." " 2," " 3 " 等替换为 " [1] " " [2] " " [3] "
            # 使用负向前瞻避免匹配题目编号
            line = re.sub(r'(?<!\d)(?<!\.)\s+(\d{1,2})([.,;:)\s])', r' [\1]\2', line)
        processed_lines.append(line)
    
    text = '\n'.join(processed_lines)
    
    # 5. 修复断行问题：选项行合并
    lines = text.split('\n')
    cleaned_lines = []
    i = 0
    while i < len(lines):
        line = lines[i].strip()
        # 跳过空行和纯符号行
        if not line or re.match(r'^[\s\-_=~|*]{3,}$', line):
            i += 1
            continue
        # 跳过只包含乱码大写字母的行
        if re.match(r'^[A-Z\s]{10,}$', line) and len(line) > 15:
            i += 1
            continue
        # 检测选项行模式
        if re.match(r'^[A-D]\.\s', line):
            merged = line
            while i + 1 < len(lines):
                next_line = lines[i + 1].strip()
                if re.match(r'^[A-D]\.\s', next_line):
                    merged += '  ' + next_line
                    i += 1
                else:
                    break
            cleaned_lines.append(merged)
        elif line:
            cleaned_lines.append(line)
        i += 1
    
    text = '\n'.join(cleaned_lines)
    
    # 6. 压缩多余空行
    text = re.sub(r'\n{3,}', '\n\n', text)
    
    # 7. 去除首尾空白
    text = text.strip()
    
    return text


# 测试 page 1 原始文本
raw = open(r'D:\code\ideaProject\petService\output\英语库课2000题（湖南可用）_pages\page_0001.txt', 'r', encoding='utf-8').read()
cleaned = clean_text(raw)
print("=== CLEANED PAGE 1 ===")
print(cleaned[:3000])
print("...")