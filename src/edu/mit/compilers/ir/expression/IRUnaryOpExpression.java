package edu.mit.compilers.ir.expression;

import java.util.Arrays;
import java.util.List;

import antlr.Token;

import edu.mit.compilers.ir.IRNode;
import edu.mit.compilers.ir.IRType;
import edu.mit.compilers.ir.IRNode.IRNodeVisitor;
import edu.mit.compilers.ir.expression.IRExpression.IRExpressionVisitor;
import edu.mit.compilers.ir.operator.IRUnaryOperator;
import edu.mit.compilers.trees.ConcreteTree;

public class IRUnaryOpExpression extends IRExpression {

	//private IRUnaryOperator operator;
	private Token operator;
	private IRExpression argument;

	public IRUnaryOpExpression(Token operator, IRExpression argument) {
		setLineNumbers(operator);
		expressionType = IRExpression.ExpressionType.UNARY;
		this.operator = operator;
		this.argument = argument;
	}

	public Token getOperator() { return operator; }
	public IRExpression getArgument() { return argument; }

	@Override
	public IRType.Type getType() {
		// TODO dear god refactor this
		String op = operator.getText();
		if (op.equals("!")) {
			return IRType.Type.BOOL;
		}
		else if (op.equals("-")) {
			return IRType.Type.INT;
		}
		else {
			throw new RuntimeException("Undefined operator " + op + ".");
		}
	}

	@Override
	public List<IRExpression> getChildren() {
		return Arrays.asList(argument);
	}

	@Override
	public String toString() { //TODO remove if null
		return operator.getText() + ((argument == null) ? "var" : argument);
	}
	
	@Override
	public int getDepth() {
		return argument.getDepth() + 1;
	}
	
	@Override
	public <R> R accept(IRExpressionVisitor<R> visitor) {
		return visitor.on(this);
	}

	@Override
	public <R> R accept(IRNodeVisitor<R> visitor) {
		return visitor.on(this);
	}

}
