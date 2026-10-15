-- Kế hoạch v2 / GĐ1 (2026-10-10): vai trò BRANCH_MANAGER. Chạy lại nhiều lần vẫn an toàn.
-- ⚠ Đổi role của dữ liệu thật: SAO LƯU DB trước khi chạy (pg_dump).
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping < database/migrations/branch_manager_role.sql
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping_test < database/migrations/branch_manager_role.sql
-- Quy tắc sau migration: tài khoản nội bộ có ĐÚNG MỘT role trong {STAFF, BRANCH_MANAGER, ADMIN}, không mang CUSTOMER;
-- mỗi chi nhánh có tối đa 1 BRANCH_MANAGER đang làm việc. Đơn hàng / giỏ hàng của họ giữ nguyên.

insert into roles (name, description)
values ('BRANCH_MANAGER', 'Branch manager')
on conflict (name) do nothing;

do $$
declare
    r record;
begin
    -- 1 quản lý / chi nhánh: giữ người có phân công sớm nhất, những người còn lại về "Nhân viên bán hàng"
    for r in
        select ea.assignment_id, ea.store_id, e.user_id, u.username,
               row_number() over (partition by ea.store_id order by ea.start_date, ea.assignment_id) as rn
        from employee_assignments ea
        join employees e on e.employee_id = ea.employee_id
        join users u on u.user_id = e.user_id
        where ea.is_active and ea.end_date is null
          and ea.position_at_store = 'Quản lý chi nhánh'
          and exists (select 1 from user_roles ur join roles ro on ro.role_id = ur.role_id
                      where ur.user_id = e.user_id and ro.name = 'STAFF')
          and not exists (select 1 from user_roles ur join roles ro on ro.role_id = ur.role_id
                          where ur.user_id = e.user_id and ro.name = 'ADMIN')
        order by ea.store_id, rn
    loop
        if r.rn = 1 then
            insert into user_roles (user_id, role_id)
            select r.user_id, role_id from roles where name = 'BRANCH_MANAGER'
            on conflict do nothing;
            delete from user_roles
            where user_id = r.user_id and role_id = (select role_id from roles where name = 'STAFF');
            raise notice 'Chi nhánh % -> quản lý: % (user %)', r.store_id, r.username, r.user_id;
        else
            update employee_assignments set position_at_store = 'Nhân viên bán hàng'
            where assignment_id = r.assignment_id;
            raise notice 'Chi nhánh % đã có quản lý -> % (user %) về Nhân viên bán hàng', r.store_id, r.username, r.user_id;
        end if;
    end loop;

    -- tài khoản nội bộ không mang CUSTOMER
    for r in
        select ur.user_id, u.username
        from user_roles ur
        join roles ro on ro.role_id = ur.role_id and ro.name = 'CUSTOMER'
        join users u on u.user_id = ur.user_id
        where exists (select 1 from user_roles x join roles rx on rx.role_id = x.role_id
                      where x.user_id = ur.user_id and rx.name in ('STAFF', 'BRANCH_MANAGER', 'ADMIN'))
    loop
        delete from user_roles
        where user_id = r.user_id and role_id = (select role_id from roles where name = 'CUSTOMER');
        raise notice 'Bỏ role CUSTOMER của tài khoản nội bộ % (user %)', r.username, r.user_id;
    end loop;

    -- ADMIN không kèm STAFF / BRANCH_MANAGER
    for r in
        select ur.user_id, u.username, ro.name as role_name
        from user_roles ur
        join roles ro on ro.role_id = ur.role_id and ro.name in ('STAFF', 'BRANCH_MANAGER')
        join users u on u.user_id = ur.user_id
        where exists (select 1 from user_roles x join roles rx on rx.role_id = x.role_id
                      where x.user_id = ur.user_id and rx.name = 'ADMIN')
    loop
        delete from user_roles
        where user_id = r.user_id and role_id = (select role_id from roles where name = r.role_name);
        raise notice 'Bỏ role % của quản trị viên % (user %)', r.role_name, r.username, r.user_id;
    end loop;
end $$;
