package com.litochina.cloud.service.impl;

import com.litochina.base.common.common.ReturnCode;
import com.litochina.base.common.controller.ApiRespBuilder;
import com.litochina.base.common.dto.ResponseDTO;
import com.litochina.cloud.dto.MailDTO;
import com.litochina.cloud.service.MailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.File;

@Slf4j
@Service
public class MailServiceImpl implements MailService {

    @Resource
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Override
    public ResponseDTO sendSimpleMail(MailDTO dto) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(dto.getTo());
        message.setSubject(dto.getSubject());
        message.setText(dto.getText());
        try {
            mailSender.send(message);
            return ApiRespBuilder.success("邮件已发送");
        } catch (Exception e) {
            log.error("发送邮件时发生异常！", e);
            return ApiRespBuilder.error(ReturnCode.SERVICE_ERROR, "邮件发送失败");
        }
    }

    @Override
    public ResponseDTO sendHtmlMail(MailDTO dto) {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper messageHelper;
        try {
            messageHelper = new MimeMessageHelper(message, true);
            //邮件发送人
            messageHelper.setFrom(from);
            //邮件接收人
            messageHelper.setTo(dto.getTo());
            //邮件主题
            messageHelper.setSubject(dto.getSubject());
            //邮件内容，html格式
            messageHelper.setText(dto.getText(), true);
            //发送
            mailSender.send(message);
            //日志信息
            log.info("邮件已发送");
            return ApiRespBuilder.success("邮件已发送");
        } catch (MessagingException e) {
            log.error("发送邮件时发生异常！", e);
            return ApiRespBuilder.error(ReturnCode.SERVICE_ERROR, "邮件发送失败");
        }
    }

    @Override
    public ResponseDTO sendAttachmentsMail(MailDTO dto) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(from);
            helper.setTo(dto.getTo());
            helper.setSubject(dto.getSubject());
            helper.setText(dto.getText(), true);

            FileSystemResource file = new FileSystemResource(new File(dto.getFilePath()));
            String fileName = dto.getFilePath().substring(dto.getFilePath().lastIndexOf(File.separator));
            helper.addAttachment(fileName, file);
            mailSender.send(message);
            //日志信息
            log.info("邮件已发送");
            return ApiRespBuilder.success("邮件已发送");
        } catch (MessagingException e) {
            log.error("发送邮件时发生异常！", e);
            return ApiRespBuilder.error(ReturnCode.SERVICE_ERROR, "邮件发送失败");
        }
    }
}
