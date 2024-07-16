

# Demo

## Vulnerable version

```
mvn -Djackson-databind-version=2.9.10.3 clean package
java -jar target/poc-0.0.1-SNAPSHOT-shaded.jar src/test/AnterosDBCPConfig.json
```

## Patched version (from project)

```
mvn -Djackson-databind-version=2.9.10.4 clean package
java -jar target/poc-0.0.1-SNAPSHOT-shaded.jar src/test/AnterosDBCPConfig.json
```

## Patched version (from Endor)

```
<TODO>
```

## Compile exception (see [here](https://github.com/FasterXML/jackson/wiki/Jackson-Release-2.10#databind-typereference-assignment-compatibility-for-readvalue))

```
mvn -Djackson-databind-version=2.10.0 clean package
java -jar target/poc-0.0.1-SNAPSHOT-shaded.jar src/test/AnterosDBCPConfig.json
```

# Exploit and patch validation in workflow 

The integration into the workflow happens by spinning up a Docker container two times: Once to validate that an exploit for the VULNERABLE library version works, another time to validate that the PATCHED library version `-endor-YYYYMMDD` cannot be exploited any more.

## Build the image

Prerequisites:
- `Dockerfile` and `run.sh` adapted to the specific vulnerability
- Vulnerable and patched library versions in folder `lib`

Build the Docker image once per CVE and library version:
- Choose the right `--platform`
- Specify the vulnerable library version
- Create an appropriate tag

Example:
```
docker build --platform linux/amd64  --build-arg JACKSON_DATABIND_VERSION=2.9.10.3 --tag val-cve-2020-9547 . 
```

## Validate the exploit

Run the image and provide the necessary arguments:
- Environment variable `PAYLOAD_FILE`: Path to malicious payload (absolute path in container)
- Env variable `CVE`
- Path to vulnerable JAR (can be local)

Example:
```
slim-demo (slim-demo *) % docker run --platform linux/amd64 -e "PAYLOAD_FILE=/app/payloads/AnterosDBCPConfig.json" -e "CVE=CVE-2020-9547" val-cve-2020-9547 lib/jackson-databind-2.9.10.3.jar
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
slim-demo (slim-demo *) % docker run --platform linux/amd64 -e "PAYLOAD_FILE=/app/payloads/AnterosDBCPConfig.json" -e "CVE=CVE-2020-9547" val-cve-2020-9547 lib/jackson-databind-2.9.10.3-endor-20240716.jar 
VALIDATE PATCH (i.e. the patched library version jackson-databind-2.9.10.3-endor-20240716.jar cannot be exploited)
=====
InvalidDefinitionException when deserializing: Invalid type definition for type `br.com.anteros.dbcp.AnterosDBCPConfig`: Illegal type (br.com.anteros.dbcp.AnterosDBCPConfig) to deserialize: prevented for security reasons
 at [Source: (File); line: 5, column: 13] (through reference chain: java.util.HashMap["foo"]->java.util.ArrayList[0])
=====
SUCCESS: Exploit failed, file 'CVE-2020-9547.txt' has not been created
```