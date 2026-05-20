package fr.insalyon.creatis.vip.social.models;

import com.fasterxml.jackson.annotation.JsonView;

import fr.insalyon.creatis.vip.core.server.inter.DataViews;

@JsonView(DataViews.User.class)
public class SendMessageRequest {

    private String[] recipients;
    private String subject;
    private String message;
    private Boolean isGroupMessage;

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

    public Boolean getIsGroupMessage() {
        return isGroupMessage;
    }

    public void setIsGroupMessage(Boolean isGroupMessage) {
        this.isGroupMessage = isGroupMessage;
    }
}
