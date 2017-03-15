package il.ac.idc.lang.compiler;

public class JackArrayAccessTerm extends JackTermArtifact {

	String varname;
	JackExpressionArtifact expression;
	
	private JackSubroutineArtifact getMethod() {
		IJackLanguageArtifact method = getParent();
		while(!(method instanceof JackSubroutineArtifact)) {
			method = method.getParent();
		}
		return (JackSubroutineArtifact) method;
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
		JackSubroutineArtifact subroutine = getMethod();
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
		JackClassArtifact klass = getKlass();
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
		expression.parent = this;
		builder.append(expression.writeVMCode());
		builder.append("add\n");
		builder.append("pop pointer 1\n");
		builder.append("push that 0\n");
		return builder.toString();
	}

}
