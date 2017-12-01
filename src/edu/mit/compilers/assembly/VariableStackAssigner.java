package edu.mit.compilers.assembly;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.List;
import java.util.HashSet;

import edu.mit.compilers.cfg.CFG;
import edu.mit.compilers.cfg.CFGProgram;
import edu.mit.compilers.cfg.lines.*;
import edu.mit.compilers.cfg.optimizations.*;
import edu.mit.compilers.symbol_tables.*;
import edu.mit.compilers.ir.decl.*;

public class VariableStackAssigner {
	private Map<String, VariableDescriptor> variables = new HashMap<>();
	private Map<String, VariableDescriptor> globals = new HashMap<>();

	public VariableStackAssigner(CFGProgram program) {
		for (VariableDescriptor var : program.getGlobalVariables()) {
			globals.put(var.getName(), var);
		}
		Map<String, CFG> methodMap = program.getMethodToCFGMap();
		for (String method : methodMap.keySet()) {
			int stackPointer = 0;
			for(VariableDescriptor desc: program.getLocalVariablesForMethod(method)) {
				stackPointer = desc.pushOntoStack(stackPointer);
				variables.put(desc.getName(), desc);
			}
			List<IRMemberDecl> params = program.getAllParameters(method);
			for (IRMemberDecl param : params) {
				VariableDescriptor newDescriptor = new VariableDescriptor(param.getName());
				stackPointer = newDescriptor.pushOntoStack(stackPointer);
				variables.put(param.getName(), newDescriptor);
			}
		}
	}

	private VariableDescriptor getVar(String variableName) {
		VariableDescriptor var = variables.get(variableName);
		if (var == null) {
			var = globals.get(variableName);
		}
		if (var == null) {
			throw new RuntimeException("Attempted to access unallocated variable '" + variableName + "'.");
		}
		return var;
	}

	private String getAddress(String variableName) {
		VariableDescriptor var = getVar(variableName);
		int offset = var.getStackOffset();
		if (! variables.containsKey(variableName)) {
			// it's a global variable
			return "$" + variableName;
		}
		if (var.isArray()) {
            return "-" + (new Integer(offset).toString()) + "(%rbp, %r10, 8)";
        } else {
            return "-" + (new Integer(offset).toString()) + "(%rbp)";
        }
	}

	// move variable to targetRegister from stack
	// usually both registers should be %r10; don't use %r11; if not an array nor global, indexRegister doesn't matter
	public String moveTo(String variableName, String targetRegister, String indexRegister) {
		VariableDescriptor var = getVar(variableName);
		int offset = var.getStackOffset();
		if (! variables.containsKey(variableName)) {
			// it's a global variable
			if (var.isArray()) {
				String code = "push %r11\n";
				code += "mov $8, %r11\n";
                code += "imul %r11, " + indexRegister + "\n"; // TODO do via shifting instead
                code += "mov $" + variableName + ", %r11\n";
				code += "add %r11, " + indexRegister + "\n";
				code += "mov 0(" + indexRegister + "), %r11\n";
				code += "mov %r11, " + targetRegister + "\n";
				code += "pop %r11\n";
				return code;
			}
			else {
				String code = "";
				code += "push %r11\n";
				code += "mov $" + variableName + ", " + indexRegister + "\n";
				code += "mov 0(" + indexRegister + "), %r11\n";
				code += "mov %r11, " + targetRegister + "\n";
				code += "pop %r11\n";
				return code;
			}
		}
		if (var.isArray()) {
			return "mov -" + (new Integer(offset).toString()) + "(%rbp, " + indexRegister + ", 8), " + targetRegister + "\n";
        } else {
			return "mov -" + (new Integer(offset).toString()) + "(%rbp), " + targetRegister + "\n";
        }
	}

	// move variable from sourceRegister to the stack
	// usually should be %r11 and %r10; source and index should not be same register and index shouldn't be %r11; if not an array nor global, indexRegister doesn't matter
	public String moveFrom(String variableName, String sourceRegister, String indexRegister) {
		VariableDescriptor var = getVar(variableName);
		int offset = var.getStackOffset();
		if (! variables.containsKey(variableName)) {
			// it's a global variable
			if (var.isArray()) {
				String code = "push %r11\n";
				code += "mov $8, %r11\n";
				code += "imul %r11, " + indexRegister + "\n"; // TODO do via shifting instead
				code += "mov $" + variableName + ", %r11\n";
				code += "add %r11, " + indexRegister + "\n";
				code += "pop %r11\n";
				code += "mov " + sourceRegister + ", 0(" + indexRegister + ")\n";
				return code;
			}
			String code = "";
			code += "mov $" + variableName + ", " + indexRegister + "\n";
			code += "mov " + sourceRegister + ", 0(" + indexRegister + ")\n";
			return code;
		}
		if (var.isArray()) {
			return "mov " + sourceRegister + ", -" + (new Integer(offset).toString()) + "(%rbp, " + indexRegister + ", 8)\n";
		} else {
			return "mov " + sourceRegister + ", -" + (new Integer(offset).toString()) + "(%rbp)\n";
		}
	}

	public String getMaxSize(String variableName) {
		VariableDescriptor var = getVar(variableName);
		if (var.isArray()) {
			int max_index = var.getLength();
			return "$" + (new Integer(max_index).toString());
        } else {
            throw new RuntimeException("Attempted to call getMaxSize() on non-array variable '" + variableName + "'.");
        }
	}

	public int getNumAllocs() {
		int allocs = 0;
		for(VariableDescriptor desc: variables.values()) {
			allocs += desc.getSpaceRequired();
		}
		return allocs;
	}
}