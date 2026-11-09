"""
Đánh giá offline trên dữ liệu công khai: Amazon Reviews 2023, nhóm Cell Phones & Accessories (McAuley Lab).

Chạy ĐÚNG quy trình của app/recommender/evaluation.py (chia 70/10/20 theo thời gian, chọn tham số trên validation,
NDCG@10 + bootstrap) nhưng thay nguồn dữ liệu bằng hành vi thật của khách Amazon. Không ghi gì vào database.

Dữ liệu (không đưa vào repo):
    curl -LO https://huggingface.co/datasets/McAuley-Lab/Amazon-Reviews-2023/resolve/main/benchmark/5core/rating_only/Cell_Phones_and_Accessories.csv
    python scripts/eval_amazon.py sample Cell_Phones_and_Accessories.csv amazon/        # → amazon/sample.csv, asins.txt
    curl -L https://huggingface.co/datasets/McAuley-Lab/Amazon-Reviews-2023/resolve/main/raw/meta_categories/meta_Cell_Phones_and_Accessories.jsonl \
        | python scripts/eval_amazon.py filter-meta amazon/asins.txt amazon/meta.jsonl  # đọc luồng 4 GB, giữ ~5.000 dòng
    python scripts/eval_amazon.py run amazon/                                            # → evals/results/amazon_cell_*

Cách lấy mẫu (giữ quy mô gần với hệ thống: 4.302 sản phẩm):
    1. 5.000 sản phẩm có nhiều đánh giá nhất trong tập 5-core của nhóm hàng;
    2. lọc lặp 5-core (mỗi người dùng và mỗi sản phẩm ≥ 5 lượt);
    3. chọn ngẫu nhiên 20.000 người dùng (seed 42), giữ mọi lượt của họ.
Ánh xạ sang dữ liệu của hệ thống:
    - mỗi đánh giá = một lượt PURCHASE (Amazon chỉ có đánh giá sau khi mua, không có xem / thêm giỏ);
    - đơn hàng (cho mua kèm) = các sản phẩm một người dùng đánh giá trong cùng một ngày;
    - danh mục = cấp 3 của cây danh mục Amazon (thiếu thì cấp 2); thương hiệu = Brand (hoặc store), 100 hãng nhiều sản phẩm nhất,
      còn lại gộp "Khác"; giá thiếu = trung vị giá của danh mục; RAM, bộ nhớ, màn hình lấy từ details.
"""

import json
import sys
from pathlib import Path

import numpy as np
import pandas as pd

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

TOP_ITEMS, CORE, USERS, SEED, TOP_BRANDS = 5000, 5, 20000, 42, 100


def k_core(frame: pd.DataFrame, k: int = CORE) -> pd.DataFrame:
    while True:
        items = frame["parent_asin"].value_counts()
        frame = frame[frame["parent_asin"].isin(items[items >= k].index)]
        users = frame["user_id"].value_counts()
        kept = frame[frame["user_id"].isin(users[users >= k].index)]
        if len(kept) == len(frame):
            return kept
        frame = kept


def sample(ratings_csv: str, out_dir: str) -> None:
    out = Path(out_dir)
    out.mkdir(parents=True, exist_ok=True)
    ratings = pd.read_csv(ratings_csv)
    top = ratings["parent_asin"].value_counts().index[:TOP_ITEMS]
    frame = k_core(ratings[ratings["parent_asin"].isin(top)])
    users = np.random.default_rng(SEED).choice(np.sort(frame["user_id"].unique()), USERS, replace=False)
    frame = frame[frame["user_id"].isin(users)]
    frame.to_csv(out / "sample.csv", index=False)
    (out / "asins.txt").write_text("\n".join(sorted(frame["parent_asin"].unique())) + "\n")
    print(f"{len(frame)} lượt, {frame['user_id'].nunique()} người dùng, {frame['parent_asin'].nunique()} sản phẩm")


def filter_meta(asins_file: str, out_file: str) -> None:
    keep = set(Path(asins_file).read_text().split())
    fields = ("parent_asin", "title", "main_category", "categories", "price", "store", "details", "rating_number")
    kept = 0
    with open(out_file, "w", encoding="utf-8") as out:
        for line in sys.stdin:
            start = line.find('"parent_asin": "')
            if start < 0 or line[start + 16:line.find('"', start + 16)] not in keep:
                continue
            record = json.loads(line)
            out.write(json.dumps({k: record.get(k) for k in fields}, ensure_ascii=False) + "\n")
            kept += 1
    print(f"giữ {kept} sản phẩm", file=sys.stderr)


def _price(value) -> float | None:
    try:
        price = float(str(value).replace("$", "").replace(",", ""))
        return price if price > 0 else None
    except (TypeError, ValueError):
        return None


def _plausible(name: str, value: str) -> bool:
    """Bỏ thông số nhập sai trên Amazon (RAM "2000000 MB", bộ nhớ "16384 GB")."""
    from app.recommender.features import _gigabytes, _number
    if name == "Kích thước màn hình":
        size = _number(value, r"(\d+(?:[.,]\d+)?)")
        return size is not None and 3 <= size <= 15
    size = _gigabytes(value)
    return size is not None and size <= (32 if name == "RAM" else 2048)


def build(data_dir: str):
    """→ (products, specs, events, baskets) đúng cột như app.recommender.data / evaluation đọc từ database."""
    path = Path(data_dir)
    ratings = pd.read_csv(path / "sample.csv")
    meta = pd.DataFrame([json.loads(line) for line in (path / "meta.jsonl").open(encoding="utf-8")])
    meta = meta.drop_duplicates("parent_asin").set_index("parent_asin")
    asins = sorted(set(ratings["parent_asin"]) & set(meta.index))
    ratings = ratings[ratings["parent_asin"].isin(asins)]
    pid = {asin: i + 1 for i, asin in enumerate(asins)}
    uid = {user: i + 1 for i, user in enumerate(sorted(ratings["user_id"].unique()))}
    ratings = ratings.assign(product_id=ratings["parent_asin"].map(pid), uid=ratings["user_id"].map(uid),
                             timestamp=pd.to_datetime(ratings["timestamp"], unit="ms"))

    meta = meta.loc[asins]
    details = meta["details"].apply(lambda d: d if isinstance(d, dict) else {})
    category = meta["categories"].apply(
        lambda c: c[2] if isinstance(c, list) and len(c) > 2 else c[1] if isinstance(c, list) and len(c) > 1 else "Khác")
    brand = [d.get("Brand") or s or "Khác" for d, s in zip(details, meta["store"])]
    brand = pd.Series(brand, index=meta.index).str.strip()
    top_brands = set(brand.value_counts().index[:TOP_BRANDS])
    brand = brand.where(brand.isin(top_brands), "Khác")
    price = meta["price"].apply(_price)
    price = price.fillna(price.groupby(category).transform("median")).fillna(price.median())
    first_seen = ratings.groupby("product_id")["timestamp"].min()
    category_ids = {name: i + 1 for i, name in enumerate(sorted(category.unique()))}

    products = pd.DataFrame({
        "product_id": [pid[a] for a in asins],
        "name": meta["title"].fillna("").to_numpy(),
        "category_id": category.map(category_ids).to_numpy(),
        "category": category.to_numpy(),
        "brand": brand.to_numpy(),
        "price": price.astype(float).to_numpy(),
        "variant_ram": None, "variant_storage": None,
        "is_active": True,
        "review_count": meta["rating_number"].fillna(0).astype(int).to_numpy(),
        "rating": 0.0,
    })
    products["created_at"] = products["product_id"].map(first_seen)
    products["updated_at"] = products["created_at"]
    products = products.set_index("product_id", drop=False)

    spec_keys = {"RAM": ("RAM", "RAM Memory Installed Size", "Memory"),
                 "Dung lượng lưu trữ": ("Memory Storage Capacity", "Storage Capacity", "Hard Disk Size"),
                 "Kích thước màn hình": ("Screen Size", "Standing screen display size")}
    specs = []
    for asin, d in zip(asins, details):
        for name, keys in spec_keys.items():
            value = next((str(d[k]) for k in keys if d.get(k)), None)
            if value and _plausible(name, value):
                specs.append({"product_id": pid[asin], "spec_name": name, "spec_value": value})
    specs = pd.DataFrame(specs, columns=["product_id", "spec_name", "spec_value"])

    events = pd.DataFrame({"user_id": ratings["uid"], "product_id": ratings["product_id"],
                           "interaction_type": "PURCHASE", "timestamp": ratings["timestamp"]})
    day = ratings["timestamp"].dt.floor("D")
    order_key = ratings["uid"].astype(str) + "|" + day.dt.strftime("%Y-%m-%d")
    baskets = pd.DataFrame({"order_id": pd.factorize(order_key)[0] + 1,
                            "user_id": ratings["uid"], "product_id": ratings["product_id"], "order_date": day})
    return products, specs, events, baskets


def run(data_dir: str, out_dir: str | None = None) -> None:
    from app import config
    from app.recommender import data, evaluation

    products, specs, events, baskets = build(data_dir)
    data.load_products = lambda: products
    data.load_specs = lambda: specs
    data.load_relations = lambda: pd.DataFrame(columns=["product_id", "related_product_id", "relation_type",
                                                        "strength"])
    data.load_needs = lambda: pd.DataFrame(columns=["need_id", "category_id", "name", "product_id"])
    evaluation.load_events = lambda: events
    evaluation.load_baskets = lambda: baskets
    print(f"{len(products)} sản phẩm, {events['user_id'].nunique()} người dùng, {len(events)} lượt, "
          f"{products['category'].nunique()} danh mục, {products['brand'].nunique()} thương hiệu", flush=True)

    result = evaluation.evaluate()
    result["dataset"] = {
        "name": "Amazon Reviews 2023 – Cell Phones & Accessories (5-core)",
        "sampling": f"top {TOP_ITEMS} sản phẩm, {CORE}-core, {USERS} người dùng ngẫu nhiên (seed {SEED})",
        "products": len(products), "users": int(events["user_id"].nunique()), "events": len(events),
        "categories": int(products["category"].nunique()), "brands": int(products["brand"].nunique()),
        "from": str(events["timestamp"].min().date()), "to": str(events["timestamp"].max().date()),
        "baskets": int(baskets["order_id"].nunique()),
        "multiItemBaskets": int((baskets.groupby("order_id").size() > 1).sum()),
    }
    out = Path(out_dir) if out_dir else config.EVAL_RESULTS_DIR
    out.mkdir(parents=True, exist_ok=True)
    (out / "amazon_cell_latest.json").write_text(json.dumps(result, ensure_ascii=False, indent=2, default=str),
                                                 encoding="utf-8")
    header = (f"# Đánh giá trên dữ liệu công khai: {result['dataset']['name']}\n\n"
              f"Lấy mẫu: {result['dataset']['sampling']}. {result['dataset']['products']} sản phẩm, "
              f"{result['dataset']['users']} người dùng, {result['dataset']['events']} lượt "
              f"({result['dataset']['from']} → {result['dataset']['to']}). Mỗi đánh giá là một lượt mua; "
              f"đơn hàng = sản phẩm cùng người, cùng ngày.\n\n")
    (out / "amazon_cell_latest.md").write_text(header + evaluation.markdown(result), encoding="utf-8")
    print(f"xong sau {result['seconds']} giây → {out / 'amazon_cell_latest.md'}")


if __name__ == "__main__":
    command, *args = sys.argv[1:]
    {"sample": sample, "filter-meta": filter_meta, "run": run}[command](*args)
