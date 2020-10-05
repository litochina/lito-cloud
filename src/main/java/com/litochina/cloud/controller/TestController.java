package com.litochina.cloud.controller;

import com.litochina.base.common.controller.ApiRespBuilder;
import com.litochina.base.common.dto.ResponseDTO;
import com.litochina.base.model.entity.tim.VoiceMap;
import com.litochina.base.persistence.timMapper.VoiceMapMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author chenxx
 * @date 2019/9/18/018
 */

@Slf4j
@Controller
@RequestMapping("/api/test")
public class TestController {
    @Resource
    private VoiceMapMapper voiceMapMapper;

    @GetMapping(path = "/show")
    @ResponseBody
    public ResponseDTO test() {
        log.info("saas平台print");

        DigestUtils.md5Hex("123456");
        return ApiRespBuilder.success("我是Saas平台项目");
    }

    @GetMapping(path = "/socket")
    public String index(){
        return "socket";
    }

    @GetMapping(path = "/tim")
    @ResponseBody
    public ResponseDTO test1() {
        List<VoiceMap> dataList = voiceMapMapper.selectAll();
        return ApiRespBuilder.success(dataList);
    }
}
