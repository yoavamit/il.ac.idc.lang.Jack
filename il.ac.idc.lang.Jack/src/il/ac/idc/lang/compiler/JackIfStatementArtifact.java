package il.ac.idc.lang.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class JackIfStatementArtifact extends JackStatementArtifact {
	
	JackExpressionArtifact condition;
	List<JackStatementArtifact> trueClause = new ArrayList<>();
	List<JackStatementArtifact> falseClause = new ArrayList<>();

	private String getClassName() {
		IJackLanguageArtifact klass = getParent();
		while(!(klass instanceof JackClassArtifact)) {
			klass = klass.getParent();
		}
		return ((JackClassArtifact)klass).name;
	}
	
	private String getSubroutineName() {
		IJackLanguageArtifact subroutine = getParent();
		while(!(subroutine instanceof JackSubroutineArtifact)) {
			subroutine = subroutine.getParent();
		}
		return ((JackSubroutineArtifact)subroutine).name;
	}
	
	@Override
	public String writeVMCode() {
		StringBuilder builder = new StringBuilder();
		int random = new Random().nextInt();
		String elseLabel =   getClassName() + "-" + getSubroutineName() + "-branch-else-" + random;
		String endIfLabel = getClassName() +"-" + getSubroutineName() + "-end-if-" + random;
		
		condition.parent = this;
		builder.append(condition.writeVMCode());
		builder.append("if-goto " + elseLabel + "\n");
		for (JackStatementArtifact statement : trueClause) {
			statement.parent = this;
			builder.append(statement.writeVMCode());
		}
		builder.append("goto " + endIfLabel + "\n");
		builder.append("label " + elseLabel + "\n");
		for (JackStatementArtifact statement : falseClause) {
			statement.parent = this;
			builder.append(statement.writeVMCode());
		}
		builder.append("label " + endIfLabel + "\n");
		return builder.toString();
	}
}
