package il.ac.idc.lang.util;

public class JackFunction {

    private String library;
    private String name;
    private int params;
    private String doc;
    
    private static final String DEFAULT_DOC = "Documentation is currently not available for this function";
    
    public JackFunction(String name, int params, String doc) {
        this.name = name;
        this.params = params;
        if (doc == null) {
            this.doc = DEFAULT_DOC;
        } else {
            this.doc = doc;
        }
    }
    
    public JackFunction(String library, String name, int params, String doc) {
        this(name, params, doc);
        this.library = library;
    }
    
    public String getLibrary() {
        return library;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDoc() {
        return doc;
    }
    
    public int getParams() {
        return params;
    }
    
    public String toString() {
        StringBuilder buff = new StringBuilder();
        if (library != null) {
            buff.append(library + ".");
        }
        buff.append(name + "(");
        if (params > 0) {
            buff.append("param1");
        }
        for(int i = 1; i < params; i++) {
            buff.append(", param" + (i + 1));
        }
        buff.append(")");
        return buff.toString();
    }
    
    public String getSignature() {
        return toString();
    }
}
