package edu.mit.compilers.cfg;

import antlr.Token;
import edu.mit.compilers.ir.expression.IRExpression;
import edu.mit.compilers.ir.expression.IRVariableExpression;
import edu.mit.compilers.ir.statement.IRAssignStatement;
import java.util.Set;

public class CFGAssignStatement2 extends CFGLine {
	private IRVariableExpression varAssigned; // operation must be = here
    private IRExpression expression;

	public CFGAssignStatement2(IRAssignStatement s) {
		if (!s.getOperator().equals("=")) {
			throw new RuntimeException("CFGAssignStatements must not have operators other than '=': " s.getOperator());
		}
		this.varAssigned = s.getVarAssigned();
		if (this.varAssigned.getDepth() > 1) {
            throw new RuntimeException("CFGAssignStatements must not have >1 varAssigned depth: " + indexLocation.toString());
        }
		this.expression = s.getValue();
		if (this.expression.getDepth() > 1) {
            throw new RuntimeException("CFGAssignStatements must not have >1 expression depth: " + expression.toString());
        }
	}

	public CFGAssignStatement2(String variableName, IRExpression expression) {
        this.varAssigned = new IRVariableExpression(variableName);
        if (expression.getDepth() > 1) {
            throw new RuntimeException("CFGAssignStatements must not have >1 expression depth: " + expression.toString());
        }
		this.expression = expression;
	}

	public CFGAssignStatement2(String variableName, IRExpression indexLocation, IRExpression expression) {
        if (indexLocation.getDepth() > 0) {
            throw new RuntimeException("CFGAssignStatements must not have >0 indexLocation depth: " + indexLocation.toString());
        }
        this.varAssigned = new IRVariableExpression(variableName, indexLocation);
        if (expression.getDepth() > 1) {
            throw new RuntimeException("CFGAssignStatements must not have >1 expression depth: " + expression.toString());
        }
        this.expression = expression;
	}

    public IRVariableExpression getVarAssigned() { return varAssigned; }
    public IRExpression getExpression() { return expression; }

    @Override
    public boolean isNoOp() { return false; }

    @Override
    public String ownValue() {
        return varAssigned.toString() + " = " + expression.toString();
    }

	@Override
    public <R> R accept(CFGVisitor<R> visitor){
		return visitor.on(this);
	}
}
