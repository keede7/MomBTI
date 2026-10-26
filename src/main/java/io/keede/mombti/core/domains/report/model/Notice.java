package io.keede.mombti.core.domains.report.model;

import java.util.Objects;

/** 미리 알아둘 것 하나. */
public record Notice(String title, String body) {

    public Notice {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(body, "body");
    }
}
