package analyzers.lexical;

import exceptions.TokenClassificationException;
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

        String delimiters = LexemeClassifier.getAllCompilerDemiliters();
        StringTokenizer tokenizer = new StringTokenizer(line, delimiters, true);

        if (!tokenizer.hasMoreTokens()) { // empty line
            return;
        }

        String token = "";
        String peek = tokenizer.nextToken().trim();
        Optional<String> tokenType, peekType = this.lexemeClassifier.classify(peek);

        while (!token.isEmpty() || tokenizer.hasMoreTokens()) {
            token = peek;
            tokenType = peekType;

            if (tokenizer.hasMoreTokens()) {
                peek = tokenizer.nextToken().trim();
                peekType = this.lexemeClassifier.checkForPrimitiveTypes(peek);
            } else {
                peek = "";
                peekType = Optional.empty();
            }


            String currentBufferToken = this.buffer.toString();
            Optional<String> nextBufferType = this.lexemeClassifier.classify(currentBufferToken + token);

            boolean isSpace = lexemeClassifier.checkTokenType(token, LexemeClassifier.SPACE);

            if (isSpace || !nextBufferType.isPresent()) {
                this.buffer.delete(0, this.buffer.length());
                this.buffer.append(token);

                String currentBufferType = this.lexemeClassifier.classify(currentBufferToken).orElse("");

                if(!currentBufferType.equals(LexemeClassifier.SPACE)) {
                    Token tkn = new Token(currentBufferType, currentBufferToken, this.currentLineNumber);
                    this.tokens.add(tkn);
                }

            } else {
                this.buffer.append(token);
            }

        }
    }

    public List<Token> getTokens() {
        return this.tokens;
    }

    //TODO: errors


}