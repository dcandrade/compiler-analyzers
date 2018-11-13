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

    public void updateToken() throws Exception {
        if (tokens.hasNext()) {
            this.currentToken = this.tokens.next();
        } else {
            throw new Exception("No more tokens");
        }
    }

    public boolean checkForTerminal(String terminal) {
        return currentToken.getValue().equals(terminal);
    }

    public boolean checkForType(String type) {
        return currentToken.getType().equals(type);
    }

    public void eatTerminal(String terminal) throws Exception {
        if (!currentToken.getValue().equals(terminal)) {
            throw new Exception("Expected " + terminal + " got " + currentToken.getValue());
        }
        updateToken();
    }

    public void eatType(String type) throws Exception {
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
            updateToken();
            eatType(TokenTypes.IDENTIFIER);
            parseExtends();
            eatTerminal("{");
            parseVariables();
            parseMethods();
            eatTerminal("}");
        }

        if (checkForTerminal("class")) {
            parseClasses();
        }
    }

    private void parseMethods() throws Exception {
        if (checkForTerminal("method")) {
            updateToken();
            parseType();
            eatTerminal("(");
            parseParams();
            eatTerminal(")");
            parseFunctionBody();
        }

        updateToken();

        if (checkForTerminal("method")) {
            updateToken();
            parseClasses();
        }
    }

    private void parseFunctionBody() throws Exception {
        eatTerminal("{");
        parseVariables();
        parseStatements();
        parseReturn();
        eatTerminal(";");
        eatTerminal("}");
    }

    private void parseReturn() throws Exception {
        if (checkForTerminal("return")) {
            updateToken();
            parseExpression();
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

    public void parseGeneralIdentifierList() throws Exception {
        parseGeneralIdentifier();
        parseOptionalExtraIds();

    }

    private void parseOptionalExtraIds() throws Exception {
        if (checkForTerminal(",")) {
            parseGeneralIdentifierList();
        } else if (true) { // TODO: replace by peek first
            parseGeneralIdentifier();
        }
    }

    public void parseGeneralIdentifier() throws Exception {
        parseOptVector();
        parseComposedIdentifier();
    }

    private boolean parseComposedIdentifier() throws Exception {
        if (checkForTerminal(".")) {
            parseGeneralIdentifier();
            return true;
        }
        return false;
    }

    public void parseOptVector() throws Exception {
        eatType(TokenTypes.IDENTIFIER);
        parseVectorIndex();
    }

    private void parseVectorIndex() throws Exception {
        if (checkForTerminal("[")) {
            eatTerminal("[");
            parseExpression();
            eatTerminal("]");

            if (checkForTerminal("["))
                parseVectorIndex();
        }

    }

    private void parseElse() throws Exception {
        if (checkForTerminal("else")) {
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

    private void parseExtends() throws Exception {
        if (currentToken.getValue().equals("extends")) {
            updateToken();
            eatType(TokenTypes.IDENTIFIER);
        }
    }


    private void parseMain() throws Exception {
        eatTerminal("main");
        eatTerminal("{");
        parseVariables();
        parseStatements();
        eatTerminal("}");
    }
}
