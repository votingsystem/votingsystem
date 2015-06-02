package org.votingsystem.web.currency.util;


import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharStreams;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.servlet.ServletRequest;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class QRUtils {

    public static final Integer MARGIN = 10;
    private static final int MAX_DIMENSION = 4096;
    private static final Collection<Charset> SUPPORTED_OUTPUT_ENCODINGS = ImmutableSet.<Charset>builder()
            .add(StandardCharsets.UTF_8).add(StandardCharsets.ISO_8859_1).add(Charset.forName("Shift_JIS")).build();


    public static ChartServletRequestParameters parseRequest(ServletRequest request, boolean readBody)
            throws IOException {
        Preconditions.checkArgument("qr".equals(request.getParameter("cht")), "Bad type");
        String widthXHeight = request.getParameter("chs");
        Preconditions.checkNotNull(widthXHeight, "No size");
        int xIndex = widthXHeight.indexOf('x');
        Preconditions.checkArgument(xIndex >= 0, "Bad size");
        int width = Integer.parseInt(widthXHeight.substring(0, xIndex));
        int height = Integer.parseInt(widthXHeight.substring(xIndex + 1));
        Preconditions.checkArgument(width > 0 && height > 0, "Bad size");
        Preconditions.checkArgument(width <= MAX_DIMENSION && height <= MAX_DIMENSION, "Bad size");
        String outputEncodingName = request.getParameter("choe");
        Charset outputEncoding = StandardCharsets.UTF_8;
        if (outputEncodingName != null) {
            outputEncoding = Charset.forName(outputEncodingName);
            Preconditions.checkArgument(SUPPORTED_OUTPUT_ENCODINGS.contains(outputEncoding), "Bad output encoding");
        }
        ErrorCorrectionLevel ecLevel = ErrorCorrectionLevel.L;
        int margin = 4;
        String ldString = request.getParameter("chld");
        if (ldString != null) {
            int pipeIndex = ldString.indexOf('|');
            if (pipeIndex < 0) {
                // Only an EC level
                ecLevel = ErrorCorrectionLevel.valueOf(ldString);
            } else {
                ecLevel = ErrorCorrectionLevel.valueOf(ldString.substring(0, pipeIndex));
                margin = Integer.parseInt(ldString.substring(pipeIndex + 1));
                Preconditions.checkArgument(margin > 0, "Bad margin");
            }
        }
        String text;
        if (readBody) {
            text = CharStreams.toString(request.getReader());
        } else {
            text = request.getParameter("chl");
        }
        Preconditions.checkArgument(text != null && !text.isEmpty(), "No input");

        Map<EncodeHintType,Object> hints = new EnumMap<EncodeHintType,Object>(EncodeHintType.class);
        hints.put(EncodeHintType.MARGIN, MARGIN);
        if (!StandardCharsets.ISO_8859_1.equals(outputEncoding)) {
            // Only set if not QR code default
            hints.put(EncodeHintType.CHARACTER_SET, outputEncoding.name());
        }
        hints.put(EncodeHintType.ERROR_CORRECTION, ecLevel);
        return new ChartServletRequestParameters(width, height, outputEncoding, ecLevel, margin, text, hints);
    }


    public static class ChartServletRequestParameters {

        private final int width;
        private final int height;
        private final Charset outputEncoding;
        private final ErrorCorrectionLevel ecLevel;
        private final int margin;
        private final String text;
        private  Map<EncodeHintType,Object> hints;

        ChartServletRequestParameters(int width, int height, Charset outputEncoding, ErrorCorrectionLevel ecLevel,
                  int margin, String text, Map<EncodeHintType,Object> hints) {
            this.width = width;
            this.height = height;
            this.outputEncoding = outputEncoding;
            this.ecLevel = ecLevel;
            this.margin = margin;
            this.text = text;
            this.hints = hints;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        Charset getOutputEncoding() {
            return outputEncoding;
        }

        ErrorCorrectionLevel getEcLevel() {
            return ecLevel;
        }

        public int getMargin() {
            return margin;
        }

        public String getText() {
            return text;
        }

        public Map<EncodeHintType, Object> getHints() {
            return hints;
        }

        public void setHints(Map<EncodeHintType, Object> hints) {
            this.hints = hints;
        }
    }
}



