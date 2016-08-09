package io.leangen.graphql.generator.strategy;

import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import graphql.schema.GraphQLTypeReference;
import io.leangen.graphql.generator.QueryRepository;
import io.leangen.graphql.metadata.DomainType;

/**
 * Created by bojan.tomic on 3/29/16.
 */
public class CappedTypeGenerationStrategy extends AbstractTypeGenerationStrategy {

	protected Map<String, String> fingerprints = new HashMap<>();

	public CappedTypeGenerationStrategy(QueryRepository queryRepository) {
		super(queryRepository);
	}

	@Override
	public Entry get(DomainType domainType, List<String> parentTrail) {
		String fg = fingerprint(domainType.getJavaType(), parentTrail, new HashSet<>());
		return getExactMatch(fg)
				.orElseGet(() -> getFallback(domainType, parentTrail, fg)
						.orElseGet(() -> getNew(domainType, parentTrail, fg)));
	}

	protected Optional<Entry> getExactMatch(String fingerprint) {
		if (fingerprints.containsKey(fingerprint)) {
			return Optional.of(new Entry(fingerprints.get(fingerprint), new GraphQLTypeReference(fingerprints.get(fingerprint))));
		}
		return Optional.empty();
	}

	protected  Optional<Entry> getFallback(DomainType domainType, List<String> parentTrail, String fingerprint) {
		return Optional.empty();
	}

	protected Entry getNew(DomainType domainType, List<String> parentTrail, String fingerprint) {
		String typeName = generateName(domainType, parentTrail);
		fingerprints.put(fingerprint, typeName);
		return new Entry(typeName, null);
	}

	protected String fingerprint(AnnotatedType type, List<String> parentTrail, Set<String> knownFingerprints) {

		StringBuilder fingerprint = new StringBuilder(type.getType().getTypeName().toLowerCase());
		fingerprint.append(Arrays.toString(type.getAnnotations()));
		queryRepository.getChildQueries(parentTrail, type).forEach(childQuery -> fingerprint.append("->").append(childQuery.getName()).append("@").append(childQuery.getFingerprint()));

		if (knownFingerprints.contains(fingerprint.toString())) return fingerprint.toString();
		knownFingerprints.add(fingerprint.toString());
		queryRepository.getChildQueries(parentTrail, type).forEach(childQuery -> {
			List<String> trail = new ArrayList<>(parentTrail);
			trail.add(childQuery.getName());
			AnnotatedType childType = childQuery.getJavaType();
			String childFngr = fingerprint(childType, trail, knownFingerprints);
			fingerprint.append(childFngr.equals(childType.getType().getTypeName().toLowerCase() + Arrays.toString(childType.getAnnotations())) ? "" : "->" + childFngr);
		});

		return fingerprint.toString();
	}

	protected String generateName(DomainType domainType, List<String> parentTrail) {
		String candidateName = domainType.getName();
		if (!fingerprints.containsValue(candidateName)) return candidateName;
		String currentCandidate = "";
		int i = parentTrail.size() - 1;
		while (fingerprints.containsValue(candidateName) && i >= 0) {
			candidateName = currentCandidate + (currentCandidate.isEmpty() ? "" : ".") + parentTrail.get(i);
			--i;
		}
		if (i > -1) return candidateName;
		i = 1;
		while (fingerprints.containsValue(candidateName)) {
			candidateName = candidateName + "." + i;
			++i;
		}
		return candidateName;
	}
}