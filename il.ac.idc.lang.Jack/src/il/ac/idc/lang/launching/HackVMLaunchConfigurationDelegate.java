package il.ac.idc.lang.launching;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.debug.core.model.IProcess;
import org.osgi.framework.Bundle;

import il.ac.idc.lang.debug.vm.JackVMDebugTarget;

public class HackVMLaunchConfigurationDelegate implements ILaunchConfigurationDelegate {

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
			throws CoreException {
		String filename = configuration.getAttribute(IHackLaunchConfigurationConstants.ATTR_PROGRAM_FILENAME, "");
		String classPath = getPluginPath();
		List<String> commandList = new ArrayList<>();
		commandList.add("java");
		commandList.add("-cp");
		commandList.add(classPath);
		commandList.add("il.ac.idc.lang.emulator.VMEmulator");
		commandList.add(filename);
		if (mode.equals(ILaunchManager.DEBUG_MODE)) {
			String requestPort = configuration.getAttribute(IHackLaunchConfigurationConstants.ATTR_VM_EMULATOR_DEBUG_REQUEST_PORT, "");
			String eventPort = configuration.getAttribute(IHackLaunchConfigurationConstants.ATTR_VM_EMULATOR_DEBUG_EVENT_PORT, "");
			commandList.add("--debug");
			commandList.add("--requestPort");
			commandList.add(requestPort);
			commandList.add("--eventPort");
			commandList.add(eventPort);
		}
		String[] cmd = commandList.toArray(new String[] {});
		Process process = DebugPlugin.exec(cmd, null);
		IProcess p = DebugPlugin.newProcess(launch, process, null);
		if (mode.equals(ILaunchManager.DEBUG_MODE)) {
			IDebugTarget target = new JackVMDebugTarget(p);
			launch.addDebugTarget(target);
		}
	}

	private static String getPluginPath() {
		Bundle bundle = Platform.getBundle("il.ac.idc.lang.Jack");
		Path path = new Path("/bin");
		URL url = FileLocator.find(bundle, path, null);
		try {
			return FileLocator.toFileURL(url).getPath();
		} catch (IOException e) {
		}
		return null;
	}
}
