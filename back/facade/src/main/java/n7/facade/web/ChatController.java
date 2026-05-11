package n7.facade.web;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import n7.facade.model.ChatMessage;
import n7.facade.model.Game;
import n7.facade.model.Joueur;
import n7.facade.repository.ChatMessageRepository;
import n7.facade.repository.GameRepository;
import n7.facade.repository.JoueurRepository;
import n7.facade.web.dto.ChatMessageRequest;
import n7.facade.web.dto.ChatMessageResponse;

@Controller
public class ChatController {

    private final ChatMessageRepository chatMessageRepository;
    private final GameRepository gameRepository;
    private final JoueurRepository joueurRepository;
    private final SimpMessagingTemplate messaging;

    public ChatController(ChatMessageRepository chatMessageRepository, 
                          GameRepository gameRepository, 
                          JoueurRepository joueurRepository, 
                          SimpMessagingTemplate messaging) {
        this.chatMessageRepository = chatMessageRepository;
        this.gameRepository = gameRepository;
        this.joueurRepository = joueurRepository;
        this.messaging = messaging;
    }

    @MessageMapping("/game/{gameId}/chat")
    @Transactional
    public void handleChatMessage(@DestinationVariable Long gameId, ChatMessageRequest request) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));
        
        Joueur joueur = joueurRepository.findById(request.getJoueurId())
                .orElseThrow(() -> new RuntimeException("Player not found"));

        ChatMessage message = new ChatMessage(game, joueur, request.getContent());
        chatMessageRepository.save(message);

        ChatMessageResponse response = new ChatMessageResponse(
                message.getId(),
                joueur.getPseudo(),
                message.getContent(),
                message.getSentAt()
        );

        messaging.convertAndSend("/topic/game/" + gameId + "/chat", response);
    }
}
