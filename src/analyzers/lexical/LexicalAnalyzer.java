package analyzers.lexical;

import model.error.Error;
import model.token.LexemeClassifier;
import model.token.Token;
import model.token.TokenTypes;

import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;


public class LexicalAnalyzer {
    private final LexemeClassifier lexemeClassifier;
    private final List<Token> tokens;
    private final List<Error> errors;
    private final StringBuilder buffer;
    private final StringBuilder errorBuffer;
    private final String delimiters;
    private int currentLineNumber;
    private boolean isComment;

    public LexicalAnalyzer() {
        this.lexemeClassifier = new LexemeClassifier();
        this.tokens = new LinkedList<>();
        this.buffer = new StringBuilder();
        this.currentLineNumber = 0;
        this.errorBuffer = new StringBuilder();
        this.errors = new LinkedList<>();
        this.isComment = false;
        this.delimiters = LexemeClassifier.getAllCompilerDemiliters();
    }

    public void processLine(String line) {
        this.currentLineNumber++;
        line = line.replaceAll(LexemeClassifier.LINE_COMMENT_REGEX, ""); // Erase line comments
        StringTokenizer tokenizer = new StringTokenizer(line, this.delimiters, true);

        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken().trim();

            String currentBufferToken = this.buffer.toString();
            String nextBufferType = this.lexemeClassifier.classify(currentBufferToken + token).orElse("");
            String currentBufferType = this.lexemeClassifier.classify(currentBufferToken).orElse("");
            boolean isSpace = lexemeClassifier.checkTokenType(token, TokenTypes.SPACE);

            if (this.isCommentSectionOpen(currentBufferToken, nextBufferType)) {
                this.errorBuffer.append(token).append(" ");
                continue;
            }


            if (isSpace && this.errorBuffer.length() > 0) {
                String errorToken = this.errorBuffer.toString();
                this.errorBuffer.delete(0, this.errorBuffer.length());
                this.errors.add(new Error(this.currentLineNumber, errorToken));
            }


            if (nextBufferType.isEmpty() && currentBufferType.equals(TokenTypes.NUMBER) && token.equals(".")) {
                this.buffer.append(token);
            } else if (isSpace || nextBufferType.isEmpty()) {
                this.buffer.delete(0, this.buffer.length());
                this.buffer.append(token);

                if (currentBufferType.isEmpty()) {
                    this.errorBuffer.append(currentBufferToken);
                } else if (!currentBufferType.equals(TokenTypes.SPACE)) {
                    Token tkn = new Token(currentBufferType, currentBufferToken, this.currentLineNumber);
                    this.tokens.add(tkn);
                }

            } else {
                this.buffer.append(token);
            }

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
        if (this.errorBuffer.length() > 0) {
            String errorToken = this.errorBuffer.toString();
            this.errorBuffer.delete(0, this.errorBuffer.length());
            this.errors.add(new Error(this.currentLineNumber, errorToken));
        }
        return this.errors;
    }
}