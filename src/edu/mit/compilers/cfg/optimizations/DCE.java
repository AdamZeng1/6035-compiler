package edu.mit.compilers.cfg.optimizations;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.math.BigInteger;

import edu.mit.compilers.ir.*;
import edu.mit.compilers.ir.decl.*;
import edu.mit.compilers.ir.expression.*;
import edu.mit.compilers.ir.expression.literal.*;
import edu.mit.compilers.ir.statement.*;
import edu.mit.compilers.symbol_tables.*;
import edu.mit.compilers.trees.EnvStack;
import edu.mit.compilers.cfg.*;
import edu.mit.compilers.cfg.lines.*;

public class DCE implements Optimization {
    // TODO (mayars) make sure that initializing global variables is added to
    // the CFG for main() because then we don't need to initialize global
    // variables to 0/false if they're initialized later.

    // TODO (mayars) make sure use/assign sets interface correctly with array variables

    private CfgUseVisitor USE = new CfgUseVisitor();
    private CfgAssignVisitor ASSIGN = new CfgAssignVisitor();

    public void optimize(CFGProgram cfgProgram) {
        Set<String> globals = new HashSet<>();
        for (VariableDescriptor var : cfgProgram.getGlobalVariables()) {
            globals.add(var.getName());
        }
        for (Map.Entry<String, CFG> method : cfgProgram.getMethodToCFGMap().entrySet()) {
            CFG cfg = method.getValue();
            System.out.println("Original CFG:");
            System.out.println(cfg);
            boolean changed = true;
            while (changed) {
                doLivenessAnalysis(cfg, method.getKey().equals("main") ? new HashSet<String>() : globals);
                changed = removeDeadCode(cfg);
            }
            System.out.println("DCE-Optimized CFG:");
            System.out.println(cfg);
        }
    }

    private void doLivenessAnalysis(CFG cfg, Set<String> globals) {
        CFGLine end = cfg.getEnd();
        end.setLivenessOut(new HashSet<String>(globals));
        Set<String> endIn = new HashSet<String>(globals);
        endIn.addAll(end.accept(USE));
        end.setLivenessIn(endIn);

        // TODO if this becomes too slow, make changed into a field variable
        // and update it with only the places where things are changed
        Set<CFGLine> changed = new HashSet<CFGLine>(cfg.getAllLines());
        changed.remove(end);

        while (! changed.isEmpty()) {
            CFGLine line = changed.iterator().next();
            changed.remove(line);

            Set<String> newOut = new HashSet<>();
            for (CFGLine child : line.getChildren()) {
                newOut.addAll(child.getLivenessIn());
            }
            Set<String> newIn = new HashSet<>();
            newIn.addAll(line.accept(USE));
            Set<String> newOutDuplicate = new HashSet<>(newOut);
            newOutDuplicate.removeAll(line.accept(ASSIGN));
            newIn.addAll(newOutDuplicate);
            if (! newIn.equals(line.getLivenessIn())) {
                changed.addAll(line.getParents());
            }
            line.setLivenessIn(newIn);
            line.setLivenessOut(newOut);
        }
    }

    /**
     * returns whether or not dead code has been removed this iteration
     */
    private boolean removeDeadCode(CFG cfg) {
        DeadCodeEliminator eliminator = new DeadCodeEliminator(cfg);
        Set<CFGLine> toPossiblyRemove = cfg.getAllLines();
        boolean changed = false;

        for (CFGLine line : toPossiblyRemove) {
            changed = changed || line.accept(eliminator);
            // if (line.accept(eliminator)) {
            //     System.out.println("Removed: " + line.ownValue());
            //     changed = true;
            // }
        }
        return changed;
    }

    // returns true if gen/kill sets might change, i.e. usually when we have removed a line
    private class DeadCodeEliminator implements CFGLine.CFGVisitor<Boolean> {
        private CFG cfg;

        public DeadCodeEliminator(CFG cfg) { this.cfg = cfg; }

        @Override
        public Boolean on(CFGAssignStatement line) {
            // use liveness sets
            Set<String> aliveAtEnd = line.getLivenessOut();
            Set<String> assigned = line.accept(ASSIGN);
            for (String var : assigned) {
                if (aliveAtEnd.contains(var)) {
                    return false;
                }
            }
            cfg.replaceLine(line, line.getExpression().accept(new LineReplacer(line)));
            return true;
        }

        @Override
        public Boolean on(CFGConditional line) {
            // remove it if it is not a branch
            if (! line.isBranch()) {
                cfg.replaceLine(line, line.getExpression().accept(new LineReplacer(line)));
                return true;
            } else {
                return false;
            }
        }

        @Override
        public Boolean on(CFGNoOp line) {
            // could remove it if it can be condensed out
            if (! line.isEnd()) {
                cfg.removeLine(line);
            }
            return false; // even if we're removing a noop, we are not affecting gen/kill sets
        }

        @Override
        public Boolean on(CFGReturn line) {
            // TODO is there any case where it could be deleted?
            return false;
        }

        @Override
        public Boolean on(CFGMethodCall line) {
            // TODO remove it if the method doesn't call any globals
            if (line.getExpression().affectsGlobals()) {
                return false;
            } else {
                cfg.replaceLine(line, LineReplacer.getReplacementLine(line));
                return true;
            }
        }

        @Override
        public Boolean on(CFGBlock line) {
            throw new RuntimeException("Eliminating blocks is hard");
        }
    }


    // if you are deleting a line which evaluates this expression it returns the CFGLine
    // you want to replace that line with.
    // returns either CFGMethodCall (if expression is a method call that affects
    // global variables), or line.getNext() (if line is not end) or new noop (if line is end)
    private static class LineReplacer implements IRExpression.IRExpressionVisitor<CFGLine> {
        private CFGLine line;

        public LineReplacer(CFGLine line) { this.line = line; }

        public static CFGLine getReplacementLine(CFGLine line) {
            if (line.isBranch()) {
                throw new RuntimeException("Trying to delete a branch");
            }
            if (line.isEnd()) {
                return new CFGNoOp();
            } else {
                return line.getTrueBranch();
            }
        }

        @Override
        public CFGLine on(IRMethodCallExpression ir) {
            if (ir.affectsGlobals()) {
                CFGLine newLine = new CFGMethodCall(ir);
                newLine.stealChildren(line);
                return newLine;
            } else {
                return getReplacementLine(line);
            }
        }

        @Override
        public CFGLine on(IRUnaryOpExpression ir) { return getReplacementLine(line); }
        @Override
        public CFGLine on(IRBinaryOpExpression ir) { return getReplacementLine(line); }
        @Override
        public CFGLine on(IRTernaryOpExpression ir) { return getReplacementLine(line); }
        @Override
        public CFGLine on(IRLenExpression ir) { return getReplacementLine(line); }
        @Override
        public CFGLine on(IRVariableExpression ir) { return getReplacementLine(line); }
        @Override
        public CFGLine on(IRBoolLiteral ir) { return getReplacementLine(line); }
        @Override
        public CFGLine on(IRIntLiteral ir) { return getReplacementLine(line); }
        @Override
        public CFGLine on(IRStringLiteral ir) { return getReplacementLine(line); }
    }

}