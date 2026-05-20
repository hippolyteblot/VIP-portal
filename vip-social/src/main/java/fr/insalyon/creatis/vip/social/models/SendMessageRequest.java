package fr.insalyon.creatis.vip.social.models;

import com.fasterxml.jackson.annotation.JsonView;

import fr.insalyon.creatis.vip.core.server.inter.DataViews;

@JsonView(DataViews.User.class)
public class SendMessageRequest {

    @JsonView(DataViews.User.class)
    private String[] recipients;
    @JsonView(DataViews.User.class)
    private String subject;
    @JsonView(DataViews.User.class)
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
