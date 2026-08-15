import DOMPurify from 'dompurify';
import { marked } from 'marked';
import {
  ArrowLeft, ArrowRight, CalendarDays, ChevronRight, Clock3, FolderOpen, Hash, Home,
  Menu, Moon, Rss, Search, Sun, X,
} from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { Link, Navigate, Route, Routes, useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { categories, compactNumber, formatDate, getAuthor, posts, tags } from './data';

function BlogBrand() {
  return (
    <Link className="brand" to="/blog" aria-label="返回博客首页">
      <span className="brand-mark" aria-hidden="true"><i /><i /><i /></span>
      <span className="brand-copy"><b>脉冲笔记</b><small>SIGNAL NOTES</small></span>
    </Link>
  );
}

function BlogThemeButton({ theme, onToggle }) {
  const Icon = theme === 'dark' ? Sun : Moon;
  return <button className="icon-button" type="button" onClick={onToggle} title="切换明暗主题" aria-label="切换明暗主题"><Icon size={18} /></button>;
}

function BlogLayout({ theme, onToggleTheme, children }) {
  const [menuOpen, setMenuOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => setMenuOpen(false), [location.pathname]);

  function submitSearch(event) {
    event.preventDefault();
    const value = new FormData(event.currentTarget).get('q')?.trim();
    if (value) navigate(`/blog/search?q=${encodeURIComponent(value)}`);
  }

  return (
    <div className="blog-shell">
      <header className="blog-header">
        <BlogBrand />
        <nav className={`blog-nav ${menuOpen ? 'is-open' : ''}`} aria-label="博客导航">
          <Link to="/blog">文章</Link>
          <Link to="/blog/categories">分类</Link>
          <Link to="/blog/archives">归档</Link>
          <Link to="/blog/about">关于</Link>
        </nav>
        <form className="header-search" onSubmit={submitSearch} role="search">
          <Search size={16} />
          <input name="q" type="search" aria-label="搜索文章" placeholder="搜索" />
        </form>
        <div className="blog-header-actions">
          <Link className="icon-button" to="/" title="站点介绍" aria-label="返回站点介绍"><Home size={18} /></Link>
          <BlogThemeButton theme={theme} onToggle={onToggleTheme} />
          <button className="icon-button blog-menu-button" type="button" onClick={() => setMenuOpen((value) => !value)} aria-label={menuOpen ? '关闭菜单' : '打开菜单'}>{menuOpen ? <X size={18} /> : <Menu size={18} />}</button>
        </div>
      </header>
      {children}
      <footer className="blog-footer">
        <div><BlogBrand /><p>关于 AI、系统与数字世界的独立技术博客。</p></div>
        <nav aria-label="页脚导航"><Link to="/">站点介绍</Link><Link to="/blog/about">关于</Link><Link to="/blog/archives">归档</Link><a href="/rss.xml"><Rss size={14} /> RSS</a></nav>
        <small>© 2026 Signal Notes · 内容以 CC BY-NC-SA 4.0 发布</small>
      </footer>
    </div>
  );
}

function PostCard({ post, large = false }) {
  return (
    <article className={`post-card ${large ? 'is-large' : ''}`}>
      <Link className="post-card-cover" to={`/blog/posts/${post.slug}`}><img src={post.cover} alt={post.coverAlt} /></Link>
      <div className="post-card-copy">
        <div className="post-meta"><Link to={`/blog/categories?category=${encodeURIComponent(post.category)}`}>{post.category}</Link><span>{formatDate(post.publishedAt)}</span><span>{post.readMinutes} 分钟</span></div>
        <h2><Link to={`/blog/posts/${post.slug}`}>{post.title}</Link></h2>
        <p>{post.excerpt}</p>
        <Link className="read-link" to={`/blog/posts/${post.slug}`}>阅读全文 <ArrowRight size={16} /></Link>
      </div>
    </article>
  );
}

function BlogHome() {
  const [params, setParams] = useSearchParams();
  const topic = params.get('topic') || '';
  const filtered = topic ? posts.filter((post) => post.category === topic || post.tags.includes(topic)) : posts;

  return (
    <main className="blog-main">
      <section className="blog-intro">
        <div><span className="page-index">INDEX / 001</span><h1>技术笔记<br />与长期观察</h1></div>
        <p>记录软件、AI、系统与工具背后的结构和取舍。每一篇文章都尽量给出可以验证的上下文，而不只是结论。</p>
      </section>
      <section className="filter-bar" aria-label="文章筛选">
        <button className={!topic ? 'active' : ''} type="button" onClick={() => setParams({})}>全部 <span>{posts.length}</span></button>
        {categories.map((category) => <button className={topic === category.name ? 'active' : ''} key={category.name} type="button" onClick={() => setParams({ topic: category.name })}>{category.name} <span>{category.count}</span></button>)}
      </section>
      <section className="post-list" aria-live="polite">
        {filtered.length ? filtered.map((post, index) => <PostCard key={post.id} post={post} large={!topic && index === 0} />) : <EmptyState title="这个主题还没有文章" copy="换一个主题，或查看全部文章。" action={<button type="button" onClick={() => setParams({})}>清除筛选</button>} />}
      </section>
      <div className="list-end"><span>END OF NOTES</span><p>已经显示全部 {filtered.length} 篇文章</p></div>
    </main>
  );
}

function EmptyState({ title, copy, action }) {
  return <div className="empty-state"><span>NO SIGNAL</span><h2>{title}</h2><p>{copy}</p>{action}</div>;
}

function PageHeading({ index, title, copy }) {
  return <header className="page-heading"><span>{index}</span><h1>{title}</h1>{copy && <p>{copy}</p>}</header>;
}

function SearchPage() {
  const [params, setParams] = useSearchParams();
  const query = params.get('q')?.trim() || '';
  const [draft, setDraft] = useState(query);
  const results = useMemo(() => {
    const needle = query.toLocaleLowerCase('zh-CN');
    if (!needle) return [];
    return posts.filter((post) => [post.title, post.excerpt, post.category, ...post.tags, post.content].join(' ').toLocaleLowerCase('zh-CN').includes(needle));
  }, [query]);

  function submit(event) {
    event.preventDefault();
    const value = draft.trim().slice(0, 100);
    setParams(value ? { q: value } : {});
  }

  return <main className="blog-main compact-main"><PageHeading index="SEARCH / 002" title="搜索" copy="从标题、摘要、分类、标签和正文中查找。" />
    <form className="search-page-form" onSubmit={submit}><Search /><input autoFocus value={draft} onChange={(event) => setDraft(event.target.value)} placeholder="输入关键词，例如：可靠性" maxLength={100} /><button type="submit">搜索</button></form>
    {query && <div className="search-summary">“{query}” 找到 {results.length} 条结果</div>}
    <section className="search-results">{results.map((post) => <article key={post.id}><div><span>{post.category}</span><span>{formatDate(post.publishedAt)}</span></div><h2><Link to={`/blog/posts/${post.slug}`}>{post.title}</Link></h2><p>{post.excerpt}</p><Link to={`/blog/posts/${post.slug}`}>查看文章 <ChevronRight size={15} /></Link></article>)}</section>
    {query && !results.length && <EmptyState title="没有找到相关内容" copy="尝试更短的关键词，或者从分类开始浏览。" action={<Link to="/blog/categories">浏览分类</Link>} />}
    {!query && <div className="search-suggestions"><h2>可以从这些主题开始</h2>{tags.slice(0, 8).map((tag) => <button key={tag.name} type="button" onClick={() => { setDraft(tag.name); setParams({ q: tag.name }); }}>#{tag.name}</button>)}</div>}
  </main>;
}

function CategoriesPage() {
  const [params] = useSearchParams();
  const selected = params.get('category');
  const selectedPosts = selected ? posts.filter((post) => post.category === selected) : [];
  return <main className="blog-main compact-main"><PageHeading index="TOPICS / 003" title="分类与标签" copy="沿着长期主题浏览，而不是被时间线推着走。" />
    <section className="category-grid">{categories.map((category, index) => <Link key={category.name} to={`/blog/categories?category=${encodeURIComponent(category.name)}`} className={selected === category.name ? 'active' : ''}><span>0{index + 1}</span><h2>{category.name}</h2><p>{category.description}</p><small>{category.count} 篇文章</small></Link>)}</section>
    {selected && <section className="category-posts"><div className="section-title"><span>FILTERED</span><h2>{selected}</h2><small>{selectedPosts.length} 篇</small></div>{selectedPosts.map((post) => <PostCard key={post.id} post={post} />)}</section>}
    <section className="tag-cloud"><div className="section-title"><span>TAG INDEX</span><h2>全部标签</h2></div><div>{tags.map((tag) => <Link key={tag.name} to={`/blog/search?q=${encodeURIComponent(tag.name)}`}>#{tag.name}<small>{tag.count}</small></Link>)}</div></section>
  </main>;
}

function ArchivesPage() {
  const grouped = posts.reduce((acc, post) => {
    const year = post.publishedAt.slice(0, 4);
    (acc[year] ||= []).push(post);
    return acc;
  }, {});
  return <main className="blog-main compact-main"><PageHeading index="ARCHIVE / 004" title="时间归档" copy={`从 ${posts.at(-1).publishedAt.slice(0, 4)} 年到现在，共 ${posts.length} 篇公开文章。`} />
    <section className="archive-list">{Object.entries(grouped).map(([year, items]) => <div className="archive-year" key={year}><h2>{year}</h2><div>{items.map((post) => <Link key={post.id} to={`/blog/posts/${post.slug}`}><time>{post.publishedAt.slice(5).replace('-', '.')}</time><strong>{post.title}</strong><span>{post.category}</span><ArrowRight size={17} /></Link>)}</div></div>)}</section>
  </main>;
}

function buildArticle(markdown) {
  const headings = [];
  const withIds = markdown.replace(/^## (.+)$/gm, (_, title) => {
    const id = `section-${headings.length + 1}`;
    headings.push({ id, title });
    return `## <span id="${id}">${title}</span>`;
  });
  return { html: DOMPurify.sanitize(marked.parse(withIds)), headings };
}

function ArticlePage() {
  const { slug } = useParams();
  const post = posts.find((item) => item.slug === slug);
  if (!post) return <Navigate to="/blog/not-found" replace />;
  const author = getAuthor(post.authorId);
  const article = useMemo(() => buildArticle(post.content), [post.content]);
  const related = posts.filter((item) => item.id !== post.id && (item.category === post.category || item.tags.some((tag) => post.tags.includes(tag)))).slice(0, 2);
  const currentIndex = posts.findIndex((item) => item.id === post.id);

  useEffect(() => {
    document.title = `${post.title} | 脉冲笔记`;
    return () => { document.title = '脉冲笔记 | Signal Notes'; };
  }, [post.title]);

  return <main className="article-page">
    <div className="article-breadcrumb"><Link to="/blog">文章</Link><ChevronRight size={13} /><Link to={`/blog/categories?category=${encodeURIComponent(post.category)}`}>{post.category}</Link></div>
    <header className="article-header">
      <div className="article-category">{post.category}</div>
      <h1>{post.title}</h1>
      <p>{post.excerpt}</p>
      <div className="article-byline"><span className="author-avatar">{author.initials}</span><div><strong>{author.name}</strong><span>{formatDate(post.publishedAt)} · {post.readMinutes} 分钟阅读</span></div><div className="article-stats"><span>{compactNumber(post.views)} 阅读</span><span>更新于 {post.updatedAt.replaceAll('-', '.')}</span></div></div>
    </header>
    <figure className="article-cover"><img src={post.cover} alt={post.coverAlt} /><figcaption>{post.coverAlt}</figcaption></figure>
    <div className="article-layout">
      <aside className="article-toc"><span>本页目录</span><nav>{article.headings.map((heading, index) => <a key={heading.id} href={`#${heading.id}`}><small>0{index + 1}</small>{heading.title}</a>)}</nav></aside>
      <article className="article-content" dangerouslySetInnerHTML={{ __html: article.html }} />
      <aside className="article-side"><div><Clock3 size={16} /><span>{post.readMinutes} 分钟</span></div><div><CalendarDays size={16} /><span>{post.updatedAt}</span></div><div><FolderOpen size={16} /><span>{post.category}</span></div><div><Hash size={16} /><span>{post.tags.length} 个标签</span></div></aside>
    </div>
    <footer className="article-footer">
      <div className="article-tags">{post.tags.map((tag) => <Link key={tag} to={`/blog/search?q=${encodeURIComponent(tag)}`}>#{tag}</Link>)}</div>
      <div className="article-author"><span className="author-avatar large">{author.initials}</span><div><span>WRITTEN BY</span><h2>{author.name}</h2><p>{author.bio}</p></div></div>
      {related.length > 0 && <section className="related-posts"><div className="section-title"><span>NEXT SIGNAL</span><h2>继续阅读</h2></div><div>{related.map((item) => <Link key={item.id} to={`/blog/posts/${item.slug}`}><span>{item.category} · {item.readMinutes} 分钟</span><h3>{item.title}</h3><ArrowRight /></Link>)}</div></section>}
      <nav className="article-pagination">
        {posts[currentIndex - 1] ? <Link to={`/blog/posts/${posts[currentIndex - 1].slug}`}><ArrowLeft />上一篇<small>{posts[currentIndex - 1].title}</small></Link> : <span />}
        {posts[currentIndex + 1] && <Link to={`/blog/posts/${posts[currentIndex + 1].slug}`}>下一篇<ArrowRight /><small>{posts[currentIndex + 1].title}</small></Link>}
      </nav>
    </footer>
  </main>;
}

function AboutPage() {
  const author = getAuthor('lin');
  return <main className="blog-main compact-main"><PageHeading index="ABOUT / 005" title="写给愿意慢下来理解技术的人" />
    <section className="about-page-grid"><div className="about-monogram">LM<span>AUTHOR / EDITOR</span></div><div><p className="about-lead">脉冲笔记是一个关于技术、系统和数字生活的独立写作项目。</p><p>我叫{author.name}，是一名软件工程师。这里的文章大多来自真实项目、代码阅读和长期使用工具之后的观察。比起追逐每一个新名词，我更关心一项技术解决了什么问题、引入了什么取舍，以及它会如何改变人的工作。</p><p>文章会持续修订。重要更新会保留日期和说明，错误也会公开更正。</p><div className="about-principles"><div><span>01</span><strong>来自实践</strong><p>尽量给出可验证的上下文和复现路径。</p></div><div><span>02</span><strong>承认边界</strong><p>明确事实、判断和不确定性的区别。</p></div><div><span>03</span><strong>长期可读</strong><p>减少热点语境，让文章在几年后仍有价值。</p></div></div></div></section>
  </main>;
}

function NotFoundPage() {
  return <main className="blog-main compact-main"><div className="not-found"><span>404 / LOST SIGNAL</span><h1>这个页面没有找到。</h1><p>链接可能已经改变，或者文章暂时下线。可以返回博客首页，或搜索你感兴趣的内容。</p><div><Link className="button button-primary" to="/blog">返回博客</Link><Link className="button" to="/blog/search">搜索文章</Link></div></div></main>;
}

export default function Blog({ theme, onToggleTheme }) {
  return <BlogLayout theme={theme} onToggleTheme={onToggleTheme}><Routes>
    <Route index element={<BlogHome />} />
    <Route path="posts/:slug" element={<ArticlePage />} />
    <Route path="search" element={<SearchPage />} />
    <Route path="categories" element={<CategoriesPage />} />
    <Route path="archives" element={<ArchivesPage />} />
    <Route path="about" element={<AboutPage />} />
    <Route path="not-found" element={<NotFoundPage />} />
    <Route path="*" element={<NotFoundPage />} />
  </Routes></BlogLayout>;
}
