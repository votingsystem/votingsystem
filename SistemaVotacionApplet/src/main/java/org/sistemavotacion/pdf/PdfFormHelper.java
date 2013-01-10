package org.sistemavotacion.pdf;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.sistemavotacion.util.FileUtils;

/**
* @author jgzornoza
* Licencia: https://raw.github.com/jgzornoza/SistemaVotacionAppletFirma/master/licencia.txt
*/
public class PdfFormHelper {
    
        
    public static void crearPdf(String filename) throws DocumentException, IOException {
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(filename));
        document.open();
        PdfPCell cell;
        PdfPTable table = new PdfPTable(2);
        table.setWidths(new int[]{ 1, 2 });

        table.addCell("Identificador del evento:");
        cell = new PdfPCell();
        cell.setCellEvent(identificadorEventoPdfPCellEvent);
        table.addCell(cell);
        
        table.addCell("Asunto:");
        cell = new PdfPCell();
        cell.setCellEvent(asuntoPdfPCellEvent);
        table.addCell(cell);
        
        table.addCell("Email del solicitante:");
        cell = new PdfPCell();
        cell.setCellEvent(emailCellEvent);
        table.addCell(cell);

        document.add(table);
        document.close();
    }
    
    
    static PdfPCellEvent identificadorEventoPdfPCellEvent = new PdfPCellEvent() {
        @Override
        public void cellLayout(PdfPCell cell, Rectangle rectangle, PdfContentByte[] canvases) {
            PdfWriter writer = canvases[0].getPdfWriter();
            TextField text = new TextField(writer, rectangle, "eventoId");
            //text.setBorderStyle(PdfBorderDictionary.STYLE_BEVELED);
            text.setText("... Identificador del manifiesto ...");
            text.setFontSize(0);
            text.setAlignment(Element.ALIGN_CENTER);
            text.setOptions(TextField.REQUIRED);
            try {
                PdfFormField field = text.getTextField();
                writer.addAnnotation(field);
            }
            catch(IOException ioe) {
                throw new ExceptionConverter(ioe);
            }
            catch(DocumentException de) {
                throw new ExceptionConverter(de);
            }
        }
    };
    
    static PdfPCellEvent asuntoPdfPCellEvent = new PdfPCellEvent() {
        @Override
        public void cellLayout(PdfPCell cell, Rectangle rectangle, PdfContentByte[] canvases) {
            PdfWriter writer = canvases[0].getPdfWriter();
            TextField text = new TextField(writer, rectangle, "asunto");
            //text.setBorderStyle(PdfBorderDictionary.STYLE_BEVELED);
            text.setText("... Asunto del manifiesto ...");
            text.setFontSize(0);
            text.setAlignment(Element.ALIGN_CENTER);
            text.setOptions(TextField.REQUIRED);
            try {
                PdfFormField field = text.getTextField();
                writer.addAnnotation(field);
            }
            catch(IOException ioe) {
                throw new ExceptionConverter(ioe);
            }
            catch(DocumentException de) {
                throw new ExceptionConverter(de);
            }
        }
    };
    
    static PdfPCellEvent emailCellEvent = new PdfPCellEvent() {
        @Override
        public void cellLayout(PdfPCell cell, Rectangle rectangle, PdfContentByte[] canvases) {
            PdfWriter writer = canvases[0].getPdfWriter();
            TextField text = new TextField(writer, rectangle, "email");
            //text.setBorderStyle(PdfBorderDictionary.STYLE_BEVELED);
            text.setText("... Email del solicitante ...");
            text.setFontSize(0);
            text.setAlignment(Element.ALIGN_CENTER);
            text.setOptions(TextField.REQUIRED);
            try {
                PdfFormField field = text.getTextField();
                writer.addAnnotation(field);
            }
            catch(IOException ioe) {
                throw new ExceptionConverter(ioe);
            }
            catch(DocumentException de) {
                throw new ExceptionConverter(de);
            }
        }
    };
    
    public static File obtenerSolicitudCopia(String eventoId, String asuntoManifiesto, 
            String emailSolicitante) throws DocumentException, IOException {
        String pathSolicitud = FileUtils.APPTEMPDIR + "solicitud.pdf";
        crearPdf(pathSolicitud);
        String pathFormularioRelleno = FileUtils.APPTEMPDIR + "solicitudRellena.pdf";
        PdfReader reader = new PdfReader(pathSolicitud);
        PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(pathFormularioRelleno));
        AcroFields form = stamper.getAcroFields();
        if(asuntoManifiesto != null) form.setField("asunto", asuntoManifiesto);
        if(emailSolicitante != null) form.setField("email", emailSolicitante);
        if(eventoId != null) form.setField("eventoId", eventoId);
        stamper.close();
        return new File(pathFormularioRelleno);
    }
    
}