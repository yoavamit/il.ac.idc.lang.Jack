package il.ac.idc.lang.compiler;

public class JackIntegerConstant extends AbstractJackTerm {

	private int constant;
	
	public JackIntegerConstant(int line, int val) {
		super(line);
		this.constant = val;
	}
	
	@Override
	public String writeVMCode() {
		return "// " + getName() + "\npush constant " + constant +"\n";
	}

	@Override
	public String getName() {
		return "integer-constant-" + constant;
	}
}
