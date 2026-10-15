"""
Đánh giá phần TÌM KIẾM của RAG (báo cáo mục 3.7.3 b, 4.8.4): so sánh 3 cách tìm (vector, keyword, hybrid) và
chọn số chunk K đưa cho LLM, trên bộ câu hỏi có đáp án evals/retrieval_cases.json.

Với mỗi câu hỏi, lấy danh sách Top-8 rồi tính, với K = 1, 3, 5, 8:
    Hit@K     = 1 nếu có ít nhất một chunk đúng trong K kết quả đầu
    Recall@K  = số chunk đúng trong K kết quả đầu / tổng số chunk đúng
    MRR       = 1 / vị trí của chunk đúng đầu tiên (0 nếu không có trong Top-8)
rồi lấy trung bình. Ngưỡng độ tương đồng giống lúc chạy thật (chính sách 0,8; sản phẩm 0,78).
Không gọi LLM, không tốn phí, chạy vài giây.

    docker compose exec ai-service python -m scripts.eval_retrieval
"""

import json
import time
from collections import defaultdict
from datetime import datetime
from pathlib import Path

from app.chatbot import rag

ROOT = Path(__file__).resolve().parent.parent
CASES_FILE = ROOT / "evals" / "retrieval_cases.json"
RESULTS_FILE = ROOT / "evals" / "results" / "retrieval_latest.md"

KS = (1, 3, 5, 8)
THRESHOLD = {"POLICY": 0.8, "PRODUCT": 0.78}     # giống tools.py


def relevant_keys(case: dict) -> set:
    return set(case.get("relevant_titles") or case.get("relevant_products") or [])


def key_of(case: dict, row: dict):
    return row["title"] if case.get("relevant_titles") else row["product_id"]


# (tên hiển thị, cách tìm, trọng số từ khoá trong RRF)
CONFIGS = [("vector", "vector", None), ("keyword", "keyword", None), ("hybrid w=1", "hybrid", 1.0),
           ("hybrid w=0.5", "hybrid", 0.5), ("hybrid w=0.25", "hybrid", 0.25)]


def evaluate(cases: list[dict], mode: str, weight: float | None) -> tuple[list[dict], float]:
    rows, started = [], time.monotonic()
    for case in cases:
        hits = rag.search(case["query"], k=max(KS), doc_type=case["doc_type"],
                          min_similarity=THRESHOLD[case["doc_type"]], mode=mode, keyword_weight=weight)
        ranked = [key_of(case, h) for h in hits]
        relevant = relevant_keys(case)
        first = next((i for i, key in enumerate(ranked, 1) if key in relevant), None)
        row = {"id": case["id"], "group": f"{case['doc_type']}/{case['kind']}", "mrr": 1 / first if first else 0.0,
               "top1": ranked[0] if ranked else None}
        for k in KS:
            found = len(relevant & set(ranked[:k]))
            row[f"hit@{k}"] = float(found > 0)
            row[f"recall@{k}"] = found / len(relevant)
        rows.append(row)
    return rows, (time.monotonic() - started) / len(cases) * 1000


def mean(rows: list[dict], key: str) -> float:
    return sum(r[key] for r in rows) / len(rows) if rows else 0.0


def main():
    cases = json.loads(CASES_FILE.read_text(encoding="utf-8"))["cases"]
    rag.search("khởi động mô hình embedding", k=1)          # nạp mô hình trước để đo thời gian công bằng
    results = {name: evaluate(cases, mode, weight) for name, mode, weight in CONFIGS}

    groups = defaultdict(int)
    for c in cases:
        groups[f"{c['doc_type']}/{c['kind']}"] += 1
    lines = [f"# Đánh giá tìm kiếm RAG ({datetime.now():%d/%m/%Y %H:%M})", "",
             f"{len(cases)} câu hỏi có đáp án: " + ", ".join(f"{g}: {n}" for g, n in sorted(groups.items())) + ".",
             "", "## Toàn bộ", "",
              "| Cách tìm | " + " | ".join(f"Hit@{k}" for k in KS) + " | Recall@3 | MRR | ms / câu |",
              "|---|" + "---|" * (len(KS) + 3)]
    for mode, (rows, ms) in results.items():
        lines.append(f"| {mode} | " + " | ".join(f"{mean(rows, f'hit@{k}'):.3f}" for k in KS)
                     + f" | {mean(rows, 'recall@3'):.3f} | {mean(rows, 'mrr'):.3f} | {ms:.0f} |")

    lines += ["", "## Theo nhóm câu hỏi (Hit@1 / Hit@3 / MRR)", "",
              "| Nhóm | " + " | ".join(results) + " |", "|---|" + "---|" * len(results)]
    for group in sorted(groups):
        cells = []
        for name in results:
            rows = [r for r in results[name][0] if r["group"] == group]
            cells.append(f"{mean(rows, 'hit@1'):.2f} / {mean(rows, 'hit@3'):.2f} / {mean(rows, 'mrr'):.2f}")
        lines.append(f"| {group} | " + " | ".join(cells) + " |")

    lines += ["", "## Câu chưa đúng ở vị trí 1", ""]
    for mode, (rows, _) in results.items():
        missed = [r for r in rows if r["hit@1"] == 0]
        lines.append(f"- **{mode}** ({len(missed)}): " + (", ".join(
            f"{r['id']} (top1: {str(r['top1'])[:40]})" for r in missed) or "không có"))

    report = "\n".join(lines) + "\n"
    RESULTS_FILE.parent.mkdir(parents=True, exist_ok=True)
    RESULTS_FILE.write_text(report, encoding="utf-8")
    print(report)


if __name__ == "__main__":
    main()
