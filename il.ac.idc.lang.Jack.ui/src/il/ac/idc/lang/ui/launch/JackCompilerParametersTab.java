package il.ac.idc.lang.ui.launch;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class JackCompilerParametersTab extends AbstractLaunchConfigurationTab {

	private Combo project;
	private Combo outputFormat;
	private Button debugInfo;
	
	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		Label projectLabel = new Label(composite, SWT.NONE);
		projectLabel.setText("Jack project");
		project = new Combo(composite, SWT.BORDER | SWT.Selection | SWT.SINGLE | SWT.DROP_DOWN);
		project.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IProject[] projects = workspaceRoot.getProjects();
		List<String> projectNames = new ArrayList<String>();
		for (IProject project : projects) {
			projectNames.add(project.getLocationURI().getPath());
		}
		String[] names = projectNames.toArray(new String[]{});
		project.setItems(names);
		
		Label outputFormatLabel = new Label(composite, SWT.NONE);
		outputFormatLabel.setText("Output format");
		outputFormat = new Combo(composite, SWT.BORDER | SWT.DROP_DOWN | SWT.SINGLE | SWT.Selection);
		outputFormat.setItems(new String[] {"binary", "bytecode"});
		
		debugInfo = new Button(composite, SWT.CHECK);
		debugInfo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		debugInfo.setText("Include debug information");
		debugInfo.setEnabled(false);
		
		final Label description = new Label(composite, SWT.NONE);
		description.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
		outputFormat.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent event) {
				String item = ((Combo)event.widget).getText();
				switch(item) {
				case "binary":
					description.setText("This option will produce a single output file named \"a.hack\" in binary format."
							+ " This file can later be run by the Jack CPU emulator program");
					debugInfo.setEnabled(false);
					break;
				case "bytecode":
					description.setText("This option will produce a Jack bytecode file for each resource compiled in the selected project");
					debugInfo.setEnabled(true);
					break;
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent event) {
				widgetSelected(event);
			}
		});
		setControl(composite);
;	}

	@Override
	public String getName() {
		return "Jack Compiler Parameters";
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {

	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy coniguration) {

	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {

	}

}
