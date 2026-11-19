package com.gymsaas.billing.infrastructure.persistence;

import com.gymsaas.billing.domain.Payment;
import com.gymsaas.billing.domain.PaymentMethod;
import com.gymsaas.billing.domain.PaymentRepository;
import com.gymsaas.billing.domain.PaymentStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.gymsaas.billing.infrastructure.persistence.JdbcChargeRepository.instant;
import static com.gymsaas.billing.infrastructure.persistence.JdbcChargeRepository.uuid;

@Repository
public class JdbcPaymentRepository implements PaymentRepository {

    private static final String COLUMNS = """
            id, tenant_id, payment_number, charge_id, student_id, branch_id, amount, currency, method, paid_at,
            reference, notes, status, received_by, refunded_at, refunded_by, refund_reason, created_at
            """;

    private static final RowMapper<Payment> MAPPER = JdbcPaymentRepository::map;

    private final JdbcTemplate jdbc;

    public JdbcPaymentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Payment> findById(UUID tenantId, UUID paymentId) {
        return jdbc.query("select " + COLUMNS + " from payments where tenant_id = ? and id = ?",
                MAPPER, tenantId, paymentId).stream().findFirst();
    }

    @Override
    public List<Payment> findByCharge(UUID tenantId, UUID chargeId) {
        return jdbc.query("select " + COLUMNS + " from payments where tenant_id = ? and charge_id = ? order by paid_at",
                MAPPER, tenantId, chargeId);
    }

    @Override
    public Payment insert(Payment payment) {
        jdbc.update("""
                insert into payments (id, tenant_id, payment_number, charge_id, student_id, branch_id, amount,
                    currency, method, paid_at, reference, notes, status, received_by, created_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, payment.id(), payment.tenantId(), payment.paymentNumber(), payment.chargeId(),
                payment.studentId(), payment.branchId(), payment.amount(), payment.currency(),
                payment.method().name(), Timestamp.from(payment.paidAt()), payment.reference(), payment.notes(),
                payment.status().name(), payment.receivedBy(), Timestamp.from(payment.createdAt()));
        return findById(payment.tenantId(), payment.id()).orElseThrow();
    }

    @Override
    public Payment markRefunded(Payment payment) {
        int updated = jdbc.update("""
                update payments set status = 'REFUNDED', refunded_at = ?, refunded_by = ?, refund_reason = ?
                where tenant_id = ? and id = ? and status = 'CONFIRMED'
                """, Timestamp.from(payment.refundedAt()), payment.refundedBy(), payment.refundReason(),
                payment.tenantId(), payment.id());
        if (updated != 1) {
            throw new IllegalStateException("Payment not refundable.");
        }
        return findById(payment.tenantId(), payment.id()).orElseThrow();
    }

    private static Payment map(ResultSet rs, int row) throws SQLException {
        return new Payment(uuid(rs, "id"), uuid(rs, "tenant_id"), rs.getString("payment_number"),
                uuid(rs, "charge_id"), uuid(rs, "student_id"), uuid(rs, "branch_id"), rs.getBigDecimal("amount"),
                rs.getString("currency").trim(), PaymentMethod.valueOf(rs.getString("method")),
                instant(rs, "paid_at"), rs.getString("reference"), rs.getString("notes"),
                PaymentStatus.valueOf(rs.getString("status")), uuid(rs, "received_by"), instant(rs, "refunded_at"),
                uuid(rs, "refunded_by"), rs.getString("refund_reason"), instant(rs, "created_at"));
    }
}
