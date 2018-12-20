package model.error;

public class LexicalError extends Error {
    private final String type;

    public LexicalError(int line, String token) {
        super(line, token);
        this.type = ErrorClassifier.classify(token);
    }

    @Override
    public String toString() {
        return String.format("%2d %s %s", this.getLine(), this.getType(), this.getToken());
    }
}
