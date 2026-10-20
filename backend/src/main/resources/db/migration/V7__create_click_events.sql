create table click_events (
    id uuid primary key,
    link_id uuid not null references links(id) on delete cascade,
    clicked_at timestamp with time zone not null,
    referrer_domain varchar(253),
    device varchar(20) not null,
    operating_system varchar(20) not null,
    browser varchar(30) not null,
    country varchar(2),
    city varchar(160)
);
create index click_events_link_time_idx on click_events(link_id, clicked_at desc);
alter table links add constraint links_click_count_nonnegative check (click_count >= 0);
