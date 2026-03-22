package com.g90.backend.modules.email.service;

import java.util.Map;

public interface EmailTemplateService {

    String render(String templateName, Map<String, Object> variables);
}
