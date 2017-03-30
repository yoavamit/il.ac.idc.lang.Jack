package il.ac.idc.lang.compiler;

public class JackArrayAccessTerm extends AbstractJackTerm {

	private static int index = 0;
	private int id;
	private JackExpression expression;
	String varname;
	
	public JackArrayAccessTerm(int lineNumber) {
		super(lineNumber);
		id = index;
		index++;
	}

	void setExpression(JackExpression exp) {
		exp.parent = this;
		this.expression = exp;
	}
	
	private AbstractJackSubroutine getMethod() {
		AbstractJackObject method = getParent();
		while(!(method instanceof AbstractJackSubroutine)) {
			method = method.getParent();
		}
		return (AbstractJackSubroutine) method;
	}
	
	private JackClass getKlass() {
		AbstractJackObject klass = getParent();
		while(!(klass instanceof JackClass)) {
			klass = klass.getParent();
		}
		return (JackClass) klass;
	}
	
	@Override
	public String writeVMCode() {
		StringBuilder builder = new StringBuilder();
		AbstractJackSubroutine subroutine = getMethod();
		if (lineNumber != parent.lineNumber) {
			builder.append("// sourceLine:" + lineNumber + "\n");
		}
		// local
		boolean found = false;
		for (int i = 0; i < subroutine.locals.size(); i++) {
			if (subroutine.locals.get(i).name.getTerminal().equals(varname)) {
				builder.append("push local " + i + "\n");
				found = true;
				break;
			}
		}
		// argument
		if (!found) {
			for (int i = 0; i < subroutine.arguments.size(); i++) {
				if (subroutine.arguments.get(i).name.getTerminal().equals(varname)) {
					builder.append("push argument " + i + "\n");
					found = true;
					break;
				}
			}
		}
		// class var
		JackClass klass = getKlass();
		if (!found) {
			for (int i = 0; i <klass.classVariables.size(); i++) {
				JackClassVariableDecl decl = klass.classVariables.get(i);
				if (decl.name.getTerminal().equals(varname)) {
					if (decl.modifier.getTerminal().equals("field")) {
						builder.append("push this " + i + "\n");
					} else {
						builder.append("push static " + i + "\n");
					}
					found = true;
					break;
				}
			}
		}
		builder.append(expression.writeVMCode());
		builder.append("add\n");
		builder.append("pop pointer 1\n");
		builder.append("push that 0\n");
		return builder.toString();
	}

	@Override
	public String getId() {
		return getClassName() + "." + getSubroutineName() + ":term-array-access-" + varname + "-" + id;
	}

}
