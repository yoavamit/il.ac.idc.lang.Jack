package il.ac.idc.lang.ui.launch;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import il.ac.idc.lang.launching.IHackLaunchConfigurationConstants;

public class HackParametersTab extends AbstractLaunchConfigurationTab {

	private Combo filename;
	private Combo fileFormat;
	
	@Override
	public void createControl(Composite composite) {
		Composite parent = new Composite(composite, SWT.NONE);
		parent.setLayout(new GridLayout(2, false));
		Label filenameLabel = new Label(parent, SWT.NONE);
		filenameLabel.setText("Jack project");
		filename = new Combo(parent, SWT.DROP_DOWN | SWT.BORDER | SWT.Selection | SWT.SINGLE);
		filename.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IProject[] projects = workspaceRoot.getProjects();
		List<String> projectNames = new ArrayList<String>();
		for (IProject project : projects) {
			projectNames.add(project.getLocationURI().getPath());
		}
		String[] names = projectNames.toArray(new String[]{});
		filename.setItems(names);
		
		Label fileFormatLabel = new Label(parent, SWT.NONE);
		fileFormatLabel.setText("Format");
		fileFormat = new Combo(parent, SWT.BORDER | SWT.Selection | SWT.SINGLE);
		String[] formats = new String[] {"Binary strings", "Numeric"};
		fileFormat.setItems(formats);
		setControl(parent);
	}

	@Override
	public String getName() {
		return "Main";
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			filename.setText(configuration.getAttribute(IHackLaunchConfigurationConstants.ATTR_PROGRAM_FILENAME, ""));
		} catch (CoreException e) {
			filename.setText("No filename specified");
		}
		try {
			String format = configuration.getAttribute(IHackLaunchConfigurationConstants.ATTR_PROGRAM_FORMAT, "s");
			if (format.equals("s")) {
				fileFormat.setText("Binary strings");
			} else {
				fileFormat.setText("Numeric");
			}
		} catch (CoreException e) {
			fileFormat.setText("Binary strings");
		}
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(IHackLaunchConfigurationConstants.ATTR_PROGRAM_FORMAT, fileFormat.getText());
		configuration.setAttribute(IHackLaunchConfigurationConstants.ATTR_PROGRAM_FILENAME, filename.getText());
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(IHackLaunchConfigurationConstants.ATTR_PROGRAM_FORMAT, "s");
		configuration.setAttribute(IHackLaunchConfigurationConstants.ATTR_PROGRAM_FILENAME, "");
	}
}
