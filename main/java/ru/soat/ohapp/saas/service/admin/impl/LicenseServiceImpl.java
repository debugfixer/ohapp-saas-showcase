package ru.soat.ohapp.saas.service.admin.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.soat.ohapp.saas.domain.LicenseStatus;
import ru.soat.ohapp.saas.dto.admin.LicenseStatusRequest;
import ru.soat.ohapp.saas.dto.admin.LicenseUpsertRequest;
import ru.soat.ohapp.saas.dto.admin.LicenseView;
import ru.soat.ohapp.saas.service.admin.LicenseService;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class LicenseServiceImpl implements LicenseService {

    private final NamedParameterJdbcTemplate jdbc;

    private static final RowMapper<LicenseView> M = new RowMapper<>() {
        @Override public LicenseView mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new LicenseView(
                    (UUID) rs.getObject("id"),
                    (UUID) rs.getObject("tenant_id"),
                    ru.soat.ohapp.saas.domain.LicensePlan.valueOf(rs.getString("plan")),
                    LicenseStatus.valueOf(rs.getString("status")),
                    rs.getInt("seats"),
                    rs.getTimestamp("starts_at").toInstant(),
                    rs.getTimestamp("expires_at").toInstant(),
                    rs.getTimestamp("suspended_until") == null ? null : rs.getTimestamp("suspended_until").toInstant()
            );
        }
    };

    @Override
    public LicenseView upsert(LicenseUpsertRequest req) {
        UUID id = UUID.randomUUID();
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("tenantId", req.tenantId())
                .addValue("plan", req.plan().name())
                .addValue("status", LicenseStatus.PENDING.name())
                .addValue("seats", req.seats())
                .addValue("startsAt", req.startsAt())
                .addValue("expiresAt", req.expiresAt());

        // Postgres: RETURNING *
        return jdbc.queryForObject("""
            insert into licenses(id, tenant_id, plan, status, seats, starts_at, expires_at, created_at, updated_at)
            values (:id, :tenantId, :plan, :status, :seats, :startsAt, :expiresAt, now(), now())
            returning id, tenant_id, plan, status, seats, starts_at, expires_at, suspended_until
        """, p, M);
    }

    @Override
    public void changeStatus(UUID licenseId, LicenseStatusRequest req) {
        switch (req.action().toUpperCase()) {
            case "SUSPEND" -> jdbc.update("""
                update licenses
                   set status = :st,
                       suspended_until = :until,
                       updated_at = now()
                 where id = :id
            """, new MapSqlParameterSource()
                    .addValue("id", licenseId)
                    .addValue("st", LicenseStatus.SUSPENDED.name())
                    .addValue("until", req.until())
            );
            case "RESUME" -> jdbc.update("""
                update licenses
                   set status = case 
                                  when now() >= expires_at then :expired
                                  else :active 
                                end,
                       suspended_until = null,
                       updated_at = now()
                 where id = :id
            """, new MapSqlParameterSource()
                    .addValue("id", licenseId)
                    .addValue("active", LicenseStatus.ACTIVE.name())
                    .addValue("expired", LicenseStatus.EXPIRED.name())
            );
            default -> throw new IllegalArgumentException("Unknown action: " + req.action());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LicenseView> listByTenant(UUID tenantId) {
        return jdbc.query("""
            select id, tenant_id, plan, status, seats, starts_at, expires_at, suspended_until
              from licenses
             where tenant_id = :tenantId
             order by starts_at desc
        """, new MapSqlParameterSource().addValue("tenantId", tenantId), M);
    }

    @Override
    @Transactional(readOnly = true)
    public LicenseView getActive(UUID tenantId) {
        try {
            return jdbc.queryForObject("""
                select id, tenant_id, plan, status, seats, starts_at, expires_at, suspended_until
                  from licenses
                 where tenant_id = :tenantId and status = 'ACTIVE'
                 order by expires_at desc
                 limit 1
            """, new MapSqlParameterSource().addValue("tenantId", tenantId), M);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public void autoExpireAndActivate() {
        // 1) ACTIVE -> EXPIRED если срок прошёл
        jdbc.update("""
            update licenses
               set status = 'EXPIRED', updated_at = now()
             where status = 'ACTIVE' and expires_at < now()
        """, new MapSqlParameterSource());

        // 2) PENDING -> ACTIVE если старт наступил
        jdbc.update("""
            update licenses
               set status = 'ACTIVE', updated_at = now()
             where status = 'PENDING' and starts_at <= now()
        """, new MapSqlParameterSource());

        // 3) SUSPENDED -> ACTIVE, если приостановка закончилась и лицензия не просрочена
        jdbc.update("""
            update licenses
               set status = 'ACTIVE', suspended_until = null, updated_at = now()
             where status = 'SUSPENDED'
               and suspended_until is not null
               and suspended_until <= now()
               and expires_at > now()
        """, new MapSqlParameterSource());

        // 4) SUSPENDED -> EXPIRED, если срок действия уже прошёл
        jdbc.update("""
            update licenses
               set status = 'EXPIRED', updated_at = now()
             where status = 'SUSPENDED'
               and expires_at < now()
        """, new MapSqlParameterSource());
    }
}
