package org.controlacceso.clientegwt.client.panel;

import java.util.logging.Logger;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

public class BarraNavegacion extends Composite {

    private static Logger logger = Logger.getLogger("BarraNavegacion");
	
    private static BarraNavegacionUiBinder uiBinder = 
    	GWT.create(BarraNavegacionUiBinder.class);

    interface BarraNavegacionUiBinder extends UiBinder<Widget, BarraNavegacion> { }

    interface Style extends CssResource {
        String pageAnchor();
        String pageSelectedAnchor();
    }
    
    @UiField Button gotoNextButton;
    @UiField Button gotoPrevButton;
    @UiField Style style;
    @UiField HorizontalPanel contenedorPaginas;

    
    Listener listener;
    int pageSelected   = 1;
    int maxNumberPages = 15;
    int size, offset, range;

    public interface Listener {
    	void gotoPage(int offset, int range);
    }
    
    public BarraNavegacion() {
    	initWidget(uiBinder.createAndBindUi(this));
    	gotoNextButton.setEnabled(false);
    	gotoPrevButton.setEnabled(false);
    }
    
    
    public void addListener (Listener listener, int offset, int range, int size) {	
    	this.offset = offset;
    	this.range = range;
    	this.listener = listener;
    	this.size = size;
    	if(offset < range)  pageSelected = 1;
    	else pageSelected = offset/range + 1;
    	logger.info("addListener - offset: " + offset + " - range: " + range 
    			+ " - size: " + size + " - pageSelected: " + pageSelected);
    	updatePageAnchors(pageSelected);
    	setNavigationButtonState(offset);
    	//if(size < range) setVisible(false);
    }
    
    
    @UiHandler("gotoNextButton")
    void handleGotoNext(ClickEvent e) {
    	pageSelected++;
    	updatePageAnchors(pageSelected);
    	listener.gotoPage((pageSelected -1) * range, range);
    }
    
    @UiHandler("gotoPrevButton")
    void handleGotoPrev(ClickEvent e) {
    	pageSelected--;
    	updatePageAnchors(pageSelected);
    	listener.gotoPage((pageSelected -1) * range, range);
    }
    
    private void setNavigationButtonState(int offset) {
    	if(offset >= range) gotoPrevButton.setEnabled(true);
    	else gotoPrevButton.setEnabled(false);
    	if((offset + range) < size) gotoNextButton.setEnabled(true);
    	else gotoNextButton.setEnabled(false);
    }
    
    private void updatePageAnchors(int pageSelected) {
    	this.pageSelected = pageSelected;
    	int numberPages = (size/range);
    	if(size % range > 0) numberPages++;
    	logger.info("updatePageAnchors - pageSelected: " + pageSelected 
    			+ " - numberPages: " + numberPages);
    	contenedorPaginas.clear();
    	int beginPage = 1;
    	int endPage = maxNumberPages;
    	if(numberPages < maxNumberPages) endPage = numberPages;
    	if(pageSelected > maxNumberPages/2) {
    		if(pageSelected + maxNumberPages/2 < numberPages) {
    			beginPage = pageSelected - maxNumberPages/2;
    			endPage = pageSelected + maxNumberPages/2;
    		} else {
    			endPage = numberPages;
    			beginPage = numberPages - maxNumberPages;
    		}	
    	}
    	for(int i = beginPage; i <= endPage; i++) {
    		final Anchor anchor = new Anchor(new Integer(i).toString());
			final int pageNumber = i;
			if(pageSelected == i) {
				anchor.setStyleName(style.pageSelectedAnchor(), true);
			} else {
				anchor.setStyleName(style.pageAnchor(), true);
				anchor.addClickHandler(new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						logger.info("Haciendo click sobre página -> " + pageNumber);
						updatePageAnchors(pageNumber);
						listener.gotoPage((pageNumber -1) * range , range);
					}});
			} 
			contenedorPaginas.add(anchor);
		}
    	//if(numberPages > 10) {} else {	}
    	
    	setNavigationButtonState(this.offset);
    }

}