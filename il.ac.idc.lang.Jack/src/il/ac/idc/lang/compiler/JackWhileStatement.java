package il.ac.idc.lang.compiler;

import java.util.ArrayList;
import java.util.List;

public class JackWhileStatement extends AbstractJackStatement {
	
	private static int index = 0;
	private int id;
	
	public JackWhileStatement(int lineNumber) {
		super(lineNumber);
		id = index;
		index++;
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
		if (lineNumber != parent.lineNumber) {
			builder.append("// sourceLine:" + lineNumber + "\n");
		}
		String conditionLabel = getSubroutineName() + "-while-condition-" + id;
		String endWhileLabel = getSubroutineName() + "-while-end-" + id;
		
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
	public String getId() {
		return getClassName() + "." + getSubroutineName() + ":statement-while-" + id;
	}
	
}
