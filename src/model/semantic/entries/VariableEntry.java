package model.semantic.entries;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class VariableEntry {
    private final List<Integer> dimensions;
    private String name;
    private String type;
    private boolean constant;
    private String dimensionString;


    public VariableEntry(String name, String type) {
        this.name = name;
        this.type = type;
        this.constant = false;
        this.dimensions = null;
    }

    public VariableEntry(String name, String type, boolean isConst) {
        this.name = name;
        this.type = type;
        this.constant = isConst;
        this.dimensions = null;
    }


    public VariableEntry(String name, String type, boolean isConst, String dimensions) {
        this.name = name;
        this.type = type;
        this.constant = isConst;
        this.dimensionString = dimensions;
        this.dimensions =  Arrays.stream(dimensions.replace("[", " ")
                .replace("]", " ")
                .split(" "))
                .filter(s-> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    public List<Integer> getDimensions() {
        return dimensions;
    }

    public boolean isVector(){
        return this.dimensions != null;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isConst() {
        return constant;
    }

    public void setConst(boolean constant) {
        this.constant = constant;
    }

    @Override
    public String toString() {
        String prefix;
        if (constant) prefix = "const ";
        else prefix = "variable ";

        if(this.isVector())
            prefix += "vector " + this.dimensionString + " ";

        return String.format("%s: %s, type: %s", prefix, this.getName(), this.getType());
    }

}
