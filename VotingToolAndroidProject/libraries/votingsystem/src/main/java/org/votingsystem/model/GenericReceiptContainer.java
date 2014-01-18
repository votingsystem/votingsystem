package org.votingsystem.model;

import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import java.io.ByteArrayInputStream;
import java.util.Date;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class GenericReceiptContainer extends ReceiptContainer {

    private static final long serialVersionUID = 1L;

    private Long localId = -1L;
    private transient SMIMEMessageWrapper receipt;
    private byte[] receiptBytes;
    private TypeVS typeVS;
    private String subject;
    private String url;


    public GenericReceiptContainer(TypeVS typeVS, String url) {
        this.typeVS = typeVS;
        this.url = url;
    }

    public String getURL() {
        return url;
    }

    public void setReceiptBytes(byte[] receiptBytes) throws Exception {
        this.receiptBytes = receiptBytes;
        receipt = new SMIMEMessageWrapper(null, new ByteArrayInputStream(receiptBytes), null);
        subject = receipt.getSubject();
    }

    public SMIMEMessageWrapper getReceipt() {
        if(receipt == null && receiptBytes != null) {
            try {
                receipt = new SMIMEMessageWrapper(
                        null, new ByteArrayInputStream(receiptBytes), null);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return receipt;
    }

    @Override public String getSubject() {
        return subject;
    }

    @Override public TypeVS getType() {
        return typeVS;
    }

    @Override public Date getValidFrom() {
        return getReceipt().getSigner().getCertificate().getNotBefore();
    }

    @Override public Date getValidTo() {
        return getReceipt().getSigner().getCertificate().getNotAfter();
    }

    @Override public Long getLocalId() {
        return localId;
    }

    @Override public void setLocalId(Long localId) {
        this.localId = localId;
    }

}
