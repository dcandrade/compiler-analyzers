package model.error;


public class SemanticError extends Error {
    private final String expected;

    public SemanticError(int line, String token, String expected, String type) {
        super(line, token);
        this.type = type;
        this.expected = expected;
    }


    @Override
    public String toString() {
        if (!this.getToken().isEmpty() && !this.expected.isEmpty())
            return String.format("%2d %s (recebido: %s, esperado: %s)", this.getLine(), this.getType(), this.getToken(), this.expected);
        else
            return String.format("%2d %s ", this.getLine(), this.type);

    }
}
