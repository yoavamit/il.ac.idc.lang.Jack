package il.ac.idc.lang.debug.vm;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;

public abstract class JackVMDebugElement extends PlatformObject implements IDebugElement {

	protected JackVMDebugTarget target;
	
	public JackVMDebugElement(JackVMDebugTarget target) {
		this.target = target;
	}
	
	@Override
	public ILaunch getLaunch() {
		return getDebugTarget().getLaunch();
	}
	
	@Override
	public IDebugTarget getDebugTarget() {
		return target;
	}

	@Override
	public String getModelIdentifier() {
		return IJackVMConstants.JACK_VM_DEBUG_MODEL_ID;
	}

	protected void abort(String message, Throwable e) throws DebugException {
		throw new DebugException(new Status(IStatus.ERROR, "il.ac.idc.lang.Jack", message, e));
	}
	
	protected void fireEvent(DebugEvent event) {
		DebugPlugin.getDefault().fireDebugEventSet(new DebugEvent[] {event});
	}
	
	protected void fireCreationEvent() {
		fireEvent(new DebugEvent(this, DebugEvent.CREATE));
	}
	
	protected void fireResumeEvent(int details) {
		fireEvent(new DebugEvent(this, DebugEvent.RESUME, details));
	}
	
	protected void fireSuspendEvent(int details) {
		fireEvent(new DebugEvent(this, DebugEvent.SUSPEND, details));
	}
	
	protected void fireTerminateEvent() {
		fireEvent(new DebugEvent(this, DebugEvent.TERMINATE));
	}
}
