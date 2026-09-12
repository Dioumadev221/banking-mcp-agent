package com.diouma.agentservice.controller;

import com.diouma.agentservice.models.AccountRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The three levels of what a model can do, exposed side by side.
 *
 * <p>They are deliberately kept separate: /chat only produces text, /extract
 * produces typed data without acting, and /agent is the only one allowed to
 * change anything. Comparing the three is the clearest way to see where an
 * agent's risk actually starts.
 */
@RestController
public class ChatController {

    private final ChatClient chatClient;
    private final SyncMcpToolCallbackProvider mcpToolProvider;

    public ChatController(ChatClient.Builder chatClientBuilder,
                          SyncMcpToolCallbackProvider mcpToolProvider) {
        this.chatClient = chatClientBuilder.build();
        this.mcpToolProvider = mcpToolProvider;
    }

    /** Plain conversation: the model has no tool and can change nothing. */
    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        return chatClient
                .prompt()
                .system("You are a polite banking adviser. Answer in at most three sentences, "
                        + "stay on banking topics, and say so honestly when you do not know.")
                .user(message)
                .call()
                .content();
    }

    /**
     * Structured extraction: a sentence becomes typed fields.
     *
     * <p>Still no side effect. This is what makes a wrong reading visible
     * before anything is created.
     */
    @GetMapping("/extract")
    public AccountRequest extract(@RequestParam String message) {
        return chatClient
                .prompt()
                .system("Extract a bank account request. The 'type' field must be exactly "
                        + "'CURRENT-ACCOUNT' for a current account or 'SAVING-ACCOUNT' for a savings account.")
                .user(message)
                .call()
                .entity(AccountRequest.class);
    }

    /**
     * The agent: the model chooses a tool and its arguments, and the call goes
     * over MCP to ebank-service, which owns the operation.
     *
     * <p>The model never touches the database. It proposes; ebank-service
     * validates the account type and the owner, and can refuse.
     *
     * <p>The third rule below exists because of a measured failure, not as a
     * precaution. Asked for a product the bank does not sell, the model used to
     * open a savings account instead and report success - 10 times out of 10
     * (see results/). Argument validation cannot catch that: the model never
     * sends an invalid value, it substitutes the intent before the guardrail
     * sees anything. The only layer that can refuse is the one choosing the
     * call.
     */
    @GetMapping("/agent")
    public String agent(@RequestParam String message) {
        return chatClient
                .prompt()
                .system("""
                        You are a banking assistant.

                        This bank opens exactly two products: CURRENT-ACCOUNT and SAVING-ACCOUNT.

                        If the customer asks for any other product, tell them it is not offered \
                        and do NOT call any tool. Never open a different product from the one \
                        requested, and never present a substitute as if it were what was asked for.

                        When the request is one of the two products, use your tool. \
                        If the tool reports an error, explain it instead of inventing a result.
                        """)
                .user(message)
                .tools(mcpToolProvider.getToolCallbacks())
                .call()
                .content();
    }

}
