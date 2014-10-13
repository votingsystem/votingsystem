package org.votingsystem.vicket.model;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.votingsystem.callable.MessageTimeStamper;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.util.ExceptionVS;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.cert.TrustAnchor;
import java.util.*;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VicketTransactionBatch {

    private static Logger log = Logger.getLogger(VicketTransactionBatch.class);

    List<Vicket> vicketList;

    public VicketTransactionBatch() {}

    public VicketTransactionBatch(List<Vicket> vicketList) {
        this.vicketList = vicketList;
    }

    public void addVicket(Vicket vicket) {
        if(vicketList == null) vicketList = new ArrayList<Vicket>();
        vicketList.add(vicket);
    }

    public void addVicket(File vicketFile) throws IOException {
        log.debug("addVicket - file name: " + vicketFile.getName());
        Vicket vicket = (Vicket) ObjectUtils.deSerializeObject(FileUtils.getBytesFromFile(vicketFile));
        vicket.setFile(vicketFile);
        addVicket(vicket);
    }

    public VicketTransactionBatch(String vicketsArrayStr) throws Exception {
        JSONArray vicketsArray = (JSONArray) JSONSerializer.toJSON(vicketsArrayStr);
        vicketList = new ArrayList<Vicket>();
        for(int i = 0; i < vicketsArray.size(); i++) {
            SMIMEMessage smimeMessage = new SMIMEMessage(new ByteArrayInputStream(
                    Base64.decode(vicketsArray.getString(i).getBytes())));
            try {
                vicketList.add(new Vicket(smimeMessage));
            } catch(Exception ex) {
                throw new ExceptionVS("Error on vicket number: '" + i + "' - " + ex.getMessage(), ex);
            }
        }
    }

    public void initTransactionVSRequest(String toUserName, String toUserIBAN, String subject,
                 Boolean isTimeLimited, String timeStampServiceURL) throws Exception {
        for(Vicket vicket:vicketList) {
            JSONObject transactionRequest = vicket.getTransaction(toUserName, toUserIBAN, subject, isTimeLimited);
            SMIMEMessage smimeMessage = vicket.getCertificationRequest().genMimeMessage(vicket.getHashCertVS(),
                    StringUtils.getNormalized(vicket.getToUserName()),
                    transactionRequest.toString(), vicket.getSubject(), null);
            MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage, timeStampServiceURL);
            ResponseVS responseVS = timeStamper.call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
            vicket.setSMIMEMessage(timeStamper.getSmimeMessage());
        }
    }

    public JSONArray getTransactionVSRequest() throws Exception {
        List<String> vicketTransactionBatch = new ArrayList<String>();
        for(Vicket vicket:vicketList) {
            vicketTransactionBatch.add(java.util.Base64.getEncoder().encodeToString(vicket.getSMIMEMessage().getBytes()));
        }
        return (JSONArray) JSONSerializer.toJSON(vicketTransactionBatch);
    }

    public void validateTransactionVSResponse(String responseStr, Set<TrustAnchor> trustAnchor) throws Exception {
        JSONArray transactionBatchResponseJSON = JSONArray.fromObject(responseStr);
        Map<String, Vicket> vicketMap = getVicketMap();
        if(vicketMap.size() != transactionBatchResponseJSON.size()) throw new ExceptionVS("Num. vickets: '" +
                vicketMap.size() + "' - num. receipts: " + transactionBatchResponseJSON.size());
        for(int i = 0; i < transactionBatchResponseJSON.size(); i++) {
            JSONObject receiptData = (JSONObject) transactionBatchResponseJSON.get(i);
            String hashCertVS = (String) receiptData.keySet().iterator().next();
            SMIMEMessage smimeReceipt = new SMIMEMessage(new ByteArrayInputStream(
                    java.util.Base64.getDecoder().decode(receiptData.getString(hashCertVS).getBytes())));
            String signatureHashCertVS = CertUtils.getHashCertVS(smimeReceipt.getCertWithCertExtension(), ContextVS.VICKET_OID);
            Vicket vicket = vicketMap.remove(signatureHashCertVS);
            vicket.validateReceipt(smimeReceipt, trustAnchor);
        }
        if(vicketMap.size() != 0) throw new ExceptionVS(vicketMap.size() + " Vicket transactions without receipt");
    }

    public Map<String, Vicket> getVicketMap() throws ExceptionVS {
        if(vicketList == null) throw new ExceptionVS("Empty vicketList");
        Map<String, Vicket> result = new HashMap<String, Vicket>();
        for(Vicket vicket:vicketList) {
            result.put(vicket.getHashCertVS(), vicket);
        }
        return result;
    }

    public List<Vicket> getVicketList() {
        return vicketList;
    }


    public void setVicketList(List<Vicket> vicketList) {
        this.vicketList = vicketList;
    }

}
