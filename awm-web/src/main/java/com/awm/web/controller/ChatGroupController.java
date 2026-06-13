package com.awm.web.controller;

import com.awm.common.result.PageResult;
import com.awm.common.result.Result;
import com.awm.model.dto.GroupCreateDTO;
import com.awm.model.dto.MessageSendDTO;
import com.awm.model.vo.AgentVO;
import com.awm.model.vo.GroupVO;
import com.awm.model.vo.MessageVO;
import com.awm.service.chat.ChatGroupService;
import com.awm.service.chat.SseManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 群聊协作接口
 */
@RestController
@RequestMapping("/api/chat/groups")
@RequiredArgsConstructor
public class ChatGroupController {

    private final ChatGroupService chatGroupService;
    private final SseManager sseManager;

    /**
     * 查询群列表
     */
    @GetMapping
    public Result<List<GroupVO>> listGroups() {
        return Result.success(chatGroupService.listGroups());
    }

    /**
     * 创建群
     */
    @PostMapping
    public Result<GroupVO> createGroup(@RequestBody @Valid GroupCreateDTO dto) {
        return Result.success(chatGroupService.createGroup(dto));
    }

    /**
     * 获取群成员
     */
    @GetMapping("/{groupId}/members")
    public Result<List<AgentVO>> getMembers(@PathVariable String groupId) {
        return Result.success(chatGroupService.getGroupMembers(groupId));
    }

    /**
     * 获取消息历史
     */
    @GetMapping("/{groupId}/messages")
    public Result<PageResult<MessageVO>> getMessages(
            @PathVariable String groupId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return Result.success(chatGroupService.getMessages(groupId, page, size));
    }

    /**
     * 发送消息
     */
    @PostMapping("/{groupId}/messages")
    public Result<MessageVO> sendMessage(@PathVariable String groupId,
                                          @RequestBody @Valid MessageSendDTO dto) {
        return Result.success(chatGroupService.sendMessage(groupId, dto));
    }

    /**
     * SSE 流式订阅
     */
    @GetMapping(value = "/{groupId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String groupId) {
        return sseManager.addEmitter(groupId);
    }
}
