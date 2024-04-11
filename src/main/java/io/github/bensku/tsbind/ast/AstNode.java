package io.github.bensku.tsbind.ast;

import java.util.function.Consumer;

public interface AstNode {

	/**
	 * Walks all AST nodes under this node, including this.
	 * @param visitor Visitor to be called for node found..
	 */
	void walk(Consumer<AstNode> visitor);

	/**
	 * Require the toString method for all node implementation to make it easier to generate debugging output
	 * @return a String representation of the node's fields.
	 */
	String toString();
}
