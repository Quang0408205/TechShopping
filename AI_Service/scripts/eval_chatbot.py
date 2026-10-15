"""
Đánh giá tự động Chatbot (báo cáo mục 3.7.3 b, 4.8.4) trên bộ câu hỏi evals/chatbot_cases.json.

Mỗi câu hỏi được gửi tới đúng luồng thật (Claude + RAG + Function Calling + API Backend) rồi chấm theo:
  1. Tool:        các tool bắt buộc có được gọi không (expect_tools)
  2. Nội dung:    có đủ cụm từ bắt buộc (must_include), không có cụm từ cấm (must_not_include),
                  có đúng giá hiện tại của sản phẩm lấy từ Backend (price_of)
  3. Thao tác:    có / không đề xuất thao tác đúng loại (expect_action, no_action)
  4. Giám khảo:   một lần gọi LLM riêng chấm theo tiêu chí (rubric) + kiểm tra không bịa thông tin ngoài kết quả
                  tool (bỏ qua bằng --no-judge). Mô hình nhỏ chạy trên máy chấm kém tin cậy hơn Claude.
Một câu ĐẠT khi qua mọi tiêu chí của nó.

Chạy (Backend + LLM đang chạy; với Ollama thì miễn phí, với Claude thì tốn phí, khoảng 30 câu × 1–4 lần gọi):
    docker compose exec ai-service python -m scripts.eval_chatbot
    docker compose exec ai-service python -m scripts.eval_chatbot --only acc-orders --only act-cancel
    docker compose exec ai-service python -m scripts.eval_chatbot --dry-run     # kiểm tra bộ câu hỏi, không gọi Claude
Kết quả: in bảng + ghi evals/results/<thời gian>.json và evals/results/latest.md (dán vào báo cáo).
Tài khoản dùng cho các câu login = true: EVAL_LOGIN / EVAL_PASSWORD (mặc định tài khoản mẫu hoangnamnguyen).
"""

import argparse
import json
import os
import re
import time
from collections import defaultdict
from datetime import datetime
from pathlib import Path

import httpx

from app import config
from app.chatbot import agent

ROOT = Path(__file__).resolve().parent.parent
CASES_FILE = ROOT / "evals" / "chatbot_cases.json"
RESULTS_DIR = ROOT / "evals" / "results"

KNOWN_TOOLS = {t["name"] for t in agent.TOOLS}

# Giá $/1 triệu token (input, output) để ước tính chi phí; cache đọc tính 10%, cache ghi 125% giá input
PRICES = {"claude-opus-5-5": (4.0, 20.0), "claude-sonnet-5-5": (2.0, 10.0), "claude-haiku-5-5": (0.10, 0.50),
          "claude-opus-5": (5.0, 25.0), "claude-fable-5-1": (10.0, 50.0)}

JUDGE_PROMPT = """Bạn là giám khảo chấm câu trả lời của trợ lý mua sắm AI của cửa hàng POY.
Chấm ĐẠT chỉ khi câu trả lời thoả MỌI điều sau:
1. Thoả tiêu chí riêng của câu hỏi (nếu có).
2. Mọi giá, thông số, chính sách, trạng thái đơn hàng nêu trong câu trả lời đều có trong KẾT QUẢ TOOL bên dưới
   (không bịa). Câu chào hỏi, mời đăng nhập, hỏi lại khách thì không cần nguồn.
3. Trả lời bằng tiếng Việt, đúng trọng tâm câu hỏi.
Trả về JSON {"pass": true|false, "reason": "<một câu giải thích bằng tiếng Việt>"}."""

JUDGE_SCHEMA = {
    "type": "object",
    "properties": {"pass": {"type": "boolean"}, "reason": {"type": "string"}},
    "required": ["pass", "reason"],
    "additionalProperties": False,
}


# ---------------------------------------------------------------- chuẩn bị

def load_cases(only: list[str] | None) -> list[dict]:
    cases = json.loads(CASES_FILE.read_text(encoding="utf-8"))["cases"]
    ids = [c["id"] for c in cases]
    if len(ids) != len(set(ids)):
        raise SystemExit("Duplicate case ids in chatbot_cases.json")
    for c in cases:
        names = {n for t in c.get("expect_tools", []) for n in (t if isinstance(t, list) else [t])}
        unknown = names - KNOWN_TOOLS
        if unknown:
            raise SystemExit(f"{c['id']}: unknown tools {unknown}")
    if only:
        cases = [c for c in cases if c["id"] in only]
    return cases


def login() -> str:
    body = {"identifier": os.getenv("EVAL_LOGIN", "hoangnamnguyen@example.test"),
            "password": os.getenv("EVAL_PASSWORD", "SampleOnly123!")}
    response = httpx.post(config.BACKEND_URL + "/auth/login", json=body, timeout=10)
    response.raise_for_status()
    return response.json()["data"]["accessToken"]


def current_price(product_id: int) -> int:
    data = httpx.get(f"{config.BACKEND_URL}/products/{product_id}", timeout=10).json()["data"]
    return int(float(data["effectivePrice"]))


# ---------------------------------------------------------------- chấm điểm

def _has(text: str, phrase: str) -> bool:
    return phrase.lower() in text.lower()


def check(case: dict, answer: dict, prices: dict[int, int]) -> dict[str, tuple[bool, str]]:
    """Các tiêu chí tự động: tên tiêu chí → (đạt?, ghi chú)."""
    reply, used, actions = answer["reply"], answer["toolsUsed"], answer["actions"]
    checks = {}
    if case.get("expect_tools"):
        missing = [t for t in case["expect_tools"]
                   if not (any(n in used for n in t) if isinstance(t, list) else t in used)]
        checks["tools"] = (not missing, f"thiếu {missing}" if missing else "")
    if case.get("must_include"):
        missing = [group for group in case["must_include"] if not any(_has(reply, p) for p in group)]
        checks["include"] = (not missing, f"thiếu {missing}" if missing else "")
    if case.get("must_not_include"):
        found = [p for p in case["must_not_include"] if _has(reply, p)]
        checks["exclude"] = (not found, f"có {found}" if found else "")
    if "price_of" in case:
        price = prices[case["price_of"]]
        ok = str(price) in re.sub(r"\D", "", reply)
        checks["price"] = (ok, "" if ok else f"không có giá {price:,}đ".replace(",", "."))
    if case.get("expect_action"):
        ok = any(a["type"] == case["expect_action"] for a in actions)
        checks["action"] = (ok, "" if ok else f"không có thao tác {case['expect_action']}")
    if case.get("no_action"):
        checks["no_action"] = (not actions, f"đề xuất thừa {[a['type'] for a in actions]}" if actions else "")
    return checks


def judge(case: dict, answer: dict, trace: list[dict]) -> tuple[bool, str, dict]:
    tool_results = "\n".join(f"- {t['tool']}({json.dumps(t['input'], ensure_ascii=False)}) → {t['output'][:3000]}"
                             for t in trace) or "(không gọi tool)"
    history = "\n".join(f"{h['role']}: {h['content']}" for h in case.get("history", []))
    content = (f"LỊCH SỬ HỘI THOẠI:\n{history or '(không có)'}\n\nCÂU HỎI CỦA KHÁCH:\n{case['question']}\n\n"
               f"KHÁCH ĐÃ ĐĂNG NHẬP: {'có' if case.get('login') else 'không'}\n\n"
               f"KẾT QUẢ TOOL:\n{tool_results}\n\nCÂU TRẢ LỜI CỦA TRỢ LÝ:\n{answer['reply']}\n\n"
               f"TIÊU CHÍ RIÊNG: {case.get('rubric') or '(không có)'}")
    verdict = agent.provider().complete_json(JUDGE_PROMPT, content, JUDGE_SCHEMA)
    if not verdict or "pass" not in verdict:
        return False, "giám khảo không trả kết quả hợp lệ", {}
    return bool(verdict["pass"]), str(verdict.get("reason", "")), {}


def cost(usage: dict, model: str) -> float:
    if model not in PRICES:
        return 0.0                  # LLM chạy trên máy (Ollama) hoặc không có bảng giá
    price_in, price_out = PRICES[model]
    return (usage.get("input_tokens", 0) * price_in + usage.get("output_tokens", 0) * price_out
            + usage.get("cache_read_input_tokens", 0) * price_in * 0.1
            + usage.get("cache_creation_input_tokens", 0) * price_in * 1.25) / 1_000_000


# ---------------------------------------------------------------- chạy

def run(cases: list[dict], use_judge: bool) -> list[dict]:
    token = login() if any(c.get("login") for c in cases) else None
    prices = {c["price_of"]: current_price(c["price_of"]) for c in cases if "price_of" in c}
    results = []
    for n, case in enumerate(cases, 1):
        print(f"[{n}/{len(cases)}] {case['id']} ...", end=" ", flush=True)
        trace: list[dict] = []
        started = time.monotonic()
        try:
            answer = agent.chat(case["question"], case.get("history", []), token if case.get("login") else None, trace)
        except Exception as error:  # noqa: BLE001 - một câu lỗi không dừng cả bộ đánh giá
            print("LỖI", error)
            results.append({"id": case["id"], "group": case["group"], "passed": False, "error": str(error)})
            continue
        seconds = round(time.monotonic() - started, 1)
        checks = check(case, answer, prices)
        judge_usage = {}
        if use_judge and (case.get("rubric") or trace):
            ok, reason, judge_usage = judge(case, answer, trace)
            checks["judge"] = (ok, reason)
        passed = all(ok for ok, _ in checks.values())
        print("ĐẠT" if passed else "TRƯỢT", f"({seconds}s)")
        results.append({
            "id": case["id"], "group": case["group"], "question": case["question"], "passed": passed,
            "checks": {k: {"pass": ok, "note": note} for k, (ok, note) in checks.items()},
            "reply": answer["reply"], "tools": answer["toolsUsed"], "actions": answer["actions"],
            "seconds": seconds, "usage": answer.get("usage", {}), "judge_usage": judge_usage,
        })
    return results


def report(results: list[dict], use_judge: bool) -> str:
    by_group = defaultdict(lambda: [0, 0])
    for r in results:
        by_group[r["group"]][0] += r["passed"]
        by_group[r["group"]][1] += 1
    total_pass = sum(r["passed"] for r in results)
    answered = [r for r in results if "seconds" in r]
    chat_cost = sum(cost(r["usage"], agent.model_name()) for r in answered)
    avg_seconds = sum(r["seconds"] for r in answered) / len(answered) if answered else 0
    avg_requests = sum(r["usage"].get("requests", 0) for r in answered) / len(answered) if answered else 0

    lines = [f"# Kết quả đánh giá Chatbot ({datetime.now():%d/%m/%Y %H:%M})", "",
             f"Mô hình: `{agent.model_name()}` ({config.LLM_PROVIDER}); giám khảo: "
             f"{'`' + agent.model_name() + '`' if use_judge else 'không dùng'}.", "",
             "| Nhóm | Đạt / Tổng | Tỉ lệ |", "|---|---|---|"]
    for group, (ok, n) in by_group.items():
        lines.append(f"| {group} | {ok}/{n} | {ok / n:.0%} |")
    lines += [f"| **Tổng** | **{total_pass}/{len(results)}** | **{total_pass / len(results):.0%}** |", "",
              f"- Thời gian trả lời trung bình: {avg_seconds:.1f} giây; trung bình {avg_requests:.1f} lần gọi LLM / câu.",
              (f"- Chi phí ước tính: chatbot ${chat_cost:.3f} (≈ ${chat_cost / max(len(answered), 1):.4f} / câu)."
               if agent.model_name() in PRICES else "- Chi phí: 0 (LLM chạy trên máy)."), "",
              "## Các câu trượt", ""]
    failed = [r for r in results if not r["passed"]]
    for r in failed:
        notes = "; ".join(f"{k}: {v['note']}" for k, v in r.get("checks", {}).items() if not v["pass"])
        lines.append(f"- **{r['id']}** ({r['group']}): {notes or r.get('error', '')}")
    if not failed:
        lines.append("- (không có)")
    return "\n".join(lines) + "\n"


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--only", action="append", help="chỉ chạy câu có id này (lặp lại được)")
    parser.add_argument("--no-judge", action="store_true", help="không dùng giám khảo LLM")
    parser.add_argument("--dry-run", action="store_true", help="kiểm tra bộ câu hỏi + đăng nhập + giá, không gọi Claude")
    args = parser.parse_args()

    cases = load_cases(args.only)
    if args.dry_run:
        login()
        prices = {c["price_of"]: current_price(c["price_of"]) for c in cases if "price_of" in c}
        print(f"{len(cases)} câu hợp lệ; đăng nhập OK; giá hiện tại: {prices}")
        return
    if agent.not_ready_reason():
        raise SystemExit(agent.not_ready_reason())

    results = run(cases, use_judge=not args.no_judge)
    summary = report(results, use_judge=not args.no_judge)
    RESULTS_DIR.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now().strftime("%Y%m%d-%H%M")
    (RESULTS_DIR / f"{stamp}.json").write_text(json.dumps(results, ensure_ascii=False, indent=2), encoding="utf-8")
    (RESULTS_DIR / "latest.md").write_text(summary, encoding="utf-8")
    print("\n" + summary)
    print(f"Chi tiết: evals/results/{stamp}.json")


if __name__ == "__main__":
    main()
