package il.ac.idc.lang.compiler;

public abstract class AbstractJackTerminal {

	protected String terminal;
	
	public AbstractJackTerminal(String name) {
		terminal = name;
	}
	
	public String getTerminal() {
		return terminal;
	}
}
