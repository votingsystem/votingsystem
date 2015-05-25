package org.votingsystem.web.currency.servlet;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.web.currency.util.QRUtils;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@WebServlet("/qr")
public class QRServlet extends HttpServlet {

    private final static Logger log = Logger.getLogger(QRServlet.class.getSimpleName());


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse resp)
            throws ServletException, IOException {
        processRequest(request, resp);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse resp)
            throws ServletException, IOException {
        processRequest(request, resp);
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, IOException {
        try {
            QRUtils.ChartServletRequestParameters qrRequest = QRUtils.parseRequest(req, "POST".equals(req.getMethod()));
            BitMatrix matrix = new QRCodeWriter().encode(qrRequest.getText(), BarcodeFormat.QR_CODE,
                    qrRequest.getWidth(), qrRequest.getHeight(), qrRequest.getHints());
            ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", pngOut);
            byte[] pngData = pngOut.toByteArray();
            resp.setContentType("image/png");
            resp.setContentLength(pngData.length);
            resp.setHeader("Cache-Control", "public");
            resp.getOutputStream().write(pngData);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            resp.setStatus(ResponseVS.SC_ERROR_REQUEST);
            String message = ex.getMessage() != null ? ex.getMessage(): "EXCEPTION: " + ex.getClass();
            resp.getOutputStream().write(message.getBytes());
        }
    }

}