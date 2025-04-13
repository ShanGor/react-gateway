package io.github.shangor.agent.state;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.shangor.statemachine.state.ActionNode;
import io.github.shangor.statemachine.util.JsonUtil;
import io.micrometer.common.util.StringUtils;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.*;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Slf4j
public class AgentNode extends ActionNode {
    public static final String ACTION_NAME = "LLM_AGENT";

    public static final ToolCallback[] EMPTY_TOOLS = new ToolCallback[0];
    private String systemPrompt;
    /**
     * supports {{inputText}} as placeholder
     */
    private String userPrompt;
    private int chatHistoryCount;
    private String model;
    private List<String> tools;
    private ChatModel chatModel;

    @Data
    @Builder
    public static class AgentParam {
        private List<Message> chatHistory;
        private String inputText;
        private List<String> imageUrls;
    }

    public ToolCallback[] getToolCallbacks(ToolCallbackProvider provider) {
        if (tools == null || tools.isEmpty()) return EMPTY_TOOLS;
        var toolSet = new HashSet<>(tools);
        var t = new HashMap<String, ToolCallback>();
        for(var tool : provider.getToolCallbacks()) {
            if (toolSet.contains(tool.getName())) {
                t.put(tool.getName(), (ToolCallback) tool);
            }
        }

        return t.values().toArray(EMPTY_TOOLS);
    }

    @Override
    public Map<String, Object> action(Param input) {
        try {
            var inputText = JsonUtil.getObjectMapper().writeValueAsString(input.getContext());
            log.info("LLM Agent: {]", input.getConfig().getActionName());
            Thread.sleep(1000);
//            AgentParam param = JsonUtil.getObjectMapper().readValue(inputText, AgentParam.class);
//            List<Message> history = new LinkedList<>();
//            if (StringUtils.isNotBlank(systemPrompt)) {
//                history.add(new SystemMessage(systemPrompt));
//            }
//            if (param.chatHistory != null && !param.chatHistory.isEmpty()) {
//                var filteredList = param.chatHistory.stream().filter(m -> !m.getMessageType().equals(MessageType.SYSTEM)).toList();
//                if (filteredList.size() > chatHistoryCount) {
//                    history.addAll(filteredList.subList(filteredList.size() - chatHistoryCount, filteredList.size()));
//
//                } else {
//                    history.addAll(filteredList);
//                }
//            }
//
//            UserMessage userMessage;
//            var text = param.inputText;
//            if (StringUtils.isNotBlank(param.inputText)) {
//                text = userPrompt.replaceFirst("\\{\\s*\\{inputText\\s*}}", param.inputText);
//            }
//            userMessage = new UserMessage(text);
//
//            history.add(userMessage);
//            var prompt = new Prompt(history);
//
//            var resp = chatModel.call(prompt);
            //TODO
            return Map.of();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
