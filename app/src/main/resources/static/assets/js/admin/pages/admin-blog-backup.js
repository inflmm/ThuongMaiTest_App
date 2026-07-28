/**
 * admin-core.js (Tiếp theo) - Logic cho Module Blog
 */
let currentSelectedFolder = null; // Lưu thư mục đang chọn
async function renderBlogModule() {
    const root = document.getElementById('admin-app-root');

    // 1. Tạo khung HTML cho module
    root.innerHTML = `
        <div class="blog-manager-container">
            <aside class="folder-tree-sidebar">
                <div class="folder-header">
                    <div class="folder-header-title">Thư mục</div>
                    <button onclick="createNewFolder()" title="Thêm thư mục" style="padding: 4px;"><i class="fa-solid fa-folder-plus"></i></button>
                
                </div>
                <div id="folder-list">
                    <div class="folder-item active" onclick="selectFolder(null)">[Tất cả bài viết]</div>
                </div>
            </aside>

            <section class="blog-list-main">
                <div class="admin-table-container">
                    <div class="table-header">
                        <div id="table-folder-title" class="folder-title-text">Tất cả bài viết</div>
                        <div class="header-actions">
                            <button class="btn btn-outline-primary" onclick="openImageExplorer(path => document.getElementById('blog-thumb-path').value = path)">
                                <i class="fa-solid fa-image"></i> Thêm ảnh
                            </button>
                            <button id="btn-delete-folder" class="btn-delete-folder" disabled onclick="confirmDeleteFolder()">
                                <i class="fa-solid fa-folder-minus"></i> Xóa thư mục
                            </button>
                            <button id="btn-add-blog" class="btn-create" disabled onclick="openCreateBlogModal()">
                                + Viết bài mới
                            </button>
                        </div>
                    </div>
                    <table>
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Tiêu đề</th>
                                <th>Trạng thái</th>
                                <th>Ngày đăng</th>
                                <th>Thao tác</th>
                            </tr>
                        </thead>
                        <tbody id="blog-table-body"></tbody>
                    </table>
                    </div>
            </section>
        </div>
    `;

    loadFolders(); // Gọi API lấy danh sách folder
    loadBlogs();   // Gọi API lấy danh sách blog
}

// Xử lý Toggle Publish với Optimistic UI
async function togglePublish(blogId, checkboxElement) {
    const id = Number(blogId);
    const originalState = checkboxElement.checked;
    const label = checkboxElement.nextElementSibling; // Phần text hiển thị trạng thái

    // 1. Chặn tương tác
    checkboxElement.disabled = true;

    try {
        const response = await fetch(joinUrl(API_BASE_URL, `/api/admin/blogs/${id}/publish`), {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(originalState)
        });

        if (!response.ok) throw new Error("Server error");

        // Cập nhật label
        label.innerText = originalState ? "Công khai" : "Đang ẩn";
        console.log("Cập nhật trạng thái thành công");

    } catch (error) {
        // 2. Rollback nếu lỗi
        alert("Lỗi: Không thể cập nhật trạng thái bài viết!");
        checkboxElement.checked = !originalState; // Quay về trạng thái cũ
    } finally {
        // 3. Mở khóa
        checkboxElement.disabled = false;
    }
}

function buildTree(paths) {
    const result = {};
    paths.forEach(path => {
        const parts = path.split('/');
        let current = result;
        parts.forEach(part => {
            if (!current[part]) current[part] = { _isFolder: true, _path: path };
            current = current[part];
        });
    });
    return result;
}

let folderDataCache = null; // Biến lưu trữ cache folder

async function loadFolders(forceRefresh = false) {
    // Nếu đã có cache và không yêu cầu refresh thì không gọi API nữa
    if (folderDataCache && !forceRefresh) {
        renderTreeUI(folderDataCache);
        return;
    }

    try {
        const response = await fetch(joinUrl(API_BASE_URL, '/api/admin/folders'));
        const paths = await response.json();
        folderDataCache = buildTree(paths); // Lưu vào cache
        renderTreeUI(folderDataCache);
    } catch (e) {
        console.error("Lỗi tải thư mục:", e);
    }
}

function renderTreeUI(treeData) {
    const container = document.getElementById('folder-list');
    // Kiểm tra active cho mục "Tất cả"
    const allActive = (currentSelectedFolder === null) ? 'active' : '';

    container.innerHTML = `
        <div class="folder-item-wrapper ${allActive}" onclick="selectFolder(null)">
            <span class="content-zone"><i class="fa-solid fa-layer-group"></i> [Tất cả bài viết]</span>
        </div>
    `;
    renderTreeRecursive(treeData, container, 0);
}

function renderTreeRecursive(node, container, level) {
    Object.keys(node).forEach(key => {
        if (key.startsWith('_')) return;

        const fullPath = node[key]._path;
        const item = document.createElement('div');
        item.className = 'folder-group';

        const isActive = (currentSelectedFolder === fullPath) ? 'active' : '';

        item.innerHTML = `
            <div class="folder-item-wrapper ${isActive ? 'active' : ''}" style="margin-left: ${level * 12}px" data-path="${fullPath}">
                <div class="toggle-zone">
                    <i class="fa-solid fa-chevron-right"></i>
                </div>
                <div class="content-zone">
                    <i class="fa-regular fa-folder"></i>
                    <span>${key}</span>
                </div>
            </div>
            <div class="sub-folders" style="display: none;"></div>
        `;

        const toggleBtn = item.querySelector('.toggle-zone');
        const contentBtn = item.querySelector('.content-zone');
        const subContainer = item.querySelector('.sub-folders');
        const icon = toggleBtn.querySelector('i');

        // Khu vực 1: Nhấn mũi tên để xổ thư mục
        toggleBtn.onclick = (e) => {
            e.stopPropagation();
            const isExpanded = subContainer.style.display === 'block';
            subContainer.style.display = isExpanded ? 'none' : 'block';
            icon.style.transform = isExpanded ? 'rotate(0deg)' : 'rotate(90deg)';
        };

        // Khu vực 2: Nhấn vào tên để xem file
        contentBtn.onclick = (e) => {
            e.stopPropagation();
            selectFolder(fullPath);
        };

        container.appendChild(item);
        renderTreeRecursive(node[key], subContainer, level + 1);
    });
}

async function loadBlogs(page = 0) {
    try {
        let url = `/api/admin/blogs?page=${page}&size=10`;
        if (currentSelectedFolder) {
            url += `&folder=${encodeURIComponent(currentSelectedFolder)}`;
        }

        const response = await fetch(joinUrl(API_BASE_URL, url));
        const data = await response.json(); // data là đối tượng Page từ Spring

        const tbody = document.getElementById('blog-table-body');
        tbody.innerHTML = '';

        if (!data.content || data.content.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="5" style="text-align: center; padding: 30px; color: #888;">
                        <i class="fa-solid fa-folder-open" style="font-size: 2rem; display: block; margin: auto;"></i>
                        Thư mục này hiện đang rỗng.
                    </td>
                </tr>`;
            return;
        }

        data.content.forEach(blog => {
            const row = `
                <tr>
                    <td>${blog.id}</td>
                    <td style="text-align: left">${blog.title}</td>
                    <td>
                        <div class="form-check form-switch">
                            <input class="form-check-input" type="checkbox" 
                                ${blog.published ? 'checked' : ''} 
                                onchange="togglePublish(${blog.id}, this)">
                            <label class="form-check-label">${blog.published ? 'Công khai' : 'Đang ẩn'}</label>
                        </div>
                    </td>
                    <td>${blog.publishTime ? new Date(blog.publishTime).toLocaleDateString('vi-VN') : '---'}</td>
                    <td>
                        <button class="btn-edit" onclick="editBlog(${blog.id})"><i class="fa-solid fa-pen"></i></button>
                        <button class="btn-delete" onclick="deleteBlog(${blog.id}, '${blog.title}')"><i class="fa-solid fa-trash"></i></button>
                    </td>
                </tr>
            `;
            tbody.insertAdjacentHTML('beforeend', row);
        });

        // Bạn sẽ render thêm thanh phân trang dựa vào data.totalPages ở đây (làm sau cùng)
    } catch (error) {
        console.error("Lỗi load blogs:", error);
    }
}

function selectFolder(folderPath) {
    currentSelectedFolder = folderPath;
    updateFolderActiveUI(folderPath);

    const btnAdd = document.getElementById('btn-add-blog');
    const btnDelFolder = document.getElementById('btn-delete-folder');

    // Chỉ bật các nút này khi không phải là "[Tất cả bài viết]" (null)
    const isSelected = folderPath !== null;
    if (btnAdd) btnAdd.disabled = !isSelected;
    if (btnDelFolder) btnDelFolder.disabled = !isSelected;

    document.getElementById('table-folder-title').innerText = isSelected ? `Thư mục: ${folderPath}` : "Tất cả bài viết";

    loadBlogs(0);
}

// Hàm bổ trợ để xử lý active trực tiếp trên DOM
function updateFolderActiveUI(folderPath) {
    // Xóa active của tất cả các folder đang có
    document.querySelectorAll('.folder-item-wrapper').forEach(el => {
        el.classList.remove('active');
    });

    // Nếu folderPath là null, active mục "Tất cả bài viết" (thường là mục đầu tiên)
    if (folderPath === null) {
        const allItemsBtn = document.querySelector('.folder-item-wrapper[onclick*="selectFolder(null)"]');
        if (allItemsBtn) allItemsBtn.classList.add('active');
        return;
    }

    // Tìm đúng element có chứa path tương ứng
    // Lưu ý: Lúc renderTreeRecursive, bạn nên gán data-path cho element để dễ tìm
    const target = document.querySelector(`.folder-item-wrapper[data-path="${folderPath}"]`);
    if (target) {
        target.classList.add('active');
    }
}

function deleteBlog(blogId, blogTitle) {
    AdminApp.showModal({
        title: 'Xác nhận xóa',
        bodyHTML: `<p>Bạn có chắc chắn muốn xóa bài viết:</p>
                   <p>ID: <strong class="text-danger">${blogId}</strong></p>
                   <p>Tên bài viết: <strong class="text-danger">${blogTitle}</strong></p>
                   <p style="color: red; font-size: 0.9em;">(Lưu ý: Bài viết chỉ xoá mềm)</p>`,
        confirmText: 'Xóa ngay',
        onConfirm: async () => {
            try {
                const response = await fetch(joinUrl(API_BASE_URL, `/api/admin/blogs/${blogId}`), {
                    method: 'DELETE'
                });
                if (response.ok) {
                    loadBlogs(0); // Load lại danh sách sau khi xóa
                }
            } catch (error) {
                alert("Lỗi khi xóa bài viết");
            }
        }
    });
}

function createNewFolder() {
    // Chuẩn bị danh sách gợi ý từ folderDataCache (đã có sẵn ở cây thư mục)
    const folderPaths = getAllPathsFromCache(folderDataCache);
    const optionsHTML = folderPaths.map(path =>
        `<option value="${path}" ${path === currentSelectedFolder ? 'selected' : ''}>${path}</option>`
    ).join('');

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
            <input type="text" id="new-folder-name" class="form-control" 
                   placeholder="new folder name">
        </div>
    `;

    AdminApp.showModal({
        id: 'folder-modal',
        title: 'Thêm thư mục mới',
        bodyHTML: bodyHTML,
        confirmText: 'Tạo thư mục',
        onConfirm: async () => {
            const parent = document.getElementById('parent-folder-path').value;
            const name = document.getElementById('new-folder-name').value.trim();

            if (!name) return alert("Vui lòng nhập tên thư mục");

            // Xử lý nối đường dẫn tránh nhập sai
            const fullPath = parent ? `${parent}/${name}` : name;

            try {
                const response = await fetch(joinUrl(API_BASE_URL, `/api/admin/folders?path=${encodeURIComponent(fullPath)}`), {
                    method: 'POST'
                });
                if (response.ok) {
                    await loadFolders(true); // Load lại cây thư mục (force refresh)
                } else {
                    const msg = await response.text();
                    alert("Lỗi: " + msg);
                }
            } catch (error) {
                alert("Không thể kết nối đến máy chủ");
            }
        }
    });
}

// Hàm bổ trợ lấy tất cả đường dẫn từ Object Tree của bạn
function getAllPathsFromCache(node, paths = []) {
    if (!node) return paths;
    Object.keys(node).forEach(key => {
        if (!key.startsWith('_')) {
            paths.push(node[key]._path);
            getAllPathsFromCache(node[key], paths);
        }
    });
    return paths;
}

function confirmDeleteFolder() {
    if (!currentSelectedFolder) return;

    AdminApp.showModal({
        title: 'Xác nhận xóa thư mục',
        bodyHTML: `
            <p>Bạn có chắc chắn muốn xóa thư mục: <strong class="text-danger">${currentSelectedFolder}</strong>?</p>
            <p class="text-muted small">* Lưu ý: Chỉ có thể xóa thư mục hoàn toàn trống.</p>
        `,
        confirmText: 'Xác nhận xóa',
        onConfirm: async () => {
            try {
                const response = await fetch(joinUrl(API_BASE_URL, `/api/admin/folders?path=${encodeURIComponent(currentSelectedFolder)}`), {
                    method: 'DELETE'
                });

                if (response.ok) {
                    currentSelectedFolder = null; // Reset về gốc
                    await loadFolders(true); // Refresh cây thư mục
                } else {
                    const errorMsg = await response.text();
                    alert(errorMsg); // Hiển thị thông báo "Thư mục không trống" từ Backend
                }
            } catch (error) {
                alert("Lỗi kết nối khi xóa thư mục");
            }
        }
    });
}

// Hàm bổ trợ tạo slug
function initSlugAutoGenerate() {
    const titleInput = document.getElementById('blog-title');
    const slugInput = document.getElementById('blog-slug');

    titleInput.addEventListener('input', () => {
        const slug = titleInput.value
            .toLowerCase()
            .normalize("NFD").replace(/[\u0300-\u036f]/g, "")
            .replace(/[đÐ]/g, "d")
            .replace(/([^a-z0-9-\s])/g, '')
            .replace(/(\s+)/g, '-')
            .replace(/-+/g, '-')
            .trim();
        slugInput.value = slug;
    });
}

// Kiểm tra input của blog trước khi gửi
async function validateAndSaveBlog() {
    const data = {
        title: document.getElementById('blog-title').value.trim(),
        slug: document.getElementById('blog-slug').value.trim(),
        summary: document.getElementById('blog-summary').value.trim() || "Mô tả bài viết",
        thumbnail: document.getElementById('blog-thumb-path').value || "/images/blog-thumb-1.jpg",
        contentPath: currentSelectedFolder // Thư mục đã chọn ở sidebar trái
    };

    // Frontend Check
    if (!data.title) return alert("Tiêu đề không được để trống!");
    if (!/^[a-z0-9-]+$/.test(data.slug)) return alert("Slug không hợp lệ (chỉ dùng chữ thường, số và dấu gạch ngang)!");

    // Backend Check Slug trùng
    const isExisted = await fetch(joinUrl(API_BASE_URL, `/api/admin/blogs/check-slug?slug=${data.slug}`)).then(r => r.json());
    if (isExisted) return alert("Slug này đã tồn tại, vui lòng chọn slug khác!");

    // Nếu ổn thì gọi hàm Save (createNewBlog)
    saveBlogToServer(data);
}

// Hàm tạo blog
function openCreateBlogModal() {
    const html = `
        <div class="blog-editor-modal">
            <div class="row">
                <div class="col-md-4">
                    <div class="form-group mb-3">
                        <label class="fw-bold">Tiêu đề bài viết <span class="text-danger">*</span></label>
                        <input type="text" id="blog-title" class="form-control" placeholder="Nhập tiêu đề...">
                    </div>
                    <div class="form-group mb-3">
                        <label class="fw-bold">Slug (Tên file & URL) <span class="text-danger">*</span></label>
                        <input type="text" id="blog-slug" class="form-control input-highlight-yellow" placeholder="tieu-de-bai-viet">
                    </div>
                    <div class="form-group mb-3">
                        <label class="fw-bold">Ảnh đại diện (Thumbnail)</label>
                        <div class="d-flex gap-2">
                            <input type="text" id="blog-thumb-path" class="form-control" value="/images/blog-thumb-1.jpg" readonly>
                            <button class="btn btn-outline-primary" onclick="openImageExplorer(path => document.getElementById('blog-thumb-path').value = path)">
                                <i class="fa-solid fa-image"></i>
                            </button>
                        </div>
                    </div>
                    <div class="form-group mb-3">
                        <label class="fw-bold">Tóm tắt</label>
                        <textarea id="blog-summary" class="form-control" rows="4">Mô tả bài viết</textarea>
                    </div>
                    <div class="form-check form-switch">
                        <input class="form-check-input" type="checkbox" id="blog-featured">
                        <label class="form-check-label">Bài viết nổi bật</label>
                    </div>
                </div>

                <div class="col-md-8">
                    <label class="fw-bold">Nội dung bài viết</label>
                    <div id="quill-editor"></div>
                </div>
            </div>
        </div>
    `;

    AdminApp.showModal({
        id: 'blog-modal',
        title: 'Tạo bài viết mới',
        bodyHTML: html,
        width: '90%', // Modal rộng để dễ viết bài
        confirmText: 'Lưu bài viết',
        onConfirm: () => validateAndSaveBlog()
    });

    // Khởi tạo Quill và Auto-Slug
    initQuill();
    initSlugAutoGenerate();
}

// Text editor Quill
let quill; // Biến toàn cục để thao tác
function initQuill() {
    quill = new Quill('#quill-editor', {
        theme: 'snow',
        modules: {
            toolbar: {
                container: [
                    [{ 'header': [1, 2, 3, false] }],
                    ['bold', 'italic', 'underline', 'strike'],
                    ['link', 'image', 'blockquote', 'code-block'],
                    [{ 'list': 'ordered' }, { 'list': 'bullet' }],
                    ['clean']
                ],
                handlers: {
                    image: function () {
                        openImageExplorer((selectedPath) => {
                            const range = quill.getSelection();
                            const fullUrl = joinUrl(API_BASE_URL, selectedPath);
                            // Chèn ảnh vào editor với đường dẫn chuẩn /images/...
                            quill.insertEmbed(range.index, 'image', fullUrl);
                        });
                    }
                }
            }
        }
    });
}

// Biến lưu trữ riêng cho Image Explorer để tránh xung đột với folderCache của Blog
let imageFolderCache = null;

// Hàm khởi tạo Image Explorer
async function openImageExplorer(onSelectCallback) {
    const html = `
        <div class="image-explorer-container">
            <aside id="exp-nav" class="explorer-nav">
                <button onclick="toggleExpNav()" class="btn-sm mb-2"><i class="fa-solid fa-bars"></i></button>
                <div id="image-folder-list">Loading...</div>
            </aside>
            <section id="exp-files" class="explorer-main">
                <p class="text-muted">Chọn một thư mục để xem ảnh</p>
            </section>
            <aside id="exp-preview" class="explorer-preview">
                <div id="preview-box">
                    <i class="fa-regular fa-image fa-4x text-muted"></i>
                    <p>Xem trước ảnh</p>
                </div>
            </aside>
        </div>
    `;

    AdminApp.showModal({
        id: 'image-explorer-modal',
        title: 'Thư viện hình ảnh',
        bodyHTML: html,
        width: '95%',
        confirmText: 'Chèn ảnh này',
        onConfirm: () => {
            const selected = document.querySelector('.img-item-card.selected');
            if (selected) {
                onSelectCallback(selected.dataset.path);
            }
        }
    });

    await loadImageFolders();
}

function toggleExpNav() {
    document.getElementById('exp-nav').classList.toggle('collapsed');
}

// Lấy danh sách folder ảnh và build tree
async function loadImageFolders(forceRefresh = false) {
    // Nếu đã có cache và không yêu cầu refresh thì không gọi API nữa
    if (imageFolderCache && !forceRefresh) {
        renderImageTreeUI(imageFolderCache);
        return;
    }

    try {
        const response = await fetch(joinUrl(API_BASE_URL, '/api/admin/folders/images/tree'));
        const paths = await response.json();
        imageFolderCache = buildTree(paths); // Lưu vào cache
        renderImageTreeUI(imageFolderCache);
    } catch (e) {
        console.error("Lỗi tải thư mục:", e);
    }
}

function renderImageTreeUI(treeData) {
    const container = document.getElementById('image-folder-list');
    // Kiểm tra active cho mục "Tất cả"
    const allActive = (currentSelectedFolder === null) ? 'active' : '';

    container.innerHTML = `
        <div class="folder-item-wrapper ${allActive}" onclick="selectFolder(null)">
            <span class="content-zone"><i class="fa-solid fa-layer-group"></i> [Thư mục gốc]</span>
        </div>
    `;
    renderImageTreeRecursive(treeData, container, 0);
}

// Hàm render cây thư mục ảnh (Dùng lại logic CSS của blog)
function renderImageTreeRecursive(node, container, level) {
    Object.keys(node).forEach(key => {
        if (key.startsWith('_')) return;

        const fullPath = node[key]._path;
        const item = document.createElement('div');
        item.className = 'folder-group';

        const isActive = (currentSelectedFolder === fullPath) ? 'active' : '';

        item.innerHTML = `
            <div class="folder-item-wrapper ${isActive ? 'active' : ''}" style="margin-left: ${level * 12}px" data-path="${fullPath}">
                <div class="toggle-zone">
                    <i class="fa-solid fa-chevron-right"></i>
                </div>
                <div class="content-zone">
                    <i class="fa-regular fa-folder"></i>
                    <span>${key}</span>
                </div>
            </div>
            <div class="sub-folders" style="display: none;"></div>
        `;

        const toggleBtn = item.querySelector('.toggle-zone');
        const contentBtn = item.querySelector('.content-zone');
        const subContainer = item.querySelector('.sub-folders');
        const icon = toggleBtn.querySelector('i');

        // Khu vực 1: Nhấn mũi tên để xổ thư mục
        toggleBtn.onclick = (e) => {
            e.stopPropagation();
            const isExpanded = subContainer.style.display === 'block';
            subContainer.style.display = isExpanded ? 'none' : 'block';
            icon.style.transform = isExpanded ? 'rotate(0deg)' : 'rotate(90deg)';
        };

        // Khu vực 2: Nhấn vào tên để xem file
        contentBtn.onclick = (e) => {
            e.stopPropagation();
            // Gọi API lấy file trong folder này
            loadImagesFromFolder(key); // key ở đây đóng vai trò path/tên folder
        };

        container.appendChild(item);
        renderImageTreeRecursive(node[key], subContainer, level + 1);
    });
}
function renderImageTreeRecursiveOld(node, container, level = 0) {
    Object.keys(node).forEach(key => {
        if (key.startsWith('_')) return;

        const item = node[key];
        const hasChildren = Object.keys(item).some(k => !k.startsWith('_'));
        const folderPath = item._path;

        const itemDiv = document.createElement('div');
        itemDiv.className = `folder-item-wrapper`;
        itemDiv.innerHTML = `
            <div class="folder-item" data-path="${folderPath}" style="padding-left: ${level * 12 + 10}px">
                <i class="fa-solid ${hasChildren ? 'fa-chevron-right exp-icon' : 'fa-folder'}"></i>
                <i class="fa-solid fa-folder folder-icon"></i>
                <span class="folder-name">${key}</span>
            </div>
            <div class="folder-children" style="display: none;"></div>
        `;

        const header = itemDiv.querySelector('.folder-item');
        header.onclick = (e) => {
            e.stopPropagation();
            // Highlight
            document.querySelectorAll('#image-folder-list .folder-item').forEach(el => el.classList.remove('active'));
            header.classList.add('active');

            // Toggle mở rộng cây
            const childrenCont = itemDiv.querySelector('.folder-children');
            if (hasChildren) {
                const icon = header.querySelector('.exp-icon');
                const isExpanded = childrenCont.style.display === 'block';
                childrenCont.style.display = isExpanded ? 'none' : 'block';
                icon.classList.toggle('fa-chevron-down', !isExpanded);
                icon.classList.toggle('fa-chevron-right', isExpanded);
            }

            // Gọi API lấy file trong folder này
            loadImagesFromFolder(key); // key ở đây đóng vai trò path/tên folder
        };

        container.appendChild(itemDiv);
        if (hasChildren) {
            renderImageTreeRecursive(item, itemDiv.querySelector('.folder-children'), level + 1);
        }
    });
}

// Hàm load file ảnh từ Backend
async function loadImagesFromFolder(path) {
    const files = await fetch(joinUrl(API_BASE_URL, `/api/admin/folders/images/files?path=${encodeURIComponent(path)}`)).then(r => r.json());
    const mainArea = document.getElementById('exp-files');
    
    mainArea.innerHTML = files.map(fileName => {
        // Đường dẫn chuẩn theo yêu cầu Backend: images/<path>/<file>
        const filePath = `images/${path}/${fileName}`.replace(/\/+/g, '/');
        const fullUrl = joinUrl(API_BASE_URL, filePath);
        
        return `
            <div class="img-item-card" data-path="/${filePath}" onclick="previewImage('/${filePath}', this)">
                <img src="${fullUrl}" loading="lazy" onerror="this.src='/images/image-error.png'">
                <div class="img-name text-truncate small">${fileName}</div>
            </div>
        `;
    }).join('');
}

function previewImage(path, element) {
    // 1. Highlight item
    document.querySelectorAll('.img-item-card').forEach(el => el.classList.remove('selected'));
    element.classList.add('selected');

    // 2. Render preview
    const fullUrl = joinUrl(API_BASE_URL, path);
    const previewBox = document.getElementById('exp-preview');
    
    previewBox.innerHTML = `
        <div class="preview-sticky">
            <img src="${fullUrl}" class="img-fluid rounded border" style="max-width: 100%; height: auto;">
            <div class="mt-3 p-2 bg-light small text-break">
                <strong>Đường dẫn:</strong><br>${path}
            </div>
            <button class="btn-create w-100 mt-2" onclick="openUploadOverlay()">
                <i class="fa-solid fa-plus"></i> Upload ảnh mới
            </button>
        </div>
    `;
}
function openUploadOverlay() {
    // Tạm thời làm chức năng rỗng như yêu cầu
    alert("Chức năng Upload sẽ được phát triển sau khi hoàn thiện phần Blog.");
}

// Đăng ký thay đổi phần tử blogs của đối tượng ModuleRegistry
registerModule('blogs', renderBlogModule);