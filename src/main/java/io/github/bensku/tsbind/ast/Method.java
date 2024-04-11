package io.github.bensku.tsbind.ast;

import java.util.List;
import java.util.function.Consumer;

public class Method extends Member {

	/**
	 * Name of the method.
	 */
	public final String name;

	/**
	 * Return type of the method.
	 */
	public final TypeRef returnType;

	/**
	 * Parameters of the method
	 */
	public final List<Parameter> params;

	/**
	 * Type (generic) parameters.
	 */
	public final List<TypeRef> typeParams;

	/**
	 * If this is annotated with {@link Override}.
	 */
	public final boolean isOverride;

	public final String typeName;

	public Method(String name, TypeRef returnType, List<Parameter> params, List<TypeRef> typeParams, String javadoc,
			boolean isPublic, boolean isStatic, boolean isOverride, String typeName) {
		super(javadoc, isPublic, isStatic);
		this.name = name;
		this.returnType = returnType;
		this.params = params;
		this.typeParams = typeParams;
		this.isOverride = isOverride;
		this.typeName = typeName;
	}

	/**
	 * Original name of the method. For setters and getters, this is different
	 * from the {@link #name()}.
	 * @return Original name.
	 */
	public String originalName() {
		return name;
	}

	@Override
	public void walk(Consumer<AstNode> visitor) {
		visitor.accept(this);
		returnType.walk(visitor);
		params.forEach(param -> param.walk(visitor));
		typeParams.forEach(param -> param.walk(visitor));
	}

	@Override
	public String name() {
		return name;
	}

	public String typeName() {
		return typeName;
	}

	@Override
	public String toString() {
		return name + "(" + params + "): " + returnType;
	}

}
