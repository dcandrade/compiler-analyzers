package analyzers.syntatical;

import model.token.Token;
import model.token.TokenTypes;

import java.util.Iterator;
import java.util.List;


public class SyntaticalAnalyzer {

    private final Iterator<Token> tokens;
    private final List<String> nativeTypes;
    private Token currentToken = null;

    public SyntaticalAnalyzer(Iterator<Token> tokens) {
        this.tokens = tokens;
        this.nativeTypes = List.of("int", "float", "string", "bool");
    }

    private void updateToken() throws Exception {
        if (tokens.hasNext()) {
            this.currentToken = this.tokens.next();
        } else {
            throw new Exception("No more tokens");
        }
    }

    private boolean checkForTerminal(String terminal) {
        return currentToken.getValue().equals(terminal);
    }

    private boolean checkForType(String type) {
        return currentToken.getType().equals(type);
    }

    private void eatTerminal(String terminal) throws Exception {
        if (!currentToken.getValue().equals(terminal)) {
            throw new Exception("Expected " + terminal + " got " + currentToken.getValue());
        }
        updateToken();
    }

    private void eatType(String type) throws Exception {
        if (!currentToken.getType().equals(type)) {
            throw new Exception("Expected type " + type + " got " + currentToken.getType());
        }
        updateToken();
    }

    private void parseProgram() throws Exception {
        this.currentToken = tokens.next();
        parseConsts();
        parseClasses();
        parseMain();
    }

    private void parseConsts() {

    }

    private void parseClasses() throws Exception {
        if (checkForTerminal("class")) {
            eatTerminal("class");
            eatType(TokenTypes.IDENTIFIER);
            parseExtends();
            eatTerminal("{");
            parseVariables();
            parseMethods();
            eatTerminal("}");

            parseClasses();
        }

        return;
    }

    private void parseExtends() throws Exception {
        if (currentToken.getValue().equals("extends")) {
            eatTerminal("extends");
            eatType(TokenTypes.IDENTIFIER);
        }
    }

    private void parseMethods() throws Exception {
        if (checkForTerminal("method")) {
            eatTerminal("method");
            parseType();
            eatTerminal("(");
            parseParams();
            eatTerminal(")");
            parseFunctionBody();

            parseMethods();
        }

        return;
    }

    private void parseFunctionBody() throws Exception {
        eatTerminal("{");
        parseVariables();
        parseStatements();
        parseReturn();
        eatTerminal("}");
    }

    private void parseReturn() throws Exception {
        if (checkForTerminal("return")) {
            eatTerminal("return");
            parseExpression();
            eatTerminal(";");
        }
    }

    private void parseExpression() throws Exception {
        parseAddExp();

        if (checkForType(TokenTypes.RELATIONAL_OPERATOR)) {
            parseExpression();
        }
    }

    private void parseAddExp() throws Exception {
        parseMultExp();

        if (checkForTerminal("+") || checkForTerminal("-")) {
            parseAddExp();
        }
    }

    private void parseMultExp() throws Exception {
        parseNegateExp();

        if (checkForTerminal("*") || checkForTerminal("/")) {
            parseMultExp();
        }
    }

    private void parseNegateExp() throws Exception {
        if (checkForTerminal("-")) {
            eatTerminal("-");
        }

        parseValue();
    }

    private void parseValue() {
    }

    private void parseStatements() {
    }

    private void parseRead() {
    }

    private void parseWrite() {
    }

    private void parseAssignment() throws Exception {
        parseGeneralIdentifier();
        eatTerminal("=");
        parseExpression();
    }

    private void parseWhile() throws Exception {
        eatTerminal("while");
        eatTerminal("(");
        parseExpression();
        eatTerminal(")");
        eatTerminal("{");
        parseStatements();
        eatTerminal("}");
    }

    private void parseIf() throws Exception {
        eatTerminal("if");
        eatTerminal("(");
        parseExpression();
        eatTerminal(")");
        eatTerminal("{");
        parseStatements();
        eatTerminal("}");
        parseElse();
    }

    private void parseGeneralIdentifierList() throws Exception {
        parseGeneralIdentifier();
        parseOptionalExtraIds();
    }

    private void parseOptionalExtraIds() throws Exception {
        if (checkForTerminal(",")) {
            eatTerminal(",");
            parseGeneralIdentifierList();
        } else if (true) { // TODO: replace by peek first
            parseGeneralIdentifier();
        }
    }

    private void parseGeneralIdentifier() throws Exception {
        parseOptVector();
        parseComposedIdentifier();
    }

    private void parseComposedIdentifier() throws Exception {
        if (checkForTerminal(".")) {
            parseGeneralIdentifier();
        }
    }

    private void parseOptVector() throws Exception {
        eatType(TokenTypes.IDENTIFIER);
        parseVectorIndex();
    }

    private void parseVectorIndex() throws Exception {
        if (checkForTerminal("[")) {
            eatTerminal("[");
            parseExpression();
            eatTerminal("]");

            parseVectorIndex();
        }

        return;
    }

    private void parseElse() throws Exception {
        if (checkForTerminal("else")) {
            eatTerminal("else");
            eatTerminal("{");
            parseStatements();
            eatTerminal("}");
        }
    }

    private void parseParams() {
    }

    private void parseType() throws Exception {
        for (String type : this.nativeTypes) {
            if (currentToken.getValue().equals(type)) {
                eatType(type);
                return;
            }
        }

        this.eatType(TokenTypes.IDENTIFIER);
    }

    private void parseVariables() {

    }


    private void parseMain() throws Exception {
        eatTerminal("main");
        eatTerminal("{");
        parseVariables();
        parseStatements();
        eatTerminal("}");
    }
}
