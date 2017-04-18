package il.ac.idc.lang.compiler;

public class JackUnaryTerm extends AbstractJackTerm {

	private static int index = 0;
	private int id;
	private char op;
	private AbstractJackTerm term;

	public JackUnaryTerm(int lineNumber, char op, AbstractJackTerm term) {
		super(lineNumber);
		term.parent = this;
		this.term = term;
		this.op = op;
		id = index;
		index++;
	}
	
	@Override
	public String writeVMCode() {
		StringBuilder builder = new StringBuilder();
		if (lineNumber != parent.lineNumber) {
			builder.append("// sourceLine:" + lineNumber + "\n");
		}
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
	public String getId() {
		return getKlassName() + "." + getSubroutineName() + ":unary-term-" + id;
	}

}
