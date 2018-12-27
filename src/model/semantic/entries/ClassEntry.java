package model.semantic.entries;

import model.token.TokenTypes;

import java.util.HashMap;
import java.util.Map;

public class ClassEntry {
    private final ClassEntry superclass;
    private String name;
    private Map<String, VariableEntry> variables;

    public ClassEntry(String name) {
        this(name, null);
    }

    public ClassEntry(String name, ClassEntry superclass) {
        this.name = name;
        this.superclass = superclass;
        this.variables = new HashMap<>();

        if(superclass != null)
            this.variables.putAll(this.superclass.variables);
    }

    public Map<String, VariableEntry> getVariables() {
        return variables;
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
