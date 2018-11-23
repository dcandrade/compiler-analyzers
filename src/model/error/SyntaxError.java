package model.error;

import model.token.Token;

public class SyntaxError extends Error {
    public final static String IDENTIFIER_MISMATCH = "id";
    public final static String TERMINAL_MISMATCH = "id";
    private final String expected;

    public SyntaxError(int line, String token, String expected, String type) {
        super(line, token);
        this.type = type;
        this.expected = expected;
    }


    @Override
    public String toString() {
        if(this.getLine() == 0){
            return this.type;
        }

        else return super.toString();
    }
}
