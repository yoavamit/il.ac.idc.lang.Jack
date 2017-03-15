package il.ac.idc.lang.compiler;

public class JackKeywordTermArtifact extends JackTermArtifact {

	String keyword;

	@Override
	public String writeVMCode() {
		switch(keyword) {
		case "NULL":
			return "push constant 0\n";
		case "FALSE":
			return "push constant 0\n";
		case "TRUE":
			return "push constant 0\nnot\n";
		case "THIS":
			return "push pointer 0\n";
		}
		return null;
	}
	
}
