// (Giả sử bạn đã nhúng config.inc.php và có thể truy cập biến API_BASE_URL)
// Trong môi trường PHP/HTML, bạn thường phải in biến này ra JS.
// Tuy nhiên, để đơn giản, ta sẽ khai báo trực tiếp URL ở đây.

/**
 * Hàm tạo HTML cho một sản phẩm
 * @param {object} product - Đối tượng sản phẩm từ API
 * @returns {string} Chuỗi HTML cho thẻ sản phẩm
 */
function createProductCard(product) {
    const productData = JSON.stringify(product).replace(/"/g, '&quot;');
    const fullImageUrl = joinUrl(API_BASE_URL, product.imageUrl);
    const productLink = `/products/${product.slug}`;
    return `
        <div class="product-card">
            <a href="${productLink}">
                <div class="homepage-product-image">
                    <img src="${fullImageUrl}" alt="${product.name}" class="homepage-product-image">
                </div>
                
                <h3 class="homepage-product-name">${product.name}</h3>
            </a>
            
            <p class="product-price-block product-price">${product.price.toLocaleString('vi-VN')} VNĐ</p>
            
            <button 
                class="homepage-add-cart" 
                data-product-slug="${product.id}"
                onclick="addToCartFromHomepage(${productData}, 1)">
                Thêm vào Giỏ
            </button>
        </div>
    `;
}

/**
 * Hàm gọi API và hiển thị danh sách sản phẩm
 */
async function loadProducts(collectionId = null) {
    try {
        CONTAINER.innerHTML = '<p>Đang tải dữ liệu...</p>';

        let url = joinUrl(API_BASE_URL, '/api/products');
        if (collectionId) {
            url = `${API_BASE_URL}/api/collections/${collectionId}/products`;
        }
        
        const PRODUCT_API_URL = url;
        // 1. Gọi API GET từ Spring Boot
        const response = await fetch(PRODUCT_API_URL);
        
        if (!response.ok) {
            throw new Error(`Lỗi HTTP: ${response.status}`);
        }
        
        // 2. Chuyển đổi JSON thành đối tượng JavaScript
        const products = await response.json();
        
        // 3. Xây dựng HTML và hiển thị
        let htmlContent = products.map(product => createProductCard(product)).join('');
        
        CONTAINER.innerHTML = htmlContent;

    } catch (error) {
        console.error("Lỗi khi tải sản phẩm:", error);
        CONTAINER.innerHTML = '<p class="text-danger">Không thể tải dữ liệu sản phẩm từ Server.</p>';
    }
}

/**
 * Hàm xử lý sự kiện "Thêm vào Giỏ"**/
// homepage.js

async function addToCartFromHomepage(product, quantity) {
    const isLoggedIn = checkUserLoginStatus();
    quantity = parseInt(quantity);

    if (!isLoggedIn) {
        // CHẾ ĐỘ KHÁCH: Dùng hàm bổ trợ đã có trong common.js
        saveToLocalStorage(product, quantity);
        alert("Đã thêm vào giỏ hàng!");
    } else {
        // CHẾ ĐỘ THÀNH VIÊN
        try {
            const response = await fetch(joinUrl(API_BASE_URL, '/api/cart/add'), {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ productId: product.id, quantity: quantity })
            });
            if (response.ok) {
                // Đồng bộ cache: Lấy số cũ + số mới thêm
                let current = parseInt(localStorage.getItem('user_cart_count') || 0);
                localStorage.setItem('user_cart_count', current + quantity);
                alert("Đã thêm vào giỏ hàng!");
            }
        } catch (error) { console.error("Lỗi API:", error); }
    }

    // BƯỚC QUAN TRỌNG NHẤT: Phát tín hiệu để common.js tự lo phần hiển thị
    window.dispatchEvent(new Event('cartUpdated'));
}

function moveSlide(direction) {
    const container = document.getElementById('product-list-container');
    
    // Lấy chiều rộng của 1 thẻ sản phẩm (bao gồm cả gap)
    const card = container.querySelector('.product-card');
    if (!card) return; // Nếu chưa có sản phẩm thì không làm gì
    
    const cardWidth = card.offsetWidth + 7; // Chiều rộng thẻ + gap
    
    const maxScroll = container.scrollWidth - container.clientWidth;

    if (direction === 1) {
        // Nếu đang ở sát nút cuối cùng (hoặc vượt quá), quay về 0
        if (container.scrollLeft >= maxScroll - 5) { 
            container.scrollLeft = 0;
        } else {
            container.scrollLeft += cardWidth;
        }
    } else {
        // Nếu đang ở đầu trang, nhảy đến cuối trang
        if (container.scrollLeft <= 5) {
            container.scrollLeft = maxScroll;
        } else {
            container.scrollLeft -= cardWidth;
        }
    }
}

// Chạy hàm khi DOM đã sẵn sàng
document.addEventListener('DOMContentLoaded', () => {
    // Chỉ chạy nếu trang có cái container chứa danh sách sản phẩm
    if (document.getElementById('product-list-container')) {
        loadProducts();
    }
});