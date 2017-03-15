package il.ac.idc.lang.compiler;

public class JackReturnStatementArtifact extends JackStatementArtifact {
	
	JackExpressionArtifact expression;
	
	@Override
	public String writeVMCode() {
		String returnString = "";
		if (expression != null) {
			expression.parent = this;
			returnString = expression.writeVMCode();
		} else {
			returnString = "push constant 0\n";
		}
		return returnString + "return\n";
	}

}
