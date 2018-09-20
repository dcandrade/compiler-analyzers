package model.error;

public class Error {
    private final int line;
    private final String token;
    private final String type;

    public Error(int line, String token) {
        this.line = line;
        this.token = token;
        this.type = ErrorClassifier.classify(token);
    }

    @Override
    public String toString() {
        return String.format("%2d %s %s", this.line, this.type, this.token);
    }
}
