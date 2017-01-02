package org.votingsystem.crypto;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public enum SignedDocumentType {

    //Admin documents
    ADMIN_CHECK_USER,
    ADMIN_BATCH_RECEIPT,


    SIGNED_DOCUMENT,

    ENTITY_METADATA,
    NEW_ELECTION_REQUEST,
    ANON_VOTE_CERT_REQUEST_REPEATED,
    ANON_VOTE_CERT_REQUEST,
    VOTE_REPEATED,
    VOTE;
}
