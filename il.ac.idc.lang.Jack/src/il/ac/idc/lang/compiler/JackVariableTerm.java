package il.ac.idc.lang.compiler;

public class JackVariableTerm extends JackTermArtifact {

	String varname;
	
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
		JackSubroutineArtifact sub = getSubroutine();
		// local
		for (int i = 0; i < sub.locals.size(); i++) {
			if (sub.locals.get(i).name.equals(varname)) {
				return "push local " + i + "\n";
			}
		}
		// argument
		for (int i = 0; i < sub.arguments.size(); i++) {
			if (sub.arguments.get(i).name.equals(varname)) {
				return "push argument " + i + "\n";
			}
		}
		JackClassArtifact klass = getKlass();
		// field
		for (int i = 0; i < klass.instanceVariables.size(); i++) {
			if (klass.instanceVariables.get(i).name.equals(varname)) {
				return "push this " + i + "\n";
			}
		}
		// static
		for (int i = 0; i < klass.classVariables.size(); i++) {
			if (klass.classVariables.get(i).name.equals(varname)) {
				return "push static " + i + "\n";
			}
		}
		return null;
	}

}
