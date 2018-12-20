package model.semantic;

import model.semantic.entries.ClassEntry;
import model.semantic.entries.VariableEntry;
import model.token.TokenTypes;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    private Map<String, ClassEntry> classes;
    private Map<String, VariableEntry> constants;

    public SymbolTable() {
        this.constants = new HashMap<>();
        this.classes = new HashMap<>();
    }

    public void addConst(String name, String type) throws Exception {
        if (type.equals("string")) {
            type = TokenTypes.STRING;
        }
        if (this.constants.get(name) == null) {
            this.constants.put(name, new VariableEntry(name, type, true));
        } else {
            throw new Exception("Constante já existe");
        }
    }

    public void addConst(VariableEntry var) throws Exception {
        this.addConst(var.getName(), var.getType());
    }

    public String getConstType(String constant) {
        VariableEntry var = this.constants.get(constant);

        if (var == null) {
            return null;
        } else return var.getType();
    }
}
