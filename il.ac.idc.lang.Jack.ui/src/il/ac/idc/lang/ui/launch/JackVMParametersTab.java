package il.ac.idc.lang.ui.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import il.ac.idc.lang.launching.IHackLaunchConfigurationConstants;

public class JackVMParametersTab extends AbstractLaunchConfigurationTab {

	private Text requestPort, eventPort;
	
	@Override
	public void createControl(Composite parent) {
		Composite control = new Composite(parent, SWT.NONE);
		control.setLayout(new GridLayout(2, false));
		Label requestLabel = new Label(control, SWT.NONE);
		requestLabel.setText("Debugger requests port");
		requestPort = new Text(control, SWT.BORDER);
		requestPort.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		Label eventLabel = new Label(control, SWT.NONE);
		eventLabel.setText("Debugger events port");
		eventPort = new Text(control, SWT.BORDER);
		eventPort.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		setControl(control);
	}

	@Override
	public String getName() {
		return "VM emulator";
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		String confRequestPort = "", confEventPort = "";
		try {
			confRequestPort = configuration.getAttribute(IHackLaunchConfigurationConstants.ATTR_VM_EMULATOR_DEBUG_REQUEST_PORT, "1337");
			confEventPort = configuration.getAttribute(IHackLaunchConfigurationConstants.ATTR_VM_EMULATOR_DEBUG_EVENT_PORT, "1338");
		} catch (CoreException e) {
		}
		requestPort.setText(confRequestPort);
		eventPort.setText(confEventPort);
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(IHackLaunchConfigurationConstants.ATTR_VM_EMULATOR_DEBUG_REQUEST_PORT,  requestPort.getText());
		configuration.setAttribute(IHackLaunchConfigurationConstants.ATTR_VM_EMULATOR_DEBUG_EVENT_PORT, eventPort.getText());
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(IHackLaunchConfigurationConstants.ATTR_VM_EMULATOR_DEBUG_REQUEST_PORT, "1337");
		configuration.setAttribute(IHackLaunchConfigurationConstants.ATTR_VM_EMULATOR_DEBUG_EVENT_PORT, "1338");
	}

}
