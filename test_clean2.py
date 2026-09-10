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
        '\ufffd?': '',      # 常见乱码字符(Unicode 替换字符)
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
    }
    for k, v in replacements.items():
        text = text.replace(k, v)
    
    # 4. 修复断行问题：选项行合并
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
    
    # 5. 压缩多余空行
    text = re.sub(r'\n{3,}', '\n\n', text)
    
    # 6. 去除首尾空白
    text = text.strip()
    
    return text


# 综合测试
raw = '''=== 第 1 页 ===
IL PER FA Jatt: 566492930 ia = >|) Aqaitt: 697729176
=
cuz svac | RAKE

3. A. global B. precise C. vague D. realistic

4. A. simple B. achievable C. reliable D. clear

=== 第 2 页 ===
A KB | estawsmzoo08s - 8
success story has a back story. And I believe that a truly 11 person is someone who

=== 第 3 页 ===
~ KoA | GHAMBI2Z000M - iB
3. A. But B. For C. So D. Because
4, A. children B. fathers C. drivers D. gentlemen

=== 第 4 页 ===
mR BB | SHAY MI2000 - HB
rice dumplings into the water, hoping that the fishes ate the dumplings instead of Qu Yuan' s body.

=== 第 5 页 ===
_=s
sun ww | MACKS
BG. RHEE AA SEM rs BEY IAT 1 RTE fe ?? BAB AB IE SK EH

=== 第 6 页 ===
mR OKA | SHAM RIZO00 - HB
Al. Fee TT TS A a ee RE
42. FORTE Eb Pe
43. PE RATE A EAE A

=== 第 7 页 ===
Normal sentence without garbage.
This is a real question 1. A. option A B. option B C. option C D. option D
Another line here.'''

cleaned = clean_text(raw)
print("=== CLEANED ===")
print(cleaned)
print()
print("=== LINES ===")
for i, line in enumerate(cleaned.split('\n')):
    print(f"{i:2d}: {repr(line)}")