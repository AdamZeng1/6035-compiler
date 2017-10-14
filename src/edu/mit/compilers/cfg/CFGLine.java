package edu.mit.compilers.cfg;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.math.BigInteger;

import antlr.Token;
import edu.mit.compilers.ir.*;
import edu.mit.compilers.ir.decl.*;
import edu.mit.compilers.ir.expression.*;
import edu.mit.compilers.ir.expression.literal.*;
import edu.mit.compilers.ir.operator.*;
import edu.mit.compilers.ir.statement.*;
import edu.mit.compilers.symbol_tables.*;
import edu.mit.compilers.trees.EnvStack;


public abstract class CFGLine {
    private CFGLine trueBranch;
    private CFGLine falseBranch;
    private int numParentLines;

    protected CFGLine(CFGLine trueBranch, CFGLine falseBranch) {
        this.trueBranch = trueBranch;
        trueBranch.addParentLine();
        this.falseBranch = falseBranch;
        falseBranch.addParentLine();
        this.numParentLines = 0;
    }

    protected CFGLine(CFGLine next) {
        this.trueBranch = next;
        this.falseBranch = next;
        this.numParentLines = 0;
    }

    protected CFGLine() {
        this.trueBranch = null;
        this.falseBranch = null;
        this.numParentLines = 0;
    }

    public CFGLine getTrueBranch() {
        return trueBranch;
    }

    public CFGLine getFalseBranch() {
        return falseBranch;
    }

    public boolean isBranch() {
        return (trueBranch != falseBranch);
    }

    public void setNext(CFGLine next) {
        this.trueBranch = next;
        this.falseBranch = next;
        next.addParentLine();
    }

    public void addParentLine() {
        this.numParentLines += 1;
    }

    public boolean isEnd() {
        if (trueBranch == null) {
            if (falseBranch != null) {
                throw new RuntimeException("CFGLine has one null branch and one non-null branch.");
            }
            else {
                return true;
            }
        }
        else if (falseBranch == null) {
            throw new RuntimeException("CFGLine has one null branch and one non-null branch.");
        }
        return false;
    }

    public boolean isNoOp() {
        throw new RuntimeException("Must be overridden by child class of CFGLine.");
    }

    @Override
    public String toString() {
        return stringHelper(0, 20);
    }

    public String stringHelper(int numIndents, int depthLimit) {
        if (depthLimit <= 0) {
            return "";
        }
        String prefix = "";
        for (int i=0; i<numIndents; i++){
            prefix += "-";
        }
        String str = prefix + ownValue() + "\n";
        if (isBranch()) {
            str += trueBranch.stringHelper(numIndents+1, depthLimit-1);
            str += falseBranch.stringHelper(numIndents+1, depthLimit-1);
        }
        else if (trueBranch != null) {
            str += trueBranch.stringHelper(numIndents, depthLimit-1);
        }
        return str;
    }

    protected String ownValue() {
        return "<CFGLine Object>";
    }

}
