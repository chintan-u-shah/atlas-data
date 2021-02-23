package com.wheelsup.jpa.audit.interceptor.audit_user;

import com.wheelsup.jpa.audit.domain.audit.AuditUserType;

public interface CurrentAuditUserProvider {

	Long getAudiUserId();

	AuditUserType getAudiUserType();

}
