package il.ac.idc.lang.compiler;

public abstract class AbstractJackObject {

	protected int lineNumber;
	protected AbstractJackObject parent;
	
	public AbstractJackObject(int lineNumber) {
		this.lineNumber = lineNumber;
	}
	
	public AbstractJackObject getParent() {
		return parent;
	}
	
	protected String getSubroutineName() {
		AbstractJackObject sub = this;
		while (sub != sub.parent && !(sub instanceof AbstractJackSubroutine)) {
			sub = sub.getParent();
		}
		if (sub instanceof AbstractJackSubroutine) {
			return sub.getName();
		} else {
			return null;
		}
	}
	
	protected String getClassName() {
		AbstractJackObject klass = this;
		while(klass != klass.parent && !(klass instanceof JackClass)) {
			klass = klass.parent;
		}
		if (klass instanceof JackClass) {
			return klass.getName();
		} else {
			return null;
		}
	}
	
	public abstract String getName();
	public abstract String writeVMCode();
}