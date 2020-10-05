package com.litochina.cloud.controller;

import java.util.List;


import com.litochina.base.common.controller.ApiRespBuilder;
import com.litochina.base.common.dto.RequestDTO;
import com.litochina.base.common.dto.ResponseDTO;
import com.litochina.common.dto.VoiceTimingDTO;
import com.litochina.common.service.SceneService;
import com.litochina.common.service.VoiceControlService;
import com.litochina.job.service.JobModifyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 
 *
 * @author liuhx
 * @date 2019年12月11日
 */
@RestController
@RequestMapping(value = "/api/scenenode")
public class SceneNodeController {

	@Autowired
	private SceneService sceneService;
	@Autowired
	private JobModifyService jobModifyService;
	@Autowired
	private VoiceControlService voicecontrolService;


	/**
	 * 删除场景
	 */
	@PostMapping(path = "/delete")
	public ResponseDTO deleteSceneNode(@RequestBody RequestDTO request) {
		String nodeId = request.getParam("nodeId");
		String sceneId = request.getParam("sceneId");
		String type = request.getParam("type");

		List<VoiceTimingDTO> voiceTimingDTOList = sceneService.deleteSceneNode(nodeId, sceneId, type);
		if (voiceTimingDTOList.size() > 0) {
			for (VoiceTimingDTO voiceTimingDTO : voiceTimingDTOList) {

				request.put("id", voiceTimingDTO.getId());
				if ("xxl_job_info".equals(voiceTimingDTO.getTablename())) {
					jobModifyService.updateJobInfo(request, "remove", "删除任务");
				} else if ("voicecontrol".equals(voiceTimingDTO.getTablename())) {
					voicecontrolService.deleteVoiceControl(request);
				}

			}
		}
		return ApiRespBuilder.success("删除成功");
	}
	
	
}
