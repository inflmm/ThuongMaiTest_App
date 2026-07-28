(function () {
    'use strict';

    const API_BASE_URL = 'https://192.168.1.12:8443';

    function joinUrl(base, path) {
        if (!path) return base;
        return base.replace(/\/$/, '') + '/' + path.replace(/^\//, '');
    }

    function escapeHtml(value) {
        return String(value ?? '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function escapeAttribute(value) {
        return escapeHtml(value);
    }

    async function requestJson(url, options = {}) {
        const response = await fetch(url, options);
        if (!response.ok) {
            const text = await response.text();
            throw new Error(text || `Request failed with status ${response.status}`);
        }

        const contentType = response.headers.get('content-type') || '';
        if (contentType.includes('application/json')) {
            return response.json();
        }

        return response.text();
    }

    window.AdminConfig = {
        API_BASE_URL,
        joinUrl,
        escapeHtml,
        escapeAttribute,
        requestJson,
        getApiUrl(path) {
            return joinUrl(API_BASE_URL, path);
        }
    };
})();
