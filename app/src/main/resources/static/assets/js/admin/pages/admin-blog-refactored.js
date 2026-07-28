(function () {
    'use strict';

    const { escapeHtml, requestJson } = window.AdminConfig || {};

    const state = {
        currentSelectedFolder: null,
        folderDataCache: {
            articles: null,
            images: null
        },
        quill: null,
        lastQuillRange: null,
        selectedUploadFiles: []
    };

    function joinUrl(base, path) {
        if (!path) return base;
        return base.replace(/\/$/, '') + '/' + path.replace(/^\//, '');
    }

    function buildTree(paths) {
        const result = {};
        paths.forEach((path) => {
            if (!path) return;
            const parts = path.split('/');
            let current = result;
            parts.forEach((part) => {
                if (!current[part]) {
                    current[part] = { _isFolder: true, _path: path };
                }
                current = current[part];
            });
        });
        return result;
    }

    function getAllPathsFromCache(node, rootPath = '') {
        if (!node || typeof node !== 'object') return [];

        let paths = [];
        Object.keys(node).forEach((key) => {
            if (key.startsWith('_')) return;
            const item = node[key];
            if (item._path && (rootPath === '' || item._path.startsWith(rootPath))) {
                paths.push(item._path);
            }
            paths = paths.concat(getAllPathsFromCache(item, rootPath));
        });
        return [...new Set(paths)].sort();
    }

    function updateFolderActiveUI(folderPath) {
        document.querySelectorAll('.folder-item-wrapper').forEach((el) => {
            el.classList.remove('active');
        });

        if (folderPath === null) {
            const allItemsBtn = document.querySelector('.folder-item-wrapper[onclick*="selectFolder(null)"]');
            if (allItemsBtn) allItemsBtn.classList.add('active');
            return;
        }

        const target = document.querySelector(`.folder-item-wrapper[data-path="${folderPath}"]`);
        if (target) {
            target.classList.add('active');
        }
    }

    function renderTreeRecursive(node, container, level, type) {
        Object.keys(node).forEach((key) => {
            if (key.startsWith('_')) return;

            const nodeData = node[key];
            const fullPath = nodeData._path;
            const item = document.createElement('div');
            item.className = 'folder-group';
            const isActive = state.currentSelectedFolder === fullPath ? 'active' : '';

            item.innerHTML = `
                <div class="folder-item-wrapper ${isActive}" style="margin-left: ${level * 12}px" data-path="${fullPath}">
                    <div class="toggle-zone">
                        <i class="fa-solid fa-chevron-right"></i>
                    </div>
                    <div class="content-zone">
                        <i class="fa-regular fa-folder"></i>
                        <span>${key}</span>
                    </div>
                </div>
                <div class="sub-folders" style="display: none;"></div>`;

            const toggleBtn = item.querySelector('.toggle-zone');
            const contentBtn = item.querySelector('.content-zone');
            const subContainer = item.querySelector('.sub-folders');
            const icon = toggleBtn.querySelector('i');

            toggleBtn.onclick = (event) => {
                event.stopPropagation();
                const isExpanded = subContainer.style.display === 'block';
                subContainer.style.display = isExpanded ? 'none' : 'block';
                toggleBtn.style.transform = isExpanded ? 'rotate(0deg)' : 'rotate(90deg)';
            };

            contentBtn.onclick = (event) => {
                event.stopPropagation();
                document.querySelectorAll('.folder-item-wrapper').forEach((el) => el.classList.remove('active'));
                contentBtn.classList.add('active');
                if (type === 'articles') {
                    selectFolder(fullPath);
                } else if (type === 'images') {
                    loadImagesInFolder(fullPath);
                }
            };

            container.appendChild(item);
            renderTreeRecursive(node[key], subContainer, level + 1, type);
        });
    }

    function renderTreeUI(treeData, type) {
        const containerId = type === 'articles' ? 'folder-list' : 'image-folder-list';
        const container = document.getElementById(containerId);
        if (!container) return;

        const allActive = state.currentSelectedFolder === null ? 'active' : '';
        if (type === 'articles') {
            container.innerHTML = `
                <div class="folder-item-wrapper ${allActive}" onclick="window.selectFolder(null)">
                    <span class="content-zone"><i class="fa-solid fa-layer-group"></i> [Tất cả bài viết]</span>
                </div>`;
        } else if (type === 'images') {
            container.innerHTML = `
                <div class="folder-item-wrapper ${allActive}" onclick="window.loadImagesInFolder('')">
                    <span class="content-zone"><i class="fa-solid fa-layer-group"></i> [Thư mục gốc]</span>
                </div>`;
        }

        renderTreeRecursive(treeData, container, 0, type);
    }

    async function loadFolders(forceRefresh = false, type = 'articles') {
        let apiUrl = '';
        if (type === 'articles') {
            apiUrl = '/api/admin/folders';
        } else if (type === 'images') {
            apiUrl = '/api/admin/folders/images/tree';
        }

        if (state.folderDataCache[type] && !forceRefresh) {
            renderTreeUI(state.folderDataCache[type], type);
            return;
        }

        try {
            const response = await fetch(joinUrl(window.API_BASE_URL, apiUrl));
            const paths = await response.json();
            state.folderDataCache[type] = buildTree(paths);
            renderTreeUI(state.folderDataCache[type], type);
        } catch (error) {
            console.error('Lỗi tải thư mục:', error);
        }
    }

    function renderBlogTable(content, containerId = 'blog-table-body') {
        const tbody = document.getElementById(containerId);
        if (!tbody) return;
        tbody.innerHTML = '';

        if (!content || content.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="5" style="text-align: center; padding: 30px; color: #888;">
                        <i class="fa-solid fa-folder-open" style="font-size: 2rem; display: block; margin: auto;"></i>
                        Thư mục này hiện đang rỗng.
                    </td>
                </tr>`;
            return;
        }

        content.forEach((blog) => {
            const row = `
                <tr>
                    <td>${escapeHtml ? escapeHtml(blog.id) : blog.id}</td>
                    <td style="text-align: left">${escapeHtml ? escapeHtml(blog.title) : blog.title}</td>
                    <td>
                        <div class="form-check form-switch">
                            <input class="form-check-input" type="checkbox" ${blog.published ? 'checked' : ''} onchange="window.togglePublish(${blog.id}, this)">
                            <label class="form-check-label">${blog.published ? 'Công khai' : 'Đang ẩn'}</label>
                        </div>
                    </td>
                    <td>${blog.publishTime ? new Date(blog.publishTime).toLocaleDateString('vi-VN') : '---'}</td>
                    <td>
                        <button class="btn-edit" onclick="window.openBlogModal(${blog.id})"><i class="fa-solid fa-pen"></i></button>
                        <button class="btn-delete" onclick="window.deleteBlog(${blog.id}, '${escapeHtml ? escapeHtml(blog.title) : blog.title}')"><i class="fa-solid fa-trash"></i></button>
                    </td>
                </tr>`;
            tbody.insertAdjacentHTML('beforeend', row);
        });
    }

    function renderPagination(data, type = 'normal') {
        const containerId = type === 'advanced' ? 'adv-search-pagination' : 'blog-pagination';
        const container = document.getElementById(containerId);
        if (!container || !data || !data.page) return;

        const loadFunc = type === 'advanced' ? 'executeAdvancedSearch' : type === 'search' ? 'handleBlogSearch' : 'loadBlogs';
        const { page } = data;
        const startIdx = page.number * page.size + 1;
        const endIdx = Math.min(startIdx + page.size - 1, page.totalElements);

        let html = `
            <div class="pagination-info">
                Hiển thị <b>${startIdx}-${endIdx}</b> trên tổng số <b>${page.totalElements}</b> bài viết
                (Trang ${page.number + 1}/${page.totalPages})
            </div>
            <div class="pagination-controls">`;

        html += `
            <button class="btn-page" onclick="window.${loadFunc}(0)" ${page.number === 0 ? 'disabled' : ''}>
                <i class="fa-solid fa-angles-left"></i>
            </button>`;

        html += `
            <button class="btn-page" onclick="window.${loadFunc}(${page.number - 1})" ${page.number === 0 ? 'disabled' : ''}>
                <i class="fa-solid fa-angle-left"></i>
            </button>`;

        let startPage = Math.max(0, page.number - 2);
        let endPage = Math.min(page.totalPages - 1, startPage + 4);
        if (endPage - startPage < 4) startPage = Math.max(0, endPage - 4);

        for (let i = startPage; i <= endPage; i += 1) {
            html += `
                <button class="btn-page ${i === page.number ? 'active' : ''}" onclick="window.${loadFunc}(${i})">
                    ${i + 1}
                </button>`;
        }

        html += `
            <button class="btn-page" onclick="window.${loadFunc}(${page.number + 1})" ${page.number >= page.totalPages - 1 ? 'disabled' : ''}>
                <i class="fa-solid fa-angle-right"></i>
            </button>`;
        html += `
            <button class="btn-page" onclick="window.${loadFunc}(${page.totalPages - 1})" ${page.number >= page.totalPages - 1 ? 'disabled' : ''}>
                <i class="fa-solid fa-angles-right"></i>
            </button>`;

        html += '</div>';
        container.innerHTML = html;
    }

    async function loadBlogs(page = 0) {
        try {
            window.AdminApp.showLoading(true);
            let url = `/api/admin/blogs?page=${page}&size=10`;
            if (state.currentSelectedFolder) {
                url += `&folder=${encodeURIComponent(state.currentSelectedFolder)}/`;
            }

            const response = await fetch(url.toString());
            const data = await response.json();
            renderBlogTable(data.content);
            renderPagination(data);
        } catch (error) {
            console.error('Lỗi load blogs:', error);
        } finally {
            window.AdminApp.showLoading(false);
        }
    }

    function selectFolder(folderPath) {
        state.currentSelectedFolder = folderPath;
        updateFolderActiveUI(folderPath);

        const btnAdd = document.getElementById('btn-add-blog');
        const btnDelFolder = document.getElementById('btn-delete-folder');
        const isSelected = folderPath !== null;
        if (btnAdd) btnAdd.disabled = !isSelected;
        if (btnDelFolder) btnDelFolder.disabled = !isSelected;

        const title = document.getElementById('table-folder-title');
        if (title) title.innerText = isSelected ? `Thư mục: ${folderPath}` : 'Tất cả bài viết';

        loadBlogs(0);
    }

    async function togglePublish(blogId, checkboxElement) {
        const id = Number(blogId);
        const originalState = checkboxElement.checked;
        const label = checkboxElement.nextElementSibling;
        checkboxElement.disabled = true;

        try {
            const response = await fetch(joinUrl(window.API_BASE_URL, `/api/admin/blogs/${id}/publish`), {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(originalState)
            });

            if (!response.ok) throw new Error('Server error');
            if (label) label.innerText = originalState ? 'Công khai' : 'Đang ẩn';
        } catch (error) {
            alert('Lỗi: Không thể cập nhật trạng thái bài viết!');
            checkboxElement.checked = !originalState;
        } finally {
            checkboxElement.disabled = false;
            await loadBlogs();
        }
    }

    function deleteBlog(blogId, blogTitle) {
        window.AdminApp.showModal({
            title: 'Xác nhận xóa',
            bodyHTML: `<p>Bạn có chắc chắn muốn xóa bài viết:</p><p>ID: <strong class="text-danger">${blogId}</strong></p><p>Tên bài viết: <strong class="text-danger">${blogTitle}</strong></p><p style="color: red; font-size: 0.9em;">(Lưu ý: Bài viết chỉ xoá mềm)</p>`,
            confirmText: 'Xóa ngay',
            onConfirm: async () => {
                try {
                    const response = await fetch(joinUrl(window.API_BASE_URL, `/api/admin/blogs/${blogId}`), { method: 'DELETE' });
                    if (response.ok) {
                        loadBlogs(0);
                    }
                } catch (error) {
                    alert('Lỗi khi xóa bài viết');
                }
            }
        });
    }

    function createNewFolder() {
        const folderPaths = getAllPathsFromCache(state.folderDataCache.articles);
        const optionsHTML = folderPaths.map((path) => `<option value="${path}" ${path === state.currentSelectedFolder ? 'selected' : ''}>${path}</option>`).join('');
        const bodyHTML = `
            <div class="form-group">
                <label>Thư mục cha:</label>
                <select id="parent-folder-path" class="form-control">
                    <option value="">[Thư mục gốc]</option>
                    ${optionsHTML}
                </select>
            </div>
            <div class="form-group" style="margin-top: 15px;">
                <label>Tên thư mục mới:</label>
                <input type="text" id="new-folder-name" class="form-control" placeholder="new folder name">
            </div>`;

        window.AdminApp.showModal({
            id: 'folder-modal',
            title: 'Thêm thư mục mới',
            bodyHTML,
            confirmText: 'Tạo thư mục',
            onConfirm: async () => {
                const parent = document.getElementById('parent-folder-path').value;
                const name = document.getElementById('new-folder-name').value.trim();
                if (!name) {
                    alert('Vui lòng nhập tên thư mục');
                    return false;
                }
                const fullPath = parent ? `${parent}/${name}` : name;
                try {
                    const response = await fetch(joinUrl(window.API_BASE_URL, `/api/admin/folders?path=${encodeURIComponent(fullPath)}`), { method: 'POST' });
                    if (response.ok) {
                        await loadFolders(true);
                    } else {
                        alert(`Lỗi: ${await response.text()}`);
                    }
                } catch (error) {
                    alert('Không thể kết nối đến máy chủ');
                }
            }
        });
    }

    function confirmDeleteFolder() {
        if (!state.currentSelectedFolder) return;
        window.AdminApp.showModal({
            title: 'Xác nhận xóa thư mục',
            bodyHTML: `<p>Bạn có chắc chắn muốn xóa thư mục: <strong class="text-danger">${state.currentSelectedFolder}</strong>?</p><p class="text-muted small">* Lưu ý: Chỉ có thể xóa thư mục hoàn toàn trống.</p>`,
            confirmText: 'Xác nhận xóa',
            onConfirm: async () => {
                try {
                    const response = await fetch(joinUrl(window.API_BASE_URL, `/api/admin/folders?path=${encodeURIComponent(state.currentSelectedFolder)}`), { method: 'DELETE' });
                    if (response.ok) {
                        state.currentSelectedFolder = null;
                        await loadFolders(true);
                    } else {
                        alert(await response.text());
                    }
                } catch (error) {
                    alert('Lỗi kết nối khi xóa thư mục');
                }
            }
        });
    }

    function initSlugAutoGenerate() {
        const titleInput = document.getElementById('blog-title');
        const slugInput = document.getElementById('blog-slug');
        if (!titleInput || !slugInput) return;

        titleInput.addEventListener('input', () => {
            const slug = titleInput.value
                .toLowerCase()
                .normalize('NFD')
                .replace(/[\u0300-\u036f]/g, '')
                .replace(/[đÐ]/g, 'd')
                .replace(/([^a-z0-9-\s])/g, '')
                .replace(/(\s+)/g, '-')
                .replace(/-+/g, '-')
                .trim();
            slugInput.value = slug;
        });
    }

    async function validateAndSaveBlog(blogId) {
        const title = document.getElementById('blog-title').value.trim();
        const slug = document.getElementById('blog-slug').value.trim();
        let thumbnail = document.getElementById('blog-thumb-path').value;
        const folder = document.getElementById('blog-folder-select').value;
        const summary = document.getElementById('blog-summary').value.trim();
        const content = state.quill.root.innerHTML;
        const publishTime = document.getElementById('blog-publish-time').value;

        if (!title || !slug || content === '<p><br></p>') {
            alert('Vui lòng điền Tiêu đề, Slug và Nội dung!');
            return false;
        }

        if (thumbnail) {
            if (!thumbnail.startsWith('/images/')) {
                const cleanPath = thumbnail.startsWith('/') ? thumbnail.substring(1) : thumbnail;
                thumbnail = '/images/' + cleanPath;
            }
        } else {
            thumbnail = '/images/blog-thumb-1.jpg';
        }

        let contentPath = folder ? folder.trim() : null;
        if (contentPath && contentPath.startsWith('/')) contentPath = contentPath.substring(1);
        if (contentPath && !contentPath.endsWith('/')) contentPath += '/';

        const blogData = {
            id: blogId,
            title,
            slug,
            thumbnail,
            contentPath,
            publishTime: publishTime ? new Date(publishTime).toISOString() : null,
            summary,
            isPublished: blogId ? undefined : false,
            isFeatured: blogId ? undefined : false
        };

        return saveBlogToServer(blogData, content, blogId ? 'PUT' : 'POST');
    }

    async function saveBlogToServer(blogData, content, method) {
        try {
            window.AdminApp.showLoading(true);
            const url = new URL(joinUrl(window.API_BASE_URL, '/api/admin/blogs'));
            url.searchParams.append('content', content);

            const response = await fetch(url.toString(), {
                method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(blogData)
            });

            if (response.ok) {
                alert(method === 'POST' ? 'Tạo thành công!' : 'Cập nhật thành công!');
                loadBlogs();
                return true;
            }

            const errorText = await response.text();
            alert(`Lỗi server: ${errorText}`);
            return false;
        } catch (error) {
            console.error('Fetch Error:', error);
            alert('Không thể kết nối đến server.');
            return false;
        } finally {
            window.AdminApp.showLoading(false);
        }
    }

    function openCreateBlogModal() {
        const allFolderPaths = getAllPathsFromCache(state.folderDataCache.articles);
        const options = allFolderPaths.map((path) => `<option value="${path}" ${path === state.currentSelectedFolder ? 'selected' : ''}>${path}</option>`).join('');
        const html = `
            <form id="blog-submit-form" class="blog-form-body">
                <div class="blog-form-row">
                    <div class="blog-form-group flex-2">
                        <label>Tiêu đề bài viết<span class="text-danger">*</span></label>
                        <input type="text" id="blog-title" placeholder="Nhập tiêu đề...">
                    </div>
                    <div class="blog-form-group flex-1">
                        <label>Đường dẫn (Slug)<span class="text-danger">*</span></label>
                        <input type="text" id="blog-slug" class="input-highlight-yellow" readonly>
                    </div>
                </div>
                <div class="blog-form-row">
                    <div class="blog-form-group flex-1">
                        <label>Thư mục lưu trữ</label>
                        <select id="blog-folder-select">${options}</select>
                    </div>
                    <div class="blog-form-group flex-2">
                        <label>Ảnh đại diện (Thumbnail)</label>
                        <div class="blog-input-group">
                            <input type="text" id="blog-thumb-path" readonly placeholder="Chọn ảnh từ thư viện...">
                            <button type="button" onclick="window.openImageExplorer(window.setBlogThumbnail)">
                                <i class="fa-solid fa-image"></i>
                            </button>
                        </div>
                        <div id="blog-thumb-preview-wrap" class="mb-3" style="display: none;">
                            <label class="small text-muted">Xem trước ảnh:</label>
                            <div style="width: 150px; height: 100px; border: 1px solid #ddd; overflow: hidden; border-radius: 4px;">
                                <img id="blog-thumb-preview-img" src="" style="width: 100%; height: 100%; object-fit: cover;">
                            </div>
                        </div>
                    </div>
                </div>
                <div class="blog-form-row">
                    <div class="blog-form-group flex-1">
                        <label class="fw-bold">Thời gian đăng bài</label>
                        <input class="datetime-input-blog" type="datetime-local" id="blog-publish-time" value="31/12/1999">
                    </div>
                </div>
                <div class="blog-form-group">
                    <label>Tóm tắt ngắn</label>
                    <textarea id="blog-summary" rows="2" class="input-highlight-yellow">Mô tả bài viết</textarea>
                </div>
                <div class="blog-form-group quill-editor">
                    <label>Nội dung bài viết</label>
                    <div id="quill-editor"></div>
                </div>
            </form>`;

        const toolbox = `<button class="btn-sm btn-outline-secondary" onclick="alert('HD soạn thảo')"><i class="fa-solid fa-circle-question"></i> Hướng dẫn</button>`;

        window.AdminApp.showModal({
            id: 'blog-modal',
            title: 'Tạo bài viết mới',
            bodyHTML: html,
            toolboxHTML: toolbox,
            width: '90%',
            confirmText: 'Lưu bài viết',
            onConfirm: () => validateAndSaveBlog()
        });

        initQuill();
        initSlugAutoGenerate();
    }

    function registerQuillFormats() {
        if (typeof Quill === 'undefined') return;
        const QuillImage = Quill.import('formats/image');
        class BlogImage extends QuillImage {
            static create(value) {
                const node = super.create(value);
                node.setAttribute('class', 'blog-image');
                return node;
            }
        }
        Quill.register(BlogImage, true);
    }

    function addQuillTooltips() {
        const tooltipMap = {
            header: 'Tiêu đề',
            font: 'Kiểu chữ',
            size: 'Kích cỡ chữ',
            bold: 'Chữ đậm (Ctrl+B)',
            italic: 'Chữ nghiêng (Ctrl+I)',
            underline: 'Gạch chân (Ctrl+U)',
            strike: 'Gạch ngang',
            link: 'Chèn liên kết',
            image: 'Chèn ảnh',
            video: 'Chèn Video',
            blockquote: 'Trích dẫn',
            'code-block': 'Khối mã code',
            'list[value="ordered"]': 'Danh sách số',
            'list[value="bullet"]': 'Danh sách chấm',
            clean: 'Xóa định dạng',
            align: 'Căn lề',
            color: 'Màu chữ',
            background: 'Màu nền'
        };

        const toolbar = document.querySelector('.ql-toolbar');
        if (!toolbar) return;
        Object.entries(tooltipMap).forEach(([selector, title]) => {
            const el = toolbar.querySelector(`.ql-${selector}`);
            if (el) el.setAttribute('title', title);
        });
    }

    function initQuill() {
        registerQuillFormats();
        state.quill = new Quill('#quill-editor', {
            theme: 'snow',
            modules: {
                toolbar: {
                    container: [
                        [{ header: [2, 3, 4, false] }],
                        [{ font: [] }, { size: ['small', false, 'large', 'huge'] }],
                        ['bold', 'italic', 'underline', 'strike'],
                        [{ color: [] }, { background: [] }],
                        [{ script: 'sub' }, { script: 'super' }],
                        [{ align: [] }, { indent: '-1' }, { indent: '+1' }],
                        ['link', 'image', 'video', 'blockquote', 'code-block'],
                        [{ list: 'ordered' }, { list: 'bullet' }],
                        ['clean']
                    ],
                    handlers: {
                        image() {
                            state.lastQuillRange = state.quill.getSelection();
                            openImageExplorer((path) => insertImageToEditor(path));
                        }
                    }
                }
            }
        });
        addQuillTooltips();
    }

    function insertImageToEditor(path) {
        const range = state.lastQuillRange || { index: state.quill.getLength() };
        const fullUrl = '/' + path;
        state.quill.insertEmbed(range.index, 'image', fullUrl);
        state.quill.setSelection(range.index + 1);
    }

    async function openImageExplorer(onSelectCallback) {
        const html = `
            <div class="image-explorer-container">
                <aside id="exp-nav" class="explorer-nav">
                    <button onclick="window.toggleExpNav()" class="btn-sm mb-2"><i class="fa-solid fa-bars"></i></button>
                    <div class="folder-header">
                        <div class="folder-header-title">Thư mục ảnh</div>
                    </div>
                    <div id="image-folder-list">
                        <div class="folder-item active" onclick="window.selectFolder(null)">[Tất cả bài viết]</div>
                    </div>
                </aside>
                <section id="exp-files" class="explorer-main">
                    <p class="text-muted">Chọn một thư mục để xem ảnh</p>
                </section>
                <aside id="exp-preview" class="explorer-preview">
                    <div class="exp-preview-toolbox">
                        <button onclick="window.togglePreviewBox()" class="btn-explorer-preview btn-sm mb-2"><i class="fa-solid fa-bars"></i></button>
                    </div>
                    <div id="preview-box">
                        <i class="fa-regular fa-image fa-4x text-muted"></i>
                        <p>Xem trước ảnh</p>
                    </div>
                </aside>
            </div>`;

        const subHeader = `
            <div id="image-explorer-path-display" class="path-display">
                <div>
                    <span>Đường dẫn: </span>
                    <span id="image-explorer-link">/images</span>
                </div>
            </div>`;

        const toolbox = `<button class="btn-create w-100 mt-2" onclick="window.openUploadOverlay()"><i class="fa-solid fa-plus"></i> Upload ảnh mới</button>`;

        window.AdminApp.showModal({
            id: 'image-explorer-modal',
            title: 'Thư viện hình ảnh',
            bodyHTML: html,
            subHeader,
            toolboxHTML: toolbox,
            width: '95%',
            confirmText: 'Chèn ảnh này',
            onConfirm: () => {
                const pathDisplay = document.getElementById('image-explorer-link');
                let finalPath = pathDisplay ? pathDisplay.innerText : '';
                finalPath = finalPath.replace(/\s\/\s/g, '/').trim();
                if (finalPath && finalPath !== 'images' && onSelectCallback) {
                    onSelectCallback(finalPath);
                    return true;
                }
                alert('Vui lòng chọn một file ảnh cụ thể!');
                return false;
            }
        });

        await loadFolders(false, 'images');
    }

    function toggleExpNav() {
        document.getElementById('exp-nav').classList.toggle('collapsed');
    }

    function togglePreviewBox() {
        document.getElementById('exp-preview').classList.toggle('collapsed');
    }

    async function loadImagesInFolder(path) {
        state.currentSelectedFolder = path;
        updateFolderActiveUI(path);

        const mainArea = document.getElementById('exp-files');
        if (!mainArea) return;
        mainArea.innerHTML = '<div class="p-3">Đang tải...</div>';

        try {
            const url = joinUrl(window.API_BASE_URL, `/api/admin/folders/images/files?path=${encodeURIComponent(path)}`);
            const response = await fetch(url);
            const files = await response.json();
            updateImageExplorerPath(path);

            if (!files || files.length === 0) {
                mainArea.innerHTML = '<div class="empty-image-folder-text"><i class="fa-solid fa-folder-open" style="font-size: 2rem; display: block; margin: auto;"></i>Thư mục này hiện đang rỗng.</div>';
                return;
            }

            mainArea.innerHTML = files.map((file) => {
                const fileName = file.name || file;
                const imagePath = `/${path}/${fileName}`.replace(/\/+/, '/');
                const fullUrl = joinUrl(window.API_BASE_URL, 'images' + imagePath);
                return `<div class="img-item-card" onclick="window.previewImage('${imagePath}', this)"><img class="exp-image-item" src="${fullUrl}"><div class="exp-image-text">${fileName}</div></div>`;
            }).join('');
        } catch (error) {
            console.log(error);
            mainArea.innerHTML = '<div class="p-3 text-danger">Lỗi tải danh sách ảnh</div>';
        }
    }

    function previewImage(path, element) {
        document.querySelectorAll('.img-item-card').forEach((el) => el.classList.remove('selected'));
        element.classList.add('selected');
        updateImageExplorerPath(path);
        const fullUrl = joinUrl(window.API_BASE_URL, 'images' + path);
        const previewBox = document.getElementById('preview-box');
        if (previewBox) {
            previewBox.innerHTML = `<div class="preview-sticky"><img src="${fullUrl}" class="img-fluid rounded border" style="max-width: 100%; height: auto;"></div>`;
        }
    }

    function updateImageExplorerPath(path) {
        const pathDisplay = document.getElementById('image-explorer-link');
        if (pathDisplay) {
            const cleanDisplay = path.startsWith('/') ? path.substring(1) : path;
            pathDisplay.innerText = 'images/' + cleanDisplay.split('/').join('/');
        }
    }

    function setBlogThumbnail(path) {
        const input = document.getElementById('blog-thumb-path');
        const previewWrap = document.getElementById('blog-thumb-preview-wrap');
        const previewImg = document.getElementById('blog-thumb-preview-img');
        if (input) input.value = '/' + path;
        if (previewImg) {
            previewImg.src = joinUrl(window.API_BASE_URL, path);
            if (previewWrap) previewWrap.style.display = 'block';
        }
    }

    async function openBlogModal(blogId = null) {
        let blogData = {
            title: '',
            slug: '',
            thumbnail: '',
            contentPath: state.currentSelectedFolder || 'articles/',
            content: '',
            publishTime: '',
            createdAt: null,
            updatedAt: null
        };

        if (blogId) {
            try {
                window.AdminApp.showLoading(true);
                const response = await fetch(joinUrl(window.API_BASE_URL, `/api/admin/blogs/${blogId}`));
                if (!response.ok) throw new Error('Không lấy được dữ liệu bài viết');
                blogData = await response.json();
            } catch (error) {
                alert(error.message);
                window.AdminApp.showLoading(false);
                return;
            } finally {
                window.AdminApp.showLoading(false);
            }
        }

        const allFolderPaths = getAllPathsFromCache(state.folderDataCache.articles);
        const folderOptions = allFolderPaths.map((path) => {
            const cleanPath = path.endsWith('/') ? path : `${path}/`;
            const targetPath = blogData.contentPath?.endsWith('/') ? blogData.contentPath : `${blogData.contentPath}/`;
            const isSelected = cleanPath === targetPath ? 'selected' : '';
            return `<option value="${cleanPath}" ${isSelected}>${cleanPath}</option>`;
        }).join('');

        const formatDateTime = (dateStr) => {
            if (!dateStr) return '';
            const d = new Date(dateStr);
            return d.toISOString().slice(0, 16);
        };

        const bodyHtml = `
            <div class="blog-editor-form">
                <div class="blog-form-row">
                    <div class="blog-form-group flex-2">
                        <label class="fw-bold">Tiêu đề bài viết</label>
                        <input type="text" id="blog-title" value="${blogData.title}" placeholder="Nhập tiêu đề...">
                    </div>
                    <div class="blog-form-group flex-1">
                        <label class="fw-bold">Slug (Định danh)</label>
                        <input type="text" id="blog-slug" value="${blogData.slug}" class="input-highlight-yellow">
                    </div>
                </div>
                <div class="blog-form-row">
                    <div class="blog-form-group flex-1">
                        <label class="fw-bold">Thư mục lưu trữ</label>
                        <select id="blog-folder-select">${folderOptions}</select>
                    </div>
                    <div class="blog-form-group flex-1">
                        <label class="fw-bold">Ảnh đại diện</label>
                        <div class="blog-input-group">
                            <input type="text" id="blog-thumb-path" value="${blogData.thumbnail}" disabled readonly placeholder="Chọn ảnh...">
                            <button type="button" onclick="window.openImageExplorer(window.setBlogThumbnail)"><i class="fa-solid fa-image"></i></button>
                        </div>
                        <div id="blog-thumb-preview-wrap" class="mb-3" style="${blogData.thumbnail ? '' : 'display: none;'}">
                            <label class="small text-muted">Preview Thumbnail:</label>
                            <div style="width: 120px; height: 80px; border: 1px solid #ddd; border-radius: 4px; overflow: hidden;">
                                <img id="blog-thumb-preview-img" src="${blogData.thumbnail ? joinUrl(window.API_BASE_URL, blogData.thumbnail) : ''}" style="width: 100%; height: 100%; object-fit: cover;">
                            </div>
                        </div>
                    </div>
                </div>
                <div class="blog-form-row">
                    <div class="blog-form-group flex-1">
                        <label class="fw-bold">Thời gian đăng bài</label>
                        <input type="datetime-local" id="blog-publish-time" value="${formatDateTime(blogData.publishTime)}">
                    </div>
                    <div class="blog-form-group flex-1">
                        ${blogId ? `<label class="small text-muted">Thông tin hệ thống</label><div class="small p-2 border rounded bg-light">Thời gian tạo: ${new Date(blogData.createdTime).toLocaleString()}<br>Thời gian sửa gần nhất: ${new Date(blogData.updatedTime).toLocaleString()}</div>` : ''}
                    </div>
                </div>
                <div class="blog-form-group">
                    <label>Tóm tắt ngắn</label>
                    <textarea id="blog-summary" rows="2" class="input-highlight-yellow">Mô tả bài viết</textarea>
                </div>
                <div class="blog-form-group quill-editor">
                    <label class="fw-bold">Nội dung bài viết</label>
                    <div id="quill-editor">${blogData.content || ''}</div>
                </div>
            </div>`;

        window.AdminApp.showModal({
            id: 'blog-modal',
            title: blogId ? 'Cập nhật bài viết' : 'Thêm bài viết mới',
            bodyHTML: bodyHtml,
            width: '85%',
            confirmText: 'Lưu bài viết',
            onConfirm: () => validateAndSaveBlog(blogId)
        });

        initQuill();
    }

    async function handleBlogSearch(page = 0) {
        const input = document.getElementById('blog-search-input');
        const keyword = input.value.trim();
        if (!keyword) {
            loadBlogs(0);
            return;
        }

        try {
            window.AdminApp.showLoading(true);
            const params = new URLSearchParams();
            if (!isNaN(keyword)) params.append('id', keyword);
            else params.append('title', keyword);
            if (state.currentSelectedFolder) params.append('contentPath', `${state.currentSelectedFolder}/`);
            params.append('page', page);
            params.append('size', 10);

            const response = await fetch(joinUrl(window.API_BASE_URL, `/api/admin/blogs/search?${params.toString()}`));
            const data = await response.json();
            renderBlogTable(data.content);
            renderPagination(data, 'search');
            const title = document.getElementById('table-folder-title');
            if (title) title.innerText = `Kết quả tìm kiếm cho "${keyword}" trong ${state.currentSelectedFolder || 'Tất cả'}`;
        } catch (error) {
            console.error('Lỗi tìm kiếm nhanh:', error);
        } finally {
            window.AdminApp.showLoading(false);
        }
    }

    async function openBlogAdvancedSearch() {
        const bodyHtml = `
            <div class="advanced-search-container">
                <div class="search-filter-grid">
                    <div class="filter-group"><label>ID Bài viết</label><input type="number" id="adv-search-id" placeholder="VD: 101"></div>
                    <div class="filter-group"><label>Tiêu đề / Slug</label><input type="text" id="adv-search-title" placeholder="Nhập từ khóa..."></div>
                    <div class="filter-group"><label>Từ ngày</label><input type="date" id="adv-search-from"></div>
                    <div class="filter-group"><label>Đến ngày</label><input type="date" id="adv-search-to"></div>
                    <div class="filter-group full-width"><label>Thẻ (Tags)</label><input type="text" id="adv-search-tags" placeholder="Hiện tại chưa tạo chức năng tags"></div>
                </div>
                <div class="search-action-row">
                    <button class="btn-blog reset-field" onclick="window.resetAdvancedFields()">Làm mới</button>
                    <button class="btn-blog btn-create" onclick="window.executeAdvancedSearch()"><i class="fa-solid fa-magnifying-glass"></i> Lọc kết quả</button>
                </div>
                <hr class="search-divider">
                <div class="search-results-wrapper">
                    <div id="adv-search-results-info" class="small text-muted mb-2"></div>
                    <div class="adv-results-table-container">
                        <table class="admin-table mini-table">
                            <thead><tr><th width="60">ID</th><th>Tiêu đề</th><th width="120">Trạng thái</th><th width="120">Ngày đăng</th><th width="100">Thao tác</th></tr></thead>
                            <tbody id="adv-search-results-body"><tr><td colspan="5" class="text-center">Nhập thông tin và nhấn "Lọc kết quả" để xem dữ liệu.</td></tr></tbody>
                        </table>
                    </div>
                </div>
                <div class="table-footer"><div id="adv-search-pagination" class="pagination-container mt-2"></div></div>
            </div>`;

        window.AdminApp.showModal({
            id: 'blog-advanced-search-modal',
            title: 'Tìm kiếm bài viết chi tiết',
            bodyHTML: bodyHtml,
            confirmText: 'Đóng',
            hideFooter: true,
            onConfirm: () => true
        });
    }

    async function executeAdvancedSearch(page = 0) {
        const id = document.getElementById('adv-search-id').value;
        const title = document.getElementById('adv-search-title').value;
        const from = document.getElementById('adv-search-from').value;
        const to = document.getElementById('adv-search-to').value;

        const params = new URLSearchParams();
        if (id) params.append('id', id);
        if (title) params.append('title', title);
        if (from) params.append('fromDate', from);
        if (to) params.append('toDate', to);
        params.append('page', page);
        params.append('size', 10);

        try {
            const response = await fetch(joinUrl(window.API_BASE_URL, `/api/admin/blogs/search?${params.toString()}`));
            const data = await response.json();
            renderBlogTable(data.content, 'adv-search-results-body');
            renderPagination(data, 'advanced');
        } catch (error) {
            console.error('Lỗi search:', error);
        }
    }

    function resetAdvancedFields() {
        ['adv-search-id', 'adv-search-title', 'adv-search-from', 'adv-search-to', 'adv-search-tags'].forEach((id) => {
            const el = document.getElementById(id);
            if (el) el.value = '';
        });
        const resultsBody = document.getElementById('adv-search-results-body');
        if (resultsBody) resultsBody.innerHTML = '<tr><td colspan="5" class="text-center">Chưa có dữ liệu</td></tr>';
        const pagination = document.getElementById('adv-search-pagination');
        if (pagination) pagination.innerHTML = '';
        const info = document.getElementById('adv-search-results-info');
        if (info) info.innerText = '';
    }

    function openUploadOverlay() {
        const displayFolder = state.currentSelectedFolder ? state.currentSelectedFolder : 'Thư mục gốc';
        window.AdminApp.showModal({
            id: 'upload-image-modal',
            title: '<i class="fa-solid fa-cloud-arrow-up"></i> Tải ảnh mới lên hệ thống',
            bodyHTML: `
                <div class="upload-overlay-wrapper">
                    <div class="upload-form-group">
                        <label class="upload-label">Thư mục đích hiện tại:</label>
                        <div class="upload-folder-display"><i class="fa-solid fa-folder"></i> <span>${displayFolder}</span></div>
                    </div>
                    <div class="upload-toggle-container">
                        <label class="upload-label-inline">Chế độ Upload:</label>
                        <div class="upload-switch-wrapper">
                            <label class="upload-switch">
                                <input type="checkbox" id="upload-mode-toggle" onchange="window.toggleUploadMode(this.checked)">
                                <span class="upload-slider"></span>
                            </label>
                            <span id="toggle-label" class="upload-toggle-text mode-raw">Ảnh thông thường (Giữ nguyên gốc)</span>
                        </div>
                    </div>
                    <div id="product-slug-wrapper" class="upload-form-group" style="display: none;">
                        <label class="upload-label">Slug Sản phẩm <span class="upload-required">*</span>:</label>
                        <input type="text" id="upload-product-slug" class="upload-input-text" placeholder="Ví dụ: ban-phim-co-gvn">
                        <small class="upload-help-text">Hệ thống sẽ tự động tối ưu và băm ảnh thành 3 kích thước (compact, grande, master).</small>
                        <div class="filter-group" style="margin-top: 12px;">
                            <label style="font-weight: 600; font-size: 13px; color: #475569;">Định dạng ảnh xuất ra:</label>
                            <select id="upload-image-format-select" style="width: 100%; padding: 8px 12px; margin-top: 6px; border: 1px solid #ddd; border-radius: 4px; font-size: 13px; background-color: #fff; outline: none; cursor: pointer;">
                                <option value="jpg">Định dạng JPG (Thumbnailator - mặc định)</option>
                                <option value="webp">Định dạng WEBP (Google CLI cwebp - dung lượng nhẹ hơn thumbnailator)</option>
                            </select>
                            <div id="webp-note-helper" style="display: none; color: #d97706; font-size: 11px; margin-top: 6px; line-height: 1.4;">
                                <i class="fa-solid fa-triangle-exclamation"></i> <strong>Lưu ý:</strong> Chế độ WEBP yêu cầu máy chủ chạy API phải cài đặt sẵn bộ công cụ libwebp của Google và cấu hình biến môi trường Path.
                            </div>
                        </div>
                    </div>
                    <div class="upload-form-group">
                        <label class="upload-label">Chọn file từ máy tính:</label>
                        <div class="upload-file-zone">
                            <input type="file" id="modal-file-input" multiple accept="image/webp, image/jpeg, image/png" onchange="window.handleModalFileSelect(this)">
                            <label for="modal-file-input" class="upload-file-trigger"><i class="fa-solid fa-images"></i> Bấm để chọn hoặc kéo thả nhiều ảnh vào đây...</label>
                        </div>
                    </div>
                    <div class="upload-preview-wrapper">
                        <div id="upload-preview-list" class="upload-preview-list"><span class="upload-empty-text" id="no-file-text">Chưa có tệp tin nào được chọn</span></div>
                    </div>
                </div>` ,
            confirmText: 'Bắt đầu Tải lên',
            onConfirm: async () => {
                await executeUploadFlow();
            }
        });

        state.selectedUploadFiles = [];
        document.getElementById('upload-image-format-select')?.addEventListener('change', function (event) {
            const helper = document.getElementById('webp-note-helper');
            if (helper) helper.style.display = event.target.value === 'webp' ? 'block' : 'none';
        });
    }

    function toggleUploadMode(isProductMode) {
        const label = document.getElementById('toggle-label');
        const slugWrapper = document.getElementById('product-slug-wrapper');
        if (!label || !slugWrapper) return;

        if (isProductMode) {
            label.innerText = 'Ảnh Sản phẩm (Tự động resize 3 mức compact, grande và master)';
            label.className = 'upload-toggle-text mode-product';
            slugWrapper.style.display = 'block';
            if (typeof window.CURRENT_PRODUCT_SLUG !== 'undefined' && window.CURRENT_PRODUCT_SLUG) {
                document.getElementById('upload-product-slug').value = window.CURRENT_PRODUCT_SLUG;
            }
        } else {
            label.innerText = 'Ảnh thông thường (Giữ nguyên gốc)';
            label.className = 'upload-toggle-text mode-raw';
            slugWrapper.style.display = 'none';
        }
    }

    function handleModalFileSelect(input) {
        const previewList = document.getElementById('upload-preview-list');
        const noFileText = document.getElementById('no-file-text');
        const MAX_SIZE_MB = 5;
        if (!previewList || !input.files || input.files.length === 0) return;
        if (noFileText) noFileText.remove();

        Array.from(input.files).forEach((file) => {
            if (file.size > MAX_SIZE_MB * 1024 * 1024) {
                alert(`File "${file.name}" dung lượng quá lớn! Vui lòng chọn file dưới ${MAX_SIZE_MB}MB.`);
                return;
            }

            state.selectedUploadFiles.push(file);
            const objectUrl = URL.createObjectURL(file);
            const fileSizeInMB = (file.size / (1024 * 1024)).toFixed(2);
            const itemRow = document.createElement('div');
            itemRow.className = 'upload-preview-item';
            itemRow.id = `upload-item-${state.selectedUploadFiles.length - 1}`;
            itemRow.innerHTML = `
                <div class="upload-item-info">
                    <img src="${objectUrl}" class="upload-item-thumb">
                    <div class="upload-item-meta">
                        <span class="upload-item-name" title="${file.name}">${file.name}</span>
                        <span class="upload-item-size">${fileSizeInMB} MB</span>
                    </div>
                </div>
                <button class="upload-item-remove-btn" onclick="window.removeSelectedFileFromUpload(${state.selectedUploadFiles.length - 1}, '${objectUrl}')" type="button">
                    <i class="fa-solid fa-trash-can"></i>
                </button>`;
            previewList.appendChild(itemRow);
        });

        input.value = '';
    }

    function removeSelectedFileFromUpload(index, objectUrl) {
        const row = document.getElementById(`upload-item-${index}`);
        if (row) row.remove();
        URL.revokeObjectURL(objectUrl);
        state.selectedUploadFiles[index] = null;
        const previewList = document.getElementById('upload-preview-list');
        if (previewList && previewList.children.length === 0) {
            previewList.innerHTML = '<span class="upload-empty-text" id="no-file-text">Chưa có tệp tin nào được chọn</span>';
        }
    }

    async function executeUploadFlow() {
        const finalFiles = state.selectedUploadFiles.filter((file) => file !== null);
        if (finalFiles.length === 0) {
            alert('Vui lòng chọn ít nhất một file ảnh!');
            return;
        }

        const isProductMode = document.getElementById('upload-mode-toggle').checked;
        const formData = new FormData();
        finalFiles.forEach((file) => formData.append('files', file));
        formData.append('folder', state.currentSelectedFolder ? state.currentSelectedFolder : '');

        let targetEndpoint = '';
        if (isProductMode) {
            const productSlugInput = document.getElementById('upload-product-slug');
            const slugValue = productSlugInput ? productSlugInput.value.trim() : '';
            if (!slugValue) {
                alert('Vui lòng điền mã Slug của sản phẩm!');
                productSlugInput.focus();
                return;
            }
            const formatValue = document.getElementById('upload-image-format-select')?.value || 'jpg';
            formData.append('format', formatValue);
            formData.append('productSlug', slugValue);
            targetEndpoint = '/api/admin/images/product-upload';
        } else {
            targetEndpoint = '/api/admin/images/raw-upload';
        }

        window.AdminApp.showLoading(true);
        try {
            const response = await fetch(joinUrl(window.API_BASE_URL, targetEndpoint), {
                method: 'POST',
                body: formData
            });
            const result = await response.json();
            if (response.ok && result.success) {
                alert(result.message || 'Tải tài nguyên lên server thành công!');
                const closeBtn = document.querySelector('.modal-header .close') || document.querySelector('[data-dismiss="modal"]');
                if (closeBtn) closeBtn.click();
                loadImagesInFolder(state.currentSelectedFolder);
            } else {
                alert(`Lỗi hệ thống: ${result.message || 'Không thể thực thi.'}`);
            }
        } catch (error) {
            console.error('Lỗi API kết nối:', error);
            alert('Mất kết nối tới máy chủ API Spring Boot.');
        } finally {
            window.AdminApp.showLoading(false);
        }
    }

    function renderBlogModule() {
        const root = document.getElementById('admin-app-root');
        if (!root) return;

        root.innerHTML = `
            <div class="blog-manager-container">
                <aside class="folder-tree-sidebar">
                    <div class="folder-header">
                        <div class="folder-header-title">Thư mục</div>
                        <button onclick="window.createNewFolder()" title="Thêm thư mục" style="padding: 4px;"><i class="fa-solid fa-folder-plus"></i></button>
                    </div>
                    <div id="folder-list">
                        <div class="folder-item active" onclick="window.selectFolder(null)">[Tất cả bài viết]</div>
                    </div>
                </aside>
                <section class="blog-list-main">
                    <div class="admin-table-container">
                        <div class="table-header">
                            <div id="table-folder-title" class="folder-title-text">Tất cả bài viết</div>
                            <div class="header-actions">
                                <button class="btn-blog btn-create" onclick="window.openImageExplorer(path => document.getElementById('blog-thumb-path').value = path)">
                                    <i class="fa-solid fa-image"></i> Thư viện ảnh
                                </button>
                                <button id="btn-delete-folder" class="btn-blog btn-delete-folder" disabled onclick="window.confirmDeleteFolder()">
                                    <i class="fa-solid fa-folder-minus"></i> Xóa thư mục
                                </button>
                                <button id="btn-add-blog" class="btn-blog btn-create" disabled onclick="window.openCreateBlogModal()">
                                    + Viết bài mới
                                </button>
                            </div>
                        </div>
                        <table>
                            <thead>
                                <tr><th>ID</th><th>Tiêu đề</th><th>Trạng thái</th><th>Ngày đăng</th><th>Thao tác</th></tr>
                            </thead>
                            <tbody id="blog-table-body"></tbody>
                        </table>
                        <div class="table-footer"><div id="blog-pagination" class="pagination-container"></div></div>
                    </div>
                </section>
            </div>`;

        const toolbox = document.getElementById('header-toolbox');
        if (toolbox) {
            toolbox.innerHTML = `
                <div class="search-box">
                    <button class="search-config-toolbox" onclick="window.openBlogAdvancedSearch()"><i class="fa-solid fa-sliders"></i>Tìm kiếm chi tiết</button>
                    <div class="search-bar-toolbox">
                        <input class="search-input-toolbox" type="text" id="blog-search-input" onkeypress="if(event.key==='Enter') window.handleBlogSearch()" placeholder="Tìm theo ID hoặc Tiêu đề">
                        <button class="search-btn-toolbox" onclick="window.handleBlogSearch()"><i class="fa-solid fa-magnifying-glass"></i></button>
                    </div>
                </div>`;
        }

        loadFolders(false, 'articles');
        loadBlogs();
    }

    window.renderBlogModule = renderBlogModule;
    window.renderBlogModule = renderBlogModule;
    window.selectFolder = selectFolder;
    window.createNewFolder = createNewFolder;
    window.confirmDeleteFolder = confirmDeleteFolder;
    window.togglePublish = togglePublish;
    window.deleteBlog = deleteBlog;
    window.openCreateBlogModal = openCreateBlogModal;
    window.openImageExplorer = openImageExplorer;
    window.loadImagesInFolder = loadImagesInFolder;
    window.previewImage = previewImage;
    window.setBlogThumbnail = setBlogThumbnail;
    window.openBlogModal = openBlogModal;
    window.handleBlogSearch = handleBlogSearch;
    window.openBlogAdvancedSearch = openBlogAdvancedSearch;
    window.executeAdvancedSearch = executeAdvancedSearch;
    window.resetAdvancedFields = resetAdvancedFields;
    window.openUploadOverlay = openUploadOverlay;
    window.toggleUploadMode = toggleUploadMode;
    window.handleModalFileSelect = handleModalFileSelect;
    window.removeSelectedFileFromUpload = removeSelectedFileFromUpload;
    window.toggleExpNav = toggleExpNav;
    window.togglePreviewBox = togglePreviewBox;

    if (window.AdminApp && typeof window.AdminApp.registerModule === 'undefined') {
        window.registerModule('blogs', renderBlogModule);
    } else {
        window.registerModule('blogs', renderBlogModule);
    }
})();
