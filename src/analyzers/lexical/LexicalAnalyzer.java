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
        String peek = tokenizer.nextToken();
        Optional<String> tokenType, peekType = this.lexemeClassifier.classify(peek);

        while (!token.isEmpty() || tokenizer.hasMoreTokens()) {
            token = peek;
            tokenType = peekType;

            if (tokenizer.hasMoreTokens()) {
                peek = tokenizer.nextToken();
                peekType = this.lexemeClassifier.checkForPrimitiveTypes(peek);
            } else {
                peek = "";
                peekType = Optional.empty();
            }

            //TODO: check empty string on lexeme classifier
            if (token.isEmpty() || lexemeClassifier.checkTokenType(token, LexemeClassifier.SPACE)) {
                continue;
            }

            System.out.println(token);


            if (tokenType.isPresent()) { // is reserved word, number, operator or delimiter
                String conjugate = token + peek;
                Optional<String> conjugateType = this.lexemeClassifier.checkForPrimitiveTypes(conjugate);

                if (conjugateType.isPresent()) { // gets &&, <= etc
                    Token tkn = new Token(conjugateType.get(), conjugate, this.currentLineNumber);
                    this.tokens.add(tkn);
                    peek = "";
                    peekType = Optional.empty();
                } else {
                    Token tkn = new Token(tokenType.get(), token, this.currentLineNumber);
                    this.tokens.add(tkn);
                }
            }

            //TODO: other checks

        }
    }

    public List<Token> getTokens() {
        return this.tokens;
    }

    //TODO: errors


}