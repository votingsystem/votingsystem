package org.votingsystem.cooin.controller

import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.BarcodeFormat
import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.cooin.util.QRUtils
import org.votingsystem.model.ResponseVS

import java.nio.charset.StandardCharsets;

class QRController {

    def index() {

    }

    //http://cooins:8086/Cooins/QR/test?cht=qr&chs=200x200&chl=operation=TRANSACTION_TO_USERVS;amount=100_eur_WILDTAG;URL=https://cooins:8086/Cooins/
    def test() {
        QRUtils.ChartServletRequestParameters qrRequest = QRUtils.parseRequest(request, "POST".equals(request.method))
        BitMatrix matrix = new QRCodeWriter().encode(qrRequest.getText(), BarcodeFormat.QR_CODE,
                qrRequest.getWidth(), qrRequest.getHeight(), qrRequest.getHints());
        ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", pngOut);
        byte[] pngData = pngOut.toByteArray();
        response.setContentType("image/png");
        response.setContentLength(pngData.length);
        response.setHeader("Cache-Control", "public");
        response.getOutputStream().write(pngData);
    }

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }
}
