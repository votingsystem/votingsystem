package org.votingsystem.simulation.util

import org.votingsystem.simulation.ApplicationContextHolder as ACH;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class PdfFormHelper {
	

	public static PdfPCellEvent eventIdPdfPCellEvent = new PdfPCellEvent() {
		@Override
		public void cellLayout(PdfPCell cell, Rectangle rectangle, PdfContentByte[] canvases) {
			PdfWriter writer = canvases[0].getPdfWriter();
			TextField text = new TextField(writer, rectangle, "eventoId");
			//text.setBorderStyle(PdfBorderDictionary.STYLE_BEVELED);			
			text.setText(ACH.getMessage("eventIdPdfPCellEventLbl"));
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
	
	public static PdfPCellEvent subjectPdfPCellEvent = new PdfPCellEvent() {
		@Override
		public void cellLayout(PdfPCell cell, Rectangle rectangle, PdfContentByte[] canvases) {
			PdfWriter writer = canvases[0].getPdfWriter();
			TextField text = new TextField(writer, rectangle, "asunto");
			//text.setBorderStyle(PdfBorderDictionary.STYLE_BEVELED);
			text.setText(ACH.getMessage("subjectPdfPCellEvent"));
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
			text.setText(ACH.getMessage("emailCellEventLbl"));
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
			String eventoId, String asuntoManifiesto,
			String emailSolicitante) throws DocumentException, IOException {
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Document document = new Document();
		PdfWriter.getInstance(document, baos);
		document.open();
		PdfPCell cell;
		PdfPTable table = new PdfPTable(2);
		int[] widtshArray = [1, 2].toArray()
		table.setWidths(widtshArray);

		table.addCell(ACH.getMessage("eventIdLbl") + ":");
		cell = new PdfPCell();
		cell.setCellEvent(eventIdPdfPCellEvent);
		table.addCell(cell);
		
		table.addCell(ACH.getMessage("subjectLbl") + ":");
		cell = new PdfPCell();
		cell.setCellEvent(subjectPdfPCellEvent);
		table.addCell(cell);
		
		table.addCell(ACH.getMessage("emailApplicantLbl") + ":");
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
		if(asuntoManifiesto != null) form.setField("asunto", asuntoManifiesto);
		if(emailSolicitante != null) form.setField("email", emailSolicitante);
		if(eventoId != null) form.setField("eventoId", eventoId);
		form.setField("UUID", UUID.randomUUID().toString());
		stamper.close();
		formStamperBaos.close();
		return formStamperBaos.toByteArray();
	}
	
}
