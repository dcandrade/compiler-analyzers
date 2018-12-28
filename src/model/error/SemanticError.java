package model.error;


import model.token.TokenTypes;

public class SemanticError extends Error {
    private final String expected;

    public SemanticError(int line, String token, String expected, String type) {
        super(line, token);
        this.type = type;
        this.expected = expected;
    }
    private String undoTranslatePRE(String PRE) {
        switch (PRE) {
            case TokenTypes.NUMBER:
                return "float";
            case TokenTypes.NUMBER_INT:
                return "int";
            case TokenTypes.NUMBER_FLOAT:
                return "float";
            case TokenTypes.STRING:
                return "string";
            default:
                return PRE;
        }
    }

    @Override
    public String toString() {
        if (!this.getToken().isEmpty() && !this.expected.isEmpty())
            return String.format("%2d %s (recebido: %s, esperado: %s)", this.getLine(), undoTranslatePRE(this.getType()), undoTranslatePRE(this.getToken()), undoTranslatePRE(this.expected));
        else
            return String.format("%2d %s ", this.getLine(), this.type);

    }
}
