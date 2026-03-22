package com.g90.backend.modules.email.service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface EmailService {

    CompletableFuture<Void> sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables);
}
