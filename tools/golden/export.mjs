// 用上游网页 index.html 自己的 JS 解析一遍正文，导出 golden JSON，给 Kotlin 端逐字段对拍。
// 解析、拆块的函数原样从 index.html 里抠出来跑（和上游 tools/check-split.mjs 同一个做法），
// 不在这里重写——重写了就成了拿自己的理解对自己的理解。
// 只有筛选谓词是照 apply() 里那十几行抄的（原函数和 DOM 绑在一起没法单独跑），改动时要对照着改。
//
// 用法：node tools/golden/export.mjs <upstream 目录> <输出 json>
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { resolve, dirname } from 'node:path';
import vm from 'node:vm';

const [up, out] = process.argv.slice(2);
if (!up || !out) { console.error('用法：node export.mjs <upstream> <out.json>'); process.exit(2); }
const read = p => readFileSync(resolve(up, p), 'utf8').replace(/\r\n/g, '\n');
const html = read('index.html');

const grab = (re, what) => {
  const m = html.match(re);
  if (!m) { console.error(`index.html 里找不到 ${what}`); process.exit(1); }
  return m[0];
};
const code = [
  grab(/^const COST_W = .*$/m, 'COST_W'),
  grab(/\/\/ <split-rules>[\s\S]*?\/\/ <\/split-rules>/, '<split-rules> 段'),
  grab(/^function parseReadme\(md\)\{[\s\S]*?\n\}/m, 'parseReadme()'),
  'this.parseReadme = parseReadme; this.splitGain = splitGain; this.splitSrc = splitSrc;',
].join('\n');
const ctx = {};
vm.runInNewContext(code, ctx);

// 文件清单和拼接方式照 init()
const readme = read('README.md');
const files = [...new Set(Array.from(readme.matchAll(/\]\((book\/[^)]+\.md)\)/g), m => m[1]))].sort();
const sections = ctx.parseReadme(files.map(read).join('\n\n'));

const entries = [];
const outSections = sections.map(s => ({
  n: s.n, title: s.title, intro: s.intro,
  entries: s.entries.map(e => {
    entries.push(e);
    return {
      ...e,
      gainChunks: ctx.splitGain(e.gain),
      noteChunks: ctx.splitGain(e.note),
      srcParts: ctx.splitSrc(e.src),
    };
  }),
}));

// ---------- 筛选：照 apply() 与 fitsMe() ----------
const DIMS = ['sec', 'who', 'ratio', 'lens', 'grade', 'money', 'time', 'will'];
function run(st) {
  const s = Object.fromEntries(DIMS.map(d => [d, new Set(st[d] ?? [])]));
  const terms = (st.q ?? '').toLowerCase().split(/\s+/).filter(Boolean);
  const fitsMe = e => {
    if (!s.who.size || !e.who.length) return true;
    const has = v => s.who.has(v) || (v === '35岁' && s.who.has('60岁'));
    return e.who.some(g => g.every(has));
  };
  return entries.filter(e => {
    if (s.ratio.size && !s.ratio.has(e.ratio)) return false;
    if (s.lens.size && !s.lens.has(e.lens)) return false;
    if (s.grade.size && !s.grade.has(e.grade)) return false;
    if (s.money.size && !s.money.has(e.money)) return false;
    if (s.time.size && !s.time.has(e.time)) return false;
    if (s.will.size && !s.will.has(e.will)) return false;
    if (!fitsMe(e)) return false;
    if (st.dispute && !e.dispute) return false;
    if (st.todo && !e.todo) return false;
    if (terms.length && !terms.every(t => e.hay.includes(t))) return false;
    if (s.sec.size && !s.sec.has(e.sec)) return false;
    return true;
  }).map(e => `${e.sec}-${e.n}`);
}
const FILTERS = [
  { name: '全部' },
  { name: '单节', sec: ['13'] },
  { name: '性价比极高', ratio: ['极高'] },
  { name: '口径 钱+自由', lens: ['金钱', '自由'] },
  { name: 'A 级且不花钱', grade: ['A'], money: ['0'] },
  { name: '时间 中/多', time: ['中', '多'] },
  { name: '毅力 不用', will: ['否'] },
  { name: '人群 60岁（蕴含35岁）', who: ['60岁'] },
  { name: '人群 35岁', who: ['35岁'] },
  { name: '人群 女性+怀孕', who: ['女性', '怀孕'] },
  { name: '人群 以上都不是', who: ['无'] },
  { name: '只看争议', dispute: true },
  { name: '只看待核实', todo: true },
  { name: '关键词 疫苗', q: '疫苗' },
  { name: '多关键词 大小写', q: '  HPV   疫苗 ' },
  { name: '组合', who: ['60岁'], q: '疫苗', grade: ['A', 'B'] },
  { name: '空结果', q: '这个词不可能出现在书里zzqq' },
].map(f => ({ ...f, ids: run(f) }));

// ---------- 交叉引用：「见第 8 节第 16 条」「本节第 3 条」「第 9 节」 ----------
// NUMS、XREF_RE、xrefKeys 和链接切分用的几个正则原样从 index.html 抠出来跑。
// 下面两个循环是照抄的（原函数 renderText / xrefInto 和 DOM 绑在一起没法单独跑），改动时要对照着改：
// ① plainSegments 照 renderText：只有链接和粗体之外的普通文字才做引用识别，裸链接后的尾巴也算普通文字；
// ② xrefIn 照 xrefInto 里的匹配循环：解析不到真实条目或节的不算引用。
const xcode = [
  grab(/^const URL_RE = .*$/m, 'URL_RE'),
  grab(/^function trimUrl\(u\)\{[\s\S]*?\n\}/m, 'trimUrl()'),
  grab(/^const MD_LINK_RE = .*$/m, 'MD_LINK_RE'),
  grab(/^const BOLD_RE = .*$/m, 'BOLD_RE'),
  grab(/^const unescMd = .*$/m, 'unescMd'),
  grab(/^const LINK_RE = .*$/m, 'LINK_RE'),
  grab(/^const NUMS = .*$/m, 'NUMS'),
  grab(/^const XREF_RE = .*$/m, 'XREF_RE'),
  grab(/^const xrefKeys = [\s\S]*?\n\};/m, 'xrefKeys'),
  'this.LINK_RE = LINK_RE; this.XREF_RE = XREF_RE; this.xrefKeys = xrefKeys; this.unescMd = unescMd; this.trimUrl = trimUrl;',
].join('\n');
const x = { ITEMS: new Map(), SECS: new Map() };
vm.runInNewContext(xcode, x);
for (const s of sections) { x.SECS.set(s.n, s); for (const e of s.entries) x.ITEMS.set(`${s.n}-${e.n}`, e); }

function plainSegments(text) {
  const segs = [];
  let last = 0, m;
  x.LINK_RE.lastIndex = 0;
  while ((m = x.LINK_RE.exec(text))) {
    segs.push(text.slice(last, m.index));
    last = m.index + m[0].length;
    if (m[4] !== undefined) continue;
    if (m[3]) { segs.push(x.trimUrl(m[3])[1]); continue; }
  }
  segs.push(text.slice(last));
  return segs.filter(Boolean).map(x.unescMd);
}
function xrefIn(s, cur) {
  const out = [];
  let m;
  x.XREF_RE.lastIndex = 0;
  while ((m = x.XREF_RE.exec(s))) {
    const keys = m[5] != null
      ? String(m[5]).split('、').map(v => v.trim()).filter(v => x.SECS.has(v)).map(v => 's' + v)
      : x.xrefKeys(m[1] ?? cur, m[2] ?? m[3] ?? m[4] ?? '');
    if (!keys.length) continue;
    out.push([m[0], ...keys]);
  }
  return out;
}
const xrefsOf = (texts, cur) => texts.flatMap(t => plainSegments(t).flatMap(seg => xrefIn(seg, cur)));
let xrefCount = 0;
for (const s of outSections) {
  s.introXrefs = xrefsOf(s.intro, s.n);
  xrefCount += s.introXrefs.length;
  for (const e of s.entries) {
    // 字段顺序照 renderCard：标题、成本、说人话、收益（拆块后）、备注（拆块后）、来源（拆条后）
    e.xrefs = xrefsOf([e.title, e.cost, e.human, ...e.gainChunks, ...e.noteChunks, ...e.srcParts], e.sec);
    xrefCount += e.xrefs.length;
  }
}

const golden = { files, sectionCount: outSections.length, entryCount: entries.length, sections: outSections, filters: FILTERS, xrefCount };
mkdirSync(dirname(resolve(out)), { recursive: true });
writeFileSync(out, JSON.stringify(golden));
console.log(`golden：${files.length} 个文件，${outSections.length} 节，${entries.length} 条，${FILTERS.length} 组筛选`
  + `（${FILTERS.map(f => f.ids.length).join('/')}），${xrefCount} 处交叉引用`);
