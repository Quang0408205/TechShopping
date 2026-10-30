-- Bỏ ảnh trùng sau khi gộp phiên bản (2026-10-30).
--   Mỗi bản dung lượng trên TGDĐ là một trang riêng (/Images/<danh mục>/<mã trang>/...) nhưng cùng một bộ ảnh
--   cho mỗi màu; gộp "iPhone 18 Pro Max 256GB / 512GB / 1TB / 2TB" thành một sản phẩm thì mỗi màu có 4 bộ ảnh
--   giống nhau (44 ảnh thay vì 11). Tên file khác nhau ("...-512gb-den-1", "...-1tb-den-1") nên không lọc theo tên.
--   Với mỗi (sản phẩm, màu): chỉ giữ ảnh của một trang nguồn (trang có nhiều ảnh nhất, hoà thì trang hiện trước);
--   ảnh không nằm trong thư mục trang (ảnh video, ảnh tải lên) giữ nguyên. Sản phẩm mất ảnh chính thì lấy ảnh đầu.
-- Chạy lại nhiều lần vẫn an toàn. Raw_data/import_catalog.py tự chạy file này sau merge_product_variants.sql.
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping < database/migrations/dedupe_product_images.sql
-- Sau khi chạy trên DB dev: dọn Redis cache:*.

begin;

create temp table di_source on commit drop as
select image_id, product_id, color, display_order,
       substring(image_url from '/Images/\d+/(\d+)/') as folder
from product_images;

create temp table di_keep on commit drop as
select distinct on (product_id, color) product_id, color, folder
from (
    select product_id, color, folder, count(*) as images, min(display_order) as first_order
    from di_source
    where folder is not null
    group by product_id, color, folder
) f
order by product_id, color, images desc, first_order;

delete from product_images i
using di_source s
join di_keep k on k.product_id = s.product_id and k.color is not distinct from s.color
where i.image_id = s.image_id
  and s.folder <> k.folder;

update product_images set is_primary = true
where image_id in (
    select distinct on (i.product_id) i.image_id
    from product_images i
    where not exists (select 1 from product_images p where p.product_id = i.product_id and p.is_primary)
    order by i.product_id, (i.color is null) desc, i.display_order, i.image_id
);

commit;

select count(*) as images_left from product_images;
