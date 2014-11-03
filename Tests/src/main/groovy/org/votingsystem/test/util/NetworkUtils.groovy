package org.votingsystem.test.util

import org.apache.log4j.Logger


class NetworkUtils {

    private static Logger log = Logger.getLogger(NetworkUtils.class);

    private NetworkUtils() { }

    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface .getNetworkInterfaces(); en.hasMoreElements();) {
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

    public static String getIpAddress() {
        try {
            Socket socket = new Socket("www.sistemavotacion.org", 80);
            log.debug("main local ip:: " + socket.getLocalAddress().getHostAddress());

            String computername = InetAddress.getLocalHost().getHostName();
            log.debug ("computername: " + computername + " - InetAddress: " + InetAddress.getLocalHost().getHostAddress());

            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if(inetAddress instanceof Inet4Address) {
                        if (inetAddress.isLoopbackAddress()) log.debug("LoopbackAddress: " + inetAddress.getHostAddress().toString())
                        else {
                            //log.debug("inetAddress: " + inetAddress.getHostAddress().toString())
                            log.debug ("addr.getHostAddress() = " + inetAddress.getHostAddress());
                            log.debug ("addr.getHostName() = " + inetAddress.getHostName());
                            log.debug ("addr.isAnyLocalAddress() = " + inetAddress.isAnyLocalAddress());
                            log.debug ("addr.isLinkLocalAddress() = " + inetAddress.isLinkLocalAddress());
                            log.debug ("addr.isLoopbackAddress() = " + inetAddress.isLoopbackAddress());
                            log.debug ("addr.isMulticastAddress() = " + inetAddress.isMulticastAddress());
                            log.debug ("addr.isSiteLocalAddress() = " + inetAddress.isSiteLocalAddress());
                            log.debug ("");
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

    public static void main(String[] args) {
        Logger log = TestUtils.init(NetworkUtils.class, [:])
        getIpAddress()
    }

}