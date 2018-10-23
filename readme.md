####
This is a demo project with an expected lifetime of 6 weeks, it provides a configured OAuth2 server
as a standalone spring boot application (with only JWT support)
The authorization server also has support for storing end-users
and authenticating them using the password grant type.


####
Structure:

Authorization-server, this module builds a standalone
spring boot app with an inmemory DB that can be used as an
authorization server. There is also a Dockerfile
if you want to create a docker container for this service

sample-micro-service, this module builds a standalone
spring boot app that is a Resource-Server and can
be configured to use the Authorization server app as its
authorization server (see its yaml config) there is also
a Dockerfile if you want to create a docker container for
this service.

kubernetes, this module holds the deployment files
witch will install the corresponding docker container
to f.ex minikube.

###
Usage:

[As standalone process]
 mvn springboot:run /authroization-server/pom.xml
 mvn springboot:run /sample-micro-service/pom.xml

[With docker]
 mvn clean install /authroization-server/pom.xml -P create-docker-image
 mvn clean install /sample-micro-service/pom.xml -P create-docker-image

verify they got installed in your docker repo with f.ex ```docker images```

[With kubernetes]
kubectl apply -f  /kubernetes/authorization-server-deployment.yml

