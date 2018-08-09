package org.gmig.gecs.executors;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by brix isOn 2/22/2018.
 */
public final class WOLPacketHelper {

    private static final char SEPARATOR = ':';

    public static String cleanMac(String mac) throws IllegalArgumentException
    {
        final String[] hex = validateMac(mac);
        StringBuffer sb = new StringBuffer();
        boolean isMixedCase = false;
        // delayedCheck for mixed case
        for(int i=0; i<6; i++) {
            sb.append(hex[i]);
        }
        String testMac = sb.toString();
        if((!testMac.toLowerCase().equals(testMac)) && (!testMac.toUpperCase().equals(testMac))) {
            isMixedCase = true;
        }
        sb = new StringBuffer();
        for(int i=0; i<6; i++) {
            // convert mixed case to lower
            if(isMixedCase) {
                sb.append(hex[i].toLowerCase());
            }else{
                sb.append(hex[i]);
            }
            if(i < 5) {
                sb.append(SEPARATOR);
            }
        }
        return sb.toString();
    }

    private static String[] validateMac(String mac) throws IllegalArgumentException
    {
        // error handle semi colons
        mac = mac.replace(";", ":");
        // attempt to assist the user a little
        String newMac = "";
        if(mac.matches("([a-zA-Z0-9]){12}")) {
            // expand 12 chars into a valid mac address
            for(int i=0; i<mac.length(); i++){
                if((i > 1) && (i % 2 == 0)) {
                    newMac += ":";
                }
                newMac += mac.charAt(i);
            }
        }else{
            newMac = mac;
        }
        // regexp pattern match a valid MAC address
        final Pattern pat = Pattern.compile("((([0-9a-fA-F]){2}[-:]){5}([0-9a-fA-F]){2})");
        final Matcher m = pat.matcher(newMac);

        if(m.find()) {
            String result = m.group();
            return result.split("(\\:|\\-)");
        }else{
            throw new IllegalArgumentException("Invalid MAC address");
        }
    }

    public static byte[] WOLPacket(String mac) throws IllegalArgumentException{
        String myMacAddr = cleanMac(mac);
        String[] macAddrHex = validateMac(myMacAddr);
        // convert to base16 bytes
        final byte[] macBytes = new byte[6];
        for(int i=0; i<6; i++) {
            macBytes[i] = (byte) Integer.parseInt(macAddrHex[i], 16);
        }
        final byte[] bytes = new byte[102];
        // fill first 6 bytes
        for(int i=0; i<6; i++) {
            bytes[i] = (byte) 0xff;
        }
        // fill remaining bytes with target MAC
        for(int i=6; i<bytes.length; i+=macBytes.length) {
            System.arraycopy(macBytes, 0, bytes, i, macBytes.length);
        }
        return bytes;
    }

}
