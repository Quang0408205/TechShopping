// Headless Edge (CDP) E2E for Checkpoint 2.8 (extended): customer/products.html + index featured grid.
// Needs: backend on the DEV profile (read-only GETs over the 877 real products), CORS for ORIGIN,
// and static-server.mjs serving the REPO ROOT on ORIGIN's port. No data is written.
import { spawn } from "node:child_process";
import { mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";

const EDGE = "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe";
const ORIGIN = process.env.E2E_ORIGIN || "http://127.0.0.1:5501";
const SITE = `${ORIGIN}/frontend`;
const API = "http://localhost:8080/api/v1";
const SHOTS = process.argv[2] || ".";
const PORT = 9337;

const sleep = ms => new Promise(r => setTimeout(r, ms));
const results = [];
function check(name, ok, detail = "") {
    results.push({ name, ok });
    console.log(`${ok ? "PASS" : "FAIL"}  ${name}${detail !== "" ? "  -> " + detail : ""}`);
}
async function api(path) {
    const body = await (await fetch(API + path)).json();
    if (!body.success) throw new Error(path + " " + JSON.stringify(body.error));
    return body.data;
}

// ---- expected values straight from the API ----
const cats = (await api("/categories?size=100")).content;
const catId = slug => cats.find(c => c.slug === slug).id;
const all = await api("/products?isActive=true&size=12&page=0&sort=id,asc");
const phones = await api(`/products?isActive=true&categoryId=${catId("dien-thoai")}&size=100&page=0&sort=id`);
let phoneProducts = phones.content;
for (let p = 1; p < phones.totalPages; p++) phoneProducts = phoneProducts.concat((await api(`/products?isActive=true&categoryId=${catId("dien-thoai")}&size=100&page=${p}&sort=id`)).content);
const phoneBrands = [...new Map(phoneProducts.map(p => [p.brandId, p.brandName])).entries()];
const laptopTotal = (await api(`/products?isActive=true&categoryId=${catId("laptop")}&size=1`)).totalElements;
let laptopBrandIds = new Set();
for (let p = 0; p * 100 < laptopTotal; p++) (await api(`/products?isActive=true&categoryId=${catId("laptop")}&size=100&page=${p}`)).content.forEach(x => laptopBrandIds.add(x.brandId));
const samsung = phoneBrands.find(([, n]) => n === "Samsung");
const samsungTotal = (await api(`/products?isActive=true&categoryId=${catId("dien-thoai")}&brandId=${samsung[0]}&size=1`)).totalElements;
const priceTotal = (await api("/products?isActive=true&minPrice=10000000&maxPrice=20000000&size=1")).totalElements;
const iphoneTotal = (await api("/products?isActive=true&keyword=iphone&size=1")).totalElements;
const priceDescFirst = (await api("/products?isActive=true&size=1&sort=basePrice,desc")).content[0];
const priceAscFirst = (await api("/products?isActive=true&size=1&sort=basePrice,asc")).content[0];
const page2First = (await api("/products?isActive=true&size=12&page=1&sort=id,asc")).content[0];
console.log(`API: total ${all.totalElements}/${all.totalPages} pages, phones ${phones.totalElements} (${phoneBrands.length} brands), laptop brands ${laptopBrandIds.size}, samsung ${samsungTotal}, 10-20tr ${priceTotal}, iphone ${iphoneTotal}`);

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
const text = selector => evaluate(`document.querySelector(${JSON.stringify(selector)})?.textContent.trim()`);
const cardCount = () => evaluate(`document.querySelectorAll("#productGrid .product-card:not(.skeleton-card)").length`);
const gridReady = `!document.querySelector("#productGrid .skeleton-card") && document.querySelector("#productGrid").children.length > 0`;
const countNumber = async () => Number(((await text("#resultCount")) || "").replace(/\D/g, "").slice(0, 6) || "0");
async function waitGrid() { await sleep(100); return waitFor(gridReady); }
async function shot(name) {
    const { result } = await send("Page.captureScreenshot", { format: "png", captureBeyondViewport: true });
    writeFileSync(join(SHOTS, name), Buffer.from(result.data, "base64"));
}
async function viewportShot(name) {
    const { result } = await send("Page.captureScreenshot", { format: "png" });
    writeFileSync(join(SHOTS, name), Buffer.from(result.data, "base64"));
}
const page = (content, n = 0) => ({ success: true, timestamp: "2026-09-26T00:00:00", error: null,
    data: { content, page: n, size: 12, totalElements: content.length, totalPages: content.length ? 1 : 0 } });

await send("Page.enable");
await send("Runtime.enable");
await send("Network.enable");
await send("Fetch.enable", { patterns: [{ urlPattern: "http://localhost:8080/api/v1/products*", requestStage: "Request" }] });
await send("Emulation.setDeviceMetricsOverride", { width: 1280, height: 900, deviceScaleFactor: 1, mobile: false });
await send("Page.addScriptToEvaluateOnNewDocument", { source: "window.__alerts = []; window.alert = m => window.__alerts.push(m);" });

try {
    // 1. initial load: 12 cards, total count, pagination
    requests.length = 0;
    await navigate(`${SITE}/customer/products.html`);
    await waitGrid();
    check("products: 12 cards from the API", (await cardCount()) === 12, await cardCount());
    check("products: count = API totalElements", (await countNumber()) === all.totalElements, await text("#resultCount"));
    const firstName = await evaluate(`document.querySelector("#productGrid .product-card h3").textContent.trim()`);
    check("products: first card = first product (sort id)", firstName === all.content[0].name, firstName);
    const pag = await evaluate(`[...document.querySelectorAll("#pagination .page-btn")].map(b => b.textContent.trim())`);
    check("pagination: compact (‹ 1 2 … last ›)", JSON.stringify(pag) === JSON.stringify(["‹", "1", "2", String(all.totalPages), "›"])
        && (await evaluate(`document.querySelectorAll("#pagination .page-ellipsis").length`)) === 1, pag.join(" "));
    await waitFor(`[...document.querySelectorAll("#productGrid img")].some(i => i.src.startsWith("https://"))`);
    await sleep(800);
    const imageCalls = requests.filter(u => /\/products\/\d+\/images$/.test(u)).length;
    check("images: one /images request per card (12)", imageCalls === 12, imageCalls);
    const remoteImgs = await evaluate(`[...document.querySelectorAll("#productGrid img")].filter(i => i.src.startsWith("https://")).length`);
    check("images: cards show the real image URL", remoteImgs >= 10, remoteImgs + "/12");
    check("category list: GET /categories without token", requests.some(u => u.includes("/categories?size=100")));
    check("brand filter hint when 'Tất cả'", ((await text("#brandOptionsList")) || "").includes("Chọn một danh mục"));
    await shot("products-desktop.png");

    // 2. sticky sidebar
    await evaluate(`window.scrollTo(0, 1400)`);
    await sleep(400);
    const sideTop = await evaluate(`Math.round(document.getElementById("productsSidebar").getBoundingClientRect().top)`);
    check("sidebar stays fixed while scrolling (top ≈ 105px)", Math.abs(sideTop - 105) <= 2, sideTop);
    await viewportShot("products-scrolled-sticky.png");
    await evaluate(`window.scrollTo(0, 0)`);

    // 3. category → brands of that category
    requests.length = 0;
    await evaluate(`document.querySelector('input[name="category"][value="phone"]').click()`);
    await waitGrid();
    check("category phone: count = API", (await countNumber()) === phones.totalElements, await text("#resultCount"));
    const labels = await evaluate(`[...document.querySelectorAll("#productGrid .product-category")].map(e => e.textContent.trim())`);
    check("category phone: every card is ĐIỆN THOẠI", labels.length > 0 && labels.every(l => l === "ĐIỆN THOẠI"), [...new Set(labels)].join(","));
    check("category phone: URL ?category=phone", (await evaluate(`location.search`)) === "?category=phone");
    await waitFor(`document.querySelectorAll('#brandOptionsList input[name="brand"]').length > 1`);
    const brandNames = await evaluate(`[...document.querySelectorAll('#brandOptionsList label')].map(l => l.textContent.trim())`);
    check("brands: exactly the brands present in Điện thoại (+ Tất cả)",
        brandNames.length === phoneBrands.length + 1 && phoneBrands.every(([, n]) => brandNames.includes(n)), `${brandNames.length - 1} brands`);
    check("brands: title mentions the category", (await text("#brandFilterTitle")) === "Thương hiệu (Điện thoại)");

    // 4. brand filter
    await evaluate(`[...document.querySelectorAll('#brandOptionsList input')].find(i => i.parentElement.textContent.trim() === "Samsung").click()`);
    await waitGrid();
    check("brand Samsung: count = API (categoryId + brandId)", (await countNumber()) === samsungTotal, await text("#resultCount"));
    const samsungNames = await evaluate(`[...document.querySelectorAll("#productGrid h3")].map(h => h.textContent.trim())`);
    check("brand Samsung: cards are Samsung", samsungNames.every(n => /samsung/i.test(n)), samsungNames[0]);
    await shot("products-phone-samsung.png");

    // 5. switching category rebuilds the brand list and resets the brand
    requests.length = 0;
    await evaluate(`document.querySelector('input[name="category"][value="laptop"]').click()`);
    await waitGrid();
    await waitFor(`document.querySelectorAll('#brandOptionsList input[name="brand"]').length > 1`);
    const laptopBrandCount = await evaluate(`document.querySelectorAll('#brandOptionsList input[name="brand"]').length - 1`);
    check("category laptop: brand list rebuilt", laptopBrandCount === laptopBrandIds.size, laptopBrandCount);
    check("category laptop: brand reset to Tất cả", await evaluate(`document.querySelector('#brandOptionsList input[value=""]').checked`));
    const scans = requests.filter(u => u.includes("size=100") && u.includes("/products?")).length;
    check("brand scan for Laptop: ≤ 5 requests of 100", scans > 0 && scans <= 5, scans);
    requests.length = 0;
    await evaluate(`document.querySelector('input[name="category"][value="phone"]').click()`);
    await waitGrid();
    await waitFor(`document.querySelectorAll('#brandOptionsList input[name="brand"]').length > 1`);
    check("brands cached: back to phone makes no new scan", requests.filter(u => u.includes("size=100") && u.includes("/products?")).length === 0);

    // 6. price range + validation
    await evaluate(`document.querySelector('input[name="category"][value=""]').click()`);
    await waitGrid();
    await evaluate(`(() => { const a = document.getElementById("minPriceInput"), b = document.getElementById("maxPriceInput");
        a.value = "10000000"; a.dispatchEvent(new Event("change")); b.value = "20000000"; b.dispatchEvent(new Event("change")); })()`);
    await waitGrid();
    await sleep(300);
    await waitGrid();
    check("price 10–20 triệu: count = API", (await countNumber()) === priceTotal, await text("#resultCount"));
    requests.length = 0;
    await evaluate(`(() => { const a = document.getElementById("minPriceInput"); a.value = "30000000"; a.dispatchEvent(new Event("change")); })()`);
    await sleep(300);
    check("price min > max: error shown, no request", ((await text("#priceError")) || "").includes("nhỏ hơn hoặc bằng")
        && !requests.some(u => u.includes("/products?")), await text("#priceError"));

    // 7. clear filters
    await evaluate(`document.getElementById("clearFiltersBtn").click()`);
    await waitGrid();
    check("clear filters: back to all products", (await countNumber()) === all.totalElements
        && (await evaluate(`document.getElementById("minPriceInput").value === "" && document.getElementById("priceError").textContent === ""`)));

    // 8. sort
    await evaluate(`(() => { const s = document.getElementById("sortSelect"); s.value = "basePrice,desc"; s.dispatchEvent(new Event("change")); })()`);
    await waitGrid();
    check("sort price desc: first card = most expensive", (await text("#productGrid h3")) === priceDescFirst.name, await text("#productGrid h3"));
    await evaluate(`(() => { const s = document.getElementById("sortSelect"); s.value = "basePrice,asc"; s.dispatchEvent(new Event("change")); })()`);
    await waitGrid();
    const zero = await evaluate(`(() => { const c = document.querySelector("#productGrid .product-card");
        return { price: c.querySelector(".product-price").textContent.trim(), disabled: c.querySelector(".add-cart").disabled }; })()`);
    check("price 0 product: 'Liên hệ' + add-to-cart disabled", Number(priceAscFirst.basePrice) === 0 && zero.price === "Liên hệ" && zero.disabled, JSON.stringify(zero));
    await evaluate(`(() => { const s = document.getElementById("sortSelect"); s.value = "id,asc"; s.dispatchEvent(new Event("change")); })()`);
    await waitGrid();

    // 9. pagination
    await evaluate(`[...document.querySelectorAll("#pagination .page-btn")].find(b => b.textContent.trim() === "2").click()`);
    await waitGrid();
    check("pagination: page 2 shows the next 12", (await text("#productGrid h3")) === page2First.name
        && (await text("#pagination .page-btn.active")) === "2", await text("#productGrid h3"));
    await evaluate(`[...document.querySelectorAll("#pagination .page-btn")].find(b => b.textContent.trim() === "${all.totalPages}").click()`);
    await waitGrid();
    const lastCount = await cardCount();
    check("pagination: last page has the remainder, › disabled",
        lastCount === all.totalElements - (all.totalPages - 1) * 12 && (await evaluate(`document.querySelector('#pagination [aria-label="Trang sau"]').disabled`)), lastCount);

    // 10. add to cart on rendered cards: toast, no alert, no double binding
    await evaluate(`localStorage.removeItem("poy_cart")`);
    await navigate(`${SITE}/customer/products.html`);
    await waitGrid();
    await evaluate(`document.querySelector("#productGrid .add-cart:not(:disabled)").click()`);
    await sleep(200);
    const cart = await evaluate(`JSON.parse(localStorage.getItem("poy_cart"))`);
    check("add to cart: item stored {name, price, quantity: 1}", cart && cart.length === 1 && cart[0].name === all.content[0].name
        && cart[0].quantity === 1 && cart[0].price === Number(all.content[0].discountPrice ?? all.content[0].basePrice), JSON.stringify(cart));
    check("add to cart: header counter = 1", (await text(".header .cart-count")) === "1");
    check("add to cart: toast instead of alert()", ((await text(".toast")) || "").includes("vào giỏ hàng")
        && (await evaluate(`window.__alerts.length`)) === 0, await text(".toast"));
    await evaluate(`setupAddToCart(document)`); // re-binding must not add a second listener
    await evaluate(`document.querySelector("#productGrid .add-cart:not(:disabled)").click()`);
    await sleep(200);
    check("add to cart: no double binding (quantity 2 after 2 clicks)",
        (await evaluate(`JSON.parse(localStorage.getItem("poy_cart"))[0].quantity`)) === 2);
    await shot("products-toast.png");

    // 10b. storage keys renamed lahy_* → poy_*: old values are moved once by api.js
    await evaluate(`localStorage.removeItem("poy_cart"); localStorage.removeItem("poy_auth");
        localStorage.setItem("lahy_cart", JSON.stringify([{ name: "Cũ", price: 1000, quantity: 2 }]));
        localStorage.setItem("lahy_auth", JSON.stringify({ accessToken: "a", refreshToken: "r", user: { id: 1, username: "cu", fullname: "Khách Cũ", roles: ["CUSTOMER"] } }))`);
    await navigate(`${SITE}/customer/products.html`);
    await waitGrid();
    const migrated = await evaluate(`({ cart: localStorage.getItem("poy_cart"), auth: localStorage.getItem("poy_auth"),
        oldCart: localStorage.getItem("lahy_cart"), oldAuth: localStorage.getItem("lahy_auth") })`);
    check("storage: lahy_cart / lahy_auth migrated to poy_* and removed", migrated.oldCart === null && migrated.oldAuth === null
        && JSON.parse(migrated.cart)[0].quantity === 2 && JSON.parse(migrated.auth).user.fullname === "Khách Cũ"
        && (await text(".header .cart-count")) === "2" && (await text(".header .login-btn")) === "Khách Cũ", JSON.stringify(migrated));
    await evaluate(`localStorage.removeItem("poy_auth"); localStorage.removeItem("poy_cart")`);
    await navigate(`${SITE}/customer/products.html`);
    await waitGrid();

    // 11. header search → ?search= → keyword
    await evaluate(`(() => { const f = document.querySelector(".header .search-form"); f.querySelector("input").value = "iphone"; f.requestSubmit(); })()`);
    await waitFor(`location.search === "?search=iphone" && document.readyState === "complete"`);
    await waitGrid();
    check("header search: lands on products.html?search=iphone", (await evaluate(`location.pathname.endsWith("/customer/products.html") && location.search === "?search=iphone"`)));
    check("header search: count = API keyword", (await countNumber()) === iphoneTotal, await text("#resultCount"));
    check("header search: input keeps the keyword", (await waitFor(`document.querySelector(".header .search-form input").value === "iphone"`)) === true);
    await navigate(`${SITE}/customer/products.html?search=zzzqqqxxx`);
    await waitGrid();
    check("search without results: empty state", ((await text("#productGrid .empty-state h3")) || "").includes("Không tìm thấy"));

    // 12. ?category=phone deep link (index category cards)
    await navigate(`${SITE}/customer/products.html?category=phone`);
    await waitGrid();
    check("?category=phone preselects the radio", await evaluate(`document.querySelector('input[name="category"][value="phone"]').checked`)
        && (await countNumber()) === phones.totalElements);

    // 13. fallback image when a product has no image (mocked empty list)
    mocks.push({ method: "GET", path: `/api/v1/products/${all.content[0].id}/images`, status: 200, body: { success: true, data: [], error: null } });
    await navigate(`${SITE}/customer/products.html`);
    await waitGrid();
    await sleep(800);
    const firstImg = await evaluate(`document.querySelector("#productGrid img").src`);
    const expected = { "dien-thoai": "phone.png", "laptop": "laptop.png", "may-tinh-bang": "tablet.png", "phu-kien": "banphim.png" }[cats.find(c => c.id === all.content[0].categoryId).slug] || "phone.png";
    check("no image → local fallback of the category", firstImg.endsWith("/assets/images/" + expected), firstImg);

    // 14. XSS: product names are shown as text
    const evil = { ...all.content[0], id: 999999, name: `<img src=x onerror="window.__xss=1">Evil`, categoryName: "<b>X</b>" };
    mocks.push({ method: "GET", path: "/api/v1/products", status: 200, body: page([evil]) });
    mocks.push({ method: "GET", path: "/api/v1/products/999999/images", status: 200, body: { success: true, data: [], error: null } });
    await navigate(`${SITE}/customer/products.html`);
    await waitGrid();
    await sleep(500);
    check("XSS: name rendered as text, no script ran", (await evaluate(`window.__xss === undefined`))
        && ((await text("#productGrid h3")) || "").startsWith("<img") && (await evaluate(`document.querySelectorAll("#productGrid h3 img, #productGrid .product-category b").length`)) === 0);

    // 15. API error → error box + retry
    mocks.push({ method: "GET", path: "/api/v1/products", status: 500,
        body: { success: false, data: null, error: { code: "INTERNAL_ERROR", message: "boom", details: null } } });
    await navigate(`${SITE}/customer/products.html`);
    await waitFor(`document.querySelector("#productGrid .error-state")`);
    check("API error: error box with THỬ LẠI", Boolean(await evaluate(`document.getElementById("productsRetryBtn")`)));
    await evaluate(`document.getElementById("productsRetryBtn").click()`);
    await waitGrid();
    check("retry: products load", (await cardCount()) === 12);

    // 16. index featured grid
    await navigate(`${SITE}/index.html`);
    await waitFor(`document.querySelectorAll("#featuredProductGrid .product-card:not(.skeleton-card)").length === 4`);
    const featured = await evaluate(`[...document.querySelectorAll("#featuredProductGrid h3")].map(h => h.textContent.trim())`);
    check("index: 4 featured products from the API", featured.length === 4 && featured.every((n, i) => n === all.content[i].name), featured[0]);
    check("index: section order unchanged (AI section still there)", Boolean(await evaluate(`document.querySelector(".ai-section")`)));
    await sleep(800);
    await evaluate(`document.querySelector(".products-preview").scrollIntoView()`);
    await sleep(300);
    await viewportShot("index-featured.png");

    // 17. logged-in header at 1100px still fits on one line
    await send("Emulation.setDeviceMetricsOverride", { width: 1100, height: 800, deviceScaleFactor: 1, mobile: false });
    await evaluate(`localStorage.setItem("poy_auth", JSON.stringify({ accessToken: "x", refreshToken: "y",
        user: { id: 1, email: "a@b.c", username: "khach", fullname: "Nguyễn Thị Giao Diện", roles: ["CUSTOMER"] } }))`);
    await navigate(`${SITE}/customer/products.html`);
    await waitGrid();
    const headerH = await evaluate(`Math.round(document.querySelector(".header").getBoundingClientRect().height)`);
    check("header 1100px logged in: single row (≤ 90px)", headerH <= 90, headerH + "px");
    await viewportShot("header-1100-logged-in.png");
    await evaluate(`localStorage.removeItem("poy_auth")`);

    // 18. mobile
    await send("Emulation.setDeviceMetricsOverride", { width: 390, height: 844, deviceScaleFactor: 2, mobile: true });
    await navigate(`${SITE}/customer/products.html`);
    await waitGrid();
    const mob = await evaluate(`({ toggle: getComputedStyle(document.getElementById("toggleFiltersBtn")).display,
        sidebar: getComputedStyle(document.getElementById("productsSidebar")).display,
        overflow: document.documentElement.scrollWidth - window.innerWidth,
        cols: getComputedStyle(document.getElementById("productGrid")).gridTemplateColumns.split(" ").length })`);
    check("mobile: filter toggle shown, sidebar hidden, 1 column, no horizontal scroll",
        mob.toggle !== "none" && mob.sidebar === "none" && mob.cols === 1 && mob.overflow <= 0, JSON.stringify(mob));
    await evaluate(`document.getElementById("toggleFiltersBtn").click()`);
    check("mobile: toggle opens the filters", (await evaluate(`getComputedStyle(document.getElementById("productsSidebar")).display`)) === "flex");
    await viewportShot("products-mobile.png");

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
