package il.ac.idc.lang.compiler;

public class JackVariableTerm extends AbstractJackTerm {

	private static int index = 0;
	private int id;
	
	public JackVariableTerm(int lineNumber) {
		super(lineNumber);
		id = index;
		index++;
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
		String code = "";
		if (lineNumber != parent.lineNumber) {
			code = "// sourceLine:" + lineNumber + "\n";
		}
		// local
		for (int i = 0; i < sub.locals.size(); i++) {
			if (sub.locals.get(i).name.terminal.equals(varname)) {
				code += "push local " + i + "\n";
			}
		}
		// argument
		for (int i = 0; i < sub.arguments.size(); i++) {
			if (sub.arguments.get(i).name.terminal.equals(varname)) {
				code += "push argument " + i + "\n";
			}
		}
		JackClass klass = getKlass();
		// field
		for (int i = 0; i < klass.classVariables.size(); i++) {
			JackClassVariableDecl decl = klass.classVariables.get(i);
			if (decl.name.getTerminal().equals(varname)) {
				if (decl.modifier.getTerminal().equals("field")) {
					code += "push this " + i + "\n";
				} else {
					code += "push static " + i + "\n";
				}
			}
		}
		return code;
	}

	@Override
	public String getId() {
		return getKlassName() + "." + getSubroutineName() + ":term-var-" + varname +"-" + id;
	}
}
