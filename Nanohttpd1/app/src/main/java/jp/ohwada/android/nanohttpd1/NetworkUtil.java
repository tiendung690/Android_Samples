/**
 * NanoHttpd Sample
 * 2019-02-01 K.OHWADA
 */


package jp.ohwada.android.nanohttpd1;

import android.util.Log;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
	 * class NetworkUtil
	 */ 
public class NetworkUtil {

    private static final String IP_ADDR_DEFAULT = "0.0.0.0";

/**
 * getIPAddress
 */ 
public static String getMyIPAddress() {
    String addr = null;
    try {
            addr = getIpAddress();
	} catch (Exception e) {
        e.printStackTrace();
	}
    return addr;
} //  getIPAddress

/**
 * getIpAddress
 */ 
public static String getIpAddress() throws Exception{

    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
    
  while( interfaces.hasMoreElements() ) {
        NetworkInterface intf = interfaces.nextElement();
        Enumeration<InetAddress> addresses = intf.getInetAddresses();

    while( addresses.hasMoreElements() ) {
        InetAddress addr = addresses.nextElement();

        if (!addr.isLoopbackAddress() ) {
            String h_addr = addr.getHostAddress();
            if ( isIPv4(h_addr) &&  ! IP_ADDR_DEFAULT.equals(h_addr) ) {
                return h_addr;
            } // if
        } // if

      } // while
    } // while
    return null;
} // etIPAddress

/**
 * isIPv4
 */ 
public static boolean isIPv4(String addr) {
    if( addr.indexOf(':') < 0) {
            return true;
    }
    return false;
} // isIPv4

} //  class NetworkUtil