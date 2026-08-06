package com.diouma.agentservice.controller;

import com.diouma.agentservice.models.DemandeCompte;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final ChatClient chatClient;
    private final SyncMcpToolCallbackProvider mcpToolProvider;

    public ChatController(ChatClient.Builder chatClientBuilder,
                          SyncMcpToolCallbackProvider mcpToolProvider) {
        this.chatClient = chatClientBuilder.build();
        this.mcpToolProvider = mcpToolProvider;
    }

//    @GetMapping("/chat")
//    public String chater(@RequestParam String message){
//        return chatClient
//                .prompt()
//                .system(" Tu es un conseiller bancaire poli, tu parles français, tu restes courtois, tu ne parles que de banque,Réponds en 3 phrases maximum,Si tu ne connais pas la réponse, dis-le honnêtement au lieu d'inventer. ")
//                .user(message)
//                .call()
//                .content();
//    }
//
//    @GetMapping("/extraire")
//    public DemandeCompte extraire(@RequestParam String message){
//        return chatClient
//                .prompt()
//                .system("Tu extrais une demande de création de compte bancaire. "
//                        + "Le champ 'type' doit valoir EXACTEMENT 'CURRENT-ACCOUNT' pour un compte courant, "
//                        + "ou 'SAVING-ACCOUNT' pour un compte épargne.")
//                .user(message)
//                .call()
//                .entity(DemandeCompte.class);
//    }


    @GetMapping("/agent")
    public String agent(@RequestParam String message) {
        return chatClient
                .prompt()
                .system("Tu es un assistant bancaire. Quand on te demande de creer un compte, "
                        + "utilise ton outil. Le type doit etre CURRENT-ACCOUNT ou SAVING-ACCOUNT.")
                .user(message)
                .tools(mcpToolProvider.getToolCallbacks())
                .call()
                .content();
    }

}

