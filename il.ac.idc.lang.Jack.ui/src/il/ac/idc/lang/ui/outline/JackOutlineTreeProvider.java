/*
* generated by Xtext
*/
package il.ac.idc.lang.ui.outline;

import org.eclipse.emf.ecore.EObject;

import il.ac.idc.lang.jack.ClassVarDecl;
import il.ac.idc.lang.jack.SubroutineDecl;

/**
 * Customization of the default outline structure.
 *
 * see http://www.eclipse.org/Xtext/documentation.html#outline
 */
public class JackOutlineTreeProvider extends org.eclipse.xtext.ui.editor.outline.impl.DefaultOutlineTreeProvider {
	
	@Override
	protected Object _text(Object modelElement) {
		if (modelElement instanceof ClassVarDecl) {
			return ((ClassVarDecl) modelElement).getAccess() + ": " + ((ClassVarDecl)modelElement).getType() + " "
					+ ((ClassVarDecl) modelElement).getVarName();
		}
		if (modelElement instanceof SubroutineDecl) {
			return ((SubroutineDecl) modelElement).getSubroutineName();
		}
		return super._text(modelElement);
	}
	
	@Override
	protected boolean _isLeaf(EObject modelElement) {
		if (modelElement instanceof SubroutineDecl) {
			return true;
		}
		return super._isLeaf(modelElement);
	}
	
	
}
