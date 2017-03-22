package il.ac.idc.lang.compiler;

public class JackConstructor extends AbstractJackSubroutine {

	private static int index = 0;
	private int id;
	public JackConstructor(int line, String name) {
		super(line, name);
		id = index;
		index++;
	}

	@Override
	public String getName() {
		return getClassName() + ".new-" + id;
	}

}
