package com.awm.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MessageSendDTO {
    @NotBlank(message = "消息内容不能为空")
    private String content;
    private String messageType = "text";
}
