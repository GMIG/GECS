package org.gmig.gecs.executors;

import org.gmig.gecs.command.Command;
import org.apache.log4j.Logger;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioDatagramConnector;
import org.icmp4j.IcmpPingRequest;
import org.icmp4j.IcmpPingResponse;
import org.icmp4j.IcmpPingUtil;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class WOLCommandExecutor extends IoHandlerAdapter {
    private static final Logger logger = Logger.getLogger(WOLCommandExecutor.class);
    private int subnetLevel = 2;
    private int ipResendTimeMillis = 5000;
    private int ipResendTries = 5;

    public int getSubnetLevel() {return subnetLevel;}
    public void setSubnetLevel(int subnetLevel) {this.subnetLevel = subnetLevel;}
    public int getIpResendTimeMillis() {return ipResendTimeMillis;}
    public void setIpResendTimeMillis(int ipResendTimeMillis) {this.ipResendTimeMillis = ipResendTimeMillis;}
    public int getIpResendTries() {return ipResendTries;}
    public void setIpResendTries(int ipResendTries) {this.ipResendTries = ipResendTries;}



    public WOLCommandExecutor() {
        connector.setHandler(this);
        connector.getSessionConfig().setReuseAddress(false);
    }

    private NioDatagramConnector connector = new NioDatagramConnector(50);

    public Command<Void> getCommand(String magicPacketAddressStr, int magicPacketPort, String mac) {
        return ()->sendWAL( magicPacketAddressStr,  magicPacketPort,  mac);
    }

    public CompletableFuture<Void> sendWAL(String magicPacketAddressStr, int magicPacketPort, String mac) {
        CompletableFuture<Void> sendWOLFuture = new CompletableFuture<>();
        try {
            InetAddress magicPacketAddress = InetAddress.getByName(magicPacketAddressStr);
            byte[] address = magicPacketAddress.getAddress();
            for (int i = 3; i > 3 - subnetLevel; i--)
                address[i] = (byte) 255;
            final byte[] bytes = WOLPacketHelper.WOLPacket(mac);
            ConnectFuture connFuture = connector.connect(new InetSocketAddress(InetAddress.getByAddress(address), magicPacketPort));
            //connFuture.awaitUninterruptibly();
            connFuture.addListener(new IoFutureListener<IoFuture>() {
                @Override
                public void operationComplete(IoFuture future) {
                    // We have connected - sending WOL
                    ConnectFuture infuture= (ConnectFuture) future;
                    if (infuture.isConnected()) {
                        IoSession session = infuture.getSession();
                        session.suspendRead();
                        SHelper.setFuture(session,sendWOLFuture);
                        SHelper.setIP(session,magicPacketAddressStr);
                        WriteFuture wf = session.write(IoBuffer.wrap(bytes));
                        AtomicInteger counter = new AtomicInteger(0);
                        wf.addListener(new IoFutureListener<IoFuture>() {
                            // We sent a packet and wait for PING
                            @Override
                            public void operationComplete(IoFuture future) {
                                WriteFuture inwf = (WriteFuture) future;
                                IoSession s = future.getSession();
                               // try {
                                IcmpPingRequest req = IcmpPingUtil.createIcmpPingRequest();
                                req.setHost(magicPacketAddress.getHostAddress());
                                req.setTimeout(ipResendTimeMillis);
                                IcmpPingResponse resp = IcmpPingUtil.executePingRequest(req);
                                    if (resp.getSuccessFlag()) {
                                        logger.info(magicPacketAddress + ":Received reply after WOL");
                                        sendWOLFuture.complete(null);
                                    } else if (counter.incrementAndGet() > ipResendTries) {
                                        logger.debug(magicPacketAddress + ":Not responding to WOL");
                                        sendWOLFuture.completeExceptionally(new Throwable(magicPacketAddress + ":Not responding to WOL"));
                                    } else {
                                        logger.debug(magicPacketAddress + ":WOL Retry " + counter.get());
                                        if(resp.getDuration() < ipResendTimeMillis)
                                            try {
                                                Thread.sleep(ipResendTimeMillis - resp.getDuration());
                                            } catch (InterruptedException e) {
                                                sendWOLFuture.completeExceptionally(e);
                                            }
                                        s.write(IoBuffer.wrap(bytes)).addListener(this);
                                    }
                              //  } catch (IOException e) {
                              //      sendWOLFuture.completeExceptionally(e);
                              //  }
                            }
                        });
                    } else {
                        logger.warn(magicPacketAddress + ":Unable to connect to WAL port ");
                        sendWOLFuture.completeExceptionally(new Throwable(magicPacketAddress + ":Unable to connect to WAL port "));
                    }
                }
            });
        } catch (UnknownHostException e) {
            sendWOLFuture.completeExceptionally(e);
        }
        return sendWOLFuture;
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) {
        logger.warn(SHelper.getIP(session)+":Apache MINA error " + cause);
        Throwable wrappedIP = new Throwable(SHelper.getIP(session)+":Apache MINA error "+cause,cause);
        SHelper.getFuture(session).completeExceptionally(wrappedIP);
    }


}
