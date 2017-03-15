package il.ac.idc.lang.compiler;

public class JackIntegerConstant extends JackTermArtifact {

	int constant;
	
	public JackIntegerConstant(int val) {
		this.constant = val;
	}
	
	@Override
	public String writeVMCode() {
		return "push constant " + constant +"\n";
	}
}
