create table support_tickets (
    id bigserial primary key,
    ticket_number varchar(32) not null,
    user_id bigint not null references app_users(id),
    title varchar(150) not null,
    description text not null,
    status varchar(20) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uk_support_ticket_number unique (ticket_number),
    constraint ck_support_ticket_status check (status in ('OPEN','IN_PROGRESS','RESOLVED','CLOSED'))
);
create index idx_support_ticket_user_created on support_tickets(user_id,created_at);
create index idx_support_ticket_status_updated on support_tickets(status,updated_at);

create table support_messages (
    id bigserial primary key,
    ticket_id bigint not null references support_tickets(id) on delete cascade,
    sender_type varchar(10) not null,
    sender_name varchar(180) not null,
    message text not null,
    created_at timestamptz not null,
    constraint ck_support_message_sender check (sender_type in ('USER','ADMIN'))
);
create index idx_support_message_ticket_created on support_messages(ticket_id,created_at);

create table support_images (
    id bigserial primary key,
    ticket_id bigint not null references support_tickets(id) on delete cascade,
    secure_url varchar(1000) not null,
    public_id varchar(500) not null,
    original_name varchar(255) not null,
    content_type varchar(100) not null,
    created_at timestamptz not null
);
create index idx_support_image_ticket on support_images(ticket_id);

create table support_status_history (
    id bigserial primary key,
    ticket_id bigint not null references support_tickets(id) on delete cascade,
    previous_status varchar(20),
    new_status varchar(20) not null,
    changed_by varchar(180) not null,
    created_at timestamptz not null,
    constraint ck_support_history_previous check (previous_status is null or previous_status in ('OPEN','IN_PROGRESS','RESOLVED','CLOSED')),
    constraint ck_support_history_new check (new_status in ('OPEN','IN_PROGRESS','RESOLVED','CLOSED'))
);
create index idx_support_status_ticket_created on support_status_history(ticket_id,created_at);
