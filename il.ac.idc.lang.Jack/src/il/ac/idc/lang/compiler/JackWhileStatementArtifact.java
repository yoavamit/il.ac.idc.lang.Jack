package il.ac.idc.lang.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class JackWhileStatementArtifact extends JackStatementArtifact {

	JackExpressionArtifact condition;
	List<JackStatementArtifact> statements = new ArrayList<>();
	
	private String getClassName() {
		IJackLanguageArtifact klass = getParent();
		while(!(klass instanceof JackClassArtifact)) {
			klass = klass.getParent();
		}
		return ((JackClassArtifact)klass).name;
	}
	
	private String getSubroutineName() {
		IJackLanguageArtifact sub = getParent();
		while(!(sub instanceof JackSubroutineArtifact)) {
			sub = sub.getParent();
		}
		return ((JackSubroutineArtifact)sub).name;
	}
	
	@Override
	public String writeVMCode() {
		StringBuilder builder = new StringBuilder();
		int random = new Random().nextInt();
		String conditionLabel = getClassName() + "-" + getSubroutineName() + "-while-condition-" + random;
		String endWhileLabel = getClassName() + "-" + getSubroutineName() + "-while-end-" + random;
		
		builder.append("label " + conditionLabel + "\n");
		condition.parent = this;
		builder.append(condition.writeVMCode());
		builder.append("if-goto " + endWhileLabel + "\n");
		for (JackStatementArtifact statement : statements) {
			statement.parent = this;
			builder.append(statement.writeVMCode());
		}
		builder.append("goto " + conditionLabel + "\n");
		builder.append("label " + endWhileLabel + "\n");
		return builder.toString();
	}

}
