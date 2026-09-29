create table password_reset_tokens (
    id uuid primary key,
    user_id uuid not null,
    token_hash varchar(64) not null,
    expires_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    constraint password_reset_tokens_user_unique unique (user_id),
    constraint password_reset_tokens_token_hash_unique unique (token_hash),
    constraint password_reset_tokens_user_fk
        foreign key (user_id) references users (id) on delete restrict
);
