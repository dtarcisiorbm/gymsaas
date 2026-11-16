package com.gymsaas.contract.domain;

/**
 * Port for contract file storage (PDFs never go into the database). The
 * current adapter writes to the local file system / Docker volume; an object
 * storage (S3/MinIO) adapter can replace it without touching the domain.
 */
public interface ContractDocumentStorage {

    /** Stores a new document; existing keys are never overwritten. */
    StoredDocument store(String storageKey, byte[] content, String contentType);

    byte[] load(String storageKey);
}
