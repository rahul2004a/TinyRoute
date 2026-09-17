create table users (
    id uuid primary key,
    email_normalized varchar(254) not null,
    token_version integer not null default 0,
    deleted_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint users_email_normalized_unique unique (email_normalized)
);

create table auth_identities (
    id uuid primary key,
    user_id uuid not null,
    provider varchar(20) not null,
    subject varchar(255) not null,
    secret_hash varchar(255),
    created_at timestamp with time zone not null,
    constraint auth_identities_user_fk
        foreign key (user_id) references users (id) on delete restrict,
    constraint auth_identities_provider_subject_unique unique (provider, subject),
    constraint auth_identities_provider_check check (provider in ('PASSWORD', 'GOOGLE'))
);
