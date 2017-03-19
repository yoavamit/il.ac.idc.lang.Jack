package il.ac.idc.lang.compiler;

public class JackStringConstant extends AbstractJackTerm {

	private String constant;

	public JackStringConstant(int line, String val) {
		super(line);
		constant = val;
	}
	
	@Override
	public String writeVMCode() {
		StringBuilder builder = new StringBuilder();
		builder.append("// " + getName() + "\n");
		builder.append("call Memory.alloc " + constant.length() + "\n");
		for (int i = 0; i < constant.length(); i++) {
			builder.append("push constant " + (short) constant.charAt(i) + "\n"); 
			builder.append("call String.appendChar 1\n");
		}
		return builder.toString();
	}

	@Override
	public String getName() {
		return "string-constant-" + constant;
	}
}
