#!/usr/bin/env bash
set -eo pipefail



################ ADAPT 3 FUNCTIONS TO CVE AND EXPLOIT



# Starts any helper programs/daemons required by the exploit (if any). Simply return 0 if no helpers are needed.
function start_helpers() {
    return 0
}

# Runs the exploit.
function run_exploit() {
    # JAR to test
    local JAR=$1
    
    # Run the Java class, env variable PAYLOAD_FILE required when running the container
    java -Dcom.sun.jndi.ldap.object.trustURLCodebase=true -cp "/app/classes:/app/deps/*:/app/lib/$JAR" com.acme.foo.DeserialPoC "$PAYLOAD_FILE"
}

# Checks whether the exploit worked or not. Returns 0 if it did, 1 otherwise. Also prints what has been checked.
function validate_exploit_success() {
    
    # Env variable CVE required when running the container
    local FILENAME="$CVE.txt"
    
    # The exploit creates this file in folder /app
    if [ -f "$FILENAME" ]; then
        printf "file '%s' has been created\n" "$FILENAME"
        return 0
    else
        printf "file '%s' has not been created\n" "$FILENAME"
        return 1
    fi
}



################ DO NOT EDIT BELOW

DELIM="====="

function help() {
    printf -- "Usage: %s <JAR>\n\n" "$0"
}

# Check that the user provided a JAR file
if [ -z "$1" ]; then
    printf "Specify a JAR file to be tested\n"
    help
    exit 1
fi
JAR_FILE=$(basename "$1")
if [ ! -f "/app/lib/$JAR_FILE" ]; then
    printf "JAR file '%s' does not exist in directory /app/lib/\n" "$JAR_FILE"
    help
    exit 1
fi

# Determine the mode, either validate exploit or patch
if [[ $JAR_FILE == *"-endor-"* ]]; then
    printf "VALIDATE PATCH (i.e. the patched library version %s cannot be exploited)\n$DELIM\n" "$JAR_FILE"
    MODE="patch"
else
    printf "VALIDATE EXPLOIT (i.e. the exploit works against the vulnerable library version %s)\n$DELIM\n" "$JAR_FILE"
    MODE="exploit"
fi

# Delete the other file
find "/app/lib" -type f ! -name "$JAR_FILE" -exec rm -f {} +

# 1) Setup stage
start_helpers
RETURN_CODE=$?
if [ $RETURN_CODE -ne 0 ]; then
    echo "Failed to start helpers, aborting ..."
    exit $RETURN_CODE
fi

# 2) Run the exploit
_=$(run_exploit "$JAR_FILE")

# 3) Check whether exploit worked
set +e
RESULT=$(validate_exploit_success)
RETURN_CODE="$?"
set -e

# Return 0 if the exploit worked, 1 otherwise
if [ "$MODE" = "exploit" ]; then
    if [[ $RETURN_CODE == 0 ]]; then
        printf "$DELIM\nSUCCESS: Exploit worked, %s\n" "$RESULT"
        exit 0
    else
        printf "$DELIM\nFAILURE: Exploit failed, %s\n" "$RESULT"
        exit 1
    fi

# Return 1 if the exploit worked, 0 otherwise
elif [ "$MODE" = "patch" ]; then
    if [[ $RETURN_CODE == 0 ]]; then
        printf "$DELIM\nFAILURE: Exploit worked, %s\n" "$RESULT"
        exit 1
    else
        printf "$DELIM\nSUCCESS: Exploit failed, %s\n" "$RESULT"
        exit 0
    fi
fi
