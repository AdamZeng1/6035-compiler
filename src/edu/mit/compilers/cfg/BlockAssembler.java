package edu.mit.compilers.cfg;

import java.io.OutputStream;
import java.io.*;
import java.util.Map;
import java.util.HashMap;

import antlr.Token;
import edu.mit.compilers.ir.*;
import edu.mit.compilers.ir.decl.*;
import edu.mit.compilers.ir.expression.*;
import edu.mit.compilers.ir.expression.literal.*;
import edu.mit.compilers.ir.operator.*;
import edu.mit.compilers.ir.statement.*;
import edu.mit.compilers.symbol_tables.*;
import edu.mit.compilers.trees.EnvStack;
import edu.mit.compilers.cfg.*;


public class BlockAssembler {

    String methodLabel;
    int blockCount;
    int numAllocs;
    Map<CFGBlock, String> blockLabels;

    public BlockAssembler(String label, int numParams) {
        this.methodLabel = label;
        this.blockCount = 0;
        this.numAllocs = numParams;  // will increment this as we add locals
        this.blockLabels = new HashMap<>();
    }

    public String makeCode(CFGBlock block) {
        String prefix = methodLabel + ":\n";
        String code = "";
        // TODO mov input params to stack

        code += makeCodeHelper(block);

        String allocSpace = new Integer(8*numAllocs).toString();
        code += "leave\n" + "ret\n";
        prefix += "enter $" + allocSpace + ", $0\n";
        return prefix + code;
    }

    private String makeCodeHelper(CFGBlock block) {
        if (blockLabels.containsKey(block)) {
            throw new RuntimeException("Making code for a block that already has code generated.");
        }
        blockCount += 1;
        String label = methodLabel+"_"+new Integer(blockCount).toString();
        blockLabels.put(block, label);
        String code = label + ":\n";
        for (CFGLine line : block.getLines()) {
            code += makeCodeLine(line);
        }

        // add code for true child
        String childCode = "";
        CFGBlock child = (CFGBlock)block.getTrueBranch();
        if (child != null) {
            if (blockLabels.containsKey(child)) {
                // jump to it
                code += "mov $1, %r11";
                code += "cmp %r10, %r11";
                code += "je " + blockLabels.get(child);
            }
            else {
                // we don't need to jump to it because it's just the next block
                childCode += makeCodeHelper(child);
            }
        }
        if (block.isBranch()) {
            //  add code for false child, and jump statement
            child = (CFGBlock)block.getFalseBranch();
            if (child != null) {
                if (!blockLabels.containsKey(child)) {
                    childCode += makeCodeHelper(child);
                }
                // jump to it
                code += "mov $0, %r11";
                code += "cmp %r10, %r11";
                code += "je " + blockLabels.get(child);
            }
            else {
                throw new RuntimeException("null false child of block when true child is not null.");
            }
        }
        return code + childCode;
    }

    private String makeCodeLine(CFGLine line) {
        //TODO
        return line.ownValue();
        //throw new RuntimeException("Unimplemented");
    }
}