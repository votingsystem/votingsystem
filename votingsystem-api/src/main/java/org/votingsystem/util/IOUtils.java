package org.votingsystem.util;

import java.io.*;
import java.util.stream.Collectors;

public class IOUtils {

    private static final int  BUFFER_SIZE = 4096;

    private static final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

    public static String readLine(String s) {
        if(System.console() == null) {
            System.out.print(s);
            try {
                return bufferedReader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        } else {
            return System.console().readLine(s);
        }
    }

    public static char[] readPassword(String s) {
        if(System.console() == null) {
            System.out.print(s);
            try {
                return bufferedReader.readLine().toCharArray();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        } else {
            return System.console().readPassword(s);
        }
    }

    public static int readInt(String s) {
        System.out.print(s);
        try {
            final String text = bufferedReader.readLine();
            return Integer.parseInt(text);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int len;
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
    }

    public static byte[] toByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buf = new byte[BUFFER_SIZE];
        int len;
        while((len = inputStream.read(buf)) > 0){
            outputStream.write(buf,0,len);
        }
        outputStream.close();
        inputStream.close();
        return outputStream.toByteArray();
    }

    public static String toString(BufferedReader reader) {
        return reader.lines().collect(Collectors.joining());
    }

}
