package model.semantic.entries;

import model.token.TokenTypes;

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

    public String getReturnType() {
        if (this.returnType == null)
            return TokenTypes.UNDEFINED;
        return returnType;
    }
}
