function renderCollectionModule() {
    const root = document.getElementById('admin-app-root');
    root.innerHTML = `
        <div class="admin-table-container">
            <div class="table-header">
                <div>
                    <div class="folder-title-text">Quản lý bộ sưu tập</div>
                    <div class="text-muted small" style="margin-top: 4px;">Tạo và quản lý các bộ sưu tập hiển thị trên storefront.</div>
                </div>
                <div class="header-actions">
                    <button class="btn-blog btn-create" onclick="openCollectionModal()">+ Thêm bộ sưu tập</button>
                </div>
            </div>
            <div style="overflow: auto;">
                <table>
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Tên</th>
                            <th>Slug</th>
                            <th>Mô tả</th>
                            <th>Thao tác</th>
                        </tr>
                    </thead>
                    <tbody id="collection-table-body"></tbody>
                </table>
            </div>
        </div>
    `;

    document.getElementById('header-toolbox').innerHTML = '';
    loadCollections();
}

async function loadCollections() {
    try {
        const response = await fetch(joinUrl(API_BASE_URL, '/api/admin/collections'));
        if (!response.ok) throw new Error('Failed to load collections');
        const collections = await response.json();
        const tbody = document.getElementById('collection-table-body');
        if (!tbody) return;

        tbody.innerHTML = collections.map(collection => `
            <tr>
                <td>${collection.id}</td>
                <td>${escapeHtml(collection.name || '—')}</td>
                <td>${escapeHtml(collection.slug || '—')}</td>
                <td>${escapeHtml(collection.description || '—')}</td>
                <td>
                    <button class="btn-secondary" onclick="openCollectionModal(${collection.id})">Sửa</button>
                    <button class="btn-delete-folder" onclick="deleteCollection(${collection.id})">Xóa</button>
                </td>
            </tr>
        `).join('');
    } catch (error) {
        console.error(error);
        document.getElementById('collection-table-body').innerHTML = '<tr><td colspan="5">Không thể tải bộ sưu tập</td></tr>';
    }
}

async function openCollectionModal(collectionId = null) {
    const collection = collectionId ? await fetchCollectionById(collectionId) : null;

    AdminApp.showModal({
        id: 'collection-modal',
        title: collection ? 'Cập nhật bộ sưu tập' : 'Thêm bộ sưu tập',
        bodyHTML: `
            <div class="form-grid" style="display: grid; gap: 14px;">
                <label style="display: flex; flex-direction: column; gap: 6px;">
                    <span>Tên bộ sưu tập</span>
                    <input id="collection-name" class="form-control" value="${escapeHtml(collection?.name || '')}" />
                </label>
                <label style="display: flex; flex-direction: column; gap: 6px;">
                    <span>Slug</span>
                    <input id="collection-slug" class="form-control" value="${escapeHtml(collection?.slug || '')}" />
                </label>
                <label style="display: flex; flex-direction: column; gap: 6px;">
                    <span>Mô tả</span>
                    <textarea id="collection-description" class="form-control" rows="4">${escapeHtml(collection?.description || '')}</textarea>
                </label>
            </div>
        `,
        confirmText: 'Lưu',
        onConfirm: async () => {
            const payload = {
                name: document.getElementById('collection-name').value.trim(),
                slug: document.getElementById('collection-slug').value.trim(),
                description: document.getElementById('collection-description').value.trim()
            };

            if (!payload.name) {
                alert('Vui lòng nhập tên bộ sưu tập');
                return false;
            }

            const response = collectionId
                ? await fetch(joinUrl(API_BASE_URL, `/api/admin/collections/${collectionId}`), {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                })
                : await fetch(joinUrl(API_BASE_URL, '/api/admin/collections'), {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });

            if (!response.ok) {
                alert('Không thể lưu bộ sưu tập');
                return false;
            }

            loadCollections();
            return true;
        }
    });

    initSlugAutoGenerate('collection-name', 'collection-slug');
}

async function fetchCollectionById(collectionId) {
    const response = await fetch(joinUrl(API_BASE_URL, `/api/admin/collections/${collectionId}`));
    return response.ok ? response.json() : null;
}

async function deleteCollection(id) {
    if (!confirm('Xóa bộ sưu tập này?')) return;
    const response = await fetch(joinUrl(API_BASE_URL, `/api/admin/collections/${id}`), { method: 'DELETE' });
    if (response.ok) {
        loadCollections();
    }
}

registerModule('collections', renderCollectionModule);
