package org.votingsystem.util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.votingsystem.model.ContextVS;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class PdfFormHelper {
    

    static PdfPCellEvent eventIdPdfPCellEvent = new PdfPCellEvent() {
        @Override
        public void cellLayout(PdfPCell cell, Rectangle rectangle, PdfContentByte[] canvases) {
            PdfWriter writer = canvases[0].getPdfWriter();
            TextField text = new TextField(writer, rectangle, "eventId");
            //text.setBorderStyle(PdfBorderDictionary.STYLE_BEVELED);
            text.setText(ContextVS.getInstance().getMessage("eventIdPdfPCellEventLbl"));
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
    
    static PdfPCellEvent subjectPdfPCellEvent = new PdfPCellEvent() {
        @Override
        public void cellLayout(PdfPCell cell, Rectangle rectangle, PdfContentByte[] canvases) {
            PdfWriter writer = canvases[0].getPdfWriter();
            TextField text = new TextField(writer, rectangle, "subject");
            //text.setBorderStyle(PdfBorderDictionary.STYLE_BEVELED);
            text.setText(ContextVS.getInstance().getMessage("subjectPdfPCellEvent"));
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
            text.setText(ContextVS.getInstance().getMessage("emailCellEventLbl"));
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
    
    public static byte[] getBackupRequest(
            String eventId, String eventVSManifestSubject,
            String email) throws DocumentException, IOException {
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, baos);
        document.open();
        PdfPCell cell;
        PdfPTable table = new PdfPTable(2);
        table.setWidths(new int[]{ 1, 2 });

        table.addCell(ContextVS.getInstance().getMessage("eventIdLbl") + ":");
        cell = new PdfPCell();
        cell.setCellEvent(eventIdPdfPCellEvent);
        table.addCell(cell);
        
        table.addCell(ContextVS.getInstance().getMessage("subjectLbl") + ":");
        cell = new PdfPCell();
        cell.setCellEvent(subjectPdfPCellEvent);
        table.addCell(cell);
        
        table.addCell(ContextVS.getInstance().getMessage("emailApplicantLbl") + ":");
        cell = new PdfPCell();
        cell.setCellEvent(emailCellEvent);
        table.addCell(cell);

        document.add(table);
        document.close();
        baos.close();
        
        
        ByteArrayOutputStream formStamperBaos = new ByteArrayOutputStream();
        PdfReader reader = new PdfReader(baos.toByteArray());
        PdfStamper stamper = new PdfStamper(reader, formStamperBaos);
        AcroFields form = stamper.getAcroFields();
        if(eventVSManifestSubject != null) form.setField("subject", eventVSManifestSubject);
        if(email != null) form.setField("email", email);
        if(eventId != null) form.setField("eventId", eventId);
        form.setField("UUID", UUID.randomUUID().toString());
        stamper.close();
        formStamperBaos.close();
        return formStamperBaos.toByteArray();
    }
    
}