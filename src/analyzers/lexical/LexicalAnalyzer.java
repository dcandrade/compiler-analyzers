package analyzers.lexical;

import model.error.Error;
import model.token.LexemeClassifier;
import model.token.Token;
import model.token.TokenTypes;

import javax.naming.OperationNotSupportedException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class LexicalAnalyzer implements Iterable<Token>{
    private final LexemeClassifier lexemeClassifier;
    private final List<Error> errors;
    private final StringBuilder buffer;
    private final StringBuilder errorBuffer;
    private final String delimiters;
    private int currentLineNumber;
    private boolean isComment;
    private final String path;

    public LexicalAnalyzer(String path) {
        this.path = path;
        this.lexemeClassifier = new LexemeClassifier();
        this.buffer = new StringBuilder();
        this.currentLineNumber = 0;
        this.errorBuffer = new StringBuilder();
        this.errors = new LinkedList<>();
        this.isComment = false;
        this.delimiters = LexemeClassifier.getAllCompilerDemiliters();
    }

    private List<Token> processLine(String line) {
        List<Token> tokens = new LinkedList<>();
        
        this.currentLineNumber++;
        line = line.replaceAll(LexemeClassifier.LINE_COMMENT_REGEX, ""); // Erase line comments
        StringTokenizer tokenizer = new StringTokenizer(line, this.delimiters, true);

        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();

            String currentBufferToken = this.buffer.toString();
            Optional<String> nextBufferType = this.lexemeClassifier.classify(currentBufferToken + token);
            Optional<String> currentBufferType = this.lexemeClassifier.classify(currentBufferToken);

            boolean isSpace = lexemeClassifier.checkTokenType(token, TokenTypes.SPACE);

            if (this.isCommentSectionOpen(currentBufferToken, nextBufferType.orElse(""))) {
                this.errorBuffer.append(token);
                continue;
            }

            if (isSpace) {
                this.checkForErrors();
            }

            boolean isMaxMatch = !nextBufferType.isPresent();

            if (isMaxMatch) {
                char firstBufferSymbol = this.buffer.charAt(0);
                char lastBufferSymbol = this.buffer.charAt(this.buffer.length() - 1);

                if (currentBufferType.isPresent() && currentBufferType.get().equals(TokenTypes.NUMBER) && token.equals(".")) {
                    this.buffer.append(token);
                    continue;
                } else if (firstBufferSymbol == '-' && isSpace) { // spaces after -. wait for digits
                    continue;
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
                    tokens.add(tkn);
                }

                // reset buffer
                this.buffer.delete(0, this.buffer.length());
                this.buffer.append(token);

            } else {
                this.buffer.append(token);
            }
        }
        
        return tokens;
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

    public List<Token> getTokens() throws IOException {
         return Files.lines(Paths.get(path))
                .map(this::processLine)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }


    public List<Error> getErrors() {
        this.checkForErrors();
        return this.errors;
    }

    @Override
    public Iterator<Token> iterator() {
        try {
            return Files.lines(Paths.get(path))
                    .map(this::processLine)
                    .flatMap(List::stream)
                    .iterator();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return (new ArrayList<Token>()).iterator();
    }


}