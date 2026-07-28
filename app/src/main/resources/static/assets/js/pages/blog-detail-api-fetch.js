/**
 * blog-detail-api-fetch.js
 * Chuyển đổi từ SSR sang CSR
 */

// Tận dụng lại cơ chế cache và tối ưu từ file cũ của bạn
document.addEventListener('DOMContentLoaded', async () => {
    // 1. Lấy slug từ URL (ví dụ: /blog/bai-viet-1 -> lấy 'bai-viet-1')
    const pathSegments = window.location.pathname.split('/');
    const slug = pathSegments[pathSegments.length - 1];

    if (!slug) return;

    try {
        // 2. Gọi API lấy chi tiết bài viết
        const response = await fetch(`${window.API_BASE_URL}/api/blogs/${slug}`);
        if (!response.ok) throw new Error("Không tìm thấy bài viết");

        const blog = await response.json();

        // 3. Render dữ liệu vào giao diện
        renderBlogData(blog);

        // 4. Gọi hàm khởi tạo Mục lục (ToC) từ logic tối ưu bạn đã viết
        // Lưu ý: Phải gọi sau khi blog-content đã có dữ liệu HTML
        if (typeof initBlogToC === 'function') {
            initBlogToC();
        }

    } catch (error) {
        console.error("Lỗi fetch blog:", error);
        document.getElementById('blog-content').innerHTML = "<p>Lỗi khi tải nội dung bài viết. Vui lòng thử lại sau.</p>";
    }
});

function renderBlogData(blog) {
    // Cập nhật các trường thông tin cơ bản
    document.title = blog.title;
    document.getElementById('breadcrumb-title').innerText = blog.title;
    document.getElementById('post-title').innerText = blog.title;
    
    // Định dạng ngày tháng
    if (blog.publishTime) {
        const date = new Date(blog.publishTime);
        document.getElementById('post-date').innerText = date.toLocaleDateString('vi-VN');
    }

    // Đổ nội dung HTML vào (tương đương th:utext)
    const contentArea = document.getElementById('blog-content');
    contentArea.innerHTML = blog.content;
}

/**
 * Quản lý Mục lục (ToC)
 */
// --- KHAI BÁO CÁC BIẾN TOÀN CỤC (Để cache và dùng chung) ---
let scrollObserver = null;
let tocLinks = []; // Cache cho các thẻ <a> trong ToC
let isScrollingUp = false;
let lastScrollTop = 0;
let lastRun = 0;
const fpsLimit = 16; // Tương đương ~60fps
let isManualScrolling = false;
let lastActiveLink = null; // Lưu trữ thẻ đang active hiện tại để tắt nhanh
const tocMap = new Map(); // Lưu trữ quan hệ ID -> Element <a>
let currentId = ""; // Biến dùng chung để chứa ID đang xử lý
let targetLink = null; // Biến dùng chung để chứa Element đang xử lý
let st = 0;
let entry = null;
let tempIndex = 0;
let i = 0;

function initBlogToC() {
    const content = document.getElementById('blog-content');
    if (!content) return;

    const headings = content.querySelectorAll('h2, h3, section h2, section h3');
    const staticList = document.querySelector('#static-toc .toc-content-list');
    const overlayList = document.querySelector('#toc-overlay-menu .toc-content-list');

    // 1. Xây dựng cấu trúc link
    headings.forEach((heading, i) => {
        // lấy id của heading hoặc tự thêm id vào nếu chưa có
        const id = heading.id || `section-idx-${i}`;
        heading.id = id;

        // gắn đường dẫn tới mục tương ứng theo id
        const link = document.createElement('a');
        link.href = `#${id}`;
        link.textContent = heading.innerText;
        link.className = heading.tagName.toLowerCase() === 'h3' ? 'toc-h3' : 'toc-h2';

        if (staticList) staticList.appendChild(link.cloneNode(true));
        if (overlayList) overlayList.appendChild(link.cloneNode(true));
    });

    // 2. Xử lý sự kiện Overlay
    const btnOpen = document.getElementById('toc-toggle-btn');
    const btnClose = document.getElementById('toc-close-btn');
    const menu = document.getElementById('toc-overlay-menu');
    const backdrop = document.getElementById('toc-backdrop');

    const toggleMenu = (isOpen) => {
        menu.classList.toggle('active', isOpen);
        backdrop.classList.toggle('active', isOpen);
    };

    if (btnOpen) btnOpen.onclick = () => toggleMenu(true);
    if (btnClose) btnClose.onclick = () => toggleMenu(false);
    if (backdrop) backdrop.onclick = () => toggleMenu(false); // Nhấn ra ngoài để tắt

    // Cập nhật Map và cache Links
    tocLinks = document.querySelectorAll('.toc-content-list a');
    tocLinks.forEach(a => {
        tocMap.set(a.getAttribute('href').slice(1), a); // slice 1 lấy tên id section-idx-1, a.getAttribute('href') trả ra #section-idx-1
    });

    setupSmartFeatures(headings);
}

/**
 * Xử lý Hiển thị Nút (Visibility) & Thanh Progress chuẩn
 */

function setupSmartFeatures(headings) {
    const blogContent = document.getElementById('blog-content');
    const tocButton = document.getElementById('toc-toggle-btn');
    if (!blogContent) return;

    // 2. Đưa các giá trị ít thay đổi ra ngoài sự kiện scroll (Cập nhật khi resize)
    let windowHeight = window.innerHeight;
    let middleScreen = windowHeight / 2;
    let contentHeight = blogContent.offsetHeight;
    let rect = blogContent.getBoundingClientRect();

    window.addEventListener('resize', () => {
        windowHeight = window.innerHeight;
        middleScreen = windowHeight / 2;
        contentHeight = blogContent.offsetHeight;
        rect = blogContent.getBoundingClientRect();
    });

    // 1. Đảm bảo Progress Bar nằm ở Body chứ không phải Overlay
    let progressBar = document.getElementById('read-progress-bar');
    if (!progressBar) {
        const container = document.createElement('div');
        container.id = 'read-progress-container';
        container.innerHTML = '<div id="read-progress-bar"></div>';
        document.body.prepend(container);
        progressBar = document.getElementById('read-progress-bar');
    }

    // 2. Cập nhật Progress Bar & ScrollSpy
    let ticking = false; // Biến khóa (Flag) để kiểm soát tần suất
    let now = 0;
    window.addEventListener('scroll', () => {
        now = performance.now();
        if (now - lastRun < fpsLimit) return; // Bỏ qua nếu chưa đủ thời gian chờ

        if (!ticking) {
            window.requestAnimationFrame(() => {
                if (!blogContent) return;

                rect = blogContent.getBoundingClientRect();
                st = window.pageYOffset || document.documentElement.scrollTop;

                // 1. Logic ẩn/hiện nút ToC
                if (rect.top < middleScreen && rect.bottom > middleScreen) {
                    tocButton.classList.add('visible');
                } else {
                    tocButton.classList.remove('visible');
                }

                // 2. Logic Progress Bar
                if (progressBar) {
                    // Thuật toán được tách tách ra
                    //let distanceRead = middleScreen - rect.top;
                    //let progress = Math.min(Math.max((distanceRead / contentHeight) * 100, 0), 100);
                    //progressBar.style.width = progress + "%";

                    // Thuật toán gốc
                    progressBar.style.width = Math.min(Math.max(((middleScreen - rect.top) / contentHeight) * 100, 0), 100) + "%";
                }


                ticking = false; // Mở khóa sau khi xử lý xong
                lastRun = performance.now();
            });

            ticking = true; // Khóa lại, không cho các sự kiện scroll khác chen vào
        }
    });

    // 4. Khởi tạo Observer
    // Chuyển headings thành mảng thực thụ MỘT LẦN DUY NHẤT để dùng .indexOf() không tốn RAM
    const headingsArray = Array.from(headings);
    
    // Giải phóng Observer cũ nếu có trước khi khởi tạo mới
    if (scrollObserver) scrollObserver.disconnect();

    scrollObserver = new IntersectionObserver((entries) => {
        st = window.pageYOffset || document.documentElement.scrollTop;
        isScrollingUp = st < lastScrollTop; // Xác định hướng cuộn ngay lập tức

        // 2. Tối ưu vòng lặp entries
        i = entries.length;
        while (i--) {
            entry = entries[i];

            if (entry.isIntersecting) {
                updateActiveLink(entry.target.id);
            }
            else if (isScrollingUp && entry.boundingClientRect.top > 0) {
                tempIndex = headingsArray.indexOf(entry.target);
                if (tempIndex > 0) {
                    updateActiveLink(headingsArray[tempIndex - 1].id);
                }
            }
        }
        lastScrollTop = st <= 0 ? 0 : st;
    }, {
        rootMargin: '-10% 0px -80% 0px',
        threshold: 0
    });

    headings.forEach(h => scrollObserver.observe(h));
}
// Hàm phụ trợ để cập nhật class active, giúp code sạch hơn
function updateActiveLink(id) {
    // Nếu ID không đổi thì không làm gì cả, tránh xử lý thừa
    if (currentId === id) return;
    currentId = id;

    // 1. Tắt active mục cũ ngay lập tức (Không cần quét mảng)
    if (lastActiveLink) {
        lastActiveLink.classList.remove('active-link');
    }

    // 2. Tìm đúng mục mới từ Map
    targetLink = tocMap.get(id);

    if (targetLink) {
        targetLink.classList.add('active-link');
        lastActiveLink = targetLink; // Cập nhật vùng dữ liệu dùng chung
    }
}