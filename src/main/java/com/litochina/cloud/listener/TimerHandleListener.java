package com.litochina.cloud.listener;

import com.litochina.common.gwserver.handler.TimerHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author Colde
 * @mail a1345629820@qq.com
 * @date 2020/2/26 16:28
 */
@Component
@Order(3)
@Slf4j
public class TimerHandleListener implements ApplicationRunner {

    @Autowired
    private TimerHandler timerHandler;

    @Override
    public void run(ApplicationArguments applicationArguments) throws Exception {
        timerHandler.sfStateTimer();
        log.info("*********** TimerHandler success ***********");
    }

    //初始化websocket
//        try {
//            new WebSocketServer(8880).start();
//            LOGGER.info("starting websocket service...");
//        } catch (Exception e) {
//            LOGGER.error("WebSocketServer thread error: [{}]", e);
//        }
}
