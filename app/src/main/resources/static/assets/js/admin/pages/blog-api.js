(function () {
    'use strict';

    const config = window.AdminConfig;

    const endpoints = {
        auth: {
            login: '/api/auth/login',
            logout: '/api/auth/logout'
        },
        admin: {
            folders: '/api/admin/folders',
            folderTree: '/api/admin/folders/images/tree',
            blogs: '/api/admin/blogs',
            blogById: (id) => `/api/admin/blogs/${id}`,
            togglePublish: (id) => `/api/admin/blogs/${id}/publish`,
            search: '/api/admin/blogs/search',
            imagesFiles: (path) => `/api/admin/folders/images/files?path=${encodeURIComponent(path)}`
        },
        images: {
            rawUpload: '/api/admin/images/raw-upload',
            productUpload: '/api/admin/images/product-upload'
        }
    };

    function getApiUrl(path) {
        return config?.getApiUrl ? config.getApiUrl(path) : path;
    }

    async function requestJson(path, options = {}) {
        return config?.requestJson ? config.requestJson(getApiUrl(path), options) : null;
    }

    window.BlogApi = {
        endpoints,
        getApiUrl,
        requestJson
    };
})();
