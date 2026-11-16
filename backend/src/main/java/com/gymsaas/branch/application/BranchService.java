package com.gymsaas.branch.application;

import com.gymsaas.audit.application.AuditService;
import com.gymsaas.branch.domain.Branch;
import com.gymsaas.branch.domain.BranchRepository;
import com.gymsaas.branch.domain.BranchSettings;
import com.gymsaas.branch.domain.BranchSettingsRepository;
import com.gymsaas.branch.domain.BranchStatus;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.shared.domain.BrDocuments;
import com.gymsaas.shared.domain.PageResult;
import com.gymsaas.shared.domain.UuidV7;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class BranchService {

    private final BranchRepository branchRepository;
    private final BranchSettingsRepository settingsRepository;
    private final AuditService auditService;

    public BranchService(BranchRepository branchRepository,
                         BranchSettingsRepository settingsRepository,
                         AuditService auditService) {
        this.branchRepository = branchRepository;
        this.settingsRepository = settingsRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResult<BranchDtos.BranchSummary> list(UUID tenantId, String status, String search,
                                                     int page, int size) {
        return toPage(branchRepository.search(tenantId, normalizeStatus(status), search,
                page, clampSize(size)));
    }

    @Transactional(readOnly = true)
    public PageResult<BranchDtos.BranchSummary> listForTenantAsPlatformAdmin(UUID tenantId,
                                                                             String status,
                                                                             String search,
                                                                             int page, int size) {
        TenantContext.requirePlatformAdmin();
        return list(tenantId, status, search, page, size);
    }

    @Transactional(readOnly = true)
    public BranchDtos.BranchDetail find(UUID tenantId, UUID branchId) {
        return BranchDtos.BranchDetail.from(requireBranch(tenantId, branchId));
    }

    @Transactional
    public BranchDtos.BranchDetail create(UUID tenantId, BranchDtos.CreateBranchRequest request) {
        String code = normalizeCode(request.code());
        if (branchRepository.existsByCode(tenantId, code, null)) {
            throw ApiException.conflict("BRANCH_CODE_EXISTS",
                    "Já existe uma unidade com este código nesta academia.");
        }

        Instant now = Instant.now();
        Branch branch = new Branch(UuidV7.generate(), tenantId, request.name().trim(), code,
                optionalDocument(request.document()), trimToNull(request.email()),
                BrDocuments.phone(request.phone()), BrDocuments.zipCode(request.zipCode()),
                trimToNull(request.street()), trimToNull(request.number()),
                trimToNull(request.complement()), trimToNull(request.neighborhood()),
                trimToNull(request.city()), upperToNull(request.state()), BranchStatus.ACTIVE.name(),
                now, now);
        Branch saved = branchRepository.save(branch);

        settingsRepository.save(new BranchSettings(UuidV7.generate(), tenantId, saved.id(),
                "America/Sao_Paulo", null, null, null, false, Map.of()));

        auditService.recordForTenant(tenantId, "BRANCH_CREATED", "branch", saved.id(), "MEDIUM",
                null, Map.of("name", saved.name(), "code", saved.code()), Map.of());

        return BranchDtos.BranchDetail.from(saved);
    }

    @Transactional
    public BranchDtos.BranchDetail update(UUID tenantId, UUID branchId,
                                          BranchDtos.UpdateBranchRequest request) {
        Branch current = requireBranch(tenantId, branchId);
        String code = normalizeCode(request.code());
        if (branchRepository.existsByCode(tenantId, code, branchId)) {
            throw ApiException.conflict("BRANCH_CODE_EXISTS",
                    "Já existe uma unidade com este código nesta academia.");
        }

        Branch updated = new Branch(current.id(), tenantId, request.name().trim(), code,
                optionalDocument(request.document()), trimToNull(request.email()),
                BrDocuments.phone(request.phone()), BrDocuments.zipCode(request.zipCode()),
                trimToNull(request.street()), trimToNull(request.number()),
                trimToNull(request.complement()), trimToNull(request.neighborhood()),
                trimToNull(request.city()), upperToNull(request.state()), current.status(),
                current.createdAt(), current.updatedAt());
        Branch saved = branchRepository.save(updated);

        auditService.recordChange("BRANCH_UPDATED", "branch", saved.id(), "MEDIUM",
                Map.of("name", current.name(), "code", current.code()),
                Map.of("name", saved.name(), "code", saved.code()));

        return BranchDtos.BranchDetail.from(saved);
    }

    @Transactional
    public BranchDtos.BranchDetail changeStatus(UUID tenantId, UUID branchId,
                                                BranchDtos.ChangeBranchStatusRequest request) {
        Branch current = requireBranch(tenantId, branchId);
        if (!BranchStatus.valid(request.status())) {
            throw ApiException.badRequest("INVALID_STATUS", "Status inválido.");
        }
        String status = request.status().trim().toUpperCase();

        Branch updated = new Branch(current.id(), tenantId, current.name(), current.code(),
                current.document(), current.email(), current.phone(), current.zipCode(),
                current.street(), current.number(), current.complement(), current.neighborhood(),
                current.city(), current.state(), status, current.createdAt(), current.updatedAt());
        Branch saved = branchRepository.save(updated);

        auditService.recordChange("BRANCH_STATUS_CHANGED", "branch", saved.id(), "MEDIUM",
                Map.of("status", current.status()), Map.of("status", saved.status()));

        return BranchDtos.BranchDetail.from(saved);
    }

    @Transactional(readOnly = true)
    public BranchDtos.BranchSettingsView settings(UUID tenantId, UUID branchId) {
        requireBranch(tenantId, branchId);
        return settingsRepository.find(tenantId, branchId)
                .map(BranchDtos.BranchSettingsView::from)
                .orElseThrow(() -> ApiException.notFound("BRANCH_SETTINGS_NOT_FOUND",
                        "Configurações da unidade não encontradas."));
    }

    @Transactional
    public BranchDtos.BranchSettingsView updateSettings(UUID tenantId, UUID branchId,
                                                        BranchDtos.UpdateBranchSettingsRequest request) {
        requireBranch(tenantId, branchId);
        BranchSettings current = settingsRepository.find(tenantId, branchId)
                .orElseGet(() -> new BranchSettings(UuidV7.generate(), tenantId, branchId,
                        "America/Sao_Paulo", null, null, null, false, Map.of()));

        BranchSettings updated = new BranchSettings(current.id(), tenantId, branchId,
                blankOrDefault(request.timezone(), current.timezone()),
                request.openingTime() == null ? current.openingTime() : request.openingTime(),
                request.closingTime() == null ? current.closingTime() : request.closingTime(),
                request.maxCapacity() == null ? current.maxCapacity() : request.maxCapacity(),
                request.allow24hAccess() == null ? current.allow24hAccess() : request.allow24hAccess(),
                request.settings() == null ? current.settings() : new LinkedHashMap<>(request.settings()));
        BranchSettings saved = settingsRepository.save(updated);

        auditService.recordChange("BRANCH_SETTINGS_UPDATED", "branch", branchId, "MEDIUM",
                Map.of("timezone", String.valueOf(current.timezone())),
                Map.of("timezone", String.valueOf(saved.timezone())));

        return BranchDtos.BranchSettingsView.from(saved);
    }

    private Branch requireBranch(UUID tenantId, UUID branchId) {
        return branchRepository.findById(tenantId, branchId)
                .orElseThrow(() -> ApiException.notFound("BRANCH_NOT_FOUND", "Unidade não encontrada."));
    }

    private String normalizeCode(String value) {
        String code = BrDocuments.branchCode(value);
        if (code == null || code.length() < 2) {
            throw ApiException.badRequest("INVALID_BRANCH_CODE",
                    "Código da unidade deve ter ao menos 2 caracteres.");
        }
        return code;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        if (!BranchStatus.valid(status)) {
            throw ApiException.badRequest("INVALID_STATUS", "Status inválido.");
        }
        return status.trim().toUpperCase();
    }

    private String optionalDocument(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return BrDocuments.document(value);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String upperToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase();
    }

    private String blankOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private int clampSize(int size) {
        return Math.max(1, Math.min(size, 100));
    }

    private PageResult<BranchDtos.BranchSummary> toPage(PageResult<Branch> page) {
        return PageResult.of(page.content().stream().map(BranchDtos.BranchSummary::from).toList(),
                page.totalElements(), page.page(), page.size());
    }
}
