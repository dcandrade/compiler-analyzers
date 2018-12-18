package model.semantic;

import java.util.ArrayList;
import java.util.List;

public class SymbolTable {
    private List<Class> classes;
    private  List<Variable> constants;

    public SymbolTable() {
        this.constants = new ArrayList<>();
    }

    public void addConst(Variable constante) {
        constants.add(constante);
    }
}
