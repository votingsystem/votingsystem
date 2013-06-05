/*
 * This class is part of the book "iText in Action - 2nd Edition"
 * written by Bruno Lowagie (ISBN: 9781935182610)
 * For more info, go to: http://itextpdf.com/examples/
 * This example only works with the AGPL version of iText.
 */

package pruebas;

import org.sistemavotacion.pdf.*;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.ExceptionConverter;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.AcroFields.Item;
import com.itextpdf.text.pdf.PdfBorderDictionary;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfFormField;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPCellEvent;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.TextField;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class PDFTextFieldsTest implements PdfPCellEvent {

    /** The text field index of a TextField that needs to be added to a cell. */
    protected int tf;
    private Map<String, Item> fields;

    /**
     * Creates a cell event that will add a text field to a cell.
     * @param tf a text field index.
     */
    public PDFTextFieldsTest(int tf) {
        this.tf = tf;
    }

    /**
     * Manipulates a PDF file src with the file dest as result
     * @param src the original PDF
     * @param dest the resulting PDF
     * @throws IOException
     * @throws DocumentException
     */
    public void manipulatePdf(String src, String dest) throws IOException, DocumentException {
        PdfReader reader = new PdfReader(src);
        PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(dest));
        AcroFields form = stamper.getAcroFields();
        
        fields = form.getFields();
        
        Set<String> keysFields = fields.keySet();
        for(String key : keysFields) {
            System.out.println("key: " + key);
        }
        
        form.setField("asunto", "Contenido del campo asunto");
        form.setField("email", "as@free.com");
        
        /*form.setFieldProperty("text_2", "fflags", 0, null);
        form.setFieldProperty("text_2", "bordercolor", BaseColor.RED, null);
        form.setField("text_2", "bruno");
        form.setFieldProperty("text_3", "clrfflags", TextField.PASSWORD, null);
        form.setFieldProperty("text_3", "setflags", PdfAnnotation.FLAGS_PRINT, null);
        form.setField("text_3", "12345678", "xxxxxxxx");
        form.setFieldProperty("text_4", "textsize", new Float(12), null);
        form.regenerateField("text_4");*/
        stamper.close();
    }

    /**
     * Creates a PDF document.
     * @param filename the path to the new PDF document
     * @throws    DocumentException 
     * @throws    IOException 
     */
    public void createPdf(String filename) throws DocumentException, IOException {
    	// step 1
        Document document = new Document();
        // step 2
        PdfWriter.getInstance(document, new FileOutputStream(filename));
        // step 3
        document.open();
        // step 4
        PdfPCell cell;
        PdfPTable table = new PdfPTable(2);
        table.setWidths(new int[]{ 1, 2 });

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
    
    PdfPCellEvent asuntoPdfPCellEvent = new PdfPCellEvent() {
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
    
    PdfPCellEvent emailCellEvent = new PdfPCellEvent() {
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


    public void cellLayout(PdfPCell cell, Rectangle rectangle, PdfContentByte[] canvases) {
        PdfWriter writer = canvases[0].getPdfWriter();
        TextField text = new TextField(
                writer, rectangle, String.format("text_%s", tf));
        //text.setBackgroundColor(new GrayColor(0.75f));
        switch(tf) {
        case 1:
            text.setBorderStyle(PdfBorderDictionary.STYLE_BEVELED);
            text.setText("Enter your name here...");
            text.setFontSize(0);
            text.setAlignment(Element.ALIGN_CENTER);
            text.setOptions(TextField.REQUIRED);
            break;
        case 2:
            text.setMaxCharacterLength(8);
            text.setOptions(TextField.COMB);
            text.setBorderStyle(PdfBorderDictionary.STYLE_SOLID);
            text.setBorderColor(BaseColor.BLUE);
            text.setBorderWidth(2);
            break;
        case 3:
            text.setBorderStyle(PdfBorderDictionary.STYLE_INSET);
            text.setOptions(TextField.PASSWORD);
            text.setVisibility(TextField.VISIBLE_BUT_DOES_NOT_PRINT);
            break;
        case 4:
            text.setBorderStyle(PdfBorderDictionary.STYLE_DASHED);
            text.setBorderColor(BaseColor.RED);
            text.setBorderWidth(2);
            text.setFontSize(8);
            text.setText("Introduzca el mensaje que quiera transmitir");
            text.setOptions(TextField.MULTILINE | TextField.REQUIRED);
            break;
        }
        try {
            PdfFormField field = text.getTextField();
            if (tf == 3) {
                field.setUserName("Choose a password");
            }
            writer.addAnnotation(field);
        }
        catch(IOException ioe) {
            throw new ExceptionConverter(ioe);
        }
        catch(DocumentException de) {
            throw new ExceptionConverter(de);
        }
    }

    /**
     * Main method
     * @param args no arguments needed
     * @throws IOException
     * @throws DocumentException
     */
    public static void main(String[] args) throws DocumentException, IOException {
        PDFTextFieldsTest example = new PDFTextFieldsTest(0);
            /** The resulting PDF. */
        String RESULT1 = "text_fields.pdf";
        /** The resulting PDF. */
        String RESULT2 = " text_filled.pdf";
        example.createPdf(RESULT1);
        example.manipulatePdf(RESULT1, RESULT2);
    }

}
