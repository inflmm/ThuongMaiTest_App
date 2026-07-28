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
                            <button class="btn-blog btn-create" onclick="openImageExplorer(path => document.getElementById('blog-thumb-path').value = path)">
                                <i class="fa-solid fa-image"></i> Thư viện ảnh
                            </button>
                            <button id="btn-delete-folder" class="btn-blog btn-delete-folder" disabled onclick="confirmDeleteFolder()">
                                <i class="fa-solid fa-folder-minus"></i> Xóa thư mục
                            </button>
                            <button id="btn-add-blog" class="btn-blog btn-create" disabled onclick="openCreateBlogModal()">
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
                    <div class="table-footer">
                        <div id="blog-pagination" class="pagination-container"></div>
                    </div>
                </div>
            </section>
        </div>
    `;

    // Thêm thanh search vào toolbox
    document.getElementById('header-toolbox').innerHTML = `
        <div class="search-box">
            <button class="search-config-toolbox" onclick="openBlogAdvancedSearch()"><i class="fa-solid fa-sliders"></i>Tìm kiếm chi tiết</button> 
            <div class="search-bar-toolbox">
                <input class="search-input-toolbox" type="text" id="blog-search-input" onkeypress="if(event.key==='Enter') handleBlogSearch()" placeholder="Tìm theo ID hoặc Tiêu đề">
                <button class="search-btn-toolbox" onclick="handleBlogSearch()"><i class="fa-solid fa-magnifying-glass"></i></button>
            </div>
        </div>
    `;

    loadFolders(false, 'articles'); // Gọi API lấy danh sách folder
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
        await loadBlogs();
    }
}

let folderDataCache = {
    articles: null,
    images: null
}; // Biến lưu trữ cache folder

async function loadFolders(forceRefresh = false, type = 'articles') {
    let apiUrl = '';
    let rootPath = '';

    // Gán thủ công vì backend không đồng nhất như bạn nói
    if (type === 'articles') {
        apiUrl = '/api/admin/folders';
        rootPath = 'articles';
    } else if (type === 'images') {
        apiUrl = '/api/admin/folders/images/tree';
        rootPath = 'images';
    }

    // Nếu đã có cache và không yêu cầu refresh thì không gọi API nữa
    if (folderDataCache[type] && !forceRefresh) {
        renderTreeUI(folderDataCache[type], type);
        return;
    }

    try {
        const response = await fetch(joinUrl(API_BASE_URL, apiUrl));
        const paths = await response.json();
        folderDataCache[type] = buildTree(paths); // Lưu vào cache
        renderTreeUI(folderDataCache[type], type);
    } catch (e) {
        console.error("Lỗi tải thư mục:", e);
    }
}

function buildTree(paths) {
    const result = {};
    paths.forEach(path => {
        if (path === '') {
            //bỏ qua thư mục gốc
        } else {
            const parts = path.split('/');
            let current = result;
            parts.forEach(part => {
                if (!current[part]) current[part] = { _isFolder: true, _path: path };
                current = current[part];
            });
        }
    });
    return result;
}

function renderTreeUI(treeData, type) {
    // Xác định container dựa trên module đang mở
    const containerId = (type === 'articles') ? 'folder-list' : 'image-folder-list';
    const container = document.getElementById(containerId);
    if (!container) return;

    // Kiểm tra active cho mục "Tất cả"
    const allActive = (currentSelectedFolder === null) ? 'active' : '';
    if (type === 'articles') {
        container.innerHTML = `
        <div class="folder-item-wrapper ${allActive}" onclick="selectFolder(null)">
            <span class="content-zone"><i class="fa-solid fa-layer-group"></i> [Tất cả bài viết]</span>
        </div>
    `;
    } else if (type === 'images') {
        container.innerHTML = `
        <div class="folder-item-wrapper ${allActive}" onclick="loadImagesInFolder('')">
            <span class="content-zone"><i class="fa-solid fa-layer-group"></i> [Thư mục gốc]</span>
        </div>
    `;
    }


    renderTreeRecursive(treeData, container, 0, type);
}

function renderTreeRecursive(node, container, level, type) {
    Object.keys(node).forEach(key => {
        if (key.startsWith('_')) return;

        const nodeData = node[key];
        const fullPath = nodeData._path;
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

        // Logic Toggle (Chỉ xoay icon và hiện con)
        toggleBtn.onclick = (e) => {
            e.stopPropagation();
            const isExpanded = subContainer.style.display === 'block';
            subContainer.style.display = isExpanded ? 'none' : 'block';
            toggleBtn.style.transform = isExpanded ? 'rotate(0deg)' : 'rotate(90deg)';
        };

        // Logic Select (Nhấn vào tên thư mục)
        contentBtn.onclick = (e) => {
            e.stopPropagation();

            // Cập nhật Highlight UI (áp dụng cho cả 2 loại)
            const listId = (type === 'articles') ? '#folder-list' : '#image-folder-list';
            document.querySelectorAll(`${listId} .folder-info, ${listId} .folder-item`).forEach(el => el.classList.remove('active'));
            contentBtn.classList.add('active');

            // Phân luồng xử lý dữ liệu
            if (type === 'articles') {
                selectFolder(fullPath); // Logic của Blog (truyền ID/Name)
            } else if (type === 'images') {
                loadImagesInFolder(fullPath); // Logic của Ảnh (truyền /images/...)
            }
        };

        container.appendChild(item);
        renderTreeRecursive(node[key], subContainer, level + 1, type);
    });
}

async function loadBlogs(page = 0) {
    try {
        AdminApp.showLoading(true);
        // Url lấy blog mặc định
        let url = `/api/admin/blogs?page=${page}&size=10`;
        if (currentSelectedFolder) {
            // Url lấy từ thư mục
            url += `&folder=${encodeURIComponent(currentSelectedFolder)}` + '/'; // Trong db lưu contentpath có dấu / ở cuối
        }

        const response = await fetch(url.toString());
        const data = await response.json();

        // 1. Vẽ bảng dữ liệu
        renderBlogTable(data.content);

        // 2. Vẽ thanh phân trang và thông tin bổ sung
        renderPagination(data);

    } catch (error) {
        console.error("Lỗi load blogs:", error);
    } finally {
        AdminApp.showLoading(false);
    }
}

function renderBlogTable(content, containerId = 'blog-table-body') {
    const tbody = document.getElementById(containerId);
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

    content.forEach(blog => {
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
                        <button class="btn-edit" onclick="openBlogModal(${blog.id})"><i class="fa-solid fa-pen"></i></button>
                        <button class="btn-delete" onclick="deleteBlog(${blog.id}, '${blog.title}')"><i class="fa-solid fa-trash"></i></button>
                    </td>
                </tr>
            `;
        tbody.insertAdjacentHTML('beforeend', row);
    });
}

function renderPagination(data, type = 'normal') {
    // Hiện tại có 3 trạng thái: normal, search, advanced
    const containerId = type === 'advanced' ? 'adv-search-pagination' : 'blog-pagination'; // nếu là advanced lấy adv-search-pagination
    const container = document.getElementById(containerId);

    const loadFunc = (type === 'advanced') ? 'executeAdvancedSearch' : 
                 (type === 'search') ? 'handleBlogSearch' : 'loadBlogs';
    
    if (!container) return;

    const { page, _links } = data;
    if (!page) return;

    // Tính toán vị trí hiển thị (ví dụ: Bản ghi 1-10 trên tổng số 45)
    const startIdx = page.number * page.size + 1;
    const endIdx = Math.min(startIdx + page.size - 1, page.totalElements);

    let html = `
        <div class="pagination-info">
            Hiển thị <b>${startIdx}-${endIdx}</b> trên tổng số <b>${page.totalElements}</b> bài viết 
            (Trang ${page.number + 1}/${page.totalPages})
        </div>
        <div class="pagination-controls">
    `;

    // Nút Về đầu (First)
    html += `
        <button class="btn-page" onclick="${loadFunc}(0)" ${page.number === 0 ? 'disabled' : ''}>
            <i class="fa-solid fa-angles-left"></i>
        </button>
    `;

    // Nút Trang trước (Sử dụng logic từ page.number)
    html += `
        <button class="btn-page" onclick="${loadFunc}(${page.number - 1})" ${page.number === 0 ? 'disabled' : ''}>
            <i class="fa-solid fa-angle-left"></i>
        </button>
    `;

    // Các nút số trang (Hiển thị thông minh, ví dụ chỉ hiện 5 trang gần nhất)
    let startPage = Math.max(0, page.number - 2);
    let endPage = Math.min(page.totalPages - 1, startPage + 4);
    if (endPage - startPage < 4) startPage = Math.max(0, endPage - 4);

    for (let i = startPage; i <= endPage; i++) {
        html += `
            <button class="btn-page ${i === page.number ? 'active' : ''}" onclick="${loadFunc}(${i})">
                ${i + 1}
            </button>
        `;
    }

    // Nút Trang kế tiếp
    html += `
        <button class="btn-page" onclick="${loadFunc}(${page.number + 1})" ${page.number >= page.totalPages - 1 ? 'disabled' : ''}>
            <i class="fa-solid fa-angle-right"></i>
        </button>
    `;

    // Nút Về cuối (Last)
    html += `
        <button class="btn-page" onclick="${loadFunc}(${page.totalPages - 1})" ${page.number >= page.totalPages - 1 ? 'disabled' : ''}>
            <i class="fa-solid fa-angles-right"></i>
        </button>
    `;

    html += `</div>`;
    container.innerHTML = html;
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
    const folderPaths = getAllPathsFromCache(folderDataCache['articles']);
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
/**
 * @param {Object} node - Nhánh cache cụ thể (folderDataCache.articles hoặc .images)
 * @param {string} rootPath - Tiền tố muốn lọc (ví dụ: 'articles' hoặc 'images')
 */
function getAllPathsFromCache(node, rootPath = '') {
    // Nếu nhánh cache chưa được khởi tạo (null hoặc undefined), trả về mảng rỗng thay vì báo lỗi
    if (!node || typeof node !== 'object') return [];

    let paths = [];

    Object.keys(node).forEach(key => {
        // Bỏ qua các metadata bắt đầu bằng dấu gạch dưới
        if (key.startsWith('_')) return;

        const item = node[key];

        // Kiểm tra nếu node này có _path và path đó bắt đầu bằng rootPath
        if (item._path && (rootPath === '' || item._path.startsWith(rootPath))) {
            paths.push(item._path);
        }

        // Đệ quy xuống các thư mục con
        const subPaths = getAllPathsFromCache(item, rootPath);
        paths = paths.concat(subPaths);
    });

    // Loại bỏ trùng lặp nếu có và sắp xếp cho đẹp
    return [...new Set(paths)].sort();
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
async function validateAndSaveBlog(blogId) {
    const title = document.getElementById('blog-title').value.trim();
    const slug = document.getElementById('blog-slug').value.trim();
    let thumbnail = document.getElementById('blog-thumb-path').value;
    const folder = document.getElementById('blog-folder-select').value; // Ví dụ: "articles/news"
    const summary = document.getElementById('blog-summary').value.trim();
    const content = quill.root.innerHTML;
    const publishTime = document.getElementById('blog-publish-time').value;

    // 1. Kiểm tra các trường bắt buộc
    if (!title || !slug || content === '<p><br></p>') {
        alert("Vui lòng điền Tiêu đề, Slug và Nội dung!");
        return false;
    }

    // 2. Chuẩn hóa Thumbnail (Phải bắt đầu bằng /images/)
    if (thumbnail) {
        // Nếu path chưa có /images ở đầu thì thêm vào
        if (!thumbnail.startsWith('/images/')) {
            // Loại bỏ dấu / ở đầu nếu có để tránh double slash //images
            const cleanPath = thumbnail.startsWith('/') ? thumbnail.substring(1) : thumbnail;
            thumbnail = '/images/' + cleanPath;
        }
    } else {
        thumbnail = "/images/blog-thumb-1.jpg"; // Giá trị mặc định
    }

    // 3. Chuẩn hóa contentPath (bắt đầu không có dấu /)
    let contentPath = folder ? folder.trim() : null;
    if (contentPath && contentPath.startsWith('/')) {
        contentPath = contentPath.substring(1);
    }
    if (contentPath && !contentPath.endsWith('/')) {
        contentPath += '/'; // Đảm bảo kết thúc bằng dấu /
    }

    // 4. Tạo Object Blog (isPublished và isFeatured mặc định là false)
    const blogData = {
        id: blogId, // Nếu là null thì POST (Create), nếu có giá trị thì PUT (Update)
        title: title,
        slug: slug,
        thumbnail: thumbnail || "/images/blog-thumb-1.jpg",
        contentPath: contentPath, // Ví dụ: "articles/news"
        publishTime: publishTime ? new Date(publishTime).toISOString() : null,
        summary: summary,
        isPublished: blogId ? undefined : false, // Tạo mới mặc định false
        isFeatured: blogId ? undefined : false
    };

    return await saveBlogToServer(blogData, content, blogId ? 'PUT' : 'POST');
}

async function saveBlogToServer(blogData, content, method) {
    try {
        AdminApp.showLoading(true);

        // API của bạn dùng @RequestParam cho content, nên ta nối vào URL
        const url = new URL(joinUrl(API_BASE_URL, '/api/admin/blogs'));
        url.searchParams.append('content', content);

        const response = await fetch(url.toString(), {
            method: method,
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(blogData)
        });

        if (response.ok) {
            alert(method === 'POST' ? "Tạo thành công!" : "Cập nhật thành công!");
            // Gọi hàm render lại danh sách bài viết nếu có
            loadBlogs();
            return true; // Để Modal tự đóng
        } else {
            const errorText = await response.text();
            alert("Lỗi server: " + errorText);
            return false;
        }
    } catch (error) {
        console.error("Fetch Error:", error);
        alert("Không thể kết nối đến server.");
        return false;
    } finally {
        AdminApp.showLoading(false);
    }
}

// Hàm tạo blog
function openCreateBlogModal() {
    // 1. Lấy danh sách thư mục từ ngăn 'articles'
    const allFolderPaths = getAllPathsFromCache(folderDataCache['articles']);

    // 2. Tạo HTML options, ưu tiên chọn folder hiện tại đang xem
    const options = allFolderPaths.map(path => {
        const isSelected = (path === currentSelectedFolder) ? 'selected' : '';
        return `<option value="${path}" ${isSelected}>${path}</option>`;
    }).join('');

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
                        <select id="blog-folder-select">
                            ${options}
                        </select>
                    </div>
                    <div class="blog-form-group flex-2">
                        <label>Ảnh đại diện (Thumbnail)</label>
                        <div class="blog-input-group">
                            <input type="text" id="blog-thumb-path" readonly placeholder="Chọn ảnh từ thư viện...">
                            <button type="button" onclick="openImageExplorer(setBlogThumbnail)">
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
            </form>
    `;

    // Toolbox dành riêng cho trình soạn thảo (ví dụ nút hỗ trợ SEO hoặc định dạng nhanh)
    const toolbox = `
        <button class="btn-sm btn-outline-secondary" onclick="alert('HD soạn thảo')">
            <i class="fa-solid fa-circle-question"></i> Hướng dẫn
        </button>
    `;

    AdminApp.showModal({
        id: 'blog-modal',
        title: 'Tạo bài viết mới',
        bodyHTML: html,
        toolboxHTML: toolbox, // Gắn vào nhóm trống bên phải tiêu đề
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
// Biến toàn cục hoặc biến trong module để giữ vị trí con trỏ
let lastQuillRange = null;
// 2. Định nghĩa hàm đăng ký Format (Để tránh lỗi Quill undefined)
function registerQuillFormats() {
    // Kiểm tra xem class Quill của thư viện đã sẵn sàng chưa
    if (typeof Quill === 'undefined'){
        //console.log('Quill is undefined');
        return;
    } 

    const QuillImage = Quill.import('formats/image');
    class BlogImage extends QuillImage {
        static create(value) {
            let node = super.create(value);
            node.setAttribute('class', 'blog-image');
            return node;
        }
    }
    Quill.register(BlogImage, true);
}

function addQuillTooltips() {
    const tooltipMap = {
        'header': 'Tiêu đề',
        'font': 'Kiểu chữ',
        'size': 'Kích cỡ chữ',
        'bold': 'Chữ đậm (Ctrl+B)',
        'italic': 'Chữ nghiêng (Ctrl+I)',
        'underline': 'Gạch chân (Ctrl+U)',
        'strike': 'Gạch ngang',
        'link': 'Chèn liên kết',
        'image': 'Chèn ảnh',
        'video': 'Chèn Video',
        'blockquote': 'Trích dẫn',
        'code-block': 'Khối mã code',
        'list[value="ordered"]': 'Danh sách số',
        'list[value="bullet"]': 'Danh sách chấm',
        'clean': 'Xóa định dạng',
        'align': 'Căn lề',
        'color': 'Màu chữ',
        'background': 'Màu nền'
    };

    const toolbar = document.querySelector('.ql-toolbar');
    if (!toolbar) return;

    for (let selector in tooltipMap) {
        // Tìm button hoặc picker (dropdown)
        const el = toolbar.querySelector(`.ql-${selector}`);
        if (el) {
            el.setAttribute('title', tooltipMap[selector]);
        }
    }
}

function initQuill() {
    registerQuillFormats();

    quill = new Quill('#quill-editor', {
        theme: 'snow',
        modules: {
            toolbar: {
                container: [
                    [{ 'header': [2, 3, 4, false] }],
                    [{ 'font': [] }, { 'size': ['small', false, 'large', 'huge'] }],
                    ['bold', 'italic', 'underline', 'strike'],
                    [{ 'color': [] }, { 'background': [] }],
                    [{ 'script': 'sub' }, { 'script': 'super' }],
                    [{ 'align': [] }, { 'indent': '-1' }, { 'indent': '+1' }],
                    ['link', 'image', 'video', 'blockquote', 'code-block'],
                    [{ 'list': 'ordered' }, { 'list': 'bullet' }],
                    ['clean'] // Nút xóa định dạng
                ],
                handlers: {
                    image: function () {
                        // Lưu lại vị trí con trỏ hiện tại trước khi mất focus
                        lastQuillRange = quill.getSelection();

                        // Mở thư viện ảnh và truyền callback xử lý chèn vào editor
                        openImageExplorer(path => insertImageToEditor(path));
                    }
                }
            }
        }
    });

    // 3. Thêm Tooltip (Hiện tên chức năng khi rê chuột)
    addQuillTooltips();
}

function insertImageToEditor(path) {
    // Nếu lastQuillRange là null (người dùng chưa click vào editor đã chọn ảnh)
    // thì mặc định chèn vào cuối văn bản
    const range = lastQuillRange || { index: quill.getLength() };

    // Chuẩn hóa đường dẫn đảm bảo có /images/
    const fullUrl = '/' + path; // Về sau nếu có dùng server để host ảnh thì cần kết hợp thêm đường dẫn + path

    // Chèn ảnh vào đúng vị trí đã lưu
    quill.insertEmbed(range.index, 'image', fullUrl);

    // Di chuyển con trỏ xuống sau ảnh vừa chèn
    quill.setSelection(range.index + 1);
}

// Hàm khởi tạo Image Explorer
async function openImageExplorer(onSelectCallback) {
    const html = `
        <div class="image-explorer-container">
            <aside id="exp-nav" class="explorer-nav">
                <button onclick="toggleExpNav()" class="btn-sm mb-2"><i class="fa-solid fa-bars"></i></button>
                <div class="folder-header">
                    <div class="folder-header-title">Thư mục ảnh</div>
                </div>
                <div id="image-folder-list">
                    <div class="folder-item active" onclick="selectFolder(null)">[Tất cả bài viết]</div>
                </div>
            </aside>
            <section id="exp-files" class="explorer-main">
                <p class="text-muted">Chọn một thư mục để xem ảnh</p>
            </section>
            <aside id="exp-preview" class="explorer-preview">
                <div class="exp-preview-toolbox">
                    <button onclick="togglePreviewBox()" class="btn-explorer-preview btn-sm mb-2"><i class="fa-solid fa-bars"></i></button>
                </div>
                <div id="preview-box">
                    <i class="fa-regular fa-image fa-4x text-muted"></i>
                    <p>Xem trước ảnh</p>
                </div>
            </aside>
        </div>
    `;

    const subHeader = `
        <div id="image-explorer-path-display" class="path-display">
            <div>
                <span>Đường dẫn: </span>
                <span id="image-explorer-link">/images</span>
            </div> 
        </div>
    `;

    const toolbox = `
        <button class="btn-create w-100 mt-2" onclick="openUploadOverlay()">
                <i class="fa-solid fa-plus"></i> Upload ảnh mới
        </button>
    `;

    AdminApp.showModal({
        id: 'image-explorer-modal',
        title: 'Thư viện hình ảnh',
        bodyHTML: html,
        subHeader: subHeader,
        toolboxHTML: toolbox,
        width: '95%',
        confirmText: 'Chèn ảnh này',
        onConfirm: () => {
            // Lấy đường dẫn từ chính cái ID bạn yêu cầu
            const pathDisplay = document.getElementById('image-explorer-link');
            let finalPath = pathDisplay ? pathDisplay.innerText : '';

            // Chuẩn hóa: biến "images / folder / file.jpg" thành "images/folder/file.jpg"
            finalPath = finalPath.replace(/\s\/\s/g, '/').trim();

            if (finalPath && finalPath !== 'images' && onSelectCallback) {
                onSelectCallback(finalPath);
                return true; // Đóng modal
            } else {
                alert("Vui lòng chọn một file ảnh cụ thể!");
                return false; // Không cho đóng modal
            }
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

// Hàm load file ảnh từ Backend
async function loadImagesInFolder(path) {
    currentSelectedFolder = path;
    updateFolderActiveUI(path);

    const mainArea = document.getElementById('exp-files');
    mainArea.innerHTML = '<div class="p-3">Đang tải...</div>';

    try {
        // Đảm bảo path truyền vào có dạng 'images/thu-muc-con'
        const url = joinUrl(API_BASE_URL, `/api/admin/folders/images/files?path=${encodeURIComponent(path)}`);
        const response = await fetch(url);
        const files = await response.json();

        updateImageExplorerPath(path);

        if (!files || files.length === 0) {
            mainArea.innerHTML = `
                    <div class="empty-image-folder-text">
                        <i class="fa-solid fa-folder-open" style="font-size: 2rem; display: block; margin: auto;"></i>
                        Thư mục này hiện đang rỗng.
                    </div>
                    `;
            return;
        }

        mainArea.innerHTML = files.map(file => {
            const fileName = file.name || file;
            // Đường dẫn ảnh để hiển thị: /images/blog/photo.jpg
            const imagePath = `/${path}/${fileName}`.replace(/\/+/g, '/');
            const fullUrl = joinUrl(API_BASE_URL, 'images' + imagePath);

            return `
                <div class="img-item-card" onclick="previewImage('${imagePath}', this)">
                    <img class="exp-image-item" src="${fullUrl}">
                    <div class="exp-image-text">${fileName}</div>
                </div>
            `;
        }).join('');
    } catch (error) {
        console.log(error);
        mainArea.innerHTML = '<div class="p-3 text-danger">Lỗi tải danh sách ảnh</div>';
    }
}

// Hàm bổ trợ đổi size file
function formatBytes(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024, sizes = ['B', 'KB', 'MB', 'GB'], i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

function previewImage(path, element) {
    // 1. Highlight item
    document.querySelectorAll('.img-item-card').forEach(el => el.classList.remove('selected'));
    element.classList.add('selected');

    // 2. Cập nhật đường dẫn lên Sub-header
    updateImageExplorerPath(path);

    // 3. Render preview
    const fullUrl = joinUrl(API_BASE_URL, 'images' + path);
    const previewBox = document.getElementById('preview-box');

    previewBox.innerHTML = `
        <div class="preview-sticky">
            <img src="${fullUrl}" class="img-fluid rounded border" style="max-width: 100%; height: auto;">
        </div>
    `;

}

function updateImageExplorerPath(path) {
    const pathDisplay = document.getElementById('image-explorer-link');

    if (pathDisplay) {
        // Làm sạch path để hiển thị: loại bỏ dấu gạch chéo dư thừa
        const cleanDisplay = path.startsWith('/') ? path.substring(1) : path;
        pathDisplay.innerText = 'images/' + cleanDisplay.split('/').join('/');
    } else {
        //console.warn("Không tìm thấy ID 'image-explorer-link' trong DOM!");
    }
}

// Hàm callback để nhận đường dẫn ảnh và hiển thị preview
function setBlogThumbnail(path) {
    const input = document.getElementById('blog-thumb-path');
    const previewWrap = document.getElementById('blog-thumb-preview-wrap');
    const previewImg = document.getElementById('blog-thumb-preview-img');

    if (input) input.value = '/' + path;
    if (previewImg) {
        previewImg.src = joinUrl(API_BASE_URL, path); // path đã có images/ từ image-explorer-link
        previewWrap.style.display = 'block';
    }
}

/**
 * MỞ MODAL TẠO HOẶC CẬP NHẬT BLOG
 * @param {Long} blogId - Nếu có ID là Update, nếu null là Create
 */
async function openBlogModal(blogId = null) {
    let blogData = {
        title: '',
        slug: '',
        thumbnail: '',
        contentPath: currentSelectedFolder || 'articles/', // Ưu tiên folder đang xem
        content: '',
        publishTime: '',
        createdAt: null,
        updatedAt: null
    };

    // 1. Nếu là UPDATE, fetch dữ liệu từ Server theo ID
    if (blogId) {
        try {
            AdminApp.showLoading(true);
            const response = await fetch(joinUrl(API_BASE_URL, `/api/admin/blogs/${blogId}`)); // API bạn đã chỉnh
            if (!response.ok) throw new Error("Không lấy được dữ liệu bài viết");
            blogData = await response.json();
            //console.log(blogData.updated_time);
        } catch (error) {
            alert(error.message);
            AdminApp.showLoading(false);
            return;
        } finally {
            AdminApp.showLoading(false);
        }
    }

    // 2. Chuẩn bị Options cho Select Folder
    const allFolderPaths = getAllPathsFromCache(folderDataCache['articles']);
    const folderOptions = allFolderPaths.map(path => {
        // Đảm bảo path có dấu / ở cuối để so sánh
        const cleanPath = path.endsWith('/') ? path : path + '/';
        const targetPath = blogData.contentPath?.endsWith('/') ? blogData.contentPath : blogData.contentPath + '/';
        const isSelected = (cleanPath === targetPath) ? 'selected' : '';
        return `<option value="${cleanPath}" ${isSelected}>${cleanPath}</option>`;
    }).join('');

    // 3. Format thời gian cho input datetime-local
    const formatDateTime = (dateStr) => {
        if (!dateStr) return "";
        const d = new Date(dateStr);
        return d.toISOString().slice(0, 16);
    };

    const bodyHtml = `
        <div class="blog-editor-form">
            <div class="blog-form-row">
                <div class="blog-form-group flex-2">
                    <label class="fw-bold">Tiêu đề bài viết</label>
                    <input type="text" id="blog-title" value="${blogData.title}" placeholder="Nhập tiêu đề..." oninput="if(!${blogId}) generateSlug()">
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
                        <button type="button" onclick="openImageExplorer(setBlogThumbnail)">
                            <i class="fa-solid fa-image"></i>
                        </button>
                    </div>
                    <div id="blog-thumb-preview-wrap" class="mb-3" style="${blogData.thumbnail ? '' : 'display: none;'}">
                        <label class="small text-muted">Preview Thumbnail:</label>
                        <div style="width: 120px; height: 80px; border: 1px solid #ddd; border-radius: 4px; overflow: hidden;">
                            <img id="blog-thumb-preview-img" src="${blogData.thumbnail ? joinUrl(API_BASE_URL, blogData.thumbnail) : ''}" style="width: 100%; height: 100%; object-fit: cover;">
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
                    ${blogId ? `
                        <label class="small text-muted">Thông tin hệ thống</label>
                        <div class="small p-2 border rounded bg-light">
                            Thời gian tạo: ${new Date(blogData.createdTime).toLocaleString()}<br>
                            Thời gian sửa gần nhất: ${new Date(blogData.updatedTime).toLocaleString()}
                        </div>
                    ` : ''}
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
        </div>
    `;

    AdminApp.showModal({
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
    
    // Nếu không nhập gì, load lại folder hiện tại như bình thường
    if (!keyword) {
        loadBlogs(0); 
        return;
    }

    try {
        AdminApp.showLoading(true);
        const params = new URLSearchParams();
        
        // Kiểm tra nếu keyword là số thì tìm theo ID, ngược lại tìm theo Title
        if (!isNaN(keyword)) {
            params.append('id', keyword);
        } else {
            params.append('title', keyword);
        }

        // CHỐT CHẶN: Chỉ tìm trong thư mục đang chọn
        if (currentSelectedFolder) {
            params.append('contentPath', currentSelectedFolder + '/'); // currentSelectedFolder bị thiếu / ở cuối thư mục
        }

        params.append('page', page);
        params.append('size', 10);

        const response = await fetch(joinUrl(API_BASE_URL, `/api/admin/blogs/search?${params.toString()}`));
        const data = await response.json();

        // Render kết quả ra bảng chính
        renderBlogTable(data.content);
        renderPagination(data, 'search');

        // Cập nhật tiêu đề bảng để người dùng biết đang xem kết quả tìm kiếm
        document.getElementById('table-folder-title').innerText = 
            `Kết quả tìm kiếm cho "${keyword}" trong ${currentSelectedFolder || 'Tất cả'}`;

    } catch (error) {
        console.error("Lỗi tìm kiếm nhanh:", error);
    } finally {
        AdminApp.showLoading(false);
    }
}

/**
 * Mở Modal tìm kiếm nâng cao
 */
async function openBlogAdvancedSearch() {
    const bodyHtml = `
        <div class="advanced-search-container">
            <div class="search-filter-grid">
                <div class="filter-group">
                    <label>ID Bài viết</label>
                    <input type="number" id="adv-search-id" placeholder="VD: 101">
                </div>
                <div class="filter-group">
                    <label>Tiêu đề / Slug</label>
                    <input type="text" id="adv-search-title" placeholder="Nhập từ khóa...">
                </div>
                <div class="filter-group">
                    <label>Từ ngày</label>
                    <input type="date" id="adv-search-from">
                </div>
                <div class="filter-group">
                    <label>Đến ngày</label>
                    <input type="date" id="adv-search-to">
                </div>
                <div class="filter-group full-width">
                    <label>Thẻ (Tags)</label>
                    <input type="text" id="adv-search-tags" placeholder="Hiện tại chưa tạo chức năng tags">
                </div>
            </div>

            <div class="search-action-row">
                <button class="btn-blog reset-field" onclick="resetAdvancedFields()">
                    Làm mới
                </button>
                <button class="btn-blog btn-create" onclick="executeAdvancedSearch()">
                    <i class="fa-solid fa-magnifying-glass"></i> Lọc kết quả
                </button>
            </div>

            <hr class="search-divider">

            <div class="search-results-wrapper">
                <div id="adv-search-results-info" class="small text-muted mb-2">
                    
                </div>
                <div class="adv-results-table-container">
                    <table class="admin-table mini-table">
                        <thead>
                            <tr>
                                <th width="60">ID</th>
                                <th>Tiêu đề</th>
                                <th width="120">Trạng thái</th>
                                <th width="120">Ngày đăng</th>
                                <th width="100">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody id="adv-search-results-body">
                            <tr><td colspan="5" class="text-center">Nhập thông tin và nhấn "Lọc kết quả" để xem dữ liệu.</td></tr>
                        </tbody>
                    </table>
                </div>
                
            </div>
            <div class="table-footer">
                <div id="adv-search-pagination" class="pagination-container mt-2"></div>
            </div>
        </div>
    `;

    AdminApp.showModal({
        id: 'blog-advanced-search-modal',
        title: 'Tìm kiếm bài viết chi tiết',
        bodyHTML: bodyHtml,
        confirmText: 'Đóng',
        hideFooter: true,
        onConfirm: () => true // Chỉ đóng modal
    });
}

/**
 * Hàm thực thi tìm kiếm nâng cao (Gọi API)
 */
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
        const response = await fetch(joinUrl(API_BASE_URL, `/api/admin/blogs/search?${params.toString()}`));
        const data = await response.json();

        // Tái sử dụng hàm render đã chỉnh sửa ở bước trước
        renderBlogTable(data.content, 'adv-search-results-body');
        renderPagination(data, 'advanced');
        
    } catch (error) {
        console.error("Lỗi search:", error);
    }
}

async function resetAdvancedFields() {
    const fields = ['adv-search-id', 'adv-search-title', 'adv-search-from', 'adv-search-to', 'adv-search-tags'];
    fields.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.value = '';
    });
    document.getElementById('adv-search-results-body').innerHTML = '<tr><td colspan="5" class="text-center">Chưa có dữ liệu</td></tr>';
    document.getElementById('adv-search-pagination').innerHTML = '';
    document.getElementById('adv-search-results-info').innerText = '';
}

// Biến toàn cục lưu trữ danh sách file đang sẵn sàng upload
let selectedUploadFiles = [];

function openUploadOverlay() {
    // 1. TỐI ƯU THEO Ý BẠN: Không ép gán lại biến gốc, chỉ tính toán chuỗi hiển thị UI
    const displayFolder = currentSelectedFolder ? currentSelectedFolder : "Thư mục gốc";

    // 2. DỰNG HTML THUẦN CSS (Sử dụng cấu trúc AdminApp.showModal dùng chung)
    AdminApp.showModal({
        id: 'upload-image-modal',
        title: '<i class="fa-solid fa-cloud-arrow-up"></i> Tải ảnh mới lên hệ thống',
        bodyHTML: `
            <div class="upload-overlay-wrapper">
                <div class="upload-form-group">
                    <label class="upload-label">Thư mục đích hiện tại:</label>
                    <div class="upload-folder-display">
                        <i class="fa-solid fa-folder"></i> <span>${displayFolder}</span>
                    </div>
                </div>

                <div class="upload-toggle-container">
                    <label class="upload-label-inline">Chế độ Upload:</label>
                    <div class="upload-switch-wrapper">
                        <label class="upload-switch">
                            <input type="checkbox" id="upload-mode-toggle" onchange="toggleUploadMode(this.checked)">
                            <span class="upload-slider"></span>
                        </label>
                        <span id="toggle-label" class="upload-toggle-text mode-raw">Ảnh thông thường (Giữ nguyên gốc)</span>
                    </div>
                </div>

                <div id="product-slug-wrapper" class="upload-form-group" style="display: none;">
                    <div class="upload-form-group">
                        <label class="upload-label">Thư mục lưu ảnh:</label>
                        <div class="upload-folder-display"> 
                            <i class="fa-solid fa-folder"></i> <span> ${displayFolder}/(slug sản phẩm)/(master/grande/compact)/ảnh upload </span>
                        </div>
                    </div>
                    
                    <div class="upload-form-group">
                        <label class="upload-label">Slug Sản phẩm<span class="upload-required">*</span>:</label>
                        <input type="text" id="upload-product-slug" class="upload-input-text" placeholder="Ví dụ: ban-phim-co-gvn">
                    </div>

                    <div class="filter-group" style="margin-top: 12px;">
                        <label style="font-weight: 600; font-size: 13px; color: #475569;">Định dạng ảnh xuất ra:</label>
                        <select id="upload-image-format-select" style="width: 100%; padding: 8px 12px; margin-top: 6px; border: 1px solid #ddd; border-radius: 4px; font-size: 13px; background-color: #fff; outline: none; cursor: pointer;">
                            <option value="webp">Định dạng WEBP (Google CLI cwebp - dung lượng nhẹ hơn thumbnailator)</option>
                            <option value="jpg">Định dạng JPG (Thumbnailator - mặc định)</option>
                        </select>
                        <div id="webp-note-helper" style="color: #d97706; font-size: 11px; margin-top: 6px; line-height: 1.4;">
                            <i class="fa-solid fa-triangle-exclamation"></i> <strong>Lưu ý:</strong> Chế độ WEBP yêu cầu máy chủ chạy API phải cài đặt sẵn bộ công cụ libwebp của Google và cấu hình biến môi trường Path.
                        </div>
                    </div>
                </div>

                <div class="upload-form-group">
                    <label class="upload-label">Chọn file từ máy tính:</label>
                    <div class="upload-file-zone">
                        <input type="file" id="modal-file-input" multiple accept="image/webp, image/jpeg, image/png" onchange="handleModalFileSelect(this)">
                        <label for="modal-file-input" class="upload-file-trigger">
                            <i class="fa-solid fa-images"></i> Bấm để chọn hoặc kéo thả nhiều ảnh vào đây...
                        </label>
                    </div>
                </div>

                <div class="upload-preview-wrapper">
                    <div id="upload-preview-list" class="upload-preview-list">
                        <span class="upload-empty-text" id="no-file-text">Chưa có tệp tin nào được chọn</span>
                    </div>
                </div>
            </div>
        `,
        confirmText: 'Bắt đầu Tải lên',
        onConfirm: async () => {
            await executeUploadFlow();
        }
    });

    // Khởi tạo lại mảng lưu file khi mở modal
    selectedUploadFiles = [];

    // Đặt đoạn này ngay sau khi Modal đã được chèn vào DOM (khi mở Modal)
    document.getElementById('upload-image-format-select')?.addEventListener('change', function(e) {
        const helper = document.getElementById('webp-note-helper');
        if (helper) {
            helper.style.display = (e.target.value === 'webp') ? 'block' : 'none';
        }
    });
}

// 3. ĐIỀU KHIỂN CHUYỂN CHẾ ĐỘ (TOGGLE MƯỢT MÀ BẰNG JS)
function toggleUploadMode(isProductMode) {
    const label = document.getElementById('toggle-label');
    const slugWrapper = document.getElementById('product-slug-wrapper');
    
    if (isProductMode) {
        label.innerText = "Ảnh Sản phẩm (Tự động resize 3 mức compact, grande và master)";
        label.className = "upload-toggle-text mode-product";
        slugWrapper.style.display = "block";
        
        // Tự động điền hộ slug nếu biến môi trường có sẵn dữ liệu sản phẩm
        if (typeof CURRENT_PRODUCT_SLUG !== 'undefined' && CURRENT_PRODUCT_SLUG) {
            document.getElementById('upload-product-slug').value = CURRENT_PRODUCT_SLUG;
        }
    } else {
        label.innerText = "Ảnh thông thường (Giữ nguyên gốc)";
        label.className = "upload-toggle-text mode-raw";
        slugWrapper.style.display = "none";
    }
}

// 4. KIỂM TRA ĐỊNH DẠNG/DUNG LƯỢNG VÀ TẠO ẢNH PREVIEW NHỎ
function handleModalFileSelect(input) {
    const previewList = document.getElementById('upload-preview-list');
    const noFileText = document.getElementById('no-file-text');
    const MAX_SIZE_MB = 5;

    if (!input.files || input.files.length === 0) return;
    if (noFileText) noFileText.remove();

    Array.from(input.files).forEach(file => {
        // Chặn file lớn hơn 5MB bảo vệ server
        if (file.size > MAX_SIZE_MB * 1024 * 1024) {
            alert(`File "${file.name}" dung lượng quá lớn! Vui lòng chọn file dưới ${MAX_SIZE_MB}MB.`);
            return; 
        }

        selectedUploadFiles.push(file);
        const objectUrl = URL.createObjectURL(file);
        const fileSizeInMB = (file.size / (1024 * 1024)).toFixed(2);

        // Render dòng item ảnh thuần HTML/CSS
        const itemRow = document.createElement('div');
        itemRow.className = "upload-preview-item";
        itemRow.id = `upload-item-${selectedUploadFiles.length - 1}`;

        itemRow.innerHTML = `
            <div class="upload-item-info">
                <img src="${objectUrl}" class="upload-item-thumb">
                <div class="upload-item-meta">
                    <span class="upload-item-name" title="${file.name}">${file.name}</span>
                    <span class="upload-item-size">${fileSizeInMB} MB</span>
                </div>
            </div>
            <button class="upload-item-remove-btn" onclick="removeSelectedFileFromUpload(${selectedUploadFiles.length - 1}, '${objectUrl}')" type="button">
                <i class="fa-solid fa-trash-can"></i>
            </button>
        `;
        previewList.appendChild(itemRow);
    });

    input.value = ''; // Giải phóng input
}

function removeSelectedFileFromUpload(index, objectUrl) {
    const row = document.getElementById(`upload-item-${index}`);
    if (row) row.remove();
    
    URL.revokeObjectURL(objectUrl); // Thu hồi bộ nhớ ảnh ảo
    selectedUploadFiles[index] = null; // Đánh dấu xóa

    const previewList = document.getElementById('upload-preview-list');
    if (previewList.children.length === 0) {
        previewList.innerHTML = `<span class="upload-empty-text" id="no-file-text">Chưa có tệp tin nào được chọn</span>`;
    }
}

// 5. ĐỒNG BỘ ĐẨY FORM DATA QUA API FETCH
async function executeUploadFlow() {
    const finalFiles = selectedUploadFiles.filter(f => f !== null);

    if (finalFiles.length === 0) {
        alert("Vui lòng chọn ít nhất một file ảnh!");
        return;
    }

    const isProductMode = document.getElementById('upload-mode-toggle').checked;
    const formData = new FormData();

    finalFiles.forEach(file => {
        formData.append("files", file);
    });

    // Truyền chính xác thư mục hiện tại mà không sợ bị biến đổi cấu trúc
    formData.append("folder", currentSelectedFolder ? currentSelectedFolder : "");
    //formData.append("folder", "PCGVN");
    
    let targetEndpoint = "";

    if (isProductMode) {
        const productSlugInput = document.getElementById('upload-product-slug');
        const slugValue = productSlugInput ? productSlugInput.value.trim() : "";

        if (!slugValue) {
            alert("Vui lòng điền mã Slug của sản phẩm!");
            productSlugInput.focus();
            return;
        }

        const formatValue = document.getElementById('upload-image-format-select')?.value || 'jpg';
        formData.append("format", formatValue);

        formData.append("productSlug", slugValue);
        targetEndpoint = "/api/admin/images/product-upload";
    } else {
        targetEndpoint = "/api/admin/images/raw-upload";
    }

    if (typeof AdminApp.showLoading === 'function') AdminApp.showLoading(true);

    try {
        const response = await fetch(joinUrl(API_BASE_URL, targetEndpoint), {
            method: "POST",
            body: formData
        });

        const result = await response.json();

        if (response.ok && result.success) {
            alert(result.message || "Tải tài nguyên lên server thành công!");
            
            // Tìm nút đóng modal dùng chung của bạn để hạ màn hình
            const closeBtn = document.querySelector('.modal-header .close') || document.querySelector('[data-dismiss="modal"]');
            if (closeBtn) closeBtn.click();

            // Refresh lại thư mục
            loadImagesInFolder(currentSelectedFolder);
        } else {
            alert("Lỗi hệ thống: " + (result.message || "Không thể thực thi."));
        }
    } catch (error) {
        console.error("Lỗi API kết nối:", error);
        alert("Mất kết nối tới máy chủ API Spring Boot.");
    } finally {
        if (typeof AdminApp.showLoading === 'function') AdminApp.showLoading(false);
    }
}

// Đăng ký thay đổi phần tử blogs của đối tượng ModuleRegistry
registerModule('blogs', renderBlogModule);