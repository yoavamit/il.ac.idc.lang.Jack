package il.ac.idc.lang.compiler;

import java.util.ArrayList;
import java.util.List;

public class JackSubroutineCallTerm extends AbstractJackTerm {

	private static int id = 0;
	private List<JackExpression> parameters = new ArrayList<>();
	String accessor;
	String subroutineName;
	
	public JackSubroutineCallTerm(int lineNumber) {
		super(lineNumber);
		id++;
	}
	
	void setParameters(List<JackExpression> params) {
		for (JackExpression param : params) {
			param.parent = this;
			parameters.add(param);
		}
	}
	
	private JackClass getKlass() {
		AbstractJackObject klass = getParent();
		while(!(klass instanceof JackClass)) {
			klass = klass.getParent();
		}
		return (JackClass) klass;
	}
	
	private AbstractJackSubroutine getSubroutine() {
		AbstractJackObject method = getParent();
		while(!(method instanceof AbstractJackSubroutine)) {
			method = method.getParent();
		}
		return (AbstractJackSubroutine) method;
	}
	
	@Override
	public String writeVMCode() {
		StringBuilder builder = new StringBuilder();
		// there are 4 cases that needs handling here:
		// 1) functionName()
		// 2) ClassName.functionName()
		// 3) varName.methodName()
		// 4) methodName()
		String className = "";
		JackClass klass = getKlass();
		boolean isStatic = false;
		
		builder.append("// " + getName() + "\n");
		if (accessor == null) {
			className = klass.getName();
			for (AbstractJackSubroutine subroutine : klass.subroutines) {
				if (subroutine.name.equals(subroutineName) && subroutine instanceof JackFunction) {
					isStatic = true;
					break;
				}
			}
			if (!isStatic) {
				builder.append("push pointer 0\n");
			}
		} else {
			// check for varName.methodName()
			// need to find the variable and push it as the first parameter
			AbstractJackSubroutine subroutine = getSubroutine();
			//local
			boolean found = false;
			for (int i = 0; i < subroutine.locals.size(); i++) {
				if (subroutine.locals.get(i).name.equals(accessor)) {
					className = subroutine.locals.get(i).type;
					builder.append("push local " + i + "\n");
					found = true;
					break;
				}
			}
			//argument
			if (!found) {
				for (int i = 0; i < subroutine.arguments.size(); i++) {
					if (subroutine.arguments.get(i).name.equals(accessor)) {
						className = subroutine.arguments.get(i).type;
						builder.append("push argument " + i + "\n");
						found = true;
						break;
					}
				}
			}
			//static
			if (!found) {
				for (int i = 0; i < klass.classVariables.size(); i++) {
					if (klass.classVariables.get(i).name.equals(accessor)) {
						builder.append("push static " + i + "\n"); // TODO handle class static var offset
						className = klass.classVariables.get(i).type;
						found = true;
						break;
					}
				}
			}
			//fiend
			if (!found) {
				for (int i = 0; i < klass.instanceVariables.size(); i++) {
					if (klass.instanceVariables.get(i).name.equals(accessor)) {
						builder.append("push pointer 0\n");
						builder.append("push constant " + i + "\n");
						builder.append("add\n");
						className = klass.instanceVariables.get(i).type;
					}
				}
			}
			// then, this is ClassName.functionName()
			if (!found) {				
				className = accessor;
				isStatic = true;
			}
		}
		
		// push parameters before calling the subroutine
		for (JackExpression parameter : parameters) {
			builder.append(parameter.writeVMCode());
		}
		int numParams = parameters.size() + (isStatic ? 0 : 1); 
		builder.append("call " + className + "." + subroutineName + " " + numParams + "\n");
		return builder.toString();
	}

	@Override
	public String getName() {
		return getClassName() + "." + getSubroutineName() + ":call-" + subroutineName + "-" + id;
	}
}
