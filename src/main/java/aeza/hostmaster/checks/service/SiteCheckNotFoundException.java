package aeza.hostmaster.checks.service;

import java.util.UUID;

public class SiteCheckNotFoundException extends RuntimeException {

    public SiteCheckNotFoundException(UUID id) {
        super("Site check result %s not found".formatted(id));
    }
}
