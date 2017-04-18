package il.ac.idc.lang.compiler;

public class JackLetStatement extends AbstractJackStatement {

	private static int index = 0;
	private int id;
	String assignee;
	private JackArrayAccessTerm arrayAccess;
	private JackExpression expression;
	
	public JackLetStatement(int lineNumber) {
		super(lineNumber);
		id = index;
		index++;
	}

	
	private AbstractJackSubroutine getSubroutine() {
		AbstractJackObject sub = getParent();
		while(!(sub instanceof AbstractJackSubroutine)) {
			sub = sub.getParent();
		}
		return (AbstractJackSubroutine) sub;
	}
	
	void setExpression(JackExpression expression) {
		expression.parent = this;
		this.expression = expression;
	}
	
	void setArrayAccess(JackArrayAccessTerm arrayAccess) {
		arrayAccess.parent = this;
		this.arrayAccess = arrayAccess;
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
		if (lineNumber != parent.lineNumber) {
			builder.append("// sourceLine:" + lineNumber + "\n");
		}
		builder.append(expression.writeVMCode());
		if (assignee != null) {
			AbstractJackSubroutine sub = getSubroutine();
			// local
			boolean found = false;
			for (int i = 0; i < sub.locals.size(); i++) {
				if (sub.locals.get(i).name.terminal.equals(assignee)) {
					builder.append("pop local " + i + "\n");
					found = true;
					break;
				}
			}
			// argument
			if (!found) {
				for (int i = 0; i < sub.arguments.size(); i++) {
					if (sub.arguments.get(i).name.terminal.equals(assignee)) {
						builder.append("pop argument " + i + "\n");
						found = true;
						break;
					}
				}
			}
			JackClass klass = getKlass();
			// field
			if (!found) {
				for (int i = 0; i < klass.classVariables.size(); i++) {
					JackClassVariableDecl decl = klass.classVariables.get(i);
					if (decl.name.terminal.equals(assignee)) {
						if (decl.modifier.getTerminal().equals("field")) {
							builder.append("pop this " + i + "\n");
						} else {
							builder.append("pop static " + i + "\n");
						}
						found = true;
						break;
					}
				}
			}
		} else {
			builder.append(arrayAccess.writeVMCode());
			// the array access object pushes the value stored at the array's given index to the stack
			// this pop instruction get's rid of that value and then pops the given expression to the array's given index which is stored at memory segment "that 0"
			builder.append("pop temp 1 // get rid of extra value in the stack\n");
			builder.append("pop that 0\n");
		}
		return builder.toString();
	}

	@Override
	public String getId() {
		return getKlassName() + "." + getSubroutineName() + ":statement-let-" + id;
	}

}
