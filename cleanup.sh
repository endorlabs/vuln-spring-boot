command1="java -jar lib/JNDI-Injection-Exploit-1.0-SNAPSHOT-all.jar -C open https://haveibeenpwned.com -A 127.0.0.1"
command2="java -Dcom.sun.jndi.ldap.object.trustURLCodebase=true -jar target/springboot-app-0.0.1-SNAPSHOT.jar"

# Find the PID of the process
pid1=$(pgrep -f "$command1")

if [ -n "$pid1" ]; then
    # If PID found, kill the process
    echo "Killing process $pid1 for command: $command1"
    kill "$pid1"
else
    echo "No process found for command: $command1"
fi
pid2=$(pgrep -f "$command2")

if [ -n "$pid2" ]; then
    # If PID found, kill the process
    echo "Killing process $pid2 for command: $command2"
    kill "$pid2"
else
    echo "No process found for command: $command2"
fi