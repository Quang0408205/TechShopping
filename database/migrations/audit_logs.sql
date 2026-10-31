-- Nhật ký thao tác của nhân sự: mỗi yêu cầu ghi dữ liệu (POST / PUT / PATCH / DELETE) thành công của tài khoản
-- nhân viên / quản lý chi nhánh / quản trị viên được ghi một dòng. Chỉ thêm, không sửa, không xoá qua API.
-- user_id không có khoá ngoại để nhật ký còn nguyên khi tài khoản bị xoá; username và họ tên lưu kèm theo.
-- Chạy lại được.

create table if not exists audit_logs (
    log_id bigserial primary key,
    user_id bigint not null,
    username varchar(50) not null,
    full_name varchar(120),
    role varchar(20) not null,
    method varchar(8) not null,
    path varchar(300) not null,
    action_label varchar(150) not null,
    entity_type varchar(50) not null,
    entity_id varchar(40),
    status int not null,
    ip varchar(64),
    request_data text,
    created_at timestamp not null default current_timestamp
);

create index if not exists idx_audit_logs_created on audit_logs (created_at desc);
create index if not exists idx_audit_logs_user on audit_logs (user_id, created_at desc);
create index if not exists idx_audit_logs_entity on audit_logs (entity_type, created_at desc);
