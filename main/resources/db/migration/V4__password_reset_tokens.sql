create table if not exists password_reset_tokens (
                                                     id               bigserial primary key,
                                                     user_account_id  uuid not null,
                                                     token_hash       varchar(255) not null,
    created_at       timestamp not null,
    expires_at       timestamp not null,
    used_at          timestamp null
    );

create index if not exists idx_prt_user on password_reset_tokens(user_account_id);
