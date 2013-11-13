package org.votingsystem.accesscontrol.service

import org.bouncycastle.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.openssl.PEMReader;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import org.votingsystem.accesscontrol.model.*;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.*
import org.votingsystem.util.*

import java.security.KeyStore
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey
import java.security.PublicKey
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Locale;
import java.security.cert.X509Certificate;

class CsrService {
	
	def grailsApplication
	def subscripcionService
	def messageSource
	def filesService

	
	public synchronized ResponseVS firmarCertificadoVoto (byte[] csr, Evento evento,
		Usuario representative, Locale locale) {
		log.debug("firmarCertificadoVoto - evento: ${evento?.id}");
		ResponseVS respuesta = validarCSRVoto(csr, evento, locale)
		if(ResponseVS.SC_OK != respuesta.statusCode) return respuesta
		PublicKey requestPublicKey = (PublicKey)respuesta.data
		AlmacenClaves almacenClaves = evento.getAlmacenClaves()
		//TODO ==== vote keystore -- this is for developement
		KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(almacenClaves.bytes,
			grailsApplication.config.VotingSystem.signKeysPassword.toCharArray());
		PrivateKey privateKeySigner = (PrivateKey)keyStore.getKey(almacenClaves.keyAlias,
			grailsApplication.config.VotingSystem.signKeysPassword.toCharArray());
		X509Certificate certSigner = (X509Certificate) keyStore.getCertificate(almacenClaves.keyAlias);
		String representativeURL = null
		if(representative && representative.type == Usuario.Type.REPRESENTATIVE)
			representativeURL = "OU=RepresentativeURL:http://${grailsApplication.config.grails.serverURL}" +
				"/AccessControl/representative/${representative.id}"
		X509Certificate issuedCert = signCSR(csr, representativeURL, 
			privateKeySigner, certSigner, evento.fechaInicio, evento.fechaFin)
		if (!issuedCert) {
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.ERROR_VALIDANDO_CSR)
		} else {
			SolicitudCSRVoto solicitudCSR = new SolicitudCSRVoto(
				numeroSerie:issuedCert.getSerialNumber().longValue(),
				contenido:csr, eventoVotacion:evento,
				estado:SolicitudCSRVoto.Estado.OK,
				hashCertificadoVotoBase64:respuesta.hashCertificadoVotoBase64)
			solicitudCSR.save()
			Certificado certificado = new Certificado(numeroSerie:issuedCert.getSerialNumber().longValue(),
				contenido:issuedCert.getEncoded(), eventoVotacion:evento, estado:Certificado.Estado.OK,
				solicitudCSRVoto:solicitudCSR, type:Certificado.Type.VOTO, usuario:representative,
				hashCertificadoVotoBase64:respuesta.hashCertificadoVotoBase64)
			certificado.save()
			byte[] issuedCertPEMBytes = CertUtil.fromX509CertToPEM(issuedCert);
			Map data = [requestPublicKey:requestPublicKey, issuedCert:issuedCert]
			return new ResponseVS(statusCode:ResponseVS.SC_OK, data:data)
		}
	}
	
	/**
	 * Genera un certificado V3
	 */
	public X509Certificate signCSR(byte[] csrPEMBytes, String organizationalUnit,
			PrivateKey caKey, X509Certificate caCert, Date fechaInicio, Date fechaFin)
			throws Exception {
		PKCS10CertificationRequest csr = fromPEMToPKCS10CertificationRequest(csrPEMBytes);
		String strSubjectDN = csr.getCertificationRequestInfo().getSubject().toString();
		if (!csr.verify() || strSubjectDN == null) {
			log.error("signCSR - ERROR VERIFYING");
			return null;
		}
		if(organizationalUnit != null) {
			strSubjectDN = organizationalUnit + "," + strSubjectDN;
		}
		//log.debug(" - strSubjectDN: " + strSubjectDN);
		X509Certificate issuedCert = CertUtil.generateV3EndEntityCertFromCsr(
				csr, caKey, caCert, fechaInicio, fechaFin, "" + strSubjectDN);
		//byte[] issuedCertPemBytes = CertUtil.fromX509CertToPEM(issuedCert);
		//byte[] caCertPemBytes = CertUtil.fromX509CertToPEM(caCert);
		//byte[] resultCsr = new byte[issuedCertPemBytes.length + caCertPemBytes.length];
		//System.arraycopy(issuedCertPemBytes, 0, resultCsr, 0, issuedCertPemBytes.length);
		//System.arraycopy(caCertPemBytes, 0, resultCsr, issuedCertPemBytes.length, caCertPemBytes.length);
		//return resultCsr;
		return issuedCert;
	}
		
	public X509Certificate getVoteCert(byte[] csrFirmada) throws Exception {
		Collection<X509Certificate> certificados =
			CertUtil.fromPEMToX509CertCollection(csrFirmada);
		X509Certificate userCert
		for (X509Certificate certificate : certificados) {
			if (certificate.subjectDN.toString().contains("OU=hashCertificadoVotoHEX:")) {
				userCert = certificate
			}
		}
		return userCert
	}
	
	
	public X509Certificate getUserCert(byte[] csrFirmada) throws Exception {
		Collection<X509Certificate> certificados =
			CertUtil.fromPEMToX509CertCollection(csrFirmada);
		X509Certificate userCert
		for (X509Certificate certificate : certificados) {
			if (certificate.subjectDN.toString().contains("OU=deviceId:")) {
				userCert = certificate
			}
		}
		return userCert
	}
		
    private ResponseVS validarCSRVoto(byte[] csrPEMBytes, 
		EventoVotacion evento, Locale locale) {
        PKCS10CertificationRequest csr = fromPEMToPKCS10CertificationRequest(csrPEMBytes);
		if(!csr) {
			String msg = messageSource.getMessage('csrRequestErrorMsg', null, locale)
			log.error("- validarCSRVoto - ERROR  ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg,
				type:TypeVS.ACCESS_REQUEST_ERROR)
		}
        CertificationRequestInfo info = csr.getCertificationRequestInfo();
		String eventoId;
		String controlAccesoURL;
		String hashCertificadoVotoHEX;
		String hashCertificadoVotoBase64;
        String subjectDN = info.getSubject().toString();
        log.debug("validarCSRVoto - subject: " + subjectDN)
		if(subjectDN.split("OU=eventoId:").length > 1) {
			eventoId = subjectDN.split("OU=eventoId:")[1].split(",")[0];
			if (!eventoId.equals(String.valueOf(evento.getId()))) {
				String msg = messageSource.getMessage('evento.solicitudCsrError', null, locale)
				log.error("- validarCSRVoto - ERROR - ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
					message:msg, type:TypeVS.ACCESS_REQUEST_ERROR)
			}
		}
		
		if(subjectDN.split("CN=controlAccesoURL:").length > 1) {
			String parte = subjectDN.split("CN=controlAccesoURL:")[1];
			if (parte.split(",").length > 1) {
				controlAccesoURL = parte.split(",")[0];
			} else controlAccesoURL = parte;
			controlAccesoURL = org.votingsystem.groovy.util.StringUtils.checkURL(controlAccesoURL)	
			String serverURL = grailsApplication.config.grails.serverURL
			if (!serverURL.equals(controlAccesoURL)) {
				String msg = messageSource.getMessage(
					'error.urlControlAccesoWrong', [serverURL, controlAccesoURL].toArray(), locale)
				log.error("- validarCSRVoto - ERROR - ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
					message:msg, type:TypeVS.ACCESS_REQUEST_ERROR)
			}	
		}
		if (subjectDN.split("OU=hashCertificadoVotoHEX:").length > 1) {
			try {
				hashCertificadoVotoHEX = subjectDN.split("OU=hashCertificadoVotoHEX:")[1].split(",")[0];
				HexBinaryAdapter hexConverter = new HexBinaryAdapter();
				hashCertificadoVotoBase64 = new String(hexConverter.unmarshal(hashCertificadoVotoHEX));
				log.debug("hashCertificadoVotoBase64: ${hashCertificadoVotoBase64}")
				SolicitudCSRVoto solicitudCSR = SolicitudCSRVoto.findWhere(
					hashCertificadoVotoBase64:hashCertificadoVotoBase64)
				if (solicitudCSR) {
					String msg = messageSource.getMessage(
						'error.hashCertificadoVotoRepetido', [hashCertificadoVotoBase64].toArray(), locale)
					log.error("- validarCSRVoto - ERROR - solicitudCSR previa: ${solicitudCSR.id} - ${msg}")
					return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
						message:msg, type:TypeVS.ACCESS_REQUEST_ERROR)
				}
			} catch (Exception ex) {
				log.error(ex.getMessage(), ex)
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
					message:ex.getMessage(), type:TypeVS.ACCESS_REQUEST_ERROR)
			}
		}
		return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.ACCESS_REQUEST,
			data:csr.getPublicKey(),hashCertificadoVotoBase64:hashCertificadoVotoBase64)
    }
	
	public ResponseVS saveUserCSR(byte[] csrPEMBytes, Locale locale) {
		PKCS10CertificationRequest csr = fromPEMToPKCS10CertificationRequest(csrPEMBytes);
		CertificationRequestInfo info = csr.getCertificationRequestInfo();
		String nif;
		String email;
		String telefono;
		String deviceId;
		String subjectDN = info.getSubject().toString();
		log.debug("saveUserCSR - subject: " + subjectDN)
		if(subjectDN.split("OU=email:").length > 1) {
			email = subjectDN.split("OU=email:")[1].split(",")[0];
		}
		if(subjectDN.split("CN=nif:").length > 1) {
			nif = subjectDN.split("CN=nif:")[1];
			if (nif.split(",").length > 1) {
				nif = nif.split(",")[0];
			}
		} else if(subjectDN.split("SERIALNUMBER=").length > 1) {
			nif = subjectDN.split("SERIALNUMBER=")[1];
			if (nif.split(",").length > 1) {
				nif = nif.split(",")[0];
			}
		}
		if (subjectDN.split("OU=telefono:").length > 1) {
			telefono = subjectDN.split("OU=telefono:")[1].split(",")[0];
		}
		if (subjectDN.split("OU=deviceId:").length > 1) {
			deviceId = subjectDN.split("OU=deviceId:")[1].split(",")[0];
			log.debug("Con deviceId: ${deviceId}")
		} else log.debug("Sin deviceId")
		ResponseVS respuesta = subscripcionService.comprobarDispositivo(
			nif, telefono, email, deviceId, locale)
		if(ResponseVS.SC_OK != respuesta.statusCode) return respuesta;
		SolicitudCSRUsuario solicitudCSR
		def solicitudesPrevias = SolicitudCSRUsuario.findAllByDispositivoAndUsuarioAndEstado(
			respuesta.dispositivo, respuesta.userVS, SolicitudCSRUsuario.Estado.PENDIENTE_APROVACION)
		solicitudesPrevias.each {eventoItem ->
			eventoItem.estado = SolicitudCSRUsuario.Estado.ANULADA
			eventoItem.save();
		}
		SolicitudCSRUsuario.withTransaction {
			solicitudCSR = new SolicitudCSRUsuario(
				estado:SolicitudCSRUsuario.Estado.PENDIENTE_APROVACION,
				contenido:csrPEMBytes, usuario:respuesta.userVS,
				dispositivo:respuesta.dispositivo).save()
		}
		if(solicitudCSR) return new ResponseVS(statusCode:ResponseVS.SC_OK, message:solicitudCSR.id)
		else return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST)
	}
	
	public synchronized ResponseVS firmarCertificadoUsuario (SolicitudCSRUsuario solicitudCSR, Locale locale) {
		log.debug("firmarCertificadoUsuario");
		File keyStoreFile = grailsApplication.mainContext.getResource(
			grailsApplication.config.VotingSystem.keyStorePath).getFile()
		String aliasClaves = grailsApplication.config.VotingSystem.signKeysAlias
		String password = grailsApplication.config.VotingSystem.signKeysPassword
		KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(
			FileUtils.getBytesFromFile(keyStoreFile), password.toCharArray());
		PrivateKey privateKeySigner = (PrivateKey)keyStore.getKey(aliasClaves, password.toCharArray());
		X509Certificate certSigner = (X509Certificate) keyStore.getCertificate(aliasClaves);
		
		//log.debug("firmarCertificadoUsuario - certSigner:${certSigner}");

		Date today = Calendar.getInstance().getTime();
		Calendar today_plus_year = Calendar.getInstance();
		today_plus_year.add(Calendar.YEAR, 1);
		X509Certificate issuedCert = signCSR(
				solicitudCSR.contenido, null, privateKeySigner,
				certSigner, today, today_plus_year.getTime())
		if (!issuedCert) {
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
				message:TypeVS.ERROR_VALIDANDO_CSR.toString())
		} else {
			solicitudCSR.estado = SolicitudCSRUsuario.Estado.OK
			solicitudCSR.numeroSerie = issuedCert.getSerialNumber().longValue()
			solicitudCSR.save()
			Certificado certificado = new Certificado(numeroSerie:issuedCert.getSerialNumber()?.longValue(),
				contenido:issuedCert.getEncoded(), usuario:solicitudCSR.usuario, estado:Certificado.Estado.OK,
				solicitudCSRUsuario:solicitudCSR, type:Certificado.Type.USUARIO, valido:true)
			certificado.save()
			return new ResponseVS(statusCode:ResponseVS.SC_OK)
		}
	}
	
	
	PKCS10CertificationRequest fromPEMToPKCS10CertificationRequest (
		byte[] csrBytes) throws Exception {
		PEMReader pemReader = new PEMReader(new InputStreamReader(
			new ByteArrayInputStream(csrBytes)));
		PKCS10CertificationRequest result = (PKCS10CertificationRequest)pemReader.readObject()
		pemReader.close();
		return result;
	}

}
