package io.github.shangor.agent.state;

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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Slf4j
public class AgentNode extends StateGraph.ActionNode {
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

    public Object action(List<Message> chatHistory, String inputText, List<String> imageUrls) {
        List<Message> history = new LinkedList<>();
        if (StringUtils.isNotBlank(systemPrompt)) {
            history.add(new SystemMessage(systemPrompt));
        }
        if (chatHistory != null && !chatHistory.isEmpty()) {
            var filteredList = chatHistory.stream().filter(m -> !m.getMessageType().equals(MessageType.SYSTEM)).toList();
            if (filteredList.size() > chatHistoryCount) {
                history.addAll(filteredList.subList(filteredList.size() - chatHistoryCount, filteredList.size()));

            } else {
                history.addAll(filteredList);
            }
        }

        UserMessage userMessage;
        var text = inputText;
        if (StringUtils.isNotBlank(inputText)) {
            text = userPrompt.replaceFirst("\\{\\s*\\{inputText\\s*}}", inputText);
        }
        userMessage = new UserMessage(text);

        history.add(userMessage);
        var prompt = new Prompt(history);

        return chatModel.call(prompt);
    }
}
