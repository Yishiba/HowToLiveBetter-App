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

const golden = { files, sectionCount: outSections.length, entryCount: entries.length, sections: outSections, filters: FILTERS };
mkdirSync(dirname(resolve(out)), { recursive: true });
writeFileSync(out, JSON.stringify(golden));
console.log(`golden：${files.length} 个文件，${outSections.length} 节，${entries.length} 条，${FILTERS.length} 组筛选`
  + `（${FILTERS.map(f => f.ids.length).join('/')}）`);
