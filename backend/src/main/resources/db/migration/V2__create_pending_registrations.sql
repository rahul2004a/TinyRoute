create table pending_registrations (
    id uuid primary key,
    token_hash varchar(64) not null,
    email_normalized varchar(254) not null,
    password_hash varchar(255) not null,
    otp_hash varchar(255) not null,
    otp_expires_at timestamp with time zone not null,
    failed_attempts integer not null default 0,
    resend_count integer not null default 0,
    resend_window_started_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint pending_registrations_token_hash_unique unique (token_hash),
    constraint pending_registrations_email_normalized_unique unique (email_normalized),
    constraint pending_registrations_failed_attempts_check check (failed_attempts between 0 and 5),
    constraint pending_registrations_resend_count_check check (resend_count between 0 and 3)
);
