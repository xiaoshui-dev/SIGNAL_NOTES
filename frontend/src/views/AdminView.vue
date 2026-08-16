<script setup>
import {
  Activity, Archive, ArrowLeft, ArrowRight, BarChart3, Check, ClipboardList, Eye, FileText,
  FolderTree, Hash, History, Image as ImageIcon, Inbox, LayoutDashboard, LogOut, Mail, Menu,
  Plus, RefreshCw, Save, Search, Settings, ShieldCheck, Trash2, Upload, Users, X,
} from 'lucide-vue-next';
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { adminRequest, apiRequest, createAdminToken } from '../api';
import { adminAccessForRole, canAccessAdminRoute } from '../adminAccess';
import { createEditorAutosave, statusForAutosave } from '../editorAutosave';
import { runMailTest } from '../mailTest';
import { applySite, site as publicSite } from '../site';
import AdminAdvancedCopy from '../components/AdminAdvancedCopy.vue';

const route = useRoute();
const router = useRouter();
const authed = ref(localStorage.getItem('signal-admin-auth') === 'true');
const login = ref({ email: 'admin', password: 'signal2026' });
const loginError = ref('');
const menuOpen = ref(false);
const apiStatus = ref('正在连接数据库');
const saved = ref('');
const savedTone = ref('success');
const adminLoading = ref(authed.value);
const adminLoadError = ref('');
let settingsMutationVersion = 0;
const adminPosts = ref([]);
const comments = ref([]);
const contactMessages = ref([]);
const subscriptions = ref([]);
const users = ref([]);
const media = ref([]);
const categoriesAdmin = ref([]);
const tagsAdmin = ref([]);
const settings = ref({ ...publicSite, shareTemplate: 'landscape' });
const logs = ref([]);
const backupJobs = ref([]);
const dashboardData = ref({ views: 0, errors: 0, popular: [], trend: [] });
const query = ref('');
const filter = ref('ALL');
const selectedPosts = ref([]);
const taxonomyDraft = ref({ name: '', slug: '', description: '' });
const tagDraft = ref({ name: '', slug: '', description: '' });
const userDraft = ref({ name: '', email: '', loginName: '', password: '', role: 'AUTHOR', status: 'INVITED' });
const commentReplies = ref({});
const passwordForm = ref({ currentPassword: '', newPassword: '', confirmPassword: '' });
const emailTestRecipient = ref('');
const mailTestStatus = ref({ pending: false, tone: 'info', message: '' });
const currentUser = ref({ loginName: '', name: '', role: 'VIEWER', status: 'ACTIVE' });
const editor = ref({});
const editorReady = ref(false);
const saving = ref(false);
const previewing = ref(false);
const revisions = ref([]);
const timeRange = ref(7);
const settingsSaving = ref(false);
const contactUpdateGenerations = new Map();
const subscriptionUpdateGenerations = new Map();
const pendingContactUpdates = reactive(new Set());
const pendingSubscriptionUpdates = reactive(new Set());

const allNav = [
  ['/admin', '仪表盘', LayoutDashboard], ['/admin/posts', '文章', FileText], ['/admin/taxonomy', '分类与标签', FolderTree],
  ['/admin/media', '媒体库', ImageIcon], ['/admin/comments', '评论', ClipboardList], ['/admin/inbox', '反馈收件箱', Inbox],
  ['/admin/subscribers', '订阅者', Mail], ['/admin/users', '用户', Users], ['/admin/settings', '设置', Settings], ['/admin/logs', '日志与备份', Activity],
];
const currentAccess = computed(() => adminAccessForRole(currentUser.value.role));
const nav = computed(() => allNav.filter(([path]) => currentAccess.value.paths.includes(path)));
const section = computed(() => route.path.split('/')[2] || 'dashboard');
const editorMode = computed(() => route.path.includes('/posts/new') || route.path.includes('/edit'));
const editId = computed(() => route.params.pathMatch?.[1]);
const filteredPosts = computed(() => adminPosts.value.filter((post) => (!query.value || post.title.includes(query.value)) && (filter.value === 'ALL' || post.status === filter.value)));
const publishChecklist = computed(() => [
  { label: '标题', ok: Boolean(editor.value.title?.trim()) }, { label: 'Slug', ok: Boolean(editor.value.slug?.trim()) },
  { label: '正文', ok: editor.value.content?.trim().length > 40 }, { label: '分类', ok: Boolean(editor.value.category) },
  { label: '封面替代文本', ok: Boolean(editor.value.coverAlt?.trim()) }, { label: 'SEO 标题与描述', ok: Boolean(editor.value.seoTitle?.trim() && editor.value.seoDescription?.trim()) },
]);
const mailConfigurationState = computed(() => {
  if (settings.value['mail.enabled'] !== 'true') return { label: '未启用', tone: 'off', detail: '反馈和订阅仍会入库，但系统不会发送邮件。' };
  const missing = [];
  if (!settings.value['mail.host']?.trim()) missing.push('SMTP 主机');
  const port = Number(settings.value['mail.port']);
  if (!Number.isInteger(port) || port < 1 || port > 65535) missing.push('有效端口');
  if (!/^\S+@\S+\.\S+$/.test(settings.value['mail.from'] || '')) missing.push('发件人邮箱');
  if (!/^\S+@\S+\.\S+$/.test(settings.value['mail.notificationTo'] || '')) missing.push('反馈通知邮箱');
  const hasPassword = settings.value['mail.passwordConfigured'] === 'true' || Boolean(settings.value['mail.password']?.trim());
  if (settings.value['mail.auth'] === 'true' && (!settings.value['mail.username']?.trim() || !hasPassword)) missing.push('SMTP 用户名和密码');
  return missing.length
    ? { label: '配置不完整', tone: 'error', detail: `还需填写：${missing.join('、')}` }
    : { label: '已配置', tone: 'ready', detail: '保存后发送测试邮件，确认服务商连接与投递结果。' };
});

function flash(message, tone = 'success') { saved.value = message; savedTone.value = tone; }
function flashError(message) { flash(message, 'error'); }
function navBadge(path) {
  if (path === '/admin/comments') return comments.value.filter((item) => item.status === 'PENDING').length;
  if (path === '/admin/inbox') return contactMessages.value.filter((item) => item.status === 'RECEIVED').length;
  if (path === '/admin/subscribers') return subscriptions.value.filter((item) => item.status === 'PENDING').length;
  return 0;
}
function syncEditor() {
  if (!editorMode.value) return;
  editorReady.value = false;
  const existing = editId.value ? adminPosts.value.find((post) => String(post.id) === String(editId.value)) : null;
  editor.value = existing ? { ...existing, tags: [...(existing.tags || [])] } : {
    id: `new-${Date.now()}`, title: '', slug: '', excerpt: '', content: '', category: categoriesAdmin.value[0]?.name || '人工智能', tags: [],
    status: 'DRAFT', cover: '/assets/hero-circuit.jpg', coverAlt: '', readMinutes: 5, seoTitle: '', seoDescription: '', canonicalUrl: '', pinned: false, scheduledAt: null,
  };
  nextTick(() => { editorReady.value = true; });
}

async function loadAdminData() {
  const settingsVersionAtLoad = settingsMutationVersion;
  saved.value = '';
  adminLoading.value = true;
  adminLoadError.value = '';
  try {
    const profile = await adminRequest('/admin/me');
    currentUser.value = profile;
    const access = adminAccessForRole(profile.role);
    const canManageSystem = access.canManageSystem;
    const [dashboard, postData, commentData, userData, settingData, mediaData, taxonomy, tagData, logData, backups, contacts, subscriberData] = await Promise.all([
      adminRequest('/admin/dashboard'), adminRequest('/admin/posts'), access.canReadCommunity ? adminRequest('/admin/comments') : Promise.resolve([]), canManageSystem ? adminRequest('/admin/users') : Promise.resolve([]),
      canManageSystem ? adminRequest('/admin/settings') : apiRequest('/site'), apiRequest('/media'), adminRequest('/admin/categories'), adminRequest('/admin/tags'),
      canManageSystem ? adminRequest('/admin/logs') : Promise.resolve([]), canManageSystem ? adminRequest('/admin/backups') : Promise.resolve([]), access.canReadCommunity ? adminRequest('/admin/contact-messages') : Promise.resolve([]), access.canReadCommunity ? adminRequest('/admin/subscriptions') : Promise.resolve([]),
    ]);
    dashboardData.value = dashboard;
    adminPosts.value = postData || [];
    comments.value = commentData || [];
    users.value = userData || [];
    if (settingsVersionAtLoad === settingsMutationVersion) settings.value = { ...settings.value, ...settingData };
    media.value = mediaData || [];
    categoriesAdmin.value = taxonomy || [];
    tagsAdmin.value = tagData || [];
    logs.value = logData || [];
    backupJobs.value = backups || [];
    contactMessages.value = contacts || [];
    subscriptions.value = subscriberData || [];
    apiStatus.value = `MySQL 已连接 · ${dashboard.posts} 篇文章`;
    if (!canAccessAdminRoute(profile.role, route.path)) await router.replace('/admin');
    syncEditor();
  } catch (error) { apiStatus.value = '后端未连接'; adminLoadError.value = error.message || '后台数据加载失败，请检查后端服务'; flashError(adminLoadError.value); }
  finally { adminLoading.value = false; }
}
async function doLogin() {
  loginError.value = '';
  const token = createAdminToken(login.value.email, login.value.password);
  try {
    localStorage.setItem('signal-admin-token', token);
    await apiRequest('/admin/dashboard', { headers: { Authorization: `Basic ${token}` } });
    authed.value = true;
    localStorage.setItem('signal-admin-auth', 'true');
    await loadAdminData();
  } catch (error) {
    localStorage.removeItem('signal-admin-token'); localStorage.removeItem('signal-admin-auth'); authed.value = false;
    loginError.value = error.message || '账号或密码不正确';
  }
}
function logout() { authed.value = false; localStorage.removeItem('signal-admin-auth'); localStorage.removeItem('signal-admin-token'); router.push('/admin'); }

async function savePost(status = editor.value.status, silent = false) {
  if (saving.value) return;
  saving.value = true;
  autosave.cancel();
  const value = { ...editor.value, status, publishedAt: status === 'PUBLISHED' ? (editor.value.publishedAt || new Date().toISOString().slice(0, 10)) : editor.value.publishedAt || null, tags: Array.isArray(editor.value.tags) ? editor.value.tags : String(editor.value.tags || '').split(',').map((tag) => tag.trim()).filter(Boolean) };
  const payload = { slug: value.slug || value.title.toLowerCase().replace(/\s+/g, '-'), title: value.title, excerpt: value.excerpt || '', content: value.content || '', cover: value.cover, coverAlt: value.coverAlt, category: value.category, tags: value.tags, status, authorName: value.authorName || '林默', publishedAt: value.publishedAt, readMinutes: Number(value.readMinutes) || 5, scheduledAt: value.scheduledAt || null, seoTitle: value.seoTitle || value.title, seoDescription: value.seoDescription || value.excerpt, canonicalUrl: value.canonicalUrl || null, pinned: Boolean(value.pinned) };
  try {
    const result = value.id && Number.isFinite(Number(value.id)) ? await adminRequest(`/admin/posts/${value.id}`, { method: 'PUT', body: JSON.stringify(payload) }) : await adminRequest('/admin/posts', { method: 'POST', body: JSON.stringify(payload) });
    autosaveSuppressed = true;
    Object.assign(editor.value, result);
    await nextTick();
    autosaveSuppressed = false;
    const index = adminPosts.value.findIndex((post) => post.id === result.id);
    if (index >= 0) adminPosts.value[index] = result; else adminPosts.value.unshift(result);
    apiStatus.value = 'MySQL 已连接';
    flash(silent ? '自动保存完成' : status === 'PUBLISHED' ? '文章已发布' : '草稿已保存');
    if (!silent && status === 'PUBLISHED') setTimeout(() => router.push('/admin/posts'), 450);
  } catch (error) { apiStatus.value = '后端未连接'; flashError(error.message || '保存失败，请检查后端服务'); }
  finally { autosaveSuppressed = false; saving.value = false; }
}
const autosave = createEditorAutosave();
let autosaveSuppressed = false;
watch(editor, () => {
  if (saving.value || autosaveSuppressed || !editorReady.value || !editorMode.value || !editor.value.title?.trim()) return;
  const editorId = String(editor.value.id ?? 'new');
  flash('有未保存修改', 'info');
  autosave.schedule(editorId, () => {
    if (!editorMode.value || String(editor.value.id ?? 'new') !== editorId) return;
    return savePost(statusForAutosave(editor.value.status), true);
  });
}, { deep: true });
watch(() => route.path, () => { autosave.cancel(); menuOpen.value = false; syncEditor(); if (editorMode.value) loadRevisions(); }, { immediate: true });
onMounted(() => { if (authed.value) loadAdminData(); });
onUnmounted(() => autosave.cancel());

async function deletePost(post) { const permanent = post.status === 'TRASHED'; if (!confirm(permanent ? `永久删除文章“${post.title}”后无法恢复，确定继续吗？` : `确定将文章“${post.title}”移入回收站吗？`)) return; try { await adminRequest(`/admin/posts/${post.id}${permanent ? '?permanent=true' : ''}`, { method: 'DELETE' }); if (permanent) adminPosts.value = adminPosts.value.filter((item) => item.id !== post.id); else post.status = 'TRASHED'; flash(permanent ? '文章已永久删除' : '文章已移入回收站'); } catch (error) { flashError(error.message || '文章删除失败'); } }
async function restorePost(post) { try { Object.assign(post, await adminRequest(`/admin/posts/${post.id}/restore`, { method: 'POST' })); flash('文章已恢复为草稿'); } catch (error) { flashError(error.message || '文章恢复失败'); } }
async function bulkStatus(status) { if (!selectedPosts.value.length) return; try { await adminRequest('/admin/posts/batch', { method: 'PATCH', body: JSON.stringify({ ids: selectedPosts.value.map(Number), status }) }); adminPosts.value.forEach((post) => { if (selectedPosts.value.map(String).includes(String(post.id))) post.status = status; }); flash(`已更新 ${selectedPosts.value.length} 篇文章`); selectedPosts.value = []; } catch (error) { flashError(error.message || '批量操作失败'); } }
async function loadRevisions() { if (!editId.value || !Number.isFinite(Number(editId.value))) return; try { revisions.value = await adminRequest(`/admin/posts/${editId.value}/revisions`); } catch { revisions.value = []; } }
async function restoreRevision(id) { if (!confirm('恢复此版本会创建新的草稿版本，继续吗？')) return; try { Object.assign(editor.value, await adminRequest(`/admin/posts/${editId.value}/revisions/${id}/restore`, { method: 'POST' })); flash('已恢复为新草稿'); await loadRevisions(); } catch (error) { flashError(error.message || '恢复失败'); } }

async function createCategory() { if (!taxonomyDraft.value.name.trim()) return flashError('请输入分类名称'); try { categoriesAdmin.value.push(await adminRequest('/admin/categories', { method: 'POST', body: JSON.stringify(taxonomyDraft.value) })); taxonomyDraft.value = { name: '', slug: '', description: '' }; flash('分类已创建'); } catch (error) { flashError(error.message || '分类创建失败'); } }
async function createTag() { if (!tagDraft.value.name.trim()) return flashError('请输入标签名称'); try { tagsAdmin.value.push(await adminRequest('/admin/tags', { method: 'POST', body: JSON.stringify(tagDraft.value) })); tagDraft.value = { name: '', slug: '', description: '' }; flash('标签已创建'); } catch (error) { flashError(error.message || '标签创建失败'); } }
async function saveCategory(item) { try { Object.assign(item, await adminRequest(`/admin/categories/${item.id}`, { method: 'PUT', body: JSON.stringify(item) })); flash('分类已更新'); } catch (error) { flashError(error.message || '分类更新失败'); } }
async function saveTag(item) { try { Object.assign(item, await adminRequest(`/admin/tags/${item.id}`, { method: 'PUT', body: JSON.stringify(item) })); flash('标签已更新'); } catch (error) { flashError(error.message || '标签更新失败'); } }
async function removeCategory(item) { if (!confirm(`确定删除分类“${item.name}”？`)) return; try { await adminRequest(`/admin/categories/${item.id}`, { method: 'DELETE' }); categoriesAdmin.value = categoriesAdmin.value.filter((value) => value.id !== item.id); flash('分类已删除'); } catch (error) { flashError(error.message || '分类删除失败'); } }
async function removeTag(item) { if (!confirm(`确定删除标签“${item.name}”？`)) return; try { await adminRequest(`/admin/tags/${item.id}`, { method: 'DELETE' }); tagsAdmin.value = tagsAdmin.value.filter((value) => value.id !== item.id); flash('标签已删除'); } catch (error) { flashError(error.message || '标签删除失败'); } }

async function uploadMedia(event) { for (const file of event.target.files || []) { try { const body = new FormData(); body.append('file', file); media.value.unshift(await adminRequest('/admin/media', { method: 'POST', headers: {}, body })); flash('媒体已上传'); } catch (error) { flashError(error.message || '媒体上传失败'); } } event.target.value = ''; }
async function saveMedia(item) { try { Object.assign(item, await adminRequest(`/admin/media/${item.id}`, { method: 'PATCH', body: JSON.stringify({ filename: item.filename, altText: item.altText || '' }) })); flash('媒体信息已保存'); } catch (error) { flashError(error.message || '媒体信息保存失败'); } }
function mediaPreviewUrl(item) { return item.previewVersion ? `${item.url}?v=${item.previewVersion}` : item.url; }
async function replaceMedia(item, event) { const file = event.target.files?.[0]; if (!file) return; try { const body = new FormData(); body.append('file', file); const result = await adminRequest(`/admin/media/${item.id}/replace`, { method: 'POST', headers: {}, body }); Object.assign(item, result, { previewVersion: Date.now() }); flash('媒体文件已替换，引用它的文章会继续使用新图片'); } catch (error) { flashError(error.message || '媒体替换失败'); } finally { event.target.value = ''; } }
async function deleteMedia(item) { if (!item.deletable) return flashError(`该媒体仍被 ${item.referenceCount} 篇文章引用，请先替换这些文章的封面`); if (!confirm(`永久删除媒体“${item.filename}”及磁盘文件？`)) return; try { await adminRequest(`/admin/media/${item.id}`, { method: 'DELETE' }); media.value = media.value.filter((value) => value.id !== item.id); flash('媒体及磁盘文件已删除'); } catch (error) { flashError(error.message || '媒体删除失败'); } }

async function moderate(item, status) { const previous = item.status; item.status = status; try { await adminRequest(`/admin/comments/${item.id}`, { method: 'PATCH', body: JSON.stringify({ status }) }); flash('评论状态已更新'); } catch (error) { item.status = previous; flashError(error.message || '评论状态更新失败'); } }
async function replyComment(item) { const content = (commentReplies.value[item.id] || '').trim(); if (content.length < 2) return flashError('回复至少需要 2 个字'); try { comments.value.push(await adminRequest(`/admin/comments/${item.id}/reply`, { method: 'POST', body: JSON.stringify({ content }) })); commentReplies.value[item.id] = ''; flash('回复已发布'); } catch (error) { flashError(error.message || '回复发布失败'); } }
async function deleteComment(item) { if (!confirm('确定删除这条评论？')) return; try { await adminRequest(`/admin/comments/${item.id}`, { method: 'DELETE' }); comments.value = comments.value.filter((value) => value.id !== item.id); flash('评论已删除'); } catch (error) { flashError(error.message || '评论删除失败'); } }

function beginUpdate(generations, pending, id) { const generation = (generations.get(id) || 0) + 1; generations.set(id, generation); pending.add(id); return generation; }
function isCurrentUpdate(generations, id, generation) { return generations.get(id) === generation; }
function finishUpdate(generations, pending, id, generation) { if (isCurrentUpdate(generations, id, generation)) { generations.delete(id); pending.delete(id); } }
async function updateContact(item, status) {
  const previous = item.status;
  const generation = beginUpdate(contactUpdateGenerations, pendingContactUpdates, item.id);
  item.status = status;
  try {
    const result = await adminRequest(`/admin/contact-messages/${item.id}`, { method: 'PATCH', body: JSON.stringify({ status }) });
    if (contactUpdateGenerations.get(item.id) === generation) { Object.assign(item, result); flash('反馈状态已更新'); }
  } catch (error) {
    if (contactUpdateGenerations.get(item.id) === generation) { item.status = previous; flashError(error.message || '反馈状态更新失败'); }
  } finally { finishUpdate(contactUpdateGenerations, pendingContactUpdates, item.id, generation); }
}
async function deleteContact(item) { if (!confirm(`确定删除工单 ${item.ticket}？`)) return; try { await adminRequest(`/admin/contact-messages/${item.id}`, { method: 'DELETE' }); contactMessages.value = contactMessages.value.filter((value) => value.id !== item.id); flash('反馈已删除'); } catch (error) { flashError(error.message || '反馈删除失败'); } }
async function updateSubscription(item, status) {
  const previous = item.status;
  const generation = beginUpdate(subscriptionUpdateGenerations, pendingSubscriptionUpdates, item.id);
  item.status = status;
  try {
    const result = await adminRequest(`/admin/subscriptions/${item.id}`, { method: 'PATCH', body: JSON.stringify({ status }) });
    if (subscriptionUpdateGenerations.get(item.id) === generation) { Object.assign(item, result); flash('订阅状态已更新'); }
  } catch (error) {
    if (subscriptionUpdateGenerations.get(item.id) === generation) { item.status = previous; flashError(error.message || '订阅状态更新失败'); }
  } finally { finishUpdate(subscriptionUpdateGenerations, pendingSubscriptionUpdates, item.id, generation); }
}
async function deleteSubscription(item) { if (!confirm(`确定删除订阅者 ${item.email}？`)) return; try { await adminRequest(`/admin/subscriptions/${item.id}`, { method: 'DELETE' }); subscriptions.value = subscriptions.value.filter((value) => value.id !== item.id); flash('订阅者已删除'); } catch (error) { flashError(error.message || '订阅者删除失败'); } }

async function createUser() { if (!userDraft.value.name.trim() || !userDraft.value.email.trim()) return flashError('姓名和邮箱不能为空'); try { users.value.push(await adminRequest('/admin/users', { method: 'POST', body: JSON.stringify(userDraft.value) })); userDraft.value = { name: '', email: '', loginName: '', password: '', role: 'AUTHOR', status: 'INVITED' }; flash('用户已创建'); } catch (error) { flashError(error.message || '用户创建失败'); } }
async function saveUser(user) { try { Object.assign(user, await adminRequest(`/admin/users/${user.id}`, { method: 'PUT', body: JSON.stringify(user) })); user.password = ''; flash('用户信息已保存'); } catch (error) { flashError(error.message || '用户保存失败'); } }
async function deleteUser(user) { if (!confirm(`确定删除用户 ${user.email}？`)) return; try { await adminRequest(`/admin/users/${user.id}`, { method: 'DELETE' }); users.value = users.value.filter((value) => value.id !== user.id); flash('用户已删除'); } catch (error) { flashError(error.message || '用户删除失败'); } }

async function persistSettings({ announce = true } = {}) {
  if (adminLoading.value || adminLoadError.value) throw new Error(adminLoadError.value || '后台数据尚未加载完成');
  if (settingsSaving.value) throw new Error('设置正在保存，请稍候');
  if (mailConfigurationState.value.tone === 'error') throw new Error(mailConfigurationState.value.detail);
  settingsSaving.value = true;
  try {
    const settingsPayload = Object.fromEntries(Object.entries(settings.value).filter(([key]) => key !== 'mail.passwordConfigured'));
    const savedSettings = await adminRequest('/admin/settings', { method: 'PUT', body: JSON.stringify(settingsPayload) });
    settingsMutationVersion += 1;
    settings.value = { ...settings.value, ...savedSettings };
    const result = Object.fromEntries(Object.entries(savedSettings).filter(([key]) => !key.startsWith('mail.')));
    applySite(result);
    apiStatus.value = 'MySQL 已连接';
    if (announce) flash('设置已保存，公开页面已同步');
    return savedSettings;
  } catch (error) { apiStatus.value = '后端未连接'; if (announce) flashError(error.message || '设置保存失败'); throw error; }
  finally { settingsSaving.value = false; }
}
async function saveSettings() { try { await persistSettings(); } catch { /* The visible notice is set by persistSettings. */ } }
async function testEmail() {
  const result = await runMailTest({
    recipient: emailTestRecipient.value,
    saveCurrentSettings: () => persistSettings({ announce: false }),
    sendTest: (to) => adminRequest('/admin/email/test', { method: 'POST', body: JSON.stringify({ to }) }),
    updateStatus: (status) => { mailTestStatus.value = status; },
  });
  flash(result.message, result.tone);
}
async function changePassword() { const value = passwordForm.value; if (value.newPassword.length < 10) return flashError('新密码至少需要 10 个字符'); if (value.newPassword !== value.confirmPassword) return flashError('两次输入的新密码不一致'); try { await adminRequest('/admin/account/password', { method: 'PUT', body: JSON.stringify({ currentPassword: value.currentPassword, newPassword: value.newPassword }) }); logout(); loginError.value = '密码已更新，请使用新密码登录'; } catch (error) { flashError(error.message || '密码更新失败'); } }
async function runBackup() { try { flash('正在导出并校验备份', 'info'); backupJobs.value.unshift(await adminRequest('/admin/backups', { method: 'POST' })); flash('备份完成，SHA-256 已校验'); } catch (error) { flashError(error.message || '备份失败'); } }
</script>

<template>
  <main v-if="!authed" class="admin-login"><div class="admin-login-mark"><span /><span /><span /></div><span class="admin-kicker">SIGNAL NOTES / ADMIN</span><h1>管理你的<br />技术记录。</h1><p>发布文章、整理媒体和处理读者反馈。</p><form @submit.prevent="doLogin"><label>管理员账号<input v-model="login.email" autocomplete="username" /></label><label>密码<input v-model="login.password" type="password" autocomplete="current-password" /></label><button class="button button-primary">进入后台 <ArrowRight :size="17" /></button><small v-if="loginError">{{ loginError }}</small></form><span class="admin-demo-hint">演示账号：admin / signal2026</span></main>
  <div v-else class="admin-shell" :class="{ 'has-error-notice': savedTone === 'error', 'has-info-notice': savedTone === 'info' }">
    <aside :class="{ 'is-open': menuOpen }"><div class="admin-aside-brand"><span class="brand-mark"><i /><i /><i /></span><div><b>{{ settings.siteName || '脉冲笔记' }}</b><small>ADMIN / CONSOLE</small></div></div><nav><RouterLink v-for="item in nav" :key="item[0]" :to="item[0]"><component :is="item[2]" :size="17" />{{ item[1] }}<em v-if="navBadge(item[0])">{{ navBadge(item[0]) }}</em></RouterLink></nav><div class="admin-aside-bottom"><RouterLink to="/blog"><ArrowRight :size="15" />查看站点</RouterLink><button @click="logout"><LogOut :size="15" />退出登录</button></div></aside>
    <div class="admin-body"><header class="admin-topbar"><button class="admin-menu-toggle" title="打开导航" aria-label="打开导航" @click="menuOpen = !menuOpen"><X v-if="menuOpen" :size="19" /><Menu v-else :size="19" /></button><div class="admin-breadcrumb">ADMIN <span>/</span> SIGNAL NOTES</div><div class="admin-top-actions"><span class="admin-online"><i />{{ apiStatus }}</span><span class="admin-user">林默 · 管理员</span></div></header>

      <main v-if="editorMode" class="admin-content editor-page"><div class="editor-top"><RouterLink to="/admin/posts"><ArrowLeft :size="16" />文章列表</RouterLink><div><span>{{ saved || '等待输入' }}</span><button class="button" :disabled="saving" @click="previewing = !previewing"><Eye :size="15" />{{ previewing ? '返回编辑' : '预览' }}</button><button class="button" :disabled="saving" @click="savePost('DRAFT')"><Save :size="15" />{{ saving ? '保存中' : '保存草稿' }}</button><button class="button button-primary" :disabled="saving || publishChecklist.some((item) => !item.ok)" @click="savePost('PUBLISHED')">{{ saving ? '保存中' : '发布' }} <ArrowRight :size="15" /></button></div></div><div v-if="previewing" class="editor-live-preview"><span>仅管理员可见 · 当前草稿预览</span><h1>{{ editor.title || '未命名文章' }}</h1><p>{{ editor.excerpt }}</p><pre>{{ editor.content }}</pre></div><div v-else class="editor-grid"><section class="editor-main"><textarea v-model="editor.title" class="editor-title" rows="2" maxlength="240" placeholder="文章标题" /><textarea v-model="editor.excerpt" class="editor-excerpt" rows="3" placeholder="写一段摘要" /><textarea v-model="editor.content" class="editor-content" placeholder="使用 Markdown 写作..." /><footer class="editor-statusbar"><span>{{ (editor.content || '').length }} 字</span><span>约 {{ Math.max(1, Math.ceil((editor.content || '').length / 500)) }} 分钟</span><button type="button" @click="loadRevisions"><History :size="14" />版本历史</button></footer></section><aside class="editor-sidebar"><div class="editor-card"><h3>发布设置</h3><label>状态<select v-model="editor.status"><option value="DRAFT">草稿</option><option value="PENDING">待审核</option><option value="SCHEDULED">定时发布</option><option value="PUBLISHED">已发布</option><option value="OFFLINE">已下线</option><option value="TRASHED">回收站</option></select></label><label v-if="editor.status === 'SCHEDULED'">计划时间<input v-model="editor.scheduledAt" type="datetime-local" /></label><label>分类<select v-model="editor.category"><option v-for="category in categoriesAdmin" :key="category.id">{{ category.name }}</option></select></label><label>标签<input v-model="editor.tags" placeholder="AI, 工程" /></label><label>预计阅读分钟<input v-model.number="editor.readMinutes" type="number" min="1" /></label><label class="admin-check"><input v-model="editor.pinned" type="checkbox" />置顶文章</label></div><div class="editor-card"><h3>SEO 与媒体</h3><label>Slug<input v-model="editor.slug" /></label><label>SEO 标题<input v-model="editor.seoTitle" maxlength="240" /></label><label>SEO 描述<textarea v-model="editor.seoDescription" rows="3" maxlength="320" /></label><label>Canonical URL<input v-model="editor.canonicalUrl" type="url" /></label><label>封面 URL<input v-model="editor.cover" /></label><label>图片替代文本<input v-model="editor.coverAlt" /></label></div><div class="editor-card publish-checklist"><h3>发布检查</h3><div v-for="item in publishChecklist" :key="item.label" :class="{ ok: item.ok }"><Check :size="14" />{{ item.label }}</div></div><div v-if="revisions.length" class="editor-card revision-list"><h3>版本历史</h3><button v-for="revision in revisions" :key="revision.id" type="button" @click="restoreRevision(revision.id)"><span>#{{ revision.versionNo }} · {{ revision.editor }}</span><small>{{ revision.changeSummary }}</small></button></div></aside></div></main>

      <main v-else class="admin-content">
        <template v-if="section === 'dashboard'"><header class="admin-page-header"><div><span>OVERVIEW / 001</span><h1>内容工作台</h1><p>统计口径为数据库当前快照。</p></div><RouterLink class="button button-primary" to="/admin/posts/new"><Plus :size="17" />新建文章</RouterLink></header><section class="admin-stat-grid"><div class="admin-stat"><FileText :size="18" /><span>已发布文章</span><strong>{{ dashboardData.published || 0 }}</strong><small>当前状态</small></div><div class="admin-stat"><ClipboardList :size="18" /><span>待处理评论</span><strong>{{ dashboardData.pendingComments || 0 }}</strong><small>需要审核</small></div><div class="admin-stat"><Archive :size="18" /><span>草稿</span><strong>{{ dashboardData.drafts || 0 }}</strong><small>继续编辑</small></div><div class="admin-stat"><BarChart3 :size="18" /><span>累计访问量</span><strong>{{ dashboardData.views || 0 }}</strong><small>数据库口径</small></div></section><section class="admin-dashboard-grid"><section class="admin-panel chart-panel"><div class="panel-heading"><div><span>RECENT NOTES</span><h2>最近文章</h2></div><RouterLink to="/admin/posts">查看全部</RouterLink></div><div class="admin-table compact"><RouterLink v-for="post in adminPosts.slice(0, 6)" :key="post.id" class="table-row" :to="`/admin/posts/${post.id}/edit`"><strong>{{ post.title }}</strong><span>{{ post.category }}</span><span class="status">{{ post.status }}</span><time>{{ post.updatedAt }}</time></RouterLink></div></section><section class="admin-panel chart-panel"><div class="panel-heading"><div><span>POPULAR / {{ timeRange }} DAYS</span><h2>热门文章</h2></div><select v-model="timeRange"><option :value="7">7 天</option><option :value="30">30 天</option><option :value="90">90 天</option></select></div><div class="popular-list"><div v-for="item in dashboardData.popular" :key="item.id"><strong>{{ item.title }}</strong><span>{{ item.views }} 阅读</span></div><p v-if="!dashboardData.popular?.length" class="admin-empty">暂无统计数据</p></div></section></section></template>

        <template v-else-if="section === 'posts'"><header class="admin-page-header"><div><span>CONTENT / POSTS</span><h1>文章</h1><p>{{ adminPosts.length }} 篇内容，按状态和更新时间管理。</p></div><RouterLink class="button button-primary" to="/admin/posts/new"><Plus :size="17" />新建文章</RouterLink></header><div v-if="saved" :class="['admin-notice', `is-${savedTone}`]">{{ saved }}</div><div class="admin-toolbar"><div class="admin-search"><Search :size="16" /><input v-model="query" placeholder="搜索标题" /></div><div class="segmented"><button v-for="item in ['ALL', 'PUBLISHED', 'DRAFT', 'PENDING', 'SCHEDULED', 'OFFLINE', 'TRASHED']" :key="item" :class="{ active: filter === item }" @click="filter = item">{{ item }}</button></div></div><div v-if="selectedPosts.length" class="bulk-bar"><strong>已选择 {{ selectedPosts.length }} 篇</strong><button @click="bulkStatus('PUBLISHED')">发布</button><button @click="bulkStatus('OFFLINE')">下线</button><button @click="bulkStatus('TRASHED')">移入回收站</button><button @click="bulkStatus('DRAFT')">恢复为草稿</button></div><section class="admin-panel"><div class="admin-table post-admin-table"><div class="table-head"><span>选择</span><span>文章</span><span>分类</span><span>状态</span><span>更新时间</span><span>操作</span></div><div v-for="post in filteredPosts" :key="post.id" class="table-row"><input v-model="selectedPosts" type="checkbox" :value="String(post.id)" :aria-label="`选择 ${post.title}`" /><RouterLink :to="`/admin/posts/${post.id}/edit`"><strong>{{ post.title }}</strong><small>{{ post.excerpt }}</small></RouterLink><span>{{ post.category }}</span><span class="status">{{ post.status }}</span><time>{{ post.updatedAt }}</time><div class="row-actions"><RouterLink :to="`/admin/posts/${post.id}/edit`" title="编辑文章" :aria-label="`编辑 ${post.title}`"><FileText :size="16" /></RouterLink><button v-if="post.status !== 'TRASHED'" title="移入回收站" :aria-label="`删除 ${post.title}`" @click="deletePost(post)"><Trash2 :size="16" /></button><button v-else title="恢复文章" :aria-label="`恢复 ${post.title}`" @click="restorePost(post)"><RefreshCw :size="16" /></button><button v-if="post.status === 'TRASHED'" class="danger" title="永久删除" :aria-label="`永久删除 ${post.title}`" @click="deletePost(post)"><Trash2 :size="16" /></button></div></div><p v-if="!filteredPosts.length" class="admin-empty">没有符合条件的文章。</p></div></section></template>

        <template v-else-if="section === 'taxonomy'"><header class="admin-page-header"><div><span>CONTENT / TAXONOMY</span><h1>分类与标签</h1><p>新增、编辑和删除都执行唯一性与文章引用检查。</p></div></header><div v-if="saved" class="admin-success">{{ saved }}</div><div class="taxonomy-layout"><section class="admin-panel taxonomy-panel"><div class="panel-heading"><div><span>CATEGORIES</span><h2>分类</h2></div></div><form class="taxonomy-form" @submit.prevent="createCategory"><input v-model="taxonomyDraft.name" placeholder="分类名称" /><input v-model="taxonomyDraft.slug" placeholder="unique-slug" /><input v-model="taxonomyDraft.description" placeholder="分类说明" /><button class="button button-primary"><Plus :size="15" />新增分类</button></form><div class="taxonomy-list taxonomy-edit-list"><div v-for="item in categoriesAdmin" :key="item.id"><FolderTree :size="16" /><div><input v-model="item.name" aria-label="分类名称" /><input v-model="item.slug" aria-label="分类别名" /><input v-model="item.description" aria-label="分类说明" /></div><button class="icon-button" title="保存分类" aria-label="保存分类" @click="saveCategory(item)"><Save :size="15" /></button><button class="icon-button danger" title="删除分类" aria-label="删除分类" @click="removeCategory(item)"><Trash2 :size="15" /></button></div></div></section><section class="admin-panel taxonomy-panel"><div class="panel-heading"><div><span>TAGS</span><h2>标签</h2></div></div><form class="taxonomy-form" @submit.prevent="createTag"><input v-model="tagDraft.name" placeholder="标签名称" /><input v-model="tagDraft.slug" placeholder="tag-slug" /><input v-model="tagDraft.description" placeholder="标签说明" /><button class="button button-primary"><Plus :size="15" />新增标签</button></form><div class="taxonomy-list taxonomy-edit-list"><div v-for="item in tagsAdmin" :key="item.id"><Hash :size="16" /><div><input v-model="item.name" aria-label="标签名称" /><input v-model="item.slug" aria-label="标签别名" /><input v-model="item.description" aria-label="标签说明" /></div><button class="icon-button" title="保存标签" aria-label="保存标签" @click="saveTag(item)"><Save :size="15" /></button><button class="icon-button danger" title="删除标签" aria-label="删除标签" @click="removeTag(item)"><Trash2 :size="15" /></button></div></div></section></div></template>

        <template v-else-if="section === 'media'"><header class="admin-page-header"><div><span>LIBRARY / MEDIA</span><h1>媒体库</h1><p>上传、预览、替换或删除图片；被文章引用的图片会受到保护。</p></div><label class="button button-primary"><Upload :size="16" />上传文件<input type="file" hidden multiple accept="image/jpeg,image/png,image/webp" @change="uploadMedia" /></label></header><div v-if="saved" class="admin-success">{{ saved }}</div><section class="media-grid"><article v-for="item in media" :key="item.id"><div class="media-thumb"><img :src="mediaPreviewUrl(item)" :alt="item.altText || item.filename" /></div><div class="media-fields"><label>文件名<input v-model="item.filename" /></label><label>替代文本<input v-model="item.altText" /></label><span>{{ item.mimeType }} · {{ item.size }} bytes</span><span class="media-usage" :class="{ 'is-blocked': !item.deletable }">{{ item.referenceCount ? `被 ${item.referenceCount} 篇文章引用，需先替换封面` : '未被文章引用，可以删除' }}</span><div class="media-card-actions"><button class="icon-button" title="保存媒体信息" aria-label="保存媒体信息" @click="saveMedia(item)"><Save :size="15" /></button><label class="icon-button" title="替换图片文件" aria-label="替换图片文件"><RefreshCw :size="15" /><input type="file" hidden :accept="item.mimeType" @change="replaceMedia(item, $event)" /></label><button class="icon-button danger" :disabled="!item.deletable" :title="item.deletable ? '删除媒体及磁盘文件' : `被 ${item.referenceCount} 篇文章引用，暂不能删除`" aria-label="删除媒体" @click="deleteMedia(item)"><Trash2 :size="15" /></button></div></div></article><p v-if="!media.length" class="admin-empty">还没有上传媒体。</p></section></template>

        <template v-else-if="section === 'comments'">
          <header class="admin-page-header"><div><span>COMMUNITY / MODERATION</span><h1>评论审核</h1><p>{{ navBadge('/admin/comments') }} 条评论等待处理。</p></div></header>
          <div v-if="saved" :class="['admin-notice', `is-${savedTone}`]">{{ saved }}</div>
          <section class="admin-panel moderation-list">
            <p v-if="adminLoading" class="admin-empty">正在加载评论…</p>
            <div v-else-if="adminLoadError" class="admin-empty admin-error-state"><p>{{ adminLoadError }}</p><button class="button" @click="loadAdminData">重新加载</button></div>
            <template v-else>
              <article v-for="item in comments" :key="item.id"><div class="moderation-meta"><strong>{{ item.authorName }}</strong><span class="risk-tag">{{ item.reportCount ? `举报 ${item.reportCount}` : item.status }}</span></div><p>{{ item.content }}</p><small>文章：{{ item.postSlug }}<template v-if="item.reportReason"> · 原因：{{ item.reportReason }}</template></small><div class="moderation-actions"><button class="button button-primary" @click="moderate(item, 'APPROVED')"><Check :size="15" />通过</button><button class="button" @click="moderate(item, 'SPAM')">标记垃圾</button><button class="icon-button danger" title="删除评论" aria-label="删除评论" @click="deleteComment(item)"><Trash2 :size="15" /></button></div><form class="moderation-reply" @submit.prevent="replyComment(item)"><input v-model="commentReplies[item.id]" maxlength="2000" placeholder="以站点管理员身份回复" /><button class="button">发布回复</button></form></article>
              <p v-if="!comments.length" class="admin-empty">暂无评论。</p>
            </template>
          </section>
        </template>

        <template v-else-if="section === 'inbox'">
          <header class="admin-page-header"><div><span>COMMUNITY / INBOX</span><h1>反馈收件箱</h1><p>{{ navBadge('/admin/inbox') }} 条反馈等待处理。</p></div></header>
          <div v-if="saved" :class="['admin-notice', `is-${savedTone}`]">{{ saved }}</div>
          <section class="admin-panel inbox-list">
            <p v-if="adminLoading" class="admin-empty">正在加载反馈…</p>
            <div v-else-if="adminLoadError" class="admin-empty admin-error-state"><p>{{ adminLoadError }}</p><button class="button" @click="loadAdminData">重新加载</button></div>
            <template v-else>
              <article v-for="item in contactMessages" :key="item.id"><header><div><strong>{{ item.subject }}</strong><span>{{ item.name }} · {{ item.email }}</span></div><time>{{ item.createdAt?.slice(0, 16).replace('T', ' ') }}</time></header><p>{{ item.message }}</p><footer><code>{{ item.ticket }}</code><select :value="item.status" :disabled="pendingContactUpdates.has(item.id)" @change="updateContact(item, $event.target.value)"><option value="RECEIVED">待处理</option><option value="IN_PROGRESS">处理中</option><option value="RESOLVED">已解决</option><option value="ARCHIVED">已归档</option></select><a class="icon-button" :href="`mailto:${item.email}?subject=Re: ${encodeURIComponent(item.subject)}`" title="回复邮件" aria-label="回复邮件"><Mail :size="15" /></a><button class="icon-button danger" title="删除反馈" aria-label="删除反馈" @click="deleteContact(item)"><Trash2 :size="15" /></button></footer></article>
              <p v-if="!contactMessages.length" class="admin-empty">还没有收到反馈。</p>
            </template>
          </section>
        </template>

        <template v-else-if="section === 'subscribers'">
          <header class="admin-page-header"><div><span>COMMUNITY / SUBSCRIBERS</span><h1>订阅者</h1><p>{{ navBadge('/admin/subscribers') }} 位订阅者等待确认。</p></div></header>
          <div v-if="saved" :class="['admin-notice', `is-${savedTone}`]">{{ saved }}</div>
          <section class="admin-panel">
            <p v-if="adminLoading" class="admin-empty">正在加载订阅者…</p>
            <div v-else-if="adminLoadError" class="admin-empty admin-error-state"><p>{{ adminLoadError }}</p><button class="button" @click="loadAdminData">重新加载</button></div>
            <template v-else>
              <div class="subscriber-toolbar"><label>发送测试邮件到<input v-model="emailTestRecipient" type="email" placeholder="your@email.com" /></label><button class="button" @click="testEmail"><Mail :size="15" />测试邮件</button></div>
              <div class="subscriber-list"><div v-for="item in subscriptions" :key="item.id"><div><strong>{{ item.email }}</strong><small>订阅于 {{ item.createdAt?.slice(0, 10) }}<template v-if="item.confirmedAt"> · 确认于 {{ item.confirmedAt.slice(0, 10) }}</template></small></div><select :value="item.status" :disabled="pendingSubscriptionUpdates.has(item.id)" @change="updateSubscription(item, $event.target.value)"><option value="PENDING">待确认</option><option value="CONFIRMED">已确认</option><option value="UNSUBSCRIBED">已退订</option></select><button class="icon-button danger" title="删除订阅者" aria-label="删除订阅者" @click="deleteSubscription(item)"><Trash2 :size="15" /></button></div><p v-if="!subscriptions.length" class="admin-empty">还没有订阅者。</p></div>
            </template>
          </section>
        </template>

        <template v-else-if="section === 'users'">
          <header class="admin-page-header"><div><span>ACCESS / USERS</span><h1>用户与角色</h1><p>管理现有用户的登录名、密码、角色和状态；停用后立即无法登录。</p></div></header>
          <div v-if="saved" :class="['admin-notice', `is-${savedTone}`]">{{ saved }}</div>
          <section class="admin-panel">
            <p v-if="adminLoading" class="admin-empty">正在加载用户…</p>
            <div v-else-if="adminLoadError" class="admin-empty admin-error-state"><p>{{ adminLoadError }}</p><button class="button" @click="loadAdminData">重新加载</button></div>
            <template v-else>
              <form class="user-create-form" @submit.prevent="createUser"><input v-model="userDraft.name" placeholder="姓名" /><input v-model="userDraft.email" type="email" placeholder="邮箱" /><input v-model="userDraft.loginName" autocomplete="username" placeholder="登录名" /><input v-model="userDraft.password" type="password" autocomplete="new-password" placeholder="初始密码（至少 10 位）" /><select v-model="userDraft.role"><option value="ADMIN">管理员</option><option value="EDITOR">编辑</option><option value="AUTHOR">作者</option><option value="VIEWER">访客</option></select><select v-model="userDraft.status"><option value="ACTIVE">启用</option><option value="INVITED">待邀请</option><option value="DISABLED">停用</option></select><button class="button button-primary"><Plus :size="15" />新增用户</button></form>
              <div class="user-management-list"><div v-for="user in users" :key="user.id"><input v-model="user.name" aria-label="用户姓名" /><input v-model="user.email" type="email" aria-label="用户邮箱" /><input v-model="user.loginName" autocomplete="username" aria-label="登录名" placeholder="未设置" /><input v-model="user.password" type="password" autocomplete="new-password" aria-label="设置新密码" placeholder="留空则不修改" /><select v-model="user.role" aria-label="用户角色"><option value="ADMIN">管理员</option><option value="EDITOR">编辑</option><option value="AUTHOR">作者</option><option value="VIEWER">访客</option></select><select v-model="user.status" aria-label="用户状态"><option value="ACTIVE">启用</option><option value="INVITED">待邀请</option><option value="DISABLED">停用</option></select><span class="quick-action-buttons"><button class="icon-button" title="保存用户" :aria-label="`保存 ${user.email}`" @click="saveUser(user)"><Save :size="15" /></button><button class="icon-button danger" title="删除用户" :aria-label="`删除 ${user.email}`" @click="deleteUser(user)"><Trash2 :size="15" /></button></span></div><p v-if="!users.length" class="admin-empty">还没有可管理的用户。</p></div>
            </template>
          </section>
        </template>

        <template v-else-if="section === 'settings' && adminLoading">
          <header class="admin-page-header"><div><span>SYSTEM / SETTINGS</span><h1>站点设置</h1><p>正在加载站点设置</p></div></header>
          <p class="admin-empty">正在加载站点设置…</p>
        </template>

        <template v-else-if="section === 'settings' && adminLoadError">
          <header class="admin-page-header"><div><span>SYSTEM / SETTINGS</span><h1>站点设置</h1><p>站点设置加载失败</p></div></header>
          <div class="admin-empty admin-error-state"><p>{{ adminLoadError }}</p><button class="button" @click="loadAdminData">重新加载</button></div>
        </template>

        <template v-else-if="section === 'settings'"><header class="admin-page-header"><div><span>SYSTEM / SETTINGS</span><h1>站点设置</h1><p>公开页面文案、分享模板、邮件投递和账户安全。</p></div><button class="button button-primary" :disabled="adminLoading || Boolean(adminLoadError) || settingsSaving" @click="saveSettings"><Save :size="15" />{{ settingsSaving ? '保存中…' : '保存全部设置' }}</button></header><div v-if="saved" :class="['admin-notice', `is-${savedTone}`]"><Check v-if="savedTone === 'success'" :size="15" />{{ saved }}</div><div class="settings-grid"><section class="admin-panel settings-panel public-copy-panel"><div class="panel-heading"><div><span>PUBLIC CONTENT</span><h2>公开页面文案</h2></div></div><div class="public-copy-grid"><label>站点名称<input v-model="settings.siteName" /></label><label>站点短名<input v-model="settings.siteShortName" /></label><label>介绍页眉标<input v-model="settings.heroEyebrow" /></label><label>介绍页标题<input v-model="settings.heroTitle" /></label><label class="wide">介绍页摘要<textarea v-model="settings.heroSummary" rows="3" /></label><label>关于页标题<input v-model="settings.aboutTitle" /></label><label>关于页引导语<textarea v-model="settings.aboutLead" rows="2" /></label><label class="wide">关于页正文<textarea v-model="settings.aboutBody" rows="3" /></label><label>联系页标题<input v-model="settings.contactTitle" /></label><label>公开邮箱<input v-model="settings.publicEmail" type="email" /></label><label class="wide">联系页说明<textarea v-model="settings.contactIntro" rows="2" /></label><label>回复承诺<input v-model="settings.replyPromise" /></label><label class="wide">隐私说明<textarea v-model="settings.privacyContent" rows="3" /></label><label>页脚描述<input v-model="settings.footerDescription" /></label><label>版权文案<input v-model="settings.copyrightText" /></label><label>许可文案<input v-model="settings.licenseText" /></label><label>订阅标题<input v-model="settings.subscribeTitle" /></label><label class="wide">订阅说明<textarea v-model="settings.subscribeDescription" rows="2" /></label></div></section><section class="admin-panel settings-panel"><div class="panel-heading"><div><span>SHARE / POSTER</span><h2>分享海报</h2></div></div><label>默认模板<select v-model="settings.shareTemplate"><option value="landscape">横版卡片</option><option value="portrait">竖版海报</option></select></label><div class="share-setting-preview"><span>{{ settings.siteShortName }}</span><strong>{{ settings.siteName }}</strong><small>{{ settings.heroSummary }}</small><i>QR</i></div></section><section class="admin-panel settings-panel mail-panel"><div class="panel-heading"><div><span>SYSTEM / EMAIL</span><h2>邮件服务</h2></div></div><div :class="['mail-status', `is-${mailConfigurationState.tone}`]" role="status"><strong>{{ mailConfigurationState.label }}</strong><span>{{ mailConfigurationState.detail }}</span></div><p class="settings-help">未启用 SMTP 时，订阅和反馈仍会入库，但不会声称邮件已发送。465 端口会自动使用 SSL；163 邮箱的密码请填写客户端授权码。</p><div class="mail-settings-grid"><label class="mail-enabled"><input v-model="settings['mail.enabled']" type="checkbox" true-value="true" false-value="false" />启用 SMTP 投递</label><label>SMTP 主机<input v-model="settings['mail.host']" placeholder="smtp.example.com" /></label><label>端口<input v-model="settings['mail.port']" type="number" min="1" max="65535" /></label><label>用户名<input v-model="settings['mail.username']" /></label><label>密码<input v-model="settings['mail.password']" type="password" autocomplete="new-password" :placeholder="settings['mail.passwordConfigured'] === 'true' ? '已保存，留空则不修改' : '首次填写密码或邮箱授权码'" /></label><label>发件人<input v-model="settings['mail.from']" type="email" placeholder="noreply@example.com" /></label><label>反馈通知邮箱<input v-model="settings['mail.notificationTo']" type="email" placeholder="owner@example.com" /></label><label class="mail-enabled"><input v-model="settings['mail.starttls']" type="checkbox" true-value="true" false-value="false" />启用 STARTTLS（465 端口会自动忽略）</label><label class="mail-enabled"><input v-model="settings['mail.auth']" type="checkbox" true-value="true" false-value="false" />启用 SMTP 登录</label></div><div class="mail-settings-actions"><input v-model="emailTestRecipient" type="email" placeholder="测试收件地址" :disabled="mailTestStatus.pending" /><button class="button" :disabled="mailTestStatus.pending || settingsSaving" @click="testEmail"><RefreshCw v-if="mailTestStatus.pending" class="spin" :size="15" /><Mail v-else :size="15" />{{ mailTestStatus.pending ? '正在连接…' : '发送测试邮件' }}</button><span v-if="mailTestStatus.message" :class="['mail-test-result', `is-${mailTestStatus.tone}`]" role="status" aria-live="polite">{{ mailTestStatus.message }}</span></div></section><section class="admin-panel settings-panel"><div class="panel-heading"><div><span>ACCOUNT / SECURITY</span><h2>修改密码</h2></div></div><form class="account-form" @submit.prevent="changePassword"><label>当前密码<input v-model="passwordForm.currentPassword" type="password" autocomplete="current-password" /></label><label>新密码<input v-model="passwordForm.newPassword" type="password" minlength="10" autocomplete="new-password" /></label><label>确认新密码<input v-model="passwordForm.confirmPassword" type="password" minlength="10" autocomplete="new-password" /></label><button class="button">更新并重新登录</button></form></section></div></template>

        <template v-else><header class="admin-page-header"><div><span>SYSTEM / AUDIT</span><h1>日志、任务与备份</h1><p>重要变更写入审计日志，备份文件包含 SHA-256 校验。</p></div><button class="button button-primary" @click="runBackup"><Archive :size="15" />立即备份</button></header><div v-if="saved" class="admin-success">{{ saved }}</div><div class="log-status-grid"><div><ShieldCheck :size="20" /><span>安全响应头</span><strong>CSP 已启用</strong><small>写接口含频率限制</small></div><div><Archive :size="20" /><span>最近备份</span><strong>{{ backupJobs[0]?.createdAt?.slice(0, 16).replace('T', ' ') || '暂无备份' }}</strong><small>{{ backupJobs[0]?.size || 0 }} bytes</small></div><div><Activity :size="20" /><span>错误任务</span><strong>{{ dashboardData.errors || 0 }}</strong><small>数据库审计口径</small></div></div><div class="operations-grid"><section class="admin-panel"><div class="panel-heading"><div><span>AUDIT LOG</span><h2>最近操作</h2></div></div><div class="operation-list"><div v-for="item in logs" :key="item.id"><span>{{ item.action }}</span><strong>{{ item.target }}</strong><small>{{ item.actor }} · {{ item.createdAt?.slice(0, 19).replace('T', ' ') }} · {{ item.result }}</small></div></div></section><section class="admin-panel"><div class="panel-heading"><div><span>BACKUP TASKS</span><h2>备份记录</h2></div></div><div class="operation-list"><div v-for="item in backupJobs" :key="item.id"><span>{{ item.status }}</span><strong>{{ item.filename }}</strong><small>{{ item.size }} bytes · {{ item.checksum?.slice(0, 16) }}…</small></div></div></section></div></template>
        <AdminAdvancedCopy v-if="section === 'settings' && !adminLoading && !adminLoadError" :settings="settings" />
      </main>
    </div>
  </div>
</template>
