import { useEffect, useMemo, useState } from 'react';
import { ArrowDown, ArrowRight, Menu, Moon, Search, Sun, X } from 'lucide-react';
import { Link, Route, Routes, useLocation } from 'react-router-dom';
import Blog from './Blog';

function useTheme() {
  const preferred = useMemo(() => {
    const saved = localStorage.getItem('signal-theme');
    if (saved) return saved;
    return window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark';
  }, []);
  const [theme, setTheme] = useState(preferred);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    localStorage.setItem('signal-theme', theme);
  }, [theme]);

  return [theme, () => setTheme((value) => (value === 'dark' ? 'light' : 'dark'))];
}

function Brand({ blog = false }) {
  return (
    <Link className="brand" to={blog ? '/blog' : '/'} aria-label={blog ? '返回博客首页' : '返回介绍页'}>
      <span className="brand-mark" aria-hidden="true"><i /><i /><i /></span>
      <span className="brand-copy"><b>脉冲笔记</b><small>SIGNAL NOTES</small></span>
    </Link>
  );
}

function ThemeButton({ theme, onToggle, light = false }) {
  const Icon = theme === 'dark' ? Sun : Moon;
  return (
    <button className={`icon-button ${light ? 'on-visual' : ''}`} type="button" onClick={onToggle} aria-label="切换明暗主题" title="切换明暗主题">
      <Icon size={18} strokeWidth={1.8} />
    </button>
  );
}

function LandingPage({ theme, onToggleTheme }) {
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    document.body.classList.add('landing-active');
    return () => document.body.classList.remove('landing-active');
  }, []);

  return (
    <main className="landing-page">
      <section className="hero" aria-labelledby="hero-title">
        <div className="hero-media" aria-hidden="true">
          <img src="/assets/hero-circuit.jpg" alt="" />
          <div className="hero-shade" />
          <div className="signal-scan" />
        </div>

        <header className="hero-nav">
          <Brand />
          <nav className={`landing-nav ${menuOpen ? 'is-open' : ''}`} aria-label="介绍页导航">
            <Link to="/blog" onClick={() => setMenuOpen(false)}>文章</Link>
            <a href="#topics" onClick={() => setMenuOpen(false)}>主题</a>
            <a href="#about" onClick={() => setMenuOpen(false)}>关于</a>
          </nav>
          <div className="hero-actions">
            <Link className="icon-button on-visual" to="/blog?search=open" aria-label="搜索文章" title="搜索文章"><Search size={18} /></Link>
            <ThemeButton theme={theme} onToggle={onToggleTheme} light />
            <button className="icon-button on-visual mobile-menu" type="button" aria-label={menuOpen ? '关闭菜单' : '打开菜单'} onClick={() => setMenuOpen((value) => !value)}>
              {menuOpen ? <X size={19} /> : <Menu size={19} />}
            </button>
          </div>
        </header>

        <div className="hero-content">
          <div className="eyebrow reveal reveal-1"><span className="live-dot" /> TECH NOTES / 2026</div>
          <h1 id="hero-title" className="reveal reveal-2">把复杂技术<br />讲清楚</h1>
          <p className="hero-summary reveal reveal-3">关于 AI、软件工程、系统与数字世界的长期记录。少一点噪声，多一点真正有用的理解。</p>
          <div className="hero-cta reveal reveal-4">
            <Link className="button button-primary" to="/blog" onClick={() => localStorage.setItem('intro_seen', 'true')}>进入博客 <ArrowRight size={18} /></Link>
            <a className="button button-quiet on-dark" href="#featured">查看精选 <ArrowDown size={17} /></a>
          </div>
        </div>

        <div className="hero-status reveal reveal-4">
          <div><span>最近更新</span><strong>2026.08.15</strong></div>
          <div><span>正在记录</span><strong>AI · SYSTEMS · CODE</strong></div>
          <a href="#featured">向下探索 <ArrowDown size={15} /></a>
        </div>
      </section>

      <section id="topics" className="landing-band topic-band" aria-labelledby="topics-title">
        <div className="section-kicker">01 / TOPICS</div>
        <div className="band-heading">
          <h2 id="topics-title">观察技术，也观察技术如何改变人。</h2>
          <p>从底层系统到日常工具，用真实项目和长期实践拆解变化。</p>
        </div>
        <div className="topic-index">
          {[
            ['01', '人工智能', '模型、产品与人机协作'],
            ['02', '软件工程', '架构、代码与团队实践'],
            ['03', '系统设计', '性能、可靠性与基础设施'],
            ['04', '数字生活', '工具、阅读与个人工作流'],
          ].map(([number, title, copy]) => (
            <Link key={number} to={`/blog?topic=${encodeURIComponent(title)}`} className="topic-row">
              <span>{number}</span><strong>{title}</strong><small>{copy}</small><ArrowRight size={20} />
            </Link>
          ))}
        </div>
      </section>

      <section id="featured" className="landing-band featured-band" aria-labelledby="featured-title">
        <div className="section-kicker">02 / SELECTED</div>
        <div className="band-heading compact">
          <h2 id="featured-title">从这里开始读</h2>
          <Link to="/blog">全部文章 <ArrowRight size={17} /></Link>
        </div>
        <div className="featured-grid">
          <Link to="/blog/posts/building-reliable-ai-systems" className="feature-story feature-main">
            <img src="/assets/hero-circuit.jpg" alt="微距拍摄的电路板与芯片" />
            <div><span>系统设计 · 12 分钟</span><h3>构建可靠 AI 系统：从一次演示走向真实生产</h3></div>
          </Link>
          <Link to="/blog/posts/local-first-software" className="feature-story feature-text">
            <span>软件工程 · 8 分钟</span><h3>Local-first 软件为什么重新受到关注</h3><p>把数据所有权、离线体验与协作重新放在产品中心。</p>
          </Link>
          <Link to="/blog/posts/reading-source-code" className="feature-story feature-accent">
            <span>代码阅读 · 6 分钟</span><h3>读懂陌生代码库的五条路径</h3><ArrowRight size={24} />
          </Link>
        </div>
      </section>

      <section id="about" className="landing-band about-band" aria-labelledby="about-title">
        <div className="section-kicker">03 / ABOUT</div>
        <div className="about-copy">
          <h2 id="about-title">写给愿意慢下来理解技术的人。</h2>
          <p>脉冲笔记关注技术背后的结构、取舍和真实影响。这里没有追逐热点的速报，只有经过实践、验证和反思之后的记录。</p>
          <Link className="text-link" to="/blog/about">了解这个项目 <ArrowRight size={17} /></Link>
        </div>
      </section>

      <footer className="landing-footer">
        <Brand />
        <p>关于 AI、系统与数字世界的独立技术博客。</p>
        <div><Link to="/blog">进入博客</Link><Link to="/blog/about">关于</Link><a href="mailto:hello@signal-notes.local">联系</a></div>
        <small>© 2026 Signal Notes. Hero photo from Unsplash.</small>
      </footer>
    </main>
  );
}

function ScrollToTop() {
  const { pathname } = useLocation();
  useEffect(() => {
    window.scrollTo(0, 0);
  }, [pathname]);
  return null;
}

export default function App() {
  const [theme, toggleTheme] = useTheme();
  return (
    <>
      <ScrollToTop />
      <Routes>
        <Route path="/" element={<LandingPage theme={theme} onToggleTheme={toggleTheme} />} />
        <Route path="/blog/*" element={<Blog theme={theme} onToggleTheme={toggleTheme} />} />
      </Routes>
    </>
  );
}
