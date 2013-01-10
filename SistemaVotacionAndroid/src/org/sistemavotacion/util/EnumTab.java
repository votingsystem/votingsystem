package org.sistemavotacion.util;

import org.sistemavotacion.modelo.Evento;


public enum EnumTab {
	
	OPEN, PENDING, CLOSED;
	
	public static EnumTab valueOf(int position)  {
        switch (position) {
	        case 0: return OPEN;
	        case 1: return PENDING;
	        case 2: return CLOSED;
	        default: return null;
        }
	}
	
	public String getColor()  {
        switch(this) {
        	case OPEN: return "#6bad74";
        	case PENDING: return "#fba131";
        	case CLOSED: return "#cc1606";
        	default: return "#000000";
        }
	}
	
	public Evento.Estado getEventState() {
        switch(this) {
	    	case OPEN: return Evento.Estado.ACTIVO;
	    	case PENDING: return Evento.Estado.PENDIENTE_COMIENZO;
	    	case CLOSED: return Evento.Estado.FINALIZADO;
	    	default: return null;
        }
	}
	
}
