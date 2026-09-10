#!/usr/bin/env python3
"""
将包含图片的 DOCX 文件转换为文本（每页一段）
使用 python-docx 提取图片 -> Tesseract OCR 识别
"""

import os
import sys
import tempfile
import shutil
from pathlib import Path
from docx import Document
from PIL import Image
import pytesseract

# 配置 Tesseract 路径
TESSERACT_CMD = r"D:\tesseract\tesseract.exe"
TESSDATA_DIR = r"D:\tesseract\tessdata"

pytesseract.pytesseract.tesseract_cmd = TESSERACT_CMD
os.environ['TESSDATA_PREFIX'] = TESSDATA_DIR


def extract_images_from_docx(docx_path, output_dir):
    """从 DOCX 提取所有图片，返回图片路径列表（按文档顺序）"""
    doc = Document(docx_path)
    images = []
    img_count = 0
    
    for rel in doc.part.rels.values():
        if "image" in rel.target_ref:
            img_count += 1
            ext = rel.target_ref.split('.')[-1].split('?')[0]
            img_path = output_dir / f"page_{img_count:04d}.{ext}"
            with open(img_path, 'wb') as f:
                f.write(rel.target_part.blob)
            images.append(img_path)
    
    # 也检查内联图片
    for inline_shape in doc.inline_shapes:
        if hasattr(inline_shape, '_inline') and inline_shape._inline.graphic.graphicData.pic:
            pass  # 已经通过 rels 提取过了
    
    return images


def ocr_image(image_path, lang='chi_sim+eng'):
    """对单张图片进行 OCR"""
    try:
        img = Image.open(image_path)
        # 预处理：转灰度、增强对比度
        if img.mode != 'L':
            img = img.convert('L')
        # 放大小图片以提高识别率
        if img.width < 1000:
            scale = 1000 / img.width
            img = img.resize((int(img.width * scale), int(img.height * scale)), Image.LANCZOS)
        
        text = pytesseract.image_to_string(
            img,
            lang=lang,
            config='--psm 6'  # 假设单块文本
        )
        return text.strip()
    except Exception as e:
        return f"[OCR 错误: {e}]"


def main():
    if len(sys.argv) < 2:
        print("用法: python convert_docx.py <docx文件路径> [输出目录]")
        sys.exit(1)
    
    docx_path = Path(sys.argv[1])
    if not docx_path.exists():
        print(f"文件不存在: {docx_path}")
        sys.exit(1)
    
    output_dir = Path(sys.argv[2]) if len(sys.argv) > 2 else Path.cwd() / "output"
    output_dir.mkdir(parents=True, exist_ok=True)
    
    # 临时目录存放提取的图片
    with tempfile.TemporaryDirectory() as tmpdir:
        tmpdir = Path(tmpdir)
        print(f"正在提取图片...")
        images = extract_images_from_docx(docx_path, tmpdir)
        print(f"共提取 {len(images)} 张图片")
        
        if not images:
            print("未发现图片，尝试直接提取文档文本...")
            doc = Document(docx_path)
            text = "\n".join([p.text for p in doc.paragraphs if p.text.strip()])
            out_file = output_dir / f"{docx_path.stem}.txt"
            out_file.write_text(text, encoding='utf-8')
            print(f"已保存: {out_file}")
            return
        
        print("开始 OCR 识别...")
        all_text = []
        for i, img_path in enumerate(images, 1):
            print(f"  处理第 {i}/{len(images)} 页: {img_path.name}")
            text = ocr_image(img_path)
            if text:
                all_text.append(f"=== 第 {i} 页 ===\n{text}\n")
        
        # 保存结果
        out_file = output_dir / f"{docx_path.stem}_ocr.txt"
        out_file.write_text("\n".join(all_text), encoding='utf-8')
        print(f"\n完成! 结果保存至: {out_file}")
        
        # 同时保存每页单独文件
        pages_dir = output_dir / f"{docx_path.stem}_pages"
        pages_dir.mkdir(exist_ok=True)
        for i, text in enumerate(all_text, 1):
            (pages_dir / f"page_{i:04d}.txt").write_text(text, encoding='utf-8')
        print(f"分页文本保存至: {pages_dir}")


if __name__ == "__main__":
    main()