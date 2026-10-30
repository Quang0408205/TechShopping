-- gộp các vị trí nhân viên. Chạy lại nhiều lần vẫn an toàn.
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping < database/migrations/staff_position_label.sql
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping_test < database/migrations/staff_position_label.sql
-- Các nhãn "Nhân viên bán hàng / Nhân viên kỹ thuật / Thu ngân" chỉ để hiển thị, không đổi quyền, nên gộp thành
-- "Nhân viên chi nhánh" (backend: StaffRoles.DEFAULT_POSITION). Nhãn "Quản lý chi nhánh" đi theo role BRANCH_MANAGER, giữ nguyên.

update employee_assignments
set position_at_store = 'Nhân viên chi nhánh'
where position_at_store is distinct from 'Quản lý chi nhánh'
  and position_at_store is distinct from 'Nhân viên chi nhánh';
