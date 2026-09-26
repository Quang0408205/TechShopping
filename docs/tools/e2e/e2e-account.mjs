// Headless Edge (CDP) E2E for Checkpoint 2.7: customer/account.html + shared CSS moves.
// Needs: backend (test profile, CORS for http://127.0.0.1:5501) and static-server.mjs serving the REPO ROOT on :5501.
import { spawn } from "node:child_process";
import { mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";

const EDGE = "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe";
const ORIGIN = "http://127.0.0.1:5501";
const SITE = `${ORIGIN}/frontend`;
const SHOTS = process.argv[2] || ".";
const PORT = 9336;
// Registers a NEW user on every run: pass a fresh name, e.g. E2E_USER=fe.e2e10 (usernames already used: see PENDING_WORK.md)
const NAME = process.env.E2E_USER || "fe.e2e8";
const USER = { fullname: "Nguyễn Văn Tài Khoản", email: NAME + "@techshopping.test", username: NAME,
    phone: "0901111444", password: "Matkhau@123" };
const NEW_PASSWORD = "MatkhauMoi@456";

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
let mock = null; // { path, method, status, body } fulfilled once through the Fetch domain

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
    if (msg.method === "Network.responseReceived") {
        const { url, status } = msg.params.response;
        if (status >= 400 && url.startsWith(ORIGIN + "/") && !url.endsWith("/favicon.ico")) missing.push(`${status} ${url}`);
    }
    if (msg.method === "Fetch.requestPaused") {
        const { requestId, request } = msg.params;
        if (mock && request.method === mock.method && new URL(request.url).pathname === mock.path) {
            const m = mock;
            mock = null;
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
const text = selector => evaluate(`document.querySelector(${JSON.stringify(selector)})?.textContent.trim()`);
const visible = selector => evaluate(`(() => { const e = document.querySelector(${JSON.stringify(selector)}); return Boolean(e && !e.hidden); })()`);
const auth = () => evaluate(`JSON.parse(localStorage.getItem("lahy_auth"))`);
const accountLoaded = `!document.getElementById("accountContent").hidden && document.getElementById("email").value`;
async function shot(name) {
    const { result } = await send("Page.captureScreenshot", { format: "png", captureBeyondViewport: true });
    writeFileSync(join(SHOTS, name), Buffer.from(result.data, "base64"));
}
function apiError(status, code, message) {
    return { success: false, timestamp: "2026-09-25T00:00:00", data: null, error: { code, message, details: null } };
}

await send("Page.enable");
await send("Runtime.enable");
await send("Network.enable");
await send("Fetch.enable", { patterns: [{ urlPattern: "http://localhost:8080/api/v1/users/me*", requestStage: "Request" }] });
await send("Emulation.setDeviceMetricsOverride", { width: 1280, height: 900, deviceScaleFactor: 1, mobile: false });

try {
    // 0. shared CSS moves: page-hero now styled on cart, login error box still styled
    await navigate(`${SITE}/customer/cart.html`);
    const hero = await evaluate(`(() => { const s = getComputedStyle(document.querySelector(".page-hero"));
        return s.paddingTop + " " + s.backgroundColor; })()`);
    check("page-hero styled on customer/cart.html (moved to style.css)", hero === "120px rgb(243, 243, 243)", hero);
    await navigate(`${SITE}/customer/products.html`);
    check("page-hero unchanged on products.html",
        (await evaluate(`getComputedStyle(document.querySelector(".page-hero")).paddingTop`)) === "120px");

    // 1. logged out → account page redirects to login with ?redirect, and back after login
    await navigate(`${SITE}/index.html`);
    const registered = await evaluate(`apiRequest("/auth/register", { method: "POST", body: ${JSON.stringify({
        fullname: USER.fullname, email: USER.email, username: USER.username, phone: USER.phone, password: USER.password })} })
        .then(a => a.user.username, e => "ERR " + e.code)`);
    check("setup: E2E user registered through the API", registered === USER.username, registered);
    await navigate(`${SITE}/customer/account.html`);
    await waitFor(`location.pathname.endsWith("/auth/login.html")`);
    check("logged out: account.html → auth/login.html?redirect=customer/account.html",
        (await evaluate(`new URLSearchParams(location.search).get("redirect")`)) === "customer/account.html");
    await fill({ identifier: USER.username, password: USER.password });
    await evaluate(`document.getElementById("loginForm").requestSubmit()`);
    await waitFor(`location.pathname.endsWith("/customer/account.html") && ${accountLoaded}`);
    check("after login: back on customer/account.html with data loaded",
        (await evaluate(`location.pathname`)) === "/frontend/customer/account.html" && Boolean(await evaluate(accountLoaded)));

    // 2. loaded values
    check("account: email/username read-only and filled",
        (await evaluate(`[document.getElementById("email"), document.getElementById("username")].every(e => e.readOnly)`))
        && (await evaluate(`document.getElementById("email").value`)) === USER.email
        && (await evaluate(`document.getElementById("username").value`)) === USER.username);
    check("account: fullname/phone filled", (await evaluate(`document.getElementById("fullname").value`)) === USER.fullname
        && (await evaluate(`document.getElementById("phone").value`)) === USER.phone);
    check("summary: username, 0 points, 0đ, join date",
        (await text("#summaryUsername")) === USER.username && (await text("#summaryPoints")) === "0"
        && (await text("#summarySpent")) === "0đ" && /^\d{1,2}\/\d{1,2}\/\d{4}$/.test(await text("#summaryJoined")),
        `${await text("#summaryPoints")} | ${await text("#summarySpent")} | ${await text("#summaryJoined")}`);
    check("profile created at registration: form empty, 'no profile' hint hidden",
        (await evaluate(`document.getElementById("city").value`)) === "" && !(await visible("#profileEmptyHint")));
    await waitFor(`document.querySelector(".header .login-btn.user-name")`);
    check("header name links to customer/account.html",
        (await evaluate(`document.querySelector(".header .login-btn.user-name").href`)) === `${SITE}/customer/account.html`);
    await shot("account-loaded.png");

    // 3. account form: client validation, then save + header update
    await fill({ fullname: "" });
    await evaluate(`document.getElementById("accountForm").requestSubmit()`);
    check("account: empty fullname → field error, general error",
        Boolean(await text('#accountForm .field-error[data-field="fullname"]')) && (await visible('#accountForm [data-role="error"]')));
    await fill({ fullname: "Trần Thị Hồ Sơ", phone: "0987654321", avatarUrl: "https://example.com/a.png" });
    await evaluate(`document.getElementById("accountForm").requestSubmit()`);
    await waitFor(`!document.querySelector('#accountForm [data-role="success"]').hidden`);
    check("account: saved, success message", (await text('#accountForm [data-role="success"]')) === "Đã lưu thông tin tài khoản.");
    check("account: header name updated immediately", (await text(".header .login-btn.user-name")) === "Trần Thị Hồ Sơ");
    check("account: stored session user updated", (await auth())?.user?.fullname === "Trần Thị Hồ Sơ");

    // 4. profile form: future date rejected, then Vietnamese profile saved and persisted
    await fill({ dateOfBirth: "2999-01-01" });
    await evaluate(`document.getElementById("profileForm").requestSubmit()`);
    check("profile: future date of birth → field error",
        (await text('#profileForm .field-error[data-field="dateOfBirth"]')) === "Ngày sinh phải là một ngày trong quá khứ.");
    await fill({ dateOfBirth: "1998-03-15", gender: "FEMALE", address: "45 Lê Lợi", ward: "Phường Bến Thành",
        district: "Quận 1", city: "TP. Hồ Chí Minh", postalCode: "700000", defaultShippingAddress: "45 Lê Lợi, Quận 1" });
    await evaluate(`document.getElementById("profileForm").requestSubmit()`);
    await waitFor(`!document.querySelector('#profileForm [data-role="success"]').hidden`);
    check("profile: saved, success message", (await text('#profileForm [data-role="success"]')) === "Đã lưu hồ sơ khách hàng.");
    await navigate(`${SITE}/customer/account.html`);
    await waitFor(accountLoaded);
    const persisted = await evaluate(`["fullname","phone","avatarUrl","dateOfBirth","gender","ward","city","defaultShippingAddress"]
        .map(id => document.getElementById(id).value)`);
    check("reload: account + profile values persisted", JSON.stringify(persisted) === JSON.stringify(["Trần Thị Hồ Sơ",
        "0987654321", "https://example.com/a.png", "1998-03-15", "FEMALE", "Phường Bến Thành", "TP. Hồ Chí Minh",
        "45 Lê Lợi, Quận 1"]), JSON.stringify(persisted));
    // clearing optional fields sends null (gender "" would be rejected by the backend pattern otherwise)
    await fill({ gender: "", dateOfBirth: "" });
    await evaluate(`document.getElementById("profileForm").requestSubmit()`);
    await waitFor(`!document.querySelector('#profileForm [data-role="success"]').hidden`);
    check("profile: clearing gender and date of birth is accepted",
        (await visible('#profileForm [data-role="success"]')) && (await evaluate(`document.getElementById("gender").value`)) === "");

    // 5. account without a profile (mocked 404) → empty form + hint
    mock = { path: "/api/v1/users/me/profile", method: "GET", status: 404,
        body: apiError(404, "CUSTOMER_PROFILE_NOT_FOUND", "Customer profile not found") };
    await navigate(`${SITE}/customer/account.html`);
    await waitFor(accountLoaded);
    check("no profile (404): page still loads, hint shown, profile form empty",
        (await visible("#profileEmptyHint")) && (await evaluate(`document.getElementById("city").value`)) === "");

    // 6. password: wrong current → field error; same as current → client error; success → back to login
    await fill({ currentPassword: "sai-mat-khau", newPassword: NEW_PASSWORD, confirmNewPassword: NEW_PASSWORD });
    await evaluate(`document.getElementById("passwordForm").requestSubmit()`);
    await waitFor(`document.querySelector('#passwordForm .field-error[data-field="currentPassword"]').textContent`);
    check("password: wrong current → 'Mật khẩu hiện tại không đúng.' on the field",
        (await text('#passwordForm .field-error[data-field="currentPassword"]')) === "Mật khẩu hiện tại không đúng.");
    await fill({ currentPassword: USER.password, newPassword: USER.password, confirmNewPassword: USER.password });
    await evaluate(`document.getElementById("passwordForm").requestSubmit()`);
    check("password: new = current → client field error",
        Boolean(await text('#passwordForm .field-error[data-field="newPassword"]')));
    await fill({ currentPassword: USER.password, newPassword: NEW_PASSWORD, confirmNewPassword: NEW_PASSWORD });
    await evaluate(`document.getElementById("passwordForm").requestSubmit()`);
    await waitFor(`!document.querySelector('#passwordForm [data-role="success"]').hidden`);
    check("password: success message", (await text('#passwordForm [data-role="success"]')) === "Đổi mật khẩu thành công. Vui lòng đăng nhập lại.");
    await waitFor(`location.pathname.endsWith("/auth/login.html") && document.readyState === "complete" && typeof apiRequest === "function"`, 5000);
    check("password: session cleared and redirected to auth/login.html",
        (await evaluate(`location.pathname`)) === "/frontend/auth/login.html" && (await auth()) === null);
    const oldLogin = await evaluate(`apiRequest("/auth/login", { method: "POST", body: { identifier: "${USER.username}",
        password: "${USER.password}" } }).then(() => "OK", e => e.code)`);
    check("password: old password rejected", oldLogin === "INVALID_CREDENTIALS", oldLogin);
    await fill({ identifier: USER.username, password: NEW_PASSWORD });
    await evaluate(`document.getElementById("loginForm").requestSubmit()`);
    await waitFor(`location.pathname.endsWith("/frontend/index.html") && localStorage.getItem("lahy_auth")`);
    check("password: new password works", Boolean(await auth()));

    // 7. deactivated account (mocked 403) → message, content hidden, session cleared
    mock = { path: "/api/v1/users/me", method: "GET", status: 403,
        body: apiError(403, "ACCOUNT_DISABLED", "Account is disabled") };
    await navigate(`${SITE}/customer/account.html`);
    await waitFor(`!document.getElementById("accountStatus").hidden`);
    check("ACCOUNT_DISABLED: message shown, content hidden, session cleared",
        (await text("#accountStatus")) === "Tài khoản đã bị khóa. Vui lòng liên hệ hỗ trợ."
        && !(await visible("#accountContent")) && (await auth()) === null);

    // 8. expired session (bad access + bad refresh) → back to login
    await navigate(`${SITE}/index.html`);
    await evaluate(`localStorage.setItem("lahy_auth", JSON.stringify({ accessToken: "bad.access", refreshToken: "bad-refresh",
        user: { username: "x", fullname: "X" } }))`);
    await navigate(`${SITE}/customer/account.html`);
    await waitFor(`location.pathname.endsWith("/auth/login.html")`);
    check("expired session: refresh fails → auth/login.html?redirect=customer/account.html",
        (await evaluate(`location.pathname + location.search`)) === "/frontend/auth/login.html?redirect=customer%2Faccount.html");
    check("expired session: stored session cleared", (await auth()) === null);

    // 9. screenshots of login/register error styling after moving .form-error to style.css
    await fill({ identifier: USER.username, password: "sai" });
    await evaluate(`document.getElementById("loginForm").requestSubmit()`);
    await waitFor(`!document.getElementById("loginError").hidden`);
    check("login error box still styled after CSS move",
        (await evaluate(`getComputedStyle(document.getElementById("loginError")).borderLeftColor`)) === "rgb(192, 57, 43)");
    await shot("login-error-after-move.png");

    // clean up: log the E2E user out on the server
    await navigate(`${SITE}/index.html`);
    await evaluate(`apiRequest("/auth/login", { method: "POST", body: { identifier: "${USER.username}", password: "${NEW_PASSWORD}" } })
        .then(saveAuth).then(() => logout())`);

    check("no missing static files", missing.length === 0, missing.join(" | "));
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
