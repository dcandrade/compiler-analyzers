package model.error;

public class Error {
    private final int line;
    private final String token;
    protected String type;

    public Error(int line, String token) {
        this.line = line;
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public String getType() {
        return type;
    }

    public int getLine() {
        return line;
    }

    @Override
    public String toString() {
        return String.format("%2d %s %s", this.line, this.type, this.token);
    }
}
