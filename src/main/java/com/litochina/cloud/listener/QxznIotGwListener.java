package com.litochina.cloud.listener;
import com.litochina.base.common.utils.SystemConfigUtil;
import com.litochina.common.gwserver.server.QxznIotGwServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author chenxx
 * @date 2019/11/6/006
 */
@Component
@Order(2)
public class QxznIotGwListener implements CommandLineRunner {
    private static Logger LOGGER = LoggerFactory.getLogger(QxznIotGwServer.class);

    @Value("${gateway.port:8889}")
    private String gwPort;

    @Override
    public void run(String... strings) {
        LOGGER.info("QxznIotGwListener starting...");
        String certPath = SystemConfigUtil.getProperty("cert.jksPath");
        new QxznIotGwServer(Integer.parseInt(gwPort), certPath).run();
    }
}
