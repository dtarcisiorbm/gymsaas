package com.gymsaas.contract.application;

import com.gymsaas.branch.domain.Branch;
import com.gymsaas.membership.domain.ContractedConditions;
import com.gymsaas.membership.domain.Membership;
import com.gymsaas.plan.domain.BillingPeriod;
import com.gymsaas.plan.domain.DurationType;
import com.gymsaas.plan.domain.FreezePolicy;
import com.gymsaas.student.domain.Student;
import com.gymsaas.tenant.domain.Tenant;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Renders the contract text from the membership snapshot. The system only
 * states the contracted facts; legal clauses come exclusively from the
 * academy (plan contract terms captured at contracting time). No clause is
 * invented here.
 */
@Component
public class ContractContentFactory {

    public static final String TEMPLATE_VERSION = "membership-v1";
    public static final String LEGAL_NOTICE = "Registro de aceite simples. Não constitui assinatura digital "
            + "qualificada (ICP-Brasil) nem certificado digital.";
    public static final String ACCEPTANCE_DECLARATION = "O(a) aluno(a) declarou, presencialmente, ter lido e aceito "
            + "o conteúdo desta versão do contrato, cujo hash SHA-256 foi registrado. " + LEGAL_NOTICE;

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String[] WEEK_DAYS = {"Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"};

    public record ContractData(int version, Instant generatedAt, ZoneId zone, Tenant tenant, Branch branch,
                               Student student, String maskedCpf, Membership membership,
                               Map<UUID, String> branchNames) {
    }

    public String render(ContractData data) {
        Membership membership = data.membership();
        ContractedConditions conditions = membership.conditions();
        Map<String, Object> snapshot = conditions.snapshot();
        StringBuilder text = new StringBuilder();

        text.append("CONTRATO DE MATRÍCULA Nº ").append(membership.registrationNumber())
                .append(" — VERSÃO ").append(data.version()).append('\n');
        text.append("Modelo: ").append(TEMPLATE_VERSION).append('\n');
        text.append("Gerado em: ").append(DATE_TIME.format(data.generatedAt().atZone(data.zone())))
                .append(" (").append(data.zone().getId()).append(")\n\n");

        text.append("1. PARTES\n");
        Tenant tenant = data.tenant();
        text.append("Academia: ").append(nonBlank(tenant.legalName(), tenant.name()))
                .append(" (").append(tenant.name()).append(")");
        if (tenant.document() != null) {
            text.append(" — Documento: ").append(tenant.document());
        }
        text.append('\n');
        text.append("Unidade: ").append(data.branch() == null ? "—" : data.branch().name()).append('\n');
        text.append("Aluno(a): ").append(data.student().name())
                .append(" — Matrícula do aluno: ").append(data.student().registrationNumber())
                .append(" — CPF: ").append(data.maskedCpf() == null ? "não informado" : data.maskedCpf())
                .append("\n\n");

        text.append("2. PLANO CONTRATADO\n");
        text.append("Plano: ").append(snapshot.getOrDefault("planName", "—"))
                .append(" (").append(snapshot.getOrDefault("planCode", "—")).append(")\n");
        text.append("Duração: ").append(duration(conditions.durationType(), conditions.durationValue())).append('\n');
        text.append("Vigência: ").append(date(membership.startDate())).append(" a ")
                .append(date(membership.originalEndDate())).append('\n');
        text.append("Benefícios:\n");
        List<String> features = features(snapshot.get("features"), data.branchNames());
        if (features.isEmpty()) {
            text.append("  - Nenhum benefício cadastrado no plano.\n");
        } else {
            features.forEach(feature -> text.append("  - ").append(feature).append('\n'));
        }
        text.append('\n');

        text.append("3. CONDIÇÕES COMERCIAIS\n");
        String currency = conditions.currency();
        text.append("Valor por ciclo de cobrança: ").append(money(conditions.amount(), currency)).append('\n');
        text.append("Periodicidade de cobrança: ")
                .append(billing(conditions.billingPeriod(), conditions.billingInterval())).append('\n');
        text.append("Taxa de adesão: ").append(money(conditions.setupFee(), currency)).append('\n');
        text.append("Desconto: ").append(money(conditions.discountAmount(), currency));
        if (conditions.discountReason() != null) {
            text.append(" (").append(conditions.discountReason()).append(")");
        }
        text.append('\n');
        text.append("Valor final por ciclo: ").append(money(conditions.finalAmount(), currency)).append('\n');
        text.append("Moeda: ").append(currency).append("\n\n");

        text.append("4. CONGELAMENTO\n");
        freezeLines(conditions.freezePolicy()).forEach(line -> text.append(line).append('\n'));
        text.append('\n');

        text.append("5. CLÁUSULAS DA ACADEMIA\n");
        Object terms = snapshot.get("contractTerms");
        text.append(terms instanceof String value && !value.isBlank()
                ? value.strip() : "Nenhuma cláusula adicional cadastrada no plano.").append("\n\n");

        text.append("6. ACEITE\n");
        text.append("Este documento registra as condições contratadas na data da contratação. ")
                .append(LEGAL_NOTICE).append('\n');
        return text.toString();
    }

    static String money(BigDecimal value, String currency) {
        NumberFormat format = NumberFormat.getCurrencyInstance(PT_BR);
        format.setCurrency(Currency.getInstance(currency));
        return format.format(value);
    }

    private static String date(LocalDate value) {
        return value == null ? "—" : DATE.format(value);
    }

    private static String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    static String duration(DurationType type, int value) {
        return switch (type) {
            case MONTHLY -> "Mensal (1 mês)";
            case QUARTERLY -> "Trimestral (3 meses)";
            case SEMIANNUAL -> "Semestral (6 meses)";
            case ANNUAL -> "Anual (12 meses)";
            case CUSTOM_MONTHS -> value + (value == 1 ? " mês" : " meses");
            case CUSTOM_DAYS -> value + (value == 1 ? " dia" : " dias");
        };
    }

    static String billing(BillingPeriod period, int interval) {
        String label = switch (period) {
            case MONTHLY -> "Mensal";
            case QUARTERLY -> "Trimestral";
            case SEMIANNUAL -> "Semestral";
            case ANNUAL -> "Anual";
            case ONE_TIME -> "Pagamento único";
        };
        return interval > 1 ? label + " (a cada " + interval + " ciclos)" : label;
    }

    private static List<String> freezeLines(FreezePolicy policy) {
        List<String> lines = new ArrayList<>();
        if (policy == null || (policy.maxFreezeDays() == null && policy.maxFreezes() == null
                && policy.minFreezeDays() == null)) {
            lines.add("Sem limites definidos no plano. Cada congelamento prorroga a vigência pelos dias congelados.");
            return lines;
        }
        if (policy.maxFreezes() != null && policy.maxFreezes() == 0) {
            lines.add("Congelamento não permitido neste plano.");
            return lines;
        }
        if (policy.maxFreezes() != null) {
            lines.add("Máximo de congelamentos: " + policy.maxFreezes());
        }
        if (policy.maxFreezeDays() != null) {
            lines.add("Máximo de dias congelados no contrato: " + policy.maxFreezeDays());
        }
        if (policy.minFreezeDays() != null) {
            lines.add("Mínimo de dias por congelamento: " + policy.minFreezeDays());
        }
        lines.add("Cada congelamento prorroga a vigência pelos dias efetivamente congelados.");
        return lines;
    }

    private static List<String> features(Object raw, Map<UUID, String> branchNames) {
        List<String> lines = new ArrayList<>();
        if (!(raw instanceof List<?> items)) {
            return lines;
        }
        for (Object item : items) {
            if (item instanceof Map<?, ?> feature) {
                lines.add(feature(feature, branchNames));
            }
        }
        return lines;
    }

    private static String feature(Map<?, ?> feature, Map<UUID, String> branchNames) {
        String type = String.valueOf(feature.get("type"));
        Object intValue = feature.get("intValue");
        return switch (type) {
            case "ALL_BRANCHES_ACCESS" -> "Acesso a todas as unidades";
            case "BRANCH_ACCESS" -> "Acesso à unidade " + branchName(feature.get("branchId"), branchNames);
            case "TIME_WINDOW" -> "Horário de acesso " + feature.get("startTime") + "–" + feature.get("endTime")
                    + " (" + weekDays(feature.get("daysOfWeek")) + ")";
            case "DAILY_CHECKIN_LIMIT" -> "Até " + intValue + " acesso(s) por dia";
            case "WEEKLY_CHECKIN_LIMIT" -> "Até " + intValue + " acesso(s) por semana";
            case "GUEST_PASSES_PER_MONTH" -> intValue + " convidado(s) por mês";
            default -> {
                Object label = feature.get("label");
                Object description = feature.get("description");
                yield description == null ? String.valueOf(label) : label + " — " + description;
            }
        };
    }

    private static String branchName(Object id, Map<UUID, String> branchNames) {
        try {
            String name = branchNames.get(UUID.fromString(String.valueOf(id)));
            return name == null ? "(unidade não encontrada)" : name;
        } catch (IllegalArgumentException exception) {
            return "(unidade não encontrada)";
        }
    }

    static String weekDays(Object mask) {
        if (!(mask instanceof Number number)) {
            return "—";
        }
        int bits = number.intValue();
        List<String> days = new ArrayList<>();
        for (int index = 0; index < WEEK_DAYS.length; index++) {
            if ((bits & (1 << index)) != 0) {
                days.add(WEEK_DAYS[index]);
            }
        }
        return String.join(", ", days);
    }
}
