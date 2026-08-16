<script setup>
defineProps({ settings: { type: Object, required: true } });
</script>

<template>
  <section class="admin-panel settings-panel advanced-copy-panel">
    <div class="panel-heading"><div><span>ADVANCED COPY</span><h2>其他页面文案</h2></div><small>点击页面顶部“保存全部设置”后生效。</small></div>
    <div class="public-copy-grid">
      <label>博客页标题<textarea v-model="settings.blogTitle" rows="2" /></label>
      <label>博客页说明<textarea v-model="settings.blogIntro" rows="2" /></label>
      <label>精选加载文案<input v-model="settings.landingLoadingLabel" /></label>
      <label>站点标语<input v-model="settings.siteTagline" /></label>
      <label class="wide">首页主题数据<textarea v-model="settings.landingTopics" rows="3" /></label>
      <label>主题区标题<input v-model="settings.landingTopicsTitle" /></label>
      <label>主题区说明<input v-model="settings.landingTopicsIntro" /></label>
      <label>精选区标题<input v-model="settings.featuredTitle" /></label>
      <label>首页状态文案<input v-model="settings.landingStatusTopics" /></label>
      <label>首页最近更新标签<input v-model="settings.landingRecentLabel" /></label>
      <label>首页主题标签<input v-model="settings.landingTopicsLabel" /></label>
      <label>首页探索链接<input v-model="settings.landingExploreLabel" /></label>
      <label>首页关于链接<input v-model="settings.landingAboutLink" /></label>
      <label>首页导航文章<input v-model="settings.landingNavPosts" /></label>
      <label>首页导航主题<input v-model="settings.landingNavTopics" /></label>
      <label>首页导航关于<input v-model="settings.landingNavAbout" /></label>
      <label>首页进入博客<input v-model="settings.heroEnterBlog" /></label>
      <label>首页查看精选<input v-model="settings.heroViewFeatured" /></label>
      <label>首页精选标签<input v-model="settings.landingSelectedLabel" /></label>
      <label>首页主题区标签<input v-model="settings.landingTopicsSectionLabel" /></label>
      <label>首页关于区标签<input v-model="settings.landingAboutSectionLabel" /></label>
      <label>首页全部文章链接<input v-model="settings.landingAllPostsLabel" /></label>
      <label>首页页脚进入博客<input v-model="settings.landingFooterEnterLabel" /></label>
      <label>首页页脚关于<input v-model="settings.landingFooterAboutLabel" /></label>
      <label>首页页脚联系<input v-model="settings.landingFooterContactLabel" /></label>
      <label>首页页脚隐私<input v-model="settings.landingFooterPrivacyLabel" /></label>
      <label>搜索页标题<input v-model="settings.searchTitle" /></label>
      <label>搜索页说明<input v-model="settings.searchIntro" /></label>
      <label>博客全部筛选<input v-model="settings.blogFilterAllLabel" /></label>
      <label>博客阅读全文<input v-model="settings.blogReadMoreLabel" /></label>
      <label>博客导航文章<input v-model="settings.blogNavPostsLabel" /></label>
      <label>博客导航分类<input v-model="settings.blogNavCategoriesLabel" /></label>
      <label>博客导航标签<input v-model="settings.blogNavTagsLabel" /></label>
      <label>博客导航归档<input v-model="settings.blogNavArchivesLabel" /></label>
      <label>博客导航关于<input v-model="settings.blogNavAboutLabel" /></label>
      <label>博客搜索占位<input v-model="settings.blogNavSearchPlaceholder" /></label>
      <label>博客返回介绍页<input v-model="settings.blogNavHomeLabel" /></label>
      <label>博客 RSS 标签<input v-model="settings.blogNavRssLabel" /></label>
      <label class="wide">搜索结果摘要模板<input v-model="settings.searchResultSummary" /></label>
      <label>分类页标题<input v-model="settings.categoriesTitle" /></label>
      <label>分类页说明<input v-model="settings.categoriesIntro" /></label>
      <label>标签页标题<input v-model="settings.tagsTitle" /></label>
      <label>标签页说明<input v-model="settings.tagsIntro" /></label>
      <label>分类详情说明<input v-model="settings.categoryRouteIntro" /></label>
      <label>标签详情说明<input v-model="settings.tagRouteIntro" /></label>
      <label>归档页标题<input v-model="settings.archiveTitle" /></label>
      <label>归档页说明<input v-model="settings.archiveIntro" /></label>
      <label>无搜索结果标题<input v-model="settings.noResultsTitle" /></label>
      <label>无搜索结果说明<input v-model="settings.noResultsDescription" /></label>
      <label>搜索推荐标题<input v-model="settings.searchSuggestionsTitle" /></label>
      <label>无公开文章文案<input v-model="settings.noPublicPosts" /></label>
      <label>无公开文章说明<input v-model="settings.noPublicPostsDescription" /></label>
      <label>无公开标签文案<input v-model="settings.noPublicTags" /></label>
      <label>标签无文章标题<input v-model="settings.noTaggedPostsTitle" /></label>
      <label>标签无文章说明<input v-model="settings.noTaggedPostsDescription" /></label>
      <label>无连接标签<input v-model="settings.noConnectionLabel" /></label>
      <label>无连接标题<input v-model="settings.noConnectionTitle" /></label>
      <label>无连接说明<input v-model="settings.noConnectionDescription" /></label>
      <label>重新连接按钮<input v-model="settings.reconnectLabel" /></label>
      <label>重新连接中<input v-model="settings.reconnectingLabel" /></label>
      <label>列表结束标签<input v-model="settings.listEndLabel" /></label>
      <label>列表结束说明<input v-model="settings.listEndDescription" /></label>
      <label>订阅眉标<input v-model="settings.subscribeEyebrow" /></label>
      <label>订阅邮箱占位<input v-model="settings.subscribeEmailPlaceholder" /></label>
      <label>订阅按钮<input v-model="settings.subscribeButtonLabel" /></label>
      <label>搜索输入占位<input v-model="settings.searchInputPlaceholder" /></label>
      <label>搜索按钮<input v-model="settings.searchButtonLabel" /></label>
      <label>搜索结果链接<input v-model="settings.searchResultLink" /></label>
      <label>搜索上一页<input v-model="settings.searchPaginationPrevious" /></label>
      <label>搜索下一页<input v-model="settings.searchPaginationNext" /></label>
      <label>分类标签标题<input v-model="settings.categoriesSectionLabel" /></label>
      <label>分类文章数量<input v-model="settings.categoryPostCountLabel" /></label>
      <label>标签文章数量<input v-model="settings.tagPostCountLabel" /></label>
      <label>作者区标签<input v-model="settings.authorSectionLabel" /></label>
      <label>作者文章数量<input v-model="settings.authorPostCountLabel" /></label>
      <label>联系姓名字段<input v-model="settings.contactNameLabel" /></label>
      <label>联系邮箱字段<input v-model="settings.contactEmailLabel" /></label>
      <label>联系主题字段<input v-model="settings.contactSubjectLabel" /></label>
      <label>联系正文字段<input v-model="settings.contactMessageLabel" /></label>
      <label>联系提交按钮<input v-model="settings.contactSubmitLabel" /></label>
      <label>无文章标签<input v-model="settings.noNotesLabel" /></label>
      <label>分享文章按钮<input v-model="settings.shareArticleLabel" /></label>
      <label>分享图片标题<input v-model="settings.sharePosterTitle" /></label>
      <label>分享图片加载<input v-model="settings.sharePosterLoadingLabel" /></label>
      <label>复制链接按钮<input v-model="settings.shareCopyLinkLabel" /></label>
      <label>保存图片按钮<input v-model="settings.shareDownloadLabel" /></label>
      <label>系统分享按钮<input v-model="settings.shareSystemLabel" /></label>
      <label>复制链接成功<input v-model="settings.shareCopiedLabel" /></label>
      <label>保存图片成功<input v-model="settings.shareSavedLabel" /></label>
      <label>扫码提示<input v-model="settings.shareScanLabel" /></label>
      <label>二维码说明<input v-model="settings.shareQrDescription" /></label>
      <label>横版模板标签<input v-model="settings.shareLandscapeLabel" /></label>
      <label>竖版模板标签<input v-model="settings.sharePortraitLabel" /></label>
      <label>文章未找到标题<input v-model="settings.articleNotFoundTitle" /></label>
      <label>文章未找到返回按钮<input v-model="settings.articleNotFoundBackLabel" /></label>
      <label>相关文章标题<input v-model="settings.articleRelatedTitle" /></label>
      <label>上一篇标签<input v-model="settings.articlePreviousLabel" /></label>
      <label>下一篇标签<input v-model="settings.articleNextLabel" /></label>
      <label>评论区眉标<input v-model="settings.commentsSectionLabel" /></label>
      <label>评论标题<input v-model="settings.commentsTitle" /></label>
      <label>评论昵称字段<input v-model="settings.commentsNameLabel" /></label>
      <label>评论昵称占位<input v-model="settings.commentsNamePlaceholder" /></label>
      <label>评论正文字段<input v-model="settings.commentsBodyLabel" /></label>
      <label>评论占位<input v-model="settings.commentsPlaceholder" /></label>
      <label>回复占位<input v-model="settings.commentsReplyPlaceholder" /></label>
      <label>提交评论按钮<input v-model="settings.commentsSubmitLabel" /></label>
      <label>提交回复按钮<input v-model="settings.commentsReplySubmitLabel" /></label>
      <label>取消回复按钮<input v-model="settings.commentsCancelReplyLabel" /></label>
      <label>评论回复按钮<input v-model="settings.commentsReplyActionLabel" /></label>
      <label>评论举报按钮<input v-model="settings.commentsReportActionLabel" /></label>
      <label>评论已举报标签<input v-model="settings.commentsReportedLabel" /></label>
      <label>无评论文案<input v-model="settings.commentsEmptyLabel" /></label>
      <label>评论回复后缀<input v-model="settings.commentsReplySuffix" /></label>
      <label>评论校验提示<input v-model="settings.commentsValidationError" /></label>
      <label>评论提交成功提示<input v-model="settings.commentsSubmittedMessage" /></label>
      <label>评论提交失败提示<input v-model="settings.commentsSubmitError" /></label>
      <label>评论举报失败提示<input v-model="settings.commentsReportError" /></label>
      <label>复制文章链接按钮<input v-model="settings.articleCopyLinkLabel" /></label>
      <label>复制文章链接成功<input v-model="settings.articleCopySuccessLabel" /></label>
      <label>复制文章链接失败<input v-model="settings.articleCopyFailureLabel" /></label>
      <label>隐私更新时间前缀<input v-model="settings.privacyUpdatedPrefix" /></label>
      <label>未找到返回按钮<input v-model="settings.notFoundBackLabel" /></label>
      <label>状态重试按钮<input v-model="settings.statusRetryLabel" /></label>
      <label>状态返回按钮<input v-model="settings.statusBackLabel" /></label>
      <label>状态搜索按钮<input v-model="settings.statusSearchLabel" /></label>
      <label>联系留言占位文案<input v-model="settings.contactFormMessagePlaceholder" /></label>
      <label>联系隐私同意文案<input v-model="settings.contactConsentLabel" /></label>
      <label>联系许可标题<input v-model="settings.contactLicenseHeading" /></label>
      <label>联系页许可说明<input v-model="settings.contactLicenseNote" /></label>
      <label>隐私页标题<input v-model="settings.privacyTitle" /></label>
      <label>隐私更新时间<input v-model="settings.privacyUpdatedAt" type="date" /></label>
      <label>隐私说明标题<input v-model="settings.privacyIntroHeading" /></label>
      <label>评论规范标题<input v-model="settings.privacyCommentsHeading" /></label>
      <label>数据权利标题<input v-model="settings.privacyRightsHeading" /></label>
      <label>使用协议标题<input v-model="settings.termsHeading" /></label>
      <label class="wide">评论规范<textarea v-model="settings.privacyCommentsPolicy" rows="3" /></label>
      <label class="wide">数据权利说明<textarea v-model="settings.privacyRights" rows="3" /></label>
      <label class="wide">使用协议<textarea v-model="settings.termsContent" rows="3" /></label>
      <label>作者姓名<input v-model="settings.authorName" /></label>
      <label>作者角色<input v-model="settings.authorRole" /></label>
      <label class="wide">作者简介<textarea v-model="settings.authorBio" rows="2" /></label>
      <label>关于原则 1 标题<input v-model="settings.aboutPrinciple1Title" /></label>
      <label>关于原则 1 说明<input v-model="settings.aboutPrinciple1Body" /></label>
      <label>关于原则 2 标题<input v-model="settings.aboutPrinciple2Title" /></label>
      <label>关于原则 2 说明<input v-model="settings.aboutPrinciple2Body" /></label>
      <label>关于原则 3 标题<input v-model="settings.aboutPrinciple3Title" /></label>
      <label>关于原则 3 说明<input v-model="settings.aboutPrinciple3Body" /></label>
      <label>404 标题<input v-model="settings.notFoundTitle" /></label>
      <label>404 说明<input v-model="settings.notFoundDescription" /></label>
      <template v-for="code in ['403', '404', '500', '503', 'Default']" :key="code">
        <label>{{ code }} 状态标签<input v-model="settings[`status${code}Label`]" /></label>
        <label>{{ code }} 状态标题<input v-model="settings[`status${code}Title`]" /></label>
        <label class="wide">{{ code }} 状态说明<input v-model="settings[`status${code}Description`]" /></label>
      </template>
    </div>
  </section>
</template>
