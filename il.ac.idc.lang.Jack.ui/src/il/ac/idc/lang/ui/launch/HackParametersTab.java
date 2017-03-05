package il.ac.idc.lang.ui.launch;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class HackParametersTab extends AbstractLaunchConfigurationTab {

	private Text filename;
	private Combo fileFormat;
	
	@Override
	public void createControl(Composite composite) {
		Composite parent = new Composite(composite, SWT.NONE);
		parent.setLayout(new GridLayout(2, false));
		Label filenameLabel = new Label(parent, SWT.NONE);
		filenameLabel.setText("Filename");
		filename = new Text(parent, SWT.FILL | SWT.BORDER);
		filename.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		Label fileFormatLabel = new Label(parent, SWT.NONE);
		fileFormatLabel.setText("Format");
		fileFormat = new Combo(parent, SWT.BORDER | SWT.Selection | SWT.SINGLE);
		fileFormat.add("Binary strings");
		fileFormat.add("Numeric");
		setControl(parent);
	}

	@Override
	public String getName() {
		return "Main";
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {

	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		
		configuration.setAttribute(IHackLaunchConfigurationConstants.ATTR_PROGRAM_FORMAT, "s");
		configuration.setAttribute(IHackLaunchConfigurationConstants.ATTR_PROGRAM_FILENAME, "");
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(IHackLaunchConfigurationConstants.ATTR_PROGRAM_FORMAT, "s");
		configuration.setAttribute(IHackLaunchConfigurationConstants.ATTR_PROGRAM_FILENAME, "");
	}

}
