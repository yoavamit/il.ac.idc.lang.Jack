package il.ac.idc.lang.debug.vm;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;

public class JackVMThread extends JackVMDebugElement implements IThread {

	private boolean isStepping;
	private IBreakpoint[] breakpoints;
	
	public JackVMThread(JackVMDebugTarget target) {
		super(target);
	}

	@Override
	public boolean canResume() {
		return isSuspended();
	}

	@Override
	public boolean canSuspend() {
		return !isSuspended();
	}

	@Override
	public boolean isSuspended() {
		return getDebugTarget().isSuspended();
	}

	@Override
	public void resume() throws DebugException {
		getDebugTarget().resume();
	}

	@Override
	public void suspend() throws DebugException {
		getDebugTarget().suspend();
	}

	@Override
	public boolean canStepInto() {
		return false;
	}

	@Override
	public boolean canStepOver() {
		return isSuspended();
	}

	@Override
	public boolean canStepReturn() {
		return false;
	}

	@Override
	public boolean isStepping() {
		return isStepping;
	}

	@Override
	public void stepInto() throws DebugException {

	}

	@Override
	public void stepOver() throws DebugException {
		((JackVMDebugTarget)getDebugTarget()).step();
	}

	@Override
	public void stepReturn() throws DebugException {

	}

	@Override
	public boolean canTerminate() {
		return !isTerminated();
	}

	@Override
	public boolean isTerminated() {
		return getDebugTarget().isTerminated();
	}

	@Override
	public void terminate() throws DebugException {
		getDebugTarget().terminate();
	}

	@Override
	public IBreakpoint[] getBreakpoints() {
		if (breakpoints != null) {
			return breakpoints;
		}
		return new IBreakpoint[0];
	}
	
	protected void setBreakpoints(IBreakpoint[] breakpoints) {
		this.breakpoints = breakpoints;
	}

	@Override
	public String getName() throws DebugException {
		return "Thread[1]";
	}

	@Override
	public int getPriority() throws DebugException {
		return 0;
	}

	@Override
	public IStackFrame[] getStackFrames() throws DebugException {
		if (isSuspended()) {
			String data = ((JackVMDebugTarget)getDebugTarget()).sendRequest("stack");
			IStackFrame[] stackFrames = null;
			if (data != null) {
				String[] frames = data.split("#");
				stackFrames = new IStackFrame[frames.length];
				for (int i = 0; i < frames.length; i++) {
					stackFrames[frames.length - i - 1] = new JackVMStackFrame(this, i, frames[i]);
				}
			}
			return stackFrames;
		} else {
			return new IStackFrame[0];
		}
	}

	@Override
	public IStackFrame getTopStackFrame() throws DebugException {
		IStackFrame[] frames = getStackFrames();
		if (frames.length > 0) {
			return frames[0];
		}
		return null;
	}

	@Override
	public boolean hasStackFrames() throws DebugException {
		return isSuspended();
	}

	protected void setStepping(boolean stepping) {
		this.isStepping = stepping;
	}

}
