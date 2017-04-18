package il.ac.idc.lang.compiler;

import java.util.ArrayList;
import java.util.List;

public class JackIfStatement extends AbstractJackStatement {
	
	private static int index = 0;
	private int id;
	private JackExpression condition;
	private List<AbstractJackStatement> trueClause = new ArrayList<>();
	private List<AbstractJackStatement> falseClause = new ArrayList<>();
	
	public JackIfStatement(int lineNumber) {
		super(lineNumber);
		id = index;
		index++;
	}

	void setCondition(JackExpression cond) {
		cond.parent = this;
		this.condition = cond;
	}
	
	void setTrueClause(List<AbstractJackStatement> statements) {
		for (AbstractJackStatement statement : statements) {
			statement.parent = this;
			trueClause.add(statement);
		}
	}
	
	void setFalseClause(List<AbstractJackStatement> statements) {
		for (AbstractJackStatement statement : statements) {
			statement.parent = this;
			falseClause.add(statement);
		}
	}
	
	@Override
	public String writeVMCode() {
		StringBuilder builder = new StringBuilder();
		if (lineNumber != parent.lineNumber) {
			builder.append("// sourceLine:" + lineNumber + "\n");
		}
		String elseLabel =   getSubroutineName() + "-branch-else-" + id;
		String endIfLabel = getSubroutineName() + "-end-if-" + id;
		
		builder.append(condition.writeVMCode());
		builder.append("if-goto " + elseLabel + "\n");
		for (AbstractJackStatement statement : trueClause) {
			builder.append(statement.writeVMCode());
		}
		builder.append("goto " + endIfLabel + "\n");
		builder.append("label " + elseLabel + "\n");
		for (AbstractJackStatement statement : falseClause) {
			builder.append(statement.writeVMCode());
		}
		builder.append("label " + endIfLabel + "\n");
		return builder.toString();
	}
	
	@Override
	public String getId() {
		return getKlassName() + "." + getSubroutineName() + ":statement-if-" + id;
	}
}
