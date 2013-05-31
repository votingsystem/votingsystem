/*
 * Copyright 2011 - Jose. J. García Zornoza
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sistemavotacion.json;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.Set;

import org.sistemavotacion.android.R;
import org.sistemavotacion.modelo.*;
import org.sistemavotacion.util.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import android.util.Log;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class DeJSONAObjeto {
    
	public static final String TAG = "DeJSONAObjeto";
    
    public static Evento obtenerEvento(String eventoStr) throws ParseException, JSONException  {
    	Log.d(TAG + ".obtenerEvento(...)", eventoStr);
    	return obtenerEvento(new JSONObject(eventoStr));
    }
    
    public static Evento obtenerEvento(JSONObject eventoJSON) throws ParseException, JSONException {
        JSONArray jsonArray;
        JSONObject jsonObject;
        Evento evento = new Evento();
        if (eventoJSON.has("URL"))
            evento.setURL(eventoJSON.getString("URL"));
        if (eventoJSON.has("contenido"))
            evento.setContenido(eventoJSON.getString("contenido"));
        if (eventoJSON.has("asunto"))        
            evento.setAsunto(eventoJSON.getString("asunto"));
        if (eventoJSON.has("id"))        
            evento.setEventoId(eventoJSON.getLong("id"));
        if (eventoJSON.has("usuario")) {
            Usuario usuario = new Usuario();
            usuario.setNombreCompleto(eventoJSON.getString("usuario"));
            evento.setUsuario(usuario);
        }
        if (eventoJSON.has("numeroTotalFirmas"))        
            evento.setNumeroTotalFirmas(eventoJSON.getInt("numeroTotalFirmas"));
        if (eventoJSON.has("numeroTotalVotos"))        
            evento.setNumeroTotalFirmas(eventoJSON.getInt("numeroTotalVotos"));      
        if (eventoJSON.has("fechaCreacion"))
            evento.setDateCreated(DateUtils.getDateFromString(eventoJSON.getString("fechaCreacion")));
        if (eventoJSON.has("fechaInicio"))
            evento.setFechaInicio(DateUtils.getDateFromString(eventoJSON.getString("fechaInicio")));
        if (eventoJSON.has("fechaFin") && !eventoJSON.isNull("fechaFin"))
            evento.setFechaFin(DateUtils.getDateFromString(eventoJSON.getString("fechaFin")));
        evento.setFirmado(Boolean.FALSE);
        if (eventoJSON.has("controlAcceso") && 
                eventoJSON.getJSONObject("controlAcceso") != null) {
            jsonObject = eventoJSON.getJSONObject("controlAcceso");
            ControlAcceso controlAcceso = new ControlAcceso ();
            controlAcceso.setServerURL(jsonObject.getString("serverURL"));
            controlAcceso.setNombre(jsonObject.getString("nombre"));
            evento.setControlAcceso(controlAcceso);
        }
        if (eventoJSON.has("etiquetas") && 
                eventoJSON.getJSONArray("etiquetas") != null) {
            List<String> etiquetas = new ArrayList<String>();
            jsonArray = eventoJSON.getJSONArray("etiquetas");
            for (int i = 0; i< jsonArray.length(); i++) {
                etiquetas.add(jsonArray.getString(i));
            }
            evento.setEtiquetas(etiquetas.toArray(new String[jsonArray.length()]));
        }
        if (eventoJSON.has("campos")) {
            Set<CampoDeEvento> campos = new HashSet<CampoDeEvento>();
            jsonArray = eventoJSON.getJSONArray("campos");
             for (int i = 0; i< jsonArray.length(); i++) {
                jsonObject = jsonArray.getJSONObject(i);
                CampoDeEvento campo = new CampoDeEvento();
                campo.setId(jsonObject.getLong("id"));
                campo.setContenido(jsonObject.getString("contenido"));
                campos.add(campo);
             }
            evento.setCampos(campos);
        }
        if (eventoJSON.has("opciones")) {
            Set<OpcionDeEvento> opciones = new HashSet<OpcionDeEvento>();
            jsonArray = eventoJSON.getJSONArray("opciones");
             for (int i = 0; i< jsonArray.length(); i++) {
                jsonObject = jsonArray.getJSONObject(i);
                OpcionDeEvento opcion = new OpcionDeEvento();
                opcion.setId(jsonObject.getLong("id"));
                opcion.setContenido(jsonObject.getString("contenido"));
                opciones.add(opcion);
             }
            evento.setOpciones(opciones);
        }
        if (eventoJSON.has("centroControl")) {
            jsonObject = eventoJSON.getJSONObject("centroControl");
            CentroControl centroControl = new CentroControl();
            if(jsonObject.has("id"))
            	centroControl.setId(jsonObject.getLong("id"));
            if(jsonObject.has("serverURL"))
            	centroControl.setServerURL(jsonObject.getString("serverURL"));
            if(jsonObject.has("nombre"))
            	centroControl.setNombre(jsonObject.getString("nombre"));
            evento.setCentroControl(centroControl);
        }
        if (eventoJSON.has("estado")) {
            evento.setEstado(eventoJSON.getString("estado"));
        }
        if (eventoJSON.has("hashSolicitudAccesoBase64")) {
            evento.setHashSolicitudAccesoBase64(eventoJSON.
            		getString("hashSolicitudAccesoBase64"));
        }
        if (eventoJSON.has("origenHashSolicitudAcceso")) {
            evento.setOrigenHashSolicitudAcceso(eventoJSON.
            		getString("origenHashSolicitudAcceso"));
        }
        if (eventoJSON.has("hashCertificadoVotoBase64")) {
            evento.setHashCertificadoVotoBase64(eventoJSON.
            		getString("hashCertificadoVotoBase64"));
        }
        if (eventoJSON.has("origenHashCertificadoVoto")) {
            evento.setOrigenHashCertificadoVoto(eventoJSON.
            		getString("origenHashCertificadoVoto"));
        }
        if (eventoJSON.has("opcionSeleccionada")) {
        	jsonObject = eventoJSON.getJSONObject("opcionSeleccionada");
        	OpcionDeEvento opcionSeleccionada = new OpcionDeEvento ();
        	opcionSeleccionada.setId(jsonObject.getLong("id"));
        	opcionSeleccionada.setContenido(jsonObject.getString("contenido"));
            evento.setOpcionSeleccionada(opcionSeleccionada);
        }
        evento.comprobarFechas();
        return evento;
    }

       public static ActorConIP obtenerActorConIP(String actorConIPStr, ActorConIP.Tipo tipo) 
    		   throws Exception {
           JSONObject actorConIPJSON = new JSONObject(actorConIPStr);
           JSONObject jsonObject = null;
           ActorConIP actorConIP = null;
           JSONArray jsonArray;
           switch (tipo) {
                case CENTRO_CONTROL:
                    actorConIP = new CentroControl();
                    break;
                case CONTROL_ACCESO:
                    actorConIP = new ControlAcceso();
                    ((ControlAcceso)actorConIP).setUrlClientePublicacionJNLP(actorConIPStr);
                    if (actorConIPJSON.getJSONArray("centrosDeControl") != null) {
                        Set<CentroControl> centrosDeControl = new HashSet<CentroControl>();
                        jsonArray = actorConIPJSON.getJSONArray("centrosDeControl");
                        for (int i = 0; i< jsonArray.length(); i++) {
                            jsonObject = jsonArray.getJSONObject(i);
                            CentroControl centroControl = new CentroControl();
                            centroControl.setNombre(jsonObject.getString("nombre"));
                            centroControl.setServerURL(jsonObject.getString("serverURL"));
                            centroControl.setId(jsonObject.getLong("id"));
                            centroControl.setDateCreated(DateUtils.getDateFromString(jsonObject.getString("fechaCreacion")));
                            if (jsonObject.getString("estado") != null) {
                                 centroControl.setEstado(ActorConIP.Estado.valueOf(jsonObject.getString("estado")));
                            }
                            centrosDeControl.add(centroControl);
                        }
                        ((ControlAcceso)actorConIP).setCentrosDeControl(centrosDeControl);
                    }
                    break;
               
           }
           if (actorConIPJSON.has("urlBlog"))
                actorConIP.setUrlBlog(actorConIPJSON.getString("urlBlog"));
           if (actorConIPJSON.has("serverURL"))
                actorConIP.setServerURL(actorConIPJSON.getString("serverURL"));
           if (actorConIPJSON.has("nombre"))
                actorConIP.setNombre(actorConIPJSON.getString("nombre"));
           if (actorConIPJSON.has("cadenaCertificacionPEM")) {
        	   Collection<X509Certificate> certChain = 
	        			CertUtil.fromPEMToX509CertCollection(actorConIPJSON.
	        			getString("cadenaCertificacionPEM").getBytes());
	        	actorConIP.setCertChain(certChain);
	        	X509Certificate serverCert = certChain.iterator().next();
	        	Log.d(TAG + ".obtenerActorConIP(..) ", " - actorConIP Cert: " 
	        			+ serverCert.getSubjectDN().toString());
	        	actorConIP.setCertificado(serverCert);
           }
           return actorConIP;
       }
    
    public Evento obtenerEventoValidado(SMIMEMessageWrapper dnieMimeMessage) throws Exception {
        Evento evento = null;
        evento = obtenerEvento(dnieMimeMessage.getSignedContent());
        return evento;
    }
    
    public static Consulta obtenerConsultaEventos(String consultaStr) throws ParseException, JSONException {
    	Log.d(TAG + ".obtenerConsultaEventos(...)", "obtenerConsultaEventos(...)");
    	JSONObject jsonObject = new JSONObject (consultaStr);
        List<Evento> eventos = new ArrayList<Evento>();
        JSONObject jsonEventos = jsonObject.getJSONObject("eventos");
        JSONArray arrayEventos;
        if (jsonEventos != null) {
        	if(jsonEventos.has("firmas")) {
                arrayEventos = jsonEventos.getJSONArray("firmas");
                if (arrayEventos != null) {
                    for (int i=0; i<arrayEventos.length(); i++) {
                        Evento evento = obtenerEvento(arrayEventos.getJSONObject(i));
                        evento.setTipo(Tipo.EVENTO_FIRMA);
                        eventos.add(evento);
                    }
                }	
        	}
        	if(jsonEventos.has("reclamaciones")) { 
                arrayEventos = jsonEventos.getJSONArray("reclamaciones");
                if (arrayEventos != null) {
                    for (int i=0; i<arrayEventos.length(); i++) {
                        Evento evento = obtenerEvento(arrayEventos.getJSONObject(i));
                        evento.setTipo(Tipo.EVENTO_RECLAMACION);
                        eventos.add(evento);
                    }
                }	
        	}
        	if(jsonEventos.has("votaciones")) {
                arrayEventos = jsonEventos.getJSONArray("votaciones");
                if (arrayEventos != null) {
                    for (int i=0; i<arrayEventos.length(); i++) {
                        Evento evento = obtenerEvento(arrayEventos.getJSONObject(i));
                        evento.setTipo(Tipo.EVENTO_VOTACION);
                        eventos.add(evento);
                    }
                }	
        	}
        }
        Consulta consulta = new Consulta();
        if(jsonEventos.has("numeroEventosFirmaEnPeticion"))
        	consulta.setNumeroEventosFirmaEnPeticion(jsonObject.getInt("numeroEventosFirmaEnPeticion"));
        if(jsonEventos.has("numeroTotalEventosFirmaEnSistema"))
        	consulta.setNumeroTotalEventosFirmaEnSistema(jsonObject.getInt("numeroTotalEventosFirmaEnSistema"));
        if(jsonEventos.has("numeroEventosVotacionEnPeticion"))
        	consulta.setNumeroEventosVotacionEnPeticion(jsonObject.getInt("numeroEventosVotacionEnPeticion"));
        if(jsonEventos.has("numeroTotalEventosVotacionEnSistema"))
        	consulta.setNumeroTotalEventosVotacionEnSistema(jsonObject.getInt("numeroTotalEventosVotacionEnSistema"));
        if(jsonEventos.has("numeroEventosReclamacionEnPeticion"))
        	consulta.setNumeroEventosReclamacionEnPeticion(jsonObject.getInt("numeroEventosReclamacionEnPeticion"));
        if(jsonEventos.has("numeroTotalEventosReclamacionEnSistema"))
        	consulta.setNumeroTotalEventosReclamacionEnSistema(jsonObject.getInt("numeroTotalEventosReclamacionEnSistema"));
        if(jsonEventos.has("numeroEventosEnPeticion"))
        	consulta.setNumeroEventosEnPeticion(jsonObject.getInt("numeroEventosEnPeticion"));
        if(jsonEventos.has("numeroTotalEventosEnSistema"))
        	consulta.setNumeroTotalEventosEnSistema(jsonObject.getInt("numeroTotalEventosEnSistema"));
        if (jsonObject.has("offset"))
            consulta.setOffset(jsonObject.getInt("offset"));
        consulta.setEventos(eventos);
        return consulta;
    }
    
    
    public Estadisticas obtenerEstadisticas (String strEstadisticas) throws IOException, Exception {
        Estadisticas estadisticas = new Estadisticas();
        JSONObject estadisticaJSON = new JSONObject(strEstadisticas);
        JSONObject jsonObject = null;
        JSONArray jsonArray = null;
        if (estadisticaJSON.has("tipo")) 
            estadisticas.setTipo(Tipo.valueOf(estadisticaJSON.getString("tipo")));
        if (estadisticaJSON.has("id"))
             estadisticas.setId(estadisticaJSON.getLong("id"));
        if (estadisticaJSON.has("estado"))
             estadisticas.setEstado(Evento.Estado.valueOf(estadisticaJSON.getString("estado")));
        if (estadisticaJSON.has("usuario")) {
            Usuario usuario = new Usuario();
            usuario.setNif(estadisticaJSON.getString("usuario"));
            estadisticas.setUsuario(usuario);
        }
        if (estadisticaJSON.has("numeroSolicitudesDeAcceso"))
             estadisticas.setNumeroSolicitudesDeAcceso(estadisticaJSON.getInt("numeroSolicitudesDeAcceso"));
        if (estadisticaJSON.has("numeroFirmasRecibidas"))
             estadisticas.setNumeroFirmasRecibidas(estadisticaJSON.getInt("numeroFirmasRecibidas"));
        if (estadisticaJSON.has("solicitudPublicacionValidadaURL"))
             estadisticas.setSolicitudPublicacionValidadaURL(estadisticaJSON.getString("solicitudPublicacionValidadaURL"));
        if (estadisticaJSON.has("solicitudPublicacionURL"))
             estadisticas.setSolicitudPublicacionURL(estadisticaJSON.getString("solicitudPublicacionURL"));
        if (estadisticaJSON.has("fechaInicio"))
             estadisticas.setFechaInicio(DateUtils.getDateFromString(estadisticaJSON.getString("fechaInicio")));
        if (estadisticaJSON.has("fechaFin"))
             estadisticas.setFechaInicio(DateUtils.getDateFromString(estadisticaJSON.getString("fechaFin")));        
        if (estadisticaJSON.has("centroControl")) {
            jsonObject = estadisticaJSON.getJSONObject("centroControl");
            CentroControl centroControl = new CentroControl();
            centroControl.setNombre(jsonObject.getString("nombre"));
            centroControl.setServerURL(jsonObject.getString("serverURL"));
            centroControl.setId(jsonObject.getLong("id"));
        } 
        if (estadisticaJSON.has("numeroSolicitudesDeAcceso"))
             estadisticas.setNumeroSolicitudesDeAcceso(estadisticaJSON.getInt("numeroSolicitudesDeAcceso"));          
        return estadisticas;	
    }
    
    public Set<Comentario> obtenerComentarios(String comentariosStr) {
         return null;
    }
    
}
