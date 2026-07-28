// product-detail.js

// Các phần tử điều khiển chung
const loadingMessage = document.getElementById('loading-message');
const errorMessage = document.getElementById('error-message');

// Các phần tử thông tin (Vẫn giữ vì ID khớp với HTML)
const detailName = document.getElementById('detail-name');
const detailPrice = document.getElementById('detail-price');
const detailDescription = document.getElementById('detail-description');

// MỚI: Khai báo các container chứa ảnh Slider
const masterContainer = document.getElementById('master-image-container');
const thumbContainer = document.getElementById('thumbnail-list');

// Bộ chọn số lượng
const detailQuantityInput = document.getElementById('detail-quantity');
const btnDecreaseQty = document.getElementById('btn-decrease-qty');
const btnIncreaseQty = document.getElementById('btn-increase-qty');
const addToCartBtn = document.getElementById('add-to-cart-btn');
const overlayImageContainer = document.getElementById('overlay-image-container');



let currentProduct = null; // Biến để lưu trữ thông tin sản phẩm hiện tại
let overlayIndex = 0; // Lưu vị trí ảnh đang xem trong overlay

// --- BIẾN CHO THUMBNAIL (Sử dụng hậu tố Thumb) ---
let isDraggingThumb = false;
let startXThumb, scrollLeftThumb;
let velThumb = 0, rafThumb;
let lastXThumb, lastTimeThumb, movedThumb = false;

// --- BIẾN CHO ZOOM OVERLAY ---
let isZoomed = false;
let isDraggingZoom = false;
let translateX = 0, translateY = 0;
let startXRaw, startYRaw, startXZoom, startYZoom;
let velX = 0, velY = 0, lastX, lastY, lastTime;
let movedZoom = false;
let rafInertia;
let dragStartTime;
let initialOverlayIndex = 0; // Biến lưu ảnh lúc bắt đầu mở overlay
let lockDirection = null; // 'horizontal', 'vertical' hoặc null
let currentMasterIndex = 0;

let masterImg = null; // Khai báo trước để các hàm đều thấy

const ZOOM_LEVEL = 2;
const SWIPE_THRESHOLD = 70;
const RESISTANCE_FACTOR = 2.0;

const overlayImg = document.getElementById('overlay-img');
const imageOverlay = document.getElementById('image-overlay');
const closeOverlayBtn = document.getElementById('close-overlay');
const overlayBtnPrev = document.getElementById('overlay-btn-prev');
const overlayBtnNext = document.getElementById('overlay-btn-next');
/**
 * Render Gallery ảnh theo kiến trúc Master-Source
 * param {object} product - Đối tượng DTO từ Backend (chứa masterFiles và image_folder_path)
 */

// --- 1. Sửa hàm renderGallery để đảm bảo click hoạt động ---
function renderGallery(product) {
    const masterFiles = product.masterFiles || [];
    const folderPath = product.image_folder_path;
    //console.log("Render Gallery với masterFiles:", masterFiles, "và folderPath:", folderPath);

    // Render Thumbnail
    thumbContainer.innerHTML = masterFiles.map((fileName, index) => {
        const compactFile = fileName.replace('_master.webp', '_compact.webp');
        const compactPath = joinUrl(API_BASE_URL, `${folderPath}/compact/${compactFile}`);
        return `
            <div class="thumb-item ${index === 0 ? 'active' : ''}" onclick="scrollToMaster(${index}, this)">
                <img src="${compactPath}">
            </div>`;
    }).join('');

    // Render Master (Click để mở Overlay)
    masterContainer.innerHTML = masterFiles.map((fileName, index) => {
        const compactFile = fileName.replace('_master.webp', '_grande.webp');
        const fullPath = joinUrl(API_BASE_URL, `${folderPath}/grande/${compactFile}`);
        return `<img src="${fullPath}" class="master-item" id="master-img-${index}" 
        onclick="openOverlay(${index})"
        onmousedown="movedThumb = false;">`;
    }).join('');


    
    initInertiaDrag(thumbContainer); // Kích hoạt kéo mượt cho Thumbnail
}

// --- LOGIC OVERLAY ---
function openOverlay(index) {
    const masterImgs = document.querySelectorAll('.master-item');
    const masterImg = masterImgs[index];
    if (!masterImg) {
        //console.error("Không tìm thấy ảnh master tại index:", index);
        return;
    }

    // RESET TRIỆT ĐỂ BIẾN TRẠNG THÁI
    cancelAnimationFrame(rafInertia);
    isZoomed = false;
    isDraggingZoom = false;
    movedZoom = false;
    translateX = 0;
    translateY = 0;
    lockDirection = null;
    initialOverlayIndex = index;
    overlayIndex = index;

    overlayImg.src = masterImg.src;

    // 1. Hiện Overlay (CSS sẽ tự lo phần Fade In nhờ transition opacity)
    imageOverlay.style.transition = 'none'; // Tắt transition để reset opacity
    imageOverlay.style.opacity = '0';
    imageOverlay.style.display = 'flex';
    
    // 4. LOGIC BAY (Chạy sau khi display: flex một nhịp để trình duyệt tính toán Rect)
    requestAnimationFrame(() => {
        const rect = masterImg.getBoundingClientRect();
        const startX = (rect.left + rect.width / 2) - window.innerWidth / 2;
        const startY = (rect.top + rect.height / 2) - window.innerHeight / 2;
        
        // Tính toán scale ban đầu dựa trên kích thước thật của ảnh master so với khung overlay
        const startScale = rect.width / (overlayImg.offsetWidth || window.innerWidth * 0.9);

        // Đưa ảnh về vị trí master ngay lập tức (không transition)
        overlayImg.classList.add('is-dragging');
        overlayImg.style.opacity = '1';
        overlayImg.style.setProperty('--zoom-x', `${startX}px`);
        overlayImg.style.setProperty('--zoom-y', `${startY}px`);
        overlayImg.style.setProperty('--zoom-scale', startScale);

        // Chạy hiệu ứng bay vào giữa
        requestAnimationFrame(() => {
            // Fade In nền và UI
            imageOverlay.style.transition = 'opacity 0.4s ease';
            imageOverlay.style.opacity = '1';
            
            // Bay về tâm (0, 0, 1)
            updateZoomStyles(true);
            
            setTimeout(() => {
                imageOverlay.classList.remove('overlay-opening');
            }, 500);
        });
    });

    updateOverlayCounter();
}

/**
 * Chuyển ảnh trong Overlay
 */
// 2. Hàm chuyển ảnh có hoạt ảnh (Fix lỗi tự động zoom)

// Hàm cập nhật số thứ tự ảnh (Vd: 2 / 12)
async function updateOverlayCounter() {
    const counter = document.getElementById('overlay-counter');
    const masterImgs = document.querySelectorAll('.master-item');
    if (counter && masterImgs.length > 0) {
        counter.innerText = `${overlayIndex + 1} / ${masterImgs.length}`;
    }
}

function closeOverlay() {
    // 1. Đồng bộ Slider ngoài nếu ảnh đã bị đổi
    if (overlayIndex !== initialOverlayIndex) {
        const thumbs = document.querySelectorAll('.thumb-item');
        if (thumbs[overlayIndex]) thumbs[overlayIndex].click();
    }

    // 2. Chỉ chạy animation thu nhỏ nếu vẫn là ảnh cũ
    if (overlayIndex === initialOverlayIndex) {
        const masterImg = document.querySelectorAll('.master-item')[overlayIndex];
        const rect = masterImg.getBoundingClientRect();
        const centerX = window.innerWidth / 2;
        const centerY = window.innerHeight / 2;

        translateX = (rect.left + rect.width / 2) - centerX;
        translateY = (rect.top + rect.height / 2) - centerY;
        const targetScale = rect.width / (overlayImg.offsetWidth || window.innerWidth * 0.9);

        imageOverlay.classList.add('overlay-opening');
        overlayImg.style.setProperty('--zoom-x', `${translateX}px`);
        overlayImg.style.setProperty('--zoom-y', `${translateY}px`);
        overlayImg.style.setProperty('--zoom-scale', targetScale);
    }

    // 3. Fade Out toàn bộ (cả nút bấm và nền)
    imageOverlay.classList.add('overlay-fade-out');

    setTimeout(() => {
        imageOverlay.style.display = 'none';
        imageOverlay.classList.remove('overlay-fade-out', 'overlay-opening');
        document.body.style.overflow = '';
        exitZoom(); // Reset hoàn toàn tọa độ cho lần sau
    }, 350);
}

// Đóng overlay khi nhấn vào vùng nền (vùng đen)
function closeOverlayOutside(event) {
    // Chỉ đóng nếu click vào đúng id "image-overlay" (vùng nền)
    // hoặc click vào wrapper nếu bạn muốn, nhưng thường là vùng nền đen.
    if (event.target.id === 'image-overlay') {
        closeOverlay();
    }
}



// --- 1. LOGIC THUMBNAIL (KÉO QUÁN TÍNH) ---
function initInertiaDrag(slider) {
    slider.addEventListener('mousedown', (e) => {
        isDraggingThumb = true;
        movedThumb = false;
        startXThumb = e.pageX - slider.offsetLeft;
        scrollLeftThumb = slider.scrollLeft;
        lastXThumb = e.pageX;
        lastTimeThumb = Date.now();
        velThumb = 0;
        cancelAnimationFrame(rafThumb);
        slider.style.scrollBehavior = 'auto';
    });

    window.addEventListener('mousemove', (e) => {
        if (!isDraggingThumb) return;
        movedThumb = true;
        const x = e.pageX - slider.offsetLeft;
        const walk = (x - startXThumb) * 1.5;
        slider.scrollLeft = scrollLeftThumb - walk;

        const now = Date.now();
        const dt = now - lastTimeThumb;
        const dx = e.pageX - lastXThumb;
        if (dt > 0) velThumb = dx / dt;
        lastXThumb = e.pageX;
        lastTimeThumb = now;
    });

    window.addEventListener('mouseup', () => {
        if (!isDraggingThumb) return;
        isDraggingThumb = false;
        slider.style.scrollBehavior = 'smooth';
        applyInertiaThumb(slider);
        // Reset movedThumb sau một khoảng ngắn để không kích hoạt click
        setTimeout(() => { movedThumb = false; }, 50);
    });
}

function applyInertiaThumb(slider) {
    if (Math.abs(velThumb) < 0.2) return;
    slider.scrollLeft -= velThumb * 10;
    velThumb *= 0.95;
    rafThumb = requestAnimationFrame(() => applyInertiaThumb(slider));
}

// Cập nhật hàm scrollToMaster để đồng bộ thumbnail
async function scrollToMaster(index) {
    const masterImgs = document.querySelectorAll('.master-item');
    if (index < 0 || index >= masterImgs.length) return;

    currentMasterIndex = index;
    const containerWidth = masterContainer.offsetWidth;
    masterContainer.scrollTo({
        left: index * containerWidth,
        behavior: 'smooth'
    });

    // Cập nhật trạng thái Active cho Thumbnail
    const thumbs = document.querySelectorAll('.thumb-item');
    thumbs.forEach((t, idx) => {
        t.classList.toggle('active', idx === index);
    });

    // Cuộn thumbnail vào vùng nhìn thấy
    const activeThumb = thumbs[index];
    if (activeThumb) {
        activeThumb.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' });
    }
}

// Hàm scrollMasterBtn hiện tại của bạn cần gọi scrollToMaster
async function scrollMasterBtn(direction) {
    const masterImgs = document.querySelectorAll('.master-item');
    let newIndex = currentMasterIndex + direction;
    if (newIndex >= 0 && newIndex < masterImgs.length) {
        await scrollToMaster(newIndex);
    }
}
// --- 4. Nút bấm Trái/Phải cho Thumbnail ---
function scrollThumb(direction) {
    const scrollAmount = 200;
    thumbContainer.scrollBy({
        left: direction * scrollAmount,
        behavior: 'smooth'
    });
}

// Zoom
// 1. Hàm tính toán ranh giới ảnh khi phóng to
function getBounds() {
    // Phải lấy kích thước sau khi đã scale
    const rect = overlayImg.getBoundingClientRect();
    const winW = window.innerWidth;
    const winH = window.innerHeight;

    // Nếu ảnh sau khi zoom vẫn nhỏ hơn màn hình thì không cho kéo
    const limitX = Math.max(0, (rect.width - winW) / 2);
    const limitY = Math.max(0, (rect.height - winH) / 2);

    return { limitX, limitY };
}

// 2. Hàm tính lực cản khi kéo quá biên
function applyResistance(currentPos, limit) {
    if (Math.abs(currentPos) <= limit) return currentPos;

    const overflow = Math.abs(currentPos) - limit;
    // Công thức tạo độ nặng khi kéo quá biên
    const resistance = overflow / (1 + (overflow / (limit * RESISTANCE_FACTOR || 100)));

    return currentPos > 0 ? limit + resistance : -limit - resistance;
}

// 3. Hàm cập nhật CSS Variables
function updateZoomStyles(withTransition = false) {
    if (withTransition) {
        overlayImg.classList.remove('is-dragging');
    } else {
        overlayImg.classList.add('is-dragging');
    }

    // Đảm bảo các giá trị luôn là số hợp lệ
    const x = Number(translateX) || 0;
    const y = Number(translateY) || 0;
    const s = isZoomed ? ZOOM_LEVEL : 1;

    overlayImg.style.setProperty('--zoom-x', `${x}px`);
    overlayImg.style.setProperty('--zoom-y', `${y}px`);
    overlayImg.style.setProperty('--zoom-scale', s);
}

// 4. Hàm thoát chế độ Zoom và reset vị trí
function exitZoom() {
    isZoomed = false;
    isDraggingZoom = false;
    movedZoom = false;
    translateX = 0;
    translateY = 0;

    overlayImg.classList.remove('is-zoomed');
    // Bật transition để ảnh thu nhỏ lại mượt mà
    updateZoomStyles(true);
}

// 5. Hàm thay đổi ảnh (Dành cho nút bấm - có Animation Fade)
function changeOverlayImage(step, event) {
    if (event) event.stopPropagation();

    // Tạo hiệu ứng mờ dần trước khi đổi src
    overlayImg.classList.add('fade-transition');

    setTimeout(() => {
        exitZoom(); // Reset về trạng thái zoom out

        if (!currentProduct.masterFiles || currentProduct.masterFiles.length === 0) return;
        
        overlayIndex = (overlayIndex + step + currentProduct.masterFiles.length) % currentProduct.masterFiles.length;
        
        // Build the image path directly from masterFiles
        const folderPath = currentProduct.image_folder_path;
        const fileName = currentProduct.masterFiles[overlayIndex];
        const imagePath = joinUrl(API_BASE_URL, `/${folderPath}/master/${fileName}`);
        
        overlayImg.src = imagePath;

        // Khi ảnh mới load xong thì hiện lên lại
        overlayImg.onload = () => {
            overlayImg.classList.remove('fade-transition');
            updateOverlayCounter();
        };
    }, 50);
}

// 6. Hàm xử lý trượt quán tính
function applyInertia() {
    const bounds = getBounds();

    // Ma sát: giảm dần vận tốc
    translateX += velX * 15;
    translateY += velY * 15;
    velX *= 0.92;
    velY *= 0.92;

    // Kiểm tra nếu chạm biên thì khựng lại ngay (Snap lập tức)
    if (Math.abs(translateX) > bounds.limitX) {
        translateX = Math.max(-bounds.limitX, Math.min(bounds.limitX, translateX));
        velX = 0; // Triệt tiêu vận tốc ngang ngay khi chạm biên
    }
    if (Math.abs(translateY) > bounds.limitY) {
        translateY = Math.max(-bounds.limitY, Math.min(bounds.limitY, translateY));
        velY = 0; // Triệt tiêu vận tốc dọc
    }

    updateZoomStyles(false);

    if (Math.abs(velX) > 0.1 || Math.abs(velY) > 0.1) {
        rafInertia = requestAnimationFrame(applyInertia);
    } else {
        // Snap lần cuối để đảm bảo vị trí chuẩn xác
        updateZoomStyles(true);
    }
}

// 7. Xử lý khi nhấn chuột/tay xuống
function handleStart(e) {
    // Phân biệt tọa độ giữa Chuột và Cảm ứng
    const clientX = e.type.includes('touch') ? e.touches[0].clientX : e.clientX;
    const clientY = e.type.includes('touch') ? e.touches[0].clientY : e.clientY;

    isDraggingZoom = true;
    dragStartTime = Date.now();
    movedZoom = false;
    cancelAnimationFrame(rafInertia); // Dừng quán tính cũ nếu đang chạy

    startXRaw = clientX;
    startYRaw = clientY;

    // Lưu vị trí tương đối để tính toán di chuyển
    startXZoom = clientX - translateX;
    startYZoom = clientY - translateY;

    // Các biến phục vụ quán tính
    lastX = clientX;
    lastY = clientY;
    lastTime = Date.now();
    velX = 0;
    velY = 0;

    // Tắt transition ngay lập tức để kéo dính tay (không bị delay)
    updateZoomStyles(false);
}

// 8. Xử lý khi di chuyển chuột/tay

function handleMove(e) {
    if (!isDraggingZoom) return;

    const clientX = e.type.includes('touch') ? e.touches[0].clientX : e.clientX;
    const clientY = e.type.includes('touch') ? e.touches[0].clientY : e.clientY;

    const deltaX = clientX - startXRaw;
    const deltaY = clientY - startYRaw;

    // NGƯỠNG NHẬN DIỆN KÉO: Nếu lệch quá 5px, chắc chắn là đang kéo
    if (Math.abs(deltaX) > 5 || Math.abs(deltaY) > 5) {
        movedZoom = true; 
    }

    if (isZoomed) {
        const bounds = getBounds();
        translateX = applyResistance(clientX - startXZoom, bounds.limitX);
        translateY = applyResistance(clientY - startYZoom, bounds.limitY);
    } else {
        // Logic khóa hướng đơn trục bạn đã có...
        if (!lockDirection) {
            if (Math.abs(deltaX) > 15) lockDirection = 'horizontal';
            else if (Math.abs(deltaY) > 15) lockDirection = 'vertical';
        }
        if (lockDirection === 'horizontal') { translateX = deltaX; translateY = 0; }
        else if (lockDirection === 'vertical') { translateX = 0; translateY = deltaY; }
    }
    updateZoomStyles(false);
}

// 9. Xử lý khi thả chuột/tay (Gắn vào window để an toàn)
function handleEnd(e) {
    
    if (!isDraggingZoom) return;
    isDraggingZoom = false;

    // QUAN TRỌNG: Chặn các sự kiện mouseup/click giả lập sau khi touchend
    if (e && e.type === 'touchend') {
        // Chỉ preventDefault nếu đó là một cú Tap (không phải di chuyển)
        if (!movedZoom) {
            if (e.cancelable) e.preventDefault();
        }
    }
    // Nếu movedZoom là false -> Người dùng chỉ chạm/nhấn rồi thả tại chỗ -> CLICK
    // Nếu movedZoom là true -> Người dùng đã rê chuột/tay đi chỗ khác -> DRAG
    if (!movedZoom) {
        // CHẾ ĐỘ NHẤN (TOGGLE ZOOM)
        toggleZoom();
    } else {
        // CHẾ ĐỘ KÉO (DRAG/SWIPE)
        if (isZoomed) {
            applyInertia();
        } else {
            const absX = Math.abs(translateX);
            const absY = Math.abs(translateY);
            if (absY > SWIPE_THRESHOLD && absY > absX) closeOverlay();
            else if (absX > SWIPE_THRESHOLD) changeOverlayImage(translateX > 0 ? -1 : 1);
            else exitZoom();
        }
    }
    
    lockDirection = null;
    // Lưu ý: KHÔNG reset movedZoom ở đây, hãy reset nó ở đầu hàm openOverlay và handleStart
}
// Tạo hàm toggleZoom riêng để quản lý trạng thái
function toggleZoom() {
    isZoomed = !isZoomed;
    if (!overlayImg) return;

    if (isZoomed) {
        overlayImg.classList.add('is-zoomed');
    } else {
        overlayImg.classList.remove('is-zoomed');
    }
    translateX = 0;
    translateY = 0;
    updateZoomStyles(true);
}

// 10. ĐĂNG KÝ CÁC EVENT LISTENERS
// Gán mousedown trực tiếp lên ảnh
overlayImg.addEventListener('mousedown', (e) => {
    e.preventDefault(); // Chặn drag mặc định của trình duyệt
    handleStart(e);
});

// Gán move và up lên WINDOW để không bị mất dấu khi rê chuột nhanh
window.addEventListener('mousemove', (e) => {
    if (isDraggingZoom) {
        handleMove(e);
    }
});

window.addEventListener('mouseup', (e) => {
    if (isDraggingZoom) {
        handleEnd(e);
    }
});

// Đối với Mobile
overlayImg.addEventListener('touchstart', (e) => {
    handleStart(e);
}, { passive: true });

overlayImg.addEventListener('touchmove', (e) => {
    handleMove(e);
}, { passive: false });

window.addEventListener('touchend', handleEnd);

async function loadProductDetail() {
    try {
        loadingMessage.style.display = 'block';
        errorMessage.style.display = 'none';
        
        let product;

        if (window.INITIAL_PRODUCT_DATA) {        
            product = window.INITIAL_PRODUCT_DATA;
        } else {
            // Vì lý do nào đó không nhúng được product thì gọi lại api
            // Hằng số từ Thymeleaf trống, ta tự lấy từ URL
            const slug = typeof CURRENT_PRODUCT_SLUG !== 'undefined' ? CURRENT_PRODUCT_SLUG : getSlugFromUrl();
            const PRODUCT_API_URL = joinUrl(API_BASE_URL, '/api/products/detail');

            const response = await fetch(`${PRODUCT_API_URL}/${slug}`);

            if (response.status === 404) {
                throw new Error("Không tìm thấy sản phẩm này.");
            }
            if (!response.ok) {
                throw new Error(`Lỗi HTTP: ${response.status}`);
            }

            product = await response.json();
        }
        currentProduct = product; // Lưu sản phẩm vào biến toàn cục
        // Cập nhật DOM với dữ liệu sản phẩm
        detailName.textContent = product.name;
        detailPrice.textContent = product.price.toLocaleString('vi-VN') + ' VNĐ';
        detailDescription.textContent = product.description;

        loadingMessage.style.display = 'none';

        // MỚI: Gọi hàm render gallery sau khi có dữ liệu
        if (product.masterFiles && product.masterFiles.length > 0) {
            renderGallery(product);
        }
    } catch (error) {
        console.error("Lỗi khi tải chi tiết sản phẩm:", error);
        loadingMessage.style.display = 'none';
        errorMessage.style.display = 'block';
        errorMessage.textContent = `Lỗi: ${error.message || "Không thể tải chi tiết sản phẩm từ Server."}`;
    }
}
// Lấy slug từ URL phòng INITIAL_PRODUCT_DATA không lấy được product
function getSlugFromUrl() {
    // window.location.pathname trả về "/products/laptop-dell"
    const path = window.location.pathname;
    const pathParts = path.split('/');
    // Lấy phần tử cuối cùng của mảng đường dẫn
    return pathParts[pathParts.length - 1];
}

// Hàm xử lý nút bấm
btnIncreaseQty.addEventListener('click', () => {
    let currentQty = parseInt(detailQuantityInput.value) || 1;
    detailQuantityInput.value = currentQty + 1;
});

btnDecreaseQty.addEventListener('click', () => {
    let currentQty = parseInt(detailQuantityInput.value) || 1;
    if (currentQty > 1) {
        detailQuantityInput.value = currentQty - 1;
    }
});

/**
 * Hàm xử lý sự kiện "Thêm vào Giỏ" từ trang chi tiết
 */
addToCartBtn.addEventListener('click', async () => {
    const CART_ADD_URL = joinUrl(API_BASE_URL, '/api/cart/add');
    if (!currentProduct) {
        alert("Không có sản phẩm để thêm vào giỏ!");
        return;
    }

    // FIX 1: Luôn ép kiểu sang Number ngay từ đầu
    const quantity = parseInt(detailQuantityInput.value) || 1;

    const isLoggedIn = document.querySelector('.user-logged') !== null;

    if (!isLoggedIn) {
        // CHẾ ĐỘ KHÁCH: Lưu LocalStorage
        saveToLocalStorage(currentProduct, quantity);
        alert("Đã thêm vào giỏ hàng tạm thời!");
    } else {
        // CHẾ ĐỘ THÀNH VIÊN: Gọi API cũ của bạn
        try {
            const response = await fetch(CART_ADD_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    productId: currentProduct.id,
                    quantity: quantity
                })
            });

            if (response.ok) {
                // FIX 2: Cập nhật cache local cho User
                let currentCount = parseInt(localStorage.getItem('user_cart_count') || 0);
                localStorage.setItem('user_cart_count', currentCount + quantity);
                alert("Đã thêm vào giỏ hàng!");
            } else {
                const errorData = await response.json(); // Cố gắng đọc lỗi từ server
                alert(`Lỗi khi thêm sản phẩm vào giỏ hàng: ${errorData.message || 'Lỗi không xác định.'}`);
            }
        } catch (error) {
            console.error("Lỗi POST API:", error);
            alert("Lỗi kết nối Server khi thêm giỏ hàng.");
        }
    }
    window.dispatchEvent(new Event('cartUpdated'));

});

// Chạy hàm khi DOM đã sẵn sàng
document.addEventListener('DOMContentLoaded', () =>{
     loadProductDetail();
     // 1. Xử lý mở Overlay khi click vào ảnh chính (Xử lý lỗi masterImg undefined)
    if (masterContainer) {
        masterContainer.addEventListener('click', (e) => {
            // Kiểm tra nếu click trúng vào ảnh master
            if (e.target.classList.contains('master-item')) {
                openOverlay(); // Gọi hàm đã có của bạn
            }
        });
    }

    // 2. Xử lý đóng Overlay
    if (closeOverlayBtn) {
        closeOverlayBtn.addEventListener('click', closeOverlay);
    }

    // 3. Xử lý chuyển ảnh trong Overlay
    if (overlayBtnPrev) {
        overlayBtnPrev.addEventListener('click', () => changeOverlayImage(-1));
    }
    if (overlayBtnNext) {
        overlayBtnNext.addEventListener('click', () => changeOverlayImage(1));
    }

    // 4. Đóng overlay khi click ra ngoài vùng ảnh
    if (imageOverlay) {
        imageOverlay.addEventListener('click', (e) => {
            if (e.target === imageOverlay || e.target === overlayImageContainer) {
                closeOverlay();
            }
        });
    }
});