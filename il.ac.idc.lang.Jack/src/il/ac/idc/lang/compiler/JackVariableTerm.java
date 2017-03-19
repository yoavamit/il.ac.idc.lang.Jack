package il.ac.idc.lang.compiler;

public class JackVariableTerm extends AbstractJackTerm {

	private static int id = 0;
	
	public JackVariableTerm(int lineNumber) {
		super(lineNumber);
		id++;
	}

	String varname;
	
	private AbstractJackSubroutine getSubroutine() {
		AbstractJackObject sub = getParent();
		while(!(sub instanceof AbstractJackSubroutine)) {
			sub = sub.getParent();
		}
		return (AbstractJackSubroutine) sub;
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
		AbstractJackSubroutine sub = getSubroutine();
		String code = "// " + getName() + "\n";
		// local
		for (int i = 0; i < sub.locals.size(); i++) {
			if (sub.locals.get(i).name.equals(varname)) {
				code += "push local " + i + "\n";
			}
		}
		// argument
		for (int i = 0; i < sub.arguments.size(); i++) {
			if (sub.arguments.get(i).name.equals(varname)) {
				code += "push argument " + i + "\n";
			}
		}
		JackClass klass = getKlass();
		// field
		for (int i = 0; i < klass.instanceVariables.size(); i++) {
			if (klass.instanceVariables.get(i).name.equals(varname)) {
				code += "push this " + i + "\n";
			}
		}
		// static
		for (int i = 0; i < klass.classVariables.size(); i++) {
			if (klass.classVariables.get(i).name.equals(varname)) {
				code += "push static " + i + "\n";
			}
		}
		return code;
	}

	@Override
	public String getName() {
		return getClassName() + "." + getSubroutineName() + ":term-var-" + varname +"-" + id;
	}
}
