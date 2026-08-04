function renderCustomRouteModule() {
    const root = document.getElementById('admin-app-root');
    root.innerHTML = `
        <div class="admin-table-container">
            <div class="table-header">
                <div>
                    <div class="folder-title-text">Quản lý đường dẫn tùy chỉnh</div>
                    <div class="text-muted small" style="margin-top: 4px;">Định tuyến các liên kết không phải sản phẩm như blog, chính sách hoặc các trang riêng.</div>
                </div>
                <div class="header-actions">
                    <button class="btn-blog btn-create" onclick="openCustomRouteModal()">+ Thêm đường dẫn</button>
                </div>
            </div>
            <div style="overflow: auto;">
                <table>
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Tên</th>
                            <th>Slug</th>
                            <th>Đích</th>
                            <th>Hiển thị</th>
                            <th>Thao tác</th>
                        </tr>
                    </thead>
                    <tbody id="custom-route-table-body"></tbody>
                </table>
            </div>
        </div>
    `;

    document.getElementById('header-toolbox').innerHTML = '';
    loadCustomRoutes();
}

async function loadCustomRoutes() {
    try {
        const response = await fetch(joinUrl(API_BASE_URL, '/api/admin/custom-routes'));
        if (!response.ok) throw new Error('Failed to load custom routes');
        const routes = await response.json();
        const tbody = document.getElementById('custom-route-table-body');
        if (!tbody) return;

        tbody.innerHTML = routes.map(route => `
            <tr>
                <td>${route.id}</td>
                <td>${escapeHtml(route.name || '—')}</td>
                <td>${escapeHtml(route.slug || '—')}</td>
                <td>${escapeHtml(route.targetPath || '—')}</td>
                <td>${route.visible ? 'Có' : 'Ẩn'}</td>
                <td>
                    <button class="btn-secondary" onclick="openCustomRouteModal(${route.id})">Sửa</button>
                    <button class="btn-delete-folder" onclick="deleteCustomRoute(${route.id})">Xóa</button>
                </td>
            </tr>
        `).join('');
    } catch (error) {
        console.error(error);
        document.getElementById('custom-route-table-body').innerHTML = '<tr><td colspan="6">Không thể tải đường dẫn</td></tr>';
    }
}

async function openCustomRouteModal(routeId = null) {
    const route = routeId ? await fetchCustomRouteById(routeId) : null;

    AdminApp.showModal({
        id: 'custom-route-modal',
        title: route ? 'Cập nhật đường dẫn' : 'Thêm đường dẫn',
        bodyHTML: `
            <div class="form-grid" style="display: grid; gap: 14px;">
                <label style="display: flex; flex-direction: column; gap: 6px;">
                    <span>Tên</span>
                    <input id="custom-route-name" class="form-control" value="${escapeHtml(route?.name || '')}" />
                </label>
                <label style="display: flex; flex-direction: column; gap: 6px;">
                    <span>Slug</span>
                    <input id="custom-route-slug" class="form-control" value="${escapeHtml(route?.slug || '')}" />
                </label>
                <label style="display: flex; flex-direction: column; gap: 6px;">
                    <span>Đích</span>
                    <input id="custom-route-target" class="form-control" value="${escapeHtml(route?.targetPath || '')}" />
                </label>
                <label style="display: flex; align-items: center; gap: 8px;">
                    <input id="custom-route-visible" type="checkbox" ${route?.visible === false ? '' : 'checked'} />
                    <span>Hiển thị</span>
                </label>
            </div>
        `,
        confirmText: 'Lưu',
        onConfirm: async () => {
            const payload = {
                name: document.getElementById('custom-route-name').value.trim(),
                slug: document.getElementById('custom-route-slug').value.trim(),
                targetPath: document.getElementById('custom-route-target').value.trim(),
                visible: document.getElementById('custom-route-visible').checked,
                routeType: 'CUSTOM'
            };

            if (!payload.name || !payload.targetPath) {
                alert('Vui lòng nhập tên và đích đường dẫn');
                return false;
            }

            const response = routeId
                ? await fetch(joinUrl(API_BASE_URL, `/api/admin/custom-routes/${routeId}`), {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                })
                : await fetch(joinUrl(API_BASE_URL, '/api/admin/custom-routes'), {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });

            if (!response.ok) {
                alert('Không thể lưu đường dẫn');
                return false;
            }

            loadCustomRoutes();
            return true;
        }
    });

    initSlugAutoGenerate('custom-route-name', 'custom-route-slug');
}

async function fetchCustomRouteById(routeId) {
    const response = await fetch(joinUrl(API_BASE_URL, `/api/admin/custom-routes/${routeId}`));
    return response.ok ? response.json() : null;
}

async function deleteCustomRoute(id) {
    if (!confirm('Xóa đường dẫn này?')) return;
    const response = await fetch(joinUrl(API_BASE_URL, `/api/admin/custom-routes/${id}`), { method: 'DELETE' });
    if (response.ok) {
        loadCustomRoutes();
    }
}

registerModule('custom-routes', renderCustomRouteModule);
