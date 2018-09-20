package model.token;

public class Token {
    private final String name;
    private final String value;
    private final int line;

    public Token(String name, String value, int line) {
        this.name = name;
        this.value = value;
        this.line = line;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public int getLine() {
        return line;
    }

    @Override
    public String toString() {
        return String.format("%2d %s %s", this.getLine(), this.getName(), this.getValue());
    }
}
