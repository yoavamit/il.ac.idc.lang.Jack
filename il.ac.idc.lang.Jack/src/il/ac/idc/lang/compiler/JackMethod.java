package il.ac.idc.lang.compiler;

public class JackMethod extends AbstractJackSubroutine {

	public JackMethod(int line, String name) {
		super(line, name);
	}

	@Override
	public String getId() {
		return getClassName() + "." + name;
	}

}
