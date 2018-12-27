package model.semantic.entries;

import java.util.List;
import java.util.Map;

public class MethodEntry {
    private String name;
    private String returnType;
    private Map<String, VariableEntry> params;

    public MethodEntry(String name, String returnType, Map<String, VariableEntry> params) {
        this.name = name;
        this.returnType = returnType;
        this.params = params;
    }

    public Map<String, VariableEntry> getParams() {
        return params;
    }

    public String getName() {
        return name;
    }
}
