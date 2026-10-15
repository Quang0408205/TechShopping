/* ================= DỮ LIỆU MẪU (MOCK) — KHU QUẢN TRỊ NỘI BỘ admin/ ================= */

/*
 * Checkpoint B1: các trang admin/ chưa có API chạy trên dữ liệu mẫu: Bảo hành /
 * Bảo trì / Đổi trả (Phase 6), Doanh số / Tổng quan / Báo cáo (Phase 10),
 * Chatbot / Hỗ trợ (Phase 9). Đơn hàng (Phase 4.5), Chi nhánh / Nhân viên (Phase 7)
 * đã dùng API thật.
 *
 * Nguyên tắc (giống cart / recommendation / contact phía khách hàng):
 *   - mọi hàm là HÀM ĐỒNG BỘ trả về dữ liệu tĩnh, KHÔNG gọi mạng;
 *   - trang admin chỉ gọi các hàm get... / update... / create... bên dưới,
 *     nên khi có API thật (giai đoạn C) chỉ cần thay thân hàm bằng
 *     apiRequest(...), phần hiển thị giữ nguyên;
 *   - KHÔNG dùng catalogue sản phẩm mock của bản tham chiếu: bản ghi bán
 *     hàng / đơn hàng / bảo hành lưu sẵn tên sản phẩm (snapshot), giống cách
 *     order_items của backend sẽ lưu.
 *
 * Mã vai trò hiển thị (EMPLOYEE / BRANCH_MANAGER / ADMIN) nằm ở staff-auth.js.
 *
 * Nạp sau js/core/api.js và js/core/ui.js, trước js/admin/staff-auth.js.
 */


/* ================= LƯU TRỮ localStorage (tiền tố poy_) ================= */

const STAFF_OVERRIDES_KEY = "poy_staff_overrides";


function readStoredJson(key, fallback) {

    try {

        const raw = localStorage.getItem(key);

        const value = raw ? JSON.parse(raw) : fallback;

        return value !== null && typeof value === "object" ? value : fallback;

    } catch (error) {

        return fallback;

    }

}


function writeStoredJson(key, value) {

    try {
        localStorage.setItem(key, JSON.stringify(value));
    } catch (error) {
        /* localStorage bị chặn: thay đổi chỉ còn trong trang hiện tại */
    }

}


/*
 * Các thao tác CẬP NHẬT (đổi trạng thái đơn / yêu cầu / ticket, khoá nhân
 * viên...) được lưu đè theo id rồi gộp vào dữ liệu gốc mỗi lần đọc, để giữ
 * được qua các lần chuyển trang mà không cần backend.
 */

function getStaffOverrides() {

    return readStoredJson(STAFF_OVERRIDES_KEY, {});

}


function setStaffOverride(id, patch) {

    const overrides = getStaffOverrides();

    overrides[id] = Object.assign({}, overrides[id], patch);

    writeStoredJson(STAFF_OVERRIDES_KEY, overrides);

}


function applyOverride(record) {

    const overrides = getStaffOverrides();

    return overrides[record.id]
        ? Object.assign({}, record, overrides[record.id])
        : record;

}


/* ================= CHI NHÁNH / NHÂN VIÊN MẪU ================= */

/*
 * Chi nhánh và nhân viên THẬT đã có API từ Phase 7 (admin/stores, admin/employees,
 * GET /employees/me). Hai danh sách dưới đây chỉ còn là dữ liệu riêng của các trang
 * mẫu (Tổng quan, Báo cáo, Bảo hành / Đổi trả, Hỗ trợ) để chúng vẫn demo được cho
 * tới phase thay chúng (6, 9, 10); mã CN01… không trùng mã chi nhánh thật (số).
 */

const MOCK_STORES = [
    { id: "CN01", name: "Chi nhánh Quận 1", address: "45 Lê Lợi, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh", phone: "028 1234 5678" },
    { id: "CN02", name: "Chi nhánh Quận 5", address: "12 Nguyễn Trãi, Phường 3, Quận 5, TP. Hồ Chí Minh", phone: "028 2345 6789" },
    { id: "CN03", name: "Chi nhánh Cầu Giấy - Hà Nội", address: "88 Trần Thái Tông, Cầu Giấy, Hà Nội", phone: "024 3456 7890" }
];


function getMockStores() {

    return MOCK_STORES.slice();

}


function getMockStoreById(id) {

    return MOCK_STORES.find(function (store) {
        return store.id === id;
    }) || null;

}


const MOCK_EMPLOYEES = [
    { id: "nv-admin-01", fullname: "Trần Văn Quản", email: "admin@poy.vn", phone: "0901 111 222", role: "ADMIN", storeId: null, position: "Quản trị hệ thống", status: "ACTIVE", joinedAt: "2023-01-10" },
    { id: "nv-bm-01", fullname: "Nguyễn Thị Lan", email: "lan.quanly@poy.vn", phone: "0902 222 333", role: "BRANCH_MANAGER", storeId: "CN01", position: "Trưởng chi nhánh", status: "ACTIVE", joinedAt: "2023-03-15" },
    { id: "nv-bm-02", fullname: "Phạm Minh Đức", email: "duc.quanly@poy.vn", phone: "0903 333 444", role: "BRANCH_MANAGER", storeId: "CN02", position: "Trưởng chi nhánh", status: "ACTIVE", joinedAt: "2023-05-02" },
    { id: "nv-staff-01", fullname: "Lê Thị Hoa", email: "hoa.nv@poy.vn", phone: "0904 444 555", role: "EMPLOYEE", storeId: "CN01", position: "Nhân viên bán hàng", status: "ACTIVE", joinedAt: "2024-02-20" },
    { id: "nv-staff-02", fullname: "Đỗ Văn Hùng", email: "hung.nv@poy.vn", phone: "0905 555 666", role: "EMPLOYEE", storeId: "CN01", position: "Nhân viên kỹ thuật", status: "ACTIVE", joinedAt: "2024-04-11" },
    { id: "nv-staff-03", fullname: "Vũ Thị Mai", email: "mai.nv@poy.vn", phone: "0906 666 777", role: "EMPLOYEE", storeId: "CN02", position: "Nhân viên bán hàng", status: "ACTIVE", joinedAt: "2024-06-01" },
    { id: "nv-staff-04", fullname: "Ngô Văn Tài", email: "tai.nv@poy.vn", phone: "0907 777 888", role: "EMPLOYEE", storeId: "CN03", position: "Nhân viên bán hàng", status: "INACTIVE", joinedAt: "2024-01-05" }
];


function getMockEmployeeById(id) {

    return MOCK_EMPLOYEES.find(function (employee) {
        return employee.id === id;
    }) || null;

}


/* Bản lưu trên trình duyệt của các màn chi nhánh / nhân viên mẫu cũ: không còn dùng */

try {
    localStorage.removeItem("poy_extra_stores");
    localStorage.removeItem("poy_extra_employees");
} catch (error) {
    /* localStorage bị chặn: không có gì để xoá */
}


/* ================= DOANH SỐ (sales_records) ================= */

/*
 * Mỗi bản ghi = một lượt bán tại chi nhánh do một nhân viên xử lý.
 * productName là snapshot (không tra catalogue).
 */

const MOCK_SALES_RECORDS = [
    { id: "SR-0001", storeId: "CN01", employeeId: "nv-staff-01", productName: "Điện thoại iPhone 18 Pro Max 256GB", quantity: 1, revenue: 41990000, soldAt: "2026-07-03" },
    { id: "SR-0002", storeId: "CN01", employeeId: "nv-staff-02", productName: "Laptop MacBook Air 13 inch M5 16GB/512GB/8GPU 70W", quantity: 1, revenue: 35290000, soldAt: "2026-07-08" },
    { id: "SR-0003", storeId: "CN02", employeeId: "nv-staff-03", productName: "Điện thoại Xiaomi Redmi Note 17 4G 4GB/128GB", quantity: 2, revenue: 11580000, soldAt: "2026-07-12" },
    { id: "SR-0004", storeId: "CN01", employeeId: "nv-staff-01", productName: "Sạc nhanh 2 cổng Type-C QC3.0 PD 30W Ugreen X516", quantity: 3, revenue: 750000, soldAt: "2026-07-15" },
    { id: "SR-0005", storeId: "CN03", employeeId: "nv-staff-04", productName: "Máy tính bảng iPad Air M4 11 inch WiFi 128GB", quantity: 1, revenue: 20690000, soldAt: "2026-07-19" },
    { id: "SR-0006", storeId: "CN02", employeeId: "nv-staff-03", productName: "Điện thoại iPhone 18 Pro 256GB", quantity: 1, revenue: 38990000, soldAt: "2026-07-22" },
    { id: "SR-0007", storeId: "CN01", employeeId: "nv-staff-02", productName: "Laptop MacBook Neo 13 inch A18 Pro 8GB/256GB", quantity: 1, revenue: 18990000, soldAt: "2026-07-27" },
    { id: "SR-0008", storeId: "CN01", employeeId: "nv-staff-01", productName: "Điện thoại iPhone 18 Pro Max 256GB", quantity: 2, revenue: 83980000, soldAt: "2026-08-02" },
    { id: "SR-0009", storeId: "CN02", employeeId: "nv-staff-03", productName: "Máy tính bảng iPad A16 WiFi 128GB", quantity: 1, revenue: 12290000, soldAt: "2026-08-05" },
    { id: "SR-0010", storeId: "CN03", employeeId: "nv-staff-04", productName: "Ốp lưng Magnetic iPhone 17 Pro Max Nhựa cứng TORRAS C1S", quantity: 4, revenue: 1476000, soldAt: "2026-08-09" },
    { id: "SR-0011", storeId: "CN01", employeeId: "nv-staff-02", productName: "Điện thoại iPhone Duo 256GB", quantity: 1, revenue: 64990000, soldAt: "2026-08-14" },
    { id: "SR-0012", storeId: "CN02", employeeId: "nv-staff-03", productName: "Điện thoại iPhone 18 Pro 256GB", quantity: 2, revenue: 77980000, soldAt: "2026-08-18" },
    { id: "SR-0013", storeId: "CN01", employeeId: "nv-staff-01", productName: "Laptop MacBook Air 13 inch M5 16GB/512GB/8GPU 70W", quantity: 1, revenue: 35290000, soldAt: "2026-08-23" },
    { id: "SR-0014", storeId: "CN03", employeeId: "nv-staff-04", productName: "Máy tính bảng iPad Air M4 11 inch WiFi 128GB", quantity: 1, revenue: 20690000, soldAt: "2026-08-28" },
    { id: "SR-0015", storeId: "CN01", employeeId: "nv-staff-02", productName: "Điện thoại iPhone 18 Pro Max 256GB", quantity: 1, revenue: 41990000, soldAt: "2026-09-03" },
    { id: "SR-0016", storeId: "CN02", employeeId: "nv-staff-03", productName: "Laptop MacBook Air 15 inch M5 16GB/512GB 70W", quantity: 1, revenue: 41290000, soldAt: "2026-09-06" },
    { id: "SR-0017", storeId: "CN01", employeeId: "nv-staff-01", productName: "Sạc nhanh 2 cổng Type-C QC3.0 PD 30W Ugreen X516", quantity: 2, revenue: 500000, soldAt: "2026-09-10" },
    { id: "SR-0018", storeId: "CN03", employeeId: "nv-staff-04", productName: "Đồng hồ định vị trẻ em Kidcare Sight S25", quantity: 1, revenue: 1590000, soldAt: "2026-09-14" },
    { id: "SR-0019", storeId: "CN02", employeeId: "nv-staff-03", productName: "Điện thoại Xiaomi Redmi Note 17 4G 4GB/128GB", quantity: 1, revenue: 5790000, soldAt: "2026-09-18" },
    { id: "SR-0020", storeId: "CN01", employeeId: "nv-staff-02", productName: "Máy tính bảng iPad A16 WiFi 128GB", quantity: 1, revenue: 12290000, soldAt: "2026-09-21" },
    { id: "SR-0021", storeId: "CN01", employeeId: "nv-staff-01", productName: "Laptop MacBook Air 13 inch M5 16GB/512GB/8GPU 70W", quantity: 1, revenue: 35290000, soldAt: "2026-09-24" }
];


/* filter: { storeId, from, to } (ngày dạng "yyyy-mm-dd") */

function getSalesRecords(filter) {

    const f = filter || {};

    return MOCK_SALES_RECORDS.filter(function (record) {

        if (f.storeId && record.storeId !== f.storeId) {
            return false;
        }

        if (f.from && record.soldAt < f.from) {
            return false;
        }

        if (f.to && record.soldAt > f.to) {
            return false;
        }

        return true;

    });

}


/* Tổng hợp cho dashboard / báo cáo: theo chi nhánh, theo tháng, top sản phẩm */

function getSalesSummary(filter) {

    const records = getSalesRecords(filter);

    const byStore = {};

    const byMonth = {};

    const byProduct = {};


    records.forEach(function (record) {

        if (!byStore[record.storeId]) {

            const store = getMockStoreById(record.storeId);

            byStore[record.storeId] = {
                storeId: record.storeId,
                storeName: store ? store.name : record.storeId,
                revenue: 0,
                orders: 0
            };

        }

        byStore[record.storeId].revenue += record.revenue;

        byStore[record.storeId].orders += 1;


        const month = record.soldAt.slice(0, 7);

        if (!byMonth[month]) {
            byMonth[month] = { month: month, revenue: 0, orders: 0 };
        }

        byMonth[month].revenue += record.revenue;

        byMonth[month].orders += 1;


        if (!byProduct[record.productName]) {
            byProduct[record.productName] = { name: record.productName, quantity: 0, revenue: 0 };
        }

        byProduct[record.productName].quantity += record.quantity;

        byProduct[record.productName].revenue += record.revenue;

    });


    return {
        totalRevenue: records.reduce(function (sum, r) { return sum + r.revenue; }, 0),
        totalOrders: records.length,
        totalUnits: records.reduce(function (sum, r) { return sum + r.quantity; }, 0),
        byStore: Object.values(byStore).sort(function (a, b) { return b.revenue - a.revenue; }),
        byMonth: Object.values(byMonth).sort(function (a, b) { return a.month.localeCompare(b.month); }),
        topProducts: Object.values(byProduct)
            .sort(function (a, b) { return b.revenue - a.revenue; })
            .slice(0, 5)
    };

}


/* ================= BẢO HÀNH / BẢO TRÌ / ĐỔI TRẢ ================= */

/*
 * DB có 3 bảng riêng (warranty_requests, maintenance_requests,
 * return_requests); mock gộp chung 1 danh sách có trường type để hiển thị.
 */

const SERVICE_TYPE_LABELS = {
    WARRANTY: "Bảo hành",
    MAINTENANCE: "Bảo trì",
    RETURN: "Đổi trả"
};


const SERVICE_STATUS_LABELS = {
    RECEIVED: "Đã tiếp nhận",
    PROCESSING: "Đang xử lý",
    COMPLETED: "Hoàn tất",
    REJECTED: "Từ chối"
};


const MOCK_SERVICE_REQUESTS = [
    { id: "BH-0001", type: "WARRANTY", productName: "Laptop MacBook Air 13 inch M5 16GB/512GB/8GPU 70W", customerName: "Nguyễn Văn An", customerPhone: "0911 222 333", storeId: "CN01", assignedEmployeeId: "nv-staff-02", status: "PROCESSING", description: "Màn hình nhấp nháy sau 2 tháng sử dụng.", createdAt: "2026-09-15" },
    { id: "BH-0002", type: "RETURN", productName: "Điện thoại Xiaomi Redmi Note 17 4G 4GB/128GB", customerName: "Trần Thị Bích", customerPhone: "0912 333 444", storeId: "CN02", assignedEmployeeId: "nv-staff-03", status: "RECEIVED", description: "Khách muốn đổi sang màu khác trong 7 ngày.", createdAt: "2026-09-20" },
    { id: "BH-0003", type: "MAINTENANCE", productName: "Laptop MacBook Neo 13 inch A18 Pro 8GB/256GB", customerName: "Lê Hoàng Nam", customerPhone: "0913 444 555", storeId: "CN01", assignedEmployeeId: "nv-staff-02", status: "COMPLETED", description: "Vệ sinh máy định kỳ.", createdAt: "2026-08-30" },
    { id: "BH-0004", type: "WARRANTY", productName: "Điện thoại iPhone 18 Pro Max 256GB", customerName: "Phạm Thu Trang", customerPhone: "0914 555 666", storeId: "CN01", assignedEmployeeId: "nv-staff-01", status: "REJECTED", description: "Máy vào nước, không thuộc diện bảo hành.", createdAt: "2026-09-05" },
    { id: "BH-0005", type: "WARRANTY", productName: "Máy tính bảng iPad Air M4 11 inch WiFi 128GB", customerName: "Đặng Quốc Bảo", customerPhone: "0915 666 777", storeId: "CN03", assignedEmployeeId: "nv-staff-04", status: "RECEIVED", description: "Pin sụt nhanh bất thường.", createdAt: "2026-09-22" }
];


/* filter: { storeId, type, status } */

function getServiceRequests(filter) {

    const f = filter || {};

    return MOCK_SERVICE_REQUESTS
        .map(applyOverride)
        .filter(function (request) {
            return (!f.storeId || request.storeId === f.storeId) &&
                (!f.type || request.type === f.type) &&
                (!f.status || request.status === f.status);
        });

}


/* Mô phỏng PATCH /service-requests/{id}/status */

function updateServiceRequestStatus(id, status) {

    setStaffOverride(id, { status: status });

}


/* ================= LỊCH SỬ CHATBOT (chat_sessions / chat_messages) ================= */

/* Chỉ để ADMIN xem lại, không điều khiển chatbot */

const MOCK_CHAT_SESSIONS = [
    {
        id: "CHAT-0001",
        customerName: "Khách (ẩn danh)",
        startedAt: "2026-09-20T10:12:00+07:00",
        resolved: true,
        messages: [
            { sender: "user", text: "Shop có giao hàng ở Đà Nẵng không?" },
            { sender: "bot", text: "POY hỗ trợ giao hàng đến địa chỉ của bạn, nhân viên sẽ liên hệ xác nhận thời gian giao." },
            { sender: "user", text: "Phí giao hàng bao nhiêu?" },
            { sender: "bot", text: "Phí giao hàng sẽ được thông báo khi xác nhận đơn hàng." }
        ]
    },
    {
        id: "CHAT-0002",
        customerName: "Nguyễn Văn An",
        startedAt: "2026-09-21T15:40:00+07:00",
        resolved: true,
        messages: [
            { sender: "user", text: "Đơn DH000123 của tôi giao tới đâu rồi?" },
            { sender: "bot", text: "Đơn DH000123 đã được giao thành công. Bạn cần hỗ trợ thêm gì không?" }
        ]
    },
    {
        id: "CHAT-0003",
        customerName: "Khách (ẩn danh)",
        startedAt: "2026-09-23T09:05:00+07:00",
        resolved: false,
        messages: [
            { sender: "user", text: "MacBook Air 13 inch M5 còn màu bạc không?" },
            { sender: "bot", text: "Mình chưa có thông tin tồn kho theo thời gian thực. Bạn vui lòng liên hệ cửa hàng để được kiểm tra nhé." }
        ]
    }
];


function getChatSessions() {

    return MOCK_CHAT_SESSIONS.slice().sort(function (a, b) {
        return b.startedAt.localeCompare(a.startedAt);
    });

}


function getChatSessionById(id) {

    return MOCK_CHAT_SESSIONS.find(function (session) {
        return session.id === id;
    }) || null;

}
