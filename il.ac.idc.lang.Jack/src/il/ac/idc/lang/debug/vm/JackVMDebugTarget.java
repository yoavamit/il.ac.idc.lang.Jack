package il.ac.idc.lang.debug.vm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;

import il.ac.idc.lang.launching.IHackLaunchConfigurationConstants;

public class JackVMDebugTarget extends JackVMDebugElement implements IDebugTarget {

	class JackVMEventDispatchJob extends Job {

		public JackVMEventDispatchJob(String name) throws IOException {
			super(name);
			setSystem(true);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			String event = "";
			while(!isTerminated() && event != null) {
				try {
					event = new BufferedReader(new InputStreamReader(eventSocket.getInputStream())).readLine();
					System.out.println("Got event from client: " + event);
					if (event != null) {
						String[] eventDetails = event.split("\\|");
						switch(eventDetails[0]) {
						case "started":
							fireCreationEvent();
							IBreakpoint[] breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(IJackVMConstants.JACK_VM_DEBUG_MODEL_ID);
							for (int i = 0; i < breakpoints.length; i++) {
								breakpointAdded(breakpoints[i]);
							}
//							try {
//								resume();
//							} catch (DebugException e) {
//							}
							break;
						case "terminated":
							terminated();
							break;
						case "resumed":
							switch(eventDetails[1]) {
							case "step":
								myThread.setStepping(true);
								resumed(DebugEvent.STEP_OVER);
								break;
							case "client":
								resumed(DebugEvent.CLIENT_REQUEST);
								break;
							}
							break;
						case "suspended":
							switch(eventDetails[1]) {
							case "client":
								suspended(DebugEvent.CLIENT_REQUEST);
								break;
							case "step":
								suspended(DebugEvent.STEP_END);
								break;
							case "breakpoint":
								break;
							}
							break;
						}
					}
				} catch (IOException e) {
					System.out.println("Cannot read event from JackVM: " + e.getMessage());
				}
			}
			return Status.OK_STATUS;
		}
		
	}
	
	private IProcess process;
	private JackVMThread myThread;
	private IThread[] programThreads;
	private ILaunch launch;
	private Socket requestSocket, eventSocket;
	private PrintWriter requestPrinter;
	private BufferedReader requestReader;
	private JackVMEventDispatchJob jobDispatcher;
	private boolean isSuspended;
	
	public JackVMDebugTarget(ILaunch launch, IProcess process, int requestPort, int eventPort) throws CoreException {
		super(null);
		this.launch = launch;
		target = this;
		this.process = process;
		myThread = new JackVMThread(this);
		programThreads = new IThread[] {myThread};
		try {
			Thread.sleep(1000);
			requestSocket = new Socket("127.0.0.1", requestPort);
			requestPrinter = new PrintWriter(requestSocket.getOutputStream());
			requestReader = new BufferedReader(new InputStreamReader(requestSocket.getInputStream()));
			eventSocket = new Socket("127.0.0.1", eventPort);
			jobDispatcher = new JackVMEventDispatchJob("VM Event Dispatcher");
			jobDispatcher.schedule();
		} catch (IOException e) {
			abort("Cannot connect to JackVM", e);
		} catch (InterruptedException e) {
		}
		DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(this);
	}
	
	@Override
	public boolean canTerminate() {
		return process.canTerminate();
	}

	@Override
	public boolean isTerminated() {
		return process.isTerminated();
	}

	@Override
	public void terminate() throws DebugException {
		synchronized (requestSocket) {
			requestPrinter.println("exit");
			requestPrinter.flush();
		}
	}

	@Override
	public boolean canResume() {
		return !isTerminated() && isSuspended();
	}

	@Override
	public boolean canSuspend() {
		return !isTerminated() && !isSuspended();
	}

	@Override
	public boolean isSuspended() {
		return isSuspended;
	}

	@Override
	public void resume() throws DebugException {
		sendRequest("resume");
	}
	
	@Override
	public void suspend() throws DebugException {
		sendRequest("suspend");
	}

	@Override
	public void breakpointAdded(IBreakpoint breakpoint) {
		if (supportsBreakpoint(breakpoint)) {
			try {
				if (breakpoint.isEnabled()) {
					sendRequest("set|" + ((ILineBreakpoint)breakpoint).getLineNumber());
				}
			} catch (CoreException e) {
			}
		}
	}

	@Override
	public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
		if (supportsBreakpoint(breakpoint)) {
			try {
				if (breakpoint.isEnabled()) {
					breakpointAdded(breakpoint);
				} else {
					breakpointRemoved(breakpoint, delta);
				}
			} catch (CoreException e) {
			}
		}
	}

	@Override
	public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
		if (supportsBreakpoint(breakpoint)) {
			try {
				sendRequest("clear|" + ((ILineBreakpoint)breakpoint).getLineNumber());
			} catch (CoreException e) {
			}
		}
	}

	@Override
	public boolean canDisconnect() {
		return false;
	}

	@Override
	public void disconnect() throws DebugException {

	}

	@Override
	public boolean isDisconnected() {
		return false;
	}

	@Override
	public IMemoryBlock getMemoryBlock(long arg0, long arg1) throws DebugException {
		return null;
	}

	@Override
	public boolean supportsStorageRetrieval() {
		return false;
	}

	@Override
	public String getName() throws DebugException {
		return "Jack VM";
	}

	@Override
	public IProcess getProcess() {
		return process;
	}

	@Override
	public IThread[] getThreads() throws DebugException {
		return programThreads;
	}

	@Override
	public boolean hasThreads() throws DebugException {
		return true;
	}

	@Override
	public boolean supportsBreakpoint(IBreakpoint breakpoint) {
		// we only support line breakpoints
		if (breakpoint instanceof ILineBreakpoint) {
			// check it our kind of breakpoint
			if (breakpoint.getModelIdentifier().equals(IJackVMConstants.JACK_VM_DEBUG_MODEL_ID)) {
				try {
					// check that the breakpoint belongs to our program
					String program = getLaunch().getLaunchConfiguration().getAttribute(IHackLaunchConfigurationConstants.ATTR_PROGRAM_FILENAME, (String) null);
					if (program != null) {
						IMarker marker = breakpoint.getMarker();
						if (marker != null) {
							return marker.getResource().getFullPath().equals(new Path(program));
						}
					}
				} catch (CoreException e) {
					
				}
			}
		}
		return false;
	}

	private void sendRequest(String request) throws DebugException {
		synchronized (requestSocket) {
			requestPrinter.println(request);
			requestPrinter.flush();
			try {
				requestReader.readLine();
			} catch (IOException e) {
				abort("Debug request failed", e);
			}
		}
	}
	
	private void resumed(int detail) {
		isSuspended = false;
		myThread.fireResumeEvent(detail);
	}
	
	private void suspended(int detail) {
		isSuspended = true;
		myThread.fireSuspendEvent(detail);
	}

	public void step() throws DebugException {
		sendRequest("step");
	}
	
	private void terminated() {
		isSuspended = false;
		DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(this);
		fireTerminateEvent();
	}
	
	protected IStackFrame[] getStackFrames() throws DebugException {
		synchronized (requestSocket) {
			requestPrinter.println("stack");
			requestPrinter.flush();
			try {
				String data = requestReader.readLine();
				if (data != null) {
					String[] frames = data.split("#");
					IStackFrame[] stackFrames = new IStackFrame[frames.length];
					for (int i = 0; i < frames.length; i++) {
						stackFrames[frames.length - i - 1] = new JackVMStackFrame(myThread, frames[i]);
					}
					return stackFrames;
				}
			} catch (IOException e) {
				abort("Cannot get stack frames", e);
			}
		}
		return new IStackFrame[0];
	}

	@Override
	public ILaunch getLaunch() {
		return launch;
	}
}
