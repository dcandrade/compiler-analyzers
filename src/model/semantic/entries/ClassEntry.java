package model.semantic.entries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        variables = new HashMap<>();
    }

    public boolean hasSuperclass() {
        return this.superclass != null;
    }

    public String getName() {
        return this.name;
    }

    protected VariableEntry getVariable(String name) {
        return this.variables.get(name);
    }

    public String getVariableType(String varName) throws Exception {
        VariableEntry var = this.variables.get(varName);

        // Variável não encontrada na classe, procurando na superclasse
        if (var == null && this.hasSuperclass()) {
            var = this.superclass.getVariable(varName);
        }

        if (var != null) {
            return var.getType();
        }

        throw new Exception("Variável não declarada");
    }

    public boolean checkVariableType(String varName, String type) throws Exception {
        return this.getVariableType(varName).equals(type);
    }

    public void addVariableList(List<VariableEntry> varList) {
        this.variables = varList.stream()
                .collect(Collectors.toMap(VariableEntry::getName, x -> x));
    }
}
