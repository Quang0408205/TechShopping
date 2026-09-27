// Headless Edge (CDP) E2E for Checkpoint A2: customer/product-detail.html from the real API.
// Needs: backend on the DEV profile (GETs only), CORS for ORIGIN, and static-server.mjs serving the REPO ROOT.
// Real products are used for the normal paths; CDP Fetch mocks cover what the data lacks
// (several variants / images, hostile strings, a 500). No data is written.
import { spawn } from "node:child_process";
import { mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";

const EDGE = "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe";
const ORIGIN = process.env.E2E_ORIGIN || "http://127.0.0.1:5501";
const SITE = `${ORIGIN}/frontend`;
const API = "http://localhost:8080/api/v1";
const SHOTS = process.argv[2] || ".";
const PORT = 9340;

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
const vnd = n => new Intl.NumberFormat("vi-VN").format(n) + "đ";

// ---- expected values straight from the API ----
const cats = (await api("/categories?size=100")).content;
const first = (await api("/products?isActive=true&size=1&page=0&sort=id,asc")).content[0];
const firstVariants = await api(`/products/${first.id}/variants`);
const firstImages = await api(`/products/${first.id}/images`);
const zero = (await api("/products?isActive=true&size=1&sort=basePrice,asc")).content[0];
const firstSlug = cats.find(c => c.id === first.categoryId).slug;
console.log(`API: first #${first.id} "${first.name}" (${firstVariants.length} variant, ${firstImages.length} image), price-0 #${zero.id}`);

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

const ok = data => ({ success: true, timestamp: "2026-09-26T00:00:00", data, error: null });
const detailReady = `!document.getElementById("detailSkeleton").hidden === false && (!document.getElementById("detailContent").hidden || !document.getElementById("detailNotFound").hidden || !document.getElementById("detailError").hidden)`;
const text = selector => evaluate(`document.querySelector(${JSON.stringify(selector)})?.textContent.trim()`);
const specs = () => evaluate(`Object.fromEntries([...document.querySelectorAll("#specsTable tr")].map(r => [r.querySelector("th").textContent.trim(), r.querySelector("td").textContent.trim()]))`);
async function openDetail(id) {
    await navigate(`${SITE}/customer/product-detail.html?id=${id}`);
    await waitFor(detailReady);
    await sleep(300);
}
async function shot(name, full = false) {
    const { result } = await send("Page.captureScreenshot", { format: "png", captureBeyondViewport: full });
    writeFileSync(join(SHOTS, name), Buffer.from(result.data, "base64"));
}

try {
    // 1. from the products page: card name → detail page
    await navigate(`${SITE}/customer/products.html`);
    await waitFor(`document.querySelector("#productGrid .product-card h3 a")`);
    const cardHref = await evaluate(`document.querySelector("#productGrid .product-card h3 a").href`);
    check("products card links to product-detail.html?id=", cardHref === `${SITE}/customer/product-detail.html?id=${first.id}`, cardHref);
    check("products card image links to the same page", (await evaluate(`document.querySelector("#productGrid .product-card a.product-image").href`)) === cardHref);
    requests.length = 0;
    await evaluate(`document.querySelector("#productGrid .product-card h3 a").click()`);
    await waitFor(`location.pathname.endsWith("product-detail.html")`);
    await waitFor(detailReady);
    await sleep(500);

    // 2. main info
    check("detail: name, category, title", (await text("#detailName")) === first.name
        && (await text("#detailCategory")) === first.categoryName.toUpperCase()
        && (await evaluate(`document.title`)) === first.name + " - POY", await evaluate(`document.title`));
    const price = Number(firstVariants[0]?.discountPrice ?? firstVariants[0]?.price ?? first.basePrice);
    check("detail: price of the selected variant", (await text("#detailPrice")) === vnd(price), await text("#detailPrice"));
    check("detail: meta = brand + SKU, no rating while totalReviews = 0",
        (await text("#detailMeta")) === `Thương hiệu: ${first.brandName} · Mã: ${first.sku}`, await text("#detailMeta"));
    const crumb = await evaluate(`({ text: document.getElementById("breadcrumbCategory").textContent, href: document.getElementById("breadcrumbCategory").href, cur: document.getElementById("breadcrumbCurrent").textContent })`);
    const key = { "dien-thoai": "phone", laptop: "laptop", "may-tinh-bang": "tablet", "phu-kien": "accessory" }[firstSlug];
    check("breadcrumb: category links to the filtered products page", crumb.text === first.categoryName
        && crumb.href === `${SITE}/customer/products.html` + (key ? `?category=${key}` : "") && crumb.cur === first.name, JSON.stringify(crumb));
    const primary = firstImages.find(i => i.isPrimary) || firstImages[0];
    check("image: primary image from /images", (await evaluate(`document.getElementById("mainImage").src`)) === primary.imageUrl);
    check("image: thumbnails hidden with a single image", await evaluate(`document.getElementById("detailThumbnails").hidden`));
    check("API calls: product, variants, images, specifications without token",
        ["", "/variants", "/images", "/specifications"].every(s => requests.some(u => u.endsWith(`/products/${first.id}${s}`))));

    // 3. variant + specs + description
    const variantButtons = await evaluate(`[...document.querySelectorAll(".variant-btn")].map(b => ({ label: b.textContent.trim(), active: b.classList.contains("active") }))`);
    check("variants: one button per variant, first active", variantButtons.length === firstVariants.length
        && variantButtons[0].active && variantButtons[0].label === firstVariants[0].variantName, JSON.stringify(variantButtons));
    const s = await specs();
    check("specs: built from real fields", s["Thương hiệu"] === first.brandName && s["Danh mục"] === first.categoryName
        && s["Phiên bản"] === firstVariants[0].variantName && s["Bảo hành"] === first.warrantyMonths + " tháng"
        && s["Mã sản phẩm"] === (firstVariants[0].skuVariant || first.sku)
        && (firstVariants[0].storage ? s["Bộ nhớ trong"] === firstVariants[0].storage : true), JSON.stringify(s));
    check("description tab: placeholder when empty", ((await evaluate(`document.getElementById("detailDescription").textContent`)) || "").includes("đang được cập nhật"));
    await evaluate(`document.querySelector('.tab-btn[data-tab="description"]').click()`);
    check("tabs: switch to description", await evaluate(`document.querySelector('[data-tab-panel="description"]').classList.contains("active")
        && !document.querySelector('[data-tab-panel="specs"]').classList.contains("active")
        && document.querySelector('.tab-btn[data-tab="description"]').classList.contains("active")
        && !document.querySelector('.tab-btn[data-tab="specs"]').classList.contains("active")`));
    check("warranty line from warrantyMonths", (await text("#detailWarranty")) === `Bảo hành chính hãng ${first.warrantyMonths} tháng`);

    // 4. related products
    await waitFor(`document.querySelectorAll("#relatedProductsGrid .product-card").length > 0`);
    const related = await evaluate(`[...document.querySelectorAll("#relatedProductsGrid .product-card")].map(c => ({ id: c.dataset.productId, cat: c.querySelector(".product-category").textContent.trim() }))`);
    check("related: 4 products of the same category, not the current one", related.length === 4
        && related.every(r => r.id !== String(first.id) && r.cat === first.categoryName.toUpperCase()), JSON.stringify(related.map(r => r.id)));
    await sleep(500);
    await shot("detail-desktop.png", true);

    // 5. quantity + add to cart (merges with the list page item)
    await evaluate(`localStorage.removeItem("poy_cart")`);
    await evaluate(`document.getElementById("qtyPlus").click(); document.getElementById("qtyPlus").click();`);
    check("quantity: + twice → 3", (await text("#qtyValue")) === "3");
    await evaluate(`document.getElementById("addToCartBtn").click()`);
    await sleep(200);
    let cart = await evaluate(`JSON.parse(localStorage.getItem("poy_cart"))`);
    check("add to cart: {name, price, quantity 3}", cart?.length === 1 && cart[0].name === first.name && cart[0].price === price && cart[0].quantity === 3, JSON.stringify(cart));
    check("add to cart: toast '3 × …', counter 3, no alert()", ((await text(".toast")) || "").startsWith("Đã thêm 3 × ")
        && (await text(".header .cart-count")) === "3" && (await evaluate(`window.__alerts.length`)) === 0);
    await evaluate(`document.getElementById("qtyMinus").click(); document.getElementById("qtyMinus").click(); document.getElementById("qtyMinus").click();`);
    check("quantity: never below 1", (await text("#qtyValue")) === "1");
    await evaluate(`for (let i = 0; i < 12; i++) document.getElementById("qtyPlus").click();`);
    check("quantity: capped at 10 with a message", (await text("#qtyValue")) === "10" && ((await evaluate(`[...document.querySelectorAll(".toast")].map(t => t.textContent).join("|")`)) || "").includes("tối đa 10"));
    await navigate(`${SITE}/customer/products.html`);
    await waitFor(`document.querySelector("#productGrid .add-cart:not(:disabled)")`);
    await evaluate(`document.querySelector("#productGrid .add-cart:not(:disabled)").click()`);
    await sleep(200);
    cart = await evaluate(`JSON.parse(localStorage.getItem("poy_cart"))`);
    check("cart: same product from the list merges into one line (quantity 4)", cart.length === 1 && cart[0].quantity === 4, JSON.stringify(cart));

    // 6. buy now → cart page
    await openDetail(first.id);
    await evaluate(`document.getElementById("buyNowBtn").click()`);
    await waitFor(`location.pathname.endsWith("/customer/cart.html")`);
    await sleep(800);
    check("buy now: goes to the cart with the item", (await evaluate(`location.pathname.endsWith("/customer/cart.html")`))
        && (await evaluate(`document.body.innerText.includes(${JSON.stringify(first.name)})`))
        && (await evaluate(`JSON.parse(localStorage.getItem("poy_cart"))[0].quantity`)) === 5);

    // 7. price-0 product
    await openDetail(zero.id);
    check("price 0: 'Liên hệ', buttons disabled, contact note", (await text("#detailPrice")) === "Liên hệ"
        && (await evaluate(`document.getElementById("addToCartBtn").disabled && document.getElementById("buyNowBtn").disabled && !document.getElementById("contactNote").hidden`)));

    // 8. not found / bad id / error + retry
    await openDetail(99999999);
    check("unknown id → not found box", await evaluate(`!document.getElementById("detailNotFound").hidden && document.getElementById("detailContent").hidden`));
    requests.length = 0;
    await openDetail("abc");
    check("non-numeric id → not found, no API call", (await evaluate(`!document.getElementById("detailNotFound").hidden`))
        && !requests.some(u => u.includes("/api/v1/products/")));
    mocks.push({ method: "GET", path: `/api/v1/products/${first.id}`, status: 500,
        body: { success: false, data: null, error: { code: "INTERNAL_ERROR", message: "boom", details: null } } });
    await openDetail(first.id);
    check("API 500 → error box with THỬ LẠI", Boolean(await evaluate(`!document.getElementById("detailError").hidden && document.getElementById("detailRetryBtn")`)));
    await evaluate(`document.getElementById("detailRetryBtn").click()`);
    await waitFor(`!document.getElementById("detailContent").hidden`);
    check("retry → product shown", (await text("#detailName")) === first.name);

    // 9. several variants + images, hostile strings (mocked)
    const evil = `<img src=x onerror="window.__xss=1">`;
    const mockProduct = { ...first, id: 999999, name: evil + "Máy thử", brandName: evil + "Hãng", brandId: first.brandId, description: evil + "Mô tả" };
    const variantsMock = [
        { ...firstVariants[0], id: 1, productId: 999999, variantName: "8GB/128GB - Đen", price: 10000000, discountPrice: 9000000, ram: "8 GB", storage: "128 GB", color: "Đen", attributeValues: [{ id: 1, attributeId: 9, attributeName: "Chip", value: evil + "A18" }] },
        { ...firstVariants[0], id: 2, productId: 999999, variantName: evil + "12GB/256GB", price: 12000000, discountPrice: null, ram: "12 GB", storage: "256 GB", color: "Trắng", attributeValues: [] }
    ];
    mocks.push({ method: "GET", path: "/api/v1/products/999999", status: 200, body: ok(mockProduct) });
    mocks.push({ method: "GET", path: "/api/v1/products/999999/variants", status: 200, body: ok(variantsMock) });
    mocks.push({ method: "GET", path: "/api/v1/products/999999/images", status: 200, body: ok([
        { id: 1, productId: 999999, imageUrl: primary.imageUrl, isPrimary: false, displayOrder: 2 },
        { id: 2, productId: 999999, imageUrl: firstImages[0].imageUrl + "?b", isPrimary: true, displayOrder: 1 },
        { id: 3, productId: 999999, imageUrl: "javascript:alert(1)", isPrimary: false, displayOrder: 3 }] ) });
    mocks.push({ method: "GET", path: "/api/v1/products/999999/specifications", status: 200, body: ok([
        { id: 1, productId: 999999, specName: "Màn hình", specValue: "6.1 inch", specOrder: 2 },
        { id: 2, productId: 999999, specName: evil + "Pin", specValue: "4000 mAh", specOrder: 1 }]) });
    await openDetail(999999);
    check("XSS: name / brand / description / variant / spec rendered as text, no script ran",
        (await evaluate(`window.__xss === undefined && document.querySelectorAll("#detailContent img[src='x'], #specsTable img, .variant-btn img").length === 0`))
        && ((await text("#detailName")) || "").startsWith("<img"));
    check("images: primary first, unsafe URL dropped, 2 thumbnails", (await evaluate(`document.getElementById("mainImage").src`)) === firstImages[0].imageUrl + "?b"
        && (await evaluate(`document.querySelectorAll(".detail-thumb").length`)) === 2);
    await evaluate(`document.querySelectorAll(".detail-thumb")[1].click()`);
    check("thumbnail click switches the main image", (await evaluate(`document.getElementById("mainImage").src`)) === primary.imageUrl);
    check("variant 1: discount price + old price", (await text("#detailPrice")) === vnd(9000000) && (await text("#detailPriceOld")) === vnd(10000000));
    let ms = await specs();
    check("specs: variant fields + extra attribute + specifications in specOrder", ms["RAM"] === "8 GB" && ms["Màu sắc"] === "Đen"
        && ms["Chip"] === evil + "A18" && Object.keys(ms).indexOf(evil + "Pin") < Object.keys(ms).indexOf("Màn hình"), JSON.stringify(Object.keys(ms)));
    await evaluate(`document.querySelectorAll(".variant-btn")[1].click()`);
    ms = await specs();
    check("variant 2: price and specs follow the selection", (await text("#detailPrice")) === vnd(12000000)
        && (await evaluate(`document.getElementById("detailPriceOld").hidden`)) && ms["RAM"] === "12 GB" && ms["Chip"] === undefined
        && (await evaluate(`[...document.querySelectorAll(".variant-btn")].map(b => b.classList.contains("active")).join()`)) === "false,true");
    await evaluate(`localStorage.removeItem("poy_cart"); document.getElementById("addToCartBtn").click()`);
    cart = await evaluate(`JSON.parse(localStorage.getItem("poy_cart"))`);
    check("several variants: cart name includes the variant label", cart[0].name === mockProduct.name + " (" + evil + "12GB/256GB)" && cart[0].price === 12000000, cart[0].name);
    await sleep(500);
    await shot("detail-variants-mock.png");

    // 10. fallback image when there is none
    mocks.push({ method: "GET", path: `/api/v1/products/${first.id}/images`, status: 200, body: ok([]) });
    await openDetail(first.id);
    const fb = { "dien-thoai": "phone.png", laptop: "laptop.png", "may-tinh-bang": "tablet.png", "phu-kien": "banphim.png" }[firstSlug] || "phone.png";
    check("no image → category fallback", (await evaluate(`document.getElementById("mainImage").src`)).endsWith("/assets/images/" + fb));

    // 11. home featured card → detail
    await navigate(`${SITE}/index.html`);
    await waitFor(`document.querySelector("#featuredProductGrid .product-card h3 a")`);
    check("home featured card links to the detail page", (await evaluate(`document.querySelector("#featuredProductGrid .product-card h3 a").href`)) === `${SITE}/customer/product-detail.html?id=${first.id}`);

    // 12. mobile
    await send("Emulation.setDeviceMetricsOverride", { width: 390, height: 844, deviceScaleFactor: 2, mobile: true });
    await openDetail(first.id);
    const mob = await evaluate(`({ cols: getComputedStyle(document.querySelector("#detailContent .product-detail-layout")).gridTemplateColumns.split(" ").length,
        overflow: document.documentElement.scrollWidth - window.innerWidth, actions: getComputedStyle(document.querySelector(".detail-actions")).flexDirection })`);
    check("mobile: one column, stacked buttons, no horizontal scroll", mob.cols === 1 && mob.actions === "column" && mob.overflow <= 0, JSON.stringify(mob));
    await shot("detail-mobile.png");

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
