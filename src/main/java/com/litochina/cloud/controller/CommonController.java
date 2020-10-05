package com.litochina.cloud.controller;

import com.litochina.base.common.annotation.RequestDTORequired;
import com.litochina.base.common.common.BaseConstant;
import com.litochina.base.common.common.ReturnCode;
import com.litochina.base.common.controller.ApiRespBuilder;
import com.litochina.base.common.controller.CurrentUserContext;
import com.litochina.base.common.dto.RequestDTO;
import com.litochina.base.common.dto.ResponseDTO;
import com.litochina.base.common.exception.ServiceException;
import com.litochina.base.common.utils.RedisUtils;
import com.litochina.base.model.dto.subcommon.NodeDeleteListDTO;
import com.litochina.base.model.entity.common.UploadFile;
import com.litochina.base.model.entity.device.DeviceTreeUser;
import com.litochina.base.model.entity.manage.SystemMenu;
import com.litochina.base.model.entity.voice.VoiceControl;
import com.litochina.base.persistence.mapper.ProcedureMapper;
import com.litochina.base.persistence.mapper.manage.SystemMenuMapper;
import com.litochina.base.service.service.FileUploadService;
import com.litochina.common.service.SubCommonService;
import com.litochina.linkage.service.LinkageService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author chenxx
 * @date 2019/12/17/017
 */
@RestController
@RequestMapping("/api/common")
public class CommonController {

    @Resource
    private FileUploadService fileService;

    @Resource
    private SubCommonService subCommonService;

    @Resource
    private ProcedureMapper procedureMapper;

    @Resource
    private LinkageService linkService;

    @Resource
    private SystemMenuMapper menuMapper;

    @PostMapping("/file/path")
    public ResponseDTO getZipFilePath() {
        String userId = CurrentUserContext.checkIsSubAndGetMainUserId();
        List<DeviceTreeUser> treeList = procedureMapper.getDeviceTreeByUserId(userId);

        String url = "";
        if (treeList.size() > 0) {
            UploadFile file = fileService.findFileByRelatedId(treeList.get(0).getTreeId());
            if (null != file) {
                url = file.getFilePath();
            }
        }
        return ApiRespBuilder.success("查询根节点图形文件路径成功", url);
    }

    @PostMapping(path = "/node/delete")
    @RequestDTORequired({"nodeId"})
    public ResponseDTO deleteNode(@RequestBody RequestDTO request) {
        String nodeId = request.getParam("nodeId");
        if (null == nodeId) {
            throw new ServiceException(ReturnCode.PARAM_REQUIRED, ReturnCode.PARAM_REQUIRED.getMsg() + ": nodeId");
        }

        NodeDeleteListDTO result = subCommonService.linkDelete(nodeId, "", true);
        //刷新联动数据
        if (null != result.getLinkList() && result.getLinkList().size() > 0) {
            result.getLinkList().forEach(link -> linkService.delDeviceRelation(link));
        }
        return ApiRespBuilder.success("节点及其所有关联数据删除成功");
    }

    @PostMapping(path = "/page/change")
    public ResponseDTO routeChange() {
        String userId = CurrentUserContext.checkLoginAndGetUserId();
        VoiceControl voice = RedisUtils.get(userId + BaseConstant.VOICE_CONTROL, VoiceControl.class);
        if (null == voice) {
            return ApiRespBuilder.error(ReturnCode.NOTHING_TO_DO, "不存在需要执行页面跳转的语音指令");
        }
        RedisUtils.del(userId + BaseConstant.VOICE_CONTROL);
        return ApiRespBuilder.success(voice);
    }

    @GetMapping(path = "/test/page/change")
    public ResponseDTO testRouteChange() {
        List<SystemMenu> menuList = menuMapper.selectAll();
        return ApiRespBuilder.success(menuList);
    }
}
