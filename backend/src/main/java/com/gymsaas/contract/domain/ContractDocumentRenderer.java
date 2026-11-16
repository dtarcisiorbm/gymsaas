package com.gymsaas.contract.domain;

/** Port that renders the contract text into a document (PDF). */
public interface ContractDocumentRenderer {

    String contentType();

    byte[] render(String title, String content);
}
