-- Nhà cung cấp, phiếu nhập kho, phiếu chuyển kho và nhật ký chuyển khoản mẫu (2026-11-03): dữ liệu demo, KHÔNG dùng
-- cho production. Tên công ty, số tài khoản, nội dung chuyển khoản là giả lập.
--   * 7 nhà cung cấp (1 đã ngừng hợp tác), mỗi nhà cung cấp gắn với nhóm thương hiệu / danh mục mình phân phối.
--   * 42 phiếu nhập từ 02/10 đến 02/11 ở các chi nhánh đang mở: đa số đã nhập kho, vài phiếu nháp và đã huỷ. Phiếu đã
--     nhập cộng tồn kho, ghi sổ kho IN (giống lúc bấm "Hoàn tất") và cập nhật giá vốn bình quân theo thứ tự thời gian.
--   * 26 phiếu chuyển kho giữa chi nhánh (ưu tiên cùng thành phố): đã nhận, đang chuyển, nháp, huỷ trước / sau khi gửi;
--     trừ / cộng tồn kho và ghi sổ kho TRANSFER_OUT / TRANSFER_IN như khi thao tác trên trang.
--   * Nhật ký chuyển khoản: mỗi đơn chuyển khoản đã thanh toán từ 15/09 có một giao dịch khớp tự động (MATCHED), thêm
--     các giao dịch cần xử lý (UNMATCHED / REVIEW) và đã xử lý (RESOLVED / IGNORED) cho trang đối soát.
-- Chạy sau 05c (nhân viên), 06 (tồn kho), 07 (đơn hàng), 11 (giá vốn) — docker-compose.yml. Chạy lại nhiều lần vẫn an
-- toàn (đã có nhà cung cấp mẫu thì bỏ qua phần kho; giao dịch ngân hàng bỏ qua mã đã có):
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping < database/docker-init/16_sample_receipts_transfers_bank.sql
-- Sau khi chạy trên DB dev: dọn Redis cache:*.

begin;

do $$
begin
    if current_database() <> 'techshopping' then
        raise exception 'Chỉ nạp dữ liệu mẫu vào database techshopping.';
    end if;
end $$;

-- Người thao tác: nhân sự mẫu (05c) của từng chi nhánh: quản lý (không có thì nhân viên, rồi ADMIN) và một nhân viên
create temp table sd_actor on commit drop as
with admin as (
    select min(u.user_id) as user_id
    from users u join user_roles ur on ur.user_id = u.user_id join roles r on r.role_id = ur.role_id
    where r.name = 'ADMIN'),
staff as (
    select a.store_id, u.user_id, r.name as role
    from employee_assignments a
    join employees e on e.employee_id = a.employee_id
    join users u on u.user_id = e.user_id
    join user_roles ur on ur.user_id = u.user_id
    join roles r on r.role_id = ur.role_id
    where a.is_active and a.end_date is null and e.is_active is not false
      and r.name in ('BRANCH_MANAGER', 'STAFF')
      and u.email like '%@poyshop.vn')
select s.store_id, s.name, coalesce(s.city, '') as city,
       coalesce((select min(user_id) from staff where staff.store_id = s.store_id and role = 'BRANCH_MANAGER'),
                (select min(user_id) from staff where staff.store_id = s.store_id),
                (select user_id from admin)) as manager_id,
       coalesce((select max(user_id) from staff where staff.store_id = s.store_id and role = 'STAFF'),
                (select min(user_id) from staff where staff.store_id = s.store_id),
                (select user_id from admin)) as staff_id
from stores s
where s.is_active;

do $$
declare
    v_admin bigint := (select min(u.user_id) from users u join user_roles ur on ur.user_id = u.user_id
                       join roles r on r.role_id = ur.role_id where r.name = 'ADMIN');
    v_stores int[];
    v_suppliers int[];
    v_store sd_actor%rowtype;
    v_to sd_actor%rowtype;
    v_supplier int;
    v_status text;
    v_created timestamp;
    v_done timestamp;
    v_id bigint;
    v_line record;
    v_lines int;
    v_before bigint;
    v_notes text[] := array[
        'Nhập bổ sung hàng bán chạy', 'Nhập hàng theo đơn đặt tháng 10', null, 'Hàng mới về, kiểm đủ số lượng',
        null, 'Nhập hàng chuẩn bị khuyến mãi cuối tháng', 'Bổ sung hàng trưng bày', null,
        'Nhà cung cấp giao 2 đợt, đây là đợt 1', 'Nhập hàng theo đề xuất của quản lý chi nhánh'];
    v_tnotes text[] := array[
        'Cân đối tồn kho giữa chi nhánh', 'Khách đặt nhận tại chi nhánh nhận', null, 'Bổ sung hàng trưng bày',
        'Chi nhánh nhận sắp hết hàng', null, 'Điều chuyển hàng bán chậm'];
begin
    if exists (select 1 from suppliers where name = 'Công ty TNHH Phân phối Di động Sao Việt') then
        raise notice 'Đã có nhà cung cấp mẫu, bỏ qua phiếu nhập / chuyển kho.';
        return;
    end if;
    perform setseed(0.2611);

    insert into suppliers (name, phone, email, address, tax_code, note, is_active, created_at, updated_at) values
        ('Công ty TNHH Phân phối Di động Sao Việt', '02838201188', 'kinhdoanh@saovietmobile.vn',
         '128 Nguyễn Thị Minh Khai, Phường Xuân Hoà, TP. Hồ Chí Minh', '0312456789',
         'Điện thoại, máy tính bảng: Apple, Samsung, OPPO, Xiaomi, realme, vivo, HONOR', true, '2026-09-20 09:00', '2026-09-20 09:00'),
        ('Công ty CP Thiết bị Máy tính Hưng Thịnh', '02437556622', 'sales@hungthinhpc.vn',
         '45 Thái Hà, Phường Đống Đa, Hà Nội', '0105987321',
         'Laptop: HP, Dell, Lenovo, Asus, Acer, MSI; công nợ 30 ngày', true, '2026-09-20 09:10', '2026-09-20 09:10'),
        ('Công ty TNHH Đồng hồ Chính hãng Thời Gian Vàng', '02839305577', 'order@thoigianvang.vn',
         '72 Lê Thánh Tôn, Phường Sài Gòn, TP. Hồ Chí Minh', '0313778899',
         'Đồng hồ Nhật / Thuỵ Sĩ: Citizen, Casio, G-Shock, Edifice, Orient, Certina, Q&Q', true, '2026-09-20 09:20', '2026-09-20 09:20'),
        ('Công ty TNHH Đồng hồ Thời trang Ánh Dương', '02838117799', 'cskh@anhduongwatch.vn',
         '210 Cách Mạng Tháng Tám, Phường Nhiêu Lộc, TP. Hồ Chí Minh', '0314552233',
         'Đồng hồ thời trang: Smile Kid, Elio, MVW, Tommy Hilfiger, Festina, Movado, Lacoste, Candino', true, '2026-09-21 10:00', '2026-09-21 10:00'),
        ('Công ty CP Công nghệ Thông minh An Khang', '02363889900', 'hotro@ankhangtech.vn',
         '15 Nguyễn Văn Linh, Phường Hải Châu, Đà Nẵng', '0401667788',
         'Đồng hồ thông minh, Garmin', true, '2026-09-21 10:15', '2026-09-21 10:15'),
        ('Công ty TNHH Thương mại Phụ kiện Minh Phát', '02862981234', 'minhphat.pk@gmail.com',
         '9 Trần Quang Khải, Phường Tân Định, TP. Hồ Chí Minh', '0315001122',
         'Phụ kiện: tai nghe, sạc, cáp, ốp lưng; giao trong ngày nội thành', true, '2026-09-22 14:00', '2026-09-22 14:00'),
        ('Công ty TNHH Điện tử Nam Á', '02838669955', 'info@namaelectronics.vn',
         '33 Hoàng Văn Thụ, Phường Phú Nhuận, TP. Hồ Chí Minh', '0310223344',
         'Ngừng hợp tác từ 10/2026 (giao hàng trễ nhiều lần)', false, '2026-09-22 14:30', '2026-10-15 16:00');

    -- Phiên bản mỗi nhà cung cấp có thể giao (theo thương hiệu hoặc danh mục)
    create temp table sd_pool on commit drop as
    select s.supplier_id, v.variant_id, v.price, v.average_cost
    from suppliers s
    join product_variants v on true
    join products p on p.product_id = v.product_id
    left join brands b on b.brand_id = p.brand_id
    left join categories c on c.category_id = p.category_id
    where p.is_active and p.deleted_at is null and v.price > 0
      and case s.name
          when 'Công ty TNHH Phân phối Di động Sao Việt' then
              b.name in ('Apple', 'Samsung', 'OPPO', 'Xiaomi', 'realme', 'vivo', 'HONOR')
              and c.name in ('Điện Thoại', 'Máy Tính Bảng')
          when 'Công ty CP Thiết bị Máy tính Hưng Thịnh' then c.name = 'Laptop'
          when 'Công ty TNHH Đồng hồ Chính hãng Thời Gian Vàng' then
              b.name in ('CITIZEN', 'CASIO', 'G-SHOCK', 'EDIFICE CASIO', 'ORIENT', 'CERTINA', 'Q&Q')
          when 'Công ty TNHH Đồng hồ Thời trang Ánh Dương' then
              b.name in ('SMILE KID', 'ELIO', 'MVW', 'TOMMY HILFIGER', 'FESTINA', 'MOVADO', 'ERNEST BOREL', 'LACOSTE', 'CANDINO')
          when 'Công ty CP Công nghệ Thông minh An Khang' then c.name = 'Đồng Hồ Thông Minh' or b.name = 'Garmin'
          when 'Công ty TNHH Thương mại Phụ kiện Minh Phát' then c.name = 'Phụ Kiện'
          else false end;

    select array_agg(store_id order by store_id) into v_stores from sd_actor;
    select array_agg(supplier_id order by supplier_id) into v_suppliers
    from suppliers where is_active and exists (select 1 from sd_pool where sd_pool.supplier_id = suppliers.supplier_id);
    if v_stores is null or v_suppliers is null then
        raise notice 'Chưa có chi nhánh / sản phẩm, bỏ qua phiếu nhập / chuyển kho.';
        return;
    end if;

    -- Phiếu nhập: theo thứ tự thời gian để giá vốn bình quân đi đúng
    for i in 1..42 loop
        select * into v_store from sd_actor where store_id = v_stores[1 + (i * 5) % array_length(v_stores, 1)];
        v_supplier := v_suppliers[1 + floor(random() * array_length(v_suppliers, 1))::int];
        v_created := timestamp '2026-10-02' + floor((i - 1) * 31.0 / 42) * interval '1 day'
                     + (8 + floor(random() * 9)) * interval '1 hour' + floor(random() * 60) * interval '1 minute';
        v_status := case when i > 38 then 'DRAFT' when i in (7, 19, 30) then 'CANCELLED' else 'COMPLETED' end;
        v_done := least(v_created + case when random() < 0.6 then (1 + floor(random() * 4)) * interval '1 hour'
                                         else interval '1 day' end, timestamp '2026-11-03 07:30');

        insert into purchase_receipts (store_id, supplier_id, status, note, created_by, created_at,
                                       completed_by, completed_at, cancelled_at)
        values (v_store.store_id, v_supplier, v_status, v_notes[1 + (i % array_length(v_notes, 1))],
                v_store.staff_id, v_created,
                case when v_status = 'COMPLETED' then v_store.manager_id end,
                case when v_status = 'COMPLETED' then v_done end,
                case when v_status = 'CANCELLED' then v_done end)
        returning receipt_id into v_id;
        if v_status = 'CANCELLED' then
            update purchase_receipts set note = 'Nhà cung cấp báo hết hàng, huỷ phiếu' where receipt_id = v_id;
        end if;

        v_lines := 2 + floor(random() * 5)::int;
        insert into purchase_receipt_items (receipt_id, variant_id, quantity, unit_cost)
        select v_id, p.variant_id,
               case when p.price >= 15000000 then 2 + floor(random() * 6)
                    when p.price >= 2000000 then 3 + floor(random() * 10)
                    else 10 + floor(random() * 21) end,
               case when p.price < 10000 then round(coalesce(p.average_cost, p.price * 0.85) * (0.96 + random() * 0.06)::numeric, 2)
                    else round(coalesce(p.average_cost, p.price * 0.85) * (0.96 + random() * 0.06)::numeric, -3) end
        from (select * from sd_pool where supplier_id = v_supplier order by random() limit v_lines) p;

        if v_status = 'COMPLETED' then
            for v_line in select variant_id, quantity, unit_cost from purchase_receipt_items
                          where receipt_id = v_id order by variant_id loop
                select coalesce(sum(quantity), 0) into v_before from inventory where variant_id = v_line.variant_id;
                update product_variants
                set average_cost = case when v_before <= 0 or average_cost is null then round(v_line.unit_cost, 2)
                                        else round((average_cost * v_before + v_line.unit_cost * v_line.quantity)
                                                   / (v_before + v_line.quantity), 2) end
                where variant_id = v_line.variant_id;
                insert into inventory (store_id, variant_id, quantity, updated_at)
                values (v_store.store_id, v_line.variant_id, v_line.quantity, v_done)
                on conflict (store_id, variant_id) do update
                    set quantity = inventory.quantity + excluded.quantity, updated_at = excluded.updated_at;
                insert into stock_movements (store_id, variant_id, movement_type, quantity_change, supplier_name, note,
                                             created_by, created_at, receipt_id)
                select v_store.store_id, v_line.variant_id, 'IN', v_line.quantity, s.name,
                       'Phiếu nhập PN' || lpad(v_id::text, 6, '0'), v_store.manager_id, v_done, v_id
                from suppliers s where s.supplier_id = v_supplier;
            end loop;
        end if;
    end loop;

    -- Phiếu chuyển kho: chọn hàng chi nhánh gửi còn nhiều, ưu tiên chi nhánh nhận cùng thành phố
    if array_length(v_stores, 1) < 2 then
        return;
    end if;
    for i in 1..26 loop
        select * into v_store from sd_actor where store_id = v_stores[1 + (i * 7) % array_length(v_stores, 1)];
        select * into v_to from sd_actor
        where store_id <> v_store.store_id
        order by (city = v_store.city and random() < 0.8) desc, random() limit 1;
        v_created := timestamp '2026-10-06' + floor((i - 1) * 27.5 / 26) * interval '1 day'
                     + (8 + floor(random() * 6)) * interval '1 hour' + floor(random() * 60) * interval '1 minute';
        v_status := case when i in (22, 25) then 'DRAFT' when i in (9, 23) then 'CANCELLED'
                         when i in (20, 21, 24, 26) then 'IN_TRANSIT' else 'RECEIVED' end;

        insert into stock_transfers (from_store_id, to_store_id, status, note, created_by, created_at)
        values (v_store.store_id, v_to.store_id, v_status, v_tnotes[1 + (i % array_length(v_tnotes, 1))],
                v_store.manager_id, v_created)
        returning transfer_id into v_id;

        insert into stock_transfer_items (transfer_id, variant_id, quantity)
        select v_id, i2.variant_id, 1 + floor(random() * least(5, i2.quantity - 5))::int
        from (select inv.variant_id, inv.quantity from inventory inv
              join product_variants v on v.variant_id = inv.variant_id
              join products p on p.product_id = v.product_id
              where inv.store_id = v_store.store_id and inv.quantity >= 8
                and p.is_active and p.deleted_at is null and v.price > 0
              order by random() limit 1 + floor(random() * 4)::int) i2;

        if v_status = 'DRAFT' or (v_status = 'CANCELLED' and i = 9) then
            if v_status = 'CANCELLED' then
                update stock_transfers
                set cancelled_by = v_store.manager_id, cancelled_at = v_created + interval '2 hour',
                    note = 'Lập nhầm chi nhánh nhận'
                where transfer_id = v_id;
            end if;
            continue;
        end if;

        -- Gửi đi
        v_done := v_created + (1 + floor(random() * 2)) * interval '1 hour';
        update stock_transfers set sent_by = v_store.manager_id, sent_at = v_done where transfer_id = v_id;
        for v_line in select variant_id, quantity from stock_transfer_items where transfer_id = v_id order by variant_id loop
            update inventory set quantity = quantity - v_line.quantity, updated_at = v_done
            where store_id = v_store.store_id and variant_id = v_line.variant_id;
            insert into stock_movements (store_id, variant_id, movement_type, quantity_change, note, created_by,
                                         created_at, transfer_id)
            values (v_store.store_id, v_line.variant_id, 'TRANSFER_OUT', -v_line.quantity, 'Chuyển tới ' || v_to.name,
                    v_store.manager_id, v_done, v_id);
        end loop;
        if v_status = 'IN_TRANSIT' then
            continue;
        end if;

        if v_status = 'CANCELLED' then
            -- Huỷ khi đang chuyển: hàng quay về chi nhánh gửi
            v_done := v_done + interval '4 hour';
            update stock_transfers
            set cancelled_by = v_store.manager_id, cancelled_at = v_done,
                note = 'Chi nhánh nhận đã nhập được hàng từ nhà cung cấp, huỷ chuyển'
            where transfer_id = v_id;
            for v_line in select variant_id, quantity from stock_transfer_items where transfer_id = v_id order by variant_id loop
                update inventory set quantity = quantity + v_line.quantity, updated_at = v_done
                where store_id = v_store.store_id and variant_id = v_line.variant_id;
                insert into stock_movements (store_id, variant_id, movement_type, quantity_change, note, created_by,
                                             created_at, transfer_id)
                values (v_store.store_id, v_line.variant_id, 'TRANSFER_IN', v_line.quantity,
                        'Huỷ phiếu chuyển CK' || lpad(v_id::text, 6, '0'), v_store.manager_id, v_done, v_id);
            end loop;
            continue;
        end if;

        -- Nhận hàng: cùng thành phố trong ngày, khác thành phố 1–2 ngày
        v_done := least(v_done + case when v_to.city = v_store.city then (2 + floor(random() * 3)) * interval '1 hour'
                                      else (24 + floor(random() * 6)) * interval '1 hour' end,
                        timestamp '2026-11-03 07:30');
        update stock_transfers set status = 'RECEIVED', received_by = v_to.staff_id, received_at = v_done
        where transfer_id = v_id;
        for v_line in select variant_id, quantity from stock_transfer_items where transfer_id = v_id order by variant_id loop
            insert into inventory (store_id, variant_id, quantity, updated_at)
            values (v_to.store_id, v_line.variant_id, v_line.quantity, v_done)
            on conflict (store_id, variant_id) do update
                set quantity = inventory.quantity + excluded.quantity, updated_at = excluded.updated_at;
            insert into stock_movements (store_id, variant_id, movement_type, quantity_change, note, created_by,
                                         created_at, transfer_id)
            values (v_to.store_id, v_line.variant_id, 'TRANSFER_IN', v_line.quantity, 'Nhận từ ' || v_store.name,
                    v_to.staff_id, v_done, v_id);
        end loop;
    end loop;
end $$;

-- Nhật ký chuyển khoản. Tài khoản nhận = tài khoản đã dùng ở giao dịch thật (nếu có), không thì mặc định của backend.
create temp table sd_bank on commit drop as
select coalesce((select account_number from bank_transactions
                 where status = 'MATCHED' and account_number is not null order by bank_transaction_id limit 1),
                '0123456789') as account,
       (select min(u.user_id) from users u join user_roles ur on ur.user_id = u.user_id
        join roles r on r.role_id = ur.role_id where r.name = 'ADMIN') as admin_id;

-- Đơn chuyển khoản đã trả tiền, chưa có giao dịch; 4 đơn mới nhất dành cho giao dịch "đã gán tay" (RESOLVED)
create temp table sd_paid on commit drop as
select row_number() over (order by p.paid_at desc, p.order_id desc) as rn, p.order_id, p.amount, p.paid_at,
       p.transaction_id
from payments p
where p.payment_method = 'BANK_TRANSFER' and p.installment_payment_id is null and p.status = 'PAID'
  and p.paid_at >= '2026-09-15' and p.paid_at < '2026-11-02' and p.transaction_id is not null
  and not exists (select 1 from bank_transactions b where b.order_id = p.order_id)
  and not exists (select 1 from bank_transactions b where b.external_id = p.transaction_id);

insert into bank_transactions (external_id, provider, amount, content, account_number, transferred_at, received_at,
                               status, order_id)
select transaction_id, 'WEBHOOK', amount,
       (array['', 'MBVCB.', 'IBFT ', 'TKThe '])[1 + order_id % 4] || 'DH' || lpad(order_id::text, 8, '0')
           || (array[' thanh toan don hang', '', ' chuyen tien', ' ck mua hang'])[1 + order_id % 4],
       (select account from sd_bank),
       date_trunc('second', paid_at - (5 + order_id % 50) * interval '1 second'), paid_at, 'MATCHED', order_id
from sd_paid
where rn > 4
on conflict (external_id) do nothing;

insert into bank_transactions (external_id, provider, amount, content, account_number, transferred_at, received_at,
                               status, order_id, resolved_by, resolved_at, note)
select transaction_id, 'WEBHOOK', amount,
       (array['chuyen tien mua dien thoai', 'NGUYEN VAN HAI chuyen khoan', 'thanh toan', 'CK tien hang'])[rn],
       (select account from sd_bank), date_trunc('second', paid_at - interval '40 second'), paid_at, 'RESOLVED',
       order_id, (select admin_id from sd_bank), paid_at + interval '35 minute',
       (array['Khách gọi hotline xác nhận chuyển cho đơn này, số tiền khớp',
              'Khách quên ghi mã đơn, đối chiếu số tiền và tên người chuyển',
              'Khách gửi ảnh biên lai qua Zalo',
              null])[rn]
from sd_paid
where rn <= 4
on conflict (external_id) do nothing;

-- Giao dịch cần xử lý / đã bỏ qua (mã FT + thời điểm nhận cố định nên chạy lại không nhân đôi)
insert into bank_transactions (external_id, provider, amount, content, account_number, transferred_at, received_at,
                               status, reason, order_id, resolved_by, resolved_at, note)
select 'FT' || to_char(x.at, 'YYMMDDHH24MISS'), 'WEBHOOK', x.amount, x.content, coalesce(x.account, (select account from sd_bank)),
       x.at - interval '20 second', x.at, x.status, x.reason, x.order_id,
       case when x.status = 'IGNORED' then (select admin_id from sd_bank) end,
       case when x.status = 'IGNORED' then x.at + interval '3 hour' end, x.note
from (values
    ('SAMPLE-BANK-01', 2490000.00, 'chuyen tien mua tai nghe', null, timestamp '2026-11-02 09:14:05', 'UNMATCHED',
     'Nội dung chuyển khoản không có mã đơn hàng', null::bigint, null),
    ('SAMPLE-BANK-02', 15990000.00, 'TRAN THI MAI chuyen khoan', null, timestamp '2026-11-02 15:41:30', 'UNMATCHED',
     'Nội dung chuyển khoản không có mã đơn hàng', null, null),
    ('SAMPLE-BANK-03', 890000.00, 'thanh toan', null, timestamp '2026-11-03 08:02:11', 'UNMATCHED',
     'Nội dung chuyển khoản không có mã đơn hàng', null, null),
    ('SAMPLE-BANK-04', 32490000.00, 'DH99990012 thanh toan don hang', null, timestamp '2026-11-01 20:25:44', 'UNMATCHED',
     'Không có đơn hàng DH99990012', null, null),
    ('SAMPLE-BANK-05', 5000000.00, 'DH00000000 dat coc', null, timestamp '2026-10-31 11:03:19', 'UNMATCHED',
     'Không có đơn hàng DH00000000', null, null),
    ('SAMPLE-BANK-06', 1200000.00, 'chuyen tien', '0987654321', timestamp '2026-10-30 16:47:52', 'UNMATCHED',
     'Tiền không chuyển vào tài khoản của cửa hàng', null, null),
    ('SAMPLE-BANK-07', 50000.00, 'Tra lai tien thua', null, timestamp '2026-10-20 10:12:00', 'IGNORED', null, null,
     'Khách chuyển nhầm, đã hoàn lại qua ngân hàng ngày 21/10'),
    ('SAMPLE-BANK-08', 2135.00, 'Tra lai tien gui', null, timestamp '2026-10-25 23:59:00', 'IGNORED', null, null,
     'Tiền lãi không kỳ hạn của ngân hàng, không liên quan đơn hàng'),
    ('SAMPLE-BANK-09', 3000000.00, 'POY chuyen quy noi bo', null, timestamp '2026-10-28 14:30:00', 'IGNORED', null, null,
     'Chuyển khoản nội bộ giữa các tài khoản công ty')
) as x(external_id, amount, content, account, at, status, reason, order_id, note)
on conflict (external_id) do nothing;

-- REVIEW: chuyển thiếu / thừa cho đơn đang chờ, chuyển lần hai cho đơn đã trả, đơn đã huỷ, đơn không chọn chuyển khoản
insert into bank_transactions (external_id, provider, amount, content, account_number, transferred_at, received_at,
                               status, reason, order_id)
select 'FT' || to_char(at, 'YYMMDDHH24MISS'), 'WEBHOOK', amt, 'DH' || lpad(order_id::text, 8, '0') || ' chuyen tien',
       (select account from sd_bank), at - interval '15 second', at, 'REVIEW',
       'Số tiền chuyển ' || round(amt)::text || 'đ khác số tiền của đơn DH' || lpad(order_id::text, 8, '0')
           || ' ' || round(due)::text || 'đ',
       order_id
from (
    -- Đơn chờ chuyển khoản: khách chuyển thiếu phí, làm tròn lên, chuyển thiếu, đặt cọc một nửa
    select k, order_id, due,
           case k when 1 then due - 30000 when 2 then round(due, -6) + 1000000
                  when 3 then due - 500000 else round(due / 2, -3) end as amt,
           timestamp '2026-11-03 07:00' - k * interval '5 hour' as at
    from (select row_number() over (order by o.order_date desc) as k, o.order_id, p.amount as due
          from orders o join payments p on p.order_id = o.order_id and p.installment_payment_id is null
          where p.payment_method = 'BANK_TRANSFER' and p.status = 'PENDING' and o.status = 'PENDING'
            and p.amount > 1500000 and o.order_date < '2026-11-02'
            and not exists (select 1 from bank_transactions b where b.order_id = o.order_id)
          order by o.order_date desc limit 4) r
) x
where not exists (select 1 from bank_transactions where external_id = 'FT261103020000')
on conflict (external_id) do nothing;

insert into bank_transactions (external_id, provider, amount, content, account_number, transferred_at, received_at,
                               status, reason, order_id)
select 'FT' || to_char(x.at, 'YYMMDDHH24MISS'), 'WEBHOOK', x.amount, 'DH' || lpad(x.order_id::text, 8, '0') || ' chuyen tien',
       (select account from sd_bank), x.at - interval '25 second', x.at, 'REVIEW', x.reason, x.order_id
from (
    (select 'SAMPLE-BANK-P1' as external_id, p.amount, p.order_id, timestamp '2026-11-02 19:22:10' as at,
            'Đơn DH' || lpad(p.order_id::text, 8, '0') || ' đã được thanh toán trước đó (có thể chuyển thừa)' as reason
     from payments p
     where p.payment_method = 'BANK_TRANSFER' and p.installment_payment_id is null and p.status = 'PAID'
       and p.paid_at between '2026-10-25' and '2026-11-02'
       and exists (select 1 from bank_transactions b where b.order_id = p.order_id and b.status = 'MATCHED')
     order by p.paid_at desc limit 1)
    union all
    (select 'SAMPLE-BANK-C1', p.amount, p.order_id, timestamp '2026-11-01 08:45:33',
            'Đơn DH' || lpad(p.order_id::text, 8, '0') || ' đã huỷ hoặc đã hoàn tiền (khoản thanh toán: '
                || p.status || '), cần hoàn tiền cho khách'
     from payments p join orders o on o.order_id = p.order_id
     where p.payment_method = 'BANK_TRANSFER' and p.installment_payment_id is null and p.status = 'CANCELLED'
       and o.status = 'CANCELLED' and o.order_date < '2026-11-01'
     order by o.order_date desc limit 1)
    union all
    (select 'SAMPLE-BANK-M1', p.amount, p.order_id, timestamp '2026-10-31 17:10:02',
            'Đơn DH' || lpad(p.order_id::text, 8, '0') || ' không thanh toán bằng chuyển khoản'
     from payments p join orders o on o.order_id = p.order_id
     where p.payment_method = 'COD' and p.installment_payment_id is null and o.status = 'PENDING'
       and o.order_date < '2026-10-31'
     order by o.order_date desc limit 1)
) x
on conflict (external_id) do nothing;

commit;
