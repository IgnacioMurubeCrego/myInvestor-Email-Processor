package org.example.myinvestor.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class MailContentExtractor {

    public static MailContent extract(MimeMessage message) throws MessagingException, IOException {
        String subject = message.getSubject();
        Date sentDate = message.getSentDate();
        LocalDateTime date = sentDate == null ? null
                : LocalDateTime.ofInstant(sentDate.toInstant(), ZoneId.systemDefault());

        StringBuilder plain = new StringBuilder();
        StringBuilder html = new StringBuilder();
        collect(message, plain, html);
        return new MailContent(subject, date, plain.toString(), html.toString());
    }

    private static void collect(Part part, StringBuilder plain, StringBuilder html)
            throws MessagingException, IOException {
        if (part.isMimeType("text/plain")) {
            plain.append(part.getContent().toString()).append('\n');
        } else if (part.isMimeType("text/html")) {
            html.append(part.getContent().toString()).append('\n');
        } else if (part.isMimeType("multipart/*")) {
            Object content = part.getContent();
            if (content instanceof Multipart multipart) {
                for (int i = 0; i < multipart.getCount(); i++) {
                    collect(multipart.getBodyPart(i), plain, html);
                }
            }
        }
    }
}
