package com.wheelsup.jpa.audit.interceptor.field;

import com.wheelsup.jpa.audit.service.utils.ModelReflectionUtils;

import java.lang.reflect.Field;

public class AuditInitialStateField extends AuditField {

	public AuditInitialStateField(Field field, Object currentValue) {
		super(field, null, currentValue);
	}

	@Override
	public boolean toBeAudit() throws Exception {

		return isAuditable() && getColumnName() != null && getPropertyValue(currentValue) != null;
	}

	@Override
	public boolean isAuditable() {
		return ModelReflectionUtils.isFieldAuditable(field);
	}
}
