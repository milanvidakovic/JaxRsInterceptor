# JaxRsInterceptor

This is JaxRs-based Authentication and Authorization framework.

To use it, you need to place following annotations in your code:

* In your application class, add the @Scanner annotation with the top-level package name for annotation scanning.
@Scanner("com.minja")   // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
@ApplicationPath("/rest")
public class JaxRsApp extends Application {

}

* On all REST endpoints where you need Authentication and/or Authorization, add @JwtSecurity annotation:
@JwtSecurity
@GET
@Path("/getall")
@Produces(MediaType.APPLICATION_JSON)
public Collection<Student> getStudenti(@Context HttpServletRequest request) {
...
}

or:

@JwtSecurity(role = "ROLE_ADMIN")
@PUT
@Path("/update")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public Student update(Student fromRequest, @Context HttpServletRequest request) {
...
}

* You need to have a field that holds the JWT key. Place @JwtKey annotation above that field:
@JwtKey
public static Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

* You need to have a class that is used to fetch user using the credentials embedded in the JWT token.
Place @UserProvider annotation above that class and @UserGetter above the method used to obtain a user:
@UserProvider
public class UserRepo {
  @UserGetter
  public User getUser(String username) {
    ...
  }
}

If you want to use Authorization, your User class must contain a filed named *role* which holds roles (usually as enums).

All you need to do is to place the agent.jar in WEB-INF/lib folder and start:
1. Tomcat: Add at the end of VM params:  -javaagent:c:\apache-tomcat-7.0.94\lib\aspectjweaver-1.9.6.jar
2. Wildfly: -Djboss.modules.system.pkgs=org.jboss.byteman,org.jboss.logmanager -Djava.util.logging.manager=org.jboss.logmanager.LogManager -javaagent:c:\apache-tomcat-7.0.94\lib\aspectjweaver-1.9.6.jar
