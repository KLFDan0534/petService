/**
 * 从后端 Java 实体/DTO 抽取字段级数据模型
 * 输出 spec-out/08_DATA_MODELS.md + models.ts
 *
 * 用法: node tools/gen_models.cjs
 */
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..', '..');   // petService 根
const OUT = path.resolve(__dirname, '..', 'spec-out');

const MODULES = ['pet-business', 'pet-system', 'pet-common'];

function walk(dir, out = []) {
  if (!fs.existsSync(dir)) return out;
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) {
      if (e.name === 'target' || e.name === 'test') continue;
      walk(p, out);
    } else if (e.name.endsWith('.java')) out.push(p);
  }
  return out;
}

/** 解析一个 Java 类的字段 */
function parseJava(file) {
  const s = fs.readFileSync(file, 'utf8');
  const lines = s.split('\n');
  const cls = (s.match(/public\s+(?:final\s+)?class\s+(\w+)/) || [])[1];
  if (!cls) return null;

  // 类注释
  const head = s.slice(0, s.indexOf('class ' + cls));
  const clsDoc = (head.match(/\/\*\*([\s\S]*?)\*\/\s*$/) || [])[1];
  const clsComment = clsDoc ? clsDoc.replace(/^\s*\*\s?/gm, '').replace(/\n{2,}/g, ' ').trim().slice(0, 160) : '';

  const fields = [];
  for (let i = 0; i < lines.length; i++) {
    const m = lines[i].match(/^\s*private\s+([A-Za-z0-9_<>,\[\]\.\s]+?)\s+(\w+)\s*(?:=[^;]*)?;/);
    if (!m) continue;
    const type = m[1].trim();
    const name = m[2];

    // 向上找注释
    let comment = '';
    for (let j = i - 1; j >= 0 && j > i - 8; j--) {
      const l = lines[j].trim();
      if (l.startsWith('*/')) {           // 多行注释块
        const block = [];
        for (let k = j; k >= 0 && k > j - 8; k--) {
          const t = lines[k].trim();
          if (t.startsWith('/**') || t.startsWith('/*')) break;
          if (t.startsWith('*')) block.unshift(t.replace(/^\*\s?/, ''));
        }
        comment = block.join(' ').trim();
        break;
      }
      if (l.startsWith('//')) { comment = l.replace(/^\/\/\s?/, ''); break; }
      if (l.startsWith('@')) continue;     // 跳过注解
      if (l === '') continue;
      break;
    }
    fields.push({ type, name, comment: comment.slice(0, 80) });
  }
  return { cls, file, clsComment, fields };
}

const files = [];
for (const m of MODULES) {
  for (const f of walk(path.join(ROOT, m, 'src', 'main', 'java'))) {
    // ★ 全量扫描：只要类里有 _wsh 字段就纳入（VO/Request/Response 可能在任意包）
    const txt = fs.readFileSync(f, 'utf8');
    if (!/_wsh\s*;/.test(txt)) continue;
    if (!/public\s+(?:final\s+)?class\s+\w+/.test(txt)) continue;
    files.push(f);
  }
}

const classes = files.map(parseJava).filter(Boolean);
// 按包分组
const byPkg = new Map();
for (const c of classes) {
  const pkg = path.relative(ROOT, c.file).replace(/\\/g, '/').split('/src/main/java/')[1].split('/').slice(0, -1).join('.');
  const key = pkg.split('.').slice(0, 3).join('.');
  if (!byPkg.has(key)) byPkg.set(key, []);
  byPkg.get(key).push({ ...c, pkg });
}

const L = [];
L.push('# 数据模型契约（后端字段级）');
L.push('');
L.push('> 由 Java `entity` / `dto` / `vo` 自动抽取。**字段名必须逐字一致**（注意 `_wsh` 后缀是项目约定，不是笔误）。');
L.push('> 复刻版的 mock 数据必须按本卷构造：页面上出现的每个值，都要能追溯到这里的某个字段或由这些字段计算得出。');
L.push('> 禁止自造字段、禁止改名、禁止用「差不多」的字段替代。');
L.push('');
L.push(`> 共 ${classes.length} 个模型，覆盖 ${byPkg.size} 个领域包。`);
L.push('');

for (const [pkg, arr] of [...byPkg.entries()].sort()) {
  L.push(`## ${pkg}`);
  L.push('');
  for (const c of arr.sort((a, b) => a.cls.localeCompare(b.cls))) {
    if (!c.fields.length) continue;
    L.push(`### \`${c.cls}\``);
    if (c.clsComment) L.push(`> ${c.clsComment}`);
    L.push('');
    L.push('| 字段 | 类型 | 说明 |');
    L.push('|---|---|---|');
    for (const f of c.fields) {
      L.push(`| \`${f.name}\` | ${f.type.replace(/\|/g, '/')} | ${(f.comment || '').replace(/\|/g, '/')} |`);
    }
    L.push('');
  }
}

fs.writeFileSync(path.join(OUT, '08_DATA_MODELS.md'), L.join('\n'));

/* ---------- 同时生成 TS 类型 ---------- */
const TS = [];
TS.push('/**');
TS.push(' * 后端数据模型（由 Java 实体/DTO/VO 机械生成，字段名逐字对应）');
TS.push(' * ★ 不要手改：改字段等于改契约。新增字段请回到后端加。');
TS.push(' */');
TS.push('');
TS.push('export type Long = number;');
TS.push('export type Integer = number;');
TS.push('export type BigDecimal = number;');
TS.push('export type LocalDateTime = string;');
TS.push('');
function tsType(j) {
  const t = j.replace(/^java\.(util|math|time)\./, '').replace(/<.*>/, '');
  if (/^String$/.test(t)) return 'string';
  if (/^(Long|Integer|BigDecimal|Double|Float|Short|Byte|int|long|double)$/.test(t)) return 'number';
  if (/^Boolean$|^boolean$/.test(t)) return 'boolean';
  if (/^LocalDateTime$|^LocalDate$|^Date$|^Timestamp$/.test(t)) return 'string';
  if (/^(List|Set|Collection)$/.test(t)) return 'any[]';
  if (/^Map$/.test(t)) return 'Record<string, any>';
  return 'any';
}
const seen = new Set();
for (const c of classes) {
  if (seen.has(c.cls) || !c.fields.length) continue;
  seen.add(c.cls);
  TS.push(`/** ${c.pkg} */`);
  TS.push(`export interface ${c.cls} {`);
  for (const f of c.fields) {
    if (f.comment) TS.push(`  /** ${f.comment.replace(/\*\//g, '* /')} */`);
    TS.push(`  ${f.name}?: ${tsType(f.type)};`);
  }
  TS.push('}');
  TS.push('');
}
fs.writeFileSync(path.join(OUT, 'models.ts'), TS.join('\n'));
console.log('TS 类型:', seen.size, '个 |', (fs.statSync(path.join(OUT, 'models.ts')).size / 1024).toFixed(0), 'KB');
console.log('模型:', classes.length, '| 包:', byPkg.size, '| 输出:', (fs.statSync(path.join(OUT, '08_DATA_MODELS.md')).size / 1024).toFixed(0), 'KB');
console.log('字段最多的 5 个:');
classes.sort((a, b) => b.fields.length - a.fields.length).slice(0, 5).forEach((c) => console.log('  ', c.cls, c.fields.length));
