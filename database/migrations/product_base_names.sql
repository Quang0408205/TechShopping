-- Tên sản phẩm chỉ còn tên máy (2026-10-30): dung lượng / màu / sạc chọn ở trang chi tiết.
--   Điện thoại, máy tính bảng, laptop ĐANG BÁN có tên dạng "<tên máy> <RAM/bộ nhớ>[/<n>GPU] [<màu> | <n>W]",
--   kèm tiền tố "Điện thoại / Máy tính bảng / Laptop" ở một số sản phẩm cào về:
--   1. Phần cấu hình chuyển vào phiên bản: màu → cột color (khi còn trống); "8GPU", "Sạc 70W" → tên phiên bản
--      ("16GB/512GB 8GPU Sạc 30W - Xanh đen"), trang chi tiết hiện thành nhãn "16GB/512GB 8GPU Sạc 30W".
--   2. Tên sản phẩm = tên máy ("MacBook Air 13 inch M5 16GB/512GB/8GPU 30W" → "MacBook Air 13 inch M5").
--   Sau file này chạy lại merge_product_variants.sql: các sản phẩm giờ trùng tên (cùng danh mục) gộp thành
--   một sản phẩm nhiều phiên bản.
-- Không đổi sản phẩm đang ẩn, không đổi slug. Chạy lại nhiều lần vẫn an toàn (lần sau không còn tên nào để đổi).
-- SAO LƯU DB TRƯỚC. Docker chạy file này sau 04 và trước 05 khi tạo volume mới (docker-compose.yml).
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping < database/migrations/product_base_names.sql
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping < database/migrations/merge_product_variants.sql
-- Sau khi chạy trên DB dev: dọn Redis cache:* và chạy lại dữ liệu AI (build_relations, build_index, huấn luyện).

begin;

create temp table nb_split on commit drop as
select s.product_id, s.name,
       trim(s.m[1]) as base,
       regexp_replace(s.m[2], '\s+', '', 'g') as capacity,
       s.m[3] as gpu,
       nullif(trim(coalesce(s.m[4], '')), '') as extra
from (
    select p.product_id, p.name,
           regexp_match(
               regexp_replace(p.name, '^(Điện thoại|Máy tính bảng|Laptop)\s+', '', 'i'),
               '^(.+?)\s+(\d+\s*GB(?:\s*/\s*\d+\s*(?:GB|TB))*)(?:\s*/\s*(\d+)\s*GPU)?(?:\s+(.*))?$', 'i') as m
    from products p
    join categories c on c.category_id = p.category_id
    where p.deleted_at is null
      and coalesce(p.is_active, true)
      and c.slug in ('dien-thoai', 'may-tinh-bang', 'laptop')
) s
where s.m is not null;

-- phần còn lại sau dung lượng: "70W" = sạc; không có chữ số = màu; còn lại giữ nguyên trong tên phiên bản
create temp table nb_plan on commit drop as
select product_id, name, base, capacity,
       case when gpu is not null then gpu || 'GPU' end as gpu,
       case when extra ~* '^\d+\s*W$' then 'Sạc ' || upper(regexp_replace(extra, '\s+', '', 'g')) end as charger,
       case when extra is not null and extra !~ '\d' then extra end as color,
       case when extra is not null and extra ~ '\d' and extra !~* '^\d+\s*W$' then extra end as other
from nb_split;


-- ---------- 1. phiên bản ----------
update product_variants v
set color = pl.color,
    updated_at = now()
from nb_plan pl
where v.product_id = pl.product_id
  and pl.color is not null
  and coalesce(trim(v.color), '') = '';

update product_variants v
set variant_name = concat_ws(' - ',
        concat_ws(' ',
            coalesce(nullif(concat_ws('/', nullif(replace(v.ram, ' ', ''), ''), nullif(replace(v.storage, ' ', ''), '')), ''),
                     pl.capacity),
            pl.gpu, pl.charger, pl.other),
        nullif(trim(v.color), '')),
    updated_at = now()
from nb_plan pl
where v.product_id = pl.product_id
  and (pl.gpu is not null or pl.charger is not null or pl.other is not null);


-- ---------- 2. tên sản phẩm ----------
update products p
set name = pl.base,
    updated_at = now()
from nb_plan pl
where p.product_id = pl.product_id
  and p.name <> pl.base;

-- tên không có dung lượng nhưng còn tiền tố danh mục ("Điện thoại Tecno Pova Curve 2 5G")
update products p
set name = regexp_replace(p.name, '^(Điện thoại|Máy tính bảng|Laptop)\s+', '', 'i'),
    updated_at = now()
from categories c
where c.category_id = p.category_id
  and c.slug in ('dien-thoai', 'may-tinh-bang', 'laptop')
  and p.deleted_at is null
  and coalesce(p.is_active, true)
  and p.name ~* '^(Điện thoại|Máy tính bảng|Laptop)\s+';

commit;
