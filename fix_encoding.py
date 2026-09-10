#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""编码乱码统一修复工具。

支持三类乱码:
  A. 双重编码 mojibake: 真实 UTF-8 字节被当作 cp1252/latin1 解读后存成 UTF-8
     (形如 'ç"¨æ^·' -> '用户')
  B. UTF-8 被当作 GBK 解读后存成 UTF-8
     (形如 '灏婃暚鐨勭敤鎴' -> '尊敬的用户')
  C. 整文件非 UTF-8 (GBK/GB18030 编码), 直接转码为 UTF-8

用法:
  python fix_encoding.py            # dry-run, 只生成报告
  python fix_encoding.py --apply    # 实际修复 (自动 .bak 备份)
"""
import os
import re
import sys
import json
import shutil

ROOT = os.path.dirname(os.path.abspath(__file__))

TEXT_EXT = {
    '.md', '.txt', '.sql', '.java', '.kt', '.ts', '.tsx', '.js', '.jsx',
    '.vue', '.json', '.xml', '.yml', '.yaml', '.properties', '.html',
    '.css', '.scss', '.less', '.lst', '.dumpstream', '.gradle', '.conf',
    '.sh', '.bat', '.ps1', '.py', '.log', '.csv', '.toml', '.ini',
}

SKIP_DIRS = {
    'node_modules', '.git', 'target', 'build', 'dist', '.gradle',
    '.idea', '.vscode', '.next', 'coverage', '.workbuddy', '__pycache__',
    '.svn', 'bin', 'obj', 'out', '.cache',
}

SELF = {os.path.abspath(os.path.join(ROOT, 'fix_encoding.py'))}

# 本工具自身产生的报告/文档(内含乱码样本, 不参与修复)
GENERATED = {os.path.abspath(os.path.join(ROOT, n)) for n in
             ('fix_encoding_report.json', '乱码修复报告.md')}

# 汉字
CJK = re.compile(r'[\u4e00-\u9fff]')
# 汉字 + 中文标点/全角符号 (标点在 mojibake 中同样常见, 如 '，' '。')
CJK_WIDE = re.compile(r'[\u4e00-\u9fff\u3000-\u303f\uff00-\uffef\u3040-\u30ff]')

# A 类素材: 可被 cp1252 编码的非 ASCII 字符
SEG_A = re.compile(
    r'[\u0080-\u00ff\u0100-\u017f\u02c6\u02dc\u2013\u2014\u2018\u2019\u201a'
    r'\u201c\u201d\u201e\u2020\u2021\u2022\u2026\u2030\u2039\u203a\u20ac]{2,}')

# B 类素材: mojibake 后可能落在 CJK / 中文标点 / 制表符等区间,
# 统一按"连续非 ASCII"切分, 避免漏掉 '┖' 之类字符
SEG_B = re.compile(r'[^\x00-\x7f]{2,}')

# 常用汉字, 用于校验还原结果是否合理
COMMON = set('的了是在我有和就不人都一个上也很到说要去你会着没有看好自己这'
             '我们你们他们什么可以这个那个因为所以但是然后时间现在已经'
             '用户信息系统数据订单支付服务宠物商家管理状态创建更新删除'
             '名称地址电话类型内容备注编号价格数量时间日期图片描述标题'
             '姓名密码手机号邮箱性别年龄是否审核通过驳回原因')

MAX_FILE_BYTES = 80 * 1024 * 1024


def _max_bytes():
    """支持 --max-size=N(MB) 限制处理的文件体积"""
    for a in sys.argv:
        if a.startswith('--max-size='):
            return int(a.split('=', 1)[1]) * 1024 * 1024
    return MAX_FILE_BYTES


def _enc_ok(s, enc):
    try:
        s.encode(enc)
        return True
    except UnicodeEncodeError:
        return False


def _decode_tolerant(b):
    """UTF-8 解码; 若仅尾部残缺则截断, 返回 (文本, 丢失字节数) 或 (None, 0)"""
    try:
        return b.decode('utf-8'), 0
    except UnicodeDecodeError as e:
        # 只处理"错误发生在末尾"的残缺情况
        if e.end == len(b) and e.start > 0:
            try:
                return b[:e.start].decode('utf-8'), len(b) - e.start
            except UnicodeDecodeError:
                return None, 0
        return None, 0


def _mixed_bytes(seg):
    """逐字符编码: <=0xFF 走 latin1, 其余走 cp1252。

    解决混合型 mojibake: 片段里同时存在 C1 控制字符(U+0080-U+009F,
    cp1252 未定义) 与 cp1252 特有字符(U+201D/U+02C6 等, latin1 无法编码),
    导致单纯用任一种编码都会整体失败。
    """
    out = bytearray()
    for c in seg:
        o = ord(c)
        if o <= 0xFF:
            out.append(o)
        else:
            try:
                b = c.encode('cp1252')
            except UnicodeEncodeError:
                return None
            if len(b) != 1:
                return None
            out.extend(b)
    return bytes(out)


def fix_seg_a(seg):
    """A 类: cp1252/latin1 误读"""
    cands = []
    for enc in ('cp1252', 'latin1'):
        if _enc_ok(seg, enc):
            cands.append((seg.encode(enc), enc))
    mb = _mixed_bytes(seg)
    if mb is not None:
        cands.append((mb, 'mixed'))

    for b, enc in cands:
        s, lost = _decode_tolerant(b)
        if s is None or lost > 2:
            continue
        if CJK_WIDE.search(s) and not re.search(r'[\x00-\x08\x0b\x0c\x0e-\x1f]', s):
            return s, enc, lost
    return None


def fix_seg_b(seg):
    """B 类: GBK 误读 (UTF-8 字节被当 GBK 解出来的汉字串)"""
    for enc in ('gb18030', 'gbk'):
        if not _enc_ok(seg, enc):
            continue
        s, lost = _decode_tolerant(seg.encode(enc))
        if s is None or lost > 2:
            continue
        cjk = CJK_WIDE.findall(s)
        if not cjk:
            continue
        # 校验: 还原后常用字占比应显著高于还原前
        before = sum(1 for c in seg if c in COMMON)
        after = sum(1 for c in s if c in COMMON)
        if after <= before and len(cjk) < 2:
            continue
        if re.search(r'[\x00-\x08\x0b\x0c\x0e-\x1f]', s):
            continue
        return s, enc, lost
    return None


BAD_CHARS = re.compile(r'[\x00-\x08\x0b\x0c\x0e-\x1f\u0370-\u03ff]')


def _score(seg, s, lost):
    """给候选修复结果打分, 越高越好; 返回 None 表示不可接受"""
    cjk = len(CJK_WIDE.findall(s))
    if cjk == 0:
        return None
    if BAD_CHARS.search(s):
        return None
    # 原片段里已有的常用字越多, 越可能是正常中文(不该改)
    common_before = sum(1 for c in seg if c in COMMON)
    common_after = sum(1 for c in s if c in COMMON)
    # 正常中文里若含常用字, 要求修复后常用字不减少
    if common_before >= 2 and common_after < common_before:
        return None
    return cjk * 2 + common_after * 3 - lost * 6


def fix_mojibake(text):
    """修复文本中的 A/B 两类乱码片段, 返回 (新文本, 统计, 样本, 残缺)"""
    stats = {'A': 0, 'B': 0}
    samples = []
    lossy = []
    out = []
    consumed = 0

    for m in SEG_B.finditer(text):
        start, end = m.start(), m.end()
        if start < consumed:
            continue
        seg = m.group()
        # 依次尝试 A(cp1252/latin1) 与 B(gbk), 取评分更高者
        best = None
        for kind, fn in (('A', fix_seg_a), ('B', fix_seg_b)):
            r = fn(seg)
            if r is None:
                continue
            s, enc, lost = r
            sc = _score(seg, s, lost)
            if sc is None:
                continue
            if best is None or sc > best[0]:
                best = (sc, s, kind, enc, lost)
        if best is None:
            continue
        _, s, kind, enc, lost = best
        out.append(text[consumed:start])
        out.append(s)
        consumed = end
        stats[kind] += 1
        if lost:
            lossy.append({'before': seg[:70], 'after': s[:70],
                          'lost_bytes': lost})
        if len(samples) < 6:
            samples.append({'kind': kind, 'enc': enc,
                            'before': seg[:70], 'after': s[:70],
                            'lost': lost})
    out.append(text[consumed:])
    return ''.join(out), stats, samples, lossy


def process_file(path, apply=False):
    with open(path, 'rb') as f:
        raw = f.read()
    if not raw:
        return None

    # C 类: 整文件非 UTF-8
    try:
        text = raw.decode('utf-8')
        is_utf8 = True
    except UnicodeDecodeError:
        is_utf8 = False
        for enc in ('gb18030', 'gbk', 'big5'):
            try:
                text = raw.decode(enc)
            except UnicodeDecodeError:
                continue
            if apply:
                shutil.copy2(path, path + '.bak')
                with open(path, 'w', encoding='utf-8', newline='') as f:
                    f.write(text)
            return {'path': path, 'type': 'C', 'enc': enc,
                    'count': len(CJK.findall(text)),
                    'samples': [{'kind': 'C', 'enc': enc,
                                 'before': text[:70].replace('\n', ' '),
                                 'after': '(整文件转码为 UTF-8)'}]}

    if not is_utf8:
        return None  # 无法解码, 跳过

    new_text, stats, samples, lossy = fix_mojibake(text)
    total = stats['A'] + stats['B']
    if total == 0:
        return None
    if apply:
        shutil.copy2(path, path + '.bak')
        # 保留原有 BOM 与换行
        with open(path, 'w', encoding='utf-8', newline='') as f:
            f.write(new_text)
    return {'path': path, 'type': f"A={stats['A']},B={stats['B']}",
            'enc': 'utf-8', 'count': total, 'samples': samples,
            'lossy': lossy}


def main():
    apply = '--apply' in sys.argv
    results = []
    scanned = 0
    for dirpath, dirnames, filenames in os.walk(ROOT):
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
        for fn in filenames:
            if os.path.splitext(fn)[1].lower() not in TEXT_EXT:
                continue
            full = os.path.join(dirpath, fn)
            if os.path.abspath(full) in SELF or os.path.abspath(full) in GENERATED:
                continue
            if fn.endswith('.bak'):
                continue
            try:
                if os.path.getsize(full) > _max_bytes():
                    print(f'  [跳过-过大] {os.path.relpath(full, ROOT)}')
                    continue
            except OSError:
                continue
            scanned += 1
            r = process_file(full, apply=apply)
            if r:
                results.append(r)

    results.sort(key=lambda x: -x['count'])
    report = os.path.join(ROOT, 'fix_encoding_report.json')
    with open(report, 'w', encoding='utf-8') as f:
        json.dump({'scanned': scanned, 'fixed': results}, f, ensure_ascii=False, indent=2)

    mode = '已修复' if apply else '待修复(dry-run)'
    print(f'\n扫描文件数: {scanned}')
    print(f'{mode}文件数: {len(results)}')
    for r in results:
        print(f"\n  [{r['type']}] {os.path.relpath(r['path'], ROOT)}  共 {r['count']} 处")
        for s in r['samples'][:4]:
            flag = f"  <尾部残缺{s.get('lost')}字节>" if s.get('lost') else ''
            print(f"      {s['kind']}/{s['enc']}: {s['before'][:60]!r}")
            print(f"        ->        {s['after'][:60]!r}{flag}")
        for s in r.get('lossy', []):
            print(f"      [需注意] 丢失 {s['lost_bytes']} 字节: "
                  f"{s['before'][:40]!r} -> {s['after'][:40]!r}")
    print(f'\n报告: {report}')
    if not apply:
        print('\n确认无误后执行: python fix_encoding.py --apply')


if __name__ == '__main__':
    main()
