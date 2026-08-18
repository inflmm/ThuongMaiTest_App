function renderCategoryModule() {
    const root = document.getElementById('admin-app-root');
    root.innerHTML = `
        <div class="admin-table-container">
            <div class="table-header">
                <div>
                    <div class="folder-title-text">Quản lý danh mục</div>
                    <div class="text-muted small" style="margin-top: 4px;">Cấu trúc danh mục chỉ giữ cấp 1 và cấp 2 làm cha để tránh tầng không hợp lệ.</div>
                </div>
                <div class="header-actions">
                    <button class="btn-secondary" onclick="recalculateCategoryCounts()">Tính lại số lượng</button>
                    <button class="btn-blog btn-create" onclick="openCreateCategoryModal()">+ Thêm danh mục</button>
                </div>
            </div>
            <div class="category-module-panel">
                <div id="category-status-banner" class="category-status-banner" style="display:none;"></div>
                <div class="category-module-summary">
                    <div class="category-module-pill">
                        <span class="text-muted">Số danh mục</span>
                        <strong id="category-summary-count">Đang tải...</strong>
                    </div>
                    <div class="category-module-pill">
                        <span class="text-muted">Tổng sản phẩm</span>
                        <strong id="category-summary-products">Đang tải...</strong>
                    </div>
                    <div class="category-module-pill">
                        <span class="text-muted">Cha được phép</span>
                        <strong>cấp 1 & cấp 2</strong>
                    </div>
                </div>
                <div id="category-tree-view" class="folder-tree"></div>
            </div>
        </div>
    `;

    document.getElementById('header-toolbox').innerHTML = '';
    loadCategories();
}

async function loadCategories() {
    try {
        const response = await fetch(joinUrl(API_BASE_URL, '/api/admin/categories/tree'));
        if (!response.ok) {
            await handleApiError(response);
            document.getElementById('category-tree-view').innerHTML = '<div>Không thể tải danh mục</div>';
            return;
        }
        const categories = await response.json();
        const treeHost = document.getElementById('category-tree-view');
        if (!treeHost) return;

        treeHost.innerHTML = renderCategoryTree(categories);
        const flattened = flattenCategories(categories);
        const countHost = document.getElementById('category-summary-count');
        const productCountHost = document.getElementById('category-summary-products');
        if (countHost) {
            const count = flattened.length;
            countHost.textContent = `${count} danh mục`;
        }
        if (productCountHost) {
            const totalProducts = flattened.reduce((sum, category) => sum + ((category.productCount ?? 0)), 0);
            productCountHost.textContent = `${totalProducts} sản phẩm`;
        }
    } catch (error) {
        console.error(error);
        showCategoryMessage('Không thể tải danh mục', 'error');
        document.getElementById('category-tree-view').innerHTML = '<div>Không thể tải danh mục</div>';
    }
}

async function recalculateCategoryCounts() {
    try {
        const response = await fetch(joinUrl(API_BASE_URL, '/api/admin/categories/recalculate-counts'), {
            method: 'POST',
            headers: csrfHeaders()
        });
        if (!response.ok) {
            await handleApiError(response);
            return;
        }

        const payload = await response.json().catch(() => ({}));
        showCategoryMessage(payload.message || 'Đã tính lại số lượng danh mục', 'success');
        loadCategories();
    } catch (error) {
        console.error(error);
        showCategoryMessage('Không thể tính lại số lượng danh mục', 'error');
    }
}

function showCategoryMessage(message, type = 'success') {
    const banner = document.getElementById('category-status-banner');
    if (!banner) return;

    banner.textContent = message;
    banner.className = `category-status-banner ${type === 'error' ? 'is-error' : 'is-success'}`;
    banner.style.display = 'flex';

    clearTimeout(showCategoryMessage.timer);
    showCategoryMessage.timer = setTimeout(() => {
        banner.style.display = 'none';
        banner.textContent = '';
    }, 3500);
}

function flattenCategories(items) {
    const result = [];
    const walk = (nodes) => {
        nodes.forEach(node => {
            result.push(node);
            if (Array.isArray(node.children) && node.children.length) {
                walk(node.children);
            }
        });
    };
    walk(items || []);
    return result;
}

function renderCategoryTree(categories) {
    return `
        <ul class="folder-tree-list">
            ${categories.map(category => renderCategoryNode(category)).join('')}
        </ul>
    `;
}

function renderCategoryNode(category) {
    const children = Array.isArray(category.children) ? category.children : [];
    const childMarkup = children.length ? `<ul class="folder-tree-children">${children.map(renderCategoryNode).join('')}</ul>` : '';
    const level = category.level || 1;
    const levelLabel = level === 1 ? 'Cấp 1' : level === 2 ? 'Cấp 2' : 'Cấp 3';
    const rootCardClass = level === 1 ? 'category-root-card' : '';

    return `
        <li class="folder-tree-item">
            <div class="folder-tree-row ${rootCardClass}">
                <div style="display: flex; flex-direction: column; gap: 4px;">
                    <div style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap;">
                        <span class="folder-tree-name">${escapeHtml(category.name)}</span>
                        <span class="category-badge" style="display: inline-flex; align-items: center; padding: 2px 8px; border-radius: 999px; background: #eef2ff; color: #4338ca; font-size: 0.75rem; font-weight: 600;">${levelLabel}</span>
                    </div>
                    <div class="text-muted small">Slug: ${escapeHtml(category.slug || '—')}</div>
                    <div class="text-muted small">Số lượng sản phẩm hiển thị: ${escapeHtml(String(category.productCount ?? 0))} • Tổng số lượng sản phẩm: ${escapeHtml(String(category.adminProductCount ?? 0))}</div>
                </div>
                <div class="folder-tree-actions">
                    <button class="btn-secondary" onclick="openCreateCategoryModal(${category.id})">Sửa</button>
                    <button class="btn-delete-folder" onclick="deleteCategory(${category.id})">Xóa</button>
                </div>
            </div>
            ${childMarkup}
        </li>
    `;
}

async function openCreateCategoryModal(categoryId = null) {
    const category = categoryId ? await fetchCategoryById(categoryId) : null;

    AdminApp.showModal({
        id: 'category-modal',
        title: categoryId ? 'Cập nhật danh mục' : 'Thêm danh mục',
        bodyHTML: `
            <div class="form-grid" style="display: grid; gap: 14px;">
                <div style="display: grid; gap: 14px; grid-template-columns: repeat(2, minmax(220px, 1fr));">
                    <label style="display: flex; flex-direction: column; gap: 6px;">
                        <span>Tên danh mục</span>
                        <input id="category-name" type="text" class="form-control" value="${escapeHtml(category?.name || '')}" placeholder="Ví dụ: Điện thoại" />
                    </label>
                    <label style="display: flex; flex-direction: column; gap: 6px;">
                        <span>Slug dự kiến</span>
                        <input id="category-slug" type="text" class="form-control" readonly value="${escapeHtml(category?.slug || '')}" placeholder="slug-sep..." />
                    </label>
                </div>
                <label style="display: flex; flex-direction: column; gap: 6px;">
                    <span>Danh mục cha</span>
                    <select id="category-parent" class="form-control"></select>
                    <small class="text-muted">Chỉ hiển thị danh mục cấp 1 và cấp 2 làm cha.</small>
                </label>
                <label style="display: flex; flex-direction: column; gap: 6px;">
                    <span>Icon URL</span>
                    <input id="category-icon-url" type="text" class="form-control" value="${escapeHtml(category?.iconUrl || '')}" placeholder="https://..." />
                </label>
            </div>
        `,
        confirmText: 'Lưu',
        onConfirm: async () => {
            const name = document.getElementById('category-name').value.trim();
            if (!name) {
                showCategoryMessage('Vui lòng nhập tên danh mục', 'error');
                return false;
            }

            const payload = {
                name,
                parentId: document.getElementById('category-parent').value || null,
                displayOrder: null,
                iconUrl: document.getElementById('category-icon-url').value.trim() || null
            };

            const response = categoryId
                ? await fetch(joinUrl(API_BASE_URL, `/api/admin/categories/${categoryId}`), {
                    method: 'PUT',
                    headers: csrfHeaders({ 'Content-Type': 'application/json' }),
                    body: JSON.stringify(payload)
                })
                : await fetch(joinUrl(API_BASE_URL, '/api/admin/categories'), {
                    method: 'POST',
                    headers: csrfHeaders({ 'Content-Type': 'application/json' }),
                    body: JSON.stringify(payload)
                });

            if (!response.ok) {
                await handleApiError(response);
                return false;
            }

            showCategoryMessage(categoryId ? 'Đã lưu danh mục' : 'Đã thêm danh mục', 'success');
            loadCategories();
            return true;
        }
    });

    populateParentCategoryOptions(categoryId, category?.parentId ?? null);
    initSlugAutoGenerate('category-name', 'category-slug');
}

async function fetchCategoryById(categoryId) {
    const response = await fetch(joinUrl(API_BASE_URL, `/api/admin/categories/tree`));
    if (!response.ok) {
        await handleApiError(response);
        return null;
    }

    const categories = await response.json();
    const flatten = [];
    const walk = (nodes) => nodes.forEach(node => {
        flatten.push(node);
        walk(node.children || []);
    });
    walk(categories || []);

    return flatten.find(item => Number(item.id) === Number(categoryId)) || null;
}

async function populateParentCategoryOptions(categoryId = null, selectedParentId = null) {
    const select = document.getElementById('category-parent');
    if (!select) return;

    const response = await fetch(joinUrl(API_BASE_URL, '/api/admin/categories/tree'));
    if (!response.ok) {
        await handleApiError(response);
        return;
    }
    const categories = await response.json();
    const flatten = [];
    const walk = (items, parentChain = []) => items.forEach(item => {
        const itemId = Number(item.id);
        const currentCategoryId = categoryId != null ? Number(categoryId) : null;
        const isSelf = currentCategoryId != null && itemId === currentCategoryId;
        const isDescendant = currentCategoryId != null && parentChain.includes(currentCategoryId);
        if ((item.level || 1) < 3 && !isSelf && !isDescendant) {
            flatten.push({ ...item, depth: parentChain.length });
        }
        walk(item.children || [], [...parentChain, itemId]);
    });
    walk(categories || []);

    select.innerHTML = '<option value="">— Không có danh mục cha —</option>' + flatten.map(category => `<option value="${category.id}" ${Number(selectedParentId) === Number(category.id) ? 'selected' : ''}>${'— '.repeat(Math.max(0, (category.depth || 0)))}${escapeHtml(category.name)}</option>`).join('');
}

async function deleteCategory(categoryId) {
    if (!confirm('Xóa danh mục này?')) return;
    const response = await fetch(joinUrl(API_BASE_URL, `/api/admin/categories/${categoryId}`), {
        method: 'DELETE',
        headers: csrfHeaders()
    });
    if (response.ok) {
        showCategoryMessage('Đã xóa danh mục', 'success');
        loadCategories();
        return;
    }

    await handleApiError(response);
}

function escapeHtml(value) {
    return String(value || '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

registerModule('categories', renderCategoryModule);
