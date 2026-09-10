#!/usr/bin/env python3
"""
将 OCR 文本按每 50 页生成可编辑的 Word 文档
清洗水印、修复常见 OCR 错误、保持题目结构
"""

from docx import Document
from docx.shared import Pt, Inches, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.style import WD_STYLE_TYPE
import re
from pathlib import Path


PAGES_DIR = Path(r"D:\code\ideaProject\petService\output\英语库课2000题（湖南可用）_pages")
OUTPUT_DIR = Path(r"D:\code\ideaProject\petService\output\word_docs")
OUTPUT_DIR.mkdir(exist_ok=True)


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
    # 匹配模式：单词后跟空格+数字+标点/空格（如 "by 1.", "so 2.", "15. I")
    # 但要避免破坏题目编号（如 "1. A. global"）
    # 策略：在非题目行中，将 " 空格+数字+标点" 替换为 " [数字] "
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
        # 匹配：非数字开头，中间有 " 空格+数字+标点/空格" 的模式
        # 但保护 "Cloze X" 等标题
        if not re.match(r'^Cloze\s+\d+', stripped):
            # 将 " 1." " 2," " 3 " 等替换为 " [1] " " [2] " " [3] "
            # 使用负向前瞻避免匹配题目编号
            line = re.sub(r'(?<!\d)(?<!\.)\s+(\d{1,2})([.,;:)\s])', r' [\1]\2', line)
            # 修复 "15. I" 这种被误匹配的情况 - 如果前面是句号且后面是大写字母，可能是句子继续
            # 这里不处理，保持原样
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


def add_page_to_doc(doc: Document, page_num: int, text: str):
    """将单页内容添加到 Word 文档"""
    # 页码标题
    heading = doc.add_heading(f'第 {page_num} 页', level=2)
    heading.alignment = WD_ALIGN_PARAGRAPH.CENTER
    
    # 分段落添加内容
    paragraphs = text.split('\n\n')
    for para_text in paragraphs:
        para_text = para_text.strip()
        if not para_text:
            continue
        
        # 判断是否为题目/选项
        if re.match(r'^\d+\.', para_text) or re.match(r'^[A-D]\.', para_text):
            p = doc.add_paragraph()
            run = p.add_run(para_text)
            run.font.size = Pt(11)
            run.font.name = 'Times New Roman'
            p.paragraph_format.space_after = Pt(4)
        elif para_text.startswith('Cloze') or para_text.startswith('VA Exercise') or para_text.startswith('Directions'):
            p = doc.add_paragraph()
            run = p.add_run(para_text)
            run.font.size = Pt(11)
            run.font.bold = True
            run.font.name = 'Times New Roman'
            run.font.color.rgb = RGBColor(0, 0, 139)
            p.paragraph_format.space_before = Pt(12)
            p.paragraph_format.space_after = Pt(6)
        else:
            p = doc.add_paragraph(para_text)
            p.style.font.size = Pt(11)
            p.style.font.name = 'Times New Roman'
            p.paragraph_format.space_after = Pt(6)
    
    # 页末分页符（最后一页除外）
    doc.add_page_break()


def create_word_doc(start_page: int, end_page: int, output_name: str):
    """创建单个 Word 文档"""
    doc = Document()
    
    # 设置默认字体
    style = doc.styles['Normal']
    font = style.font
    font.name = 'Times New Roman'
    font.size = Pt(11)
    font.color.rgb = RGBColor(0, 0, 0)
    
    # 设置中文字体兼容
    style.element.rPr.rFonts.set(qn('w:eastAsia'), '微软雅黑')
    
    # 标题页
    title = doc.add_heading(f'英语库课 2000 题（湖南可用）', level=1)
    title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    subtitle = doc.add_paragraph(f'第 {start_page}–{end_page} 页')
    subtitle.alignment = WD_ALIGN_PARAGRAPH.CENTER
    doc.add_paragraph()
    
    # 逐页处理
    for page_num in range(start_page, end_page + 1):
        page_file = PAGES_DIR / f'page_{page_num:04d}.txt'
        if not page_file.exists():
            print(f'警告: 第 {page_num} 页文件不存在')
            continue
        
        raw_text = page_file.read_text(encoding='utf-8')
        # 提取实际内容（去除 "=== 第 X 页 ===" 标记）
        content = raw_text.replace(f'=== 第 {page_num} 页 ===', '').strip()
        cleaned = clean_text(content)
        
        if cleaned:
            add_page_to_doc(doc, page_num, cleaned)
            print(f'  已处理第 {page_num} 页 ({len(cleaned)} 字符)')
        else:
            print(f'  第 {page_num} 页内容为空，跳过')
    
    # 移除最后一个分页符
    if doc.paragraphs:
        last_para = doc.paragraphs[-1]
        if last_para.runs and 'page break' in str(last_para._element.xml).lower():
            p_element = last_para._element
            p_element.getparent().remove(p_element)
    
    output_path = OUTPUT_DIR / output_name
    doc.save(output_path)
    print(f'[OK] 已生成: {output_path}')
    return output_path


from docx.oxml.ns import qn


def main():
    print('开始生成 Word 文档...')
    print(f'源目录: {PAGES_DIR}')
    print(f'输出目录: {OUTPUT_DIR}')
    print()
    
    # 第 1-50 页
    print('正在生成第 1-50 页...')
    create_word_doc(1, 50, '英语库课2000题_第1-50页.docx')
    
    # 第 51-100 页
    print('\n正在生成第 51-100 页...')
    create_word_doc(51, 100, '英语库课2000题_第51-100页.docx')
    
    # 第 101-126 页
    print('\n正在生成第 101-126 页...')
    create_word_doc(101, 126, '英语库课2000题_第101-126页.docx')
    
    print('\n全部完成！')
    print(f'文档保存在: {OUTPUT_DIR}')


if __name__ == '__main__':
    # 确保输出目录存在
    OUTPUT_DIR.mkdir(exist_ok=True)
    main()


if __name__ == '__main__':
    main()