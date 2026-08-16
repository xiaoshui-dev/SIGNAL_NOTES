<script setup>
defineProps({ settings: { type: Object, required: true } });
</script>

<template>
  <section class="admin-panel settings-panel advanced-copy-panel">
    <div class="panel-heading"><div><span>ADVANCED COPY</span><h2>其他页面文案</h2></div><small>点击页面顶部“保存全部设置”后生效。</small></div>
    <div class="public-copy-grid">
      <label>博客页标题<textarea v-model="settings.blogTitle" rows="2" /></label>
      <label>博客页说明<textarea v-model="settings.blogIntro" rows="2" /></label>
      <label>主题区标题<input v-model="settings.landingTopicsTitle" /></label>
      <label>主题区说明<input v-model="settings.landingTopicsIntro" /></label>
      <label>精选区标题<input v-model="settings.featuredTitle" /></label>
      <label>首页状态文案<input v-model="settings.landingStatusTopics" /></label>
      <label>搜索页标题<input v-model="settings.searchTitle" /></label>
      <label>搜索页说明<input v-model="settings.searchIntro" /></label>
      <label>分类页标题<input v-model="settings.categoriesTitle" /></label>
      <label>分类页说明<input v-model="settings.categoriesIntro" /></label>
      <label>归档页标题<input v-model="settings.archiveTitle" /></label>
      <label>联系页许可说明<input v-model="settings.contactLicenseNote" /></label>
      <label>隐私页标题<input v-model="settings.privacyTitle" /></label>
      <label>隐私更新时间<input v-model="settings.privacyUpdatedAt" type="date" /></label>
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
    </div>
  </section>
</template>
