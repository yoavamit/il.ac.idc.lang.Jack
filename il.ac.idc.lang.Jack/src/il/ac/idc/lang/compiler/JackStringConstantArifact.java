package il.ac.idc.lang.compiler;

public class JackStringConstantArifact extends JackTermArtifact {

	String constant;

	@Override
	public String writeVMCode() {
		StringBuilder builder = new StringBuilder();
		builder.append("call Memory.alloc " + constant.length() + "\n");
		for (int i = 0; i < constant.length(); i++) {
			builder.append("push constant " + (short) constant.charAt(i) + "\n"); 
			builder.append("call String.appendChar 1\n");
		}
		return builder.toString();
	}

}
