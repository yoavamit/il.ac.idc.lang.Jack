package il.ac.idc.lang.debug.vm;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

public class JackVMVariable extends JackVMDebugElement implements IVariable {

	private JackVMStackFrame frame;
	private String name;
	private IValue value;
	
	public JackVMVariable(JackVMStackFrame frame, String data) {
		super((JackVMDebugTarget)frame.getDebugTarget());
		this.frame = frame;
		parseVarData(data);
	}

	private void parseVarData(String data) {
		String[] split = data.split(":");
		name = split[0];
		value = new JackVMValue(this, Integer.parseInt(split[1]));
	}
	
	@Override
	public void setValue(String arg0) throws DebugException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setValue(IValue arg0) throws DebugException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean supportsValueModification() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean verifyValue(String arg0) throws DebugException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean verifyValue(IValue arg0) throws DebugException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() throws DebugException {
		return name;
	}

	@Override
	public String getReferenceTypeName() throws DebugException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IValue getValue() throws DebugException {
		// TODO Auto-generated method stub
		return value;
	}

	@Override
	public boolean hasValueChanged() throws DebugException {
		// TODO Auto-generated method stub
		return false;
	}

}
