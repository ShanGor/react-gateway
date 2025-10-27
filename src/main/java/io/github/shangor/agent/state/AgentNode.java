package io.github.shangor.agent.state;

import io.github.shangor.data.dto.AgentConfig;
import io.github.shangor.data.dto.LlmContext;
import io.github.shangor.service.LlmAgentService;
import io.github.shangor.statemachine.state.ActionNode;
import io.github.shangor.statemachine.state.NodeStatus;
import io.github.shangor.util.IntegrationUtils;
import io.micrometer.common.util.StringUtils;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class AgentNode extends ActionNode {
    public static final String ACTION_NAME = "LLM_AGENT";

//    public static final ToolCallback[] EMPTY_TOOLS = new ToolCallback[0];

//
//    public static ToolCallback[] getToolCallbacks(ToolCallbackProvider provider, List<String> tools) {
//        if (tools == null || tools.isEmpty()) return EMPTY_TOOLS;
//        var toolSet = new HashSet<>(tools);
//        var t = new HashMap<String, ToolCallback>();
//        for(var tool : provider.getToolCallbacks()) {
//            var toolName = tool.getToolDefinition().name();
//            if (toolSet.contains(toolName)) {
//                t.put(toolName, (ToolCallback) tool);
//            }
//        }
//
//        return t.values().toArray(EMPTY_TOOLS);
//    }

    /**
     * Take action to do LLM agent work.
     *
     * @param input: it is the flow context, format would be like
     *          inputText: String
     *          images: List<String> (Which is dataUrl of the images, optional)
     *          textDocs: List<String> (optional, it is additional to inputText)
     *          chatHistory: List<Message>
     * @return a new context
     */
    @Override
    public Output action(Param input) {
        try {
            LlmContext context = LlmContext.from(input.getContext());
            if (context == null) {
                context = new LlmContext();
            }
            var node = input.getConfig();
            var opt = AgentConfig.fromConfig(node);
            if (opt.isEmpty()) {
                log.error("Failed to get agent config: {}", node);
                return Output.builder().overriddenStatus(NodeStatus.FAILED).extraMessage("Failed to get agent config").build();
            }
            var agentConfig = opt.get();
//
//            List<Message> history = new LinkedList<>();
//            if (StringUtils.isNotBlank(agentConfig.getSystemPrompt())) {
//                history.add(new SystemMessage(agentConfig.getSystemPrompt()));
//            }
//
//            var chatHistory = new LinkedList<LlmContext.Message>();
//            if (context.getChatHistory() != null) {
//                chatHistory.addAll(context.getChatHistory());
//            }
//
//            if (!chatHistory.isEmpty()) {
//                var filteredList = chatHistory.stream().filter(m -> !m.getRole().equals(MessageType.SYSTEM.getValue())).map(m -> {
//                    if (m.getRole().equals(MessageType.ASSISTANT.getValue())) {
//                        return new AssistantMessage(m.getText());
//                    } else {
//                        return new UserMessage(m.getText());
//                    }
//                }).toList();
//
//                history.addAll(filteredList);
//            }
//
//            UserMessage userMessage;
//            var text = context.getInputText();
//            if (StringUtils.isNotBlank(text)) {
//                text = agentConfig.getUserPrompt().replaceFirst("\\{\\s*\\{inputText\\s*}}", text);
//            } else {
//                text = agentConfig.getUserPrompt();
//            }
//            if (context.getImages() != null && !context.getImages().isEmpty()) {
//                var medias = context.getImages().stream().map(IntegrationUtils::convertDataUrlToMedia).toList();
//
//                userMessage = UserMessage.builder().text(text).media(medias).build();
//            } else {
//                userMessage = new UserMessage(text);
//            }
//
//            history.add(userMessage);
//            var prompt = new Prompt(history, ChatOptions.builder().model(agentConfig.getModel()).build());
//
//            //TODO add tools usage
//            var resp = LlmAgentService.instance.getChatModel().call(prompt);
//
//            var usage = LlmUsage.from(resp.getMetadata().getUsage());
//            var result = resp.getResult().getOutput();
//            var medias = resp.getResult().getOutput().getMedia();
//            var images = new LinkedList<String>();
//            if (medias != null && !medias.isEmpty()) {
//                medias.forEach(media -> images.add(IntegrationUtils.convertMediaToDataUrl(media)));
//            }
//
//            chatHistory.add(LlmContext.Message.builder().role("user")
//                    .images(context.getImages())
//                    .text(text)
//                    .build());
//            chatHistory.add(LlmContext.Message.builder().role("assistant").text(result.getText()).images(images).build());
//
//            context.setUsage(usage);
//            context.setInputText("");
//            context.setChatHistory(chatHistory);
            context.setImages(Collections.emptyList());
            context.setTextDocs(Collections.emptyList());
            return Output.builder().context(context.toString()).build();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
