package org.example.myinvestor.model;

import java.time.LocalDateTime;

public record UnparsedEmail(String subject, LocalDateTime emailDate, String reason, String snippet) {
}
