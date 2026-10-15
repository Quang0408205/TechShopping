-- Phase AI (2026-10-13): khuyến nghị sản phẩm + chatbot AI. Chạy lại nhiều lần vẫn an toàn.
-- db tạo mới từ database/techshopping.sql đã có sẵn mọi thứ dưới đây.
--   Get-Content database\migrations\ai_recommendation.sql -Raw | docker exec -i techshopping-postgres psql -U postgres -d techshopping
--   Get-Content database\migrations\ai_recommendation.sql -Raw | docker exec -i techshopping-postgres psql -U postgres -d techshopping_test
-- Cần image pgvector/pgvector:pg16 (docker-compose.yml) cho bảng kb_chunks.

-- user_interactions: loại hành vi viết hoa giống các enum khác; điểm = trọng số w_t của hành vi
update user_interactions set interaction_type = upper(interaction_type) where interaction_type <> upper(interaction_type);

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'chk_user_interactions_type') then
        alter table user_interactions add constraint chk_user_interactions_type
            check (interaction_type in ('VIEW', 'CLICK', 'LIKE', 'ADD_TO_CART', 'PURCHASE'));
    end if;
end $$;

create index if not exists idx_user_interactions_user_product_type
    on user_interactions(user_id, product_id, interaction_type, timestamp);

-- recommendations: điểm Hybrid nằm trong [0, 1] nên decimal(5,3) là đủ
create index if not exists idx_recommendations_user_created on recommendations(user_id, created_at);

-- product_relations: mỗi cặp + loại quan hệ chỉ một dòng
create unique index if not exists uq_product_relations_pair_type
    on product_relations(product_id, related_product_id, relation_type);

-- chat_sessions: khách chưa đăng nhập vẫn được hỏi câu chung (mục 3.6.5 d), nên user_id được phép null;
-- trình duyệt giữ session_token ngẫu nhiên (không đoán được) thay cho session_id tăng dần
alter table chat_sessions alter column user_id drop not null;
alter table chat_sessions add column if not exists session_token varchar(64);
alter table chat_sessions add column if not exists last_message_at timestamp;
create unique index if not exists uq_chat_sessions_token on chat_sessions(session_token);
create index if not exists idx_chat_sessions_last_message on chat_sessions(last_message_at);

-- kho tri thức RAG (mục 3.6.2): mỗi dòng là một chunk + vector của nó
create extension if not exists vector;

create table if not exists kb_chunks (
    chunk_id bigserial primary key,
    doc_type varchar(30) not null,
    source varchar(255) not null,
    product_id bigint references products(product_id) on delete cascade,
    title varchar(500),
    content text not null,
    -- metadata để lọc khi truy xuất: {"category": ..., "brand": ..., "price": ...}
    metadata jsonb not null default '{}',
    embedding vector(384) not null,
    created_at timestamp default current_timestamp
);

alter table kb_chunks add column if not exists metadata jsonb not null default '{}';

create index if not exists idx_kb_chunks_doc_type on kb_chunks(doc_type);
create index if not exists idx_kb_chunks_embedding on kb_chunks using hnsw (embedding vector_cosine_ops);

-- tìm kiếm từ khoá (full-text) cho RAG lai: bỏ dấu tiếng Việt, tên sản phẩm (title) nặng hơn nội dung
create extension if not exists unaccent;

create or replace function f_unaccent(text) returns text
    language sql immutable parallel safe strict
as $$ select public.unaccent('public.unaccent', $1) $$;

alter table kb_chunks add column if not exists search_tsv tsvector
    generated always as (
        setweight(to_tsvector('simple', f_unaccent(coalesce(title, ''))), 'A') ||
        setweight(to_tsvector('simple', f_unaccent(content)), 'D')
    ) stored;

create index if not exists idx_kb_chunks_search on kb_chunks using gin (search_tsv);
