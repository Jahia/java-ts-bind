package io.github.bensku.tsbind.binding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.bensku.tsbind.ast.Getter;
import io.github.bensku.tsbind.ast.Member;
import io.github.bensku.tsbind.ast.Method;
import io.github.bensku.tsbind.ast.Setter;
import io.github.bensku.tsbind.ast.TypeDefinition;
import io.github.bensku.tsbind.ast.TypeRef;

public class TsClass implements TsGenerator<TypeDefinition> {

	public static final TsClass INSTANCE = new TsClass();
	
	private TsClass() {}
	
	private static class Members {
		
		private final TypeDefinition type;
		
		/**
		 * All public members.
		 */
		private final List<Member> members;
		
		/**
		 * Not for printing, we just need to access some data.
		 */
		private final TsEmitter emitter;
		
		public Members(TypeDefinition type, TsEmitter emitter) {
			this.type = type;
			this.members = type.members.stream()
					.filter(member -> member.isPublic)
					.collect(Collectors.toList());
			this.emitter = emitter;
		}
		
		/**
		 * Resolves the type which might contain the overridden method.
		 * @param type Root type.
		 * @param method Overriding method.
		 * @return Type definition, if found.
		 */
		private Optional<TypeDefinition> resolveOverrideSource(TypeRef type, Method method) {
			Optional<TypeDefinition> opt = emitter.resolveType(type);
			if (opt.isEmpty()) {
				return Optional.empty(); // Nothing here...
			}
			TypeDefinition def = opt.orElseThrow();
			
			// Check if type we're checking now has it
			if (def.hasMember(method.name())) {
				return Optional.of(def);
			}
			
			// No? Recursively check supertypes and interfaces, maybe they have it
			for (TypeRef parent : def.superTypes) {
				Optional<TypeDefinition> result = resolveOverrideSource(parent, method);
				if (result.isPresent()) {
					return result;
				}
			}
			for (TypeRef parent : def.interfaces) {
				Optional<TypeDefinition> result = resolveOverrideSource(parent, method);
				if (result.isPresent()) {
					return result;
				}
			}
			return Optional.empty(); // Didn't find it
		}
				
		/**
		 * Finds an interface method that the given method overrides.
		 * @param method Method to find overrides for.
		 * @return Overridden member, if found.
		 */
		private Optional<Member> resolveInterfaceOverride(Method method) {
			if (!method.isOverride) {
				return Optional.empty();
			}
			// Don't iterate over supertypes, only interfaces requested
			for (TypeRef parent : type.interfaces) {
				Optional<TypeDefinition> result = resolveOverrideSource(parent, method);
				if (result.isPresent()) {
					for (Member m : result.get().members) {
						if (m.getClass().equals(method.getClass()) && m.name().equals(method.name())) {
							return Optional.of(m); // Same name, same type -> found it!
						}
					}
				}
			}
			return Optional.empty();
		}
		
		/**
		 * Checks if a member has valuable Javadoc.
		 * @param member Member.
		 * @return If the member is likely to have useful information in
		 * its Javadoc.
		 */
		private boolean hasValuableJavadoc(Member member) {
			Optional<String> javadoc = member.javadoc;
			if (javadoc.isEmpty()) {
				return false;
			} else {
				// Short Javadoc with @inheritDoc is unlikely to contain anything of value
				String doc = javadoc.get();
				return doc.length() > 50 || !doc.contains("{@inheritDoc}");
			}
		}
		
		/**
		 * Manually copy inherited Javadoc from superclasses that our class
		 * (not interface) can't extend.
		 */
		public void fixInheritDoc() {
			// TODO Javadoc with overrides of overrides
			for (Member member : members) {
				if (!member.isStatic && member instanceof Method && !hasValuableJavadoc(member)) {
					resolveInterfaceOverride((Method) member).ifPresent(override -> {
						override.javadoc.ifPresent(doc -> member.javadoc = Optional.of(doc));
					});
				}
			}
		}
		
		/**
		 * Many Java types are emitted as 'number', which can cause strange
		 * duplicates to appear in TS types. This pass removes them.
		 */
		public void removeDuplicates() {
			Set<MethodId> methods = new HashSet<>();
			Iterator<Member> it = members.iterator();
			while (it.hasNext()) {
				Member member = it.next();
				if (member instanceof Method) {
					MethodId id = new MethodId((Method) member);
					if (methods.contains(id)) {
						it.remove(); // Duplicate, remove it
					} else {
						methods.add(id); // First occurrance
					}
				}
			}
		}
		
		/**
		 * Transforms a TS getter/setter at given index to a normal method.
		 * If the member there is not an accessor, nothing is done.
		 * @param index Index.
		 */
		private void invalidateGetSet(int index) {
			Member member = members.get(index);
			if (member instanceof Getter || member instanceof Setter) {
				Method original = (Method) member;
				Method method = new Method(original.originalName(), original.returnType, original.params,
						original.typeParams, original.javadoc.orElse(null), original.isPublic, original.isStatic, original.isOverride, original.typeName);
				members.set(index, method);
			} // other kinds of conflicts we don't touch
		}
		
		/**
		 * Resolves name conflicts caused by getters/setters.
		 */
		public void resolveConflicts() {
			// Figure out members with same names
			Map<String, List<Integer>> indices = new HashMap<>();
			for (int i = 0; i < members.size(); i++) {
				Member member = members.get(i);
				indices.computeIfAbsent(member.name(), n -> new ArrayList<>()).add(i);
			}
			
			// Resolve conflicts with getters/setters
			for (Map.Entry<String, List<Integer>> entry : indices.entrySet()) {
				List<Integer> conflicts = entry.getValue();
				if (conflicts.size() == 1) {
					continue; // No conflict exists
				} else if (conflicts.size() == 2) {
					Member first = members.get(conflicts.get(0));
					Member second = members.get(conflicts.get(1));
					if ((first instanceof Getter && second instanceof Setter)
							|| first instanceof Setter && second instanceof Getter) {
						continue; // Getter/setter pair, no conflict
					}
				}
				// Do not touch other kinds of conflicts - overloaded normal methods are ok
				
				// Transform getters and setters back to normal methods
				for (int index : conflicts) {
					invalidateGetSet(index);
				}
			}
		}
				
		public Stream<Member> stream() {
			return members.stream()
					.filter(member -> !(member instanceof TypeDefinition));
		}
	}
	
	@Override
	public void emit(TypeDefinition node, TsEmitter out) {
		node.javadoc.ifPresent(out::javadoc);
		// Class declaration, including superclass and interfaces
		
		// Transform functional interfaces into function signatures
		// For now, only do this if there are no (static) methods or fields
		if (node.kind == TypeDefinition.Kind.FUNCTIONAL_INTERFACE) {
			Member member = node.members.get(0);
			if (!member.isStatic && member instanceof Method) {
				out.print("export type ");
				emitName(node.ref.simpleName(), node.ref, out);
				out.print(" = ");
				emitFunction((Method) member, out);
				out.println(";");
				return;
			}
		}

		boolean mixinTrick = false;
		boolean inInterface = false;
		if (out.isUseGettersAndSetters()) {
			out.print("export class ");
			emitName(node.ref.simpleName(), node.ref, out);

			// We can't use TS 'implements', because TS interfaces
			// don't support e.g getters/setters
			// Instead, we translate Java implements to TS extends
			List<TypeRef> superTypes = new ArrayList<>(node.superTypes);
			superTypes.addAll(node.interfaces);
			if (!superTypes.isEmpty()) {
				// At least one supertype; there may be more, but we'll use mixin trick for that
				// (we still want to extend even just one type to get @inheritDoc)
				out.print(" extends %s", superTypes.get(0));
			}
			if (superTypes.size() > 1) {
				mixinTrick = true; // Trick multiple inheritance
			}
		} else {
			String kind = node.kind.toString().toLowerCase();
			if (node.kind == TypeDefinition.Kind.ENUM) {
				// we map enums to classes in TS
				kind = "class";
			}
			out.print("export " + kind + " ");
			emitName(node.ref.simpleName(), node.ref, out);
			if (node.kind == TypeDefinition.Kind.CLASS || node.kind == TypeDefinition.Kind.ENUM) {
				List<TypeRef> superTypes = new ArrayList<>(node.superTypes);
				if (!superTypes.isEmpty()) {
					out.print(" extends ");
					out.print(superTypes,", ");
				}
				List<TypeRef> interfaces = new ArrayList<>(node.interfaces);
				if (!interfaces.isEmpty()) {
					out.print(" implements ");
					out.print(interfaces, ", ");
				}
			} else if (node.kind == TypeDefinition.Kind.INTERFACE) {
				inInterface = true;
				List<TypeRef> superTypes = new ArrayList<>(node.superTypes);
				if (!superTypes.isEmpty()) {
					out.print(" extends ");
					out.print(superTypes,", ");
				}
			} else {
				throw new AssertionError("Unknown type kind: " + node.kind);
			}
		}
		out.println(" {");
		
		// If this is Iterable, make types extending this iterable
		if (node.name().equals("java.lang.Iterable")) {
			out.println("  [Symbol.iterator](): globalThis.Iterator<T>;");
		}
		
		// Prepare to emit members
		Members members = new Members(node, out);
		members.fixInheritDoc();
		members.removeDuplicates();
		members.resolveConflicts();
		
		// Emit class members with some indentation
		try (var none = out.startBlock()) {
			// TODO use stream for printing to avoid unnecessary list creation in hot path
			Stream<Member> stream = members.stream();
			out.print(stream.collect(Collectors.toList()), "\n");
		}
		
		out.println("\n}");
		
		// Emit supertypes/interfaces to interface that is merged with class
		if (mixinTrick) {
			out.print("export interface ");
			emitName(node.ref.simpleName(), node.ref, out);
			out.print(" extends ");
			// FIXME quick hack to get List -> array conversion out of supertypes
			List<TypeRef> superTypes = new ArrayList<>(node.superTypes);
			superTypes.addAll(node.interfaces);
			out.print(superTypes.stream()
					.filter(type -> !type.baseType().equals(TypeRef.LIST)).collect(Collectors.toList()), ", ");
			out.println(" {}");
		}
	}
	
	private void emitName(String name, TypeRef type, TsEmitter out) {
		// Needs specialized handling, because we DON'T (always) want package name here
		out.print(name);
		if (type instanceof TypeRef.Parametrized) {
			out.print("<").print(((TypeRef.Parametrized) type).typeParams(), ", ").print(">");
		}
	}
	
	private void emitFunction(Method method, TsEmitter out) {
		out.print("(");
		out.print(method.params, ", ");
		out.print(") => ");
		out.print(method.returnType);
	}

}
