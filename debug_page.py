from pathlib import Path

p = Path(r'D:\code\ideaProject\petService\output\英语库课2000题（湖南可用）_pages\page_0001.txt')
text = p.read_text(encoding='utf-8')
print("=== RAW TEXT (first 800 chars) ===")
print(repr(text[:800]))
print()
print("=== LINES ===")
for i, line in enumerate(text.split('\n')[:30]):
    print(f"{i:2d}: {repr(line)}")