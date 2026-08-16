<script setup>
import DOMPurify from 'dompurify';
import { marked } from 'marked';
import { ArrowLeft, ArrowRight, CalendarDays, ChevronRight, Clock3, Copy, FolderOpen, Hash, Maximize2, X } from 'lucide-vue-next';
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import BlogHeader from '../components/BlogHeader.vue';
import BlogFooter from '../components/BlogFooter.vue';
import SharePoster from '../components/SharePoster.vue';
import { formatDate, getAuthor } from '../data';
import { apiRequest, submitComment } from '../api';
import { setArticleJsonLd, setPageSeo } from '../seo';
import { useSite } from '../site';

const route = useRoute();
const { site, loadSite } = useSite();
const post = ref(null);
const comment = ref({ name: '', content: '' });
const comments = ref([]);
const commentStatus = ref('');
const replyTo = ref(null);
const contentRef = ref(null);
const lightbox = ref({ open: false, src: '', alt: '' });
const continueFrom = ref(0);
const allPosts = ref([]);

const author = computed(() => ({ ...getAuthor(post.value?.authorId), name: post.value?.authorName || site.authorName, role: site.authorRole, bio: site.authorBio }));
const article = computed(() => {
  if (!post.value) return { html: '', headings: [] };
  const headings = [];
  const md = (post.value.content || '').replace(/^## (.+)$/gm, (_, title) => {
    const id = `section-${headings.length + 1}`;
    headings.push({ id, title });
    return `## <span id="${id}">${title}</span>`;
  });
  return { html: DOMPurify.sanitize(marked.parse(md)), headings };
});
const related = computed(() => allPosts.value.filter((item) => item.slug !== post.value?.slug && item.category === post.value?.category).slice(0, 2));
const currentIndex = computed(() => allPosts.value.findIndex((item) => item.slug === post.value?.slug));
const previous = computed(() => currentIndex.value > 0 ? allPosts.value[currentIndex.value - 1] : null);
const next = computed(() => currentIndex.value >= 0 ? allPosts.value[currentIndex.value + 1] : null);
function orderComments(list){return list.filter(item=>!item.parentId).flatMap(parent=>[parent,...list.filter(item=>item.parentId===parent.id)]);}

onMounted(async () => {
  await loadSite().catch(() => {});
  try {
    const value = await apiRequest(`/posts/${route.params.slug}`);
    if (value) post.value = value;
    const list = await apiRequest('/posts');
    allPosts.value = Array.isArray(list) ? list : [];
  } catch (error) { commentStatus.value = error.message || '文章服务暂时不可用'; }
  if (post.value) {
    setPageSeo({ title: `${post.value.title} | ${site.siteName}`, description: post.value.excerpt, canonical: new URL(`/blog/posts/${post.value.slug}`, window.location.origin).href, type: 'article' });
    setArticleJsonLd(post.value);
    continueFrom.value = Number(localStorage.getItem(`signal-reading-${post.value.slug}`) || 0);
  }
  await nextTick();
  setupArticleInteractions();
  window.addEventListener('scroll', progress, { passive: true });
  try { comments.value = orderComments(await apiRequest(`/comments?postSlug=${route.params.slug}`)); } catch (error) { comments.value = []; commentStatus.value = error.message || '评论服务暂时不可用'; }
});

onUnmounted(() => {
  document.title = `${site.siteName} | ${site.siteShortName}`;
  document.getElementById('signal-article-jsonld')?.remove();
  window.removeEventListener('scroll', progress);
  document.documentElement.style.removeProperty('--reading-progress');
});

function progress() {
  const h = document.documentElement.scrollHeight - window.innerHeight;
  const value = h ? Math.min(100, Math.max(0, window.scrollY / h * 100)) : 0;
  document.documentElement.style.setProperty('--reading-progress', `${value}%`);
  if (post.value && value > 3) localStorage.setItem(`signal-reading-${post.value.slug}`, String(Math.round(value)));
}

function setupArticleInteractions() {
  if (!contentRef.value) return;
  contentRef.value.querySelectorAll('pre').forEach((pre) => {
    if (pre.querySelector('.code-copy')) return;
    const button = document.createElement('button'); button.className = 'code-copy'; button.type = 'button'; button.textContent = '复制代码';
    button.addEventListener('click', async () => {
      const text = pre.querySelector('code')?.textContent || pre.textContent || '';
      try { await navigator.clipboard.writeText(text); button.textContent = '已复制'; setTimeout(() => { button.textContent = '复制代码'; }, 1600); }
      catch { button.textContent = '请手动复制'; setTimeout(() => { button.textContent = '复制代码'; }, 1600); }
    });
    pre.appendChild(button);
  });
  contentRef.value.querySelectorAll('img').forEach((image) => image.addEventListener('click', () => { lightbox.value = { open: true, src: image.currentSrc || image.src, alt: image.alt }; }));
}

function scrollToContinue() { document.querySelector('.article-content')?.scrollIntoView({ behavior: 'smooth', block: 'start' }); setTimeout(() => window.scrollTo({ top: (document.documentElement.scrollHeight - window.innerHeight) * continueFrom.value / 100, behavior: 'smooth' }), 120); }
async function copyLink() { try { await navigator.clipboard.writeText(`${post.value.title}\n${post.value.excerpt}\n${window.location.href}`); commentStatus.value = '文章标题、摘要和链接已复制'; } catch { commentStatus.value = '请手动复制地址栏链接'; } }
async function addComment() { if (!comment.value.name.trim() || comment.value.content.trim().length < 2 || comment.value.content.length > 2000) { commentStatus.value = '请填写昵称，评论需为 2-2000 个字'; return; } const payload = { postSlug: post.value.slug, parentId: replyTo.value?.id || null, authorName: comment.value.name.trim(), content: comment.value.content.trim() }; try { await submitComment(payload); comment.value = { name: '', content: '' }; replyTo.value = null; commentStatus.value = '评论已提交，审核通过后会显示'; } catch (error) { commentStatus.value = error.message || '评论提交失败，请稍后重试'; } }
function startReply(item){replyTo.value=item;commentStatus.value=`正在回复 ${item.authorName || item.name}`;document.querySelector('.comment-form input')?.focus();}
async function reportComment(item){if(item.reported){commentStatus.value='这条评论已经举报过';return;}try{const result=await apiRequest(`/comments/${item.id}/report`,{method:'POST',body:JSON.stringify({reason:'内容不当'})});item.reported=true;commentStatus.value=result.message;}catch{commentStatus.value='举报提交失败，请稍后重试';}}
</script>

<template>
  <div class="blog-shell"><BlogHeader />
    <main v-if="post" class="article-page">
      <div class="article-breadcrumb"><RouterLink to="/blog">文章</RouterLink><ChevronRight :size="13" /><span>{{ post.category }}</span><button class="article-copy-link" type="button" @click="copyLink"><Copy :size="14" />复制链接</button></div>
      <div v-if="continueFrom > 8 && continueFrom < 96" class="continue-reading"><span>上次读到 {{ continueFrom }}%</span><button type="button" @click="scrollToContinue">继续阅读 <ArrowRight :size="14" /></button></div>
      <header class="article-header"><div class="article-category">{{ post.category }}</div><h1>{{ post.title }}</h1><p>{{ post.excerpt }}</p><div class="article-byline"><span class="author-avatar">{{ author.initials }}</span><div><strong>{{ author.name }}</strong><span>{{ formatDate(post.publishedAt) }} · {{ post.readMinutes }} 分钟阅读</span></div><div class="article-stats"><span>{{ post.views }} 阅读</span><span>更新于 {{ post.updatedAt }}</span></div><SharePoster :post="post" /></div></header>
      <figure class="article-cover"><img :src="post.cover" :alt="post.coverAlt" /><figcaption>{{ post.coverAlt }} <button type="button" title="放大图片" @click="lightbox = { open: true, src: post.cover, alt: post.coverAlt }"><Maximize2 :size="14" /></button></figcaption></figure>
      <div class="article-layout"><aside class="article-toc"><span>本页目录</span><nav><a v-for="(heading,index) in article.headings" :key="heading.id" :href="`#${heading.id}`"><small>0{{ index + 1 }}</small>{{ heading.title }}</a></nav></aside><article ref="contentRef" class="article-content" v-html="article.html" /><aside class="article-side"><div><Clock3 :size="16" /><span>{{ post.readMinutes }} 分钟</span></div><div><CalendarDays :size="16" /><span>{{ post.updatedAt }}</span></div><div><FolderOpen :size="16" /><span>{{ post.category }}</span></div><div><Hash :size="16" /><span>{{ post.tags?.length || 0 }} 个标签</span></div></aside></div>
      <footer class="article-footer"><div class="article-tags"><RouterLink v-for="tag in post.tags" :key="tag" :to="`/blog/tags/${encodeURIComponent(tag)}`">#{{ tag }}</RouterLink></div><section v-if="related.length" class="related-articles"><div class="section-title"><span>KEEP READING</span><h2>相关文章</h2></div><div><RouterLink v-for="item in related" :key="item.slug" :to="`/blog/posts/${item.slug}`"><span>{{ item.category }}</span><strong>{{ item.title }}</strong><ArrowRight :size="16" /></RouterLink></div></section><div class="article-pagination"><RouterLink v-if="previous" :to="`/blog/posts/${previous.slug}`"><ArrowLeft :size="16" /><span><small>上一篇</small>{{ previous.title }}</span></RouterLink><span v-else /><RouterLink v-if="next" :to="`/blog/posts/${next.slug}`"><span><small>下一篇</small>{{ next.title }}</span><ArrowRight :size="16" /></RouterLink></div><section class="comments"><div class="section-title"><span>DISCUSSION</span><h2>评论 <small>{{ comments.length }}</small></h2></div><form class="comment-form" @submit.prevent="addComment"><div><label>昵称</label><input v-model="comment.name" maxlength="80" placeholder="你的名字" /></div><div><label>{{replyTo ? `回复 ${replyTo.authorName}` : '评论'}}</label><textarea v-model="comment.content" maxlength="2000" rows="4" :placeholder="replyTo ? '写下回复内容' : '分享你的想法'" /></div><button class="button button-primary">{{replyTo ? '提交回复' : '提交评论'}} <ArrowRight :size="16" /></button><button v-if="replyTo" type="button" class="comment-cancel" @click="replyTo=null">取消回复</button><small v-if="commentStatus" role="status">{{ commentStatus }}</small></form><div class="comment-list"><article v-for="item in comments" :key="item.id" :class="{reply:item.parentId}"><div><strong>{{ item.authorName || item.name }}</strong><span>{{ (item.createdAt || item.date || '').slice(0, 10) }}{{item.parentId ? ' · 回复' : ''}}</span></div><p>{{ item.content }}</p><footer><button type="button" @click="startReply(item)">回复</button><button type="button" :disabled="item.reported" @click="reportComment(item)">{{item.reported ? '已举报' : '举报'}}</button></footer></article><p v-if="!comments.length" class="comments-empty">还没有评论，欢迎留下第一条。</p></div></section><div class="article-author"><span class="author-avatar large">{{ author.initials }}</span><div><span>WRITTEN BY</span><h2>{{ author.name }}</h2><p>{{ author.bio }}</p><RouterLink class="read-link" :to="`/blog/authors/${author.id}`">查看作者全部文章 <ArrowRight :size="15" /></RouterLink></div></div></footer>
    </main>
    <main v-else class="blog-main"><div class="not-found"><span>404 / LOST SIGNAL</span><h1>文章没有找到。</h1><RouterLink class="button button-primary" to="/blog">返回博客</RouterLink></div></main>
    <div v-if="lightbox.open" class="image-lightbox" role="dialog" aria-modal="true" @click.self="lightbox.open = false"><button class="icon-button" type="button" aria-label="关闭图片预览" @click="lightbox.open = false"><X :size="20" /></button><img :src="lightbox.src" :alt="lightbox.alt" /></div>
    <BlogFooter />
  </div>
</template>
