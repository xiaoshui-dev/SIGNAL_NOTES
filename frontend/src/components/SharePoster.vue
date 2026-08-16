<script setup>
import QRCode from 'qrcode';
import { Check, Clipboard, Download, Image as ImageIcon, Share2, X } from 'lucide-vue-next';
import { computed, nextTick, ref, watch } from 'vue';
import { useSite } from '../site';
import { resolveShareUrl } from '../shareUrl';
import { resolveAuthorName, resolveAvatarUrl } from '../authorIdentity';

const props = defineProps({ post: { type: Object, required: true } });
const { site } = useSite();
const open = ref(false);
const variant = ref(site.shareTemplate === 'portrait' ? 'portrait' : 'landscape');
const qr = ref('');
const posterUrl = ref('');
const posterBusy = ref(false);
const status = ref('');
const statusTone = ref('success');
let generation = 0;
const url = computed(() => resolveShareUrl(props.post, window.location.origin));
const authorName = computed(() => resolveAuthorName(props.post.authorName, site.authorName));
const authorAvatarUrl = computed(() => resolveAvatarUrl({ uploadedAvatarUrl: props.post.authorAvatarUrl, name: authorName.value }));

function wrap(ctx, text, maxWidth, maxLines) {
  const source = String(text || '');
  const lines = [];
  let line = '';
  let consumed = 0;
  for (const char of [...source]) {
    if (ctx.measureText(line + char).width > maxWidth && line) {
      lines.push(line);
      consumed += line.length;
      line = char;
      if (lines.length === maxLines - 1) break;
    } else line += char;
  }
  if (line) lines.push(line);
  if (lines.length === maxLines && consumed + lines.at(-1).length < source.length) lines[maxLines - 1] = `${lines[maxLines - 1].slice(0, -1)}…`;
  return lines;
}

function loadImage(src) {
  return new Promise((resolve) => {
    const image = new Image();
    image.crossOrigin = 'anonymous';
    image.onload = () => resolve(image);
    image.onerror = () => resolve(null);
    image.src = src;
  });
}

function drawCover(ctx, image, x, y, width, height) {
  if (!image?.naturalWidth) return;
  const scale = Math.max(width / image.naturalWidth, height / image.naturalHeight);
  const sw = width / scale;
  const sh = height / scale;
  const sx = Math.max(0, (image.naturalWidth - sw) / 2);
  const sy = Math.max(0, (image.naturalHeight - sh) / 2);
  ctx.drawImage(image, sx, sy, sw, sh, x, y, width, height);
}

async function generate() {
  const currentGeneration = ++generation;
  posterBusy.value = true;
  posterUrl.value = '';
  status.value = '';
  try {
    await document.fonts?.ready;
    qr.value = await QRCode.toDataURL(url.value, { width: 420, margin: 4, errorCorrectionLevel: 'M', color: { dark: '#090d0b', light: '#f9fbf7' } });
    const portrait = variant.value === 'portrait';
    const canvas = document.createElement('canvas');
    canvas.width = portrait ? 1080 : 1200;
    canvas.height = portrait ? 1440 : 630;
    const ctx = canvas.getContext('2d');
    ctx.fillStyle = '#090d0b';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    const [cover, qrImage, avatarImage] = await Promise.all([loadImage(props.post.cover), loadImage(qr.value), loadImage(authorAvatarUrl.value)]);
    if (portrait) {
      drawCover(ctx, cover, 0, 0, 1080, 410);
      ctx.fillStyle = 'rgba(9,13,11,.48)';
      ctx.fillRect(0, 0, 1080, 410);
      ctx.fillStyle = '#b9ff66';
      ctx.fillRect(0, 402, 1080, 8);
    } else {
      drawCover(ctx, cover, 704, 0, 496, 630);
      ctx.fillStyle = 'rgba(9,13,11,.22)';
      ctx.fillRect(704, 0, 496, 630);
      ctx.fillStyle = '#b9ff66';
      ctx.fillRect(696, 0, 8, 630);
    }

    const pad = portrait ? 72 : 62;
    const textWidth = portrait ? 936 : 570;
    const top = portrait ? 480 : 64;
    ctx.fillStyle = '#b9ff66';
    ctx.font = `400 ${portrait ? 24 : 19}px "Fusion Pixel 12px Proportional SC", monospace`;
    ctx.fillText(`${site.siteShortName} / ${String(props.post.category || 'TECH').toUpperCase()}`, pad, top);

    ctx.fillStyle = '#f9fbf7';
    ctx.font = `700 ${portrait ? 64 : 48}px "Noto Serif SC", serif`;
    const title = wrap(ctx, props.post.title, textWidth, portrait ? 4 : 3);
    const titleY = top + (portrait ? 92 : 74);
    const titleLine = portrait ? 84 : 62;
    title.forEach((line, index) => ctx.fillText(line, pad, titleY + index * titleLine));

    ctx.fillStyle = 'rgba(249,251,247,.72)';
    ctx.font = `400 ${portrait ? 26 : 19}px "Noto Sans SC", sans-serif`;
    const excerpt = wrap(ctx, props.post.excerpt, textWidth, portrait ? 4 : 3);
    const excerptY = titleY + title.length * titleLine + (portrait ? 44 : 30);
    const excerptLine = portrait ? 43 : 31;
    excerpt.forEach((line, index) => ctx.fillText(line, pad, excerptY + index * excerptLine));

    const qrSize = portrait ? 224 : 150;
    const qrX = portrait ? canvas.width - pad - qrSize : canvas.width - 62 - qrSize;
    const qrY = portrait ? canvas.height - 72 - qrSize : canvas.height - 58 - qrSize;
    ctx.fillStyle = '#f9fbf7';
    ctx.fillRect(qrX - 12, qrY - 12, qrSize + 24, qrSize + 24);
    if (qrImage) ctx.drawImage(qrImage, qrX, qrY, qrSize, qrSize);

    const metaY = portrait ? canvas.height - 110 : canvas.height - 50;
    const avatarSize = portrait ? 52 : 40;
    if (avatarImage) {
      ctx.imageSmoothingEnabled = false;
      ctx.drawImage(avatarImage, pad, metaY - avatarSize + 8, avatarSize, avatarSize);
      ctx.imageSmoothingEnabled = true;
    }
    ctx.fillStyle = '#ff6d3a';
    ctx.font = `400 ${portrait ? 22 : 17}px "Fusion Pixel 12px Proportional SC", monospace`;
    ctx.fillText(`${authorName.value} / ${String(props.post.publishedAt || '').slice(0, 10)}`, pad + avatarSize + 14, metaY);
    ctx.fillStyle = 'rgba(249,251,247,.65)';
    ctx.font = `400 ${portrait ? 18 : 14}px "Noto Sans SC", sans-serif`;
    ctx.fillText(site.shareScanLabel, qrX, qrY - 26);

    if (currentGeneration === generation && open.value) posterUrl.value = canvas.toDataURL('image/png');
  } catch (error) {
    if (currentGeneration === generation) {
      statusTone.value = 'error';
      status.value = error?.message || '分享图片生成失败';
    }
  } finally {
    if (currentGeneration === generation) posterBusy.value = false;
  }
}

watch(() => site.shareTemplate, (value) => { variant.value = value === 'portrait' ? 'portrait' : 'landscape'; });
watch([
  open, variant,
  () => site.siteName, () => site.siteShortName, () => site.siteTagline,
  () => site.sharePosterTitle, () => site.shareScanLabel,
  () => props.post.slug, () => props.post.canonicalUrl, () => props.post.title,
  () => props.post.excerpt, () => props.post.cover, () => props.post.authorName, () => props.post.authorAvatarUrl,
  () => props.post.publishedAt,
], async () => { if (open.value) { await nextTick(); generate(); } });

async function copy() {
  try {
    await navigator.clipboard?.writeText(`${props.post.title}\n${props.post.excerpt}\n${url.value}`);
    statusTone.value = 'success';
    status.value = site.shareCopiedLabel;
  } catch {
    statusTone.value = 'error';
    status.value = site.articleCopyFailureLabel || site.shareCopiedLabel;
  }
  setTimeout(() => { status.value = ''; }, 1800);
}

async function systemShare() {
  if (!navigator.share) return copy();
  try { await navigator.share({ title: props.post.title, text: props.post.excerpt, url: url.value }); }
  catch { await copy(); }
}

function download() {
  if (!posterUrl.value) return;
  const anchor = document.createElement('a');
  anchor.href = posterUrl.value;
  anchor.download = `${site.siteName}-${props.post.title.slice(0, 28)}.png`;
  anchor.click();
  statusTone.value = 'success';
  status.value = site.shareSavedLabel;
}
</script>

<template>
  <button class="article-share-trigger" type="button" @click="open = true"><Share2 :size="17" />{{ site.shareArticleLabel }}</button>
  <div v-if="open" class="modal-backdrop" @mousedown.self="open = false">
    <div class="modal" role="dialog" aria-modal="true" aria-labelledby="share-poster-heading">
      <header><div><span>SHARE / SIGNAL</span><h2 id="share-poster-heading">{{ site.sharePosterTitle }}</h2></div><button class="icon-button" type="button" title="关闭分享窗口" aria-label="关闭分享窗口" @click="open = false"><X :size="18" /></button></header>
      <div class="share-modal-body">
        <div class="poster-preview" :aria-busy="posterBusy"><div class="poster-preview-brand">{{ site.siteShortName }}</div><img v-if="posterUrl" :src="posterUrl" alt="文章分享海报预览" /><div v-else class="poster-loading" role="status"><ImageIcon :size="20" />{{ site.sharePosterLoadingLabel }}</div></div>
        <div class="poster-controls">
          <div class="poster-tabs" aria-label="分享图片尺寸"><button type="button" :class="{ active: variant === 'landscape' }" @click="variant = 'landscape'">{{ site.shareLandscapeLabel }}</button><button type="button" :class="{ active: variant === 'portrait' }" @click="variant = 'portrait'">{{ site.sharePortraitLabel }}</button></div>
          <div class="share-actions"><button type="button" @click="copy"><Clipboard :size="16" />{{ site.shareCopyLinkLabel }}</button><button type="button" :disabled="posterBusy || !posterUrl" @click="download"><Download :size="16" />{{ site.shareDownloadLabel }}</button><button type="button" :disabled="posterBusy || !posterUrl" @click="systemShare"><Share2 :size="16" />{{ site.shareSystemLabel }}</button></div>
          <div v-if="qr" class="qr-row"><img :src="qr" alt="文章二维码" /><span>{{ site.shareScanLabel }}<br /><small>{{ site.shareQrDescription }}</small></span></div>
          <div v-if="status" :class="['inline-success', { 'is-error': statusTone === 'error' }]" role="status"><X v-if="statusTone === 'error'" :size="15" /><Check v-else :size="15" />{{ status }}</div>
        </div>
      </div>
    </div>
  </div>
</template>
