package il.ac.idc.lang.debug.vm;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;

public class JackVMStackFrame extends JackVMDebugElement implements IStackFrame {

	private JackVMThread thread;
	private String name;
	int id;
	private int pc;
	private IVariable[] vars;
	
	public JackVMStackFrame(JackVMThread thread, int id, String data) {
		super((JackVMDebugTarget) thread.getDebugTarget());
		this.id = id;
		this.thread = thread;
		parseFrameData(data);
	}
	
	private void parseFrameData(String frameData) {
		String[] data = frameData.split("\\|");
		if (data.length >= 2) {
			name = data[0];
			pc = Integer.parseInt(data[1]);
		}
		vars = new IVariable[data.length - 2];
		for (int i = 2; i < data.length; i++) {
			vars[i - 2] = new JackVMVariable(this, data[i]);
		}
	}

	@Override
	public boolean canStepInto() {
		return getThread().canStepInto();
	}

	@Override
	public boolean canStepOver() {
		return getThread().canStepOver();
	}

	@Override
	public boolean canStepReturn() {
		return getThread().canStepReturn();
	}

	@Override
	public boolean isStepping() {
		return getThread().isStepping();
	}

	@Override
	public void stepInto() throws DebugException {
		getThread().stepInto();
	}

	@Override
	public void stepOver() throws DebugException {
		getThread().stepOver();
	}

	@Override
	public void stepReturn() throws DebugException {
		getThread().stepReturn();
	}

	@Override
	public boolean canResume() {
		return getThread().canResume();
	}

	@Override
	public boolean canSuspend() {
		return getThread().canSuspend();
	}

	@Override
	public boolean isSuspended() {
		return getThread().isSuspended();
	}

	@Override
	public void resume() throws DebugException {
		getThread().resume();
	}

	@Override
	public void suspend() throws DebugException {
		getThread().suspend();
	}

	@Override
	public boolean canTerminate() {
		return getThread().canTerminate();
	}

	@Override
	public boolean isTerminated() {
		return getThread().isTerminated();
	}

	@Override
	public void terminate() throws DebugException {
		getThread().terminate();
	}

	@Override
	public int getCharEnd() throws DebugException {
		return -1;
	}

	@Override
	public int getCharStart() throws DebugException {
		return -1;
	}

	@Override
	public int getLineNumber() throws DebugException {
		return pc;
	}

	@Override
	public String getName() throws DebugException {
		return name;
	}

	@Override
	public IRegisterGroup[] getRegisterGroups() throws DebugException {
		return null;
	}

	@Override
	public IThread getThread() {
		return thread;
	}

	@Override
	public IVariable[] getVariables() throws DebugException {
		if (vars == null) {
			String data = ((JackVMDebugTarget)getDebugTarget()).sendRequest("vars|" + id);
			if (data != null) {
				String[] varsData = data.split("\\|");
				IVariable[] vars = new IVariable[varsData.length];
				for (int i = 0; i < varsData.length; i++) {
					vars[i] = new JackVMVariable(this, varsData[i]);
				}
				this.vars = vars;
			}
		}
		return vars;
	}

	@Override
	public boolean hasRegisterGroups() throws DebugException {
		return false;
	}

	@Override
	public boolean hasVariables() throws DebugException {
		return ((JackVMDebugTarget)getDebugTarget()).sendRequest("vars|" + id).length() > 0;
	}

}
