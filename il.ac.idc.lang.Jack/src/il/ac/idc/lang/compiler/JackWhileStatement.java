package il.ac.idc.lang.compiler;

import java.util.ArrayList;
import java.util.List;

public class JackWhileStatement extends AbstractJackStatement {
	
	private static int id = 0; 
	
	public JackWhileStatement(int lineNumber) {
		super(lineNumber);
		id++;
	}

	private JackExpression condition;
	private List<AbstractJackStatement> statements = new ArrayList<>();
	
	void setCondition(JackExpression condition) {
		condition.parent = this;
		this.condition = condition;
	}
	
	void setStatements(List<AbstractJackStatement> statements) {
		for (AbstractJackStatement statement : statements) {
			statement.parent = this;
			this.statements.add(statement);
		}
	}
	
	@Override
	public String writeVMCode() {
		StringBuilder builder = new StringBuilder();
		builder.append("// " + getName() + "\n");
		String conditionLabel = getClassName() + "-" + getSubroutineName() + "-while-condition-" + id;
		String endWhileLabel = getClassName() + "-" + getSubroutineName() + "-while-end-" + id;
		
		builder.append("label " + conditionLabel + "\n");
		builder.append(condition.writeVMCode());
		builder.append("if-goto " + endWhileLabel + "\n");
		for (AbstractJackStatement statement : statements) {
			builder.append(statement.writeVMCode());
		}
		builder.append("goto " + conditionLabel + "\n");
		builder.append("label " + endWhileLabel + "\n");
		return builder.toString();
	}

	@Override
	public String getName() {
		return getClassName() + "." + getSubroutineName() + ":statement-while-" + id;
	}
	
}
