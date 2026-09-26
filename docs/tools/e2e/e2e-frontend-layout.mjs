// Headless Edge (CDP) E2E for the restructured frontend: role folders, header/footer partials, auth flow.
// Needs: backend on :8080 with CORS for http://127.0.0.1:5501, and static-server.mjs serving the REPO ROOT
// on :5501, so pages live under /frontend/ exactly like VS Code Live Server opened on the workspace.
import { spawn } from "node:child_process";
import { mkdtempSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";

const EDGE = "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe";
const SITE = "http://127.0.0.1:5501/frontend";
const PORT = 9335;
// Registers a NEW user on every run: pass a fresh name, e.g. E2E_USER=fe.e2e9 (usernames already used: see PENDING_WORK.md)
const NAME = process.env.E2E_USER || "fe.e2e7";
const USER = { fullname: "Nguyễn Thị Giao Diện", email: NAME.toUpperCase() + "@TechShopping.test", username: NAME.toUpperCase(),
    phone: "0901111333", password: "Matkhau@123" };
const PAGES = [
    ["index.html", "home"], ["customer/products.html", "products"], ["customer/cart.html", null],
    ["customer/recommendation.html", "recommendation"], ["customer/services.html", "services"],
    ["customer/contact.html", "contact"]
];

const sleep = ms => new Promise(r => setTimeout(r, ms));
const results = [];
function check(name, ok, detail = "") {
    results.push({ name, ok });
    console.log(`${ok ? "PASS" : "FAIL"}  ${name}${detail ? "  -> " + detail : ""}`);
}

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
ws.addEventListener("message", ({ data }) => {
    const msg = JSON.parse(data);
    if (msg.id && pending.has(msg.id)) { pending.get(msg.id)(msg); pending.delete(msg.id); return; }
    listeners.forEach(l => l(msg));
    if (msg.method === "Runtime.exceptionThrown") jsProblems.push("exception: " + msg.params.exceptionDetails.exception?.description);
    if (msg.method === "Runtime.consoleAPICalled" && ["error", "warning"].includes(msg.params.type)) {
        jsProblems.push(`console.${msg.params.type}: ` + msg.params.args.map(a => a.value ?? a.description).join(" "));
    }
    // static files of the site that fail to load (API 4xx are expected in some steps and ignored)
    if (msg.method === "Network.responseReceived") {
        const { url, status } = msg.params.response;
        if (status >= 400 && url.startsWith("http://127.0.0.1:5501/") && !url.endsWith("/favicon.ico")) missing.push(`${status} ${url}`);
    }
});
const send = (method, params = {}) => new Promise(resolve => {
    const id = ++nextId; pending.set(id, resolve); ws.send(JSON.stringify({ id, method, params }));
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
async function waitFor(expression, timeout = 8000) {
    const end = Date.now() + timeout;
    while (Date.now() < end) {
        try { const v = await evaluate(expression); if (v) return v; } catch { }
        await sleep(150);
    }
    return null;
}
const fill = values => evaluate(`Object.entries(${JSON.stringify(values)})
    .forEach(([id, v]) => { document.getElementById(id).value = v; })`);
const auth = () => evaluate(`JSON.parse(localStorage.getItem("lahy_auth"))`);
const headerReady = `document.querySelector("header.header") && document.querySelector("footer.footer")`;

await send("Page.enable");
await send("Runtime.enable");
await send("Network.enable");

try {
    // 1. every customer page gets the shared header/footer, the right active menu item and working paths
    for (const [page, active] of PAGES) {
        await navigate(`${SITE}/${page}`);
        const ok = await waitFor(headerReady);
        const info = await evaluate(`({
            placeholders: document.querySelectorAll("[data-include]").length,
            headerParent: document.querySelector("header.header")?.parentElement.tagName,
            sticky: getComputedStyle(document.querySelector("header.header")).position,
            active: [...document.querySelectorAll(".nav a.active")].map(a => a.dataset.page),
            cssLoaded: getComputedStyle(document.querySelector(".logo-main")).fontWeight,
            footerBottom: Boolean(document.querySelector(".footer-bottom")),
            loginHref: document.querySelector(".header .login-btn").href,
            cartHref: document.querySelector(".header .cart-btn").href,
            productsHref: document.querySelector('.nav a[data-page="products"]').href
        })`);
        const expectedActive = active ? [active] : [];
        check(`${page}: header + footer injected, no placeholder left, header sticky under <body>`,
            Boolean(ok) && info.placeholders === 0 && info.headerParent === "BODY" && info.sticky === "sticky",
            JSON.stringify({ p: info.placeholders, parent: info.headerParent, pos: info.sticky }));
        check(`${page}: active menu = ${JSON.stringify(expectedActive)}, full footer`,
            JSON.stringify(info.active) === JSON.stringify(expectedActive) && info.footerBottom, JSON.stringify(info.active));
        check(`${page}: header links resolve from the site root`,
            info.loginHref === `${SITE}/auth/login.html` && info.cartHref === `${SITE}/customer/cart.html`
            && info.productsHref === `${SITE}/customer/products.html`);
    }

    // 2. navigation through the shared header works
    await navigate(`${SITE}/index.html`);
    await waitFor(headerReady);
    await evaluate(`document.querySelector('.nav a[data-page="contact"]').click()`);
    await waitFor(`location.pathname.endsWith("/frontend/customer/contact.html") && document.readyState === "complete"`);
    check("nav click: index → customer/contact.html", (await evaluate(`location.pathname`)).endsWith("/frontend/customer/contact.html"));
    await evaluate(`document.querySelector(".header .logo").click()`);
    await waitFor(`location.pathname.endsWith("/frontend/index.html")`);
    check("logo click: back to index.html", (await evaluate(`location.pathname`)).endsWith("/frontend/index.html"));
    await navigate(`${SITE}/index.html`);
    await evaluate(`document.querySelector('a.category-card[href*="category=phone"]').click()`);
    await waitFor(`location.search === "?category=phone" && document.readyState === "complete"`);
    check("index category card → customer/products.html?category=phone, filter applied",
        (await evaluate(`location.pathname.endsWith("customer/products.html")`))
        && (await waitFor(`document.querySelector('.filter-btn[data-category="phone"]').classList.contains("active")`)) === true);

    // 3. auth pages in auth/ still work: register → redirect to root index with name in header
    await navigate(`${SITE}/auth/register.html`);
    check("auth/register.html: styles loaded", (await evaluate(`getComputedStyle(document.querySelector(".login-box")).backgroundColor`)) === "rgb(255, 255, 255)");
    await fill({ fullname: USER.fullname, email: USER.email, username: USER.username, phone: USER.phone,
        password: USER.password, confirmPassword: USER.password });
    await evaluate(`document.getElementById("registerForm").requestSubmit()`);
    await waitFor(`location.pathname.endsWith("/frontend/index.html") && document.querySelector(".header .logout-btn")`);
    check("register → /frontend/index.html (site root, not auth/index.html)",
        (await evaluate(`location.pathname`)) === "/frontend/index.html");
    check("header (from partial) shows the name + 'Đăng xuất'",
        (await evaluate(`document.querySelector(".header .login-btn.user-name")?.textContent`)) === USER.fullname);
    check("cart counter updated after layout load", (await evaluate(`document.querySelector(".cart-count").textContent.trim()`)) === "0");

    // 4. logout from a page in customer/
    await navigate(`${SITE}/customer/cart.html`);
    await waitFor(`document.querySelector(".header .logout-btn")`);
    await evaluate(`document.querySelector(".header .logout-btn").click()`);
    await waitFor(`document.readyState === "complete" && document.querySelector("header.header") && !document.querySelector(".header .logout-btn") && !localStorage.getItem("lahy_auth")`);
    check("logout on customer/cart.html: storage cleared, header back to 'Đăng nhập'",
        (await auth()) === null && (await evaluate(`document.querySelector(".header .login-btn").textContent.trim()`)) === "Đăng nhập");

    // 5. redirectToLogin + ?redirect with a folder path
    await navigate(`${SITE}/customer/recommendation.html?x=1`);
    await evaluate(`redirectToLogin()`);
    await waitFor(`location.pathname.endsWith("/auth/login.html")`);
    check("redirectToLogin: → auth/login.html?redirect=customer/recommendation.html?x=1",
        (await evaluate(`new URLSearchParams(location.search).get("redirect")`)) === "customer/recommendation.html?x=1");
    await fill({ identifier: NAME, password: USER.password });
    await evaluate(`document.getElementById("loginForm").requestSubmit()`);
    await waitFor(`location.pathname.endsWith("/frontend/customer/recommendation.html") && document.querySelector(".header .logout-btn")`);
    check("login returns to customer/recommendation.html?x=1 logged in",
        (await evaluate(`location.pathname + location.search`)) === "/frontend/customer/recommendation.html?x=1");

    // 6. open-redirect guard with folders
    await navigate(`${SITE}/index.html`);
    const guard = await evaluate(`(() => { const r = [];
        for (const q of ["https://evil.example", "//evil.example/x.html", "../../x.html", "a/b/c.html", "customer/cart.html"]) {
            history.replaceState(null, "", "index.html?redirect=" + encodeURIComponent(q)); r.push(getRedirectTarget("index.html")); }
        history.replaceState(null, "", "index.html"); return r; })()`);
    const home = `${SITE}/index.html`;
    check("redirect guard: external, parent and deep paths rejected; customer/cart.html allowed",
        JSON.stringify(guard) === JSON.stringify([home, home, home, home, `${SITE}/customer/cart.html`]), JSON.stringify(guard));

    // 7. already logged in → login page sends to root index
    await navigate(`${SITE}/auth/login.html`);
    await waitFor(`location.pathname.endsWith("/frontend/index.html")`);
    check("auth/login.html while logged in → /frontend/index.html", (await evaluate(`location.pathname`)) === "/frontend/index.html");
    await evaluate(`logout()`);

    check("no missing static files (css/js/images/partials) on any page", missing.length === 0, missing.join(" | "));
    check("no JS exceptions / console errors or warnings", jsProblems.length === 0, jsProblems.join(" | "));
} catch (error) {
    check("script error", false, error.message);
} finally {
    ws.close();
    edge.kill();
}
const failed = results.filter(r => !r.ok).length;
console.log(`\n${results.length - failed}/${results.length} checks passed`);
process.exit(failed ? 1 : 0);
