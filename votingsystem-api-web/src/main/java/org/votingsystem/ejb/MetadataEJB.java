package org.votingsystem.ejb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import eu.europa.esig.dss.DSSDocument;
import eu.europa.esig.dss.InMemoryDocument;
import org.votingsystem.crypto.SignatureParams;
import org.votingsystem.crypto.SignedDocumentType;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.metadata.KeyDto;
import org.votingsystem.dto.metadata.MetadataDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.model.User;
import org.votingsystem.throwable.HttpRequestException;
import org.votingsystem.throwable.SignatureException;
import org.votingsystem.throwable.XMLValidationException;
import org.votingsystem.util.Messages;
import org.votingsystem.xml.XML;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class MetadataEJB implements MetadataService {

    private static final Logger log = Logger.getLogger(MetadataEJB.class.getName());

    @Inject private SignatureService signatureService;
    @Inject private Config config;
    @Inject private SignerInfoService signerInfoService;


    public byte[] getMetadataSigned() throws JsonProcessingException, SignatureException {
        MetadataDto metadata = config.getMetadata();
        byte[] metadataBytes = XML.getMapper().writeValueAsBytes(metadata);
        return signatureService.signXAdES(metadataBytes);
    }

    @Override
    public MetadataDto getMetadataFromURL(String metadataURL, boolean validateSignature, boolean withTimeStampValidation)
            throws XMLValidationException, HttpRequestException {
        MetadataDto metadata = null;
        ResponseDto response = HttpConn.getInstance().doGetRequest(metadataURL, null);
        if(ResponseDto.SC_OK != response.getStatusCode())
            throw new HttpRequestException("Error fetching metadata from: " + metadataURL);
        try {
            metadata = new XmlMapper().readValue(response.getMessageBytes(), MetadataDto.class);
            if(!validateSignature)
                return metadata;
            //First we check if the trusted entity sign its messages with a certificate issued by one trusted CA
            for(KeyDto keyDto:metadata.getKeyDescriptorSet()) {
                if(KeyDto.Use.SIGN == keyDto.getUse())
                    signerInfoService.checkSigner(keyDto.getX509Certificate(), User.Type.ENTITY, metadata.getEntity().getId());
            }

            DSSDocument signedDocument = new InMemoryDocument(response.getMessageBytes());
            SignatureParams signatureParams = new SignatureParams(metadata.getEntity().getId(), User.Type.ENTITY,
                    SignedDocumentType.ENTITY_METADATA).setWithTimeStampValidation(withTimeStampValidation);
            signatureService.validateXAdESAndSave(signedDocument, signatureParams);
            config.putEntityMetadata(metadata);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            throw new XMLValidationException(Messages.currentInstance().get("invalidMetadataMsg"));
        }
        return metadata;
    }
}
