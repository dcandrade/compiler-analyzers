package model.semantic.entries;

import model.token.TokenTypes;

import java.util.Collections;
import java.util.List;

public class VariableEntry {
    private final List<Integer> dimensions;
    private int line;
    private String name;
    private String type;
    private boolean isConstant;
    private String dimensionString;
    private String value;

    public VariableEntry(String name, String type, int line) {
        this.name = name;
        this.type = type;
        this.isConstant = false;
        this.dimensions = Collections.EMPTY_LIST;
        this.line = line;
    }


    public VariableEntry(String name, String type, boolean isConst, int line) {
        this.name = name;
        this.type = type;
        this.isConstant = isConst;
        this.dimensions = Collections.EMPTY_LIST;
        this.line = line;
    }

    public VariableEntry(String name, String type, boolean isConst, List<Integer> dimensions, int line) {
        this.name = name;
        this.type = type;
        this.isConstant = isConst;
        this.dimensionString = dimensions.toString();
        this.line = line;

        this.dimensions = dimensions;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<Integer> getDimensions() {
        return dimensions;
    }

    public boolean isVector() {
        return !this.dimensions.isEmpty();
    }

    public String getName() {
        return this.name;
    }

    public int getLine() {
        return line;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isConst() {
        return isConstant;
    }

    public void setConst(boolean constant) {
        this.isConstant = constant;
    }

    public boolean isClassInstance() {
        return !TokenTypes.encodedNativeTypes.contains(this.type);
    }

    @Override
    public String toString() {
        String prefix;
        if (isConstant) prefix = "const ";
        else prefix = "variable ";

        if (this.isVector())
            prefix += "vector " + this.dimensionString + " ";

        return String.format("%s: %s, type: %s", prefix, this.getName(), this.getType());
    }

}
