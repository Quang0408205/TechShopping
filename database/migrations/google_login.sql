-- Đăng nhập bằng Google: mã định danh Google (claim "sub") của tài khoản đã liên kết.
-- Mỗi mã chỉ gắn với một tài khoản; tài khoản chưa liên kết để trống.
-- Chạy lại được.

alter table users add column if not exists google_sub varchar(64);

create unique index if not exists uq_users_google_sub on users(google_sub) where google_sub is not null;
