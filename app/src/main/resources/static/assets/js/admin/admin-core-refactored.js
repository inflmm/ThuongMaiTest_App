(function () {
    'use strict';

    const API_BASE_URL = window.AdminConfig?.API_BASE_URL || 'https://192.168.1.12:8443';
    const blogApi = window.BlogApi;

    function joinUrl(base, path) {
        if (!path) return base;
        return base.replace(/\/$/, '') + '/' + path.replace(/^\//, '');
    }

    const ModuleRegistry = {
        blogs: () => {
            if (typeof window.renderBlogModule === 'function') {
                window.renderBlogModule();
            }
            const toolbox = document.getElementById('header-toolbox');
            if (toolbox) toolbox.innerHTML = '';
        },
        products: () => {
            const root = document.getElementById('admin-app-root');
            if (root) root.innerHTML = '<h3>Module Sản phẩm đang phát triển</h3>';
            const toolbox = document.getElementById('header-toolbox');
            if (toolbox) toolbox.innerHTML = '';
        },
        categories: () => {
            const root = document.getElementById('admin-app-root');
            if (root) root.innerHTML = '<h3>Module Danh mục đang phát triển</h3>';
            const toolbox = document.getElementById('header-toolbox');
            if (toolbox) toolbox.innerHTML = '';
        },
        dashboard: () => {
            const root = document.getElementById('admin-app-root');
            if (root) root.innerHTML = '<h3>Chào mừng Admin quay trở lại!</h3>';
            const toolbox = document.getElementById('header-toolbox');
            if (toolbox) toolbox.innerHTML = '';
        }
    };

    const AdminApp = {
        rootElement: document.getElementById('admin-app-root'),
        overlay: document.getElementById('loading-overlay'),
        titleElement: document.getElementById('current-module-title'),

        init() {
            this.bindEvents();
            this.handleInitialRoute();
            if (localStorage.getItem('sidebar-mini') === 'true') {
                this.toggleSidebar(true);
            }
        },

        bindEvents() {
            document.querySelectorAll('.nav-link[data-module]').forEach((link) => {
                link.addEventListener('click', (event) => {
                    event.preventDefault();
                    const path = event.currentTarget.getAttribute('href');
                    const module = event.currentTarget.getAttribute('data-module');
                    this.navigate(path, module);
                });
            });

            window.addEventListener('popstate', () => {
                this.handleInitialRoute();
            });
        },

        navigate(path, moduleName) {
            window.history.pushState(null, '', path);
            this.loadModule(moduleName);
        },

        handleInitialRoute() {
            const path = window.location.pathname;
            const segments = path.split('/');
            const moduleName = segments[segments.length - 1] || 'dashboard';
            this.loadModule(moduleName);
            this.updateActiveLink(moduleName);
        },

        async loadModule(moduleName) {
            this.showLoading(true);

            try {
                const title = this.titleElement;
                if (title) {
                    title.innerText = `Quản lý ${moduleName.charAt(0).toUpperCase()}${moduleName.slice(1)}`;
                }

                if (ModuleRegistry[moduleName]) {
                    await ModuleRegistry[moduleName]();
                } else {
                    if (this.rootElement) {
                        this.rootElement.innerHTML = `<h3>Không tìm thấy module: ${moduleName}</h3>`;
                    }
                }

                this.updateActiveLink(moduleName);
            } catch (error) {
                if (this.rootElement) {
                    this.rootElement.innerHTML = `<p style="color:red">Lỗi: ${error.message}</p>`;
                }
            } finally {
                this.showLoading(false);
            }
        },

        showModal(options = {}) {
            const {
                id = 'default-modal',
                title = '',
                bodyHTML = '',
                confirmText = 'Xác nhận',
                onConfirm = null,
                toolboxHTML = '',
                subHeader = '',
                hideFooter = false
            } = options;

            const existing = document.getElementById(id);
            if (existing) existing.remove();

            const zIndex = id === 'image-explorer-modal' ? 1100 : 1050;
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
                            <div id="${id}-sub-header" class="modal-sub-header">${subHeader}</div>
                        </div>
                        <div class="modal-body">${bodyHTML}</div>
                        <div class="modal-footer"></div>
                    </div>
                </div>`;

            document.body.insertAdjacentHTML('beforeend', modalHTML);
            const modal = document.getElementById(id);
            const footer = modal.querySelector('.modal-footer');

            if (hideFooter) {
                footer.style.display = 'none';
            } else {
                footer.style.display = 'flex';
                if (!confirmText) {
                    footer.innerHTML = '';
                } else {
                    footer.innerHTML = `
                        <button class="btn-secondary" onclick="document.getElementById('${id}').remove()">
                            <span>Hủy</span>
                            <span>&times;</span>
                        </button>
                        <button id="${id}-confirm-btn" class="btn-primary">${confirmText}</button>`;
                    const confirmBtn = modal.querySelector(`#${id}-confirm-btn`);
                    if (confirmBtn) {
                        confirmBtn.onclick = async () => {
                            if (onConfirm) {
                                const result = await onConfirm();
                                if (result === false) return;
                            }
                            const currentModal = document.getElementById(id);
                            if (currentModal) currentModal.remove();
                        };
                    }
                }
            }
        },

        showLoading(isLoading) {
            if (this.overlay) {
                this.overlay.style.display = isLoading ? 'flex' : 'none';
            }
        },

        updateActiveLink(moduleName) {
            document.querySelectorAll('.nav-link').forEach((link) => {
                link.classList.toggle('active', link.getAttribute('data-module') === moduleName);
            });
        },

        toggleMaximize(id) {
            const content = document.querySelector(`#${id} .modal-content`);
            const icon = document.querySelector(`#${id} .fa-expand, #${id} .fa-compress`);
            if (!content || !icon) return;

            content.classList.toggle('modal-maximized');
            if (content.classList.contains('modal-maximized')) {
                icon.classList.replace('fa-expand', 'fa-compress');
                content.style.width = '100%';
                content.style.height = '100%';
            } else {
                icon.classList.replace('fa-compress', 'fa-expand');
                content.style.width = '';
                content.style.height = '';
            }
        },

        toggleSidebar(forceMini = false) {
            const container = document.querySelector('.admin-container');
            const sidebar = document.querySelector('.sidebar');
            if (!sidebar || !container) return;

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

    async function handleAdminLogin() {
        const username = document.getElementById('admin-user')?.value || '';
        const password = document.getElementById('admin-pass')?.value || '';

        try {
            const response = await fetch(joinUrl(API_BASE_URL, blogApi?.endpoints?.auth?.login || '/api/auth/login'), {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: new URLSearchParams({
                'username': username,
                'password': password
                })
            });

            const data = await response.json();
            if (response.ok) {
                const userRole = data.role;
                if (userRole === 'ROLE_ADMIN') {
                    window.location.href = '/admin/main/dashboard';
                } else {
                    alert('Tài khoản của bạn không có quyền truy cập vùng Quản trị.');
                    fetch(joinUrl(API_BASE_URL, blogApi?.endpoints?.auth?.logout || '/api/auth/logout'), { method: 'POST' });
                }
            } else {
                alert('Sai tài khoản hoặc mật khẩu!');
            }
        } catch (error) {
            console.error('Lỗi login:', error);
            alert('Có lỗi xảy ra khi kết nối server.');
        }
    }

    async function handleLogout() {
        AdminApp.showModal({
            title: 'Xác nhận thoát',
            bodyHTML: `
                <div class="text-center p-4">
                    <div class="text-danger">Bạn có chắc chắn muốn thoát khỏi hệ thống Quản trị không?</div>
                </div>`,
            confirmText: 'Đăng xuất',
            onConfirm: async () => {
                try {
                    const response = await fetch(joinUrl(API_BASE_URL, blogApi?.endpoints?.auth?.logout || '/api/auth/logout'), { method: 'POST' });
                    if (response.ok) {
                        window.location.href = '/admin/login';
                    }
                } catch (error) {
                    console.error(error);
                }
            }
        });
    }

    function registerModule(name, renderFn) {
        ModuleRegistry[name] = renderFn;
    }

    window.API_BASE_URL = API_BASE_URL;
    window.joinUrl = joinUrl;
    window.AdminApp = AdminApp;
    window.ModuleRegistry = ModuleRegistry;
    window.handleAdminLogin = handleAdminLogin;
    window.handleLogout = handleLogout;
    window.registerModule = registerModule;

    document.addEventListener('DOMContentLoaded', () => AdminApp.init());
})();
