// Headless Edge (CDP) E2E for Checkpoint B1: the admin/ area on mock data (3 staff roles).
// Needs only static-server.mjs serving the REPO ROOT (no backend: B1 makes no API calls).
import { spawn } from "node:child_process";
import { mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";

const EDGE = "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe";
const ORIGIN = process.env.E2E_ORIGIN || "http://127.0.0.1:5501";
const SITE = `${ORIGIN}/frontend`;
const SHOTS = process.argv[2] || ".";
const PORT = 9341;

const sleep = ms => new Promise(r => setTimeout(r, ms));
const results = [];
function check(name, ok, detail = "") {
    results.push({ name, ok });
    console.log(`${ok ? "PASS" : "FAIL"}  ${name}${detail !== "" ? "  -> " + detail : ""}`);
}
const vnd = n => new Intl.NumberFormat("vi-VN").format(n) + "đ";

// ---- browser ----
const edge = spawn(EDGE, ["--headless=new", `--remote-debugging-port=${PORT}`,
    `--user-data-dir=${mkdtempSync(join(tmpdir(), "edge-e2e-"))}`, "--no-first-run", "--disable-extensions",
    "about:blank"], { stdio: "ignore" });
let target;
for (let i = 0; i < 50 && !target; i++) {
    await sleep(200);
    try { target = (await (await fetch(`http://127.0.0.1:${PORT}/json/list`)).json()).find(t => t.type === "page"); } catch { }
}
if (!target) { console.error("Edge did not start"); edge.kill(); process.exit(1); }

const ws = new WebSocket(target.webSocketDebuggerUrl);
await new Promise(r => ws.addEventListener("open", r));
let nextId = 0;
const pending = new Map();
const listeners = [];
const jsProblems = [];
const missing = [];
const requests = [];
let mocks = []; // [{ path, method, status, body }] each fulfilled once through the Fetch domain

const send = (method, params = {}) => new Promise(resolve => {
    const id = ++nextId; pending.set(id, resolve); ws.send(JSON.stringify({ id, method, params }));
});
ws.addEventListener("message", async ({ data }) => {
    const msg = JSON.parse(data);
    if (msg.id && pending.has(msg.id)) { pending.get(msg.id)(msg); pending.delete(msg.id); return; }
    listeners.forEach(l => l(msg));
    if (msg.method === "Runtime.exceptionThrown") jsProblems.push("exception: " + msg.params.exceptionDetails.exception?.description);
    if (msg.method === "Runtime.consoleAPICalled" && ["error", "warning"].includes(msg.params.type)) {
        jsProblems.push(`console.${msg.params.type}: ` + msg.params.args.map(a => a.value ?? a.description).join(" "));
    }
    if (msg.method === "Network.requestWillBeSent") requests.push(msg.params.request.url);
    if (msg.method === "Network.responseReceived") {
        const { url, status } = msg.params.response;
        if (status >= 400 && url.startsWith(ORIGIN + "/") && !url.endsWith("/favicon.ico")) missing.push(`${status} ${url}`);
    }
    if (msg.method === "Fetch.requestPaused") {
        const { requestId, request } = msg.params;
        const index = mocks.findIndex(m => request.method === m.method && new URL(request.url).pathname === m.path);
        if (index >= 0) {
            const m = mocks.splice(index, 1)[0];
            await send("Fetch.fulfillRequest", {
                requestId, responseCode: m.status,
                responseHeaders: [{ name: "Content-Type", value: "application/json" },
                    { name: "Access-Control-Allow-Origin", value: ORIGIN }],
                body: Buffer.from(JSON.stringify(m.body)).toString("base64")
            });
        } else {
            await send("Fetch.continueRequest", { requestId });
        }
    }
});
async function evaluate(expression) {
    const { result } = await send("Runtime.evaluate", { expression, awaitPromise: true, returnByValue: true });
    if (result.exceptionDetails) throw new Error(result.exceptionDetails.exception?.description);
    return result.result.value;
}
async function navigate(url) {
    const loaded = new Promise(resolve => {
        const l = m => { if (m.method === "Page.loadEventFired") { listeners.splice(listeners.indexOf(l), 1); resolve(); } };
        listeners.push(l);
    });
    await send("Page.navigate", { url });
    await loaded;
}
async function waitFor(expression, timeout = 10000) {
    const end = Date.now() + timeout;
    while (Date.now() < end) {
        try { const v = await evaluate(expression); if (v) return v; } catch { }
        await sleep(150);
    }
    return null;
}

await send("Page.enable");
await send("Runtime.enable");
await send("Network.enable");
await send("Fetch.enable", { patterns: [{ urlPattern: "http://localhost:8080/api/v1/products*", requestStage: "Request" }] });
await send("Emulation.setDeviceMetricsOverride", { width: 1280, height: 900, deviceScaleFactor: 1, mobile: false });
await send("Page.addScriptToEvaluateOnNewDocument", { source: "window.__alerts = []; window.alert = m => window.__alerts.push(m);" });

const text = selector => evaluate(`document.querySelector(${JSON.stringify(selector)})?.textContent.trim()`);
const ready = `document.body.classList.contains("admin-ready")`;
const path = () => evaluate(`location.pathname.replace(/^.*\\/frontend\\//, "") + location.search`);
async function open(page) {
    await navigate(`${SITE}/${page}`);
    await sleep(400);
}
async function openAdmin(page) {
    await open(page);
    await waitFor(`${ready} || location.pathname.endsWith("/login.html") || location.search.includes("denied=1")`);
    await sleep(300);
}
async function login(email, password) {
    await open("admin/login.html");
    await evaluate(`document.getElementById("staffEmail").value = ${JSON.stringify(email)};
        document.getElementById("staffPassword").value = ${JSON.stringify(password)};
        document.getElementById("staffLoginForm").requestSubmit()`);
    await sleep(700);
    await waitFor(`${ready} || document.getElementById("staffLoginError") && !document.getElementById("staffLoginError").hidden`);
    await sleep(300);
}
const visibleNav = () => evaluate(`[...document.querySelectorAll(".admin-nav a")].filter(a => !a.hidden).map(a => a.dataset.page)`);
const rowIds = tbody => evaluate(`[...document.querySelectorAll("${tbody} tr")].map(r => r.cells[0]?.textContent.trim())`);
async function shot(name) {
    const { result } = await send("Page.captureScreenshot", { format: "png" });
    writeFileSync(join(SHOTS, name), Buffer.from(result.data, "base64"));
}
// expected numbers from the same mock data the pages use
const CN01_REVENUE = [41990000, 35290000, 750000, 18990000, 83980000, 64990000, 35290000, 41990000, 500000, 12290000, 35290000].reduce((a, b) => a + b, 0);
const ALL_REVENUE = CN01_REVENUE + [11580000, 38990000, 12290000, 77980000, 41290000, 5790000].reduce((a, b) => a + b, 0)
    + [20690000, 1476000, 20690000, 1590000].reduce((a, b) => a + b, 0);

try {
    await open("admin/login.html");
    await evaluate(`localStorage.clear()`);
    await evaluate(`localStorage.setItem("poy_auth", JSON.stringify({ accessToken: "cust", refreshToken: "r", user: { id: 9, username: "khach", fullname: "Khách Hàng", roles: ["CUSTOMER"] } }))`);

    // 1. guard without a session
    await open("admin/dashboard.html");
    await waitFor(`location.pathname.endsWith("/admin/login.html")`);
    check("no session → admin/login.html?redirect=admin/dashboard.html", (await path()) === "admin/login.html?redirect=admin%2Fdashboard.html", await path());

    // 2. login errors
    await login("", "");
    check("login: empty fields → message", ((await text("#staffLoginError")) || "").includes("Vui lòng nhập"));
    await login("hoa.nv@poy.vn", "sai-mat-khau");
    check("login: wrong password → generic message", (await text("#staffLoginError")) === "Email hoặc mật khẩu không đúng.");
    await login("tai.nv@poy.vn", "nhanvien123");
    check("login: locked account → locked message", ((await text("#staffLoginError")) || "").includes("đã bị khoá"));
    await shot("admin-login.png");

    // 3. EMPLOYEE
    await login("HOA.NV@poy.vn", "nhanvien123");
    check("employee: lands on dashboard (email case-insensitive)", (await path()) === "admin/dashboard.html");
    const auth = await evaluate(`JSON.parse(localStorage.getItem("poy_staff_auth"))`);
    check("poy_staff_auth has the poy_auth shape + staff part", auth.accessToken === null && auth.refreshToken === null
        && auth.user.email === "hoa.nv@poy.vn" && JSON.stringify(auth.user.roles) === '["STAFF"]'
        && auth.staff.role === "EMPLOYEE" && auth.staff.storeId === "CN01", JSON.stringify(auth));
    check("customer session poy_auth untouched", (await evaluate(`JSON.parse(localStorage.getItem("poy_auth")).accessToken`)) === "cust");
    check("employee: topbar name + role · store", (await text("#adminUserName")) === "Lê Thị Hoa" && (await text("#adminUserMeta")) === "Nhân viên · Chi nhánh Quận 1");
    check("employee: menu = dashboard, service-requests, support-tickets", JSON.stringify(await visibleNav()) === '["dashboard","service-requests","support-tickets"]', JSON.stringify(await visibleNav()));
    check("employee: empty menu groups hidden", (await evaluate(`[...document.querySelectorAll(".admin-nav-label")].filter(l => !l.hidden).map(l => l.textContent.trim()).join("|")`)) === "Chung|Nghiệp vụ");
    check("employee: active menu item", (await evaluate(`document.querySelector(".admin-nav a.active").dataset.page`)) === "dashboard");
    const cards = await evaluate(`[...document.querySelectorAll(".admin-stat-card")].map(c => c.querySelector(".admin-stat-value").textContent.trim())`);
    check("employee dashboard: 3 cards, revenue of CN01 only, no store breakdown", cards.length === 3 && cards[0] === vnd(CN01_REVENUE)
        && (await evaluate(`document.getElementById("storeBreakdownPanel").hidden`)), cards.join(" | "));
    check("dashboard charts rendered (SVG)", (await evaluate(`document.querySelectorAll("#revenueChart svg, #topProductsChart svg").length`)) === 2);
    await shot("admin-employee-dashboard.png");

    await openAdmin("admin/orders.html");
    check("employee → orders.html redirected to dashboard?denied=1 with notice", (await path()) === "admin/dashboard.html?denied=1"
        && (await evaluate(`!document.getElementById("deniedNotice").hidden`)));

    await openAdmin("admin/service-requests.html");
    check("employee service requests: store filter locked to CN01", await evaluate(`document.getElementById("storeFilter").disabled && document.getElementById("storeFilter").value === "CN01"`));
    check("employee service requests: only CN01 rows", JSON.stringify(await rowIds("#requestTableBody")) === '["BH-0001","BH-0004","BH-0003"]', JSON.stringify(await rowIds("#requestTableBody")));
    await evaluate(`(() => { const s = document.getElementById("storeFilter"); s.disabled = false; s.innerHTML += '<option value="CN02">hack</option>'; s.value = "CN02"; s.dispatchEvent(new Event("change")); })()`);
    await sleep(200);
    check("DOM tampering with the store filter still shows only CN01", JSON.stringify(await rowIds("#requestTableBody")) === '["BH-0001","BH-0004","BH-0003"]');
    await evaluate(`(() => { const s = document.querySelector('.js-status-select[data-id="BH-0001"]'); s.value = "COMPLETED"; s.dispatchEvent(new Event("change")); })()`);
    await sleep(200);
    check("status change → toast", ((await text(".toast")) || "").includes("BH-0001"));
    await openAdmin("admin/service-requests.html");
    check("status change persists after reload (poy_staff_overrides)", (await evaluate(`document.querySelector('.js-status-select[data-id="BH-0001"]').value`)) === "COMPLETED"
        && (await evaluate(`JSON.parse(localStorage.getItem("poy_staff_overrides"))["BH-0001"].status`)) === "COMPLETED");
    await evaluate(`(() => { const s = document.getElementById("typeFilter"); s.value = "WARRANTY"; s.dispatchEvent(new Event("change")); })()`);
    check("type filter", JSON.stringify(await rowIds("#requestTableBody")) === '["BH-0001","BH-0004"]');
    await openAdmin("admin/support-tickets.html");
    check("employee tickets: only CN01", JSON.stringify(await rowIds("#ticketTableBody")) === '["TK-0002","TK-0001"]', JSON.stringify(await rowIds("#ticketTableBody")));

    await evaluate(`document.getElementById("adminLogoutBtn").click()`);
    await waitFor(`location.pathname.endsWith("/admin/login.html")`);
    check("logout → login page, poy_staff_auth removed, poy_auth kept", (await evaluate(`localStorage.getItem("poy_staff_auth")`)) === null
        && (await evaluate(`localStorage.getItem("poy_auth")`)) !== null);

    // 4. BRANCH_MANAGER, redirect back to the requested page
    await open("admin/reports.html");
    await waitFor(`location.pathname.endsWith("/admin/login.html")`);
    await evaluate(`document.getElementById("staffEmail").value = "lan.quanly@poy.vn"; document.getElementById("staffPassword").value = "quanly123"; document.getElementById("staffLoginForm").requestSubmit()`);
    await waitFor(`location.pathname.endsWith("/admin/reports.html")`);
    await waitFor(ready);
    await sleep(300);
    check("manager: redirected back to reports.html after login", (await path()) === "admin/reports.html");
    check("manager: menu adds orders, reports, employees (no stores / chat)", JSON.stringify(await visibleNav()) === '["dashboard","service-requests","support-tickets","orders","reports","employees"]', JSON.stringify(await visibleNav()));
    check("manager reports: CN01 only, no store comparison", (await evaluate(`document.querySelector("#reportStatGrid .admin-stat-value").textContent.trim()`)) === vnd(CN01_REVENUE)
        && (await evaluate(`document.getElementById("reportStoreChartPanel").hidden && document.getElementById("storeFilter").disabled`)));
    await evaluate(`(() => { const f = document.getElementById("fromDateFilter"); f.value = "2026-09-01"; f.dispatchEvent(new Event("change")); })()`);
    check("reports: from 2026-09-01 → 4 CN01 records", (await text("#reportRowCount")) === "4 bản ghi", await text("#reportRowCount"));
    await evaluate(`(() => { const t = document.getElementById("toDateFilter"); t.value = "2026-08-01"; t.dispatchEvent(new Event("change")); })()`);
    check("reports: from > to → error", !(await evaluate(`document.getElementById("reportDateError").hidden`)));
    await openAdmin("admin/orders.html");
    check("manager orders: CN01 only, no unassigned option", JSON.stringify(await evaluate(`[...document.querySelectorAll("#orderTableBody .js-toggle-detail")].map(b => b.dataset.orderId)`)) === '["DH000118","DH000102"]'
        && !(await evaluate(`[...document.querySelectorAll("#storeFilter option")].some(o => o.value === "UNASSIGNED")`)));
    await evaluate(`document.querySelector('.js-toggle-detail[data-order-id="DH000118"]').click()`);
    check("order detail row: items + total", await evaluate(`!document.querySelector('.js-order-detail[data-order-id="DH000118"]').hidden`)
        && (await evaluate(`document.querySelector('.js-order-detail[data-order-id="DH000118"]').textContent.includes("Ugreen X516")`))
        && (await evaluate(`[...document.querySelectorAll("#orderTableBody tr")][0].cells[5].textContent.trim()`)) === vnd(250000 * 2 + 369000 + 30000));
    await openAdmin("admin/employees.html");
    check("manager employees: CN01 staff, read-only, no add button", JSON.stringify(await evaluate(`[...document.querySelectorAll("#employeeTableBody tr")].map(r => r.cells[1].textContent.trim())`)) === '["lan.quanly@poy.vn","hoa.nv@poy.vn","hung.nv@poy.vn"]'
        && (await evaluate(`document.querySelectorAll(".js-toggle-status").length === 0 && document.getElementById("toggleCreateFormBtn").hidden`)));
    await openAdmin("admin/stores.html");
    check("manager → stores.html denied", (await path()) === "admin/dashboard.html?denied=1");

    // 5. ADMIN (switching account: log out first; the login page sends a logged-in user to the dashboard)
    await open("admin/login.html");
    check("login page while logged in → dashboard", (await path()) === "admin/dashboard.html", await path());
    await evaluate(`document.getElementById("adminLogoutBtn").click()`);
    await waitFor(`location.pathname.endsWith("/admin/login.html")`);
    await login("admin@poy.vn", "admin123");
    check("admin: full menu", (await visibleNav()).length === 8);
    check("admin: poy_staff_auth roles = [ADMIN]", (await evaluate(`JSON.parse(localStorage.getItem("poy_staff_auth")).user.roles[0]`)) === "ADMIN");
    const adminCards = await evaluate(`[...document.querySelectorAll(".admin-stat-card .admin-stat-value")].map(v => v.textContent.trim())`);
    check("admin dashboard: 4 cards, system revenue, store breakdown", adminCards.length === 4 && adminCards[0] === vnd(ALL_REVENUE)
        && !(await evaluate(`document.getElementById("storeBreakdownPanel").hidden`)), adminCards.join(" | "));
    await shot("admin-admin-dashboard.png");
    await openAdmin("admin/orders.html");
    await evaluate(`(() => { const s = document.getElementById("storeFilter"); s.value = "UNASSIGNED"; s.dispatchEvent(new Event("change")); })()`);
    check("admin orders: unassigned filter → DH000135", JSON.stringify(await evaluate(`[...document.querySelectorAll("#orderTableBody .js-toggle-detail")].map(b => b.dataset.orderId)`)) === '["DH000135"]');
    await openAdmin("admin/reports.html");
    check("admin reports: store comparison chart", await evaluate(`!document.getElementById("reportStoreChartPanel").hidden && Boolean(document.querySelector("#reportStoreChart svg"))`));

    // employees: self protection, lock → session check, add with validation, XSS
    await openAdmin("admin/employees.html");
    check("admin employees: all 7, own row without toggle", (await evaluate(`document.querySelectorAll("#employeeTableBody tr").length`)) === 7
        && (await evaluate(`[...document.querySelectorAll("#employeeTableBody tr")].find(r => r.cells[1].textContent.includes("admin@poy.vn")).querySelector(".js-toggle-status") === null`)));
    await evaluate(`document.querySelector('.js-toggle-status[data-id="nv-staff-02"]').click()`);
    await sleep(200);
    check("admin locks hung.nv", (await evaluate(`document.querySelector('.js-toggle-status[data-id="nv-staff-02"]').dataset.status`)) === "INACTIVE");
    await evaluate(`document.getElementById("toggleCreateFormBtn").click()`);
    const submitEmployee = async (fields) => {
        await evaluate(`Object.entries(${JSON.stringify(fields)}).forEach(([id, v]) => { const e = document.getElementById(id); e.value = v; e.dispatchEvent(new Event("change")); });
            document.getElementById("createEmployeeForm").requestSubmit()`);
        await sleep(200);
    };
    await submitEmployee({ newFullname: "A", newEmail: "khong-hop-le" });
    check("add employee: invalid email → error", (await text("#createEmployeeError")) === "Email không hợp lệ.");
    await submitEmployee({ newFullname: "A", newEmail: "HOA.NV@poy.vn" });
    check("add employee: duplicate email → error", ((await text("#createEmployeeError")) || "").includes("đã được dùng"));
    await submitEmployee({ newFullname: '<img src=x onerror="window.__xss=1">Mới', newEmail: "moi.nv@poy.vn", newPhone: "0908 000 111", newPosition: "Thu ngân", newRole: "EMPLOYEE", newStoreId: "CN02" });
    check("add employee: row added, name shown as text (no XSS)", (await evaluate(`document.querySelectorAll("#employeeTableBody tr").length`)) === 8
        && (await evaluate(`window.__xss === undefined && document.querySelectorAll("#employeeTableBody img").length === 0`))
        && (await evaluate(`document.getElementById("employeeTableBody").textContent.includes('<img src=x onerror="window.__xss=1">Mới')`)));

    // stores
    await openAdmin("admin/stores.html");
    await evaluate(`document.getElementById("toggleCreateFormBtn").click();
        document.getElementById("newStoreName").value = "Chi nhánh Quận 1"; document.getElementById("newStoreAddress").value = "x";
        document.getElementById("createStoreForm").requestSubmit()`);
    await sleep(200);
    check("add store: duplicate name → error", ((await text("#createStoreError")) || "").includes("Đã có chi nhánh"));
    await evaluate(`document.getElementById("newStoreName").value = "Chi nhánh Thủ Đức"; document.getElementById("newStoreAddress").value = "1 Võ Văn Ngân, Thủ Đức";
        document.getElementById("createStoreForm").requestSubmit()`);
    await sleep(200);
    check("add store: CN04 appears, persists", (await evaluate(`document.querySelectorAll("#storeTableBody tr").length`)) === 4
        && (await evaluate(`document.getElementById("storeTableBody").textContent.includes("CN04")`)));
    await openAdmin("admin/employees.html");
    check("new store shows in the employees filter", await evaluate(`[...document.querySelectorAll("#storeFilter option")].some(o => o.value === "CN04")`));

    // chat history
    await openAdmin("admin/chat-history.html");
    check("chat history: 3 sessions, newest selected, messages shown", (await evaluate(`document.querySelectorAll(".chat-session-item").length`)) === 3
        && (await evaluate(`document.querySelector(".chat-session-item.active").dataset.id`)) === "CHAT-0003"
        && (await evaluate(`document.querySelectorAll(".admin-chat-message").length`)) === 2);
    await evaluate(`document.querySelector('.chat-session-item[data-id="CHAT-0001"]').click()`);
    check("chat history: switch session", (await evaluate(`document.querySelectorAll(".admin-chat-message.from-user").length`)) === 2);
    await shot("admin-chat.png");

    // 6. a locked account loses its session on the next page
    await evaluate(`saveStaffSession(getEmployeeById("nv-staff-02"))`);
    await open("admin/dashboard.html");
    await waitFor(`location.pathname.endsWith("/admin/login.html")`);
    await sleep(300);
    check("locked account with a session → login?reason=locked + message", (await path()).includes("reason=locked")
        && ((await text("#staffLoginError")) || "").includes("đã bị khoá") && (await evaluate(`localStorage.getItem("poy_staff_auth")`)) === null);
    await login("hung.nv@poy.vn", "nhanvien123");
    check("locked account cannot log in", ((await text("#staffLoginError")) || "").includes("đã bị khoá"));
    await login("moi.nv@poy.vn", "nhanvien123");
    check("new employee can log in (default demo password), store CN02", (await text("#adminUserMeta")) === "Nhân viên · Chi nhánh Quận 5");

    // 7. mobile
    await send("Emulation.setDeviceMetricsOverride", { width: 390, height: 844, deviceScaleFactor: 2, mobile: true });
    await openAdmin("admin/service-requests.html");
    const offscreen = await evaluate(`document.getElementById("adminSidebar").getBoundingClientRect().right <= 0`);
    await evaluate(`document.getElementById("adminSidebarToggle").click()`);
    await sleep(400);
    check("mobile: sidebar off-canvas, toggle opens it, no page overflow", offscreen && (await evaluate(`document.getElementById("adminSidebar").getBoundingClientRect().left`)) === 0
        && (await evaluate(`document.documentElement.scrollWidth - window.innerWidth`)) <= 0);
    await shot("admin-mobile.png");

    check("no missing static files", missing.length === 0, missing.join(", "));
    check("no JS exceptions / console errors or warnings", jsProblems.length === 0, jsProblems.join(" | "));
} catch (error) {
    check("script error", false, error.stack);
} finally {
    ws.close();
    edge.kill();
}

const failed = results.filter(r => !r.ok).length;
console.log(`\n${results.length - failed}/${results.length} checks passed`);
process.exit(failed ? 1 : 0);
