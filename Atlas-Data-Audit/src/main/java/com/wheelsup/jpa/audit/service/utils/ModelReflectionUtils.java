package com.wheelsup.jpa.audit.service.utils;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.wheelsup.jpa.audit.interceptor.audit.Audited;
import com.wheelsup.jpa.audit.interceptor.audit.NotAudited;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.type.NullType;
import javax.persistence.*;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public final class ModelReflectionUtils {

	public static boolean isEntity(Class<?> clazz) {
		return clazz.isAnnotationPresent(Entity.class);
	}

	public static String getColumnName(Field field) {
		if (isEntity(field.getDeclaringClass())) {
			Column columnAnnotation = field.getAnnotation(Column.class);
			if (columnAnnotation != null && StringUtils.isNotEmpty(columnAnnotation.name())) {
				return columnAnnotation.name();
			}

			JoinColumn joinColumnAnnotation = field.getAnnotation(JoinColumn.class);
			if (joinColumnAnnotation != null && StringUtils.isNotEmpty(joinColumnAnnotation.name())) {
				return joinColumnAnnotation.name();
			}

			return field.getName();
		}

		return null;
	}

	public static Map<String, PropertyDescriptor> getEntityClassPropertyDescriptor(Class<?> clazz) throws IntrospectionException {
		Map<String, PropertyDescriptor> propertiesDescriptor = new HashMap<>();

		if (isEntity(clazz)) {

			Map<String, Field> columnsField = getClassFields(clazz);

			BeanInfo info = Introspector.getBeanInfo(clazz);
			PropertyDescriptor[] propertyDescriptors = info.getPropertyDescriptors();
			for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
				String fieldName = propertyDescriptor.getName();
				Method readMethod = propertyDescriptor.getReadMethod();

				if (columnsField.containsKey(fieldName)) {
					String columnName = getColumnName(columnsField.get(fieldName));

					if (StringUtils.isNotEmpty(columnName) && readMethod != null) {
						propertiesDescriptor.put(columnName, propertyDescriptor);
					}
				}
			}
		}

		return propertiesDescriptor;
	}

	public static PropertyDescriptor getFieldPropertyDescriptor(Class<?> clazz, Field field) {
		try {
			BeanInfo info = Introspector.getBeanInfo(clazz);
			PropertyDescriptor[] propertyDescriptors = info.getPropertyDescriptors();

			for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {

				if (StringUtils.equals(propertyDescriptor.getName(), field.getName())) {
					return propertyDescriptor;
				}
			}
		} catch (Exception ignored) {

		}

		return null;
	}

	public static Map<Field, PropertyDescriptor> getEntityClassPropertyDescriptorByFields(Class<?> clazz) throws IntrospectionException {
		Map<Field, PropertyDescriptor> propertiesDescriptor = new HashMap<>();

		if (isEntity(clazz)) {

			Map<String, Field> columnsField = getClassFields(clazz);

			BeanInfo info = Introspector.getBeanInfo(clazz);
			PropertyDescriptor[] propertyDescriptors = info.getPropertyDescriptors();
			for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
				String fieldName = propertyDescriptor.getName();
				Method readMethod = propertyDescriptor.getReadMethod();

				if (columnsField.containsKey(fieldName)) {

					if (readMethod != null) {
						propertiesDescriptor.put(columnsField.get(fieldName), propertyDescriptor);
					}
				}
			}
		}

		return propertiesDescriptor;
	}


	public static String getEntityName(Class<?> clazz) {
		if (isEntity(clazz)) {
			Entity entityAnnotation = clazz.getAnnotation(Entity.class);
			if (entityAnnotation != null && StringUtils.isNotEmpty(entityAnnotation.name())) {
				return entityAnnotation.name();
			}

			return clazz.getSimpleName();
		}

		return null;
	}


	public static String getTableName(Class<?> clazz) {
		return searchEntityTableName(clazz, null);
	}

	public static Map<String, Field> getClassFields(Class<?> clazz) {
		Map<String, Field> classFields = new HashMap<>();

		addCurrentClassFields(clazz, classFields);

		addSuperClassFields(clazz, Object.class, classFields);

		return classFields;
	}

	public static Field getClassField(Class<?> clazz, String columnName) {
		Field field = searchCurrentClassField(clazz, columnName);
		if (field != null) {
			return field;
		}

		return searchSuperClassField(clazz, Object.class, columnName);
	}

	public static boolean isEntityAuditable(Class<?> clazz) throws Exception {
		return searchEntityAuditable(clazz);
	}

	public static Audited.DependsOn auditableDependsOn(Class<?> clazz) {
		if (isEntity(clazz)) {
			boolean auditedAnnotationPresent = clazz.isAnnotationPresent(Audited.class);

			if (auditedAnnotationPresent) {
				Audited.DependsOn dependsOn = clazz.getAnnotation(Audited.class).value();

				if (!dependsOn.entity().equals(NullType.class)) {
					return dependsOn;
				}
			} else {
				return auditableDependsOn(clazz.getSuperclass());
			}
		}

		return null;
	}

	public static boolean isFieldAuditable(Field field) {
		if (isEntity(field.getDeclaringClass())) {
			if (!Modifier.isStatic(field.getModifiers())) {
				if (!Modifier.isFinal(field.getModifiers())) {
					if (field.getAnnotation(Transient.class) == null) {
						if (field.getAnnotation(OneToMany.class) == null) {
							if (field.getAnnotation(ElementCollection.class) == null) {
								return field.getAnnotation(NotAudited.class) == null;
							}
						}
					}
				}
			}
		}

		return false;
	}

	public static Long getEntityId(Object entity) {
		if (isEntity(entity.getClass())) {
			try {
				return getClassEntityId(entity.getClass(), entity);
			} catch (Exception ignored) {

			}
		}

		return null;
	}

	public static Object getEntityColumnValue(Object entity, String columnName) {
		try {
			if (isEntity(entity.getClass())) {
				Field field = getClassField(entity.getClass(), columnName);
				PropertyDescriptor propertyDescriptor = getFieldPropertyDescriptor(entity.getClass(), field);

				if (propertyDescriptor != null) {
					Method readMethod = propertyDescriptor.getReadMethod();

					if (readMethod != null) {
						return readMethod.invoke(entity);
					}
				}
			}
		} catch (Exception ignored) {

		}

		return null;
	}

	public static String getEntityIdColumnName(Class<?> clazz) {
		for (Field field : clazz.getDeclaredFields()) {

			if (field.isAnnotationPresent(Id.class)) {
				return getColumnName(field);
			}
		}

		Class<?> superclass = clazz.getSuperclass();
		if (superclass != null && !Object.class.equals(superclass)) {
			return getEntityIdColumnName(superclass);
		}

		return null;
	}

	private static Long getClassEntityId(Class<?> clazz, Object entity) throws Exception {
		String idFieldName = getEntityIdFieldName(clazz);

		if (StringUtils.isNotEmpty(idFieldName)) {
			Long idFieldValue = readIdFieldValue(entity, idFieldName);

			if (idFieldValue != null) {
				return idFieldValue;
			}
		}

		Class<?> superclass = clazz.getSuperclass();
		if (superclass != null && !Object.class.equals(superclass)) {
			return getClassEntityId(superclass, superclass.cast(entity));
		}

		return null;
	}

	private static Long readIdFieldValue(Object entity, String idFieldName) throws IntrospectionException, IllegalAccessException, InvocationTargetException {
		BeanInfo info = Introspector.getBeanInfo(entity.getClass());
		PropertyDescriptor[] propertyDescriptors = info.getPropertyDescriptors();
		for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
			String fieldName = propertyDescriptor.getName();

			if (StringUtils.equals(fieldName, idFieldName)) {
				Method readMethod = propertyDescriptor.getReadMethod();

				if (readMethod != null) {
					return (Long) readMethod.invoke(entity);
				}
			}
		}
		return null;
	}

	private static String getEntityIdFieldName(Class<?> clazz) {
		for (Field field : clazz.getDeclaredFields()) {

			if (field.isAnnotationPresent(Id.class)) {
				return field.getName();
			}
		}

		return null;
	}

	private static boolean searchEntityAuditable(Class<?> clazz) {
		if (Object.class.equals(clazz)) {
			return false;
		}

		if (clazz.isAnnotationPresent(Audited.class)) {
			if (isEntity(clazz)) {
				return true;
			}
		}

		return searchEntityAuditable(clazz.getSuperclass());
	}

	private static String searchEntityTableName(Class<?> clazz, String tableName) {
		if (Object.class.equals(clazz)) {
			return tableName;
		}

		if (isEntity(clazz)) {
			Table tableAnnotation = clazz.getAnnotation(Table.class);
			if (tableAnnotation != null && StringUtils.isNotEmpty(tableAnnotation.name())) {
				return tableAnnotation.name();
			}

			tableName = clazz.getSimpleName();
		}

		return searchEntityTableName(clazz.getSuperclass(), tableName);
	}

	private static Field searchClassField(Class<?> clazz, String columnName) {
		for (Field field : clazz.getDeclaredFields()) {
			Column columnAnnotation = field.getAnnotation(Column.class);
			JoinColumn joinColumnAnnotation = field.getAnnotation(JoinColumn.class);

			if (columnAnnotation != null && StringUtils.equalsIgnoreCase(columnAnnotation.name(), columnName)) {
				return field;
			}

			if (joinColumnAnnotation != null && StringUtils.equalsIgnoreCase(joinColumnAnnotation.name(), columnName)) {
				return field;
			}

			if (StringUtils.equalsIgnoreCase(field.getName(), columnName)) {
				return field;
			}
		}
		return null;
	}

	private static Field searchCurrentClassField(Class<?> clazz, String columnName) {
		Field field = searchClassField(clazz, columnName);
		if (field != null) {
			return field;
		}

		JsonSubTypes jsonSubTypes = clazz.getAnnotation(JsonSubTypes.class);
		if (jsonSubTypes != null && ArrayUtils.isNotEmpty(jsonSubTypes.value())) {
			for (JsonSubTypes.Type type : jsonSubTypes.value()) {
				field = searchClassField(type.value(), columnName);
				if (field != null) {
					return field;
				}
				field = searchSuperClassField(type.value(), clazz, columnName);
				if (field != null) {
					return field;
				}


			}
		}

		return null;
	}

	private static Field searchSuperClassField(Class<?> clazz, Class<?> stopClass, String columnName) {
		Class<?> superclass = clazz.getSuperclass();
		if (superclass != null && !stopClass.equals(superclass)) {
			Field field = searchCurrentClassField(superclass, columnName);
			if (field != null) {
				return field;
			}

			return searchSuperClassField(superclass, stopClass, columnName);
		}

		return null;
	}

	private static void addClassFields(Class<?> clazz, Map<String, Field> classFields) {
		for (Field field : clazz.getDeclaredFields()) {

			if (!Modifier.isStatic(field.getModifiers())) {
				if (!Modifier.isFinal(field.getModifiers())) {
					if (!field.isAnnotationPresent(Transient.class)) {
						classFields.put(field.getName(), field);
					}
				}
			}
		}
	}

	private static void addCurrentClassFields(Class<?> clazz, Map<String, Field> classFields) {
		addClassFields(clazz, classFields);

		JsonSubTypes jsonSubTypes = clazz.getAnnotation(JsonSubTypes.class);
		if (jsonSubTypes != null && ArrayUtils.isNotEmpty(jsonSubTypes.value())) {
			for (JsonSubTypes.Type type : jsonSubTypes.value()) {
				addClassFields(type.value(), classFields);

				addSuperClassFields(type.value(), clazz, classFields);
			}
		}
	}

	private static void addSuperClassFields(Class<?> clazz, Class<?> stopClass, Map<String, Field> classFields) {
		Class<?> superclass = clazz.getSuperclass();
		if (superclass != null && !stopClass.equals(superclass)) {
			addCurrentClassFields(superclass, classFields);

			addSuperClassFields(superclass, stopClass, classFields);
		}
	}

	private ModelReflectionUtils() {
	}
}
