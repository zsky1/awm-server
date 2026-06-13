package com.awm.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_group_member")
public class ChatGroupMember {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String groupId;
    private String agentId;
    private String role;
    private LocalDateTime joinedAt;
}
