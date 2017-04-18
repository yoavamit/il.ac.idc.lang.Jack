package il.ac.idc.lang.compiler;

public class JackFunction extends AbstractJackSubroutine {

	public JackFunction(int line, String name) {
		super(line, name);
	}

	@Override
	public String getId() {
		return getKlassName() + "." + name;
	}

}
