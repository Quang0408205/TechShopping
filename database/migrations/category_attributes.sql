create table if not exists category_attributes (
    category_id int not null references categories(category_id) on delete cascade,
    attribute_id int not null references attributes(attribute_id) on delete cascade,
    is_required boolean not null default true,
    is_filterable boolean not null default true,
    display_order int,
    primary key (category_id, attribute_id)
);

create index if not exists idx_category_attributes_attribute_id on category_attributes(attribute_id);
