package il.ac.idc.lang.ui.debug;

import org.eclipse.core.resources.IFile;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;

import il.ac.idc.lang.debug.vm.JackVMValue;

public class JackVMModelPresentation implements IDebugModelPresentation {

	@Override
	public void addListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isLabelProperty(Object arg0, String arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getEditorId(IEditorInput editorInput, Object element) {
		if (element instanceof IFile || element instanceof ILineBreakpoint) {
			return "org.eclipse.ui.DefaultTextEditor";
		}
		return null;
	}

	@Override
	public IEditorInput getEditorInput(Object element) {
		if (element instanceof IFile) {
			return new FileEditorInput((IFile)element);
		}
		if (element instanceof ILineBreakpoint) {
			return new FileEditorInput((IFile)((ILineBreakpoint)element).getMarker().getResource());
		}
		return null;
	}

	@Override
	public void computeDetail(IValue value, IValueDetailListener listener) {
		String computedDetails = null;
		if (value instanceof JackVMValue) {
			try {
				String type = value.getReferenceTypeName();
				switch(type) {
				case "int":
					computedDetails = value.getValueString();
					break;
				case "char":
					computedDetails = "" + (char) Integer.parseInt(value.getValueString());
					break;
				case "boolean":
					computedDetails = value.getValueString().equals("-1") ? "True" : "False";
					break;
				default:
					computedDetails = value.getReferenceTypeName() + "@" + value.getValueString();
				}
			} catch (DebugException e) {
				
			}
		}
		listener.detailComputed(value, computedDetails);
	}

	@Override
	public Image getImage(Object object) {
		return null;
	}

	@Override
	public String getText(Object object) {
		try {
			if  (object instanceof IDebugTarget) {
				return ((IDebugTarget)object).getName();
			}
			if (object instanceof IThread) {
				return ((IThread)object).getName();
			}
			if (object instanceof IStackFrame) {
				return ((IStackFrame) object).getName();
			}
			if (object instanceof IVariable) {
				return ((IVariable) object).getName();
			}
		} catch (DebugException e) {
			
		}
		return null;
	}

	@Override
	public void setAttribute(String arg0, Object arg1) {
		// TODO Auto-generated method stub

	}

}
