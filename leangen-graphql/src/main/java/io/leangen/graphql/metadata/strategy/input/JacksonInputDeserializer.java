package io.leangen.graphql.metadata.strategy.input;

import java.lang.reflect.Type;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Created by bojan.tomic on 6/6/16.
 */
public class JacksonInputDeserializer implements InputDeserializer {

	private ObjectMapper objectMapper;

	public JacksonInputDeserializer() {
		this.objectMapper = new ObjectMapper();
	}

	@Override
	public <T> T deserialize(Object graphQlInput, Type type) {
		return objectMapper.convertValue(graphQlInput, objectMapper.getTypeFactory().constructType(type));
	}
}
