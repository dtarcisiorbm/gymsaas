package com.gymsaas.branch.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "branch_settings")
public class BranchSettingsJpaEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    private String timezone;

    @Column(name = "opening_time")
    private LocalTime openingTime;

    @Column(name = "closing_time")
    private LocalTime closingTime;

    @Column(name = "max_capacity")
    private Integer maxCapacity;

    @Column(name = "allow_24h_access")
    private boolean allow24hAccess;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", nullable = false)
    private Map<String, Object> settings = new HashMap<>();

    public BranchSettingsJpaEntity() {
    }

    public BranchSettingsJpaEntity(UUID id, UUID tenantId, UUID branchId, String timezone,
                                   LocalTime openingTime, LocalTime closingTime, Integer maxCapacity,
                                   boolean allow24hAccess, Map<String, Object> settings) {
        this.id = id;
        this.tenantId = tenantId;
        this.branchId = branchId;
        this.timezone = timezone;
        this.openingTime = openingTime;
        this.closingTime = closingTime;
        this.maxCapacity = maxCapacity;
        this.allow24hAccess = allow24hAccess;
        this.settings = settings == null ? new HashMap<>() : new HashMap<>(settings);
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public String getTimezone() {
        return timezone;
    }

    public LocalTime getOpeningTime() {
        return openingTime;
    }

    public LocalTime getClosingTime() {
        return closingTime;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public boolean isAllow24hAccess() {
        return allow24hAccess;
    }

    public Map<String, Object> getSettings() {
        return settings;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public void setOpeningTime(LocalTime openingTime) {
        this.openingTime = openingTime;
    }

    public void setClosingTime(LocalTime closingTime) {
        this.closingTime = closingTime;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public void setAllow24hAccess(boolean allow24hAccess) {
        this.allow24hAccess = allow24hAccess;
    }

    public void setSettings(Map<String, Object> settings) {
        this.settings = settings == null ? new HashMap<>() : new HashMap<>(settings);
    }
}
