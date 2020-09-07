package com.minja.interceptor.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Place this annotation above any REST method that needs JWT authentication and authorization.
 * If you want to restrict that method by user role, then fill the <code>role</code> attribute
 * with the role name that can call that method.
 * @see com.minja.interceptor.annotations.UserProvider
 * @see com.minja.interceptor.annotations.UserGetter
 * @author minja
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JwtSecurity {
	/**
	 * This attribute is optional. Use if you want to restrict REST method to only those users with the given role.
	 * @return role name
	 */
	public String role() default "";
}
