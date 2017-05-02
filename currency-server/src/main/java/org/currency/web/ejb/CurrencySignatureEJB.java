package org.currency.web.ejb;

import eu.europa.esig.dss.DSSDocument;
import eu.europa.esig.dss.InMemoryDocument;
import org.votingsystem.crypto.SignatureParams;
import org.votingsystem.crypto.SignedDocumentType;
import org.votingsystem.ejb.SignatureServiceEJB;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.throwable.DuplicatedDbItemException;
import org.votingsystem.throwable.SignatureException;
import org.votingsystem.throwable.XAdESValidationException;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.inject.Inject;
import java.util.logging.Logger;

import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class CurrencySignatureEJB {

    private static final Logger log = Logger.getLogger(CurrencySignatureEJB.class.getName());

    @Inject private SignatureServiceEJB signatureService;

    @TransactionAttribute(REQUIRES_NEW)
    public SignedDocument addReceipt(SignedDocumentType signedDocumentType, SignedDocument signedDocument) throws SignatureException {
        SignatureParams signatureParams = new SignatureParams(null, User.Type.CURRENCY_SERVER,
                signedDocumentType).setWithTimeStampValidation(false);
        SignedDocument receipt = signatureService.signXAdESAndSave(signedDocument.getBody().getBytes(), signatureParams);
        signedDocument.setReceipt(receipt);
        return signedDocument;
    }

    @TransactionAttribute(REQUIRES_NEW)
    public SignedDocument validateXAdESAndSave(byte[] signedXML) throws XAdESValidationException, DuplicatedDbItemException {
        SignatureParams signatureParams = new SignatureParams(null, User.Type.ID_CARD_USER,
                SignedDocumentType.SIGNED_DOCUMENT).setWithTimeStampValidation(true);
        return signatureService.validateXAdESAndSave(new InMemoryDocument(signedXML), signatureParams);
    }

    @TransactionAttribute(REQUIRES_NEW)
    public SignedDocument validateXAdESAndSave(DSSDocument signedXML, SignatureParams signatureParams)
            throws XAdESValidationException, DuplicatedDbItemException {
        return signatureService.validateXAdESAndSave(signedXML, signatureParams);
    }

    @TransactionAttribute(REQUIRES_NEW)
    public SignedDocument validateCurrency(byte[] signedXML) throws XAdESValidationException, DuplicatedDbItemException {
        SignatureParams signatureParams = new SignatureParams(null, User.Type.ANON_CURRENCY,
                SignedDocumentType.CURRENCY_CHANGE).setWithTimeStampValidation(true);
        return signatureService.validateXAdESAndSave(new InMemoryDocument(signedXML), signatureParams);
    }

}
