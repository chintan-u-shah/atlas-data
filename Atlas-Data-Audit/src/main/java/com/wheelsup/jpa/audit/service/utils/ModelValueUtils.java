package com.wheelsup.jpa.audit.service.utils;

import org.apache.commons.lang3.math.NumberUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.Date;


public final class ModelValueUtils {

	public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("MM/dd/yyyy HH:mm:ss");
	public static final DecimalFormat DECIMAL_NUMBER_FORMATTER = new DecimalFormat("#0.000000000000");

	public static String getFormattedValue(Object propertyValue) throws Exception {
		if (propertyValue == null) {
			return null;
		}

		return getFormattedValue(propertyValue, propertyValue.getClass());
	}

	public static String getFormattedValue(Object propertyValue, Class<?> propertyType) throws Exception {
		if (propertyValue == null) {
			return null;
		}

		if (ModelReflectionUtils.isEntity(propertyType)) {
			return String.valueOf(ModelReflectionUtils.getEntityId(propertyValue));
		}

		if (Date.class.equals(propertyType)) {
			return new DateTime(propertyValue).toString(ModelValueUtils.DATE_TIME_FORMATTER);
		}

		if (Timestamp.class.equals(propertyType)) {
			return new DateTime(propertyValue).toString(ModelValueUtils.DATE_TIME_FORMATTER);
		}

		if (BigDecimal.class.equals(propertyType)) {
			return ModelValueUtils.DECIMAL_NUMBER_FORMATTER.format(propertyValue);
		}

		if (Double.class.equals(propertyType)) {
			return ModelValueUtils.DECIMAL_NUMBER_FORMATTER.format(propertyValue);
		}

		return String.valueOf(propertyValue);
	}


	@SuppressWarnings("unchecked")
	public static <T> T getParsedValue(String value, Class<T> propertyType, EntityRetriever entityRetriever) throws Exception {
		if (value == null) {
			return propertyType.cast(null);
		}

		if (ModelReflectionUtils.isEntity(propertyType)) {
			return entityRetriever.getEntityValue(propertyType, Long.valueOf(value));
		}

		if (Date.class.equals(propertyType)) {
			return (T) DATE_TIME_FORMATTER.parseDateTime(value).toDate();
		}

		if (Timestamp.class.equals(propertyType)) {
			return (T) DATE_TIME_FORMATTER.parseDateTime(value).toDate();
		}

		if (Number.class.isAssignableFrom(propertyType)) {
			if (NumberUtils.isNumber(value)) {
				if (BigDecimal.class.isAssignableFrom(propertyType)) {
					try {
						DECIMAL_NUMBER_FORMATTER.setParseBigDecimal(true);
						return (T) DECIMAL_NUMBER_FORMATTER.parse(value);
					} finally {
						DECIMAL_NUMBER_FORMATTER.setParseBigDecimal(false);
					}
				}

				if (Double.class.isAssignableFrom(propertyType)) {
					return (T) new Double(DECIMAL_NUMBER_FORMATTER.parse(value).doubleValue());
				}

				if (Integer.class.equals(propertyType)) {
					return (T) Integer.valueOf(value);
				}

				if (Long.class.equals(propertyType)) {
					return (T) Long.valueOf(value);
				}
			}
		}

		if (Boolean.class.equals(propertyType)) {
			if (NumberUtils.isNumber(value)) {
				value = String.valueOf(NumberUtils.createInteger(value) != 0);
			}

			return (T) Boolean.valueOf(value);
		}

		if (boolean.class.equals(propertyType)) {
			if (NumberUtils.isNumber(value)) {
				value = String.valueOf(NumberUtils.createInteger(value) != 0);
			}

			return (T) Boolean.valueOf(value);
		}

		if (Enum.class.isAssignableFrom(propertyType)) {
			return (T) Enum.valueOf((Class<Enum>) propertyType, value);
		}

		return propertyType.cast(value);
	}

	public interface EntityRetriever {
		<T> T getEntityValue(Class<T> clazz, Long entityId);
	}

	private ModelValueUtils() {
	}
}
