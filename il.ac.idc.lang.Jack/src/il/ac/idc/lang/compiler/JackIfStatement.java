package il.ac.idc.lang.compiler;

import java.util.ArrayList;
import java.util.List;

public class JackIfStatement extends AbstractJackStatement {
	
	private static int id = 0;
	private JackExpression condition;
	private List<AbstractJackStatement> trueClause = new ArrayList<>();
	private List<AbstractJackStatement> falseClause = new ArrayList<>();
	
	public JackIfStatement(int lineNumber) {
		super(lineNumber);
		id++;
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
		builder.append("// " + getName() + "\n");
		String elseLabel =   getClassName() + "-" + getSubroutineName() + "-branch-else-" + id;
		String endIfLabel = getClassName() +"-" + getSubroutineName() + "-end-if-" + id;
		
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
	public String getName() {
		return getClassName() + "." + getSubroutineName() + ":statement-if-" + id;
	}
}
