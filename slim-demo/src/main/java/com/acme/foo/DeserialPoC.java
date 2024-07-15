package com.acme.foo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DeserialPoC {

    private static final Logger logger = org.apache.logging.log4j.LogManager.getLogger(DeserialPoC.class);

    public static void main(String[] args) {
        if (args.length != 1) {
            logger.error("Provide a file with a JSON payload as argument");
            System.exit(1);
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.enableDefaultTyping();
        try {
            // Compile exception as of 2.10.0, cf.
            // https://github.com/FasterXML/jackson/wiki/Jackson-Release-2.10#databind-typereference-assignment-compatibility-for-readvalue
            TypeReference ref_obj = new TypeReference<HashMap<String, ArrayList<Foo<Object>>>>() {
            };
            Map<String, List<Foo<Object>>> foo_obj = mapper.readValue(new File(args[0]), ref_obj);
            logger.info("Deserialized JSON to " + foo_obj);
        } catch (IOException e) {
            logger.error(e.getClass().getSimpleName() + " when deserializing: " + e.getMessage());
        }

        try {
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            File file = new File("file.yaml");
            InputStream inputStream = new FileInputStream(file);
            Foo<String> foo = yaml.load(inputStream);
        } catch (FileNotFoundException fnfe) {
        }

        try {
            org.verapdf.policy.PolicyChecker.applyPolicy(null, null, null);
        } catch(Exception e) {}
    }
}

class Foo<T> {
    public T bar;

    public T getBar() {
        return bar;
    }

    public void setBar(T bar) {
        this.bar = bar;
    }

    public String toString() {
        return "baz";
    }
}
