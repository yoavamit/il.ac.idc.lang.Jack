package il.ac.idc.lang.debug.vm;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

public class JackVMValue extends JackVMDebugElement implements IValue {

	private IVariable var;
	private int value;
	
	public JackVMValue(JackVMVariable var, int value) {
		super((JackVMDebugTarget) var.getDebugTarget());
		this.var = var;
		this.value = value;
	}

	@Override
	public String getReferenceTypeName() throws DebugException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getValueString() throws DebugException {
		return "" + value;
	}

	@Override
	public IVariable[] getVariables() throws DebugException {
		return new IVariable[]{var};
	}

	@Override
	public boolean hasVariables() throws DebugException {
		return true;
	}

	@Override
	public boolean isAllocated() throws DebugException {
		// TODO Auto-generated method stub
		return false;
	}

}
