create table links (
    id uuid primary key,
    code varchar(64) collate "C" not null unique,
    owner_id uuid not null references users (id) on delete restrict,
    destination_url text not null,
    status varchar(20) not null,
    click_count bigint not null default 0,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    deleted_at timestamp with time zone,
    expires_at timestamp with time zone,
    constraint links_status_check check (status in ('ACTIVE', 'DISABLED', 'DELETED'))
);
create index links_owner_created_idx on links (owner_id, created_at desc, id desc);

create table account_deletion_cleanup (
    id uuid primary key,
    user_id uuid not null references users (id) on delete restrict,
    kind varchar(20) not null,
    redirect_code varchar(64),
    attempts integer not null default 0,
    next_attempt_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    constraint account_deletion_cleanup_kind_check check (kind in ('SESSION', 'REDIRECT_CACHE')),
    constraint account_deletion_cleanup_code_check check (
        (kind = 'SESSION' and redirect_code is null) or
        (kind = 'REDIRECT_CACHE' and redirect_code is not null)
    )
);
create index account_deletion_cleanup_due_idx on account_deletion_cleanup (next_attempt_at, created_at);
