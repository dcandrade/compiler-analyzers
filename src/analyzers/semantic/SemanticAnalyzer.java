package analyzers.semantic;

import model.semantic.SymbleTable;
import model.semantic.Variable;
import model.token.Token;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SemanticAnalyzer {

    private final List<Token> tokens;
    private SymbleTable symbleTable;
    private int tokenIndex;

    private Token currentToken, lastToken;
    private Variable currentVariable;
    private String currentType;

    private final List<String> nativeTypes;


    public SemanticAnalyzer(List<Token> tokens) {
        currentVariable = new Variable();
        symbleTable = new SymbleTable();
        this.tokens = tokens;
        this.tokenIndex = 0;
        this.updateToken();
        this.nativeTypes = Arrays.asList("int", "float", "string", "bool", "void");
        analyzer();
    }

    private void updateToken() throws IndexOutOfBoundsException {
        if (this.tokenIndex < this.tokens.size()) {
            lastToken = currentToken;
            this.currentToken = this.tokens.get(this.tokenIndex);
            this.tokenIndex++;
        } else {
            throw new IndexOutOfBoundsException("No more tokens");
        }
    }

    public void analyzer() {
        Const();
    }

    public void Const() {
        if(currentToken.getValue().equals("const")) {
            this.updateToken();
            checkToken("{");
            analyzerConst();
        }
    }

    public boolean analyzerConst() {
        currentVariable.setConst(true);
        checkType();
        checkDeclaration();
        if(checkToken(";")) {
            if(checkToken("}")) {
                System.out.println("Finishe Const");
                return true;
            } else {
                analyzerConst();
            }
        } else if(checkToken(",")) {
            checkDeclaration();
        }
        return false;
    }

    public boolean checkToken(String type) {
        if(currentToken.getValue().equals(type) || currentToken.getType().equals(type)) {
            System.out.println(currentToken.getValue());
            this.updateToken();
            return true;
        }
        return false;
    }

    public boolean checkType() {
        if(nativeTypes.contains(currentToken.getValue())) {
            currentType = currentToken.getValue();
            System.out.println(currentToken.getValue());
            this.updateToken();
            return true;
        }
        return false;
    }

    public boolean checkDeclaration() {
        if(checkToken("IDE")) {
            currentVariable.setName(lastToken.getValue());
            checkToken("REL");
            checkAssignment();
            return true;
        }
        return true;
    }

    public boolean checkAssignment() {

        if(currentType.equals("string")) {

            if(currentToken.getType().equals("CDC")){
                currentVariable.setType(currentToken.getValue());
                System.out.println(currentToken.getValue());
                this.updateToken();
            } else {
                System.err.println("Erro");
            }
        } else if (currentType.equals("boolean")){

            if(currentToken.getValue().equals("true") || currentToken.getValue().equals("false")) {
                currentVariable.setType(currentToken.getValue());
                System.out.println(currentToken.getValue());
                this.updateToken();
            } else {
                System.err.println("Erro");
            }
        } else if (currentType.equals("int") || currentType.equals("float")) {

            if(currentToken.getType().equals("NRO")) {
                currentVariable.setType(currentToken.getValue());
                System.out.println(currentToken.getValue());
                this.updateToken();
            } else {
                System.err.println("Erro");
            }
        }
        symbleTable.addConst(currentVariable);
        return true;
    }
}