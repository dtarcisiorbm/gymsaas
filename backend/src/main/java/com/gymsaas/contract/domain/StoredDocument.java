package com.gymsaas.contract.domain;

public record StoredDocument(String storageKey, String sha256, long sizeBytes, String contentType) {
}
