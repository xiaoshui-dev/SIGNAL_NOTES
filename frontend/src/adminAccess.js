const PATHS = {
  VIEWER: ['/admin'],
  AUTHOR: ['/admin', '/admin/posts', '/admin/media'],
  EDITOR: [
    '/admin', '/admin/posts', '/admin/taxonomy', '/admin/media', '/admin/comments',
    '/admin/inbox', '/admin/subscribers',
  ],
  ADMIN: [
    '/admin', '/admin/posts', '/admin/taxonomy', '/admin/media', '/admin/comments',
    '/admin/inbox', '/admin/subscribers', '/admin/users', '/admin/settings', '/admin/logs',
  ],
};

export function adminAccessForRole(role) {
  const normalized = Object.hasOwn(PATHS, role) ? role : 'VIEWER';
  return {
    paths: PATHS[normalized],
    canWritePosts: normalized !== 'VIEWER',
    canManageEditorial: normalized === 'EDITOR' || normalized === 'ADMIN',
    canReadCommunity: normalized === 'EDITOR' || normalized === 'ADMIN',
    canManageSystem: normalized === 'ADMIN',
  };
}

export function canAccessAdminRoute(role, path) {
  return adminAccessForRole(role).paths.some((allowed) => (
    allowed === '/admin' ? path === allowed : path === allowed || path.startsWith(`${allowed}/`)
  ));
}
