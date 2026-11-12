alter table support_tickets add column request_key varchar(64);
create unique index uk_support_ticket_user_request on support_tickets(user_id,request_key) where request_key is not null;
