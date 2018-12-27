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

    public void addConst(String name, String type, int line) throws Exception {
        if (type.equals("string")) {
            type = TokenTypes.STRING;
        }
        if (this.constants.get(name) == null) {
            this.constants.put(name, new VariableEntry(name, type, true, line));
        } else {
            throw new Exception("Constante já existe");
        }
    }

    public boolean isConst(VariableEntry var) {
        return this.constants.get(var.getName()) != null;
    }

    public VariableEntry getConst(String varName){
        return this.constants.get(varName);
    }

    public void addConst(VariableEntry var) throws Exception {
        this.addConst(var.getName(), var.getType(), var.getLine());
    }

    public String getConstType(String constant) {
        VariableEntry var = this.constants.get(constant);

        if (var == null) {
            return null;
        } else return var.getType();
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

    public ClassEntry addClass(String className, String superClassName) throws ClassNotFoundException, InstanceAlreadyExistsException {
        if (this.classes.get(className) != null) {
            throw new InstanceAlreadyExistsException("Classe já existe");
        }

        ClassEntry classEntry = new ClassEntry(className, this.getClass(superClassName));
        this.classes.put(className, classEntry);

        return classEntry;
    }

    public ClassEntry checkClassVariable(String varName) {
        for (Map.Entry<String, ClassEntry> entry : classes.entrySet()) {
            ClassEntry ce = entry.getValue();

            if (ce.hasVariable(varName)) {
                return ce;
            }
        }
        return null;
    }

    public Map<String, VariableEntry> getConstContext(){
        return this.constants;
    }

    public boolean isValidType(String type){
        return TokenTypes.encodedNativeTypes.contains(type) ||  this.classes.get(type) != null;
    }
}