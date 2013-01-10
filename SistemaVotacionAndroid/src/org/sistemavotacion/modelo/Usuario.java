package org.sistemavotacion.modelo;

import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
public class Usuario {

    private static final long serialVersionUID = 1L;

    private String nif;
    private String primerApellido;
    private String pais;
    private String cn;
    private String nombre;
    private String nombreCompleto = "";
    private String organizacion;
    private String email;
    private String telefono;

    private Set<Comentario> comentarios = new HashSet<Comentario>(0);

    private X509Certificate certificate;

   /**
     * @return the id
     */
    public String getNif() {
        return nif;
    }

    /**
     * @param id the id to set
     */
    public void setNif(String nif) {
        this.nif = nif;
    }

    /**
     * @return the comentarios
     */
    public Set<Comentario> getComentarios() {
        return comentarios;
    }

    /**
     * @param comentarios the comentarios to set
     */
    public void setComentarios(Set<Comentario> comentarios) {
        this.comentarios = comentarios;
    }

    public static Usuario getUsuario (X509Certificate certificate) {
    	Usuario usuario = new Usuario();
    	usuario.setCertificate(certificate);
    	String subjectDN = certificate.getSubjectDN().getName();
    	if (subjectDN.contains("C="))
    		usuario.setPais(subjectDN.split("C=")[1].split(",")[0]);
    	if (subjectDN.contains("SERIALNUMBER="))
    		usuario.setNif(subjectDN.split("SERIALNUMBER=")[1].split(",")[0]);
    	if (subjectDN.contains("SURNAME="))
    		usuario.setPrimerApellido(subjectDN.split("SURNAME=")[1].split(",")[0]);
    	if (subjectDN.contains("GIVENNAME="))
    		usuario.setNombre(subjectDN.split("GIVENNAME=")[1].split(",")[0]);
    	if (subjectDN.contains("CN="))
    		usuario.setCn(subjectDN.split("CN=")[1]);
    	return usuario;
    }

    public void setPais(String pais) {
            this.pais = pais;
    }

    public String getPais() {
            return pais;
    }

    public void setPrimerApellido(String primerApellido) {
            this.primerApellido = primerApellido;
    }

    public String getPrimerApellido() {
            return primerApellido;
    }

    public void setCn(String cn) {
            this.cn = cn;
    }

    public String getCn() {
            return cn;
    }

    public void setCertificate(X509Certificate certificate) {
            this.certificate = certificate;
    }

    public X509Certificate getCertificate() {
            return certificate;
    }

    public void setEmail(String email) {
            this.email = email;
    }

    public String getEmail() {
            return email;
    }

    public void setTelefono(String telefono) {
            this.telefono = telefono;
    }

    public String getTelefono() {
            return telefono;
    }

    /**
     * @return the organizacion
     */
    public String getOrganizacion() {
        return organizacion;
    }

    /**
     * @param organizacion the organizacion to set
     */
    public void setOrganizacion(String organizacion) {
        this.organizacion = organizacion;
    }

	public void setNombre(String nombre) {
		this.nombre = nombre;		
	}

	public String getNombre() {
		return nombre;		
	}
	
	public void setNombreCompleto(String nombreCompleto) {
		this.nombreCompleto = nombreCompleto;		
	}

	public String getNombreCompleto() {
		return nombreCompleto;		
	}
	
	
}
