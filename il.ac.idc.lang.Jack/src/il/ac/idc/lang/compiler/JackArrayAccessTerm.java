package il.ac.idc.lang.compiler;

public class JackArrayAccessTerm extends AbstractJackTerm {

	private static int id = 0;
	private JackExpression expression;
	String varname;
	
	public JackArrayAccessTerm(int lineNumber) {
		super(lineNumber);
		id++;
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
		builder.append("// " + getName() + "\n");
		// local
		boolean found = false;
		for (int i = 0; i < subroutine.locals.size(); i++) {
			if (subroutine.locals.get(i).name.equals(varname)) {
				builder.append("push local " + i + "\n");
				found = true;
				break;
			}
		}
		// argument
		if (!found) {
			for (int i = 0; i < subroutine.arguments.size(); i++) {
				if (subroutine.arguments.get(i).name.equals(varname)) {
					builder.append("push argument " + i + "\n");
					found = true;
					break;
				}
			}
		}
		// field
		JackClass klass = getKlass();
		if (!found) {
			for (int i = 0; i <klass.instanceVariables.size(); i++) {
				if (klass.instanceVariables.get(i).name.equals(varname)) {
					builder.append("push this " + i + "\n");
					found = true;
					break;
				}
			}
		}
		// static
		if (!found) {
			for (int i = 0; i < klass.classVariables.size(); i++) {
				if (klass.classVariables.get(i).name.equals(varname)) {
					builder.append("push static " + i + "\n"); // TODO handle static offset
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
	public String getName() {
		return getClassName() + "." + getSubroutineName() + ":term-array-access-" + varname + "-" + id;
	}

}
