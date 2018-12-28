package model.semantic;

import model.semantic.entries.ClassEntry;
import model.semantic.entries.VariableEntry;
import model.token.TokenTypes;

import javax.management.InstanceAlreadyExistsException;
import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    private Map<String, ClassEntry> classes;
    private Map<String, VariableEntry> constants;


    public SymbolTable() {
        this.constants = new HashMap<>();
        this.classes = new HashMap<>();
    }

    public VariableEntry getConst(String varName) {
        return this.constants.get(varName);
    }


    public ClassEntry getClass(String className) throws ClassNotFoundException {
        if (className == null) {
            return null;
        }

        ClassEntry classEntry = this.classes.get(className);

        if (classEntry == null) {
            throw new ClassNotFoundException();
        }

        return classEntry;
    }

    public ClassEntry addClass(String className, ClassEntry superClass) throws InstanceAlreadyExistsException {
        if (this.classes.get(className) != null) {
            throw new InstanceAlreadyExistsException("Classe já existe");
        }

        ClassEntry classEntry = new ClassEntry(className, superClass);
        this.classes.put(className, classEntry);

        return classEntry;
    }

    public Map<String, VariableEntry> getConstContext() {
        return this.constants;
    }

    public Map<String, ClassEntry> getClasses() {
        return classes;
    }

    public Map<String, VariableEntry> getFullVariablesContext() {
        Map<String, VariableEntry> context = new HashMap<>(this.constants);

        for (Map.Entry<String, ClassEntry> ce : classes.entrySet()) {
            context.putAll(ce.getValue().getVariables());
        }

        return context;
    }


    public boolean isValidType(String type) {
        return TokenTypes.encodedNativeTypes.contains(type) || this.classes.get(type) != null;
    }
}