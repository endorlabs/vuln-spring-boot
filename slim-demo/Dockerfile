FROM maven:3.9.8-eclipse-temurin-11-alpine

COPY . /poc
RUN chmod 755 /poc/run.sh

ARG JACKSON_DATABIND_VERSION

WORKDIR /poc
#RUN mvn clean -Djackson-databind-version=2.9.10.3 package

CMD /bin/bash

#"java", "-Dcom.sun.jndi.ldap.object.trustURLCodebase=true", "-jar", "/poc/target/poc-0.0.1-SNAPSHOT-shaded.jar", "/poc/src/test/AnterosDBCPConfig.json"]

# docker build --platform linux/amd64 --build-arg JACKSON_DATABIND_VERSION=2.9.10.3 -t poc-2.9.10.3 .