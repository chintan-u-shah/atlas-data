package com.wheelsup.jpa.audit.interceptor.field;

import com.wheelsup.jpa.audit.service.utils.ModelReflectionUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.lang.reflect.Field;
import java.math.BigDecimal;

public class AuditUpdatedField extends AuditField {

	public AuditUpdatedField(Field field, Object previousValue, Object currentValue) {
		super(field, previousValue, currentValue);
	}

	@Override
	public boolean toBeAudit() throws Exception {

		return isAuditable() && getColumnName() != null && changesDetected();
	}

	@Override
	public boolean isAuditable() {
		return ModelReflectionUtils.isFieldAuditable(field);

	}

	private boolean changesDetected() {

		if (BigDecimal.class.isAssignableFrom(getType())) {
			return bigDecimalNotEqual((BigDecimal) currentValue, (BigDecimal) previousValue);
		}

		return ObjectUtils.notEqual(getPropertyValue(previousValue), getPropertyValue(currentValue));
	}

	private boolean bigDecimalNotEqual(BigDecimal currentValue, BigDecimal previousValue) {
		if (previousValue == null && currentValue == null) {
			return false;
		}

		if (previousValue == null) {
			return true;
		}

		if (currentValue == null) {
			return true;
		}

		return currentValue.compareTo(previousValue) != 0;
	}
}
