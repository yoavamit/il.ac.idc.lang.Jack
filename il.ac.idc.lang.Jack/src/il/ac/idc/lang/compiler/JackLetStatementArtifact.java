package il.ac.idc.lang.compiler;

public class JackLetStatementArtifact extends JackStatementArtifact {

	String assignee;
	JackArrayAccessTerm arrayAccess;
	JackExpressionArtifact expression;
	
	private JackSubroutineArtifact getSubroutine() {
		IJackLanguageArtifact sub = getParent();
		while(!(sub instanceof JackSubroutineArtifact)) {
			sub = sub.getParent();
		}
		return (JackSubroutineArtifact) sub;
	}
	
	private JackClassArtifact getKlass() {
		IJackLanguageArtifact klass = getParent();
		while(!(klass instanceof JackClassArtifact)) {
			klass = klass.getParent();
		}
		return (JackClassArtifact) klass;
	}
	
	@Override
	public String writeVMCode() {
		StringBuilder builder = new StringBuilder();
		expression.parent = this;
		builder.append(expression.writeVMCode());
		if (assignee != null) {
			JackSubroutineArtifact sub = getSubroutine();
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
			JackClassArtifact klass = getKlass();
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

}
