package edu.mit.compilers.cfg;
import edu.mit.compilers.cfg.CFGLine;
import edu.mit.compilers.ir.statement.IRStatement;
import java.util.Set;

public class CFGStatement extends CFGLine {
    IRStatement statement;

    public CFGStatement(CFGLine trueBranch, CFGLine falseBranch, IRStatement statement) {
        super(trueBranch, falseBranch);
        this.statement = statement;
    }

    public CFGStatement(IRStatement statement) {
        super();
        this.statement = statement;
    }

    public IRStatement getStatement() { return statement; }

    @Override
    public boolean isNoOp() { return false; }

    @Override
    public String ownValue() {
        return statement.toString();
    }

    @Override
    public <R> R accept(CFGVisitor<R> visitor){
		return visitor.on(this);
	}
}
