package exceptions;

public class TokenClassificationException extends Exception {
    public TokenClassificationException(){
        super("Unable to classify token");
    }
}
