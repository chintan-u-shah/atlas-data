package com.wheelsup.jpa.audit.interceptor.audit;

import org.apache.commons.lang3.StringUtils;

import javax.lang.model.type.NullType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Audited {

	DependsOn value() default @DependsOn(entity = NullType.class, column = StringUtils.EMPTY);

	@interface DependsOn {

		Class<?> entity();

		String column();
	}
}
