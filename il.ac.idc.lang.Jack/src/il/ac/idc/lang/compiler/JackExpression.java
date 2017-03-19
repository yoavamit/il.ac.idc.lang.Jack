package il.ac.idc.lang.compiler;

public class JackExpression extends AbstractJackTerm {

	private static int id = 0;
	private AbstractJackTerm left;
	private AbstractJackTerm right;
	char op;
	
	public JackExpression(int lineNumber) {
		super(lineNumber);
		id++;
	}

	void setLeft(AbstractJackTerm left) {
		left.parent = this;
		this.left = left;
	}
	
	void setRight(AbstractJackTerm right) {
		right.parent = this;
		this.right = right;
	}

	@Override
	public String writeVMCode() {
		StringBuilder builder = new StringBuilder();
		builder.append("// " + getName() + "\n");
		builder.append(left.writeVMCode());
		if (right != null) {
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

	@Override
	public String getName() {
		return getClassName() + "." + getSubroutineName() +":expression-" + id;
	}
}
