// Tạo một getter/setter để theo dõi biến
/* let _internalCart = [];
Object.defineProperty(window, 'currentCartItems', {
    get: function () { return _internalCart; },
    set: function (value) {
        console.log('%c BIẾN currentCartItems ĐANG BỊ THAY ĐỔI! ', 'background: #222; color: #bada55');
        console.log('Giá trị mới:', value);
        console.trace(); // IN RA TOÀN BỘ LUỒNG CHẠY DẪN ĐẾN DÒNG NÀY
        _internalCart = value;
    }
}); */

const cartItemsContainer = document.getElementById('cart-items');
const subTotalElement = document.getElementById('sub-total');
const finalTotalElement = document.getElementById('final-total');
const checkoutButton = document.getElementById('checkout-button');

let currentCartItems = []; // Lưu trữ dữ liệu giỏ hàng
window.currentCartItems = currentCartItems; // Ép vào global

/**
 * Định dạng số thành tiền tệ VNĐ
 */
function formatCurrency(amount) {
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(amount);
}

/**
 * Hiển thị giỏ hàng lên giao diện
 */
function renderCart() {
    //console.log(window.currentCartItems)
    cartItemsContainer.innerHTML = '';
    // 1. Tạo một biến chứa toàn bộ HTML
    let listHtml = '';
    let totalAllProducts = 0;

    if (window.currentCartItems.length === 0) {
        cartItemsContainer.innerHTML = '<p class="cart-empty">Giỏ hàng của bạn đang trống.</p>';
        checkoutButton.disabled = true;
        subTotalElement.textContent = formatCurrency(0);
        finalTotalElement.textContent = formatCurrency(0);
        return;
    }

    window.currentCartItems.forEach(item => {

        // --- Đảm bảo các trường này đã được lấy từ DTO ---
        const cid = item.id;
        const pid = item.productId;
        const productLink = `/products/${item.productSlug}`;
        const unitPrice = item.unitPrice || 0;
        const productName = item.productName || "Sản phẩm không rõ";
        const imageUrl = item.imageUrl || "";
        const itemPrice = unitPrice * item.quantity;
        totalAllProducts += itemPrice;
        // --------------------------------------------------
        const fullImageUrl = joinUrl(API_BASE_URL, imageUrl);

        // Cộng dồn vào biến listHtml
        listHtml += `
            <div class="cart-item">
                <div class="cart-item-image">
                    <img src="${fullImageUrl}" alt="${productName}" class="cart-item-image">
                </div>
                
                <div class="cart-item-details">
                    <a href="${productLink}">
                        <h3 class="cart-item-name">${productName}</h3>
                    </a>
                    
                    <p class="cart-item-unit-price">Giá đơn vị: 
                        <strong>${formatCurrency(unitPrice)}</strong>
                    </p>
                    
                    <div class="cart-item-actions">
                        
                        <div class="quantity-control">
                            <label class="label-qty-control">Số lượng:</label>
                            
                            <button 
                                    class="btn-qty-control btn-decrease" 
                                    onclick="updateQuantity(${item.quantity - 1}, ${pid}, '${cid}')" 
                                    ${item.quantity <= 1 ? 'disabled' : ''}>
                                    -
                                </button>

                                <span class="cart-item-quantity">${item.quantity}</span>
                                
                                <button 
                                    class="btn-qty-control btn-increase" 
                                    onclick="updateQuantity(${item.quantity + 1}, ${pid}, '${cid}')">
                                    +
                                </button>
                            
                        </div>

                        <div class="cart-item-subtotal">
                            <p>Thành tiền: <strong class="price-total product-price">${formatCurrency(itemPrice)}</strong></p>
                        </div>
                    </div>
                    
                    <button class="cart-item-remove" onclick="removeCartItem(${pid}, '${cid}')">
                            <i class="fa-regular fa-trash-can"></i> 
                            <span class="item-remove-text">Xóa khỏi giỏ</span> 
                        </button>
                </div>
            </div>
        `;
    });

    // CHỈ gọi innerHTML một lần duy nhất tại đây
    cartItemsContainer.innerHTML = listHtml;

    // Cập nhật tổng tiền
    // 1. Tính toán các chi phí phụ
    const shippingFee = (totalAllProducts > 500000 || totalAllProducts === 0) ? 0 : 30000;
    const finalTotal = totalAllProducts + shippingFee;

    // 2. Lấy các phần tử HTML (Hãy chắc chắn ID này khớp với file .php)
    const subTotalElem = document.getElementById('sub-total');
    const shippingElem = document.getElementById('shipping-fee');
    const finalTotalElem = document.getElementById('final-total');

    // 3. Ghi giá trị vào HTML
    if (subTotalElem) {
        subTotalElem.textContent = formatCurrency(totalAllProducts);
    }

    if (shippingElem) {
        shippingElem.textContent = shippingFee === 0 ? "Miễn phí" : formatCurrency(shippingFee);
    }

    if (finalTotalElem) {
        finalTotalElem.textContent = formatCurrency(finalTotal);
    }
    checkoutButton.disabled = false;
}

async function renderGuestCart() {
    const guestItems = JSON.parse(localStorage.getItem('guest_cart')) || [];

    if (!guestItems || guestItems.length === 0) {
        window.currentCartItems = [];
        renderCart(); // Gọi renderCart gốc để hiện chữ "Giỏ hàng trống"
        return;
    }

    try {
        // 1. Lấy danh sách ID sản phẩm từ LocalStorage
        const productIds = guestItems.map(item => item.productId);
        const PRODUCT_API_URL = joinUrl(API_BASE_URL, '/api/products/list-by-ids');

        // 2. Gọi API để lấy chi tiết các sản phẩm này (Bạn cần tạo API này ở Backend)
        const response = await fetch(PRODUCT_API_URL, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(productIds)
        });

        if (response.ok) {
            const productsInfo = await response.json();

            // 3. Hợp nhất dữ liệu (Merge): Lấy thông tin sản phẩm gán vào số lượng trong giỏ khách
            window.currentCartItems = guestItems.map(guestItem => {
                const info = productsInfo.find(p => p.id === guestItem.productId);
                return {
                    productId: guestItem.productId,
                    quantity: guestItem.quantity,
                    productName: info ? info.name : "Sản phẩm đã ngừng bán",
                    unitPrice: info ? info.price : 0,
                    imageUrl: info ? info.imageUrl : "",
                    productSlug: info ? info.slug : guestItem.productSlug
                };
            });

            // 4. Gọi hàm renderCart chung để hiển thị lên UI
            renderCart();
        }
    } catch (error) {
        console.error("Lỗi khi lấy thông tin sản phẩm khách:", error);
        cartItemsContainer.innerHTML = '<p>Không thể tải thông tin sản phẩm.</p>';
    }
}

/**
 * Tải giỏ hàng từ Back-end
 */
async function loadCart() {
    const isLoggedIn = document.querySelector('.user-logged') !== null;
    if (!isLoggedIn) {
        renderGuestCart();
        return;
    }

    try {
        const CART_API_URL = joinUrl(API_BASE_URL, '/api/cart/my-cart');
        const response = await fetch(CART_API_URL);
        if (!response.ok) {
            throw new Error('Lỗi khi tải giỏ hàng.');
        }
        window.currentCartItems = await response.json();
        renderCart();
    } catch (error) {
        console.error("Lỗi:", error);
        alert('Không thể kết nối đến server hoặc lỗi tải giỏ hàng.');
    }
}

/**
 * Cập nhật số lượng sản phẩm trong giỏ hàng
 */
async function updateQuantity(newQuantity, productId, cartItemId) {
    // FIX 4: Ép kiểu Number ngay lập tức để tránh lỗi cộng chuỗi (ví dụ: "4" + 1 = "41")
    const qty = parseInt(newQuantity);
    if (isNaN(qty) || qty < 1) {
        return;
    }

    const isLoggedIn = checkUserLoginStatus();
    // 1. CẬP NHẬT BIẾN HIỂN THỊ (Dùng chung cho cả 2 để UI nhảy ngay lập tức)
    // Dùng == để khớp ID chuỗi/số như bạn đã xử lý
    const itemInUI = window.currentCartItems.find(i =>
        isLoggedIn ? (i.id == cartItemId) : (i.productId == productId)
    );

    if (itemInUI) {
        itemInUI.quantity = qty;
        renderCart(); // Vẽ lại bảng ngay lập tức, không chờ API
    }

    // 2. CẬP NHẬT DỮ LIỆU "CỨNG"
    if (!isLoggedIn) {
        // Chế độ khách: Lưu Local
        let cart = JSON.parse(localStorage.getItem('guest_cart')) || [];
        const item = cart.find(i => i.productId == productId);
        if (item) {
            item.quantity = qty;
            localStorage.setItem('guest_cart', JSON.stringify(cart));
        }
    } else {
        // Chế độ người dùng: Gọi API và cập nhật cache Badge
        try {
            const response = await fetch(`${API_BASE_URL}/api/cart/update/${cartItemId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ quantity: qty })
            });

            if (response.ok) {
                // Tính lại tổng số lượng để lưu vào cache cho Header
                const newTotalCount = window.currentCartItems.reduce((sum, i) => sum + i.quantity, 0);
                localStorage.setItem('user_cart_count', newTotalCount);
            }
        } catch (error) {
            console.error("Lỗi cập nhật User:", error);
        }
    }

    // 3. THÔNG BÁO HEADER CẬP NHẬT
    window.dispatchEvent(new Event('cartUpdated'));
}

function renderCartTotals() {
    let totalAllProducts = window.currentCartItems.reduce((sum, item) => {
        return sum + (item.unitPrice * item.quantity);
    }, 0);

    // Cập nhật lên HTML
    if (subTotalElement) subTotalElement.textContent = formatCurrency(totalAllProducts);
    if (finalTotalElement) finalTotalElement.textContent = formatCurrency(totalAllProducts);
}

/**
 * Xóa một sản phẩm khỏi giỏ hàng
 */
async function removeCartItem(productId, cartItemId) {
    if (!confirm("Bạn có chắc chắn muốn xóa sản phẩm này?")) {
        return;
    }

    const isLoggedIn = checkUserLoginStatus();

    // 1. Xóa ngay lập tức trên mảng hiển thị (UI)
    window.currentCartItems = window.currentCartItems.filter(i =>
        isLoggedIn ? (i.id != cartItemId) : (i.productId != productId)
    );
    renderCart(); // Vẽ lại bảng ngay lập tức

    // 2. Cập nhật "Kho" dữ liệu
    if (!isLoggedIn) {
        let cart = JSON.parse(localStorage.getItem('guest_cart')) || [];
        const newCart = cart.filter(i => i.productId != productId);
        localStorage.setItem('guest_cart', JSON.stringify(newCart));
    } else {
        try {
            const CART_API_URL = joinUrl(API_BASE_URL, '/api/cart/remove');

            const response = await fetch(`${CART_API_URL}/${cartItemId}`, {
                method: 'DELETE'
            });

            if (response.ok) {
                // Tính lại tổng số lượng cho User sau khi xóa
                const newTotal = window.currentCartItems.reduce((sum, i) => sum + i.quantity, 0);
                localStorage.setItem('user_cart_count', newTotal);
            }
        } catch (error) {
            console.error("Lỗi xóa hàng User:", error);
        }
    }

    // 3. Đồng bộ Header
    window.dispatchEvent(new Event('cartUpdated'));
}

/**
 * Xử lý sự kiện nút THANH TOÁN
 */
async function handleCheckout() {
    if (window.currentCartItems.length === 0) {
        alert("Giỏ hàng trống!");
        return;
    }

    checkoutButton.disabled = true; // Vô hiệu hóa nút để tránh double click

    try {
        const CHECKOUT_API_URL = joinUrl(API_BASE_URL, '/api/checkout');
        const response = await fetch(CHECKOUT_API_URL, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            // Giả định gửi một số thông tin thanh toán (có thể bỏ trống nếu Back-end không yêu cầu)
            body: JSON.stringify({ /* paymentMethod: "CASH_ON_DELIVERY" */ })
        });

        if (response.ok) {
            const orderResult = await response.json();
            
            const isLoggedIn = checkUserLoginStatus();
            
            if (!isLoggedIn) {
                // Clear guest cart from localStorage
                localStorage.removeItem('guest_cart');
            } else {
                // Reset user cart count to 0
                localStorage.setItem('user_cart_count', 0);
            }
            
            // Clear the display data
            window.currentCartItems = [];
            
            // Update the badge counter to 0
            window.dispatchEvent(new Event('cartUpdated'));
            
            alert(`Đặt hàng thành công! Mã đơn hàng: ${orderResult.id}. Tổng tiền: ${formatCurrency(orderResult.totalAmount)}`);
            
            // Redirect after clearing data
            window.location.href = 'success?orderId=' + orderResult.id;

        } else {
            const errorData = await response.json();
            alert(`Thanh toán thất bại: ${errorData.message || 'Lỗi không xác định'}`);
            checkoutButton.disabled = false;
        }

    } catch (error) {
        console.error("Lỗi thanh toán:", error);
        alert('Lỗi kết nối Server khi thanh toán.');
        checkoutButton.disabled = false;
    }
}


// Khởi tạo
document.addEventListener('DOMContentLoaded', () => {
    loadCart();
    checkoutButton.addEventListener('click', handleCheckout);
});