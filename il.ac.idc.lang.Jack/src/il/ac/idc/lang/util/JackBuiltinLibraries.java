package il.ac.idc.lang.util;

import java.util.HashSet;
import java.util.Set;

public class JackBuiltinLibraries {

    public static Set<JackFunction> functions;
    public static Set<String> libraries;
    static {
        functions = new HashSet<JackFunction>();
                
        functions.add(new JackFunction("Math", "abs", 1, null));
        functions.add(new JackFunction("Math", "multiply", 2, null));
        functions.add(new JackFunction("Math", "divide", 2, null));
        functions.add(new JackFunction("Math", "min", 2, null));
        functions.add(new JackFunction("Math", "max", 2, null));
        functions.add(new JackFunction("Math", "sqrt", 1, null));
        
        functions.add(new JackFunction("String", "new", 1, null));
        functions.add(new JackFunction("String", "dispose", 0, null));
        functions.add(new JackFunction("String", "length", 0, null));
        functions.add(new JackFunction("String", "charAt", 1, null));
        functions.add(new JackFunction("String", "setCharAt", 2, null));
        functions.add(new JackFunction("String", "appendChar", 1, null));
        functions.add(new JackFunction("String", "eraseLastChar", 1, null));
        functions.add(new JackFunction("String", "intValue", 0, null));
        functions.add(new JackFunction("String", "setInt", 1, null));
        functions.add(new JackFunction("String", "backSpace", 0, null));
        functions.add(new JackFunction("String", "doubleQuote", 0, null));
        functions.add(new JackFunction("String", "newLine", 0, null));
        
        functions.add(new JackFunction("Array", "new", 1, null));
        functions.add(new JackFunction("Array", "dispose", 0, null));
        
        functions.add(new JackFunction("Output", "moveCursor", 2, null));
        functions.add(new JackFunction("Output", "printChar", 1, null));
        functions.add(new JackFunction("Output", "printString", 1, null));
        functions.add(new JackFunction("Output", "printInt", 1, null));
        functions.add(new JackFunction("Output", "println", 1, null));
        functions.add(new JackFunction("Output", "backspace", 1, null));
        
        functions.add(new JackFunction("Screen", "clearScreen", 0, null));
        functions.add(new JackFunction("Screen", "setColor", 1, null));
        functions.add(new JackFunction("Screen", "drawPixel", 2, null));
        functions.add(new JackFunction("Screen", "drawLine", 4, null));
        functions.add(new JackFunction("Screen", "drawRectangle", 4, null));
        functions.add(new JackFunction("Screen", "drawCircle", 2, null));
        
        functions.add(new JackFunction("Keyboard", "keyPressed", 0, null));
        functions.add(new JackFunction("Keyboard", "readChar", 0, null));
        functions.add(new JackFunction("Keyboard", "readLine", 1, null));
        functions.add(new JackFunction("Keyboard", "readInt", 1, null));
        
        functions.add(new JackFunction("Memory", "peek", 1, null));
        functions.add(new JackFunction("Memory", "poke", 2, null));
        functions.add(new JackFunction("Memory", "alloc", 1, null));
        functions.add(new JackFunction("Memory", "deAlloc", 1, null));
        
        functions.add(new JackFunction("Sys", "halt", 0, null));
        functions.add(new JackFunction("Sys", "error", 1, null));
        functions.add(new JackFunction("Sys", "wait", 1, null));
        
        libraries = new HashSet<String>();
        libraries.add("Math");
        libraries.add("Array");
        libraries.add("Sys");
        libraries.add("Memory");
        libraries.add("String");
        libraries.add("Screen");
        libraries.add("Keyboard");
    }
    
    public static JackFunction findFunction(String library, String name) {
        for (JackFunction func : functions) {
            if (func.getName().equals(name) && func.getLibrary().equals(library)) {
                return func;
            }
        }
        return null;
    }
    
    public static JackFunction findFunction(String name) {
        for (JackFunction func : functions) {
            if (func.getName().equals(name)) {
                return func;
            }
        }
        return null;
    }
    
    public static boolean isFunction(String name) {
        return (findFunction(name) != null);
    }
    
    public static boolean isFunction(String library, String name) {
        return findFunction(library, name) != null;
    }
    
    public static boolean isLibrary(String lib) {
        return libraries.contains(lib);
    }
    
    public static Set<JackFunction> findFunctionPrefix(String lib, String name) {
        Set<JackFunction> funcs = new HashSet<JackFunction>();
        for (JackFunction func : functions) {
            if (func.getLibrary().equals(lib) && func.getName().startsWith(name)) {
                funcs.add(func);
            }
        }
        return funcs;
    }
    
    public static Set<JackFunction> findFunctionPrefix(String name) {
        Set<JackFunction> funcs = new HashSet<JackFunction>();
        for (JackFunction func : functions) {
            if (func.getName().startsWith(name)) {
                funcs.add(func);
            }
        }
        
        return funcs;
    }
}
