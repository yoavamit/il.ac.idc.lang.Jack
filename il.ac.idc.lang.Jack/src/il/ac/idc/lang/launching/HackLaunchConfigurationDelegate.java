package il.ac.idc.lang.launching;

import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.osgi.framework.Bundle;

public class HackLaunchConfigurationDelegate implements ILaunchConfigurationDelegate {

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
			throws CoreException {
		String filename = configuration.getAttribute(IHackLaunchConfigurationConstants.ATTR_PROGRAM_FILENAME, "");
		String format = configuration.getAttribute(IHackLaunchConfigurationConstants.ATTR_PROGRAM_FORMAT, "s");
		String classPath = getPluginPath(); 
		String[] cmd = new String[] {"java", 
				"-cp", classPath, 
				"il.ac.idc.lang.emulator.CPUEmulator", 
				"--inputFile", filename, 
				"--format", format};
		Process process = DebugPlugin.exec(cmd, null);
		DebugPlugin.newProcess(launch, process, null);
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
