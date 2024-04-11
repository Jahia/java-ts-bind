package io.github.bensku.tsbind.binding;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.github.bensku.tsbind.ast.Member;
import io.github.bensku.tsbind.ast.Method;
import io.github.bensku.tsbind.ast.TypeDefinition;
import io.github.bensku.tsbind.ast.TypeRef;

/**
 * Performs early type transformations. They are required e.g. when the pass
 * might add new used types that could affect imports.
 *
 * Early transform passes can and will mutate the contents of types!
 *
 */
public class EarlyTypeTransformer {

	private final Map<String, TypeDefinition> typeTable;

	private final List<Pattern> methodWhiteListPatterns;

	public EarlyTypeTransformer(Map<String, TypeDefinition> typeTable, List<String> methodWhitelist) {
		this.typeTable = typeTable;
		this.methodWhiteListPatterns = methodWhitelist.stream().map(Pattern::compile).collect(Collectors.toList());
	}

	private void visitSupertypes(TypeDefinition type, Consumer<TypeDefinition> visitor) {
		// Call visitor only on supertypes, not the type initially given as parameter
		for (TypeRef ref : type.superTypes) {
			TypeDefinition def = typeTable.get(ref.name());
			if (def != null) {
				visitor.accept(def);
				visitSupertypes(def, visitor);
			}
		}
		for (TypeRef ref : type.interfaces) {
			TypeDefinition def = typeTable.get(ref.name());
			if (def != null) {
				visitor.accept(def);
				visitSupertypes(def, visitor);
			}
		}
	}

	/**
	 * TypeScript removes inherited overloads unless they're re-specified.
	 * As such, we copy them to classes that should inherit them.
	 */
	public void addMissingOverloads(TypeDefinition type) {
		// Figure out what methods we already have
		Set<MethodId> methods = new HashSet<>();
		for (Member member : type.members) {
			if (member instanceof Method) {
				methods.add(new MethodId((Method) member));
			}
		}

		// Visit supertypes and interfaces to see what we're missing
		visitSupertypes(type, parent -> {
			for (Member parentMember : parent.members) {
				if (parentMember instanceof Method && type.hasMember(parentMember.name())) {
					// We have a member with same name
					// If it has different signature, we need to copy the missing overload
					MethodId parentMethodId = new MethodId((Method) parentMember);
					if (!methods.contains(parentMethodId)) {
						// now we must check if the parent method is allowed by the whitelist
						if (methodWhiteListPatterns.stream().anyMatch(p -> p.matcher(type.name() + "." + parentMethodId.name).matches())) {
							type.members.add(parentMember);
						}

						type.members.add(parentMember);
					}
				}
			}
		});
	}

	public void flattenType(TypeDefinition type) {
		// Figure out what methods we already have
		Set<MethodId> typeMethodIds = new HashSet<>();
		for (Member member : type.members) {
			if (member instanceof Method) {
				typeMethodIds.add(new MethodId((Method) member));
			}
		}
		List<TypeRef> superTypesToRemove = new ArrayList<>();
		visitSupertypes(type, parent -> {
			for (Member parentMember : parent.members) {
				if (parentMember instanceof Method) {
					// If it has different signature, we need to copy the missing overload
					MethodId parentMethodId = new MethodId((Method) parentMember);
					if (!typeMethodIds.contains(parentMethodId)) {
						// now we must check if the parent method is allowed by the whitelist
						if (methodWhiteListPatterns.stream().anyMatch(p -> p.matcher(type.name() + "." + parentMethodId.name).matches())) {
							type.members.add(parentMember);
						}
					}
				}
			}
			// now we must remove the parent from the type's superTypes
			superTypesToRemove.add(parent.ref);
		});
		type.superTypes.removeAll(superTypesToRemove);
		type.interfaces.removeAll(superTypesToRemove);
	}

	public void forceParentJavadocs(TypeDefinition type) {
		// Figure out what methods we already have
		Set<MethodId> typeMethodIds = new HashSet<>();
		for (Member typeMember : type.members) {
			if (typeMember instanceof Method) {
				typeMethodIds.add(new MethodId((Method) typeMember));
			}
		}
		visitSupertypes(type, parent -> {
			for (Member parentMember : parent.members) {
				if (parentMember instanceof Method) {
					MethodId parentMethodId = new MethodId((Method) parentMember);
					if (typeMethodIds.contains(parentMethodId)) {
						// we now need to find the corresponding method by its method it in the type members and copy
						// the javadoc from the parent only if the type's one is empty or only contains @inheritDoc
						for (Member typeMember : type.members) {
							if (typeMember instanceof Method) {
								MethodId typeMethodId = new MethodId((Method) typeMember);
								if (typeMethodId.equals(parentMethodId)) {
									Method typeMethod = (Method) typeMember;
									if (typeMethod.javadoc.isEmpty() || typeMethod.javadoc.get().trim().equals("@inheritDoc")) {
										typeMethod.javadoc = parentMember.javadoc;
									}
								}
							}
						}
					}
				}
			}
		});
	}

}
