package org.example.myinvestor.mail;

import java.time.LocalDateTime;

public record MailContent(String subject, LocalDateTime date, String plainText, String htmlText) {
}
