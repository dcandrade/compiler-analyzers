package analyzers.lexical;

import model.error.LexicalError;
import model.token.LexemeClassifier;
import model.token.Token;
import model.token.TokenTypes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class LexicalAnalyzer {
    private final LexemeClassifier lexemeClassifier;
    private final List<LexicalError> lexicalErrors;
    private final StringBuilder errorBuffer;
    private final String delimiters;
    private StringBuilder buffer;
    private int currentLineNumber;
    private boolean isComment;
    private Path inputFilePath;
    private List<Token> tokens;

    public LexicalAnalyzer(Path inputFilePath) {
        this.lexemeClassifier = new LexemeClassifier();
        this.currentLineNumber = 0;
        this.errorBuffer = new StringBuilder();
        this.lexicalErrors = new LinkedList<>();
        this.isComment = false;
        this.delimiters = LexemeClassifier.getAllCompilerDemiliters();
        this.inputFilePath = inputFilePath;
        this.tokens = Collections.emptyList();
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
        line = line.trim();
        this.buffer = new StringBuilder();
        this.currentLineNumber++;

        line = line.replaceAll(LexemeClassifier.LINE_COMMENT_REGEX, ""); //Deletar todos os comentários de linha

        //Armazenar todos os tokens separados pelos delimitadores
        StringTokenizer lexemeTokenizer = new StringTokenizer(line, this.delimiters, true);

        //Iniciar a varredura dos tokens para classificação
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

            //Verifica ser o token é um máximo match
            boolean isMaxMatch = nextBufferType.equals(TokenTypes.INVALID_TOKEN) && !currentBufferLexeme.isEmpty();

            if (isMaxMatch) {
                // Se for exceção ao máximo match passa para o próximo token
                if (isMaxMatchException(tokens, lexemeTokenizer, lexeme, currentBufferLexeme, currentBufferType, isSpace))
                    continue;

                this.validateBufferLexeme(currentBufferLexeme, currentBufferType, tokens);
            }
            this.buffer.append(lexeme);
        }

        String currentBufferToken = this.buffer.toString();
        String currentBufferType = this.lexemeClassifier.classify(currentBufferToken);
        //System.out.println(currentLineNumber + " " + currentBufferToken);
        this.validateBufferLexeme(currentBufferToken, currentBufferType, tokens);

        return tokens;
    }

    private boolean isMaxMatchException(List<Token> tokens, StringTokenizer lexemeTokenizer, String lexeme, String currentBufferLexeme, String currentBufferType, boolean isSpace) {
        char firstBufferSymbol = this.buffer.charAt(0);
        char lastBufferSymbol = this.buffer.charAt(this.buffer.length() - 1);
        boolean bufferIsNumber = currentBufferType.equals(TokenTypes.NUMBER);

        //Verificação de números
        boolean incomingFPNumber = bufferIsNumber && lexeme.equals(".");
        if (incomingFPNumber) {
            this.buffer.append(lexeme);
            return true;
        }

        boolean negativeNumberCandidate = firstBufferSymbol == '-' && isSpace;
        if (negativeNumberCandidate) { // spaces after -. wait for digits
            return true;
        }

        //Verifica se um sinal negativo é uma expressão aritmética ou pertence a um número negativo
        boolean subtractionExpression = firstBufferSymbol == '-' && bufferIsNumber && this.getLastInsertedTokenType(tokens).equals(TokenTypes.NUMBER);
        if (subtractionExpression) {
            this.expandSubtraction(currentBufferLexeme, firstBufferSymbol, tokens);
            this.buffer.append(lexeme);
            return true;
        }

        //Verificação de String incompleta
        boolean incomingString = firstBufferSymbol == '"' && (lastBufferSymbol != '"' || this.buffer.length() == 1);
        if (incomingString) { // String received
            this.processIncomingString(lexemeTokenizer, lexeme, tokens);
            return true;

        }
        return false;
    }

    private void expandSubtraction(String currentBufferLexeme, char firstBufferSymbol, List<Token> tokens) {

        String number = currentBufferLexeme.substring(1);

        this.validateBufferLexeme(String.valueOf(firstBufferSymbol), TokenTypes.ARITHMETICAL_OPERATOR, tokens);
        this.validateBufferLexeme(number, TokenTypes.NUMBER, tokens);

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

    //Validação de um um token
    private void validateBufferLexeme(String token, String tokenType, List<Token> tokens) {
        if (tokenType.equals(TokenTypes.INVALID_TOKEN)) {
            this.lexicalErrors.add(new LexicalError(this.currentLineNumber, token));
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
            this.lexicalErrors.add(new LexicalError(this.currentLineNumber, errorToken));
        }
    }

    //Verificação de comentários de bloco
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

    public List<Token> getTokens() throws IOException {
        if (this.tokens.size() == 0) {
            this.tokens = Files.lines(this.inputFilePath)
                    .map(this::processLine)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
            this.tokens.add(new Token(TokenTypes.NO_MORE_TOKENS, "$", -1));
        }

        return this.tokens;
    }


    public List<LexicalError> getLexicalErrors() {
        this.checkForErrors();
        return this.lexicalErrors;
    }

    // Para cada linha do arquivo de entrada, extrai os tokens, acumula e retorna um iterador.
    // As linhas são processadas por demanda, ou seja, sṍ são processadas quando um token novo é solicitado.

}