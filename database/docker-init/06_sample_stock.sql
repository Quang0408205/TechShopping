-- Tồn kho mẫu cho mọi phiên bản đang bán (2026-10-30): dữ liệu demo, KHÔNG dùng cho production.
--   Ở mỗi chi nhánh đang mở, phiên bản của sản phẩm đang bán (giá > 0) mà chi nhánh đó chưa còn cái nào → nhập
--   5–40 cái, kèm phiếu nhập (stock_movements IN). Số lượng tính từ mã phiên bản + chi nhánh nên chạy ở máy nào
--   cũng ra như nhau. Dòng tồn kho đang còn hàng không bị đổi.
-- Docker chạy file này sau 05 khi tạo volume mới (docker-compose.yml). Chạy lại nhiều lần vẫn an toàn
-- (chỉ nhập cho chi nhánh mới thêm và phiên bản đã bán hết ở chi nhánh đó):
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping < database/docker-init/06_sample_stock.sql
-- Sau khi chạy trên DB dev: dọn Redis cache:*.

begin;

do $$
begin
    if current_database() <> 'techshopping' then
        raise exception 'Chỉ nạp tồn kho mẫu vào database techshopping.';
    end if;
end $$;

create temp table ss_target on commit drop as
select s.store_id, v.variant_id,
       5 + abs(hashtext(v.variant_id || '-' || s.store_id)) % 36 as quantity
from product_variants v
join products p on p.product_id = v.product_id
cross join stores s
where p.is_active and p.deleted_at is null
  and v.price > 0
  and s.is_active
  and not exists (
      select 1 from inventory i
      where i.store_id = s.store_id and i.variant_id = v.variant_id and i.quantity > 0);

with stocked as (
    insert into inventory (store_id, variant_id, quantity)
    select store_id, variant_id, quantity from ss_target
    on conflict (store_id, variant_id) do update
        set quantity = excluded.quantity, updated_at = current_timestamp
        where inventory.quantity = 0
    returning store_id, variant_id, quantity
)
insert into stock_movements (store_id, variant_id, movement_type, quantity_change, note)
select store_id, variant_id, 'IN', quantity, 'Nhập kho mẫu'
from stocked;

select count(distinct variant_id) as variants_stocked, coalesce(sum(quantity), 0) as units
from ss_target;

commit;
