package il.ac.idc.lang.util;

import il.ac.idc.lang.jack.Jack;
import org.eclipse.emf.ecore.EObject;

public class JackObjectUtil {

    
    public static Jack getRoot(EObject obj) {
        EObject root = obj;
        while (!(root instanceof Jack)) {
            root = root.eContainer();
        }
        return (Jack)root;
    }
}
