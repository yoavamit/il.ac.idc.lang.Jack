package il.ac.idc.lang.assembler;

public class Code {

	public static int dest(String dest) {
		int destBits = 0;
		if (dest.indexOf("A") >= 0) {
			destBits += 4;
		}
		if (dest.indexOf("D") >= 0) {
			destBits += 2;
		}
		if (dest.indexOf("M") >= 0) {
			destBits += 1;
		}
		return destBits << 3;
	}
	
	public static int jump(String jump) {
		int jumpBits = 0;
		switch(jump) {
		case "":
			break;
		case "JGT":
			jumpBits = 1;
			break;
		case "JEQ":
			jumpBits = 2;
			break;
		case "JGE":
			jumpBits = 3;
			break;
		case "JLT":
			jumpBits = 4;
			break;
		case "JNE":
			jumpBits = 5;
			break;
		case "JLE":
			jumpBits = 6;
			break;
		case "JMP":
			jumpBits = 7;
			break;
		}
		return jumpBits;
	}
	
	public static int comp(String comp) {
		int compBits = 0;
		switch(comp) {
		case "0":
			compBits = 42;
			break;
		case "1":
			compBits = 63;
			break;
		case "-1":
			compBits = 58;
			break;
		case "D":
			compBits = 12;
			break;
		case "A":
			compBits = 48;
			break;
		case "M":
			compBits = 112;
			break;
		case "!D":
			compBits = 13;
			break;
		case "!A":
			compBits = 49;
			break;
		case "!M":
			compBits = 113;
			break;
		case "-D":
			compBits = 15;
			break;
		case "-A":
			compBits = 51;
			break;
		case "-M":
			compBits = 115;
			break;
		case "D+1":
			compBits = 31;
			break;
		case "A+1":
			compBits = 55;
			break;
		case "M+1":
			compBits = 119;
			break;
		case "D-1":
			compBits = 14;
			break;
		case "A-1":
			compBits = 50;
			break;
		case "M-1":
			compBits = 114;
			break;
		case "D+A":
			compBits = 2;
			break;
		case "D+M":
			compBits = 34;
			break;
		case "D-A":
			compBits = 19;
			break;
		case "D-M":
			compBits = 83;
			break;
		case "A-D":
			compBits = 7;
			break;
		case "M-D":
			compBits = 71;
			break;
		case "D&A":
			compBits = 0;
			break;
		case "D&M":
			compBits = 64;
			break;
		case "D|A":
			compBits = 21;
			break;
		case "D|M":
			compBits = 85;
			break;
		}
		return compBits << 6;
	}
}
