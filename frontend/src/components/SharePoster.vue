<script setup>
import QRCode from 'qrcode';
import { Check, Clipboard, Download, Image as ImageIcon, Share2, X } from 'lucide-vue-next';
import { computed, nextTick, ref, watch } from 'vue';
import { useSite } from '../site';
import { resolveShareUrl } from '../shareUrl';

const props = defineProps({ post: { type: Object, required: true } });
const { site } = useSite();
const open = ref(false); const variant = ref(site.shareTemplate === 'portrait' ? 'portrait' : 'landscape'); const qr = ref(''); const posterUrl = ref(''); const status = ref(''); const statusTone = ref('success');
let generation = 0;
const url = computed(() => resolveShareUrl(props.post, window.location.origin));

function wrap(ctx, text, maxWidth, maxLines) { const lines=[]; let line=''; for (const char of [...text]) { if (ctx.measureText(line+char).width>maxWidth && line) { lines.push(line); line=char; if (lines.length===maxLines-1) break; } else line+=char; } if (line) lines.push(line); if (lines.length===maxLines && lines.join('').length<text.length) lines[maxLines-1]=`${lines[maxLines-1].slice(0,-1)}…`; return lines; }
async function generate() {
  const currentGeneration = ++generation;
  posterUrl.value = '';
  qr.value = await QRCode.toDataURL(url.value, { width: 420, margin: 1, color: { dark: '#101513', light: '#ffffff' } });
  const portrait = variant.value === 'portrait'; const canvas=document.createElement('canvas'); canvas.width=portrait?1080:1200; canvas.height=portrait?1440:630; const ctx=canvas.getContext('2d');
  const bg=ctx.createLinearGradient(0,0,canvas.width,canvas.height); bg.addColorStop(0,'#101513'); bg.addColorStop(1,'#203226'); ctx.fillStyle=bg; ctx.fillRect(0,0,canvas.width,canvas.height);
  const cover=new Image(); cover.src=props.post.cover; await new Promise((resolve)=>{ cover.onload=resolve; cover.onerror=resolve; }); if(cover.naturalWidth){ctx.globalAlpha=.42;ctx.drawImage(cover,portrait?0:canvas.width*.55,0,portrait?canvas.width:canvas.width*.45,portrait?540:canvas.height);ctx.globalAlpha=1;}
  const pad=portrait?76:66; ctx.fillStyle='#b9ff66'; ctx.fillRect(pad,pad,44,4); ctx.fillStyle='rgba(255,255,255,.7)';ctx.font='500 20px sans-serif';ctx.fillText(`${site.siteShortName} / ${site.siteTagline}`,pad,pad+42);
  ctx.fillStyle='#fff';ctx.font=`700 ${portrait?60:49}px serif`;const title=wrap(ctx,props.post.title,portrait?canvas.width-pad*2:canvas.width*.48,portrait?4:3);const ty=portrait?310:180;title.forEach((line,i)=>ctx.fillText(line,pad,ty+i*(portrait?76:64)));
  ctx.fillStyle='rgba(255,255,255,.72)';ctx.font=`400 ${portrait?25:20}px sans-serif`;const excerpt=wrap(ctx,props.post.excerpt,portrait?canvas.width-pad*2:canvas.width*.48,portrait?4:3);const ey=ty+title.length*(portrait?76:64)+42;excerpt.forEach((line,i)=>ctx.fillText(line,pad,ey+i*(portrait?41:33)));
  const qrImage=new Image();qrImage.src=qr.value;await new Promise((resolve)=>{qrImage.onload=resolve;qrImage.onerror=resolve;});const size=portrait?220:150;const qx=portrait?pad:canvas.width-size-pad;const qy=canvas.height-size-(portrait?140:pad);ctx.fillStyle='#fff';ctx.fillRect(qx-12,qy-12,size+24,size+24);ctx.drawImage(qrImage,qx,qy,size,size);ctx.fillStyle='rgba(255,255,255,.7)';ctx.font='400 15px sans-serif';ctx.fillText(site.shareScanLabel,qx,qy+size+34);ctx.fillText(`${props.post.category} · ${props.post.readMinutes} MIN`,pad,canvas.height-pad);
  if (currentGeneration === generation && open.value) posterUrl.value=canvas.toDataURL('image/png');
}
watch(() => site.shareTemplate, (value) => { variant.value = value === 'portrait' ? 'portrait' : 'landscape'; });
watch([open, variant, () => site.siteName, () => site.siteShortName, () => site.siteTagline, () => site.sharePosterTitle, () => site.shareScanLabel, () => site.shareQrDescription, () => props.post.slug, () => props.post.canonicalUrl, () => props.post.title, () => props.post.excerpt, () => props.post.cover], async()=>{if(open.value){await nextTick();generate();}});
async function copy(){try { await navigator.clipboard?.writeText(`${props.post.title}\n${props.post.excerpt}\n${url.value}`); statusTone.value='success'; status.value=site.shareCopiedLabel; } catch { statusTone.value='error'; status.value=site.articleCopyFailureLabel || site.shareCopiedLabel; } setTimeout(()=>status.value='',1800);}
async function systemShare(){
  if (!navigator.share) return copy();
  try { await navigator.share({title: props.post.title, text: props.post.excerpt, url: url.value}); }
  catch { await copy(); }
}
function download(){const a=document.createElement('a');a.href=posterUrl.value;a.download=`${site.siteName}-${props.post.title.slice(0,28)}.png`;a.click();statusTone.value='success';status.value=site.shareSavedLabel;}
</script>

<template>
  <button class="article-share-trigger" type="button" @click="open=true"><Share2 :size="17" />{{ site.shareArticleLabel }}</button>
  <div v-if="open" class="modal-backdrop" @mousedown.self="open=false"><div class="modal" role="dialog" aria-modal="true"><header><div><span>SHARE / SIGNAL</span><h2>{{ site.sharePosterTitle }}</h2></div><button class="icon-button" type="button" title="关闭分享窗口" aria-label="关闭分享窗口" @click="open=false"><X :size="18" /></button></header><div class="share-modal-body"><div class="poster-preview"><div class="poster-preview-brand">{{ site.siteShortName }}</div><img v-if="posterUrl" :src="posterUrl" alt="文章分享海报预览" /><div v-else class="poster-loading"><ImageIcon :size="20" />{{ site.sharePosterLoadingLabel }}</div></div><div class="poster-controls"><div class="poster-tabs"><button :class="{active:variant==='landscape'}" @click="variant='landscape'">{{ site.shareLandscapeLabel }}</button><button :class="{active:variant==='portrait'}" @click="variant='portrait'">{{ site.sharePortraitLabel }}</button></div><div class="share-actions"><button @click="copy"><Clipboard :size="16" />{{ site.shareCopyLinkLabel }}</button><button :disabled="!posterUrl" @click="download"><Download :size="16" />{{ site.shareDownloadLabel }}</button><button :disabled="!posterUrl" @click="systemShare"><Share2 :size="16" />{{ site.shareSystemLabel }}</button></div><div v-if="qr" class="qr-row"><img :src="qr" alt="文章二维码" /><span>{{ site.shareScanLabel }}<br /><small>{{ site.shareQrDescription }}</small></span></div><div v-if="status" :class="['inline-success', {'is-error': statusTone === 'error'}]"><X v-if="statusTone === 'error'" :size="15" /><Check v-else :size="15" />{{ status }}</div></div></div></div></div>
</template>
