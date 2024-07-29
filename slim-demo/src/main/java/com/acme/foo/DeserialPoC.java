package com.acme.foo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DeserialPoC {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Provide a file with a JSON payload as argument");
            System.exit(1);
        }

        LdapServer ldap_server = new LdapServer().run();
        ObjectMapper mapper = new ObjectMapper();
        mapper.enableDefaultTyping();
        try {
            mapper.readValue(new File(args[0]), Object.class);
        } catch (IOException e) {
            System.err.println(e.getClass().getSimpleName() + " when deserializing: " + e.getMessage());
        }
        ldap_server.shutdown();

        new com.fasterxml.jackson.databind.jsontype.impl.ClassNameIdResolver(null, null);
    }
}