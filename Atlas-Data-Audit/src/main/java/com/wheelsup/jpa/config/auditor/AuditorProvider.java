package com.wheelsup.jpa.config.auditor;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class AuditorProvider implements AuditorAware<String> {

	public String getCurrentAuditor() {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()) {
			return null;
		}

		Object principal = authentication.getPrincipal();
		if (principal != null) {
			if (UserDetails.class.isInstance(principal)) {
				return UserDetails.class.cast(principal).getUsername();
			}
		}

		return StringUtils.EMPTY;
	}
}
