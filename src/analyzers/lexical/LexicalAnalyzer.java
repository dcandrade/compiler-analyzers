package analyzers.lexical;

import model.error.Error;
import model.token.LexemeClassifier;
import model.token.Token;
import model.token.TokenTypes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class LexicalAnalyzer implements Iterable<Token> {
    private final LexemeClassifier lexemeClassifier;
    private final List<Error> errors;
    private StringBuilder buffer;
    private final StringBuilder errorBuffer;
    private final String delimiters;
    private int currentLineNumber;
    private boolean isComment;
    private String inputFilePath;

    public LexicalAnalyzer(String inputFilePath) {
        this.lexemeClassifier = new LexemeClassifier();
        this.currentLineNumber = 0;
        this.errorBuffer = new StringBuilder();
        this.errors = new LinkedList<>();
        this.isComment = false;
        this.delimiters = LexemeClassifier.getAllCompilerDemiliters();
        this.inputFilePath = inputFilePath;
    }

    private String getLastInsertedTokenType(List<Token> tokens) {
        if (!tokens.isEmpty()) {
            Token lastInsertedToken = tokens.get(tokens.size() - 1);
            return lastInsertedToken.getType();
        }

        return TokenTypes.INVALID_TOKEN;
    }


    private List<Token> processLine(String line) {
        List<Token> tokens = new ArrayList<>();

        this.buffer = new StringBuilder();
        this.currentLineNumber++;

        line = line.replaceAll(LexemeClassifier.LINE_COMMENT_REGEX, ""); // Delete line comments
        StringTokenizer lexemeTokenizer = new StringTokenizer(line, this.delimiters, true); //get the tolkens

        while (lexemeTokenizer.hasMoreTokens()) { //check all tokens

            String lexeme = lexemeTokenizer.nextToken();

            String currentBufferLexeme = this.buffer.toString();
            String nextBufferLexeme = currentBufferLexeme + lexeme;

            String nextBufferType = this.lexemeClassifier.classify(nextBufferLexeme);
            String currentBufferType = this.lexemeClassifier.classify(currentBufferLexeme);

            boolean isSpace = this.lexemeClassifier.checkTokenType(lexeme, TokenTypes.SPACE);

            if (this.isCommentSectionOpen(currentBufferLexeme, nextBufferType)) {
                this.errorBuffer.append(lexeme);
                continue;
            }

            boolean isMaxMatch = nextBufferType.equals(TokenTypes.INVALID_TOKEN) && !currentBufferLexeme.isEmpty();

            if (isMaxMatch) {
                char firstBufferSymbol = this.buffer.charAt(0);
                char lastBufferSymbol = this.buffer.charAt(this.buffer.length() - 1);
                boolean bufferIsNumber = currentBufferType.equals(TokenTypes.NUMBER);


                if (bufferIsNumber && lexeme.equals(".")) {
                    this.buffer.append(lexeme);
                    continue;
                } else if (firstBufferSymbol == '-' && isSpace) { // spaces after -. wait for digits
                    continue;
                } else if (firstBufferSymbol == '-' && bufferIsNumber) {
                    if (this.checkAndExpandArithmeticalExpression(currentBufferLexeme, firstBufferSymbol, tokens))
                        continue;
                } else if (firstBufferSymbol == '"') { // String received
                    if (lastBufferSymbol != '"' || this.buffer.length() == 1) {
                        this.processIncomingString(lexemeTokenizer, lexeme, tokens);
                        continue;
                    }
                }

                this.validateBufferLexeme(currentBufferLexeme, currentBufferType, tokens);
            }
            this.buffer.append(lexeme);
        }

        String currentBufferToken = this.buffer.toString();
        String currentBufferType = this.lexemeClassifier.classify(currentBufferToken);

        this.validateBufferLexeme(currentBufferToken, currentBufferType, tokens);

        return tokens;
    }

    private boolean checkAndExpandArithmeticalExpression(String currentBufferLexeme, char firstBufferSymbol, List<Token> tokens) {
        String lastInsertedTokenType = this.getLastInsertedTokenType(tokens);

        if (lastInsertedTokenType.equals(TokenTypes.NUMBER)) {
            String number = currentBufferLexeme.substring(1);

            this.validateBufferLexeme(String.valueOf(firstBufferSymbol), TokenTypes.ARITHMETICAL_OPERATOR, tokens);
            this.validateBufferLexeme(number, TokenTypes.NUMBER, tokens);
            return true;
        }
        return false;
    }

    private void processIncomingString(StringTokenizer lexemeTokenizer, String lexeme, List<Token> tokens) {
        String currentBufferLexeme;
        String currentBufferType;

        this.buffer.append(lexeme);

        while (this.buffer.charAt(this.buffer.length() - 1) != '"' && lexemeTokenizer.hasMoreTokens()) {
            this.buffer.append(lexemeTokenizer.nextToken());
        }

        currentBufferLexeme = this.buffer.toString();
        currentBufferType = TokenTypes.INVALID_TOKEN;

        if (currentBufferLexeme.endsWith("\"")) {
            currentBufferType = TokenTypes.STRING;
        }

        this.validateBufferLexeme(currentBufferLexeme, currentBufferType, tokens);
    }


    private void validateBufferLexeme(String token, String tokenType, List<Token> tokens) {
        if (tokenType.equals(TokenTypes.INVALID_TOKEN)) {
            this.errors.add(new Error(this.currentLineNumber, token));
        } else if (!tokenType.equals(TokenTypes.SPACE)) {
            Token tkn = new Token(tokenType, token, this.currentLineNumber);
            tokens.add(tkn);
        }
        // reset buffer
        this.buffer.delete(0, this.buffer.length());

    }

    private void checkForErrors() {
        if (this.errorBuffer.length() > 0) {
            String errorToken = this.errorBuffer.toString();
            this.errorBuffer.delete(0, this.errorBuffer.length());
            this.errors.add(new Error(this.currentLineNumber, errorToken));
        }
    }

    private boolean isCommentSectionOpen(String currentBufferToken, String nextBufferType) {
        int size = errorBuffer.length();
        if (nextBufferType.equals(TokenTypes.BLOCK_COMMENT_START)) {
            this.isComment = true;
            this.errorBuffer.append(currentBufferToken);
            this.buffer.delete(0, this.buffer.length());
        } else if (size > 0 && errorBuffer.substring(size - 2, size).equals("*/")) {
            this.isComment = false;
            this.errorBuffer.delete(0, this.errorBuffer.length());
        }

        return this.isComment;
    }

    public List<Token> getTokens() {
        try {
            return Files.lines(Paths.get(this.inputFilePath))
                    .map(this::processLine)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }


    public List<Error> getErrors() {
        this.checkForErrors();
        return this.errors;
    }

    @Override
    public Iterator<Token> iterator() {

        try {
            return Files.lines(Paths.get(this.inputFilePath))
                    .map(this::processLine)
                    .flatMap(List::stream)
                    .iterator();

        } catch (IOException e) {
            return Collections.emptyIterator();
        }

    }
}