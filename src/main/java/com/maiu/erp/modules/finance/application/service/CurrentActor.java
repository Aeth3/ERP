package com.maiu.erp.modules.finance.application.service;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.maiu.erp.shared.exception.BadRequestException;

/** The authenticated user's login name; finance actions are never attributed to a client-supplied name. */
final class CurrentActor {
    private CurrentActor() {
    }

    static String name() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new BadRequestException("An authenticated user is required for finance actions");
        }
        return authentication.getName();
    }
}
