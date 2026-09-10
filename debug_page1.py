from pathlib import Path
import sys

sys.stdout.reconfigure(encoding='utf-8')

p = Path(r'D:\code\ideaProject\petService\output\英语库课2000题（湖南可用）_pages\page_0001.txt')
text = p.read_text(encoding='utf-8')
print("=== FULL PAGE 1 ===")
print(text)
print()
print("=== LINES ===")
for i, line in enumerate(text.split('\n')):
    print(f"{i:3d}: {repr(line)}")