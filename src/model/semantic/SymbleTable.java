package model.semantic;

import java.util.ArrayList;
import java.util.List;

public class SymbleTable {
    private List<Class> classes;
    private  List<Variable> constantes;

    public  SymbleTable() {
        this.constantes = new ArrayList<Variable>();
    }

    public void addConst(Variable constante) {
        constantes.add(constante);
    }
}
