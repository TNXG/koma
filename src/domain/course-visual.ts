/**
 * 课程视觉身份：按课程名关键字查映射表挑学科图标，色调随表定；
 * 表里没命中的课程回退到书本图标，色调稳定取自名称哈希。
 * 图标只做点缀（28px 线条），不做色块底、不做卡片装饰。
 */

import type { FunctionalComponent, SVGAttributes } from "vue";
import BankLine from "~icons/mingcute/bank-line";
import Book4Line from "~icons/mingcute/book-4-line";
import BriefcaseLine from "~icons/mingcute/briefcase-line";
import ChipLine from "~icons/mingcute/chip-line";
import CodeLine from "~icons/mingcute/code-line";
import FitnessLine from "~icons/mingcute/fitness-line";
import Flask2Line from "~icons/mingcute/flask-2-line";
import FormulaLine from "~icons/mingcute/formula-line";
import HeartLine from "~icons/mingcute/heart-line";
import PaletteLine from "~icons/mingcute/palette-line";
import TranslateLine from "~icons/mingcute/translate-line";

export type CourseTone = "blue" | "amber" | "rose" | "green" | "violet" | "orange" | "teal";

export type CourseIcon = FunctionalComponent<SVGAttributes>;

export interface CourseIconRule {
  /** 课程名关键字，全部小写；匹配时课程名也转小写后做子串判断 */
  keywords: string[];
  icon: CourseIcon;
  tone: CourseTone;
}

/** 关键字 → 学科图标映射表；按表序匹配，先命中先取 */
export const COURSE_ICON_RULES: CourseIconRule[] = [
  {
    keywords: ["英语", "english", "外语", "日语", "俄语"],
    icon: TranslateLine,
    tone: "blue",
  },
  {
    keywords: ["数学", "高数", "代数", "统计", "概率", "微积分"],
    icon: FormulaLine,
    tone: "amber",
  },
  {
    keywords: ["体育", "健康", "运动"],
    icon: FitnessLine,
    tone: "rose",
  },
  {
    keywords: [
      "程序",
      "编程",
      "计算机",
      "软件",
      "数据",
      "网络",
      "python",
      "java",
      "web",
      "前端",
    ],
    icon: CodeLine,
    tone: "teal",
  },
  {
    keywords: ["物理", "化学", "生物", "实验"],
    icon: Flask2Line,
    tone: "green",
  },
  {
    keywords: [
      "思想",
      "政治",
      "马克思",
      "毛泽东",
      "形势",
      "历史",
      "法治",
      "习近平",
    ],
    icon: BankLine,
    tone: "orange",
  },
  {
    keywords: ["设计", "美术", "艺术", "音乐", "绘画"],
    icon: PaletteLine,
    tone: "violet",
  },
  {
    keywords: ["就业", "创业", "职业", "管理", "经济"],
    icon: BriefcaseLine,
    tone: "orange",
  },
  {
    keywords: ["电子", "电路", "机械", "自动化", "嵌入式", "单片机"],
    icon: ChipLine,
    tone: "teal",
  },
  {
    keywords: ["心理"],
    icon: HeartLine,
    tone: "rose",
  },
];

const FALLBACK_TONES: CourseTone[] = [
  "blue",
  "violet",
  "green",
  "orange",
  "teal",
  "amber",
];

export function courseVisual(courseName: string): { icon: CourseIcon; tone: CourseTone } {
  const name = courseName.trim().toLowerCase();
  const hit = COURSE_ICON_RULES.find(rule => rule.keywords.some(keyword => name.includes(keyword)));
  if (hit)
    return { icon: hit.icon, tone: hit.tone };
  let hash = 0;
  for (const ch of courseName)
    hash = (hash * 31 + ch.codePointAt(0)!) >>> 0;
  return { icon: Book4Line, tone: FALLBACK_TONES[hash % FALLBACK_TONES.length] };
}
