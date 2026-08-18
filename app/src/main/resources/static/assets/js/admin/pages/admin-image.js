/**
 * admin-image.js - Image explorer and upload workflows shared by admin modules.
 */
let selectedExplorerImageUrl = '';
let selectedUploadFiles = [];

async function openImageExplorer(onSelectCallback) {
    const html = `
        <div class="image-explorer-container">
            <aside id="exp-nav" class="explorer-nav">
                <button onclick="toggleExpNav()" class="btn-sm mb-2"><i class="fa-solid fa-bars"></i></button>
                <div class="folder-header"><div class="folder-header-title">Thư mục ảnh</div></div>
                <div id="image-folder-list"></div>
            </aside>
            <section id="exp-files" class="explorer-main"><p class="text-muted">Chọn một thư mục để xem ảnh</p></section>
            <aside id="exp-preview" class="explorer-preview">
                <div class="exp-preview-toolbox"><button onclick="togglePreviewBox()" class="btn-explorer-preview btn-sm mb-2"><i class="fa-solid fa-bars"></i></button></div>
                <div id="preview-box"><i class="fa-regular fa-image fa-4x text-muted"></i><p>Xem trước ảnh</p></div>
            </aside>
        </div>`;
    const subHeader = `<div id="image-explorer-path-display" class="path-display"><div><span>Đường dẫn: </span><span id="image-explorer-link">/images</span></div></div>`;
    const toolbox = `<button class="btn-create w-100 mt-2" onclick="openUploadOverlay()"><i class="fa-solid fa-plus"></i> Upload ảnh mới</button>`;

    AdminApp.showModal({
        id: 'image-explorer-modal', title: 'Thư viện hình ảnh', bodyHTML: html,
        subHeader, toolboxHTML: toolbox, width: '95%', confirmText: 'Chèn ảnh này',
        onConfirm: () => {
            const pathDisplay = document.getElementById('image-explorer-link');
            const finalPath = (pathDisplay ? pathDisplay.innerText : '').replace(/\s\/\s/g, '/').trim();
            if (selectedExplorerImageUrl && onSelectCallback) {
                onSelectCallback(selectedExplorerImageUrl);
                return true;
            }
            if (finalPath && finalPath !== 'images' && onSelectCallback) {
                onSelectCallback(finalPath);
                return true;
            }
            alert('Vui lòng chọn một file ảnh cụ thể!');
            return false;
        }
    });

    await loadFolders(false, 'images');
    await loadImagesInFolder('');
}

function toggleExpNav() {
    document.getElementById('exp-nav')?.classList.toggle('collapsed');
}

function togglePreviewBox() {
    document.getElementById('exp-preview')?.classList.toggle('collapsed');
}

async function loadImagesInFolder(path) {
    const normalizedPath = (path || '').replace(/^\/+|\/+$/g, '');
    currentSelectedImageFolder = normalizedPath;
    updateFolderActiveUI(normalizedPath, 'images');
    const mainArea = document.getElementById('exp-files');
    if (!mainArea) return;
    mainArea.innerHTML = '<div class="p-3">Đang tải...</div>';

    try {
        const url = joinUrl(API_BASE_URL, `/api/admin/folders/images/files?path=${encodeURIComponent(normalizedPath)}`);
        const response = await fetch(url);
        if (!response.ok) {
            await handleApiError(response);
            mainArea.innerHTML = '<div class="p-3 text-danger">Lỗi tải danh sách ảnh</div>';
            return;
        }
        const files = await response.json();
        selectedExplorerImageUrl = '';
        updateImageExplorerPath(normalizedPath);
        if (!files || files.length === 0) {
            mainArea.innerHTML = '<div class="empty-image-folder-text"><i class="fa-solid fa-folder-open" style="font-size: 2rem; display: block; margin: auto;"></i>Thư mục này hiện đang rỗng.</div>';
            return;
        }
        mainArea.innerHTML = files.map(file => {
            const fileName = file.name || file;
            const imagePath = normalizedPath ? `/${normalizedPath}/${fileName}` : `/${fileName}`;
            const directUrl = file.publicUrl || file.url || '';
            const fullUrl = directUrl || joinUrl(API_BASE_URL, 'images' + imagePath.replace(/\/+/g, '/'));
            return `<div class="img-item-card" data-public-url="${directUrl}" onclick="previewImage('${imagePath}', this)"><img class="exp-image-item" src="${fullUrl}"><div class="exp-image-text">${fileName}</div></div>`;
        }).join('');
    } catch (error) {
        console.error(error);
        mainArea.innerHTML = '<div class="p-3 text-danger">Lỗi tải danh sách ảnh</div>';
    }
}

function formatBytes(bytes) {
    if (bytes === 0) return '0 Bytes';
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const index = Math.floor(Math.log(bytes) / Math.log(1024));
    return parseFloat((bytes / Math.pow(1024, index)).toFixed(1)) + ' ' + sizes[index];
}

function previewImage(path, element) {
    document.querySelectorAll('.img-item-card').forEach(item => item.classList.remove('selected'));
    element.classList.add('selected');
    updateImageExplorerPath(path);
    selectedExplorerImageUrl = element.dataset.publicUrl || '';
    const previewBox = document.getElementById('preview-box');
    if (previewBox) previewBox.innerHTML = `<div class="preview-sticky"><img src="${selectedExplorerImageUrl}" class="img-fluid rounded border" style="max-width: 100%; height: auto;"></div>`;
}

function updateImageExplorerPath(path) {
    const pathDisplay = document.getElementById('image-explorer-link');
    if (!pathDisplay) return;
    const normalizedPath = (path || '').replace(/^\/+|\/+$/g, '');
    const cleanDisplay = normalizedPath.split('/').filter(Boolean).join('/');
    pathDisplay.innerText = cleanDisplay ? `images/${cleanDisplay}` : 'images';
}

function openUploadOverlay() {
    const displayFolder = currentSelectedImageFolder || 'Thư mục gốc';
    AdminApp.showModal({
        id: 'upload-image-modal',
        title: '<i class="fa-solid fa-cloud-arrow-up"></i> Tải ảnh mới lên hệ thống',
        bodyHTML: `<div class="upload-overlay-container"><div class="upload-overlay-wrapper">
            <div class="upload-form-group"><label class="upload-label">Thư mục đích hiện tại:</label><div class="upload-folder-display"><i class="fa-solid fa-folder"></i> <span>${displayFolder}</span></div></div>
            <div class="upload-toggle-container"><label class="upload-label-inline">Chế độ upload:</label><div class="upload-switch-wrapper"><label class="upload-switch"><input type="checkbox" id="upload-mode-toggle" onchange="toggleUploadMode(this.checked)"><span class="upload-slider"></span></label><span id="toggle-label" class="upload-toggle-text mode-raw">Ảnh gốc (không resize / không chuyển định dạng)</span></div></div>
            <div id="upload-advanced-controls" style="display:none"><div class="upload-form-grid"><div class="upload-form-group"><label class="upload-label">Định dạng đầu ra:</label><select id="upload-image-format-select"><option value="webp">WebP (mặc định)</option><option value="jpg">JPG</option></select></div><div class="upload-form-group"><label class="upload-label">Chất lượng nén:</label><input type="range" id="upload-quality-slider" min="50" max="100" value="80"><span id="upload-quality-value">80%</span></div></div>
                <div class="upload-form-group"><label class="upload-label">Cách tạo ảnh:</label><select id="upload-resize-mode-select"><option value="all4">Tạo 4 kích thước chuẩn</option><option value="specific">Tạo 1 kích thước cụ thể</option></select></div>
                <div class="upload-form-group"><label class="upload-label">Kích thước mục tiêu:</label><select id="upload-variant-select"><option value="large">Large</option><option value="medium">Medium</option><option value="compact">Compact</option><option value="thumbnail">Thumbnail</option><option value="custom">Custom</option></select></div>
                <div class="upload-form-grid"><input type="number" id="upload-custom-width" min="1" value="1200" placeholder="Chiều rộng (px)"><input type="number" id="upload-custom-height" min="1" value="1200" placeholder="Chiều cao (px)"></div>
                <div class="upload-form-group"><label class="upload-label">Cách đặt tên:</label><select id="upload-naming-mode-select"><option value="original">Giữ tên gốc</option><option value="random">Tạo prefix ngẫu nhiên</option><option value="manual">Dùng prefix tự nhập</option></select></div>
                <input type="text" id="upload-prefix-input" placeholder="Prefix tùy chỉnh"><select id="upload-suffix-style-select"><option value="size">Thêm hậu tố kích thước</option><option value="none">Không thêm hậu tố</option></select>
            </div></div>
            <div class="upload-preview-divider"><div class="upload-form-group"><label class="upload-label">Chọn file từ máy tính:</label><div class="upload-file-zone"><input type="file" id="modal-file-input" multiple accept="image/webp, image/jpeg, image/png" onchange="handleModalFileSelect(this)"><label for="modal-file-input" class="upload-file-trigger"><i class="fa-solid fa-images"></i> Bấm để chọn hoặc kéo thả nhiều ảnh vào đây...</label></div></div><div class="upload-preview-wrapper"><div id="upload-preview-list" class="upload-preview-list"><span class="upload-empty-text" id="no-file-text">Chưa có tệp tin nào được chọn</span></div></div></div></div></div>`,
        confirmText: 'Bắt đầu Tải lên',
        onConfirm: executeUploadFlow
    });
    selectedUploadFiles = [];
    document.getElementById('upload-quality-slider')?.addEventListener('input', event => {
        const label = document.getElementById('upload-quality-value');
        if (label) label.textContent = `${event.target.value}%`;
    });
}

function toggleUploadMode(isResizeMode) {
    const label = document.getElementById('toggle-label');
    const controls = document.getElementById('upload-advanced-controls');
    if (label) { label.innerText = isResizeMode ? 'Xử lý ảnh (resize / chuyển định dạng)' : 'Ảnh gốc (không resize / không chuyển định dạng)'; label.className = `upload-toggle-text ${isResizeMode ? 'mode-product' : 'mode-raw'}`; }
    if (controls) controls.style.display = isResizeMode ? 'block' : 'none';
}

function handleModalFileSelect(input) {
    const previewList = document.getElementById('upload-preview-list');
    const noFileText = document.getElementById('no-file-text');
    const maxBytes = 10 * 1024 * 1024;
    if (!input.files?.length) return;
    noFileText?.remove();
    const incomingFiles = Array.from(input.files);
    if (selectedUploadFiles.reduce((sum, file) => sum + (file?.size || 0), 0) + incomingFiles.reduce((sum, file) => sum + file.size, 0) > maxBytes) { alert('Tổng dung lượng vượt quá 10MB.'); return; }
    incomingFiles.filter(file => file.size <= maxBytes).forEach(file => {
        const index = selectedUploadFiles.push(file) - 1;
        const objectUrl = URL.createObjectURL(file);
        const row = document.createElement('div'); row.className = 'upload-preview-item'; row.id = `upload-item-${index}`;
        row.innerHTML = `<div class="upload-item-info"><img src="${objectUrl}" class="upload-item-thumb"><div class="upload-item-meta"><span class="upload-item-name" title="${file.name}">${file.name}</span><span class="upload-item-size">${(file.size / (1024 * 1024)).toFixed(2)} MB</span></div></div><button class="upload-item-remove-btn" onclick="removeSelectedFileFromUpload(${index}, '${objectUrl}')" type="button"><i class="fa-solid fa-trash-can"></i></button>`;
        previewList?.appendChild(row);
    });
    input.value = '';
}

function removeSelectedFileFromUpload(index, objectUrl) {
    document.getElementById(`upload-item-${index}`)?.remove();
    URL.revokeObjectURL(objectUrl); selectedUploadFiles[index] = null;
    const previewList = document.getElementById('upload-preview-list');
    if (previewList && !previewList.children.length) previewList.innerHTML = '<span class="upload-empty-text" id="no-file-text">Chưa có tệp tin nào được chọn</span>';
}

async function executeUploadFlow() {
    const finalFiles = selectedUploadFiles.filter(Boolean);
    if (!finalFiles.length) { alert('Vui lòng chọn ít nhất một file ảnh!'); return false; }
    const formData = new FormData(); finalFiles.forEach(file => formData.append('files', file)); formData.append('folder', currentSelectedImageFolder || '');
    const isResizeMode = document.getElementById('upload-mode-toggle')?.checked;
    const targetEndpoint = isResizeMode ? '/api/admin/images/product-upload' : '/api/admin/images/raw-upload';
    if (isResizeMode) {
        formData.append('format', document.getElementById('upload-image-format-select')?.value || 'webp');
        formData.append('quality', document.getElementById('upload-quality-slider')?.value || '85');
        formData.append('resizeMode', document.getElementById('upload-resize-mode-select')?.value || 'all4');
        formData.append('variant', document.getElementById('upload-variant-select')?.value || 'large');
        formData.append('customWidth', document.getElementById('upload-custom-width')?.value || '0'); formData.append('customHeight', document.getElementById('upload-custom-height')?.value || '0');
        formData.append('namingMode', document.getElementById('upload-naming-mode-select')?.value || 'original'); formData.append('prefix', document.getElementById('upload-prefix-input')?.value || ''); formData.append('suffixStyle', document.getElementById('upload-suffix-style-select')?.value || 'size');
    }
    AdminApp.showLoading(true);
    try {
        const response = await fetch(joinUrl(API_BASE_URL, targetEndpoint), {
            method: 'POST',
            headers: csrfHeaders(),
            body: formData
        });
        if (!response.ok) {
            await handleApiError(response);
            return false;
        }
        const result = await response.json();
        if (!result.success) { alert('Lỗi hệ thống: ' + (result.message || 'Không thể thực thi.')); return false; }
        alert(result.message || 'Tải tài nguyên lên server thành công!');
        document.getElementById('upload-image-modal')?.remove();
        await loadImagesInFolder(currentSelectedImageFolder);
        return true;
    } catch (error) { console.error('Lỗi API kết nối:', error); alert('Mất kết nối tới máy chủ API Spring Boot.'); return false; }
    finally { AdminApp.showLoading(false); }
}
