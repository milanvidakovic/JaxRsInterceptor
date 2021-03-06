# JaxRsInterceptor

This is JaxRs-based Authentication and Authorization framework.

To use it, you need to place following annotations in your code:

* In your application class, add the @Scanner annotation with the top-level package name for annotation scanning.

```java
@Scanner("com.minja")   // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
@ApplicationPath("/rest")
public class JaxRsApp extends Application {

}
```

* On all REST endpoints where you need Authentication and/or Authorization, add @JwtSecurity annotation:

```java
@JwtSecurity
@GET
@Path("/getall")
@Produces(MediaType.APPLICATION_JSON)
public Collection<Student> getStudenti(@Context HttpServletRequest request) {
...
}
```

or:

```java
@JwtSecurity(role = "ROLE_ADMIN")
@PUT
@Path("/update")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public Student update(Student fromRequest, @Context HttpServletRequest request) {
...
}
```

* You need to have a field that holds the JWT key. Place @JwtKey annotation above that field:

```java
@JwtKey
public static Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
```

* You need to have a class that is used to fetch user using the credentials embedded in the JWT token.
Place @UserProvider annotation above that class and @UserGetter above the method used to obtain a user: 

```java
@UserProvider
public class UserRepo {
  @UserGetter
  public User getUser(String username) {
    ...
  }
}
```

If you want to use Authorization, your User class must contain a filed named *role* which holds roles (usually as enums).

All you need to do is to place the agent.jar in WEB-INF/lib folder and modify server:

## Tomcat: 
* Add at the end of VM params:  

```
-javaagent:<path-to-tomcat>/lib/aspectjweaver-1.9.6.jar
```

* You also need to place following files in the <TOMCAT_HOME>/lib folder:
    * aspectjweaver-1.9.6.jar
    * aspectjrt-1.9.6.jar
    * aspectjtools-1.9.6.jar

## Wildfly: 
* Add at the end of VM params: 

```
-Djboss.modules.system.pkgs=org.jboss.byteman,org.jboss.logmanager -Djava.util.logging.manager=org.jboss.logmanager.LogManager -javaagent:<path-to-aspectj>/aspectjweaver-1.9.6.jar
```
* Add following jars to the `Servers -> Wildfly Runtime Server -> Open launch configuration -> Class Path -> User Entries`:
    * <WILDFLY_HOME>/modules/system/layers/base/org/jboss/logmanager/main/jboss-logmanager-(version).jar
    * <WILDFLY_HOME>/modules/system/layers/base/org/jboss/log4j/logmanager/main/log4j-jboss-logmanager-(version).jar    
