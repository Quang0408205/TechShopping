"""
Đánh giá offline các mô hình khuyến nghị (báo cáo mục 3.7 / 4.8.3), tinh chỉnh tham số và chọn α cho Hybrid.
Cách chia dữ liệu (huấn luyện / validation / kiểm thử), các bước tinh chỉnh, công thức chỉ số và bootstrap:
app/recommender/evaluation.py. Chạy mất vài phút (18 bộ tham số ALS + 11 giá trị α + 3 mức giảm trọng số).

    python -m scripts.evaluate --n 10

Kết quả in ra màn hình và ghi vào evals/results/: recommender_latest.md (bảng dán vào báo cáo),
recommender_latest.csv (mở bằng Excel), recommender_latest.json (trang admin) và recommender_alpha.svg (biểu đồ);
lần chạy cũng được thêm vào lịch sử trong database (recommendation_evaluations).
"""

import argparse

from app import config
from app.recommender.evaluation import evaluate, markdown, save, save_history


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--n", type=int, default=10)
    parser.add_argument("--train-ratio", type=float, default=0.7)
    parser.add_argument("--validation-ratio", type=float, default=0.1)
    parser.add_argument("--bootstrap", type=int, default=1000, help="số lần lấy mẫu lại cho khoảng tin cậy")
    parser.add_argument("--no-save", action="store_true", help="chỉ in kết quả, không ghi file / database")
    args = parser.parse_args()

    result = evaluate(args.n, args.train_ratio, args.validation_ratio, args.bootstrap)
    print(markdown(result))
    print(f"({result['seconds']} giây)")
    if not args.no_save:
        save(result)
        save_history(result)
        print(f"Saved to {config.EVAL_RESULTS_DIR}/recommender_latest.(md|csv|json) + recommender_alpha.svg "
              "and to recommendation_evaluations")


if __name__ == "__main__":
    main()
