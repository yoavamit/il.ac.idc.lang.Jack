package il.ac.idc.lang.debug.vm;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

public class JackVMValue extends JackVMDebugElement implements IValue {

	private JackVMVariable var;
	private String value;
	
	public JackVMValue(JackVMVariable var, String value) {
		super((JackVMDebugTarget) var.getDebugTarget());
		this.var = var;
		if (value.indexOf('|') > 0) {
			this.value = value.split("\\|")[3];
		} else {
			this.value = value;
		}
	}

	@Override
	public String getReferenceTypeName() throws DebugException {
		return var.getReferenceTypeName();
	}

	@Override
	public String getValueString() throws DebugException {
		return "" + value;
	}

	@Override
	public IVariable[] getVariables() throws DebugException {
		String[] data = ((JackVMDebugTarget)getDebugTarget()).sendRequest("value-get|" + getReferenceTypeName() + "|" + value).split(",");
		IVariable[] vars = new IVariable[data.length];
		for (int i = 0; i < data.length; i++) {
			String type, name, address;
			String[] datum = data[i].split("\\|");
			type = datum[1];
			name = datum[2];
			address = datum[3];
			vars[i] = new JackVMVariable(this, type + ":" + name + ":" + address);
		}
		return vars;
	}

	private boolean isPrimitive() throws DebugException {
		switch(getReferenceTypeName()) {
		case "int":
		case "char":
		case "boolean":
			return false;
		default:
			return true;
		}
	}
	
	@Override
	public boolean hasVariables() throws DebugException {
		return isPrimitive();
	}

	@Override
	public boolean isAllocated() throws DebugException {
		if (!isPrimitive()) {
			return !value.equals("0");
		}
		return true;
	}
}
