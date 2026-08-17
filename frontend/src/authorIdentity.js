import { createAvatar } from '@dicebear/core';
import * as pixelArt from '@dicebear/pixel-art';

export const DEFAULT_AUTHOR_NAME = '站点作者';

function normalized(value) {
  return typeof value === 'string' ? value.trim() : '';
}

export function resolveAuthorName(accountName, siteName) {
  return normalized(accountName) || normalized(siteName) || DEFAULT_AUTHOR_NAME;
}

export function createPostAuthor({ userName, siteAuthorName } = {}) {
  return resolveAuthorName(userName, siteAuthorName);
}

export function displayAdminIdentity({ name, loginName } = {}) {
  return resolveAuthorName(name, loginName);
}

export function authorInitials(name) {
  const value = resolveAuthorName(name);
  const words = value.match(/[A-Za-z0-9]+/g);
  if (words?.length > 1) return `${words[0][0]}${words[1][0]}`.toUpperCase();
  if (words?.length === 1) return words[0].slice(0, 2).toUpperCase();
  return Array.from(value).slice(0, 2).join('');
}

export function resolveAvatarUrl({ uploadedAvatarUrl, name } = {}) {
  const uploaded = normalized(uploadedAvatarUrl);
  if (uploaded.startsWith('/uploads/')) return uploaded;
  const seed = resolveAuthorName(name).toLocaleLowerCase('zh-CN');
  return createAvatar(pixelArt, {
    seed: [seed],
    size: 96,
    backgroundType: ['solid'],
    backgroundColor: ['b9ff66'],
  }).toDataUri();
}
