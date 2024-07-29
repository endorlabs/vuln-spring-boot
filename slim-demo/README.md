
# Demo

## Pre-requisites

- Have Java and Maven installed
- Configure a settings.xml file with an API key and secret as the user and password.

Your settings.xml file should look like this: 

Please make sure to replace the username with your Endor Labs API key and the password with your Endor Labs API Key Secret.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              http://maven.apache.org/xsd/settings-1.0.0.xsd">
    <servers>
        <server>
            <id>endorlabs</id>
            <username>endr+XXXXXXX</username>
            <password>endr+XXXXXXX</password>
        </server>
    </servers>
</settings>
```

Prior to the demo go to the pom.xml file at slim-demo/pom.xml and update the <namespace> to be the namespace you used for your API key. The section should look like this:

```xml
 <!--
	<repositories>
	<repository>
		<id>endorlabs</id>
		<url>https://factory.endorlabs.com/v1/namespaces/<namespace>/maven2</url>
	</repository>
	</repositories>
-->
```

## Vulnerable version

```
mvn -Dlibrary-version=2.9.10.3 clean package
java -jar target/poc-0.0.1-SNAPSHOT-shaded.jar src/test/AnterosDBCPConfig.json
```

Explanation: The malicious class is served through an LDAP server, and loaded when JSON deserializes the payload `src/test/AnterosDBCPConfig.json`. Upon class loading, its static initializer is executed and creates a file `CVE-2020-9547.txt` in the work directory, and prints a console message.

PS: The exception printed afterwards is due to a class cast exception that happens after the class has been loaded. At this time, the malicious code already ran.

## Compile exception

```
mvn -Dlibrary-version=2.10.0 clean package
```

Explanation:
- The visibility of the public constructor `ClassNameIdResolver(JavaType, TypeFactory)` was changed to `protected` as of version [2.10.0](https://github.com/FasterXML/jackson-databind/blob/a1eedfdeea46f2a8da0ed23f06e7e1d39050499b/src/main/java/com/fasterxml/jackson/databind/jsontype/impl/ClassNameIdResolver.java), which leads to a compile exception.
- There's no single commit changing the visibility from `public` to `protected`. Instead, they first deleted the constructor with [c456a08a2adccad646040d5ad5c7d4558c28745e](https://github.com/FasterXML/jackson-databind/commit/c456a08a2adccad646040d5ad5c7d4558c28745e#diff-5e05c51585370aaf3b9b7fc8489ee4feafc3e56c846b3f5f0fbef5570fa6769b), and re-introduced it with [e2859a691e1c30d7454df7a6ddb140ceffc6c78c](https://github.com/FasterXML/jackson-databind/commit/e2859a691e1c30d7454df7a6ddb140ceffc6c78c#diff-5e05c51585370aaf3b9b7fc8489ee4feafc3e56c846b3f5f0fbef5570fa6769b), having protected visibility.
- BC candidates between 2.9.10.3 and 2.10.0 can be found by running the following command (whereby the config file needs to point to the respective library CGs):
```
bazel run //src/golang/internal.endor.ai/pkg/x/peekimpact:peekimpact -- --config config.json --debug=1 --reach=false --json-summary=9-10-diff.json
```
- The BC in question looks as follows:
```
{
  "functionChanges": [
    {"defined":true,"diffChange":"Changed","functionRef":"java://com.fasterxml.jackson.core:jackson-databind$2.9.10.3/com.fasterxml.jackson.databind.jsontype.impl/ClassNameIdResolver.\u003cinit\u003e(/com.fasterxml.jackson.databind/JavaType,/com.fasterxml.jackson.databind.type/TypeFactory)/java.lang/VoidType","modifierChange":"Public to protected"},
```

## Patched version (from project)

First remove the text file created and then run the same command with the Endor Labs patch:

```bash
mvn -Dlibrary-version=2.9.10.3-endor-latest clean package
java -jar target/poc-0.0.1-SNAPSHOT-shaded.jar src/test/AnterosDBCPConfig.json
```

Explanation: The [patch](https://github.com/FasterXML/jackson-databind/commit/03f30bf11c9315c3acd4ec8db97a2f22dbbc2f94) extends the deny-list with known gadget types such as class `AnterosDBCPConfig`. This deny-list is checked before deserialization, and throws an exception with message `Illegal type (br.com.anteros.dbcp.AnterosDBCPConfig) to deserialize: prevented for security reasons`.

# Exploit and patch validation in workflow 

The integration into the workflow happens by spinning up a Docker container two times: Once to validate that an exploit for the VULNERABLE library version works, another time to validate that the PATCHED library version `-endor-YYYYMMDD` cannot be exploited any more.

Goals:
- Simple:
    - One Dockerfile for multiple vulnerable versions, e.g. `2.9.10.3` and `2.9.10.4`
    - One Docker image to validate the exploit and patch for a given version
- The return code of the Docker container indicates whether the validation of exploit and patch were successful
- Make it easy to adapt the Dockerfile and the entry point `run.sh` to other vulns
- Do not recompile client when running against patched version

## Build the image


Directory structure of tarball:
```
.
├── Dockerfile
├── run.sh
├── pom.xml
├── lib/
│   ├── <artifact>-<vul_version>.jar
│   └── <artifact>-<vul_version>-endor-<yyyymmdd>.jar
└── src/
    ├── main/
    │   └── java/
    │       └── ...
    └── test/
        └── payload.txt (if any)
```

Directory structure of Docker image:

```
.
├── workspace/ (used to compile the classes)
│   ├── pom.xml
│   ├── src
│   │   └── ...
│   ├── target
│   │   └── ...
│   ├── deps/ (contains all deps except the vulnerable artifact)
│   └── lib
│       ├── <artifact>-<vul_version>.jar
│       └── <artifact>-<vul_version>-endor-<yyyymmdd>.jar
└── app (used to run the app)
    ├── run.sh
    ├── classes/ (compiled classes fro workspace/target/classes)
    ├── deps/
    └── lib/
```

Adapt to new vulns:
- Change `Dockerfile` and `run.sh` for the specific vulnerability
- Put vulnerable and patched library versions in folder `lib`

Build the Docker image once per CVE and library version:
- Choose the right `--platform`
- Specify the artifact and vulnerable library version with build args `ARTIFACT_ID` and `VULNERABLE_VERSION`
- Create an appropriate `--tag`

Example:
```
docker build --platform linux/amd64 --build-arg ARTIFACT_ID=jackson-databind --build-arg VULNERABLE_VERSION=2.9.10.3 --tag validate-cve-2020-9547 .
```

## Validate the exploit

Run the image and provide the necessary arguments:
- Environment variable `PAYLOAD_FILE`: Path to malicious payload (absolute path in container)
- Env variable `CVE`
- Path to vulnerable JAR (can be local)

Example:
```
slim-demo (slim-demo *) % docker run --platform linux/amd64 -e "PAYLOAD_FILE=/app/payloads/AnterosDBCPConfig.json" -e "CVE=CVE-2020-9547" validate-cve-2020-9547 lib/jackson-databind-2.9.10.3.jar
VALIDATE EXPLOIT (i.e. the exploit works against the vulnerable library version jackson-databind-2.9.10.3.jar)
=====
JsonMappingException when deserializing: class Malicious cannot be cast to class javax.naming.spi.ObjectFactory (Malicious is in unnamed module of loader 'app'; javax.naming.spi.ObjectFactory is in module java.naming of loader 'bootstrap')
 at [Source: (File); line: 6, column: 40] (through reference chain: java.util.HashMap["foo"]->java.util.ArrayList[0]->br.com.anteros.dbcp.AnterosDBCPConfig["healthCheckRegistry"])
=====
SUCCESS: Exploit worked, file 'CVE-2020-9547.txt' has been created
```

## Validate the patch

Run the image and provide the necessary arguments:
- Environment variable `PAYLOAD_FILE`: Path to malicious payload (absolute path in container)
- Env variable `CVE`
- Path to patched JAR (can be local)

Example:
```
slim-demo (slim-demo *) % docker run --platform linux/amd64 -e "PAYLOAD_FILE=/app/payloads/AnterosDBCPConfig.json" -e "CVE=CVE-2020-9547" validate-cve-2020-9547 lib/jackson-databind-2.9.10.3-endor-20240716.jar 
VALIDATE PATCH (i.e. the patched library version jackson-databind-2.9.10.3-endor-20240716.jar cannot be exploited)
=====
InvalidDefinitionException when deserializing: Invalid type definition for type `br.com.anteros.dbcp.AnterosDBCPConfig`: Illegal type (br.com.anteros.dbcp.AnterosDBCPConfig) to deserialize: prevented for security reasons
 at [Source: (File); line: 5, column: 13] (through reference chain: java.util.HashMap["foo"]->java.util.ArrayList[0])
=====
SUCCESS: Exploit failed, file 'CVE-2020-9547.txt' has not been created
```
