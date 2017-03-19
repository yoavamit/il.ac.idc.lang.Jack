package il.ac.idc.lang.compiler;

public class JackLetStatement extends AbstractJackStatement {

	private static int id = 0;
	String assignee;
	private JackArrayAccessTerm arrayAccess;
	private JackExpression expression;
	
	public JackLetStatement(int lineNumber) {
		super(lineNumber);
		id++;
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
		builder.append("// " + getName() + "\n");
		builder.append(expression.writeVMCode());
		if (assignee != null) {
			AbstractJackSubroutine sub = getSubroutine();
			// local
			boolean found = false;
			for (int i = 0; i < sub.locals.size(); i++) {
				if (sub.locals.get(i).name.equals(assignee)) {
					builder.append("pop local " + i + "\n");
					found = true;
					break;
				}
			}
			// argument
			if (!found) {
				for (int i = 0; i < sub.arguments.size(); i++) {
					if (sub.arguments.get(i).name.equals(assignee)) {
						builder.append("pop argument " + i + "\n");
						found = true;
						break;
					}
				}
			}
			JackClass klass = getKlass();
			// field
			if (!found) {
				for (int i = 0; i < klass.instanceVariables.size(); i++) {
					if (klass.instanceVariables.get(i).name.equals(assignee)) {
						builder.append("pop this " + i + "\n");
						found = true;
						break;
					}
				}
			}
			// static
			if (!found) {
				for (int i = 0; i < klass.classVariables.size(); i++) {
					if (klass.classVariables.get(i).name.equals(assignee)) {
						builder.append("pop static " + i + "\n"); // TODO handle static variable offset 
					}
				}
			}
		} else {
			// TODO 
		}
		return builder.toString();
	}

	@Override
	public String getName() {
		return getClassName() + "." + getSubroutineName() + ":statement-let-" + id;
	}

}
