-- Tích điểm khi đơn đã thanh toán (chuyển khoản được xác nhận, thẻ đã trả, trả góp được duyệt; COD vẫn là lúc giao vì
-- tiền thu khi giao), không còn đợi đơn giao xong. Backend làm việc này từ nay; file này cộng bù cho các đơn đã trả tiền
-- nhưng chưa giao trước khi đổi: tổng tiền vào total_spent, 1 điểm / 10.000đ tiền hàng thực trả (không tính ship) nhân
-- hệ số của hạng hiện tại, rồi xét lên hạng (không bao giờ hạ). Đơn đã có dòng EARN thì bỏ qua: chạy lại được.

do $$
declare
    o record;
    profile record;
    multiplier decimal(4, 2);
    earned int;
    reached varchar(20);
begin
    for o in
        select ord.order_id, ord.user_id, ord.total_amount,
               greatest(ord.total_amount - coalesce(ord.shipping_cost, 0) - coalesce(ord.tax_amount, 0), 0) as paid_goods,
               p.paid_at
        from orders ord
        join payments p on p.order_id = ord.order_id and p.installment_payment_id is null
        where p.status = 'PAID'
          and ord.status in ('PENDING', 'CONFIRMED', 'SHIPPING')
          and ord.user_id is not null
          and not exists (select 1 from loyalty_transactions t where t.order_id = ord.order_id and t.type = 'EARN')
        order by p.paid_at nulls last, ord.order_id
    loop
        insert into customer_profiles (customer_id) values (o.user_id) on conflict (customer_id) do nothing;
        select * into profile from customer_profiles where customer_id = o.user_id for update;
        select point_multiplier into multiplier from loyalty_tiers where tier_code = profile.tier_code;
        earned := floor(floor(o.paid_goods / 10000) * coalesce(multiplier, 1));
        -- 0 điểm thì không có dòng EARN để nhận ra lần sau: bỏ qua, tránh cộng total_spent hai lần
        continue when earned <= 0;

        update customer_profiles
        set total_spent = total_spent + o.total_amount,
            loyalty_points = loyalty_points + earned,
            updated_at = current_timestamp
        where customer_id = o.user_id;
        update orders set points_earned = earned where order_id = o.order_id;
        insert into loyalty_transactions (user_id, order_id, type, points, balance_after, note, created_at)
        values (o.user_id, o.order_id, 'EARN', earned, profile.loyalty_points + earned,
                'Tích điểm đơn DH' || lpad(o.order_id::text, 8, '0') || ' (hạng '
                    || (select name from loyalty_tiers where tier_code = profile.tier_code) || ' x'
                    || trim(trailing '.' from trim(trailing '0' from coalesce(multiplier, 1)::text)) || ')',
                coalesce(o.paid_at, current_timestamp));

        select t.tier_code into reached
        from loyalty_tiers t
        where t.min_spent <= profile.total_spent + o.total_amount
        order by t.sort_order desc
        limit 1;
        update customer_profiles c set tier_code = reached
        where c.customer_id = o.user_id
          and (select sort_order from loyalty_tiers where tier_code = reached)
              > (select sort_order from loyalty_tiers where tier_code = c.tier_code);
    end loop;
end $$;
