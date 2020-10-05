package com.litochina.cloud.service;

import com.litochina.base.common.dto.ResponseDTO;
import com.litochina.cloud.dto.MailDTO;

public interface MailService {

    /**
     * 发送文本邮件
     * @param dto
     * @return
     */
    ResponseDTO sendSimpleMail(MailDTO dto);

    /**
     * 发送html邮件
     * @param dto
     * @return
     */
    ResponseDTO sendHtmlMail(MailDTO dto);

    /**
     * 发送带附件的邮件
     * @param dto
     * @return
     */
    ResponseDTO sendAttachmentsMail(MailDTO dto);
}
