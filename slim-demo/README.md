

# Demo

## Vulnerable version

```
mvn -q -Dlibrary-version=2.9.10.3 clean package
java -jar target/poc-0.0.1-SNAPSHOT-shaded.jar src/test/AnterosDBCPConfig.json
```

Explanation: The malicious class is served through an LDAP server, and loaded when JSON deserializes the payload `src/test/AnterosDBCPConfig.json`. Upon class loading, its static initializer is executed and creates a file `CVE-2020-9547.txt` in the work directory, and prints a console message.

PS: The exception printed afterwards is due to a class cast exception that happens after the class has been loaded. At this time, the malicious code already ran.

## Compile exception

```
mvn -q -Dlibrary-version=2.10.0 clean package
```

Explanation:
- Java generics are handled differently by version 2.10.0, which can lead to compile exceptions according to the [2.10.0 release notes](https://github.com/FasterXML/jackson/wiki/Jackson-Release-2.10#databind-typereference-assignment-compatibility-for-readvalue): "[...] you CAN NOT use subtype of a type variable, so this DOES NOT compile. [...] One thing to note is that this change IS binary-compatible (so anything compiled against 2.9 will still link fine against 2.10), but NOT source-compatible. This means that change should not cause any issues with transitive dependencies; but will cause compilation failure."
- Developers using generics in such way cannot easily update to non-vulnerable versions.

## Patched version (from project)

```
mvn -q -Dlibrary-version=2.9.10.4 clean package
java -jar target/poc-0.0.1-SNAPSHOT-shaded.jar src/test/AnterosDBCPConfig.json
```

Explanation: The [patch](https://github.com/FasterXML/jackson-databind/commit/03f30bf11c9315c3acd4ec8db97a2f22dbbc2f94) extends the deny-list with known gadget types such as class `AnterosDBCPConfig`. This deny-list is checked before deserialization, and throws an exception with message `Illegal type (br.com.anteros.dbcp.AnterosDBCPConfig) to deserialize: prevented for security reasons`.

## Patched version (from Endor)

```
mvn -q -Dlibrary-version=2.9.10.3-endor-latest clean package
java -jar target/poc-0.0.1-SNAPSHOT-shaded.jar src/test/AnterosDBCPConfig.json
```

TODO: Configure pom.xml

The patched version raises the same exception as the original fix.

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


Directory structure:
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
- Develop a vulnerable app in `src/`
- Adapt functions `start_helper`, `run_exploit` and `validate_exploit_success` in bash script `run.sh`
- Adapt `Dockerfile` (if necessary)
- Include vulnerable and patched library versions in folder `lib`

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