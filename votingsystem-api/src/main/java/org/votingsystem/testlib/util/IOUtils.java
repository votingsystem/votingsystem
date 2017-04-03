package org.votingsystem.testlib.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class IOUtils {

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
}
