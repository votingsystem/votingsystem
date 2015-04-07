package org.votingsystem.test.util;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.logging.Logger;


class NetworkUtils {

    private static Logger log = Logger.getLogger(NetworkUtils.class.getSimpleName());

    private NetworkUtils() { }

    public static String getLocalIpAddress() throws SocketException {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static String getIpAddress() throws IOException {
        try {
            Socket socket = new Socket("www.sistemavotacion.org", 80);
            log.info("main local ip:: " + socket.getLocalAddress().getHostAddress());

            String computername = InetAddress.getLocalHost().getHostName();
            log.info ("computername: " + computername + " - InetAddress: " + InetAddress.getLocalHost().getHostAddress());

            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if(inetAddress instanceof Inet4Address) {
                        if (inetAddress.isLoopbackAddress()) log.info("LoopbackAddress: " +
                                inetAddress.getHostAddress().toString());
                        else {
                            //log.info("inetAddress: " + inetAddress.getHostAddress().toString())
                            log.info ("addr.getHostAddress() = " + inetAddress.getHostAddress());
                            log.info ("addr.getHostName() = " + inetAddress.getHostName());
                            log.info ("addr.isAnyLocalAddress() = " + inetAddress.isAnyLocalAddress());
                            log.info ("addr.isLinkLocalAddress() = " + inetAddress.isLinkLocalAddress());
                            log.info ("addr.isLoopbackAddress() = " + inetAddress.isLoopbackAddress());
                            log.info ("addr.isMulticastAddress() = " + inetAddress.isMulticastAddress());
                            log.info ("addr.isSiteLocalAddress() = " + inetAddress.isSiteLocalAddress());
                            log.info ("");
                        }
                        /*if (!inetAddress.isLoopbackAddress()) {
                            return inetAddress.getHostAddress().toString();
                        }*/
                    }

                }
            }
        } catch (SocketException e) {
            // Log.e(Constants.LOG_TAG, e.getMessage(), e);
        }
        return null;
    }

    /* ANROID ->
    public static boolean connectionPresent(final ConnectivityManager cMgr) {
        if (cMgr != null) {
            NetworkInfo netInfo = cMgr.getActiveNetworkInfo();
            if ((netInfo != null) && (netInfo.getState() != null)) {
                return netInfo.getState().equals(State.CONNECTED);
            } else {
                return false;
            }
        }
        return false;
    }*/

    public static void main(String[] args) throws Exception {
        Logger log = TestUtils.init(NetworkUtils.class, new SimulationData());
        getIpAddress();
    }

}