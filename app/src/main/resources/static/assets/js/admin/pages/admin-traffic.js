const TRAFFIC_PAGE_SIZE = 10;

// Biến lưu trữ state của Chart để hỗ trợ sự kiện hover & resize
let trafficChartData = [];
let activeChartHoverIndex = -1;
let chartResizeObserver = null;

function renderTrafficModule() {
    const root = document.getElementById('admin-app-root');
    const today = new Date();
    const endDate = toISODate(today);
    const startDate = toISODate(addDays(today, -6));

    root.innerHTML = `
        <div class="admin-table-container">
            <!-- Overview Cards -->
            <div id="traffic-overview" style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin-bottom: 20px;"></div>

            <!-- Filter Panel -->
            <div class="admin-panel" style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 14px; margin-bottom: 18px;">
                <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; align-items: end;">
                    <div style="display: flex; flex-direction: column; gap: 6px;">
                        <label style="font-size: 0.8rem; color: #334155; font-weight: 700;">Khoảng thời gian</label>
                        <select id="traffic-range-select" class="form-control" style="min-width: unset; background-color: #fff;">
                            <option value="7d" selected>7 ngày</option>
                            <option value="14d">2 tuần</option>
                            <option value="30d">1 tháng</option>
                            <option value="60d">2 tháng</option>
                            <option value="90d">3 tháng</option>
                            <option value="180d">6 tháng</option>
                            <option value="365d">1 năm</option>
                            <option value="custom">Tùy chọn</option>
                        </select>
                    </div>

                    <div style="display: flex; flex-direction: column; gap: 6px;">
                        <label style="font-size: 0.8rem; color: #334155; font-weight: 700;">Từ ngày</label>
                        <input id="traffic-start-date" type="date" value="${startDate}" class="form-control" style="min-width: unset; background-color: #fff;" />
                    </div>

                    <div style="display: flex; flex-direction: column; gap: 6px;">
                        <label style="font-size: 0.8rem; color: #334155; font-weight: 700;">Đến ngày</label>
                        <input id="traffic-end-date" type="date" value="${endDate}" class="form-control" style="min-width: unset; background-color: #fff;" />
                    </div>

                    <div style="display: flex; align-items: flex-end;">
                        <button id="traffic-apply-btn" class="btn-blog btn-create" style="width: 100%;">
                            <i class="fa-solid fa-filter"></i> Áp dụng
                        </button>
                    </div>
                </div>
            </div>

            <!-- Chart Section -->
            <div class="admin-panel" style="background: #ffffff; border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; margin-bottom: 20px;">
                <div style="font-weight: 700; margin-bottom: 12px; color: #2c3e50; font-size: 1.1rem;">Biểu đồ Traffic theo thời gian</div>
                <div style="position: relative; width: 100%;">
                    <canvas id="traffic-chart" style="display: block; width: 100%; height: 320px;"></canvas>
                </div>
            </div>

            <!-- Data Table Container (Đồng bộ cấu trúc như Admin Blog) -->
            <div class="table-header">
                <div id="table-folder-title" class="folder-title-text">Bảng dữ liệu chi tiết</div>
                <div id="traffic-range-label" style="color: #64748b; font-size: 0.9rem; font-weight: 600;">7 ngày</div>
            </div>

            <table>
                <thead>
                    <tr>
                        <th style="text-align: left;">Ngày</th>
                        <th style="text-align: right;">Lượt truy cập</th>
                        <th style="text-align: right;">Phiên</th>
                        <th style="text-align: right;">Trung bình / ngày</th>
                    </tr>
                </thead>
                <tbody id="traffic-table-body"></tbody>
            </table>

            <div class="table-footer">
                <div id="traffic-pagination" class="pagination-container"></div>
            </div>
        </div>
    `;

    document.getElementById('header-toolbox').innerHTML = `
        <button class="btn-blog btn-create" id="traffic-sync-btn" onclick="syncTrafficData()">
            <i class="fa-solid fa-arrows-rotate"></i> Đồng bộ dữ liệu
        </button>
    `;

    bindTrafficControls();
    initChartListeners();
    loadTrafficOverview();
}

function bindTrafficControls() {
    const rangeSelect = document.getElementById('traffic-range-select');
    const startInput = document.getElementById('traffic-start-date');
    const endInput = document.getElementById('traffic-end-date');
    const applyBtn = document.getElementById('traffic-apply-btn');

    if (!rangeSelect || !startInput || !endInput || !applyBtn) return;

    rangeSelect.addEventListener('change', () => {
        if (rangeSelect.value === 'custom') return;
        const days = parseRangeToDays(rangeSelect.value);
        const end = new Date();
        const start = addDays(end, -(days - 1));
        startInput.value = toISODate(start);
        endInput.value = toISODate(end);
    });

    applyBtn.addEventListener('click', () => {
        const selectedStart = startInput.value;
        const selectedEnd = endInput.value;

        if (!selectedStart || !selectedEnd) {
            alert('Vui lòng chọn đầy đủ ngày bắt đầu và ngày kết thúc.');
            return;
        }

        const start = new Date(selectedStart + 'T00:00:00');
        const end = new Date(selectedEnd + 'T00:00:00');

        if (start > end) {
            alert('Ngày bắt đầu không được lớn hơn ngày kết thúc.');
            return;
        }

        if (rangeSelect.value === 'custom' || !rangeSelect.value) {
            rangeSelect.value = '7d';
        }

        loadTrafficOverview({ startDate: selectedStart, endDate: selectedEnd });
    });
}

function parseRangeToDays(value) {
    switch (value) {
        case '7d': return 7;
        case '14d': return 14;
        case '30d': return 30;
        case '60d': return 60;
        case '90d': return 90;
        case '180d': return 180;
        case '365d': return 365;
        default: return 7;
    }
}

async function syncTrafficData() {
    const btn = document.getElementById('traffic-sync-btn');
    if (!btn) return;

    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Đang đồng bộ...';

    try {
        const response = await fetch(joinUrl(API_BASE_URL, '/api/internal/analytics/sync'), {
            method: 'POST',
            headers: csrfHeaders({
                'Content-Type': 'application/json',
                'X-Cron-Secret': window.ADMIN_CRON_SECRET || ''
            })
        });

        if (!response.ok) {
            await handleApiError(response);
            return;
        }

        await loadTrafficOverview();
        alert('Đồng bộ dữ liệu traffic thành công');
    } catch (error) {
        console.error('syncTrafficData error:', error);
        alert('Không thể đồng bộ dữ liệu traffic. Vui lòng kiểm tra secret key hoặc server log.');
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="fa-solid fa-arrows-rotate"></i> Đồng bộ dữ liệu';
    }
}

async function loadTrafficOverview(options = {}) {
    try {
        if (typeof AdminApp !== 'undefined' && AdminApp.showLoading) {
            AdminApp.showLoading(true);
        }

        const startInput = document.getElementById('traffic-start-date');
        const endInput = document.getElementById('traffic-end-date');

        const selectedStart = options.startDate || (startInput ? startInput.value : null);
        const selectedEnd = options.endDate || (endInput ? endInput.value : null);

        if (selectedStart && selectedEnd) {
            if (startInput) startInput.value = selectedStart;
            if (endInput) endInput.value = selectedEnd;
        }

        const query = new URLSearchParams();
        if (selectedStart) query.set('startDate', selectedStart);
        if (selectedEnd) query.set('endDate', selectedEnd);

        const dailyUrl = query.toString()
            ? `${joinUrl(API_BASE_URL, '/api/admin/analytics/daily')}?${query.toString()}`
            : joinUrl(API_BASE_URL, '/api/admin/analytics/daily?days=7');

        const [overviewRes, dailyRes] = await Promise.all([
            fetch(joinUrl(API_BASE_URL, '/api/admin/analytics/overview')),
            fetch(dailyUrl)
        ]);

        if (!overviewRes.ok) {
            await handleApiError(overviewRes);
            return;
        }
        if (!dailyRes.ok) {
            await handleApiError(dailyRes);
            return;
        }

        const overview = await overviewRes.json();
        const daily = await dailyRes.json();

        trafficChartData = daily || [];
        activeChartHoverIndex = -1;

        renderTrafficOverviewCards(overview);
        renderTrafficChart();
        renderTrafficTable(daily);
        updateTrafficRangeLabel(selectedStart, selectedEnd, daily.length);
    } catch (error) {
        console.error(error);
        const container = document.getElementById('traffic-overview');
        if (container) {
            container.innerHTML = '<div style="padding: 16px; color: #b91c1c;">Không thể tải dữ liệu traffic.</div>';
        }
    } finally {
        if (typeof AdminApp !== 'undefined' && AdminApp.showLoading) {
            AdminApp.showLoading(false);
        }
    }
}

function renderTrafficOverviewCards(overview) {
    const container = document.getElementById('traffic-overview');
    if (!container) return;

    const cards = [
        { label: 'Hôm nay', value: formatNumber(overview.todayTraffic || 0), icon: 'fa-chart-column', tone: '#2563eb' },
        { label: 'Phiên hôm nay', value: formatNumber(overview.todaySessions || 0), icon: 'fa-user', tone: '#16a34a' },
        { label: 'Tổng traffic', value: formatNumber(overview.totalTraffic || 0), icon: 'fa-globe', tone: '#7c3aed' },
        { label: 'Tổng phiên', value: formatNumber(overview.totalSessions || 0), icon: 'fa-users', tone: '#ea580c' }
    ];

    container.innerHTML = cards.map(card => `
        <div style="background: linear-gradient(135deg, ${card.tone}12, #ffffff); border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; display: flex; align-items: center; gap: 14px;">
            <div style="width: 44px; height: 44px; border-radius: 8px; display: inline-flex; align-items: center; justify-content: center; background: ${card.tone}; color: white; font-size: 1.2rem;">
                <i class="fa-solid ${card.icon}"></i>
            </div>
            <div>
                <div style="font-size: 0.85rem; color: #64748b; font-weight: 600;">${card.label}</div>
                <div style="font-size: 1.35rem; font-weight: 700; color: #0f172a; margin-top: 2px;">${card.value}</div>
            </div>
        </div>
    `).join('');
}

/* ==========================================================================
   NÂNG CẤP BẢN VẼ BIỂU ĐỒ (CANVAS CHART WITH INTERACTION & Y-AXIS)
   ========================================================================== */
function initChartListeners() {
    const canvas = document.getElementById('traffic-chart');
    if (!canvas) return;

    // Sự kiện di chuột tìm điểm gần nhất
    canvas.addEventListener('mousemove', (e) => {
        if (!trafficChartData || trafficChartData.length === 0) return;

        const rect = canvas.getBoundingClientRect();
        const mouseX = e.clientX - rect.left;
        
        const padding = { left: 50, right: 20 };
        const chartWidth = rect.width - padding.left - padding.right;
        
        if (mouseX < padding.left || mouseX > rect.width - padding.right) {
            if (activeChartHoverIndex !== -1) {
                activeChartHoverIndex = -1;
                renderTrafficChart();
            }
            return;
        }

        const step = chartWidth / Math.max(trafficChartData.length - 1, 1);
        const index = Math.round((mouseX - padding.left) / step);

        if (index >= 0 && index < trafficChartData.length && index !== activeChartHoverIndex) {
            activeChartHoverIndex = index;
            renderTrafficChart();
        }
    });

    canvas.addEventListener('mouseleave', () => {
        if (activeChartHoverIndex !== -1) {
            activeChartHoverIndex = -1;
            renderTrafficChart();
        }
    });

    // Tự redraw khi thay đổi kích thước màn hình
    if (window.ResizeObserver) {
        if (chartResizeObserver) chartResizeObserver.disconnect();
        chartResizeObserver = new ResizeObserver(() => renderTrafficChart());
        chartResizeObserver.observe(canvas.parentElement);
    }
}

function renderTrafficChart() {
    const canvas = document.getElementById('traffic-chart');
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    const width = canvas.clientWidth || 600;
    const height = canvas.clientHeight || 320;
    const deviceScale = window.devicePixelRatio || 1;

    canvas.width = width * deviceScale;
    canvas.height = height * deviceScale;
    ctx.setTransform(deviceScale, 0, 0, deviceScale, 0, 0);
    ctx.clearRect(0, 0, width, height);

    if (!trafficChartData || trafficChartData.length === 0) {
        ctx.fillStyle = '#94a3b8';
        ctx.font = '14px sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText('Chưa có dữ liệu thống kê', width / 2, height / 2);
        return;
    }

    const values = trafficChartData.map(item => item.traffic || 0);
    const rawMax = Math.max(...values, 5);
    // Làm tròn giá trị max cho đẹp mốc Y-axis
    const maxValue = Math.ceil(rawMax * 1.15);

    const padding = { top: 25, right: 20, bottom: 40, left: 50 };
    const chartWidth = width - padding.left - padding.right;
    const chartHeight = height - padding.top - padding.bottom;

    // 1. Vẽ các đường lưới ngang và nhãn TRỤC TUNG (Y-Axis)
    const yTicks = 4;
    ctx.font = '11px sans-serif';
    ctx.fillStyle = '#64748b';
    ctx.textAlign = 'right';
    ctx.textBaseline = 'middle';

    for (let i = 0; i <= yTicks; i++) {
        const yVal = Math.round((maxValue / yTicks) * (yTicks - i));
        const yPos = padding.top + (chartHeight * i) / yTicks;

        // Đường gióng ngang
        ctx.strokeStyle = '#f1f5f9';
        ctx.lineWidth = 1;
        ctx.beginPath();
        ctx.moveTo(padding.left, yPos);
        ctx.lineTo(width - padding.right, yPos);
        ctx.stroke();

        // Nhãn giá trị
        ctx.fillText(formatNumber(yVal), padding.left - 8, yPos);
    }

    // Tính toán tọa độ các điểm dữ liệu
    const points = values.map((val, idx) => {
        const x = padding.left + (chartWidth * idx) / Math.max(values.length - 1, 1);
        const y = height - padding.bottom - ((val / maxValue) * chartHeight);
        return { x, y, value: val, date: trafficChartData[idx].date, sessions: trafficChartData[idx].sessions };
    });

    // 2. Vẽ dải màu Gradient bên dưới đường Line
    const gradient = ctx.createLinearGradient(0, padding.top, 0, height - padding.bottom);
    gradient.addColorStop(0, 'rgba(37, 99, 235, 0.25)');
    gradient.addColorStop(1, 'rgba(37, 99, 235, 0.00)');

    ctx.beginPath();
    ctx.moveTo(points[0].x, height - padding.bottom);
    points.forEach(pt => ctx.lineTo(pt.x, pt.y));
    ctx.lineTo(points[points.length - 1].x, height - padding.bottom);
    ctx.closePath();
    ctx.fillStyle = gradient;
    ctx.fill();

    // 3. Vẽ đường Line chính
    ctx.strokeStyle = '#2563eb';
    ctx.lineWidth = 2.5;
    ctx.beginPath();
    points.forEach((pt, idx) => {
        if (idx === 0) ctx.moveTo(pt.x, pt.y);
        else ctx.lineTo(pt.x, pt.y);
    });
    ctx.stroke();

    // 4. Vẽ các điểm Node nhỏ
    ctx.fillStyle = '#2563eb';
    points.forEach(pt => {
        ctx.beginPath();
        ctx.arc(pt.x, pt.y, 3, 0, Math.PI * 2);
        ctx.fill();
    });

    // 5. Vẽ nhãn TRỤC HOÀNH (X-Axis) - Tự động giãn khoảng khi nhiều dữ liệu
    ctx.fillStyle = '#64748b';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'top';

    const labelStep = Math.max(1, Math.ceil(points.length / 10)); // Chỉ hiện tối đa ~10 nhãn ngày
    points.forEach((pt, idx) => {
        if (idx % labelStep === 0 || idx === points.length - 1) {
            ctx.fillText(formatDateLabel(pt.date), pt.x, height - padding.bottom + 8);
        }
    });

    // 6. Xử lý HIỆU ỨNG HOVER (Crosshair & Tooltip)
    if (activeChartHoverIndex >= 0 && activeChartHoverIndex < points.length) {
        const target = points[activeChartHoverIndex];

        // Đường gióng dọc
        ctx.strokeStyle = '#94a3b8';
        ctx.setLineDash([4, 4]);
        ctx.beginPath();
        ctx.moveTo(target.x, padding.top);
        ctx.lineTo(target.x, height - padding.bottom);
        ctx.stroke();
        ctx.setLineDash([]); // Reset line dash

        // Điểm Highlight lớn
        ctx.beginPath();
        ctx.arc(target.x, target.y, 6, 0, Math.PI * 2);
        ctx.fillStyle = '#2563eb';
        ctx.fill();
        ctx.lineWidth = 2;
        ctx.strokeStyle = '#ffffff';
        ctx.stroke();

        // Vẽ Khung Tooltip
        const toolTitle = `Ngày: ${formatDateWithYear(target.date)}`;
        const toolTraffic = `Lượt truy cập: ${formatNumber(target.value)}`;
        const toolSession = `Phiên: ${formatNumber(target.sessions || 0)}`;

        ctx.font = '12px sans-serif';
        const boxWidth = Math.max(ctx.measureText(toolTitle).width, ctx.measureText(toolTraffic).width) + 24;
        const boxHeight = 62;
        
        let boxX = target.x + 12;
        if (boxX + boxWidth > width - padding.right) {
            boxX = target.x - boxWidth - 12; // Đẩy qua trái nếu tràn lề
        }
        let boxY = target.y - boxHeight / 2;
        if (boxY < padding.top) boxY = padding.top;

        // Nền Tooltip
        ctx.fillStyle = 'rgba(15, 23, 42, 0.88)';
        ctx.beginPath();
        ctx.roundRect(boxX, boxY, boxWidth, boxHeight, 6);
        ctx.fill();

        // Chữ Tooltip
        ctx.fillStyle = '#ffffff';
        ctx.textAlign = 'left';
        ctx.textBaseline = 'top';
        ctx.font = 'bold 11px sans-serif';
        ctx.fillText(toolTitle, boxX + 10, boxY + 8);

        ctx.font = '11px sans-serif';
        ctx.fillStyle = '#60a5fa';
        ctx.fillText(toolTraffic, boxX + 10, boxY + 26);
        ctx.fillStyle = '#4ade80';
        ctx.fillText(toolSession, boxX + 10, boxY + 42);
    }
}

/* ==========================================================================
   BẢNG DỮ LIỆU & PHÂN TRANG (QUY CHUẨN CỦA HỆ THỐNG ADMIN)
   ========================================================================== */
function renderTrafficTable(daily) {
    const tbody = document.getElementById('traffic-table-body');
    const pagination = document.getElementById('traffic-pagination');
    if (!tbody) return;

    if (!daily || daily.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="4" style="text-align: center; padding: 30px; color: #888;">
                    <i class="fa-solid fa-folder-open" style="font-size: 2rem; display: block; margin: auto; margin-bottom: 8px;"></i>
                    Chưa có dữ liệu traffic trong khoảng thời gian này.
                </td>
            </tr>`;
        if (pagination) pagination.innerHTML = '';
        return;
    }

    const currentPage = Number(window.trafficCurrentPage || 1);
    const totalPages = Math.max(1, Math.ceil(daily.length / TRAFFIC_PAGE_SIZE));
    const safePage = Math.min(currentPage, totalPages);
    window.trafficCurrentPage = safePage;

    const startIndex = (safePage - 1) * TRAFFIC_PAGE_SIZE;
    const paginatedData = daily.slice(startIndex, startIndex + TRAFFIC_PAGE_SIZE);

    tbody.innerHTML = paginatedData.map(item => {
        const avg = (item.traffic || 0) / Math.max(item.sessions || 1, 1);
        return `
            <tr>
                <td style="text-align: left; font-weight: 600; color: #0f172a;">${formatDateLabel(item.date)}</td>
                <td style="text-align: right;">${formatNumber(item.traffic || 0)}</td>
                <td style="text-align: right;">${formatNumber(item.sessions || 0)}</td>
                <td style="text-align: right;">${Number(avg).toFixed(1)}</td>
            </tr>
        `;
    }).join('');

    // Thanh phân trang dùng cấu trúc CSS của admin.css
    if (pagination) {
        const endIdx = Math.min(startIndex + TRAFFIC_PAGE_SIZE, daily.length);

        let html = `
            <div class="pagination-info">
                Hiển thị <b>${startIndex + 1}-${endIdx}</b> trên tổng số <b>${daily.length}</b> ngày
                (Trang ${safePage}/${totalPages})
            </div>
            <div class="pagination-controls">
                <button class="btn-page" onclick="changeTrafficPage(1)" ${safePage === 1 ? 'disabled' : ''}>
                    <i class="fa-solid fa-angles-left"></i>
                </button>
                <button class="btn-page" onclick="changeTrafficPage(${safePage - 1})" ${safePage === 1 ? 'disabled' : ''}>
                    <i class="fa-solid fa-angle-left"></i>
                </button>
        `;

        let startPage = Math.max(1, safePage - 2);
        let endPage = Math.min(totalPages, startPage + 4);
        if (endPage - startPage < 4) startPage = Math.max(1, endPage - 4);

        for (let i = startPage; i <= endPage; i++) {
            html += `
                <button class="btn-page ${i === safePage ? 'active' : ''}" onclick="changeTrafficPage(${i})">
                    ${i}
                </button>
            `;
        }

        html += `
                <button class="btn-page" onclick="changeTrafficPage(${safePage + 1})" ${safePage >= totalPages ? 'disabled' : ''}>
                    <i class="fa-solid fa-angle-right"></i>
                </button>
                <button class="btn-page" onclick="changeTrafficPage(${totalPages})" ${safePage >= totalPages ? 'disabled' : ''}>
                    <i class="fa-solid fa-angles-right"></i>
                </button>
            </div>
        `;

        pagination.innerHTML = html;
    }
}

// Global handler cho việc chuyển trang
window.changeTrafficPage = function(page) {
    window.trafficCurrentPage = page;
    if (trafficChartData) {
        renderTrafficTable(trafficChartData);
    }
};

function updateTrafficRangeLabel(startDate, endDate, totalCount) {
    const label = document.getElementById('traffic-range-label');
    if (!label) return;

    if (startDate && endDate) {
        label.textContent = `${formatDateWithYear(startDate)} - ${formatDateWithYear(endDate)} (${totalCount} ngày)`;
        return;
    }

    label.textContent = `${totalCount} ngày`;
}

/* ==========================================================================
   HELPERS
   ========================================================================== */
function formatDateLabel(dateString) {
    if (!dateString) return '—';
    const date = new Date(dateString + 'T00:00:00');
    return `${date.getDate()}/${date.getMonth() + 1}`;
}

function formatDateWithYear(dateString) {
    if (!dateString) return '—';
    const date = new Date(dateString + 'T00:00:00');
    return date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

function formatNumber(value) {
    return Number(value || 0).toLocaleString('vi-VN');
}

function toISODate(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

function addDays(date, days) {
    const copy = new Date(date.getTime());
    copy.setDate(copy.getDate() + days);
    return copy;
}

registerModule('traffic', renderTrafficModule);