package model.semantic.entries;

import java.util.HashMap;
import java.util.Map;

public class ClassEntry {
    private final ClassEntry superclass;
    private String name;
    private Map<String, VariableEntry> variables;
    private Map<String, MethodEntry> methods;

    public ClassEntry(String name) {
        this(name, null);
    }


    public ClassEntry(String name, ClassEntry superclass) {
        this.name = name;
        this.superclass = superclass;
        this.variables = new HashMap<>();
        this.methods = new HashMap<>();

        if (superclass != null) {
            this.variables.putAll(this.superclass.variables);
            this.methods.putAll(this.superclass.methods);
        }
    }

    public Map<String, VariableEntry> getVariables() {
        return variables;
    }

    public MethodEntry addMethod(String name, String returnType, Map<String, VariableEntry> params) throws Exception {
        if (methods.get(name) != null) {
            throw new Exception("repetido");
        }

        MethodEntry methodEntry = new MethodEntry(name, returnType, params);

        return methodEntry;
    }


    public void addMethod(MethodEntry method) throws Exception {
        if (methods.get(method.getName()) != null) {
            throw new Exception();
        }

        this.methods.put(method.getName(), method);
    }

    public String getMethodType(String method) throws Exception {
        MethodEntry methodEntry = this.methods.get(method);

        if (methodEntry == null) {
            throw new Exception("Metodo inexistente");
        }

        return methodEntry.getReturnType();
    }

    private boolean hasSuperclass() {
        return this.superclass != null;
    }

    public String getName() {
        return this.name;
    }

    public VariableEntry getVariable(String varName) throws Exception {
        VariableEntry var = this.variables.get(varName);

        if (var != null) {
            return var;
        }

        throw new Exception("Variável não declarada");
    }

    public boolean hasVariable(VariableEntry var) {
        return this.hasVariable(var.getName());
    }

    public boolean hasVariable(String name) {
        return this.variables.get(name) != null;

    }

    public String getVariableType(String varName) {
        VariableEntry var = this.variables.get(varName);

        if (var != null) {
            return var.getType();
        }

        return null;
    }

    public boolean checkVariableType(String varName, String type) throws Exception {
        return this.getVariableType(varName).equals(type);
    }

}
