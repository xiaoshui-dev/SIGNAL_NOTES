import QRCode from 'qrcode';
import { Check, Clipboard, Download, Image as ImageIcon, Link2, QrCode, Share2, X } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';

function wrapText(ctx, text, maxWidth, maxLines) {
  const chars = [...text];
  const lines = [];
  let line = '';
  for (const char of chars) {
    const next = line + char;
    if (ctx.measureText(next).width > maxWidth && line) {
      lines.push(line);
      line = char;
      if (lines.length === maxLines - 1) break;
    } else {
      line = next;
    }
  }
  if (lines.length < maxLines && line) lines.push(line);
  if (lines.length === maxLines && [...text].length > [...lines.join('')].length) {
    lines[maxLines - 1] = `${lines[maxLines - 1].slice(0, -1)}…`;
  }
  return lines;
}

async function createPoster({ post, qrDataUrl, variant }) {
  const canvas = document.createElement('canvas');
  const isPortrait = variant === 'portrait';
  canvas.width = isPortrait ? 1080 : 1200;
  canvas.height = isPortrait ? 1440 : 630;
  const ctx = canvas.getContext('2d');
  const width = canvas.width;
  const height = canvas.height;
  const gradient = ctx.createLinearGradient(0, 0, width, height);
  gradient.addColorStop(0, '#101513');
  gradient.addColorStop(1, '#1e2b23');
  ctx.fillStyle = gradient;
  ctx.fillRect(0, 0, width, height);

  const cover = new Image();
  cover.src = post.cover;
  await new Promise((resolve) => {
    cover.onload = resolve;
    cover.onerror = resolve;
  });
  if (cover.complete && cover.naturalWidth) {
    const imageSize = isPortrait ? width : Math.round(width * .47);
    const x = isPortrait ? 0 : width - imageSize;
    const imageHeight = isPortrait ? Math.round(width * .5) : height;
    const scale = Math.max(imageSize / cover.naturalWidth, imageHeight / cover.naturalHeight);
    const drawWidth = cover.naturalWidth * scale;
    const drawHeight = cover.naturalHeight * scale;
    ctx.save();
    ctx.globalAlpha = .55;
    ctx.drawImage(cover, x + (imageSize - drawWidth) / 2, (imageHeight - drawHeight) / 2, drawWidth, drawHeight);
    ctx.restore();
    const overlay = ctx.createLinearGradient(x, 0, x + imageSize, 0);
    overlay.addColorStop(0, 'rgba(16,21,19,.75)');
    overlay.addColorStop(1, 'rgba(16,21,19,.08)');
    ctx.fillStyle = overlay;
    ctx.fillRect(x, 0, imageSize, imageHeight);
  }

  const pad = isPortrait ? 78 : 70;
  ctx.fillStyle = '#b9ff66';
  ctx.fillRect(pad, pad, 44, 4);
  ctx.fillStyle = 'rgba(255,255,255,.65)';
  ctx.font = '500 20px monospace';
  ctx.fillText('SIGNAL NOTES  /  TECH NOTES', pad, pad + 43);
  ctx.fillStyle = '#ffffff';
  ctx.font = `700 ${isPortrait ? 58 : 48}px serif`;
  const titleLines = wrapText(ctx, post.title, isPortrait ? width - pad * 2 : width * .48, isPortrait ? 4 : 3);
  const titleY = isPortrait ? 270 : 176;
  titleLines.forEach((line, index) => ctx.fillText(line, pad, titleY + index * (isPortrait ? 74 : 64)));
  ctx.fillStyle = 'rgba(255,255,255,.74)';
  ctx.font = `400 ${isPortrait ? 26 : 21}px sans-serif`;
  const excerptLines = wrapText(ctx, post.excerpt, isPortrait ? width - pad * 2 : width * .48, isPortrait ? 5 : 3);
  const excerptY = titleY + titleLines.length * (isPortrait ? 74 : 64) + 46;
  excerptLines.forEach((line, index) => ctx.fillText(line, pad, excerptY + index * (isPortrait ? 42 : 34)));

  const qrImage = new Image();
  qrImage.src = qrDataUrl;
  await new Promise((resolve) => { qrImage.onload = resolve; qrImage.onerror = resolve; });
  const qrSize = isPortrait ? 230 : 158;
  const qrX = isPortrait ? pad : width - qrSize - pad;
  const qrY = isPortrait ? height - qrSize - 150 : height - qrSize - pad;
  ctx.fillStyle = '#ffffff';
  ctx.fillRect(qrX - 15, qrY - 15, qrSize + 30, qrSize + 30);
  if (qrImage.complete) ctx.drawImage(qrImage, qrX, qrY, qrSize, qrSize);
  ctx.fillStyle = 'rgba(255,255,255,.72)';
  ctx.font = `400 ${isPortrait ? 19 : 15}px monospace`;
  ctx.fillText('SCAN TO READ', qrX, qrY + qrSize + 42);
  ctx.fillStyle = 'rgba(255,255,255,.55)';
  ctx.font = '400 16px monospace';
  ctx.fillText(`${post.category}  ·  ${post.readMinutes} MIN READ`, pad, height - pad);
  return canvas;
}

function Modal({ children, onClose, title }) {
  return <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}><div className="modal" role="dialog" aria-modal="true" aria-labelledby="share-title"><header><div><span>SHARE / SIGNAL</span><h2 id="share-title">{title}</h2></div><button className="icon-button" type="button" onClick={onClose} aria-label="关闭"><X size={18} /></button></header>{children}</div></div>;
}

export default function SharePoster({ post }) {
  const [open, setOpen] = useState(false);
  const [qr, setQr] = useState('');
  const [variant, setVariant] = useState('landscape');
  const [posterUrl, setPosterUrl] = useState('');
  const [status, setStatus] = useState('');
  const url = useMemo(() => `${window.location.origin}/blog/posts/${post.slug}`, [post.slug]);

  useEffect(() => {
    if (!open) return undefined;
    let active = true;
    QRCode.toDataURL(url, { width: 420, margin: 1, color: { dark: '#101513', light: '#ffffff' } }).then((value) => { if (active) setQr(value); });
    return () => { active = false; };
  }, [open, url]);

  useEffect(() => {
    if (!qr) return undefined;
    let active = true;
    createPoster({ post, qrDataUrl: qr, variant }).then((canvas) => { if (active) setPosterUrl(canvas.toDataURL('image/png')); });
    return () => { active = false; };
  }, [post, qr, variant]);

  async function copyLink() {
    await navigator.clipboard?.writeText(`${post.title}\n${post.excerpt}\n${url}`);
    setStatus('链接和摘要已复制');
    window.setTimeout(() => setStatus(''), 2400);
  }

  async function shareNative() {
    if (!posterUrl) return;
    try {
      const blob = await (await fetch(posterUrl)).blob();
      const file = new File([blob], `${post.title}.png`, { type: 'image/png' });
      if (navigator.share && (!navigator.canShare || navigator.canShare({ files: [file] }))) await navigator.share({ title: post.title, text: post.excerpt, url, files: [file] });
      else await copyLink();
    } catch (error) {
      if (error?.name !== 'AbortError') setStatus('当前设备不支持图片分享，请保存图片');
    }
  }

  function download() {
    if (!posterUrl) return;
    const link = document.createElement('a');
    link.href = posterUrl;
    link.download = `脉冲笔记-${post.title.replace(/[\\/:*?"<>|]/g, '').slice(0, 32)}.png`;
    link.click();
    setStatus('分享图片已保存');
    window.setTimeout(() => setStatus(''), 2400);
  }

  return <>
    <button className="article-share-trigger" type="button" onClick={() => setOpen(true)}><Share2 size={17} /> 分享文章</button>
    {open && <Modal title="生成分享图片" onClose={() => setOpen(false)}>
      <div className="share-modal-body">
        <div className="poster-preview"><div className="poster-preview-brand">SIGNAL NOTES</div>{posterUrl ? <img src={posterUrl} alt="文章分享海报预览" /> : <div className="poster-loading"><ImageIcon size={21} />正在生成图片</div>}</div>
        <div className="poster-controls">
          <div className="poster-tabs"><button type="button" className={variant === 'landscape' ? 'active' : ''} onClick={() => setVariant('landscape')}>横版 1200×630</button><button type="button" className={variant === 'portrait' ? 'active' : ''} onClick={() => setVariant('portrait')}>竖版 1080×1440</button></div>
          <div className="share-actions"><button type="button" onClick={copyLink}><Clipboard size={16} />复制链接</button><button type="button" onClick={download} disabled={!posterUrl}><Download size={16} />保存图片</button><button type="button" onClick={shareNative} disabled={!posterUrl}><Share2 size={16} />分享图片</button></div>
          {qr && <div className="qr-row"><img src={qr} alt="文章二维码" /><span><QrCode size={16} />扫码阅读全文<br /><small>二维码指向文章 canonical URL</small></span></div>}
          <p className="share-note"><Link2 size={14} /> 图片包含标题、摘要、分类和二维码，可直接转发到聊天或朋友圈。</p>
          {status && <div className="inline-success" role="status"><Check size={15} />{status}</div>}
        </div>
      </div>
    </Modal>}
  </>;
}
