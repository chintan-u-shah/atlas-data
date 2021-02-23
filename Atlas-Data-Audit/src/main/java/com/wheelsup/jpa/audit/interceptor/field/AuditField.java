package com.wheelsup.jpa.audit.interceptor.field;

import com.wheelsup.jpa.audit.service.utils.ModelReflectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import java.lang.reflect.Field;
import java.util.Date;

public abstract class AuditField {

	protected Field field;
	protected final Object previousValue;
	protected final Object currentValue;


	AuditField(Field field, Object previousValue, Object currentValue) {
		this.field = field;
		this.previousValue = previousValue;
		this.currentValue = currentValue;
	}

	public Object getPreviousValue() {
		return previousValue;
	}

	public Object getCurrentValue() {
		return currentValue;
	}

	public String getColumnName() {
		return ModelReflectionUtils.getColumnName(field);
	}

	public Class<?> getType() {
		return field.getType();
	}

	public abstract boolean toBeAudit() throws Exception;

	public abstract boolean isAuditable();

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		AuditField that = (AuditField) o;

		return field.equals(that.field);
	}

	@Override
	public int hashCode() {
		return field.hashCode();
	}


	final Object getPropertyValue(Object value) {

		if (ModelReflectionUtils.isEntity(getType())) {
			if (value != null) {
				return ModelReflectionUtils.getEntityId(value);
			}
		}

		if (Date.class.isAssignableFrom(getType())) {
			if (value != null) {
				return new DateTime(value).minuteOfHour().roundFloorCopy().toDate();
			}
		}

		if (String.class.isAssignableFrom(getType())) {
			return StringUtils.isEmpty((CharSequence) value) ? null : value;
		}

		if (Boolean.class.isAssignableFrom(getType())) {
			return BooleanUtils.isTrue((Boolean) value) ? true : null;
		}

		return value;
	}

	@Override
	public String toString() {
		return "AuditUpdatedField{" +
				"fieldName=" + field.getName() +
				(isAuditable() ?
						", fieldType=" + getType() +
								", previousValue=" + getPropertyValue(previousValue) +
								", currentValue=" + getPropertyValue(currentValue)
						: ", <Not Auditable>") +
				'}';
	}

}
