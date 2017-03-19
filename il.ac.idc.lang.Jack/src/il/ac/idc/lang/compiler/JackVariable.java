package il.ac.idc.lang.compiler;

public class JackVariable extends AbstractJackObject {

	String name, type;
	
	public JackVariable(int line, String name, String type) {
		super(line);
		this.name = name;
		this.type = type;
	}
	
	@Override
	public String writeVMCode() {
		return "// " + getName() + "\n";
	}
	
	@Override
	public String getName() {
		String subroutineName = getSubroutineName();
		if (subroutineName == null) {
			return getClassName() + ":class-var-" + name;
		} else {
			return getClassName() + "." + subroutineName + ":subroutine-var-" + name;
		}
	}
}
