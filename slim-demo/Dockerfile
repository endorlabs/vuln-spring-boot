# Use Maven to download dependencies and compile the application against a vulnerable version
FROM maven:3.9.8-eclipse-temurin-11-alpine AS build

# Must be an existing artifact and vulnerable version (i.e. must not end on -endor-YYYYMMDD)
ARG ARTIFACT_ID
ARG VULNERABLE_VERSION

WORKDIR /workspace
COPY ./pom.xml .
COPY ./src ./src
RUN mvn clean
RUN mvn -Dlibrary-version=$VULNERABLE_VERSION dependency:copy-dependencies -DoutputDirectory=./deps
RUN mvn -Dlibrary-version=$VULNERABLE_VERSION compile

# Remove the vulnerable lib (which will be served from the lib folder)
RUN rm ./deps/${ARTIFACT_ID}-${VULNERABLE_VERSION}.jar

# The vulnerable and patched JARs
COPY ./lib/${ARTIFACT_ID}-${VULNERABLE_VERSION}.jar ./lib/
COPY ./lib/${ARTIFACT_ID}-${VULNERABLE_VERSION}-endor-*.jar ./lib/

# Use OpenJDK to run the application
FROM openjdk:11-slim

# The CVE to look at and the corresponding payload
ENV CVE CVE-UNSPECIFIED
ENV PAYLOAD_FILE AnterosDBCPConfig.json

# Do not compile again, to run the old bytecode against the patched library
WORKDIR /app
COPY --from=build /workspace/target/classes  /app/classes
COPY --from=build /workspace/deps            /app/deps
COPY --from=build /workspace/lib             /app/lib
COPY --from=build /workspace/src/test/*.json /app/payloads/
COPY ./run.sh .
RUN chmod +x /app/run.sh

# Run the script that validates the exploit and patch (depending on the argument)
#ENTRYPOINT ["/bin/bash"]
ENTRYPOINT ["/app/run.sh"]
