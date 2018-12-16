package model.semantic;

public class Variable {
    private String name;
    private String value;
    private String type;
    private boolean constant;

    public Variable() {

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

    public void setValue(String value) {
        this.value = value;
    }

    public String getName() {
        return this.name;
    }

    public String getType() {
        return this.type;
    }

    public String getValue() {
        return this.value;
    }

    public boolean isConst() {
        return constant;
    }
}
