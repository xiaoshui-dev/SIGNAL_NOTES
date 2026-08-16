import test from 'node:test';
import assert from 'node:assert/strict';
import { postsForTaxonomy, resolveTaxonomy, taxonomyPath } from '../src/taxonomyRoutes.js';

const categories = [{ slug: 'system-design', name: '系统设计' }];
const tags = [{ slug: 'machine-learning', name: '机器学习' }];
const posts = [
  { id: 1, category: '系统设计', tags: ['机器学习'] },
  { id: 2, category: '其他', tags: [] },
];

test('public taxonomy links use the stable slug', () => {
  assert.equal(taxonomyPath('categories', categories[0]), '/blog/categories/system-design');
  assert.equal(taxonomyPath('tags', tags[0]), '/blog/tags/machine-learning');
});

test('category route resolves the slug before filtering by display name', () => {
  const category = resolveTaxonomy(categories, 'system-design');
  assert.equal(category.name, '系统设计');
  assert.deepEqual(postsForTaxonomy(posts, category, 'category').map((post) => post.id), [1]);
});

test('tag route resolves encoded slugs and filters by tag name', () => {
  const tag = resolveTaxonomy(tags, encodeURIComponent('machine-learning'));
  assert.equal(tag.name, '机器学习');
  assert.deepEqual(postsForTaxonomy(posts, tag, 'tag').map((post) => post.id), [1]);
});

test('unknown slugs do not fall back to display-name URLs', () => {
  assert.equal(resolveTaxonomy(categories, '系统设计'), null);
  assert.deepEqual(postsForTaxonomy(posts, null, 'category'), []);
});
