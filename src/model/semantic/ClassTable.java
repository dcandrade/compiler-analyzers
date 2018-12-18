package model.semantic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ClassTable {
    private String name;
    private List<Variable> variables;

    public ClassTable() {
        variables = new ArrayList<>();
    }

    public String getName() {
        return this.name;
    }

    public void addVariableList(List<Variable> variables) {
        this.variables = variables;
    }

    public boolean checkVariableType(String variable, String type) {
        Variable variable1Aux = new Variable();
        variable1Aux.setName(variable);
        Iterator<Variable> i = variables.iterator();

        while (i.hasNext()) {
            Variable aux =  i.next();

            if(aux.getName().equals(variable)) {
                if(aux.getType().equals(type)){
                    return true;
                }
            }
        }
        return false;
    }
}
