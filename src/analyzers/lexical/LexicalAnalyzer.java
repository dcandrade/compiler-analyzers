package analyzers.lexical;

import model.LexemeClassifier;
import model.Token;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class LexicalAnalyzer {
    private final LexemeClassifier lexemeClassifier;
    private final Map<Integer, String> symbolTable; // int to lexeme
    private final List<Token> tokens;
    private final StringBuilder buffer;
    private int currentLineNumber;

    public LexicalAnalyzer() {
        this.lexemeClassifier = new LexemeClassifier();
        this.symbolTable = new HashMap<>();
        this.tokens = new LinkedList<>();
        this.buffer = new StringBuilder();
        this.currentLineNumber = 0;
    }

    public void processLine(String line) {
        this.currentLineNumber++;
        line = line.replaceAll("//.*", ""); // Erase line comments
        Scanner scanner = new Scanner(line);
        scanner.useDelimiter("[ \t\n]");

        if (!scanner.hasNext()) { // empty line
            return;
        }

        String token = "";
        String peek = scanner.next();
        Optional<String> tokenType, peekType = this.lexemeClassifier.checkForPrimitiveTypes(peek);

        while (!token.isEmpty() || scanner.hasNext()) {
            token = peek;
            tokenType = peekType;

            if (scanner.hasNext()) {
                peek = scanner.next();
                peekType = this.lexemeClassifier.checkForPrimitiveTypes(peek);
            } else {
                peek = "";
                peekType = Optional.empty();
            }

            if (tokenType.isPresent()) { // is reserved word, number, operator or delimiter
                Token tkn = new Token(tokenType.get(), token, this.currentLineNumber);
                this.tokens.add(tkn);
            }

            //TODO: other checks

        }

    }

    public List<Token> getTokens() {
        return this.tokens;
    }

    //TODO: errors


}