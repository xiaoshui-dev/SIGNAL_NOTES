INSERT INTO categories (id, name, slug, description) VALUES
  (1, '人工智能', 'artificial-intelligence', '模型、产品、评估与人机协作。'),
  (2, '软件工程', 'software-engineering', '代码、架构、团队和工程方法。'),
  (3, '系统设计', 'system-design', '可靠性、性能与基础设施。'),
  (4, '数字生活', 'digital-life', '工具、阅读与个人工作流。');

INSERT INTO posts (id, slug, title, excerpt, content, cover, cover_alt, category_id, status, author_name, published_at, updated_at, read_minutes, views) VALUES
  (1, 'building-reliable-ai-systems', '构建可靠 AI 系统：从一次演示走向真实生产', '模型能力只是起点。真正的 AI 产品需要评估、可观测性、降级策略和人类反馈共同构成可靠性边界。', '## 演示成功不等于系统可靠\n\n一个模型在精心准备的提示词下给出漂亮答案，证明的是能力上限，不是产品下限。\n\n> 可靠性不是让错误消失，而是让错误可见、可控，并且可以恢复。\n\n## 建立可复现的评估集\n\n每次修改提示词、模型或检索策略，都应运行同一组评估。\n\n## 让链路可观测\n\n记录延迟、Token、引用命中和用户反馈。\n\n## 设计明确的降级路径\n\n外部模型超时，可以返回检索结果；高风险操作则应要求人工确认。', '/assets/hero-circuit.jpg', '电路板上的芯片与精密电子元件', 3, 'PUBLISHED', '林默', '2026-08-15', '2026-08-15', 12, 4826),
  (2, 'local-first-software', 'Local-first 软件为什么重新受到关注', '在云协作成为默认之后，离线可用、数据所有权和即时响应又重新成为产品竞争力。', '## 云优先解决了什么\n\n云端应用让多设备同步、协作和发布变得简单。\n\n## 体验首先来自即时响应\n\n用户操作直接写入本地数据，界面无需等待网络往返。\n\n## 数据所有权是一种产品能力\n\n开放格式和本地副本能够建立长期信任。', '/assets/workstation.jpg', '桌面上的笔记本电脑与开发环境', 2, 'PUBLISHED', '林默', '2026-08-02', '2026-08-06', 8, 3214),
  (3, 'ai-interface-is-a-contract', 'AI 界面也是契约：如何设计可预期的人机协作', '当输出具有不确定性，界面需要让用户理解系统在做什么、可以相信什么以及如何纠正。', '## 不确定性必须可见\n\n来源、时间范围、工具调用状态和未完成部分，都应该在界面里有明确位置。\n\n## 把等待变成过程\n\n长任务需要显示正在进行的阶段，而不只是无限旋转的加载图标。', '/assets/ai-interface.jpg', '显示抽象人工智能界面的笔记本电脑', 1, 'PUBLISHED', '林默', '2026-05-30', '2026-06-02', 10, 3672);

INSERT INTO post_tags (post_id, tag) VALUES
  (1, '人工智能'), (1, '可靠性'), (1, '工程实践'),
  (2, 'Local-first'), (2, '产品设计'), (2, '数据'),
  (3, '人工智能'), (3, '交互设计'), (3, '产品设计');

INSERT INTO site_users (name, email, role, status) VALUES
  ('林默', 'admin@signal.local', 'ADMIN', 'ACTIVE'),
  ('编辑同事', 'editor@signal.local', 'EDITOR', 'ACTIVE');

INSERT INTO site_settings (setting_key, setting_value) VALUES
  ('siteName', '脉冲笔记'),
  ('tagline', '把复杂技术讲清楚'),
  ('shareTemplate', 'landscape'),
  ('commentsEnabled', 'true');

INSERT INTO media_assets (filename, url, mime_type, size, alt_text) VALUES
  ('hero-circuit.jpg', '/assets/hero-circuit.jpg', 'image/jpeg', 767870, '电路板上的芯片与精密电子元件'),
  ('workstation.jpg', '/assets/workstation.jpg', 'image/jpeg', 251331, '桌面上的笔记本电脑与开发环境');

INSERT INTO audit_logs (actor, action, target) VALUES ('system', 'MIGRATE_DATABASE', 'V2 demo content');
