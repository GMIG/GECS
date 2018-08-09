package org.gmig.gecs.executors;

import org.icmp4j.IcmpPingUtil;

/**
 * Created by brix on 7/5/2018.
 */
public class PingCommandExecutor {
    public PingCommandExecutor() {
        IcmpPingUtil.executePingRequest(IcmpPingUtil.createIcmpPingRequest());

    }

}
