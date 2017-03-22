package il.ac.idc.lang.compiler;

public class JackReturnStatement extends AbstractJackStatement {
	
	private static int index = 0;
	private int id;
	private JackExpression expression;
	
	public JackReturnStatement(int lineNumber) {
		super(lineNumber);
		id = index;
		index++;
	}
	
	void setExpression(JackExpression exp) {
		exp.parent = this;
		this.expression = exp;
	}
	
	@Override
	public String writeVMCode() {
		String returnString = "// " + getName() + "\n";
		if (expression != null) {
			returnString = expression.writeVMCode();
		} else {
			returnString = "push constant 0\n";
		}
		return returnString + "return\n";
	}

	@Override
	public String getName() {
		return getClassName() + "." + getSubroutineName() + ":statement-return-" + id;
	}

}
