package ru.soat.ohapp.saas.service.admin.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.soat.ohapp.saas.dto.admin.AdminUserView;
import ru.soat.ohapp.saas.dto.admin.CreateUserRequest;
import ru.soat.ohapp.saas.dto.admin.UpdateUserRequest;
import ru.soat.ohapp.saas.service.admin.UserAdminService;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class UserAdminServiceImpl implements UserAdminService {

    private final NamedParameterJdbcTemplate jdbc;

    private static final RowMapper<AdminUserView> USER_VIEW_MAPPER = new RowMapper<>() {
        @Override public AdminUserView mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new AdminUserView(
                    (UUID) rs.getObject("id"),
                    rs.getString("full_name"),
                    rs.getString("email"),
                    rs.getString("role")
            );
        }
    };

    @Override
    public UUID createUser(CreateUserRequest req) {
        // 1) Проверим, существует ли пользователь с таким email
        UUID userId = findUserIdByEmail(req.email()).orElseGet(() -> {
            UUID newId = UUID.randomUUID();
            jdbc.update("""
                    insert into users(id, full_name, email, enabled, created_at, updated_at)
                    values (:id, :fullName, :email, true, now(), now())
                    """, new MapSqlParameterSource()
                    .addValue("id", newId)
                    .addValue("fullName", req.fullName())
                    .addValue("email", req.email().toLowerCase())
            );
            return newId;
        });

        // 2) Привяжем к тенанту с ролью, если связи ещё нет
        jdbc.update("""
            insert into user_tenants(user_id, tenant_id, role)
            select :userId, :tenantId, :role
            where not exists(
                select 1 from user_tenants
                where user_id = :userId and tenant_id = :tenantId
            )
            """, new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("tenantId", req.tenantId())
                .addValue("role", req.role())
        );

        return userId;
    }

    @Override
    public void updateUser(UUID userId, UpdateUserRequest req) {
        jdbc.update("""
            update users
               set full_name = :fullName,
                   email     = :email,
                   updated_at= now()
             where id = :id
            """, new MapSqlParameterSource()
                .addValue("id", userId)
                .addValue("fullName", req.fullName())
                .addValue("email", req.email().toLowerCase())
        );
        // смену роли при необходимости можно делать отдельным вызовом, либо дописать тут через user_tenants
    }

    @Override
    public void deleteUser(UUID userId) {
        // мягкое удаление (флаг enabled=false), чтобы не ломать внешние ключи
        jdbc.update("""
            update users set enabled = false, updated_at = now()
             where id = :id
            """, new MapSqlParameterSource().addValue("id", userId)
        );
        // при необходимости разорвать связь с тенантами:
        // jdbc.update("delete from user_tenants where user_id=:id", Map.of("id", userId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminUserView> listUsers(UUID tenantId, int page, int size) {
        int offset = Math.max(page, 0) * Math.max(size, 1);
        return jdbc.query("""
            select u.id, u.full_name, u.email, ut.role
              from users u
              join user_tenants ut on ut.user_id = u.id
             where ut.tenant_id = :tenantId
               and coalesce(u.enabled, true) = true
             order by u.full_name
             limit :size offset :offset
            """, new MapSqlParameterSource()
                        .addValue("tenantId", tenantId)
                        .addValue("size", size)
                        .addValue("offset", offset),
                USER_VIEW_MAPPER
        );
    }

    private Optional<UUID> findUserIdByEmail(String email) {
        try {
            UUID id = jdbc.queryForObject(
                    "select id from users where lower(email) = :email limit 1",
                    new MapSqlParameterSource().addValue("email", email.toLowerCase()),
                    (rs, rn) -> (UUID) rs.getObject("id")
            );
            return Optional.ofNullable(id);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
