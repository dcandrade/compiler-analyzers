package model.semantic.entries;

public class VariableEntry {
    private String name;
    private String type;
    private boolean constant;


    public VariableEntry(String name, String type){
        this.name = name;
        this.type = type;
        this.constant = false;
    }

    public VariableEntry(String name, String type, boolean isConst){
        this.name = name;
        this.type = type;
        this.constant = isConst;
    }

    public void setConst(boolean constant) {
        this.constant = constant;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return this.name;
    }

    public String getType() {
        return this.type;
    }

    public boolean isConst() {
        return constant;
    }
}
