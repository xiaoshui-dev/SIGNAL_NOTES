import { reactive, ref } from 'vue';
import { apiRequest } from './api';

export const site = reactive({
  siteName: '脉冲笔记', siteShortName: 'SIGNAL NOTES', siteTagline: '把复杂技术讲清楚',
  heroEyebrow: 'TECH NOTES / 2026', heroTitle: '把复杂技术讲清楚',
  heroSummary: '关于 AI、软件工程、系统与数字世界的长期记录。少一点噪声，多一点真正有用的理解。',
  aboutTitle: '写给愿意慢下来理解技术的人。', aboutLead: '脉冲笔记关注技术背后的结构、取舍和真实影响。',
  aboutBody: '这里没有追逐热点的速报，只有经过实践、验证和反思之后的记录。',
  contactTitle: '把问题、想法或合作方向告诉我。', contactIntro: '如果你发现文章中的错误，或者有值得长期讨论的技术问题，欢迎留下消息。',
  publicEmail: 'hello@signal-notes.local', replyPromise: '通常会在 3 个工作日内回复。',
  privacyContent: '我们只收集完成联系、评论和订阅所必需的信息，不出售个人数据。',
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
