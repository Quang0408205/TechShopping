alter table payments add column if not exists refund_bank varchar(100);
alter table payments add column if not exists refund_account_number varchar(30);
alter table payments add column if not exists refund_account_holder varchar(100);
