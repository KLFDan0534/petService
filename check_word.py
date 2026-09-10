from docx import Document
import os

files = os.listdir(r'D:\code\ideaProject\petService\output\word_docs')
for fname in files:
    if fname.endswith('.docx') and '1-50' in fname:
        fpath = os.path.join(r'D:\code\ideaProject\petService\output\word_docs', fname)
        print(f'=== {fname} ===')
        doc = Document(fpath)
        for i, p in enumerate(doc.paragraphs[:50]):
            text = p.text.strip()
            if text:
                print(f'  {i}: [{p.style.name}] {text[:300]}')
        break