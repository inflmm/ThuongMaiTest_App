

//const API_BASE_URL = 'http://localhost:8080';
const API_BASE_URL = 'http://192.168.1.18:8080';

// Gắn biến api ở mức cấp cao nhất, phòng không lấy được const API_BASE_URL
window.API_BASE_URL = window.API_BASE_URL || API_BASE_URL;

const CONTAINER = document.getElementById('product-list-container');

// Hàm xử lý nối URL
function joinUrl(base, path) {
    if (!path) return base;
    return base.replace(/\/$/, '') + '/' + path.replace(/^\//, '');
}

window.addEventListener('scroll', function () {
    // 1. Lấy tất cả các Side Banner
    const banners = document.querySelectorAll('.side-banner');

    // 2. Kiểm tra vị trí cuộn của người dùng (tính bằng pixel)
    const scrollPos = window.scrollY;

    // 3. Nếu cuộn xuống quá 50px (chiều cao banner xanh chẳng hạn)
    if (scrollPos > 50) {
        banners.forEach(banner => {
            banner.classList.add('scrolled'); // Thêm class để đổi 'top' thành 100px
        });
    } else {
        banners.forEach(banner => {
            banner.classList.remove('scrolled'); // Xóa class để về lại 195px
        });
    }
});

function checkUserLoginStatus() {
    return document.querySelector('.user-logged') !== null;
}

let authMode = 'login'; // 'login' hoặc 'register'
let authType = 'email'; // 'email' hoặc 'phone'

// 1. Hàm bật/tắt Modal (Gọi khi click vào icon User)
function toggleAuthModal() {
    const modal = document.getElementById('auth-modal');
    modal.classList.toggle('active');
}

// 2. Chuyển đổi giữa nhập Email hoặc Số điện thoại
function toggleAuthType(type) {
    authType = type;
    const inputField = document.getElementById('auth-input');

    // Cập nhật giao diện nút
    document.querySelectorAll('.auth-toggle button').forEach(btn => btn.classList.remove('active'));
    event.target.classList.add('active');

    if (type === 'email') {
        inputField.placeholder = "Email của bạn";
        inputField.type = "email";
    } else {
        inputField.placeholder = "Số điện thoại của bạn";
        inputField.type = "tel";
    }
}

// 3. Chuyển đổi giữa chế độ Đăng nhập và Đăng ký
function switchMode() {
    const title = document.getElementById('auth-title');
    const btn = document.getElementById('btn-submit-auth');
    const switchText = event.target;

    if (authMode === 'login') {
        authMode = 'register';
        title.innerText = "Đăng ký tài khoản";
        btn.innerText = "Tạo tài khoản";
        switchText.innerText = "Đã có tài khoản? Đăng nhập";
    } else {
        authMode = 'login';
        title.innerText = "Đăng nhập";
        btn.innerText = "Xác nhận";
        switchText.innerText = "Chưa có tài khoản? Đăng ký ngay";
    }
}

// 4. Xử lý gửi dữ liệu (AJAX)
async function handleAuth() {
    const identifier = document.getElementById('auth-input').value;
    const password = document.getElementById('auth-password').value;

    if (!identifier || !password) {
        alert("Vui lòng nhập đầy đủ thông tin");
        return;
    }

    // Nếu là Đăng ký, gọi API bạn vừa viết ở AuthController
    if (authMode === 'register') {
        try {
            const response = await fetch('/api/auth/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    username: identifier,
                    password: password,
                    type: authType // 'email' hoặc 'phone'
                })
            });

            if (response.ok) {
                alert("Đăng ký thành công! Hãy đăng nhập.");
                switchMode(); // Quay lại màn hình đăng nhập
            } else {
                alert("Đăng ký thất bại. Tài khoản có thể đã tồn tại.");
            }
        } catch (error) {
            console.error("Error:", error);
        }
    } else {
        // LOGIC ĐĂNG NHẬP
        const params = new URLSearchParams();
        params.append('username', identifier);
        params.append('password', password);

        try {
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: params
            });

            if (response.ok) {
                // 1. Xóa cache cũ của người dùng trước đó (nếu có)
                localStorage.removeItem('user_cart_count');

                const guestCart = localStorage.getItem('guest_cart');
                if (guestCart) {
                    // Gửi merge nhưng không dùng alert() để khách không biết
                    fetch('/api/cart/merge', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: guestCart
                    }).then(() => {
                        // Xoá giỏ hàng khi đăng nhập
                        localStorage.removeItem('guest_cart');
                        location.reload();
                    }).catch(err => {
                        console.warn("Silent merge failed:", err);
                        location.reload();
                    });
                }
                alert("Đăng nhập thành công!");
                location.reload(); // Load lại trang để cập nhật trạng thái Header
            } else {
                alert("Sai tài khoản hoặc mật khẩu!");
            }
        } catch (error) {
            console.error("Login Error:", error);
        }
    }
}

// Đóng modal khi click ra ngoài
// common.js
window.onclick = function (event) {
    const modal = document.getElementById('auth-modal');
    // Thêm kiểm tra modal tồn tại để tránh lỗi "is null"
    if (modal && !event.target.closest('.action-item') && modal.classList.contains('active')) {
        modal.classList.remove('active');
    }
}

async function handleLogout() {
    try {
        const response = await fetch('/api/auth/logout', { method: 'POST' });
        if (response.ok) {
            alert("Đã đăng xuất!");
            location.href = '/homepage';
        }
    } catch (error) {
        console.error("Logout Error:", error);
    }
}

function getGuestId() {
    let guestId = localStorage.getItem('guest_id');
    if (!guestId) {
        // Tạo một chuỗi ngẫu nhiên đơn giản hoặc dùng UUID
        guestId = 'guest_' + Math.random().toString(36).substr(2, 9) + Date.now();
        localStorage.setItem('guest_id', guestId);
    }
    return guestId;
}
// Hàm bổ trợ lưu Local
function saveToLocalStorage(product, quantity) {
    let cart = JSON.parse(localStorage.getItem('guest_cart')) || [];
    const item = cart.find(i => i.productId == product.id);
    if (item) {
        item.quantity += quantity;
    } else {
        cart.push({
            productId: product.id,
            quantity: quantity,
            productSlug: product.slug
        });
    }
    localStorage.setItem('guest_cart', JSON.stringify(cart));
}


// common.js

// 1. Hàm thống nhất để lấy TỔNG số lượng từ Storage
function getCartCountFromStorage() {
    const isLoggedIn = checkUserLoginStatus();
    if (!isLoggedIn) {
        const cart = JSON.parse(localStorage.getItem('guest_cart')) || [];
        return cart.reduce((sum, item) => sum + item.quantity, 0);
    } else {
        // Luôn ưu tiên số lượng đã lưu trong cache cho User
        return parseInt(localStorage.getItem('user_cart_count') || 0);
    }
}

// 2. Hàm thống nhất để cập nhật giao diện Header (Dùng chung cho cả 2)
function refreshCartBadge() {
    const badge = document.querySelector('.cart-badge') || document.querySelector('#cart-count');
    if (!badge) return;
    
    badge.innerText = getCartCountFromStorage();
}

// 3. Lắng nghe sự kiện duy nhất
window.addEventListener('cartUpdated', refreshCartBadge);

// 4. Khi tải trang lần đầu
document.addEventListener('DOMContentLoaded', () => {
    const isLoggedIn = checkUserLoginStatus();
    // Nếu là User và chưa có cache, thì mới fetch API 1 lần duy nhất
    if (isLoggedIn && localStorage.getItem('user_cart_count') === null) {
        updateUserCartCountFromServer();
    } else {
        refreshCartBadge();
    }
});

// Hàm bổ trợ fetch số lượng từ server cho User (chỉ gọi khi cần thiết)
async function updateUserCartCountFromServer() {
    try {
        const response = await fetch(joinUrl(API_BASE_URL, '/api/cart/count'));
        if (response.ok) {
            const count = await response.json();
            localStorage.setItem('user_cart_count', count);
            refreshCartBadge();
        }
    } catch (e) { console.error(e); }
}