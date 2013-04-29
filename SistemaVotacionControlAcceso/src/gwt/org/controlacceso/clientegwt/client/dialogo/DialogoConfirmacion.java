package org.controlacceso.clientegwt.client.dialogo;

import java.util.Date;
import java.util.logging.Logger;

import org.controlacceso.clientegwt.client.Constantes;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.datepicker.client.DateBox;

public class DialogoConfirmacion {
	
    private static Logger logger = Logger.getLogger("DialogoConfirmacion");
	
    private static DialogoConfirmacionUiBinder uiBinder = 
    		GWT.create(DialogoConfirmacionUiBinder.class);
   

    interface DialogoConfirmacionUiBinder extends UiBinder<Widget, DialogoConfirmacion> {}

    interface Style extends CssResource {
        String errorTextBox();
    }

    @UiField DialogBox dialogBox;
    @UiField Style style;
    @UiField PushButton aceptarButton;
    @UiField PushButton cerrarButton;
    @UiField VerticalPanel messagePanel;
    @UiField HorizontalPanel datePanel;
    @UiField DateBox dateToBox;
    @UiField DateBox dateFromBox;
    @UiField Label errorMessageLabel;
    @UiField Label dateFromLabel;
    @UiField Label dateToLabel;
    @UiField VerticalPanel errorMessagePanel;
    private boolean isDateFromVisible = false;
    private boolean isDateToVisible = false;
    private ConfirmacionListener listener;
    private Integer id;
    
    
	public DialogoConfirmacion(Integer id, ConfirmacionListener listener) {
        uiBinder.createAndBindUi(this);
        datePanel.setVisible(false);
        this.listener = listener;
        this.id = id;
        setErrorMessage(null);
        setDates(null, null);
	}
	
	public void setDates(Date dateFrom, Date dateTo) {
		if(dateFrom == null) {
			logger.info("dateFrom null");
			dateToLabel.setVisible(false);
			dateFromLabel.setVisible(false);
			datePanel.setVisible(false);
			return;
		}
		dateFromBox.setValue(dateFrom);
		dateFromBox.setVisible(true);
		isDateFromVisible = true;
		if(dateTo != null) {
			dateToBox.setValue(dateTo);
			dateToBox.setVisible(true);
			dateFromLabel.setText(Constantes.INSTANCIA.requestDateFromLabel() + ": ");
			dateToLabel.setText(Constantes.INSTANCIA.requestDateToLabel() + ": ");
			dateFromLabel.setVisible(true);
			dateToLabel.setVisible(true);
			isDateToVisible = true;
		} else {
			dateToBox.setVisible(false);
			dateToLabel.setVisible(false);
			dateFromLabel.setText(Constantes.INSTANCIA.requestDateLabel() + ": ");
			dateFromLabel.setVisible(true);
		}
		datePanel.setVisible(true);
	}

	private void setErrorMessage (String message) {
		if(message == null || "".equals(message)) errorMessagePanel.setVisible(false);
		else {
			errorMessageLabel.setText(message);
			errorMessagePanel.setVisible(true);
		}
	}
	
	private boolean isValidForm() {
		if(isDateFromVisible && dateFromBox.getValue() == null) {
			setErrorMessage(Constantes.INSTANCIA.emptyFieldException());
			dateFromBox.setStyleName(style.errorTextBox(), true);
			return false;
		} else {
			dateFromBox.setStyleName(style.errorTextBox(), false);
		}
		if(isDateToVisible && dateToBox.getValue() == null) {
			setErrorMessage(Constantes.INSTANCIA.emptyFieldException());
			dateToBox.setStyleName(style.errorTextBox(), true);
			return false;
		} else {
			dateToBox.setStyleName(style.errorTextBox(), false);
		}
		if(dateFromBox.getValue().after(dateToBox.getValue())) {
			setErrorMessage(Constantes.INSTANCIA.fechaInicioAfterFechaFinalMsg());
			dateFromBox.setStyleName(style.errorTextBox(), true);
			dateToBox.setStyleName(style.errorTextBox(), true);
			return false;
		} else {
			dateToBox.setStyleName(style.errorTextBox(), false);
			dateFromBox.setStyleName(style.errorTextBox(), false);
		}
		return true;
	}

	@UiHandler("aceptarButton")
    void handleAceptarButton(ClickEvent e) {
		if(!isValidForm()) return;
		if(!isDateFromVisible) {
			listener.confirmed(id, null);
			dialogBox.hide();
		}
		if(isDateFromVisible && isDateToVisible) {
			Date[] dateArray = new Date[2];
			dateArray[0] = dateFromBox.getValue();
			dateArray[1] = dateToBox.getValue();
			listener.confirmed(id, dateArray);
			dialogBox.hide();
		}
		if(isDateFromVisible && !isDateToVisible) {
			listener.confirmed(id, dateFromBox.getValue());
			dialogBox.hide();
		}
    }
    
    @UiHandler("cerrarButton")
    void handleCerrarButton(ClickEvent e) {
    	dialogBox.hide();
    }
    
    public void show(String caption, String message) {
    	HTML htmlMessage = new HTML(message);
    	messagePanel.add(htmlMessage);
    	dialogBox.setText(caption);
    	dialogBox.center();
    	dialogBox.show();
    }
    
    
    public void show(String message) {
    	HTML htmlMessage = new HTML(message);
    	messagePanel.add(htmlMessage);
    	dialogBox.center();
    	dialogBox.show();
    }

}
