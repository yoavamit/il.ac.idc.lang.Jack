package il.ac.idc.lang.debug.vm;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

public class JackVMVariable extends JackVMDebugElement implements IVariable {

	private String name;
	private String type;
	private String address;
	private IValue value;
	
	public JackVMVariable(JackVMDebugElement parent, String data) {
		super((JackVMDebugTarget)parent.getDebugTarget());
		String[] varData = data.split(":");
		type = varData[0];
		name = varData[1];
		address = varData[2];  
	}

	private boolean isPrimitive() {
		switch(type) {
		case "int":
		case "char":
		case "boolean":
			return true;
		default:
			return false;
		}
	}
	
	@Override
	public void setValue(String expression) throws DebugException {
		((JackVMDebugTarget)getDebugTarget()).sendRequest("value-set|" + address + "|" + Integer.parseInt(expression));
	}

	@Override
	public void setValue(IValue value) throws DebugException {
		if (value instanceof JackVMValue) {
			setValue(value.getValueString());
		}
	}

	@Override
	public boolean supportsValueModification() {
		return isPrimitive();
	}

	@Override
	public boolean verifyValue(String expression) throws DebugException {
		try {
			Integer.parseInt(expression);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	@Override
	public boolean verifyValue(IValue value) throws DebugException {
		return verifyValue(value.getValueString());
	}

	@Override
	public String getName() throws DebugException {
		return name;
	}

	@Override
	public String getReferenceTypeName() throws DebugException {
		return type;
	}

	@Override
	public IValue getValue() throws DebugException {
		if (value == null) {
			String data = ((JackVMDebugTarget)getDebugTarget()).sendRequest("value-get|int|" + address);
			value = new JackVMValue(this, data);
		}
		return value;
	}

	@Override
	public boolean hasValueChanged() throws DebugException {
		return false;
	}

}
