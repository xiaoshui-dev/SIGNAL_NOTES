<script setup>
import DOMPurify from 'dompurify';
import { marked } from 'marked';
import { ArrowLeft, ArrowRight, CalendarDays, ChevronRight, Clock3, Copy, FolderOpen, Hash, Maximize2, X } from 'lucide-vue-next';
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
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
const articleStatus = ref('loading');
const articleError = ref('');
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

let loadGeneration = 0;
let removeArticleInteractionListeners = () => {};

async function loadArticle(slug) {
  const generation = ++loadGeneration;
  articleStatus.value = 'loading';
  articleError.value = '';
  post.value = null;
  allPosts.value = [];
  comments.value = [];
  comment.value = { name: '', content: '' };
  replyTo.value = null;
  commentStatus.value = '';
  lightbox.value = { open: false, src: '', alt: '' };
  continueFrom.value = 0;
  document.getElementById('signal-article-jsonld')?.remove();
  document.title = `${site.siteName} | ${site.siteShortName}`;
  document.documentElement.style.removeProperty('--reading-progress');
  removeArticleInteractionListeners();
  await loadSite().catch(() => {});
  const [postResult, listResult] = await Promise.allSettled([
    apiRequest(`/posts/${encodeURIComponent(slug)}`),
    apiRequest('/posts'),
  ]);
  if (generation !== loadGeneration || route.params.slug !== slug) return;
  if (postResult.status === 'rejected') {
    const error = postResult.reason || {};
    articleStatus.value = error.status === 404 ? 'not-found' : 'error';
    articleError.value = error.message || site.noConnectionDescription;
    return;
  }
  post.value = postResult.value || null;
  allPosts.value = listResult.status === 'fulfilled' && Array.isArray(listResult.value) ? listResult.value : [];
  if (!post.value) {
    articleStatus.value = 'not-found';
    return;
  }
  articleStatus.value = 'ready';
  setPageSeo({ title: `${post.value.title} | ${site.siteName}`, description: post.value.excerpt, canonical: new URL(`/blog/posts/${post.value.slug}`, window.location.origin).href, type: 'article' });
  setArticleJsonLd(post.value);
  continueFrom.value = Number(localStorage.getItem(`signal-reading-${post.value.slug}`) || 0);
  try {
    const list = await apiRequest(`/comments?postSlug=${encodeURIComponent(slug)}`);
    if (generation === loadGeneration) comments.value = orderComments(Array.isArray(list) ? list : []);
  } catch (error) {
    if (generation === loadGeneration) commentStatus.value = error.message || '评论服务暂时不可用';
  }
  if (generation !== loadGeneration) return;
  await nextTick();
  if (generation !== loadGeneration) return;
  setupArticleInteractions();
  progress();
}

watch(() => route.params.slug, (slug) => {
  if (slug) loadArticle(String(slug));
}, { immediate: true });

function retryArticle() {
  if (route.params.slug) loadArticle(String(route.params.slug));
}

onMounted(() => window.addEventListener('scroll', progress, { passive: true }));

onUnmounted(() => {
  loadGeneration += 1;
  removeArticleInteractionListeners();
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
  removeArticleInteractionListeners();
  if (!contentRef.value) return;
  const cleanups = [];
  contentRef.value.querySelectorAll('pre').forEach((pre) => {
    if (pre.querySelector('.code-copy')) return;
    const button = document.createElement('button'); button.className = 'code-copy'; button.type = 'button'; button.textContent = '复制代码';
    button.addEventListener('click', async () => {
      const text = pre.querySelector('code')?.textContent || pre.textContent || '';
      try { await navigator.clipboard.writeText(text); button.textContent = '已复制'; setTimeout(() => { button.textContent = '复制代码'; }, 1600); }
      catch { button.textContent = '请手动复制'; setTimeout(() => { button.textContent = '复制代码'; }, 1600); }
    });
    pre.appendChild(button);
    cleanups.push(() => button.remove());
  });
  contentRef.value.querySelectorAll('img').forEach((image) => {
    const onClick = () => { lightbox.value = { open: true, src: image.currentSrc || image.src, alt: image.alt }; };
    image.addEventListener('click', onClick);
    cleanups.push(() => image.removeEventListener('click', onClick));
  });
  removeArticleInteractionListeners = () => { cleanups.splice(0).forEach((cleanup) => cleanup()); };
}

function scrollToContinue() { document.querySelector('.article-content')?.scrollIntoView({ behavior: 'smooth', block: 'start' }); setTimeout(() => window.scrollTo({ top: (document.documentElement.scrollHeight - window.innerHeight) * continueFrom.value / 100, behavior: 'smooth' }), 120); }
async function copyLink() { try { await navigator.clipboard.writeText(`${post.value.title}\n${post.value.excerpt}\n${window.location.href}`); commentStatus.value = site.articleCopySuccessLabel; } catch { commentStatus.value = site.articleCopyFailureLabel; } }
async function addComment() { if (!comment.value.name.trim() || comment.value.content.trim().length < 2 || comment.value.content.length > 2000) { commentStatus.value = site.commentsValidationError; return; } const payload = { postSlug: post.value.slug, parentId: replyTo.value?.id || null, authorName: comment.value.name.trim(), content: comment.value.content.trim() }; try { await submitComment(payload); comment.value = { name: '', content: '' }; replyTo.value = null; commentStatus.value = site.commentsSubmittedMessage; } catch (error) { commentStatus.value = error.message || site.commentsSubmitError; } }
function startReply(item){replyTo.value=item;commentStatus.value=`正在回复 ${item.authorName || item.name}`;document.querySelector('.comment-form input')?.focus();}
async function reportComment(item){if(item.reported){commentStatus.value=site.commentsReportedLabel;return;}try{const result=await apiRequest(`/comments/${item.id}/report`,{method:'POST',body:JSON.stringify({reason:'内容不当'})});item.reported=true;commentStatus.value=result.message;}catch{commentStatus.value=site.commentsReportError;}}
</script>

<template>
  <div class="blog-shell"><BlogHeader />
    <main v-if="articleStatus === 'loading'" class="blog-main"><div class="empty-state" role="status" aria-live="polite"><span>{{ site.landingLoadingLabel }}</span><h2>{{ site.landingLoadingLabel }}</h2><p>{{ site.noPublicPostsDescription }}</p></div></main>
    <main v-else-if="articleStatus === 'error'" class="blog-main"><div class="empty-state" role="alert"><span>{{ site.noConnectionLabel }}</span><h2>{{ site.noConnectionTitle }}</h2><p>{{ articleError || site.noConnectionDescription }}</p><button class="button" type="button" @click="retryArticle">{{ site.reconnectLabel }}</button></div></main>
    <main v-else-if="articleStatus === 'not-found'" class="blog-main"><div class="not-found"><span>{{ site.status404Label }}</span><h1>{{ site.articleNotFoundTitle }}</h1><RouterLink class="button button-primary" to="/blog">{{ site.articleNotFoundBackLabel }}</RouterLink></div></main>
    <main v-else-if="post" class="article-page">
      <div class="article-breadcrumb"><RouterLink to="/blog">文章</RouterLink><ChevronRight :size="13" /><span>{{ post.category }}</span><button class="article-copy-link" type="button" @click="copyLink"><Copy :size="14" />{{ site.articleCopyLinkLabel }}</button></div>
      <div v-if="continueFrom > 8 && continueFrom < 96" class="continue-reading"><span>上次读到 {{ continueFrom }}%</span><button type="button" @click="scrollToContinue">继续阅读 <ArrowRight :size="14" /></button></div>
      <header class="article-header"><div class="article-category">{{ post.category }}</div><h1>{{ post.title }}</h1><p>{{ post.excerpt }}</p><div class="article-byline"><span class="author-avatar">{{ author.initials }}</span><div><strong>{{ author.name }}</strong><span>{{ formatDate(post.publishedAt) }} · {{ post.readMinutes }} 分钟阅读</span></div><div class="article-stats"><span>{{ post.views }} 阅读</span><span>更新于 {{ post.updatedAt }}</span></div><SharePoster :post="post" /></div></header>
      <figure class="article-cover"><img :src="post.cover" :alt="post.coverAlt" /><figcaption>{{ post.coverAlt }} <button type="button" title="放大图片" @click="lightbox = { open: true, src: post.cover, alt: post.coverAlt }"><Maximize2 :size="14" /></button></figcaption></figure>
      <div class="article-layout"><aside class="article-toc"><span>本页目录</span><nav><a v-for="(heading,index) in article.headings" :key="heading.id" :href="`#${heading.id}`"><small>0{{ index + 1 }}</small>{{ heading.title }}</a></nav></aside><article ref="contentRef" class="article-content" v-html="article.html" /><aside class="article-side"><div><Clock3 :size="16" /><span>{{ post.readMinutes }} 分钟</span></div><div><CalendarDays :size="16" /><span>{{ post.updatedAt }}</span></div><div><FolderOpen :size="16" /><span>{{ post.category }}</span></div><div><Hash :size="16" /><span>{{ post.tags?.length || 0 }} 个标签</span></div></aside></div>
      <footer class="article-footer"><div class="article-tags"><RouterLink v-for="tag in post.tags" :key="tag" :to="`/blog/tags/${encodeURIComponent(tag)}`">#{{ tag }}</RouterLink></div><section v-if="related.length" class="related-articles"><div class="section-title"><span>KEEP READING</span><h2>{{ site.articleRelatedTitle }}</h2></div><div><RouterLink v-for="item in related" :key="item.slug" :to="`/blog/posts/${item.slug}`"><span>{{ item.category }}</span><strong>{{ item.title }}</strong><ArrowRight :size="16" /></RouterLink></div></section><div class="article-pagination"><RouterLink v-if="previous" :to="`/blog/posts/${previous.slug}`"><ArrowLeft :size="16" /><span><small>{{ site.articlePreviousLabel }}</small>{{ previous.title }}</span></RouterLink><span v-else /><RouterLink v-if="next" :to="`/blog/posts/${next.slug}`"><span><small>{{ site.articleNextLabel }}</small>{{ next.title }}</span><ArrowRight :size="16" /></RouterLink></div><section class="comments"><div class="section-title"><span>{{ site.commentsSectionLabel }}</span><h2>{{ site.commentsTitle }} <small>{{ comments.length }}</small></h2></div><form class="comment-form" @submit.prevent="addComment"><div><label>{{ site.commentsNameLabel }}</label><input v-model="comment.name" maxlength="80" :placeholder="site.commentsNamePlaceholder" /></div><div><label>{{replyTo ? `${site.commentsReplyActionLabel} ${replyTo.authorName}` : site.commentsBodyLabel}}</label><textarea v-model="comment.content" maxlength="2000" rows="4" :placeholder="replyTo ? site.commentsReplyPlaceholder : site.commentsPlaceholder" /></div><button class="button button-primary">{{replyTo ? site.commentsReplySubmitLabel : site.commentsSubmitLabel}} <ArrowRight :size="16" /></button><button v-if="replyTo" type="button" class="comment-cancel" @click="replyTo=null">{{ site.commentsCancelReplyLabel }}</button><small v-if="commentStatus" role="status">{{ commentStatus }}</small></form><div class="comment-list"><article v-for="item in comments" :key="item.id" :class="{reply:item.parentId}"><div><strong>{{ item.authorName || item.name }}</strong><span>{{ (item.createdAt || item.date || '').slice(0, 10) }}{{item.parentId ? ` · ${site.commentsReplySuffix}` : ''}}</span></div><p>{{ item.content }}</p><footer><button type="button" @click="startReply(item)">{{ site.commentsReplyActionLabel }}</button><button type="button" :disabled="item.reported" @click="reportComment(item)">{{item.reported ? site.commentsReportedLabel : site.commentsReportActionLabel}}</button></footer></article><p v-if="!comments.length" class="comments-empty">{{ site.commentsEmptyLabel }}</p></div></section><div class="article-author"><span class="author-avatar large">{{ author.initials }}</span><div><span>WRITTEN BY</span><h2>{{ author.name }}</h2><p>{{ author.bio }}</p><RouterLink class="read-link" :to="`/blog/authors/${author.id}`">查看作者全部文章 <ArrowRight :size="15" /></RouterLink></div></div></footer>
    </main>
    <div v-if="lightbox.open" class="image-lightbox" role="dialog" aria-modal="true" @click.self="lightbox.open = false"><button class="icon-button" type="button" aria-label="关闭图片预览" @click="lightbox.open = false"><X :size="20" /></button><img :src="lightbox.src" :alt="lightbox.alt" /></div>
    <BlogFooter />
  </div>
</template>
