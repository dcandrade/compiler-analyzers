package model.token;

public class Token {
    private final String type;
    private final String value;
    private final int line;

    public Token(String type, String value, int line) {
        this.type = type;
        this.value = value;
        this.line = line;
    }

    public Token(String type, char value, int line) {
        this.type = type;
        this.value = String.valueOf(value);
        this.line = line;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public int getLine() {
        return line;
    }

    @Override
    public String toString() {
        return String.format("%2d %s %s", this.getLine(), this.getType(), this.getValue());
    }
}
