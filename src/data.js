export const authors = [
  {
    id: 'lin',
    name: '林默',
    initials: 'LM',
    role: '软件工程师 / 独立写作者',
    bio: '关注 AI 系统、软件架构与数字工具，喜欢把复杂问题拆成可以验证的步骤。',
  },
];

export const posts = [
  {
    id: 'post-001',
    slug: 'building-reliable-ai-systems',
    title: '构建可靠 AI 系统：从一次演示走向真实生产',
    excerpt: '模型能力只是起点。真正的 AI 产品需要评估、可观测性、降级策略和人类反馈共同构成可靠性边界。',
    cover: '/assets/hero-circuit.jpg',
    coverAlt: '电路板上的芯片与精密电子元件',
    category: '系统设计',
    tags: ['人工智能', '可靠性', '工程实践'],
    authorId: 'lin',
    publishedAt: '2026-08-15',
    updatedAt: '2026-08-15',
    readMinutes: 12,
    featured: true,
    views: 4826,
    content: `## 演示成功不等于系统可靠

一个模型在精心准备的提示词下给出漂亮答案，证明的是能力上限，不是产品下限。生产环境里的输入分布更复杂，用户也不会按照我们设想的方式提问。

可靠 AI 系统首先要承认不确定性。与其问“模型是否聪明”，更应该问：当模型不知道、误解或外部服务失败时，系统会发生什么？

> 可靠性不是让错误消失，而是让错误可见、可控，并且可以恢复。

## 建立可复现的评估集

评估集应该来自真实任务，而不是只由开发者编写的理想样本。最小评估集至少包含正常输入、边界输入、对抗输入和历史故障案例。

\`\`\`js
const result = await evaluate({
  dataset: 'support-cases-v3',
  dimensions: ['accuracy', 'groundedness', 'latency'],
  threshold: 0.86,
});
\`\`\`

每次修改提示词、模型或检索策略，都应运行同一组评估。这样团队讨论的是可比较的变化，而不是几条看起来不错的示例。

## 让链路可观测

一次请求通常经过检索、排序、提示词组装、模型调用、工具执行和结果校验。只记录最终错误无法定位问题，需要为每一段链路留下结构化信息。

| 信号 | 作用 | 注意事项 |
| --- | --- | --- |
| 延迟 | 发现模型或工具的性能变化 | 分阶段记录，不只记录总耗时 |
| Token | 控制成本与上下文膨胀 | 区分输入、输出和缓存命中 |
| 引用命中 | 判断回答是否基于可靠来源 | 记录来源版本和检索分数 |
| 用户反馈 | 找出评估集遗漏 | 不把点赞直接等同于准确性 |

## 设计明确的降级路径

外部模型超时，可以返回检索结果；检索不可用，可以提示用户稍后再试；高风险操作则应要求人工确认。降级策略必须在上线前演练，而不是故障发生时临时决定。

## 把反馈变成系统资产

用户反馈、人工审核和线上故障最终都应该进入评估集。这样每次问题都能提高系统的最低表现，而不是只修复一次性的表面症状。

可靠 AI 产品不是一个模型接口，而是一套持续测量、约束和改进的工程系统。`,
  },
  {
    id: 'post-002',
    slug: 'local-first-software',
    title: 'Local-first 软件为什么重新受到关注',
    excerpt: '在云协作成为默认之后，离线可用、数据所有权和即时响应又重新成为产品竞争力。',
    cover: '/assets/workstation.jpg',
    coverAlt: '桌面上的笔记本电脑与开发环境',
    category: '软件工程',
    tags: ['Local-first', '产品设计', '数据'],
    authorId: 'lin',
    publishedAt: '2026-08-02',
    updatedAt: '2026-08-06',
    readMinutes: 8,
    featured: true,
    views: 3214,
    content: `## 云优先解决了什么

云端应用让多设备同步、协作和发布变得简单。它也带来了新的默认假设：网络必须在线，服务必须存在，用户的数据通常只能通过应用本身访问。

Local-first 并不是否定云，而是把本地副本重新视为主要数据，把云端当作同步和协作层。

## 体验首先来自即时响应

用户操作直接写入本地数据，界面无需等待网络往返。同步发生在后台，冲突则通过可解释的规则处理。

这种架构的困难不在读取，而在多个副本如何合并。CRDT、操作日志和版本向量提供了不同的实现路径，选择取决于数据结构和产品场景。

## 数据所有权是一种产品能力

当用户可以导出、备份并使用开放格式读取自己的内容，产品建立的是长期信任。即使服务短暂不可用，核心工作也不会停止。

## 从小范围开始

不必一次重写整个系统。可以先让草稿、最近文档或个人偏好具备离线能力，再逐步验证同步和冲突策略。`,
  },
  {
    id: 'post-003',
    slug: 'reading-source-code',
    title: '读懂陌生代码库的五条路径',
    excerpt: '从运行入口、数据流、测试、变更历史和失败路径出发，比从第一个文件顺序阅读更有效。',
    cover: '/assets/code-screen.jpg',
    coverAlt: '屏幕上显示的软件代码',
    category: '软件工程',
    tags: ['代码阅读', '工程效率', '方法论'],
    authorId: 'lin',
    publishedAt: '2026-07-21',
    updatedAt: '2026-07-21',
    readMinutes: 6,
    featured: true,
    views: 2819,
    content: `## 先让系统运行起来

运行命令、环境变量和启动日志会告诉你系统的真实边界。记录一次请求从入口到响应经过的模块，比阅读目录名更可靠。

## 追踪一条数据流

选择一个常用功能，跟踪输入如何被校验、转换、保存和返回。过程中只记录关键类型和所有权边界，暂时忽略工具函数细节。

## 从测试寻找契约

测试常常比注释更接近真实行为。失败案例尤其重要，它们揭示开发者最担心哪些边界。

## 阅读最近的变更

提交历史解释了代码为什么是现在的样子。查看一个模块最近三次修改，通常能找到需求变化和曾经踩过的坑。

## 主动制造失败

关闭依赖、输入错误数据或让请求超时，观察系统如何表现。错误路径能够快速暴露模块之间真正的依赖关系。`,
  },
  {
    id: 'post-004',
    slug: 'observability-without-noise',
    title: '没有噪声的可观测性：只记录能够行动的信号',
    excerpt: '日志越多不代表系统越透明。好的可观测性应该帮助团队更快地判断、定位和恢复。',
    cover: '/assets/server-room.jpg',
    coverAlt: '整齐排列的数据中心服务器机架',
    category: '系统设计',
    tags: ['可观测性', 'SRE', '日志'],
    authorId: 'lin',
    publishedAt: '2026-07-08',
    updatedAt: '2026-07-10',
    readMinutes: 9,
    featured: false,
    views: 1938,
    content: `## 信号必须对应行动

每一个告警都应该回答：谁需要处理、最晚何时处理、可以从哪里开始排查。无法行动的告警只是新的噪声来源。

## 先定义服务目标

从用户可感知的成功率和延迟出发，再决定需要哪些指标。指标是服务目标的证据，不是展示系统繁忙程度的装饰。

## 为上下文而记录

结构化日志要包含请求标识、关键业务实体和结果状态，但要避免敏感信息。错误信息应足以复现问题，同时保持数据最小化。`,
  },
  {
    id: 'post-005',
    slug: 'small-tools-compound',
    title: '小工具的复利：建立自己的自动化工作台',
    excerpt: '真正节省时间的不是一次大型重构，而是把每天重复的十秒钟逐渐交给工具。',
    cover: '/assets/workstation.jpg',
    coverAlt: '用于编程和写作的个人工作台',
    category: '数字生活',
    tags: ['自动化', '工具', '工作流'],
    authorId: 'lin',
    publishedAt: '2026-06-19',
    updatedAt: '2026-06-19',
    readMinutes: 7,
    featured: false,
    views: 2241,
    content: `## 从重复动作开始

不要先寻找“最强工具”，先记录一周内重复出现的动作。重命名文件、创建项目、整理会议笔记，都是可以逐步自动化的入口。

## 工具要容易退出

个人自动化最大的风险是几年后只有自己知道如何维护。使用文本格式、清晰配置和普通脚本，可以让工具在不再合适时被轻松替换。

## 衡量维护成本

一个每月节省五分钟却需要持续修复的脚本没有复利。自动化应该减少认知负担，而不是制造新的值班任务。`,
  },
  {
    id: 'post-006',
    slug: 'ai-interface-is-a-contract',
    title: 'AI 界面也是契约：如何设计可预期的人机协作',
    excerpt: '当输出具有不确定性，界面需要让用户理解系统在做什么、可以相信什么以及如何纠正。',
    cover: '/assets/ai-interface.jpg',
    coverAlt: '显示抽象人工智能界面的笔记本电脑',
    category: '人工智能',
    tags: ['人工智能', '交互设计', '产品设计'],
    authorId: 'lin',
    publishedAt: '2026-05-30',
    updatedAt: '2026-06-02',
    readMinutes: 10,
    featured: false,
    views: 3672,
    content: `## 不确定性必须可见

AI 产品不应该用确定的语气掩盖不确定的结果。来源、时间范围、工具调用状态和未完成部分，都应该在界面里有明确位置。

## 把等待变成过程

长任务需要显示正在进行的阶段，而不只是一个无限旋转的加载图标。用户应该知道任务是否仍在推进，以及能否安全离开。

## 纠正比重试更重要

好的界面允许用户修改某一步，而不是每次从头生成。编辑输入、锁定正确片段和比较版本，能够让协作逐渐收敛。`,
  },
];

export const categories = [...new Set(posts.map((post) => post.category))].map((name) => ({
  name,
  count: posts.filter((post) => post.category === name).length,
  description: {
    人工智能: '模型、产品、评估与人机协作。',
    软件工程: '代码、架构、团队和工程方法。',
    系统设计: '可靠性、性能与基础设施。',
    数字生活: '工具、阅读与个人工作流。',
  }[name],
}));

export const tags = [...new Set(posts.flatMap((post) => post.tags))]
  .map((name) => ({ name, count: posts.filter((post) => post.tags.includes(name)).length }))
  .sort((a, b) => b.count - a.count || a.name.localeCompare(b.name, 'zh-CN'));

export function getAuthor(id) {
  return authors.find((author) => author.id === id) || authors[0];
}

export function formatDate(value) {
  return new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' }).format(new Date(`${value}T00:00:00`));
}

export function compactNumber(value) {
  return new Intl.NumberFormat('zh-CN', { notation: 'compact', maximumFractionDigits: 1 }).format(value);
}
