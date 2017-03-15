package il.ac.idc.lang.compiler;

public class JackIntegerConstant extends JackTermArtifact {

	int constant;
	
	@Override
	public String writeVMCode() {
		return "push constant " + constant +"\n";
	}
}
