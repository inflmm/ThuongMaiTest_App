function renderProductModule() {
    const root = document.getElementById('admin-app-root');
    root.innerHTML = `
        <div class="admin-table-container">
            <div class="table-header">
                <div>
                    <div class="folder-title-text">Quản lý sản phẩm</div>
                    <div class="text-muted small" style="margin-top: 4px;">Theo dõi sản phẩm, danh mục gốc và trạng thái hiển thị trong một giao diện tập trung.</div>
                </div>
                <div class="header-actions">
                    <button class="btn-blog btn-create" onclick="openProductModal()">+ Thêm sản phẩm</button>
                </div>
            </div>
            <div style="overflow: auto;">
                <table>
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Sản phẩm</th>
                            <th>SKU</th>
                            <th>Giá</th>
                            <th>Tồn kho</th>
                            <th>Hiển thị</th>
                            <th>Thao tác</th>
                        </tr>
                    </thead>
                    <tbody id="product-table-body"></tbody>
                </table>
            </div>
        </div>
    `;

    document.getElementById('header-toolbox').innerHTML = '';
    loadProducts();
}

async function loadProducts() {
    try {
        const response = await fetch(joinUrl(API_BASE_URL, '/api/products/admin'));
        if (!response.ok) throw new Error('Failed to load products');
        const products = await response.json();
        const tbody = document.getElementById('product-table-body');
        if (!tbody) return;

        tbody.innerHTML = products.map(product => `
            <tr>
                <td>${product.id}</td>
                <td>
                    <div style="display: flex; flex-direction: column; gap: 4px;">
                        <strong>${escapeHtml(product.name)}</strong>
                        <span class="text-muted small">${escapeHtml(product.category?.name || 'Chưa phân loại')}</span>
                    </div>
                </td>
                <td>${product.sku || '—'}</td>
                <td>${Number(product.price || 0).toLocaleString('vi-VN')}₫</td>
                <td>${product.stockQuantity ?? 0}</td>
                <td>${product.visible ? 'Có' : 'Ẩn'}</td>
                <td>
                    <button class="btn-secondary" onclick="openProductModal(${product.id})">Sửa</button>
                    <button class="btn-delete-folder" onclick="deleteProduct(${product.id})">Xóa</button>
                </td>
            </tr>
        `).join('');
    } catch (error) {
        console.error(error);
        document.getElementById('product-table-body').innerHTML = '<tr><td colspan="7">Không thể tải sản phẩm</td></tr>';
    }
}

async function openProductModal(productId = null) {
    const product = productId ? await fetchProductById(productId) : null;
    const categories = await loadCategoryOptions();

    AdminApp.showModal({
        id: 'product-modal',
        title: product ? 'Cập nhật sản phẩm' : 'Thêm sản phẩm',
        bodyHTML: `
            <div class="form-grid" style="display: grid; gap: 14px;">
                <div style="display: grid; gap: 14px; grid-template-columns: repeat(2, minmax(220px, 1fr));">
                    <label style="display: flex; flex-direction: column; gap: 6px;">
                        <span>Tên sản phẩm</span>
                        <input id="product-name" class="form-control" value="${escapeHtml(product?.name || '')}" />
                    </label>
                    <label style="display: flex; flex-direction: column; gap: 6px;">
                        <span>Slug</span>
                        <input id="product-slug" class="form-control" readonly value="${escapeHtml(product?.slug || '')}" />
                    </label>
                </div>
                <div style="display: grid; gap: 14px; grid-template-columns: repeat(2, minmax(220px, 1fr));">
                    <label style="display: flex; flex-direction: column; gap: 6px;">
                        <span>SKU</span>
                        <input id="product-sku" class="form-control" value="${escapeHtml(product?.sku || '')}" />
                    </label>
                    <label style="display: flex; flex-direction: column; gap: 6px;">
                        <span>Danh mục</span>
                        <select id="product-category" class="form-control">${categories}</select>
                    </label>
                </div>
                <div style="display: grid; gap: 14px; grid-template-columns: repeat(2, minmax(220px, 1fr));">
                    <label style="display: flex; flex-direction: column; gap: 6px;">
                        <span>Giá</span>
                        <input id="product-price" type="number" class="form-control" value="${escapeHtml(String(product?.price || 0))}" />
                    </label>
                    <label style="display: flex; flex-direction: column; gap: 6px;">
                        <span>Tồn kho</span>
                        <input id="product-stock" type="number" class="form-control" value="${escapeHtml(String(product?.stockQuantity ?? 0))}" />
                    </label>
                </div>
                <label style="display: flex; flex-direction: column; gap: 6px;">
                    <span>Mô tả ngắn</span>
                    <input id="product-short-description" class="form-control" value="${escapeHtml(product?.shortDescription || '')}" />
                </label>
                <div style="display: grid; gap: 14px; grid-template-columns: repeat(2, minmax(220px, 1fr));">
                    <label style="display: flex; flex-direction: column; gap: 6px;">
                        <span>URL ảnh</span>
                        <input id="product-image-url" class="form-control" value="${escapeHtml(product?.imageUrl || '')}" />
                    </label>
                    <label style="display: flex; flex-direction: column; gap: 6px;">
                        <span>Thư mục ảnh</span>
                        <input id="product-image-folder" class="form-control" value="${escapeHtml(product?.image_folder_path || '')}" />
                    </label>
                </div>
                <div style="display: grid; gap: 14px; grid-template-columns: repeat(2, minmax(220px, 1fr));">
                    <label style="display: flex; align-items: center; gap: 8px;">
                        <input id="product-available" type="checkbox" ${product?.available === false ? '' : 'checked'} />
                        <span>Đang bán</span>
                    </label>
                    <label style="display: flex; align-items: center; gap: 8px;">
                        <input id="product-visible" type="checkbox" ${product?.visible === false ? '' : 'checked'} />
                        <span>Hiển thị cho khách</span>
                    </label>
                </div>
            </div>
        `,
        confirmText: 'Lưu',
        onConfirm: async () => {
            const payload = {
                id: product?.id || null,
                name: document.getElementById('product-name').value.trim(),
                slug: document.getElementById('product-slug').value.trim(),
                sku: document.getElementById('product-sku').value.trim(),
                price: Number(document.getElementById('product-price').value || 0),
                stockQuantity: Number(document.getElementById('product-stock').value || 0),
                imageUrl: document.getElementById('product-image-url').value.trim(),
                image_folder_path: document.getElementById('product-image-folder').value.trim(),
                shortDescription: document.getElementById('product-short-description').value.trim(),
                available: document.getElementById('product-available').checked,
                visible: document.getElementById('product-visible').checked,
                category: document.getElementById('product-category').value ? { id: Number(document.getElementById('product-category').value) } : null
            };

            if (!payload.name) {
                alert('Vui lòng nhập tên sản phẩm');
                return false;
            }

            const response = await fetch(joinUrl(API_BASE_URL, '/api/products/admin'), {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                alert('Không thể lưu sản phẩm');
                return false;
            }

            loadProducts();
            return true;
        }
    });

    initSlugAutoGenerate('product-name', 'product-slug');

    const categorySelect = document.getElementById('product-category');
    if (categorySelect && product?.category?.id) {
        categorySelect.value = String(product.category.id);
    }
}

async function fetchProductById(productId) {
    const response = await fetch(joinUrl(API_BASE_URL, `/api/products/admin/${productId}`));
    return response.ok ? response.json() : null;
}

async function loadCategoryOptions() {
    const response = await fetch(joinUrl(API_BASE_URL, '/api/admin/categories/tree'));
    if (!response.ok) return '<option value="">— Chọn danh mục —</option>';

    const categories = await response.json();
    const flattened = [];
    const walk = (nodes, depth = 0) => nodes.forEach(node => {
        flattened.push({ ...node, depth });
        if (Array.isArray(node.children) && node.children.length) {
            walk(node.children, depth + 1);
        }
    });
    walk(categories || []);

    return '<option value="">— Chọn danh mục —</option>' + flattened.map(category => `<option value="${category.id}">${'— '.repeat(category.depth)}${escapeHtml(category.name)}</option>`).join('');
}

async function deleteProduct(id) {
    if (!confirm('Xóa sản phẩm này?')) return;
    const response = await fetch(joinUrl(API_BASE_URL, `/api/products/admin/${id}`), { method: 'DELETE' });
    if (response.ok) {
        loadProducts();
    }
}

function escapeHtml(value) {
    return String(value || '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

registerModule('products', renderProductModule);
