package com.litochina.cloud.listener;

import com.litochina.common.cache.DevBrandInfoCache;
import com.litochina.common.gwserver.sevice.GwDeviceService;
import com.litochina.linkage.cache.LinkageCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author Colde
 * @mail a1345629820@qq.com
 * @date 2019/12/23 16:21
 */
@Component
@Order(1)
@Slf4j
public class CacheListener implements ApplicationRunner {

    @Autowired
    private GwDeviceService gwDeviceService;

    @Autowired
    private LinkageCache linkageCache;

    @Resource
    private DevBrandInfoCache devBrandInfoCache;

    @Override
    public void run(ApplicationArguments args) throws Exception{

        //初始化所有状态数据
        log.info("*********** starting state listener... ***********");
        gwDeviceService.initAllDeviceState();

        linkageCache.initLinkageInfo();
        log.info("*********** initLinkageInfo success ***********");

        devBrandInfoCache.initDevBrandInfo();
        log.info("*********** initDevBrandInfo success ***********");
    }
}
