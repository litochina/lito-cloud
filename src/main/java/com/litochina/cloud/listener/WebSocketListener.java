package com.litochina.cloud.listener;

import com.litochina.common.gwserver.server.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author chenxx
 * @date 2020/4/11/011
 */
@Component
@Order(4)
@Slf4j
public class WebSocketListener implements CommandLineRunner {

    @Override
    public void run(String... strings) throws Exception {
//        初始化websocket
        try {
            new WebSocketServer(8880).start();
            log.info("starting websocket service...");
        } catch (Exception e) {
            log.error("WebSocketServer thread error: [{}]", e);
        }
    }
}
