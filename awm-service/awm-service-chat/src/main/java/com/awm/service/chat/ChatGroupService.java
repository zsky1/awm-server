package com.awm.service.chat;

import com.awm.common.result.PageResult;
import com.awm.dal.mapper.AgentMapper;
import com.awm.dal.mapper.ChatGroupMapper;
import com.awm.dal.mapper.ChatGroupMemberMapper;
import com.awm.model.dto.GroupCreateDTO;
import com.awm.model.dto.MessageSendDTO;
import com.awm.model.entity.Agent;
import com.awm.model.entity.ChatGroup;
import com.awm.model.entity.ChatGroupMember;
import com.awm.model.entity.Message;
import com.awm.model.vo.AgentVO;
import com.awm.model.vo.GroupVO;
import com.awm.model.vo.MessageVO;
import org.springframework.context.ApplicationEventPublisher;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 群聊协作服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatGroupService {

    private final ChatGroupMapper chatGroupMapper;
    private final ChatGroupMemberMapper chatGroupMemberMapper;
    private final AgentMapper agentMapper;
    private final MessageService messageService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 查询群列表
     */
    public List<GroupVO> listGroups() {
        List<ChatGroup> groups = chatGroupMapper.selectList(
                new LambdaQueryWrapper<ChatGroup>().orderByDesc(ChatGroup::getCreatedAt));
        return groups.stream().map(this::toGroupVO).toList();
    }

    /**
     * 创建群 + 添加成员 + 指定总管
     */
    @Transactional
    public GroupVO createGroup(GroupCreateDTO dto) {
        // 创建群
        ChatGroup group = new ChatGroup();
        group.setName(dto.getName());
        group.setManagerId(dto.getManagerId());
        chatGroupMapper.insert(group);

        // 添加成员
        if (dto.getMemberIds() != null) {
            for (String memberId : dto.getMemberIds()) {
                ChatGroupMember member = new ChatGroupMember();
                member.setGroupId(group.getId());
                member.setAgentId(memberId);
                member.setRole(memberId.equals(dto.getManagerId()) ? "manager" : "member");
                chatGroupMemberMapper.insert(member);
            }
        }

        // 确保总管也在成员列表中
        if (dto.getManagerId() != null) {
            Long existCount = chatGroupMemberMapper.selectCount(
                    new LambdaQueryWrapper<ChatGroupMember>()
                            .eq(ChatGroupMember::getGroupId, group.getId())
                            .eq(ChatGroupMember::getAgentId, dto.getManagerId()));
            if (existCount == 0) {
                ChatGroupMember manager = new ChatGroupMember();
                manager.setGroupId(group.getId());
                manager.setAgentId(dto.getManagerId());
                manager.setRole("manager");
                chatGroupMemberMapper.insert(manager);
            }
        }

        return toGroupVO(group);
    }

    /**
     * 获取群成员
     */
    public List<AgentVO> getGroupMembers(String groupId) {
        List<ChatGroupMember> members = chatGroupMemberMapper.selectList(
                new LambdaQueryWrapper<ChatGroupMember>().eq(ChatGroupMember::getGroupId, groupId));

        return members.stream().map(member -> {
            Agent agent = agentMapper.selectById(member.getAgentId());
            if (agent == null) return null;
            AgentVO vo = new AgentVO();
            vo.setId(agent.getId());
            vo.setName(agent.getName());
            vo.setAvatar(agent.getAvatar());
            vo.setPosition(agent.getPosition());
            vo.setRuntimeStatus(agent.getRuntimeStatus());
            vo.setLifecycleStatus(agent.getLifecycleStatus());
            return vo;
        }).filter(java.util.Objects::nonNull).toList();
    }

    /**
     * 分页查询消息历史
     */
    public PageResult<MessageVO> getMessages(String groupId, int page, int size) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getGroupId, groupId)
                .orderByDesc(Message::getCreatedAt);

        IPage<Message> messagePage = messageService.pageMessages(groupId, page, size);

        List<MessageVO> voList = messagePage.getRecords().stream()
                .map(this::toMessageVO)
                .toList();

        return PageResult.of(voList, messagePage.getTotal(), page, size);
    }

    /**
     * 发送消息 + 触发 Agent 处理
     */
    @Transactional
    public MessageVO sendMessage(String groupId, MessageSendDTO dto) {
        // 保存消息
        Message message = messageService.saveMessage(groupId, "user", null,
                null, dto.getContent(), dto.getMessageType(), null);

        // 处理用户消息
        processUserMessage(groupId, dto.getContent());

        return toMessageVO(message);
    }

    /**
     * 处理用户消息
     * 1. 检查是否 @总管
     * 2. 如果是，调用 DispatchService 进行任务调度
     * 3. 如果不是，作为普通对话处理
     */
    public void processUserMessage(String groupId, String message) {
        // 获取群内总管
        ChatGroup group = chatGroupMapper.selectById(groupId);
        if (group == null || group.getManagerId() == null) {
            return;
        }

        Agent manager = agentMapper.selectById(group.getManagerId());
        if (manager == null) {
            return;
        }

        // 检查是否 @总管
        String mentionPattern = "@" + manager.getName();
        if (message.contains(mentionPattern)) {
            // 发布任务调度事件
            eventPublisher.publishEvent(new DispatchTaskEvent(groupId, message));
        }
        // 普通对话处理可在此扩展
    }

    private GroupVO toGroupVO(ChatGroup group) {
        GroupVO vo = new GroupVO();
        vo.setId(group.getId());
        vo.setName(group.getName());
        vo.setManagerId(group.getManagerId());
        vo.setCreatedAt(group.getCreatedAt());

        // 查询总管名称
        if (group.getManagerId() != null) {
            Agent manager = agentMapper.selectById(group.getManagerId());
            if (manager != null) {
                vo.setManagerName(manager.getName());
            }
        }

        // 统计成员数
        Long memberCount = chatGroupMemberMapper.selectCount(
                new LambdaQueryWrapper<ChatGroupMember>().eq(ChatGroupMember::getGroupId, group.getId()));
        vo.setMemberCount(memberCount.intValue());

        return vo;
    }

    private MessageVO toMessageVO(Message msg) {
        MessageVO vo = new MessageVO();
        vo.setId(msg.getId());
        vo.setGroupId(msg.getGroupId());
        vo.setSenderType(msg.getSenderType());
        vo.setSenderId(msg.getSenderId());
        vo.setSenderName(msg.getSenderName());
        vo.setContent(msg.getContent());
        vo.setMessageType(msg.getMessageType());
        vo.setMetadata(msg.getMetadata());
        vo.setCreatedAt(msg.getCreatedAt());
        return vo;
    }
}
