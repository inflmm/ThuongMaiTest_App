/**
 * admin-core.js - Quản lý điều hướng và trạng thái Admin
 */
const API_BASE_URL = 'https://192.168.1.9:8443';
// Hàm xử lý nối URL
function joinUrl(base, path) {
    if (!path) return base;
    return base.replace(/\/$/, '') + '/' + path.replace(/^\//, '');
}
// 1. Khai báo danh sách các hàm render cho từng module
const ModuleRegistry = {
    'blogs': () => {
        renderBlogModule();
        document.getElementById('header-toolbox').innerHTML = '';
    },
    'products': () => {
        document.getElementById('admin-app-root').innerHTML = '<h3>Module Sản phẩm đang phát triển</h3>';
        document.getElementById('header-toolbox').innerHTML = '';
    },
    'categories': () => {
        document.getElementById('admin-app-root').innerHTML = '<h3>Module Danh mục đang phát triển</h3>';
        document.getElementById('header-toolbox').innerHTML = '';
    },
    'dashboard': () => {
        document.getElementById('admin-app-root').innerHTML = '<h3>Chào mừng Admin quay trở lại!</h3>';
        document.getElementById('header-toolbox').innerHTML = '';
    }
};
const AdminApp = {
    rootElement: document.getElementById('admin-app-root'),
    overlay: document.getElementById('loading-overlay'),
    titleElement: document.getElementById('current-module-title'),

    init() {
        this.bindEvents();
        this.handleInitialRoute();
    },

    bindEvents() {
        // Bắt sự kiện click vào Sidebar
        document.querySelectorAll('.nav-link[data-module]').forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                const path = e.currentTarget.getAttribute('href');
                const module = e.currentTarget.getAttribute('data-module');
                this.navigate(path, module);
            });
        });

        // Xử lý khi nhấn nút Back/Forward của trình duyệt
        window.addEventListener('popstate', () => {
            this.handleInitialRoute();
        });
    },

    navigate(path, moduleName) {
        window.history.pushState(null, null, path);
        this.loadModule(moduleName);
    },

    handleInitialRoute() {
        const path = window.location.pathname;
        // Phân tích path để lấy module name
        // Ví dụ: /admin/main/blogs -> blogs
        const segments = path.split('/');
        const moduleName = segments[segments.length - 1] || 'dashboard';

        this.loadModule(moduleName);
        this.updateActiveLink(moduleName);
    },

    init() {
        this.bindEvents();
        this.handleInitialRoute();
        // Kiểm tra trạng thái sidebar từ lần trước
        if (localStorage.getItem('sidebar-mini') === 'true') {
            this.toggleSidebar(true);
        }
    },

    async loadModule(moduleName) {
        this.showLoading(true);

        try {
            this.titleElement.innerText = `Quản lý ${moduleName.charAt(0).toUpperCase() + moduleName.slice(1)}`;

            // Gọi hàm render tương ứng từ Registry
            if (ModuleRegistry[moduleName]) {
                await ModuleRegistry[moduleName]();
            } else {
                this.rootElement.innerHTML = `<h3>Không tìm thấy module: ${moduleName}</h3>`;
            }

            this.updateActiveLink(moduleName); // Cập nhật highlight sidebar
        } catch (error) {
            if (this.rootElement) {
                this.rootElement.innerHTML = `<p style="color:red">Lỗi: ${error.message}</p>`;
            }
        } finally {
            this.showLoading(false);
        }
    },

    /**
     * Hiển thị Modal hỗ trợ nhiều lớp (ID riêng biệt)
     * Giữ nguyên các class: modal-overlay, modal-content, modal-header, modal-body, modal-footer
     */
    showModal(options) {
        const {
            id = 'default-modal', title, bodyHTML, width,
            confirmText = 'Xác nhận', onConfirm,
            toolboxHTML = '',
            subHeader = '',
            hideFooter = false // Thêm option này
        } = options;
        // 1. Kiểm tra và xóa modal cũ trùng ID nếu có để tránh lỗi trùng lặp DOM
        const existing = document.getElementById(id);
        if (existing) existing.remove();

        // 2. Tính toán z-index: Nếu là modal image explorer thì cho cao hơn modal thường
        // Dựa trên file CSS của bạn, #image-explorer-modal cần z-index 1100
        const zIndex = (id === 'image-explorer-modal') ? 1100 : 1050;

        const modalHTML = `
            <div id="${id}" class="modal-overlay" style="z-index: ${zIndex}">
                <div id="${id}-content" class="modal-content">
                    <div class="modal-header">
                        <div class="modal-main-header">
                            <div class="modal-title-group">
                                <span class="modal-title">${title}</span>
                                <div id="${id}-toolbox" class="modal-toolbox">${toolboxHTML}</div>
                            </div>
                            <div class="modal-actions">
                                <button class="btn-action-icon" title="Phóng to" onclick="AdminApp.toggleMaximize('${id}')">
                                    <i class="fa-solid fa-expand"></i>
                                </button>
                                <button class="close-modal" onclick="document.getElementById('${id}').remove()">&times;</button>
                            </div>
                        </div>
                        <div id="${id}-sub-header" class="modal-sub-header">
                            ${subHeader}
                        </div>
                    </div>
                    <div class="modal-body">
                        ${bodyHTML}
                    </div>
                    <div id="modal-footer" class="modal-footer">
                        
                    </div>
                </div>
            </div>
        `;

        document.body.insertAdjacentHTML('beforeend', modalHTML);

        //3. Kiểm tra có bỏ footer hay không
        const modal = document.getElementById(id);
        const footer = modal.querySelector('.modal-footer');

        if (hideFooter) {
            footer.style.display = 'none';
        } else {
            footer.style.display = 'flex';
            // 2. Nếu confirmText trống, có thể làm rỗng footer
            if (!confirmText) {
                footer.innerHTML = '';
            } else {
                // Render nút mặc định như cũ
                footer.innerHTML = `
                    <button class="btn-secondary" onclick="document.getElementById('${id}').remove()">
                        <span>Hủy</span>
                        <span>&times;</span>
                    </button>
                    <button id="${id}-confirm-btn" class="btn-primary">${confirmText}</button>
                `;
                modal.querySelector(`#${id}-confirm-btn`).onclick = onConfirm;
            }
        }

        // 4. Gán sự kiện cho nút xác nhận
        const confirmBtn = document.getElementById(`${id}-confirm-btn`);
        if (confirmBtn) {
            confirmBtn.onclick = async () => {
                if (onConfirm) {
                    const result = await onConfirm();
                    // Nếu hàm onConfirm trả về false, ta sẽ không đóng modal (để giữ lại xem lỗi)
                    if (result === false) return;
                }
                // Mặc định đóng modal sau khi hoàn tất
                const currentModal = document.getElementById(id);
                if (currentModal) currentModal.remove();
            };
        }
    },

    showLoading(isLoading) {
        if (this.overlay) {
            this.overlay.style.display = isLoading ? 'flex' : 'none';
        }
    },

    updateActiveLink(moduleName) {
        document.querySelectorAll('.nav-link').forEach(link => {
            link.classList.toggle('active', link.getAttribute('data-module') === moduleName);
        });
    },

    // Hàm hỗ trợ phóng to (thêm vào đối tượng AdminApp)
    toggleMaximize(id) {
        const modal = document.querySelector(`#${id} .modal-content`);
        const icon = document.querySelector(`#${id} .fa-expand, #${id} .fa-compress`);

        modal.classList.toggle('modal-maximized');

        if (modal.classList.contains('modal-maximized')) {
            icon.classList.replace('fa-expand', 'fa-compress');
            modal.style.width = '100%';
            modal.style.height = '100%';
        } else {
            icon.classList.replace('fa-compress', 'fa-expand');
            modal.style.width = ''; // Trả về mặc định
            modal.style.height = '';
        }
    },

    // Thêm vào trong AdminApp
    toggleSidebar(forceMini = false) {
        const container = document.querySelector('.admin-container');
        const sidebar = document.querySelector('.sidebar');

        if (forceMini || !sidebar.classList.contains('mini')) {
            sidebar.classList.add('mini');
            container.classList.add('sidebar-is-mini');
            localStorage.setItem('sidebar-mini', 'true');
        } else {
            sidebar.classList.remove('mini');
            container.classList.remove('sidebar-is-mini');
            localStorage.setItem('sidebar-mini', 'false');
        }
    }
};

/**
 * admin-core.js (Tiếp theo) - Phần Xử lý Auth
 */

// Hàm xử lý đăng nhập dành riêng cho Admin
async function handleAdminLogin() {
    const username = document.getElementById('admin-user').value;
    const password = document.getElementById('admin-pass').value;

    try {
        const response = await fetch(joinUrl(API_BASE_URL, '/api/auth/login'), {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams({
                'username': username,
                'password': password
            })
        });

        const data = await response.json();

        if (response.ok) {
            // Giả sử API trả về role hoặc bạn fetch thông tin profile sau khi login
            const userRole = data.role; // Cần đảm bảo API login trả về role
            console.log(userRole);

            if (userRole === 'ROLE_ADMIN') {
                window.location.href = '/admin/main/dashboard';
            } else {
                alert('Tài khoản của bạn không có quyền truy cập vùng Quản trị.');
                // Có thể gọi logout luôn để xóa session vừa tạo
                fetch(joinUrl(API_BASE_URL, '/api/auth/logout'), { method: 'POST' });
            }
        } else {
            alert('Sai tài khoản hoặc mật khẩu!');
        }
    } catch (error) {
        console.error('Lỗi login:', error);
        alert('Có lỗi xảy ra khi kết nối server.');
    }
}

// Cập nhật hàm Logout sử dụng modal mới
async function handleLogout() {
    AdminApp.showModal({
        title: 'Xác nhận thoát',
        bodyHTML: `
            <div class="text-center p-4">
                <div class="text-danger">Bạn có chắc chắn muốn thoát khỏi hệ thống Quản trị không?</div>
            </div>
        `,
        confirmText: 'Đăng xuất',
        onConfirm: async () => {
            try {
                const response = await fetch(joinUrl(API_BASE_URL, '/api/auth/logout'), { method: 'POST' });
                if (response.ok) window.location.href = '/admin/login';
            } catch (e) { console.error(e); }
        }
    });
}

// Tạo đối tượng rỗng tránh lỗi undefined
async function renderBlogModule() { }
// Hàm cập nhật đối tượng cho ModuleRegistry
function registerModule(name, renderFn) {
    ModuleRegistry[name] = renderFn;
}

// Khởi tạo app
document.addEventListener('DOMContentLoaded', () => AdminApp.init());