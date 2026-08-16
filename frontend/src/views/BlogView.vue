<script setup>
import { ArrowRight, ChevronRight, Search } from "lucide-vue-next";
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import BlogHeader from "../components/BlogHeader.vue";
import BlogFooter from "../components/BlogFooter.vue";
import { formatDate, getAuthor } from "../data";
import {
  loadCategories,
  loadPosts,
  loadTags,
  subscribe,
  submitContact,
} from "../api";
import { useSite } from "../site";

const route = useRoute();
const router = useRouter();
const { site, loadSite } = useSite();
const items = ref([]);
const categories = ref([]);
const tags = ref([]);
const apiMode = ref("正在连接内容服务");
const contentStatus = ref("loading");
const loading = ref(false);
const draft = ref(route.query.q || "");
const searchError = ref("");
const email = ref("");
const subStatus = ref("");
const contact = ref({
  name: "",
  email: "",
  subject: "",
  message: "",
  consent: false,
});
const contactStatus = ref("");

async function refreshPosts() {
  loading.value = true;
  if (!items.value.length && !categories.value.length && !tags.value.length) contentStatus.value = "loading";
  try {
    const [posts, categoryData, tagData] = await Promise.allSettled([
      loadPosts(),
      loadCategories(),
      loadTags(),
    ]);
    if (posts.status === "fulfilled") items.value = Array.isArray(posts.value) ? posts.value : [];
    if (categoryData.status === "fulfilled") categories.value = Array.isArray(categoryData.value) ? categoryData.value : [];
    if (tagData.status === "fulfilled") tags.value = Array.isArray(tagData.value) ? tagData.value : [];
    const failed = [posts, categoryData, tagData].find((result) => result.status === "rejected");
    apiMode.value = failed ? `后端未连接 · ${failed.reason?.message || "内容服务暂时不可用"}` : "MySQL 实时数据";
    contentStatus.value = failed ? "error" : "ready";
  } catch (error) {
    apiMode.value = `后端未连接 · ${error.message || "内容服务暂时不可用"}`;
    contentStatus.value = "error";
  } finally {
    loading.value = false;
  }
}
onMounted(() => {
  refreshPosts();
  loadSite().catch(() => {});
});
watch(
  () => route.query.q,
  (value) => {
    draft.value = value || "";
  },
);

const section = computed(() => route.path.split("/")[2] || "");
const isConnectionError = computed(() => contentStatus.value === "error");
const hasUsableContent = computed(() => Boolean(items.value.length || categories.value.length || tags.value.length));
const isBlockingConnectionError = computed(() => isConnectionError.value && !hasUsableContent.value);
const topic = computed(() => route.query.topic || "");
const routeSlug = computed(() => route.params.slug || "");
const routeAuthor = computed(() => ({
  ...getAuthor(route.params.id),
  name: site.authorName,
  role: site.authorRole,
  bio: site.authorBio,
}));
const filtered = computed(() =>
  topic.value
    ? items.value.filter(
        (post) =>
          post.category === topic.value || post.tags?.includes(topic.value),
      )
    : items.value,
);
const archiveGroups = computed(() => {
  const groups = new Map();
  items.value.forEach((post) => {
    const year = String(post.publishedAt || "").slice(0, 4) || "未定";
    if (!groups.has(year)) groups.set(year, []);
    groups.get(year).push(post);
  });
  return [...groups.entries()]
    .sort(([a], [b]) => b.localeCompare(a))
    .map(([year, posts]) => ({ year, posts }));
});
const searchResults = computed(() => {
  const query = String(route.query.q || "").toLowerCase();
  return query
    ? items.value.filter((post) =>
        [
          post.title,
          post.excerpt,
          post.category,
          ...(post.tags || []),
          post.content,
        ]
          .join(" ")
          .toLowerCase()
          .includes(query),
      )
    : [];
});
const searchPage = computed(() => Math.max(1, Number(route.query.page) || 1));
const searchPages = computed(() =>
  Math.max(1, Math.ceil(searchResults.value.length / 5)),
);
const pagedSearchResults = computed(() =>
  searchResults.value.slice(
    (Math.min(searchPage.value, searchPages.value) - 1) * 5,
    Math.min(searchPage.value, searchPages.value) * 5,
  ),
);
function highlightedParts(text) {
  const query = String(route.query.q || "").trim();
  if (!query) return [{ text, match: false }];
  const value = String(text || "");
  const lower = value.toLowerCase();
  const needle = query.toLowerCase();
  const result = [];
  let from = 0;
  let index = lower.indexOf(needle);
  while (index >= 0) {
    if (index > from)
      result.push({ text: value.slice(from, index), match: false });
    result.push({
      text: value.slice(index, index + query.length),
      match: true,
    });
    from = index + query.length;
    index = lower.indexOf(needle, from);
  }
  if (from < value.length)
    result.push({ text: value.slice(from), match: false });
  return result.length ? result : [{ text: value, match: false }];
}
function doSearch() {
  const value = draft.value.trim();
  if (!value) {
    searchError.value = "请输入要查找的关键词";
    return;
  }
  if (value.length > 80) {
    searchError.value = "关键词不能超过 80 个字符";
    return;
  }
  searchError.value = "";
  router.push({ path: "/blog/search", query: { q: value } });
}
function goSearchPage(page) {
  router.push({ path: "/blog/search", query: { q: route.query.q, page } });
}
async function doSubscribe() {
  if (!/^\S+@\S+\.\S+$/.test(email.value)) {
    subStatus.value = "请输入有效邮箱";
    return;
  }
  try {
    const result = await subscribe(email.value);
    subStatus.value = result.message || "订阅已保存";
    email.value = "";
  } catch (error) {
    subStatus.value = error.message || "订阅失败，请稍后重试";
  }
}
async function doContact() {
  contactStatus.value = "";
  if (
    !contact.value.name.trim() ||
    !/^\S+@\S+\.\S+$/.test(contact.value.email) ||
    !contact.value.subject.trim() ||
    contact.value.message.trim().length < 10 ||
    !contact.value.consent
  ) {
    contactStatus.value = "请完整填写表单，并同意隐私说明";
    return;
  }
  try {
    const result = await submitContact({
      ...contact.value,
      idempotencyKey: `contact-${Date.now()}`,
    });
    contactStatus.value = `${result.message}，工单号 ${result.ticket}`;
    contact.value = {
      name: "",
      email: "",
      subject: "",
      message: "",
      consent: false,
    };
  } catch (error) {
    contactStatus.value =
      error.message || "提交失败，请稍后重试；已保留你填写的内容";
  }
}
</script>

<template>
  <div class="blog-shell">
    <BlogHeader />
    <main class="blog-main" :class="{ 'compact-main': section }">
      <div v-if="isConnectionError && hasUsableContent" class="empty-state inline-load-error"><span>{{ site.noConnectionLabel }}</span><h2>{{ site.noConnectionTitle }}</h2><p>{{ site.noConnectionDescription }}</p><button class="button" type="button" :disabled="loading" @click="refreshPosts">{{ loading ? site.reconnectingLabel : site.reconnectLabel }}</button></div>
      <template v-if="!section">
        <section class="blog-intro">
          <div>
            <span class="page-index">INDEX / 001</span>
            <h1>{{ site.blogTitle }}</h1>
          </div>
          <p>{{ site.blogIntro }}</p>
        </section>
        <section class="filter-bar">
          <button :class="{ active: !topic }" @click="router.push('/blog')">
            {{ site.blogFilterAllLabel }}
            <span>{{ items.length }}</span></button
          ><button
            v-for="category in categories"
            :key="category.slug"
            :class="{ active: topic === category.name }"
            @click="
              router.push({ path: '/blog', query: { topic: category.name } })
            "
          >
            {{ category.name }} <span>{{ category.count }}</span>
          </button>
        </section>
        <section v-if="filtered.length" class="post-list">
          <article
            v-for="(post, index) in filtered"
            :key="post.id"
            class="post-card"
            :class="{ 'is-large': index === 0 && !topic }"
          >
            <RouterLink class="post-card-cover" :to="`/blog/posts/${post.slug}`"
              ><img :src="post.cover" :alt="post.coverAlt"
            /></RouterLink>
            <div class="post-card-copy">
              <div class="post-meta">
                <span>{{ post.category }}</span
                ><span>{{ formatDate(post.publishedAt) }}</span
                ><span>{{ post.readMinutes }} 分钟</span>
              </div>
              <h2>
                <RouterLink :to="`/blog/posts/${post.slug}`">{{
                  post.title
                }}</RouterLink>
              </h2>
              <p>{{ post.excerpt }}</p>
              <RouterLink class="read-link" :to="`/blog/posts/${post.slug}`"
                >{{ site.blogReadMoreLabel }} <ArrowRight :size="16"
              /></RouterLink>
            </div>
          </article>
        </section>
        <div v-else class="empty-state">
          <span>{{ contentStatus === 'loading' ? site.landingLoadingLabel : isBlockingConnectionError ? site.noConnectionLabel : site.noNotesLabel }}</span>
          <h2>
            {{
              isBlockingConnectionError
                ? site.noConnectionTitle
                : contentStatus === 'loading' ? site.landingLoadingLabel : site.noPublicPosts
            }}
          </h2>
          <p>
            {{
              isBlockingConnectionError
                ? site.noConnectionDescription
                : site.noPublicPostsDescription
            }}
          </p>
          <button
            v-if="isBlockingConnectionError"
            class="button"
            type="button"
            :disabled="loading"
            @click="refreshPosts"
          >
            {{ loading ? site.reconnectingLabel : site.reconnectLabel }}
          </button>
        </div>
        <div v-if="filtered.length" class="list-end">
          <span>{{ site.listEndLabel }}</span>
          <p>
            {{ site.listEndDescription.replace("{count}", filtered.length) }}
          </p>
        </div>
        <section class="subscribe-box">
          <div>
            <span>{{ site.subscribeEyebrow }}</span>
            <h2>{{ site.subscribeTitle }}</h2>
            <p>{{ site.subscribeDescription }}</p>
          </div>
          <form @submit.prevent="doSubscribe">
            <input
              v-model="email"
              type="email"
              :placeholder="site.subscribeEmailPlaceholder"
              aria-label="邮箱地址"
            /><button>
              {{ site.subscribeButtonLabel }} <ArrowRight :size="16" /></button
            ><small v-if="subStatus">{{ subStatus }}</small>
          </form>
        </section>
      </template>
      <template v-else-if="section === 'search'"
        ><header class="page-heading">
          <span>SEARCH / 002</span>
          <h1>{{ site.searchTitle }}</h1>
          <p>{{ site.searchIntro }}</p>
        </header>
        <form class="search-page-form" @submit.prevent="doSearch">
          <Search /><input
            v-model="draft"
            maxlength="81"
            :placeholder="site.searchInputPlaceholder"
          /><button>{{ site.searchButtonLabel }}</button>
        </form>
        <small v-if="searchError" class="form-error" role="alert">{{
          searchError
        }}</small>
        <div v-if="isBlockingConnectionError" class="empty-state"><span>{{ site.noConnectionLabel }}</span><h2>{{ site.noConnectionTitle }}</h2><p>{{ site.noConnectionDescription }}</p><button class="button" type="button" :disabled="loading" @click="refreshPosts">{{ loading ? site.reconnectingLabel : site.reconnectLabel }}</button></div>
        <div v-if="route.query.q && !isBlockingConnectionError" class="search-summary">
          {{ site.searchResultSummary.replace('{query}', route.query.q).replace('{count}', searchResults.length).replace('{page}', Math.min(searchPage, searchPages)).replace('{pages}', searchPages) }}
        </div>
        <section v-if="!isBlockingConnectionError" class="search-results">
          <article v-for="post in pagedSearchResults" :key="post.id">
            <div>
              <span>{{ post.category }}</span
              ><span>{{ formatDate(post.publishedAt) }}</span>
            </div>
            <h2>
              <RouterLink :to="`/blog/posts/${post.slug}`"
                ><template
                  v-for="(part, index) in highlightedParts(post.title)"
                  :key="index"
                  ><mark v-if="part.match">{{ part.text }}</mark
                  ><template v-else>{{ part.text }}</template></template
                ></RouterLink
              >
            </h2>
            <p>
              <template
                v-for="(part, index) in highlightedParts(post.excerpt)"
                :key="index"
                ><mark v-if="part.match">{{ part.text }}</mark
                ><template v-else>{{ part.text }}</template></template
              >
            </p>
            <RouterLink :to="`/blog/posts/${post.slug}`"
              >{{ site.searchResultLink }} <ChevronRight :size="15"
            /></RouterLink>
          </article>
        </section>
        <nav
          v-if="!isBlockingConnectionError && searchPages > 1"
          class="result-pagination"
          aria-label="搜索结果分页"
        >
          <button
            :disabled="searchPage <= 1"
            @click="goSearchPage(searchPage - 1)"
          >
            {{ site.searchPaginationPrevious }}</button
          ><button
            v-for="page in searchPages"
            :key="page"
            :class="{ active: page === searchPage }"
            @click="goSearchPage(page)"
          >
            {{ page }}</button
          ><button
            :disabled="searchPage >= searchPages"
            @click="goSearchPage(searchPage + 1)"
          >
            {{ site.searchPaginationNext }}
          </button>
        </nav>
        <div v-if="!isBlockingConnectionError && route.query.q && !searchResults.length" class="empty-state">
          <span>NO SIGNAL</span>
          <h2>{{ site.noResultsTitle }}</h2>
          <p>{{ site.noResultsDescription }}</p>
        </div>
        <div v-if="!isBlockingConnectionError && !route.query.q" class="search-suggestions">
          <h2>{{ site.searchSuggestionsTitle }}</h2>
          <button
            v-for="tag in tags.slice(0, 8)"
            :key="tag.slug"
            @click="
              router.push({ path: '/blog/search', query: { q: tag.name } })
            "
          >
            #{{ tag.name }}
          </button>
        </div></template
      >
      <template v-else-if="section === 'categories'"
        ><template v-if="routeSlug"
          ><header class="page-heading">
            <span>TOPIC / 003</span>
            <h1>{{ routeSlug }}</h1>
            <p>{{ site.categoryRouteIntro }}</p>
          </header>
          <div v-if="isBlockingConnectionError" class="empty-state"><span>{{ site.noConnectionLabel }}</span><h2>{{ site.noConnectionTitle }}</h2><p>{{ site.noConnectionDescription }}</p><button class="button" type="button" :disabled="loading" @click="refreshPosts">{{ loading ? site.reconnectingLabel : site.reconnectLabel }}</button></div>
          <section v-if="!isBlockingConnectionError" class="post-list compact-post-list">
            <article
              v-for="post in items.filter(
                (item) =>
                  item.category === routeSlug ||
                  item.category?.toLowerCase() === routeSlug.toLowerCase(),
              )"
              :key="post.id"
              class="post-card"
            >
              <div class="post-card-copy">
                <div class="post-meta">
                  <span>{{ post.category }}</span
                  ><span>{{ formatDate(post.publishedAt) }}</span>
                </div>
                <h2>
                  <RouterLink :to="`/blog/posts/${post.slug}`">{{
                    post.title
                  }}</RouterLink>
                </h2>
                <p>{{ post.excerpt }}</p>
              </div>
            </article>
          </section><div v-if="!isBlockingConnectionError && !items.some((item) => item.category === routeSlug || item.category?.toLowerCase() === routeSlug.toLowerCase())" class="empty-state"><span>{{ site.noNotesLabel }}</span><h2>{{ site.noPublicPosts }}</h2><p>{{ site.noPublicPostsDescription }}</p><RouterLink class="button" to="/blog/categories">{{ site.categoriesTitle }}</RouterLink></div></template
        ><template v-else
          ><header class="page-heading">
            <span>TOPICS / 003</span>
            <h1>{{ site.categoriesTitle }}</h1>
            <p>{{ site.categoriesIntro }}</p>
          </header>
          <div v-if="isBlockingConnectionError" class="empty-state"><span>{{ site.noConnectionLabel }}</span><h2>{{ site.noConnectionTitle }}</h2><p>{{ site.noConnectionDescription }}</p><button class="button" type="button" :disabled="loading" @click="refreshPosts">{{ loading ? site.reconnectingLabel : site.reconnectLabel }}</button></div>
          <section v-else class="category-grid">
            <RouterLink
              v-for="(category, index) in categories"
              :key="category.slug"
              :to="{
                path: `/blog/categories/${encodeURIComponent(category.name)}`,
              }"
              ><span>0{{ index + 1 }}</span>
              <h2>{{ category.name }}</h2>
              <p>{{ category.description }}</p>
              <small>{{ site.categoryPostCountLabel.replace('{count}', category.count) }}</small></RouterLink
            >
            <p v-if="!categories.length" class="admin-empty">{{ site.noPublicPosts }}</p>
          </section>
          <section v-if="!isBlockingConnectionError" class="tag-cloud">
            <div class="section-title">
              <span>{{ site.categoriesSectionLabel }}</span>
              <h2>{{ site.tagsTitle }}</h2>
            </div>
            <div>
              <RouterLink
                v-for="tag in tags"
                :key="tag.slug"
                :to="{ path: `/blog/tags/${encodeURIComponent(tag.name)}` }"
                >#{{ tag.name }} <small>{{ site.tagPostCountLabel.replace('{count}', tag.count) }}</small></RouterLink
              >
            </div>
            <p v-if="!tags.length" class="admin-empty">{{ site.noPublicTags }}</p>
          </section></template
        ></template
      >
      <template v-else-if="section === 'tags'"
        ><template v-if="routeSlug"
          ><header class="page-heading">
            <span>TAG / 008</span>
            <h1>#{{ routeSlug }}</h1>
            <p>{{ site.tagRouteIntro }}</p>
          </header>
          <div v-if="isBlockingConnectionError" class="empty-state"><span>{{ site.noConnectionLabel }}</span><h2>{{ site.noConnectionTitle }}</h2><p>{{ site.noConnectionDescription }}</p><button class="button" type="button" :disabled="loading" @click="refreshPosts">{{ loading ? site.reconnectingLabel : site.reconnectLabel }}</button></div>
          <section v-else class="post-list compact-post-list">
            <article
              v-for="post in items.filter((item) =>
                item.tags?.includes(routeSlug),
              )"
              :key="post.id"
              class="post-card"
            >
              <div class="post-card-copy">
                <div class="post-meta">
                  <span>{{ post.category }}</span
                  ><span>{{ formatDate(post.publishedAt) }}</span>
                </div>
                <h2>
                  <RouterLink :to="`/blog/posts/${post.slug}`">{{
                    post.title
                  }}</RouterLink>
                </h2>
                <p>{{ post.excerpt }}</p>
              </div>
            </article>
          </section>
          <div
            v-if="!isBlockingConnectionError && !items.some((item) => item.tags?.includes(routeSlug))"
            class="empty-state"
          >
            <span>{{ site.noNotesLabel }}</span>
            <h2>{{ site.noTaggedPostsTitle }}</h2>
            <p>{{ site.noTaggedPostsDescription }}</p>
            <RouterLink class="button" to="/blog/tags">{{
              site.tagsTitle
            }}</RouterLink>
          </div></template
        ><template v-else
          ><header class="page-heading">
            <span>TAG INDEX / 008</span>
            <h1>{{ site.tagsTitle }}</h1>
            <p>{{ site.tagsIntro }}</p>
          </header>
          <div v-if="isBlockingConnectionError" class="empty-state"><span>{{ site.noConnectionLabel }}</span><h2>{{ site.noConnectionTitle }}</h2><p>{{ site.noConnectionDescription }}</p><button class="button" type="button" :disabled="loading" @click="refreshPosts">{{ loading ? site.reconnectingLabel : site.reconnectLabel }}</button></div>
          <section v-else class="tag-cloud tag-index-page">
            <div>
              <RouterLink
                v-for="tag in tags"
                :key="tag.slug"
                :to="`/blog/tags/${encodeURIComponent(tag.name)}`"
                >#{{ tag.name }} <small>{{ site.tagPostCountLabel.replace('{count}', tag.count) }}</small></RouterLink
              >
            </div>
            <p v-if="!tags.length" class="admin-empty">
              {{ site.noPublicTags }}
            </p>
          </section></template
        ></template
      >
      <template v-else-if="section === 'authors'"
        ><header class="page-heading">
          <span>{{ site.authorSectionLabel }}</span>
          <h1>{{ routeAuthor.name }}</h1>
          <p>
            {{ routeAuthor.role }} ·
            {{ site.authorPostCountLabel.replace('{count}', items.filter((item) => item.authorId === route.params.id).length) }}
          </p>
        </header>
        <section class="about-page-grid">
          <div class="about-monogram">
            {{ routeAuthor.initials }}<span>{{ site.authorSectionLabel }}</span>
          </div>
          <div>
            <p class="about-lead">{{ routeAuthor.bio }}</p>
            <section class="post-list compact-post-list">
              <article
                v-for="post in items.filter(
                  (item) => item.authorId === route.params.id,
                )"
                :key="post.id"
                class="post-card"
              >
                <div class="post-card-copy">
                  <h2>
                    <RouterLink :to="`/blog/posts/${post.slug}`">{{
                      post.title
                    }}</RouterLink>
                  </h2>
                  <p>{{ post.excerpt }}</p>
                </div>
              </article>
            </section>
          </div>
        </section></template
      >
      <template v-else-if="section === 'archives'"
        ><header class="page-heading">
          <span>ARCHIVE / 004</span>
          <h1>{{ site.archiveTitle || "时间归档" }}</h1>
          <p>
            {{
              (
                site.archiveIntro || "按发布时间整理，共 {count} 篇公开文章。"
              ).replace("{count}", items.length)
            }}
          </p>
        </header>
        <div v-if="isBlockingConnectionError" class="empty-state"><span>{{ site.noConnectionLabel }}</span><h2>{{ site.noConnectionTitle }}</h2><p>{{ site.noConnectionDescription }}</p><button class="button" type="button" :disabled="loading" @click="refreshPosts">{{ loading ? site.reconnectingLabel : site.reconnectLabel }}</button></div>
        <section v-else class="archive-list">
          <div
            v-for="group in archiveGroups"
            :key="group.year"
            class="archive-year"
          >
            <h2>{{ group.year }}</h2>
            <div>
              <RouterLink
                v-for="post in group.posts"
                :key="post.id"
                :to="`/blog/posts/${post.slug}`"
                ><time>{{ post.publishedAt?.slice(5).replace("-", ".") }}</time
                ><strong>{{ post.title }}</strong
                ><span>{{ post.category }}</span
                ><ArrowRight :size="17"
              /></RouterLink>
            </div>
          </div>
          <p v-if="!archiveGroups.length" class="admin-empty">
            {{ site.noPublicPosts || "还没有公开文章" }}
          </p>
        </section></template
      >
      <template v-else-if="section === 'about'"
        ><header class="page-heading">
          <span>ABOUT / 005</span>
          <h1>{{ site.aboutTitle }}</h1>
        </header>
        <section class="about-page-grid">
          <div class="about-monogram">
            {{ (site.siteShortName || "").slice(0, 2)
            }}<span>AUTHOR / EDITOR</span>
          </div>
          <div>
            <p class="about-lead">{{ site.aboutLead }}</p>
            <p>{{ site.aboutBody }}</p>
            <div class="about-principles">
              <div>
                <span>01</span><strong>{{ site.aboutPrinciple1Title }}</strong>
                <p>{{ site.aboutPrinciple1Body }}</p>
              </div>
              <div>
                <span>02</span><strong>{{ site.aboutPrinciple2Title }}</strong>
                <p>{{ site.aboutPrinciple2Body }}</p>
              </div>
              <div>
                <span>03</span><strong>{{ site.aboutPrinciple3Title }}</strong>
                <p>{{ site.aboutPrinciple3Body }}</p>
              </div>
            </div>
          </div>
        </section></template
      >
      <template v-else-if="section === 'contact'"
        ><header class="page-heading">
          <span>CONTACT / 006</span>
          <h1>{{ site.contactTitle }}</h1>
          <p>{{ site.contactIntro }}</p>
        </header>
        <section class="legal-page contact-page">
          <form class="contact-form" @submit.prevent="doContact">
            <label
              >{{ site.contactNameLabel }}<input
                v-model="contact.name"
                maxlength="80"
                required /></label
            ><label
              >{{ site.contactEmailLabel }}<input
                v-model="contact.email"
                type="email"
                maxlength="180"
                required /></label
            ><label
              >{{ site.contactSubjectLabel }}<input
                v-model="contact.subject"
                maxlength="180"
                required /></label
            ><label
              >{{ site.contactMessageLabel }}<textarea
                v-model="contact.message"
                rows="7"
                maxlength="2000"
                required
                :placeholder="site.contactFormMessagePlaceholder"
              /></label
            ><label class="check-line"
              ><input v-model="contact.consent" type="checkbox" />{{
                site.contactConsentLabel
              }}</label
            ><button class="button button-primary">
              {{ site.contactSubmitLabel }} <ArrowRight :size="16" /></button
            ><small v-if="contactStatus" role="status">{{
              contactStatus
            }}</small>
          </form>
          <aside>
            <span>PUBLIC EMAIL</span>
            <h2>{{ site.publicEmail }}</h2>
            <p>{{ site.replyPromise }}</p>
            <h2>{{ site.contactLicenseHeading }}</h2>
            <p>{{ site.licenseText }}；{{ site.contactLicenseNote }}</p>
          </aside>
        </section></template
      >
      <template v-else-if="section === 'privacy'"
        ><header class="page-heading">
          <span>PRIVACY / 007</span>
          <h1>{{ site.privacyTitle }}</h1>
          <p>{{ site.privacyUpdatedPrefix }}{{ site.privacyUpdatedAt }}</p>
        </header>
        <article class="legal-page legal-copy">
          <section>
            <h2>{{ site.privacyIntroHeading }}</h2>
            <p>{{ site.privacyContent }}</p>
          </section>
          <section>
            <h2>{{ site.privacyCommentsHeading }}</h2>
            <p>{{ site.privacyCommentsPolicy }}</p>
          </section>
          <section>
            <h2>{{ site.privacyRightsHeading }}</h2>
            <p>{{ site.privacyRights }}</p>
          </section>
          <section>
            <h2>{{ site.termsHeading }}</h2>
            <p>{{ site.termsContent }}</p>
          </section>
        </article></template
      >
      <template v-else
        ><div class="not-found">
          <span>404 / LOST SIGNAL</span>
          <h1>{{ site.notFoundTitle }}</h1>
          <p>{{ site.notFoundDescription }}</p>
          <RouterLink class="button button-primary" to="/blog"
            >{{ site.notFoundBackLabel }}</RouterLink
          >
        </div></template
      >
    </main>
    <BlogFooter />
  </div>
</template>
