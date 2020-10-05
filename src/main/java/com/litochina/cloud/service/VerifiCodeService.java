package com.litochina.cloud.service;

import com.litochina.base.common.dto.RequestDTO;
import com.litochina.base.model.entity.common.VerifiCode;
import com.litochina.base.persistence.service.BaseService;

public interface VerifiCodeService extends BaseService<VerifiCode> {
	/**
	 * 添加验证码
	 * @param verifiCode
	 * @return
	 */
	public void save(VerifiCode verifiCode);
	/**
	 * 添加验证码
	 * @param requestDTO
	 * @return
	 */
	public VerifiCode selectver(RequestDTO requestDTO);
	
}
