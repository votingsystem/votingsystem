package org.votingsystem.accesscontrol.model;

import java.io.Serializable;

import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

public class EtiquetaEventoPK implements Serializable {

	private static final long serialVersionUID = 1L;

	@ManyToOne
    @JoinColumn(name = "etiquetaId", referencedColumnName = "ID")
    private Etiqueta etiqueta;

    @ManyToOne
    @JoinColumn(name = "eventoId", referencedColumnName = "ID")
    private Evento evento;
}
