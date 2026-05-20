package fr.insalyon.creatis.vip.social.server.controller;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.insalyon.creatis.vip.core.client.DefaultError;
import fr.insalyon.creatis.vip.core.client.VipException;
import fr.insalyon.creatis.vip.core.models.User;
import fr.insalyon.creatis.vip.core.server.business.UserBusiness;
import fr.insalyon.creatis.vip.social.models.Message;
import fr.insalyon.creatis.vip.social.server.business.MessageBusiness;

@RestController
@RequestMapping("/messages")
public class MessageController {

    private final MessageBusiness messageBusiness;
    private final UserBusiness userBusiness;

    @Autowired
    public MessageController(MessageBusiness messageBusiness, UserBusiness userBusiness) {
        this.messageBusiness = messageBusiness;
        this.userBusiness = userBusiness;
    }

    @GetMapping
    public List<Message> getReceivedMessages(@RequestParam(required = false) Long startDate) throws VipException {
        User currentUser = userBusiness.getCurrentUser();
        return messageBusiness.getMessagesByUser(currentUser.getEmail(), toDate(startDate));
    }

    @GetMapping("/send")
    public List<Message> getSentMessages(@RequestParam(required = false) Long startDate) throws VipException {
        User currentUser = userBusiness.getCurrentUser();
        return messageBusiness.getSentMessagesByUser(currentUser.getEmail(), toDate(startDate));
    }

    @PostMapping
    public void sendMessage(@RequestBody SendMessageRequest request) throws VipException {
        if (request == null || request.getRecipients() == null || request.getRecipients().length == 0) {
            throw new VipException(DefaultError.BAD_INPUT_FIELD, "recipients", "Recipients are required!");
        }
        if (request.getSubject() == null || request.getSubject().isBlank()) {
            throw new VipException(DefaultError.BAD_INPUT_FIELD, "subject", "Subject is required!");
        }
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new VipException(DefaultError.BAD_INPUT_FIELD, "message", "Message is required!");
        }

        User currentUser = userBusiness.getCurrentUser();
        messageBusiness.sendMessage(currentUser, request.getRecipients(), request.getSubject(), request.getMessage());
    }

    @DeleteMapping("/{id}")
    public void deleteMessage(@PathVariable long id) throws VipException {
        User currentUser = userBusiness.getCurrentUser();
        messageBusiness.removeByReceiver(id, currentUser.getEmail());
    }

    @DeleteMapping("/send/{id}")
    public void deleteSentMessage(@PathVariable long id) throws VipException {
        messageBusiness.remove(id);
    }

    private Date toDate(Long startDate) {
        return startDate == null ? new Date() : new Date(startDate);
    }

    public static class SendMessageRequest {

        private String[] recipients;
        private String subject;
        private String message;

        public String[] getRecipients() {
            return recipients;
        }

        public void setRecipients(String[] recipients) {
            this.recipients = recipients;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
