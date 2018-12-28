package model.token;

public class Token {
    public final static Token EMPTY_TOKEN = new Token("", "", -1);

    private final String type;
    private final String value;
    private final int line;

    public Token(String type, String value, int line) {
        this.type = type;
        this.value = value;
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

    public boolean isIdentifier() {
        return this.type.equals(TokenTypes.IDENTIFIER);
    }

    public boolean isBolean() {
        return this.value.equals("true") || this.value.equals("false");
    }

    @Override
    public String toString() {
        return String.format("%2d %s %s", this.getLine(), this.getType(), this.getValue());
    }
}
