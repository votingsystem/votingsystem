package org.votingsystem.client.webextension;

import org.votingsystem.util.DateUtils;
import org.votingsystem.util.JSON;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.util.logging.*;

public class BrowserHost {

    private static Logger log = Logger.getLogger(BrowserHost.class.getSimpleName());

    private static final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));


    static public void main(String[] args) throws URISyntaxException, IOException, InterruptedException {
        FileHandler fh = new FileHandler("./BrowserExetensionHost.log");
        SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);
        log.addHandler(fh);
        log.info("waiting for browser messages");
        while(true) {
            ByteBuffer buf = ByteBuffer.allocate(1000000);
            System.in.available();
            ReadableByteChannel channel = Channels.newChannel(System.in);
            channel.read(buf);
            buf.flip();
            byte[] resultBytes = buf.array();
            byte[] bytes = Arrays.copyOfRange(resultBytes, 4, buf.limit());
            processMessageFromBrowser(bytes);
        }
    }

    private static void processMessageFromBrowser(byte[] messageBytes) throws IOException{
        log.info("processMessageFromBrowser: " + new String(messageBytes));

        sendMessageToBrowser(new HashMap<>());
    }

    private static void sendMessageToBrowser(Object messageToBrowser) throws IOException{
        log.info("sendMessageToBrowser");
        try {
            Map<String,String> map = new HashMap<String, String>();
            String msg = new Date() + " - España con acentuación";
            map.put("message", msg);
            map.put("UUID", UUID.randomUUID().toString());
            String base64msg = Base64.getEncoder().encodeToString(JSON.getMapper().writeValueAsBytes(messageToBrowser) );
            map.put("native_message", base64msg);
            byte[] messageBytes = JSON.getMapper().writeValueAsBytes(map);
            System.out.write(getBytes(messageBytes.length));
            System.out.write(messageBytes);
            System.out.flush();
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public static byte[] getBytes(int length) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) ( length      & 0xFF);
        bytes[1] = (byte) ((length>>8)  & 0xFF);
        bytes[2] = (byte) ((length>>16) & 0xFF);
        bytes[3] = (byte) ((length>>24) & 0xFF);
        return bytes;
    }

}