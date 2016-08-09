package io.leangen.graphql.metadata.strategy.input;

import java.lang.reflect.Type;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

/**
 * Created by bojan.tomic on 5/25/16.
 */
public class GsonInputDeserializer implements InputDeserializer {

	private Gson gson;

	public GsonInputDeserializer() {
		this(new GsonFieldNamingStrategy());
	}

	public GsonInputDeserializer(FieldNamingStrategy fieldNamingStrategy) {
		this.gson = new GsonBuilder()
				.setFieldNamingStrategy(fieldNamingStrategy)
				.create();
	}

	@Override
	public <T> T deserialize(Object graphQlInput, Type type) {
		if (graphQlInput.getClass() == type) {
			return (T)graphQlInput;
		}
		JsonElement jsonElement = gson.toJsonTree(graphQlInput);
		return gson.fromJson(jsonElement, type);
	}
}