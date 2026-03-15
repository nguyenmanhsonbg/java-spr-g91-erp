package com.g90.backend.modules.project.entity;

import java.util.Locale;

public enum ProjectDocumentType {
    PHOTO,
    DOCUMENT,
    EVIDENCE;

    public static ProjectDocumentType from(String value) {
        return ProjectDocumentType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
