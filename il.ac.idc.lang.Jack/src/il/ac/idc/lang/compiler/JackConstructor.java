package il.ac.idc.lang.compiler;

public class JackConstructor extends AbstractJackSubroutine {

	private static int id = 0;
	
	public JackConstructor(int line, String name) {
		super(line, name);
		id++;
	}

	@Override
	public String getName() {
		return getClassName() + ".new-" + id;
	}

}
