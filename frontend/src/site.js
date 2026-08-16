import { reactive, ref } from 'vue';
import { apiRequest } from './api';

export const site = reactive({
  siteName: '脉冲笔记', siteShortName: 'SIGNAL NOTES', siteTagline: '把复杂技术讲清楚',
  heroEyebrow: 'TECH NOTES / 2026', heroTitle: '把复杂技术讲清楚',
  heroSummary: '关于 AI、软件工程、系统与数字世界的长期记录。少一点噪声，多一点真正有用的理解。',
  blogTitle: '技术笔记\n与长期观察', blogIntro: '记录软件、AI、系统与工具背后的结构和取舍。每一篇文章都尽量给出可以验证的上下文，而不只是结论。',
  landingTopicsTitle: '观察技术，也观察技术如何改变人。', landingTopicsIntro: '从底层系统到日常工具，用真实项目和长期实践拆解变化。',
  featuredTitle: '从这里开始读', landingStatusTopics: 'AI · SYSTEMS · CODE',
  searchTitle: '搜索', searchIntro: '从标题、摘要、分类、标签和正文中查找。',
  categoriesTitle: '分类与标签', categoriesIntro: '沿着长期主题浏览，而不是被时间线推着走。',
  aboutTitle: '写给愿意慢下来理解技术的人。', aboutLead: '脉冲笔记关注技术背后的结构、取舍和真实影响。',
  aboutBody: '这里没有追逐热点的速报，只有经过实践、验证和反思之后的记录。',
  aboutPrinciple1Title: '来自实践', aboutPrinciple1Body: '给出可验证的上下文。',
  aboutPrinciple2Title: '承认边界', aboutPrinciple2Body: '区分事实与判断。',
  aboutPrinciple3Title: '长期可读', aboutPrinciple3Body: '减少热点语境。',
  contactTitle: '把问题、想法或合作方向告诉我。', contactIntro: '如果你发现文章中的错误，或者有值得长期讨论的技术问题，欢迎留下消息。',
  publicEmail: 'hello@signal-notes.local', replyPromise: '通常会在 3 个工作日内回复。',
  privacyContent: '我们只收集完成联系、评论和订阅所必需的信息，不出售个人数据。',
  privacyTitle: '隐私与使用说明', privacyUpdatedAt: '2026-08-16',
  privacyCommentsPolicy: '评论默认进入审核队列。垃圾信息、人身攻击、违法内容和泄露他人隐私的内容会被拒绝或删除。',
  privacyRights: '需要查询、更正或删除订阅和评论数据时，请通过联系页面发送请求。',
  termsContent: '文章仅用于知识交流，不构成法律、医疗或投资建议。引用时应保留作者与原文链接，不得歪曲原意。',
  contactLicenseNote: '商业使用请提前联系获得书面许可。', archiveTitle: '时间归档',
  notFoundTitle: '这个页面没有找到。', notFoundDescription: '链接可能已经改变，或者文章暂时下线。',
  authorName: '林默', authorRole: '软件工程师 / 独立写作者', authorBio: '关注 AI 系统、软件架构与数字工具，喜欢把复杂问题拆成可以验证的步骤。',
  footerDescription: '关于 AI、系统与数字世界的独立技术博客。', copyrightText: '© 2026 Signal Notes', licenseText: '内容以 CC BY-NC-SA 4.0 发布',
  subscribeTitle: '每两周，收到一封有用的信。', subscribeDescription: '只发送新文章和真正值得分享的链接，不追踪打开行为。',
});
export const siteLoading = ref(false);
let sitePromise;
export async function loadSite() {
  if (sitePromise) return sitePromise;
  siteLoading.value = true;
  sitePromise = apiRequest('/site').then((value) => { Object.assign(site, value || {}); return site; }).catch((error) => { sitePromise = null; throw error; }).finally(() => { siteLoading.value = false; });
  return sitePromise;
}
export function useSite() { return { site, siteLoading, loadSite }; }
