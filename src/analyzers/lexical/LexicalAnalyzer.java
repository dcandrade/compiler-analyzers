package analyzers.lexical;

import model.error.Error;
import model.token.LexemeClassifier;
import model.token.Token;
import model.token.TokenTypes;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;

public class LexicalAnalyzer {
    private final LexemeClassifier lexemeClassifier;
    private final List<Token> tokens;
    private final List<Error> errors;
    private StringBuilder buffer;
    private final StringBuilder errorBuffer;
    private final String delimiters;
    private int currentLineNumber;
    private boolean isComment;

    public LexicalAnalyzer() {
        this.lexemeClassifier = new LexemeClassifier();
        this.tokens = new LinkedList<>();
        this.currentLineNumber = 0;
        this.errorBuffer = new StringBuilder();
        this.errors = new LinkedList<>();
        this.isComment = false;
        this.delimiters = LexemeClassifier.getAllCompilerDemiliters();
    }

    private Optional<String> getLastInsertedTokenType() {
        if (!this.tokens.isEmpty()) {
            Token lastInsertedToken = this.tokens.get(this.tokens.size() - 1);
            return Optional.of(lastInsertedToken.getType());
        }

        return Optional.empty();
    }


    public void processLine(String line) {
        this.buffer = new StringBuilder();
        this.currentLineNumber++;

        line = line.replaceAll(LexemeClassifier.LINE_COMMENT_REGEX, ""); // Delete line comments
        StringTokenizer tokenizer = new StringTokenizer(line, this.delimiters, true); //get the tolkens

        while (tokenizer.hasMoreTokens()) { //check all tokens

            String token = tokenizer.nextToken();
            String currentBufferToken = this.buffer.toString();

            String nextBufferToken = currentBufferToken + token;
            String nextBufferType = this.lexemeClassifier.classify(nextBufferToken);
            String currentBufferType = this.lexemeClassifier.classify(currentBufferToken);

            boolean isSpace = this.lexemeClassifier.checkTokenType(token, TokenTypes.SPACE);

            if (this.isCommentSectionOpen(currentBufferToken, nextBufferType)) {
                this.errorBuffer.append(token);
                continue;
            }

            boolean isMaxMatch = nextBufferType.equals(TokenTypes.INVALID_TOKEN) && !currentBufferToken.isEmpty();

            if (isMaxMatch) {
                char firstBufferSymbol = this.buffer.charAt(0);
                char lastBufferSymbol = this.buffer.charAt(this.buffer.length() - 1);
                boolean bufferIsNumber = currentBufferType.equals(TokenTypes.NUMBER);

                if (bufferIsNumber && token.equals(".")) {
                    this.buffer.append(token);
                    continue;

                } else if (firstBufferSymbol == '-' && isSpace) { // spaces after -. wait for digits
                    continue;

                } else if (firstBufferSymbol == '-' && bufferIsNumber) {
                    Optional<String> lastInsertedTokenType = this.getLastInsertedTokenType();

                    if (lastInsertedTokenType.isPresent() && lastInsertedTokenType.get().equals(TokenTypes.NUMBER)) {
                        String number = currentBufferToken.substring(1);

                        this.validateBufferToken(String.valueOf(firstBufferSymbol), TokenTypes.ARITHMETICAL_OPERATOR);
                        this.validateBufferToken(number, TokenTypes.NUMBER);
                        continue;
                    }
                } else if (firstBufferSymbol == '"') { // String received
                    if (lastBufferSymbol != '"' || this.buffer.length() == 1) {
                        this.buffer.append(token);

                        while (this.buffer.charAt(this.buffer.length() - 1) != '"' && tokenizer.hasMoreTokens()) {
                            this.buffer.append(tokenizer.nextToken());
                        }

                        currentBufferToken = this.buffer.toString();
                        currentBufferType = TokenTypes.INVALID_TOKEN;

                        if (currentBufferToken.endsWith("\"")) {
                            currentBufferType = TokenTypes.STRING;
                        }

                        this.validateBufferToken(currentBufferToken, currentBufferType);
                        continue;
                    }
                }

                this.validateBufferToken(currentBufferToken, currentBufferType, token);
            } else {
                this.buffer.append(token);
            }
        }

        String currentBufferToken = this.buffer.toString();
        String currentBufferType = this.lexemeClassifier.classify(currentBufferToken);

        this.validateBufferToken(currentBufferToken, currentBufferType);
    }

    private void validateBufferToken(String token, String tokenType, String nextToken) {
        this.validateBufferToken(token, tokenType);
        // restart buffer with given input
        this.buffer.append(nextToken);
    }


    private void validateBufferToken(String token, String tokenType) {
        if (tokenType.equals(TokenTypes.INVALID_TOKEN)) {
            this.errors.add(new Error(this.currentLineNumber, token));
        } else if (!tokenType.equals(TokenTypes.SPACE)) {
            Token tkn = new Token(tokenType, token, this.currentLineNumber);
            this.tokens.add(tkn);
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
        return this.tokens;
    }


    public List<Error> getErrors() {
        this.checkForErrors();
        return this.errors;
    }
}