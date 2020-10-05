package com.litochina.cloud.service.impl;



import com.litochina.base.common.dto.RequestDTO;
import com.litochina.base.common.exception.ServiceException;
import com.litochina.base.model.entity.common.VerifiCode;
import com.litochina.base.persistence.mapper.common.VerifiCodeMapper;
import com.litochina.base.persistence.service.impl.BaseServiceImpl;
import com.litochina.cloud.service.VerifiCodeService;
import org.springframework.stereotype.Service;
@Service
public class VerifiCodeServiceImpl extends BaseServiceImpl<VerifiCodeMapper, VerifiCode> implements VerifiCodeService {

	@Override
	public void save(VerifiCode verifiCode) {

		if(!insert(verifiCode)) {
			throw new ServiceException("保存失败");
		}
		return;
	}

	@Override
	public VerifiCode selectver(RequestDTO requestDTO) {
		VerifiCode verifiCode=new VerifiCode();
		verifiCode.setTimekey(requestDTO.get("timekey").toString());
		return selectOne(verifiCode);
		
	}

	
}
