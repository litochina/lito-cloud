package com.litochina.cloud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author lisy
 * @since 2020/7/23
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MailDTO {

    private String to;
    private String subject;
    private String text;
    private String filePath;

}
