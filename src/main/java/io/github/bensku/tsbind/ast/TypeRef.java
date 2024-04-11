package io.github.bensku.tsbind.ast;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedArrayType;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;

public abstract class TypeRef implements AstNode {

	public static final Simple VOID = new Simple("void");
	public static final Simple BOOLEAN = new Simple("boolean");
	public static final Simple BYTE = new Simple("byte");
	public static final Simple SHORT = new Simple("short");
	public static final Simple CHAR = new Simple("char");
	public static final Simple INT = new Simple("int");
	public static final Simple LONG = new Simple("long");
	public static final Simple FLOAT = new Simple("float");
	public static final Simple DOUBLE = new Simple("double");

	public static final Simple OBJECT = new Simple("java.lang.Object");
	public static final Simple STRING = new Simple("java.lang.String");
	public static final Simple LIST = new Simple("java.util.List");

	public static TypeRef fromType(ResolvedType type, boolean nullable) {
		if (nullable) {
			return new Nullable(fromType(type));
		} else {
			return fromType(type);
		}
	}

	public static TypeRef fromType(ResolvedType type) {
		if (type.isVoid()) {
			return VOID;
		} else if (type.isPrimitive()) {
			switch (type.asPrimitive()) {
			case BOOLEAN:
				return BOOLEAN;
			case BYTE:
				return BYTE;
			case SHORT:
				return SHORT;
			case CHAR:
				return CHAR;
			case INT:
				return INT;
			case LONG:
				return LONG;
			case FLOAT:
				return FLOAT;
			case DOUBLE:
				return DOUBLE;
			default:
				throw new AssertionError();
			}
		} else if (type.isReferenceType()) {
			ResolvedReferenceType reference = type.asReferenceType();
			List<ResolvedType> typeParams = reference.typeParametersValues();
			if (typeParams.isEmpty()) { // No generic type parameters
				return getSimpleType(reference.getQualifiedName());
			} else {
				List<TypeRef> params = typeParams.stream().map(TypeRef::fromType).collect(Collectors.toList());
				return new Parametrized(getSimpleType(reference.getQualifiedName()), params);
			}
		} else if (type.isArray()) {
			ResolvedArrayType array = type.asArrayType();
			TypeRef component = fromType(array.getComponentType());
			return new Array(component, array.arrayLevel());
		} else if (type.isWildcard()) {
			if (type.asWildcard().isExtends()) {
				return new Wildcard(fromType(type.asWildcard().getBoundedType()));
			} else { // We can't describe ? super X in TS (AFAIK)
				return OBJECT;
			}
		} else if (type.isTypeVariable()) {
			return fromDeclaration(type.asTypeParameter());
		} else {
			throw new AssertionError("unexpected type: " + type);
		}
	}

	private static Simple getSimpleType(String name) {
		switch (name) {
		case "java.lang.Boolean":
			return BOOLEAN;
		case "java.lang.Byte":
			return BYTE;
		case "java.lang.Short":
			return SHORT;
		case "java.lang.Character":
			return CHAR;
		case "java.lang.Integer":
			return INT;
		case "java.lang.Long":
			return LONG;
		case "java.lang.Float":
			return FLOAT;
		case "java.lang.Double":
			return DOUBLE;
		case "java.lang.Object":
			return OBJECT;
		case "java.lang.String":
			return STRING;
		default:
			return new Simple(name);
		}
	}

	public static TypeRef fromDeclaration(ResolvedTypeParameterDeclaration decl) {
		if (decl.hasUpperBound()) {
			return new Parametrized(new Simple(decl.getName()),
					Collections.singletonList(fromType(decl.getUpperBound())));
		} else if (decl.hasUpperBound()) { // We can't describe X super Y in TS (AFAIK)
			return OBJECT;
		} else {
			return getSimpleType(decl.getName());
		}
	}

	public static TypeRef enumSuperClass(TypeRef enumType) {
		return new Parametrized(getSimpleType("java.lang.Enum"), List.of(enumType));
	}

	public static TypeRef fromDeclaration(String typeName, ResolvedReferenceTypeDeclaration decl) {
		var typeParams = decl.getTypeParameters();
		if (typeParams.isEmpty()) {
			return getSimpleType(decl.getQualifiedName());
		} else {
			List<TypeRef> params = typeParams.stream().map(TypeRef::fromDeclaration).collect(Collectors.toList());
			return new Parametrized(getSimpleType(decl.getQualifiedName()), params);
		}
	}

	public static class Simple extends TypeRef {

		/**
		 * Fully qualified name of the type, excluding array dimensions.
		 */
		private final String name;

		private Simple(String name) {
			this.name = name;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public TypeRef baseType() {
			return this; // Base of most types
		}

		@Override
		public int arrayDimensions() {
			return 0;
		}

		@Override
		public void walk(Consumer<AstNode> visitor) {
			visitor.accept(this);
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Simple)) {
				return false;
			}
			return ((Simple) obj).name.equals(this.name);
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}
	}

	public static class Wildcard extends TypeRef {

		/**
		 * Type that this generic parameter must extend.
		 */
		private final TypeRef extendedType;

		private Wildcard(TypeRef extendedType) {
			this.extendedType = extendedType;
		}

		@Override
		public String name() {
			return "*";
		}

		@Override
		public TypeRef baseType() {
			return this; // Extended type is definitely not base type
		}

		@Override
		public int arrayDimensions() {
			return 0;
		}

		public TypeRef extendedType() {
			return extendedType;
		}

		@Override
		public void walk(Consumer<AstNode> visitor) {
			visitor.accept(this);
			extendedType.walk(visitor);
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Wildcard)) {
				return false;
			}
			return ((Wildcard) obj).extendedType.equals(this.extendedType);
		}

		@Override
		public int hashCode() {
			return extendedType.hashCode();
		}

	}

	public static class Parametrized extends TypeRef {

		/**
		 * Base type that generic type parameters are applied to.
		 */
		private final TypeRef baseType;

		/**
		 * Type parameters.
		 */
		private final List<TypeRef> params;

		private Parametrized(TypeRef baseType, List<TypeRef> params) {
			this.baseType = baseType;
			this.params = params;
		}

		@Override
		public String name() {
			return baseType.name();
		}

		@Override
		public TypeRef baseType() {
			return baseType.baseType();
		}

		@Override
		public int arrayDimensions() {
			return 0;
		}

		public List<TypeRef> typeParams() {
			return params;
		}

		@Override
		public void walk(Consumer<AstNode> visitor) {
			visitor.accept(this);
			baseType.walk(visitor);
			params.forEach(param -> param.walk(visitor));
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Parametrized)) {
				return false;
			}
			Parametrized o = (Parametrized) obj;
			return o.baseType.equals(this.baseType) && o.params.equals(this.params);
		}

		@Override
		public int hashCode() {
			return baseType.hashCode() + 31 * params.hashCode();
		}
	}

	public static class Array extends TypeRef {

		/**
		 * Array component type.
		 */
		private final TypeRef component;

		/**
		 * Array dimensions.
		 */
		private final int dimensions;

		private Array(TypeRef component, int dimensions) {
			this.component = component;
			this.dimensions = dimensions;
		}

		@Override
		public String name() {
			return component.name() + "[]".repeat(dimensions);
		}

		@Override
		public TypeRef baseType() {
			return component.baseType();
		}

		@Override
		public int arrayDimensions() {
			return dimensions;
		}

		@Override
		public void walk(Consumer<AstNode> visitor) {
			visitor.accept(this);
			component.walk(visitor);
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Array)) {
				return false;
			}
			Array o = (Array) obj;
			return o.component.equals(this.component) && o.dimensions == this.dimensions;
		}

		@Override
		public int hashCode() {
			return component.hashCode() + 31 * dimensions;
		}
	}

	public static class Nullable extends TypeRef {

		/**
		 * Type that is nullable.
		 */
		private final TypeRef type;

		private Nullable(TypeRef type) {
			this.type = type;
		}

		/**
		 * Type that we wrap because it is nullable.
		 * @return Nullable type.
		 */
		public TypeRef nullableType() {
			return type;
		}

		@Override
		public String name() {
			return type.name();
		}

		@Override
		public TypeRef baseType() {
			return type.baseType();
		}

		@Override
		public int arrayDimensions() {
			return type.arrayDimensions();
		}

		@Override
		public void walk(Consumer<AstNode> visitor) {
			type.walk(visitor);
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Nullable)) {
				return false;
			}
			return ((Nullable) obj).type.equals(this.type);
		}

		@Override
		public int hashCode() {
			return type.hashCode();
		}

	}

	public abstract String name();

	public String simpleName() {
		String name = name();
		return name.substring(name.lastIndexOf('.') + 1);
	}

	public abstract TypeRef baseType();

	public abstract int arrayDimensions();

	public Array makeArray(int dimensions) {
		return new Array(this, dimensions);
	}

	@Override
	public abstract boolean equals(Object obj);

	@Override
	public abstract int hashCode();

	@Override
	public String toString() {
		return name();
	}

}
