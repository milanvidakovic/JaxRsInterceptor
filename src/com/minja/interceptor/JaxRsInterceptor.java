package com.minja.interceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.Key;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import com.minja.interceptor.annotations.JwtKey;
import com.minja.interceptor.annotations.JwtSecurity;
import com.minja.interceptor.annotations.Scanner;
import com.minja.interceptor.annotations.UserGetter;
import com.minja.interceptor.annotations.UserProvider;

import eu.infomas.annotation.AnnotationDetector;
import eu.infomas.annotation.AnnotationDetector.FieldReporter;
import eu.infomas.annotation.AnnotationDetector.TypeReporter;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;

@Aspect
public class JaxRsInterceptor {
	/**
	 * We try to intercept a constructor which has @com.minja.interceptor.annotations.JwtSecurity
	 * annotation above. 
	 */ 
	@Pointcut("execution(* *..*(..)) && @annotation(com.minja.interceptor.annotations.JwtSecurity)")
	void aroundHandle3() {
	}

	private Method hasGetHeader(Class<?> cls) {
		try {
			return cls.getDeclaredMethod("getHeader", String.class);
		} catch (Exception ex) {
			return null;
		}
	}

	@Around("aroundHandle3()")
	public Object aroundHandleAdvice3(ProceedingJoinPoint jp) throws Throwable {
		String methodName = jp.getSignature().getName();
		System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&: " + methodName);
		boolean foundRequest = false;
		/* Go through all arguments and try to find injected
		 * HttpServletRequest argument.
		 */
		for (Object o : jp.getArgs()) {
			Class<?> cls = o.getClass();
			Method getHeader = hasGetHeader(cls);
			System.out.println("Argument type: " + cls.getName());
			if (getHeader != null) {
				foundRequest = true;
				String auth = (String) getHeader.invoke(o, "Authorization");
				if ((auth != null) && (auth.contains("Bearer "))) {
					/*
					 * We found the JWT token in the HTTP request.
					 */
					System.out.println("FOUND AUTHORIZATION U HTTP REQUEST HEADER!");
					String jwt = auth.substring(auth.indexOf("Bearer ") + 7);
					Jws<Claims> claims = Jwts.parserBuilder().setSigningKey(jwtKey).build().parseClaimsJws(jwt);
					// if it fails, it will throw an exception
					String username = claims.getBody().getSubject();
					System.out.println("User " + username + " logged in.");
					/*
					 * We now try to use @UserProvider-annotated class to obtain 
					 * user from the JWT token.
					 */
					Object user = userGetter.invoke(userProvider, username);
					if (user == null)
						throw new RuntimeException("Unknown user!");

					/*
					 * Now we look for the role attribute in the JwtSecurity annotation.
					 */
					MethodSignature signature = (MethodSignature) jp.getSignature();
					Method method = signature.getMethod();
					JwtSecurity security = method.getAnnotation(JwtSecurity.class);
					String role = security.role();
					if (!role.equals("")) {
						System.out.println("ROLE: " + role);
						Field roleF = user.getClass().getDeclaredField("role");
						if (roleF != null) {
							Object roleO = roleF.get(user);
							System.out.println("Current user Role: " + roleO.toString());
							if (!role.equals(roleO.toString())) {
								throw new RuntimeException("Insufficient priviledges");
							}
						}
					}
					break;
				} else throw new RuntimeException("No or invalid JWT token");
			} 
		}

		if (!foundRequest)
			throw new RuntimeException(
					"No HttpServletRequest injected as @Context argument to " + methodName + " function");

		Object target = jp.getTarget();
		String className = target.toString();
		System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&: " + className);
		return jp.proceed();
	}

	/** Object which acts User Provider.
	 * Any object can be this, providing that it has a method annotated with
	 * @UserGetter annotation.
	 */
	Object userProvider;
	/** An actual method in User Provider which return User object.
	 * If you want to have roles, that User object should have a field named Role.
	 */
	Method userGetter;
	/** JWT key found in intercepted code. */
	Key jwtKey;

	/**
	 * We try to intercept a constructor with @Scanner annotation above.
	 * When we find that constructor, we will look for the package name to be scanned.
	 * We will scan those packages (and subpackages) to find two things:
	 * <ol>
	 * <li>Use Repository annotated class which will be used to check if the 
	 * JWT-embedded user can perform the action</li>
	 * <li>JWT key used to sign JWT token</li>
	 * </ol>  
	 */
	@Pointcut("execution((@com.minja.interceptor.annotations.Scanner *).new(..))")
	void initService() {
	}

	@After("initService()")
	public void initServiceAdvice(JoinPoint tp) throws Throwable {
		// Report all .class files, with @UserProvider annotation
		final TypeReporter reporter1 = new TypeReporter() {

			@SuppressWarnings("unchecked")
			@Override
			public Class<? extends Annotation>[] annotations() {
				return new Class[] { UserProvider.class };
			}

			@Override
			public void reportTypeAnnotation(Class<? extends Annotation> annotation, String className) {
				System.out.println("FOUND USER PROVIDER: " + className);
				try {
					Class<?> cls = Class.forName(className);
					userProvider = cls.newInstance();
					Method[] methods = cls.getDeclaredMethods();
					for (Method m : methods) {
						if (m.isAnnotationPresent(UserGetter.class)) {
							userGetter = m;
							System.out.println("FOUND USER GETTER: " + m);
							break;
						}
					}
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}

		};
		final AnnotationDetector ad1 = new AnnotationDetector(reporter1);

		final FieldReporter reporter2 = new FieldReporter() {

			@SuppressWarnings("unchecked")
			@Override
			public Class<? extends Annotation>[] annotations() {
				return new Class[] { JwtKey.class };
			}

			@Override
			public void reportFieldAnnotation(Class<? extends Annotation> annotation, String className,
					String fieldName) {
				System.out.println("JWT KEY FIELD: " + className + "." + fieldName + "->" + annotation);
				try {
					try {
						Class<?> cls = Class.forName(className);
						Field field = cls.getDeclaredField(fieldName);
						/*
						 * When we find a field with the @JwtKey annotation,
						 * we save that field so we can use it for the JWT processing.
						 */
						JaxRsInterceptor.this.jwtKey = (Key) field.get(null);
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} catch (NoSuchFieldException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				}
			}

		};
		final AnnotationDetector ad2 = new AnnotationDetector(reporter2);

		Object target = tp.getTarget();
		String className = target.toString();
		System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!: " + className);
		
		Class<?> cls = target.getClass();
		Scanner scanner = cls.getDeclaredAnnotation(Scanner.class);
		String pack = scanner.value();
		if (pack != null) {
			ad1.detect(pack);
			ad2.detect(pack);
		} else {
			System.err.println("ERROR: missing scanning package definition in the @Scanner annotation");
		}
	}

}
