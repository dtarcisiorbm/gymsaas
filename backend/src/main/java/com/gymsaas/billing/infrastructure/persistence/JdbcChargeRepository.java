package com.gymsaas.billing.infrastructure.persistence;

import com.gymsaas.billing.domain.BillingTotals;
import com.gymsaas.billing.domain.Charge;
import com.gymsaas.billing.domain.ChargeKind;
import com.gymsaas.billing.domain.ChargeRepository;
import com.gymsaas.billing.domain.ChargeSearchCriteria;
import com.gymsaas.billing.domain.ChargeStatus;
import com.gymsaas.billing.domain.PaymentMethod;
import com.gymsaas.shared.domain.PageResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Every statement filters by tenant explicitly (defense in depth on top of RLS).
 * Sort columns come from a fixed whitelist, never from the request verbatim.
 */
@Repository
public class JdbcChargeRepository implements ChargeRepository {

    private static final Map<String, String> SORTS = Map.of(
            "dueDate", "c.due_date",
            "createdAt", "c.created_at",
            "amount", "c.amount",
            "chargeNumber", "c.charge_number",
            "status", "c.status");

    private static final String COLUMNS = """
            c.id, c.tenant_id, c.charge_number, c.student_id, c.membership_id, c.branch_id, c.kind,
            c.installment_number, c.installment_count, c.description, c.amount, c.currency, c.due_date,
            c.period_start, c.period_end, c.status, c.paid_at, c.cancelled_at, c.cancelled_by,
            c.cancellation_reason, c.notes, c.created_by, c.created_at, c.updated_at
            """;

    private static final RowMapper<Charge> MAPPER = JdbcChargeRepository::map;

    private final JdbcTemplate jdbc;

    public JdbcChargeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Charge> findById(UUID tenantId, UUID chargeId) {
        return jdbc.query("select " + COLUMNS + " from charges c where c.tenant_id = ? and c.id = ?",
                MAPPER, tenantId, chargeId).stream().findFirst();
    }

    @Override
    public Optional<Charge> lockById(UUID tenantId, UUID chargeId) {
        return jdbc.query("select " + COLUMNS + " from charges c where c.tenant_id = ? and c.id = ? for update",
                MAPPER, tenantId, chargeId).stream().findFirst();
    }

    @Override
    public List<Charge> findByMembership(UUID tenantId, UUID membershipId) {
        return jdbc.query("select " + COLUMNS + """
                 from charges c where c.tenant_id = ? and c.membership_id = ?
                order by c.due_date, c.kind desc, c.installment_number
                """, MAPPER, tenantId, membershipId);
    }

    @Override
    public List<Charge> lockOpenByMembershipDueFrom(UUID tenantId, UUID membershipId, LocalDate date) {
        return jdbc.query("select " + COLUMNS + """
                 from charges c
                where c.tenant_id = ? and c.membership_id = ? and c.status = 'OPEN' and c.due_date >= ?
                order by c.due_date
                for update
                """, MAPPER, tenantId, membershipId, date);
    }

    @Override
    public PageResult<Charge> search(UUID tenantId, ChargeSearchCriteria criteria) {
        StringBuilder where = new StringBuilder(" from charges c where c.tenant_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(tenantId);
        if (criteria.status() != null) {
            where.append(" and c.status = ?");
            args.add(criteria.status().name());
        }
        if (criteria.overdueOnly()) {
            where.append(" and c.status = 'OPEN' and c.due_date < ?");
            args.add(criteria.today());
        }
        if (criteria.studentId() != null) {
            where.append(" and c.student_id = ?");
            args.add(criteria.studentId());
        }
        if (criteria.membershipId() != null) {
            where.append(" and c.membership_id = ?");
            args.add(criteria.membershipId());
        }
        if (criteria.branchId() != null) {
            where.append(" and c.branch_id = ?");
            args.add(criteria.branchId());
        }
        if (criteria.dueFrom() != null) {
            where.append(" and c.due_date >= ?");
            args.add(criteria.dueFrom());
        }
        if (criteria.dueTo() != null) {
            where.append(" and c.due_date <= ?");
            args.add(criteria.dueTo());
        }
        if (criteria.search() != null && !criteria.search().isBlank()) {
            String like = "%" + escapeLike(criteria.search().trim().toLowerCase()) + "%";
            where.append("""
                     and (lower(c.charge_number) like ? escape '\\' or lower(c.description) like ? escape '\\'
                          or exists (select 1 from students s where s.tenant_id = c.tenant_id and s.id = c.student_id
                                     and (lower(s.name) like ? escape '\\'
                                          or lower(s.registration_number) like ? escape '\\')))
                    """);
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }
        Long total = jdbc.queryForObject("select count(*)" + where, Long.class, args.toArray());
        String order = criteria.sort() == null ? "c.due_date" : SORTS.getOrDefault(criteria.sort(), "c.due_date");
        String direction = "desc".equalsIgnoreCase(criteria.direction()) ? "desc" : "asc";
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(criteria.size());
        pageArgs.add((long) criteria.page() * criteria.size());
        List<Charge> content = jdbc.query("select " + COLUMNS + where + " order by " + order + " " + direction
                + ", c.charge_number limit ? offset ?", MAPPER, pageArgs.toArray());
        return PageResult.of(content, total == null ? 0 : total, criteria.page(), criteria.size());
    }

    @Override
    public Charge insert(Charge charge) {
        jdbc.update("""
                insert into charges (id, tenant_id, charge_number, student_id, membership_id, branch_id, kind,
                    installment_number, installment_count, description, amount, currency, due_date, period_start,
                    period_end, status, notes, created_by, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, charge.id(), charge.tenantId(), charge.chargeNumber(), charge.studentId(), charge.membershipId(),
                charge.branchId(), charge.kind().name(), charge.installmentNumber(), charge.installmentCount(),
                charge.description(), charge.amount(), charge.currency(), charge.dueDate(), charge.periodStart(),
                charge.periodEnd(), charge.status().name(), charge.notes(), charge.createdBy(),
                Timestamp.from(charge.createdAt()), Timestamp.from(charge.updatedAt()));
        return findById(charge.tenantId(), charge.id()).orElseThrow();
    }

    @Override
    public Charge updateStatus(Charge charge) {
        int updated = jdbc.update("""
                update charges set status = ?, paid_at = ?, cancelled_at = ?, cancelled_by = ?,
                    cancellation_reason = ?, updated_at = ?
                where tenant_id = ? and id = ?
                """, charge.status().name(), timestamp(charge.paidAt()), timestamp(charge.cancelledAt()),
                charge.cancelledBy(), charge.cancellationReason(), Timestamp.from(charge.updatedAt()),
                charge.tenantId(), charge.id());
        if (updated != 1) {
            throw new IllegalStateException("Charge not found for update.");
        }
        return findById(charge.tenantId(), charge.id()).orElseThrow();
    }

    @Override
    public BillingTotals totals(UUID tenantId, UUID branchId, LocalDate from, LocalDate to, LocalDate today,
                                Instant receivedFrom, Instant receivedTo) {
        String branchFilter = branchId == null ? "" : " and branch_id = ?";
        BillingTotals.Amount receivable = amount("""
                select count(*), coalesce(sum(amount), 0) from charges
                where tenant_id = ? and status = 'OPEN' and due_date >= ? and due_date >= ? and due_date <= ?
                """ + branchFilter, withBranch(branchId, tenantId, from, today, to));
        BillingTotals.Amount overdue = amount("""
                select count(*), coalesce(sum(amount), 0) from charges
                where tenant_id = ? and status = 'OPEN' and due_date < ?
                """ + branchFilter, withBranch(branchId, tenantId, today));
        Timestamp start = Timestamp.from(receivedFrom);
        Timestamp end = Timestamp.from(receivedTo);
        Map<PaymentMethod, BillingTotals.Amount> byMethod = new EnumMap<>(PaymentMethod.class);
        jdbc.query("""
                select method, count(*), coalesce(sum(amount), 0) from payments
                where tenant_id = ? and status = 'CONFIRMED' and paid_at >= ? and paid_at < ?
                """ + branchFilter + " group by method", (ResultSet rs) -> {
            byMethod.put(PaymentMethod.valueOf(rs.getString(1)),
                    new BillingTotals.Amount(rs.getLong(2), rs.getBigDecimal(3)));
        }, withBranch(branchId, tenantId, start, end));
        long receivedCount = byMethod.values().stream().mapToLong(BillingTotals.Amount::count).sum();
        BigDecimal receivedTotal = byMethod.values().stream().map(BillingTotals.Amount::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BillingTotals.Amount refunded = amount("""
                select count(*), coalesce(sum(amount), 0) from payments
                where tenant_id = ? and status = 'REFUNDED' and refunded_at >= ? and refunded_at < ?
                """ + branchFilter, withBranch(branchId, tenantId, start, end));
        return new BillingTotals(receivable, overdue, new BillingTotals.Amount(receivedCount, receivedTotal),
                byMethod, refunded);
    }

    private BillingTotals.Amount amount(String sql, Object[] args) {
        return jdbc.queryForObject(sql, (rs, row) -> new BillingTotals.Amount(rs.getLong(1), rs.getBigDecimal(2)),
                args);
    }

    private static Object[] withBranch(UUID branchId, Object... args) {
        if (branchId == null) {
            return args;
        }
        Object[] result = java.util.Arrays.copyOf(args, args.length + 1);
        result[args.length] = branchId;
        return result;
    }

    static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    static Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    static UUID uuid(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class);
    }

    private static Charge map(ResultSet rs, int row) throws SQLException {
        return new Charge(uuid(rs, "id"), uuid(rs, "tenant_id"), rs.getString("charge_number"),
                uuid(rs, "student_id"), uuid(rs, "membership_id"), uuid(rs, "branch_id"),
                ChargeKind.valueOf(rs.getString("kind")), (Integer) rs.getObject("installment_number"),
                (Integer) rs.getObject("installment_count"), rs.getString("description"), rs.getBigDecimal("amount"),
                rs.getString("currency").trim(), rs.getObject("due_date", LocalDate.class),
                rs.getObject("period_start", LocalDate.class), rs.getObject("period_end", LocalDate.class),
                ChargeStatus.valueOf(rs.getString("status")), instant(rs, "paid_at"), instant(rs, "cancelled_at"),
                uuid(rs, "cancelled_by"), rs.getString("cancellation_reason"), rs.getString("notes"),
                uuid(rs, "created_by"), instant(rs, "created_at"), instant(rs, "updated_at"));
    }
}
