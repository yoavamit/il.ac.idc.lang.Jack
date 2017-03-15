package il.ac.idc.lang.compiler;

public class JackUnaryTermArifact extends JackTermArtifact {

	char op;
	JackTermArtifact term;
	
	@Override
	public String writeVMCode() {
		StringBuilder builder = new StringBuilder();
		term.parent = this;
		builder.append(term.writeVMCode());
		switch(op) {
		case '-':
			builder.append("neg\n");
			break;
		case '~':
			builder.append("not\n");
			break;
		}
		return builder.toString();
	}

}
