package il.ac.idc.lang.compiler;

public class JackExpressionArtifact extends JackTermArtifact {

	JackTermArtifact left;
	char op;
	JackTermArtifact right;

	@Override
	public String writeVMCode() {
		StringBuilder builder = new StringBuilder();
		left.parent = this;
		builder.append(left.writeVMCode());
		if (right != null) {
			right.parent = this;
			builder.append(right.writeVMCode());
			switch(op) {
			case '+':
				builder.append("add\n");
				break;
			case '-':
				builder.append("sub\n");
				break;
			case '*':
				builder.append("call Math.mult 2\n");
				break;
			case '/':
				builder.append("call Math.div 2\n");
				break;
			case '&':
				builder.append("and\n");
				break;
			case '|':
				builder.append("or\n");
				break;
			case '<':
				builder.append("lt\n");
				break;
			case '>':
				builder.append("gt\n");
				break;
			case '=':
				builder.append("eq\n");
			}
		}
		return builder.toString();
	}

}
