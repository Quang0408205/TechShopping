-- Kế hoạch v3 / mật khẩu tạm (2026-10-15): tài khoản nội bộ mới tuyển phải đổi mật khẩu ở lần đăng nhập đầu,
-- và mật khẩu tạm hết hạn nếu chưa đổi. Chạy lại nhiều lần vẫn an toàn.
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping < database/migrations/temporary_password.sql
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping_test < database/migrations/temporary_password.sql
-- must_change_password = true: chỉ được đổi mật khẩu, mọi API khác bị chặn (PASSWORD_CHANGE_REQUIRED).
-- password_expires_at: quá hạn mà chưa đổi thì không đăng nhập được (TEMP_PASSWORD_EXPIRED); null = không hết hạn.

alter table users add column if not exists must_change_password boolean not null default false;
alter table users add column if not exists password_expires_at timestamp;
