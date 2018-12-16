package model.semantic;

public class Variable {
    private String name;
    private String type;
    private boolean isConst;

    public Variable() {

    }

    public void setConst(boolean isConst) {
        this.isConst = isConst;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean getConst() {
        return isConst;
    }

    public String getName() {
        return this.name;
    }

    public String getType() {
        return this.type;
    }
}
