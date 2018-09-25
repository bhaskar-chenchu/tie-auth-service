FROM openjdk:8-jre-alpine
MAINTAINER tie-team <tie-team@volvocars.com>
ENTRYPOINT ["/usr/bin/java", "-Dspring.profiles.active=docker","-jar", "/usr/share/myservice/authorization.jar"]
EXPOSE 8080
ARG JAR_FILE
ADD target/${JAR_FILE} /usr/share/myservice/authorization.jar