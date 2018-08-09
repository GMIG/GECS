package org.gmig.gecs.device;

/**
 * Created by brix on 4/26/2018.
 */
public enum StandardCommands {
    switchOn("switch on"),
    switchOff("switch off"),
    check("check"),
    init("update state");
    public final String friendlyName;

    StandardCommands(String in_name) {
        friendlyName = in_name;
    }
}
