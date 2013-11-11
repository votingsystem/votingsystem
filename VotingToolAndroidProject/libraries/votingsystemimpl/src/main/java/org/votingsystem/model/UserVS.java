package org.votingsystem.model;

import java.security.cert.X509Certificate;
import org.bouncycastle2.cms.SignerInformation;

import org.bouncycastle.tsp.TimeStampToken;

public interface UserVS {

	public void setCertificate(X509Certificate certificate);
	public X509Certificate getCertificate();
	public void setCertificateCA(CertificateVS certificate);
	public void setPais(String country);
	public void setNif(String nif);
	public String getNif();
	public void setPrimerApellido(String surname);
	public void setNombre(String firstName);
	public String getNombre();
	public void setCn(String cn);
	public void setEmail(String email);
	public String getEmail();
	public void setTelefono(String phone);
	public void setTimeStampToken(TimeStampToken timeStampToken);
	public TimeStampToken getTimeStampToken();
	public void setSignerInformation(SignerInformation signer);
	 
}