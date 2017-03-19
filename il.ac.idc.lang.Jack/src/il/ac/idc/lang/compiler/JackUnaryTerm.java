package il.ac.idc.lang.compiler;

public class JackUnaryTerm extends AbstractJackTerm {

	private static int id = 0;
	private char op;
	private AbstractJackTerm term;

	public JackUnaryTerm(int lineNumber, char op, AbstractJackTerm term) {
		super(lineNumber);
		term.parent = this;
		this.term = term;
		this.op = op;
		id++;
	}
	
	@Override
	public String writeVMCode() {
		StringBuilder builder = new StringBuilder();
		builder.append("// " + getName() + "\n");
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

	@Override
	public String getName() {
		return getClassName() + "." + getSubroutineName() + ":unary-term-" + id;
	}

}
