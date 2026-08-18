/**
 * admin-core.js (Tiếp theo) - Logic cho Module Blog
 */
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
            headers: csrfHeaders({ 'Content-Type': 'application/json' }),
            body: JSON.stringify(originalState)
        });

        if (!response.ok) {
            await handleApiError(response);
            checkboxElement.checked = !originalState; // Quay về trạng thái cũ
            return;
        }

        // Cập nhật label
        label.innerText = originalState ? "Công khai" : "Đang ẩn";
        console.log("Cập nhật trạng thái thành công");

    } catch (error) {
        // 2. Rollback nếu lỗi kết nối (không phải lỗi HTTP, đã xử lý ở trên)
        alert("Lỗi: Không thể kết nối đến máy chủ!");
        checkboxElement.checked = !originalState; // Quay về trạng thái cũ
    } finally {
        // 3. Mở khóa
        checkboxElement.disabled = false;
        await loadBlogs();
    }
}

async function loadBlogs(page = 0) {
    try {
        AdminApp.showLoading(true);
        // Url lấy blog mặc định
        let url = `/api/admin/blogs?page=${page}&size=10`;
        const folderParam = currentSelectedFolder ? currentSelectedFolder.replace(/^\/+|\/+$/g, '') : '';
        if (folderParam) {
            // Url lấy từ thư mục
            url += `&folder=${encodeURIComponent(folderParam)}`;
        }

        const response = await fetch(url.toString());
        if (!response.ok) {
            await handleApiError(response);
            return;
        }
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
                    method: 'DELETE',
                    headers: csrfHeaders()
                });
                if (!response.ok) {
                    await handleApiError(response);
                    return;
                }
                loadBlogs(0); // Load lại danh sách sau khi xóa
            } catch (error) {
                alert("Lỗi khi xóa bài viết");
            }
        }
    });
}

// Hàm bổ trợ tạo slug
function initSlugAutoGenerate(titleFieldId = 'blog-title', slugFieldId = 'blog-slug') {
    const titleInput = document.getElementById(titleFieldId);
    const slugInput = document.getElementById(slugFieldId);

    if (!titleInput || !slugInput) return;

    const generateSlug = (value) => value
        .toLowerCase()
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .replace(/[đÐ]/g, "d")
        .replace(/([^a-z0-9-\s])/g, '')
        .replace(/(\s+)/g, '-')
        .replace(/-+/g, '-')
        .trim();

    const syncSlug = () => {
        slugInput.value = generateSlug(titleInput.value);
    };

    titleInput.removeEventListener('input', syncSlug);
    titleInput.addEventListener('input', syncSlug);
    syncSlug();
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

    // 2. Chuẩn hóa Thumbnail (cho phép cả URL trực tiếp và đường dẫn nội bộ /images/...)
    if (thumbnail) {
        // Nếu là URL trực tiếp (http/https) thì giữ nguyên; nếu là đường dẫn nội bộ thì chuẩn hóa thành /images/...
        if (!/^https?:\/\//i.test(thumbnail) && !thumbnail.startsWith('/images/')) {
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
            headers: csrfHeaders({
                'Content-Type': 'application/json'
            }),
            body: JSON.stringify(blogData)
        });

        if (!response.ok) {
            await handleApiError(response);
            return false;
        }

        alert(method === 'POST' ? "Tạo thành công!" : "Cập nhật thành công!");
        // Gọi hàm render lại danh sách bài viết nếu có
        loadBlogs();
        return true; // Để Modal tự đóng
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
    if (typeof Quill === 'undefined') {
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

    // The explorer should pass the actual public URL (Supabase CDN) so Quill inserts a direct image URL.
    const normalizedUrl = path && /^(https?:)?\/\//i.test(path) ? path : (path ? (path.startsWith('/') ? path : '/' + path) : '');

    // Chèn ảnh vào đúng vị trí đã lưu
    quill.insertEmbed(range.index, 'image', normalizedUrl);

    // Di chuyển con trỏ xuống sau ảnh vừa chèn
    quill.setSelection(range.index + 1);
}

// Hàm callback để nhận đường dẫn ảnh và hiển thị preview
function setBlogThumbnail(path) {
    const input = document.getElementById('blog-thumb-path');
    const previewWrap = document.getElementById('blog-thumb-preview-wrap');
    const previewImg = document.getElementById('blog-thumb-preview-img');
    const normalizedPath = path && /^(https?:)?\/\//i.test(path) ? path : (path ? (path.startsWith('/') ? path : '/' + path) : '');

    if (input) input.value = normalizedPath;
    if (previewImg) {
        previewImg.src = normalizedPath;
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
        contentPath: currentSelectedFolder || 'articles/',
        content: '',
        publishTime: '',
        createdAt: null,
        updatedAt: null
    };

    if (blogId) {
        try {
            AdminApp.showLoading(true);
            const response = await fetch(joinUrl(API_BASE_URL, `/api/admin/blogs/${blogId}`)); // API bạn đã chỉnh
            if (!response.ok) {
                await handleApiError(response);
                AdminApp.showLoading(false);
                return;
            }
            blogData = await response.json();
            //console.log(blogData.updated_time);
        } catch (error) {
            alert("Không thể kết nối đến máy chủ.");
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
        if (!response.ok) {
            await handleApiError(response);
            return;
        }
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
        if (!response.ok) {
            await handleApiError(response);
            return;
        }
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

// Đăng ký thay đổi phần tử blogs của đối tượng ModuleRegistry
registerModule('blogs', renderBlogModule);