/**
 * admin-folder.js - Shared folder state, tree rendering, and folder management.
 */
let currentSelectedFolder = null;
let currentSelectedImageFolder = null;
let folderDataCache = {
    articles: null,
    images: null
};

async function loadFolders(forceRefresh = false, type = 'articles') {
    const apiUrl = type === 'images' ? '/api/admin/folders/images/tree' : '/api/admin/folders';

    if (folderDataCache[type] && !forceRefresh) {
        renderTreeUI(folderDataCache[type], type);
        return;
    }

    try {
        const response = await fetch(joinUrl(API_BASE_URL, apiUrl), { credentials: 'same-origin' });
        if (!response.ok) {
            await handleApiError(response);
            throw new Error(`HTTP ${response.status}`);
        }
        const payload = await response.json().catch(() => []);
        folderDataCache[type] = buildTree(Array.isArray(payload) ? payload : []);
        renderTreeUI(folderDataCache[type], type);
    } catch (error) {
        console.error('Lỗi tải thư mục:', error);
        folderDataCache[type] = {};
        renderTreeUI(folderDataCache[type], type);
    }
}

function buildTree(paths) {
    const result = {};
    (Array.isArray(paths) ? paths : []).forEach(path => {
        const normalizedPath = String(path || '').trim().replace(/^\/+|\/+$/g, '');
        if (!normalizedPath) return;

        const parts = normalizedPath.split('/').filter(Boolean);
        let current = result;
        parts.forEach((part, index) => {
            const folderPath = parts.slice(0, index + 1).join('/');
            if (!current[part]) current[part] = { _isFolder: true, _path: folderPath };
            current = current[part];
        });
    });
    return result;
}

function renderTreeUI(treeData, type) {
    const containerId = type === 'articles' ? 'folder-list' : 'image-folder-list';
    const container = document.getElementById(containerId);
    if (!container) return;

    const selectedFolder = type === 'images' ? currentSelectedImageFolder : currentSelectedFolder;
    const allActive = selectedFolder === null ? 'active' : '';
    container.innerHTML = type === 'articles'
        ? `<div class="folder-item-wrapper ${allActive}" onclick="selectFolder(null)"><span class="content-zone"><i class="fa-solid fa-layer-group"></i> [Tất cả bài viết]</span></div>`
        : `<div class="folder-item-wrapper ${allActive}" onclick="loadImagesInFolder('')"><span class="content-zone"><i class="fa-solid fa-layer-group"></i> [Thư mục gốc]</span></div>`;

    renderTreeRecursive(treeData, container, 0, type);
}

function renderTreeRecursive(node, container, level, type) {
    Object.keys(node).forEach(key => {
        if (key.startsWith('_')) return;

        const nodeData = node[key];
        const fullPath = nodeData._path;
        const item = document.createElement('div');
        item.className = 'folder-group';
        item.innerHTML = `
            <div class="folder-item-wrapper ${(type === 'images' ? currentSelectedImageFolder : currentSelectedFolder) === fullPath ? 'active' : ''}" style="margin-left: ${level * 12}px" data-path="${fullPath}">
                <div class="toggle-zone"><i class="fa-solid fa-chevron-right"></i></div>
                <div class="content-zone"><i class="fa-regular fa-folder"></i><span>${key}</span></div>
            </div>
            <div class="sub-folders" style="display: none;"></div>`;

        const toggleButton = item.querySelector('.toggle-zone');
        const contentButton = item.querySelector('.content-zone');
        const subContainer = item.querySelector('.sub-folders');

        toggleButton.onclick = event => {
            event.stopPropagation();
            const isExpanded = subContainer.style.display === 'block';
            subContainer.style.display = isExpanded ? 'none' : 'block';
            toggleButton.style.transform = isExpanded ? 'rotate(0deg)' : 'rotate(90deg)';
        };

        contentButton.onclick = event => {
            event.stopPropagation();
            const listId = type === 'articles' ? '#folder-list' : '#image-folder-list';
            document.querySelectorAll(`${listId} .folder-info, ${listId} .folder-item`).forEach(element => element.classList.remove('active'));
            contentButton.classList.add('active');
            if (type === 'articles') selectFolder(fullPath);
            if (type === 'images') loadImagesInFolder(fullPath);
        };

        container.appendChild(item);
        renderTreeRecursive(node[key], subContainer, level + 1, type);
    });
}

function selectFolder(folderPath) {
    currentSelectedFolder = folderPath;
    updateFolderActiveUI(folderPath);

    const addButton = document.getElementById('btn-add-blog');
    const deleteButton = document.getElementById('btn-delete-folder');
    const isSelected = folderPath !== null;
    if (addButton) addButton.disabled = !isSelected;
    if (deleteButton) deleteButton.disabled = !isSelected;

    const title = document.getElementById('table-folder-title');
    if (title) title.innerText = isSelected ? `Thư mục: ${folderPath}` : 'Tất cả bài viết';
    loadBlogs(0);
}

function updateFolderActiveUI(folderPath, type = 'articles') {
    document.querySelectorAll('.folder-item-wrapper').forEach(element => element.classList.remove('active'));
    if (folderPath === null) {
        const selector = type === 'images'
            ? '.folder-item-wrapper[onclick*="loadImagesInFolder(\'\')"]'
            : '.folder-item-wrapper[onclick*="selectFolder(null)"]';
        const allItemsButton = document.querySelector(selector);
        if (allItemsButton) allItemsButton.classList.add('active');
        return;
    }

    const target = document.querySelector(`.folder-item-wrapper[data-path="${folderPath}"]`);
    if (target) target.classList.add('active');
}

function getAllPathsFromCache(node, rootPath = '') {
    if (!node || typeof node !== 'object') return [];

    let paths = [];
    Object.keys(node).forEach(key => {
        if (key.startsWith('_')) return;
        const item = node[key];
        if (item._path && (rootPath === '' || item._path.startsWith(rootPath))) paths.push(item._path);
        paths = paths.concat(getAllPathsFromCache(item, rootPath));
    });
    return [...new Set(paths)].sort();
}

function createNewFolder() {
    const folderPaths = getAllPathsFromCache(folderDataCache.articles);
    const optionsHTML = folderPaths.map(path => `<option value="${path}" ${path === currentSelectedFolder ? 'selected' : ''}>${path}</option>`).join('');
    const bodyHTML = `
        <div class="form-group"><label>Thư mục cha:</label><select id="parent-folder-path" class="form-control"><option value="">[Thư mục gốc]</option>${optionsHTML}</select></div>
        <div class="form-group" style="margin-top: 15px;"><label>Tên thư mục mới:</label><input type="text" id="new-folder-name" class="form-control" placeholder="new folder name"></div>`;

    AdminApp.showModal({
        id: 'folder-modal', title: 'Thêm thư mục mới', bodyHTML, confirmText: 'Tạo thư mục',
        onConfirm: async () => {
            const parent = document.getElementById('parent-folder-path').value;
            const name = document.getElementById('new-folder-name').value.trim();
            if (!name) return alert('Vui lòng nhập tên thư mục');
            const fullPath = parent ? `${parent}/${name}` : name;
            try {
                const response = await fetch(joinUrl(API_BASE_URL, `/api/admin/folders?path=${encodeURIComponent(fullPath)}`), {
                    method: 'POST', headers: csrfHeaders()
                });
                if (response.ok) await loadFolders(true);
                else await handleApiError(response);
            } catch (error) {
                alert('Không thể kết nối đến máy chủ');
            }
        }
    });
}

function confirmDeleteFolder() {
    if (!currentSelectedFolder) return;
    AdminApp.showModal({
        title: 'Xác nhận xóa thư mục',
        bodyHTML: `<p>Bạn có chắc chắn muốn xóa thư mục: <strong class="text-danger">${currentSelectedFolder}</strong>?</p><p class="text-muted small">* Lưu ý: Chỉ có thể xóa thư mục hoàn toàn trống.</p>`,
        confirmText: 'Xác nhận xóa',
        onConfirm: async () => {
            try {
                const response = await fetch(joinUrl(API_BASE_URL, `/api/admin/folders?path=${encodeURIComponent(currentSelectedFolder)}`), {
                    method: 'DELETE', headers: csrfHeaders()
                });
                if (response.ok) {
                    currentSelectedFolder = null;
                    await loadFolders(true);
                } else await handleApiError(response);
            } catch (error) {
                alert('Lỗi kết nối khi xóa thư mục');
            }
        }
    });
}
