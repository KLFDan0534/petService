import re

def clean_text(text: str) -> str:
    """清洗单页 OCR 文本"""
    # 1. 去除水印
    text = re.sub(r'www\.kuke99\.com', '', text)
    text = re.sub(r'Be www\.kuke99\.com', '', text)
    text = re.sub(r'www\.kuke99\.com _', '', text)
    
    # 2. 去除页眉/页脚干扰行
    text = re.sub(r'=== 第 \d+ 页 ===', '', text)
    
    # 先按行处理，更精确
    lines = text.split('\n')
    filtered_lines = []
    for line in lines:
        stripped = line.strip()
        # 跳过空行
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


# 测试
raw = '''=== 第 1 页 ===
IL PER FA Jatt: 566492930 ia = >|) Aqaitt: 697729176
=
cuz svac | RAKE

3. A. global B. precise C. vague D. realistic

4. A. simple B. achievable C. reliable D. clear

5. A. equal B. unusual C. regular D. specific

6. A. slowly B. accurately C. easily D. instantly

7. A. support B. lead C. force D. follow

8. A. comfortable B. familiar C. careful D. patient

9. A. change B. to change C. changing D. being changed

10. A. Therefore B. Besides C. Otherwise D. However

11, A. inform B. convince C. remind D. warn

12. A. and B. but C. or D. for

13. A. notebook B. file C. schedule D. case

14. A. doubt B. hope C. dimension D. room

15. A. progress B. process C. practice D. promotion

16. A. selective B. curious C. skeptical D. optimistic

17. A. figure oul B. leave out C. take out'''

cleaned = clean_text(raw)
print("=== CLEANED ===")
print(cleaned)
print()
print("=== LINES ===")
for i, line in enumerate(cleaned.split('\n')):
    print(f"{i:2d}: {repr(line)}")