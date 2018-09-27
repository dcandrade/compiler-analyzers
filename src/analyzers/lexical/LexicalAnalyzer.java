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

    public void processLine(String line) {
        this.buffer = new StringBuilder();

        this.currentLineNumber++;

        line = line.replaceAll(LexemeClassifier.LINE_COMMENT_REGEX, ""); // Erase line comments
        StringTokenizer tokenizer = new StringTokenizer(line, this.delimiters, true); //get the tolkens

        while (tokenizer.hasMoreTokens()) { //check all tokens

            String token = tokenizer.nextToken();
            String currentBufferToken = this.buffer.toString();

            //System.out.println(currentLineNumber + currentBufferToken.toString());
            String nextBufferToken = currentBufferToken + token;
            Optional<String> nextBufferType = this.lexemeClassifier.classify(nextBufferToken);
            Optional<String> currentBufferType = this.lexemeClassifier.classify(currentBufferToken);

            boolean isSpace = lexemeClassifier.checkTokenType(token, TokenTypes.SPACE);

            if (this.isCommentSectionOpen(currentBufferToken, nextBufferType.orElse(""))) {
                this.errorBuffer.append(token);
                continue;
            }

            if (isSpace) {
                this.checkForErrors();
            }

            boolean isMaxMatch = !nextBufferType.isPresent() && !currentBufferToken.isEmpty();

            if (isMaxMatch) {
                char firstBufferSymbol = this.buffer.charAt(0);
                char lastBufferSymbol = this.buffer.charAt(this.buffer.length() - 1);

                if (currentBufferType.isPresent() && currentBufferType.get().equals(TokenTypes.NUMBER) && token.equals(".")) {
                    this.buffer.append(token);
                    continue;

                } else if (firstBufferSymbol == '-' && isSpace) { // spaces after -. wait for digits
                    continue;

                } else if (firstBufferSymbol == '-' && lexemeClassifier.checkTokenType(currentBufferToken, TokenTypes.NUMBER)) {
                    if (!tokens.isEmpty()) {
                        Token tokenAux = tokens.get(tokens.size() - 1);
                        boolean isNumber = lexemeClassifier.checkTokenType(tokenAux.getValue(), TokenTypes.NUMBER);

                        if(isNumber) {
                            isNumber = lexemeClassifier.checkTokenType(currentBufferToken, TokenTypes.NUMBER);
                            if (isNumber) {
                                System.out.println(currentBufferToken);

                                char token1 = currentBufferToken.charAt(0);
                                String token2 = currentBufferToken.substring(1);

                                Optional<String> token1aux = this.lexemeClassifier.classify(""+token1);
                                Token tkn = new Token(token1aux.get(), token1+"", this.currentLineNumber);
                                this.tokens.add(tkn);

                                token1aux = this.lexemeClassifier.classify(token2);
                                tkn = new Token(token1aux.get(), token2, this.currentLineNumber);
                                this.tokens.add(tkn);

                                continue;
                            }
                        }
                    }

                } else if (firstBufferSymbol == '"') { // String received
                    if (lastBufferSymbol != '"' || this.buffer.length() <= 1) {
                        this.buffer.append(token);

                        while (this.buffer.charAt(this.buffer.length() - 1) != '"' && tokenizer.hasMoreTokens()) {
                            this.buffer.append(tokenizer.nextToken());
                        }

                        if (this.buffer.charAt(this.buffer.length() - 1) == '"') {
                            currentBufferType = Optional.of(TokenTypes.STRING);
                        } else {
                            currentBufferType = Optional.empty();
                        }
                        currentBufferToken = this.buffer.toString();
                    }
                }


                if (!currentBufferType.isPresent()) {
                    this.errorBuffer.append(currentBufferToken);
                } else if (!currentBufferType.get().equals(TokenTypes.SPACE)) {
                    Token tkn = new Token(currentBufferType.get(), currentBufferToken, this.currentLineNumber);
                    this.tokens.add(tkn);
                }

                // reset buffer
                this.buffer.delete(0, this.buffer.length());
                this.buffer.append(token);

            } else {
                this.buffer.append(token);
            }
        }
    }

    private void checkForErrors() {
        if (this.errorBuffer.length() > 0) {
            String errorToken = this.errorBuffer.toString();
            this.errorBuffer.delete(0, this.errorBuffer.length());
            this.errors.add(new Error(this.currentLineNumber, errorToken));
        }
    }

    private boolean isCommentSectionOpen(String currentBufferToken, String nextBufferType) {
        if (nextBufferType.equals(TokenTypes.BLOCK_COMMENT_START)) {
            this.isComment = true;
            this.errorBuffer.append(currentBufferToken);
            this.buffer.delete(0, this.buffer.length());
        } else if (nextBufferType.equals(TokenTypes.BLOCK_COMMENT_END)) {
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