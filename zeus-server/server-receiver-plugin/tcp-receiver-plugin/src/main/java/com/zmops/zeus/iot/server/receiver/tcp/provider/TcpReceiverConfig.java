package com.zmops.zeus.iot.server.receiver.tcp.provider;

import com.zmops.zeus.iot.server.library.module.ModuleConfig;
import lombok.Getter;
import lombok.Setter;

/**
 * @author nantian created at 2021/8/14 22:47
 */

@Getter
@Setter
public class TcpReceiverConfig extends ModuleConfig {


    /**
     * Export http port
     */
    private int port = 9020;
}