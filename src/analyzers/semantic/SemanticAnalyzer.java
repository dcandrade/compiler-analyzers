package analyzers.semantic;

import model.semantic.SymbleTable;
import model.semantic.Variable;
import model.semantic.Class;
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
    private Class currenteClass;
    private String currentType, currentValue, getCurrentTypeMethod;
    private List<Variable> currentVariableList;
    private List<Variable> currentConstantList;
    private boolean isCost = false;

    private final List<String> nativeTypes;


    public SemanticAnalyzer(List<Token> tokens) {
        currentVariable = new Variable();
        currenteClass = new Class();
        currentVariableList = new ArrayList<Variable>();
        currentConstantList = new ArrayList<Variable>();

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
        checkConst();
        chechClass();
    }

    public boolean checkConst() {
        if(checkToken("const")) {
            isCost = true;
            if (checkToken("{")) {
                analyzerVariable();
                return true;
            }
        }
        return false;
    }

    public boolean checkVariable() {
        if (checkToken("variables")) {
            isCost = false;
            if (checkToken("{")) {
                analyzerVariable();
                return true;
            }
        }
        return false;
    }

    public boolean analyzerVariable() {
        checkDeclaration();
        return true;
    }

    public boolean checkToken(String type) {
        if(currentToken.getValue().equals(type) || currentToken.getType().equals(type)) {
            System.out.println(currentToken.getValue());
            this.updateToken();
            return true;
        }
        return false;
    }

    public boolean checkType(int hierarchy) {
        if(nativeTypes.contains(currentToken.getValue())) {
            if(hierarchy == 1) {
                currentType = currentToken.getValue();
            } else if (hierarchy == 2) {
                getCurrentTypeMethod = currentToken.getValue();
            }

            //System.out.println(currentToken.getValue());
            this.updateToken();
            return true;
        }
        return false;
    }

    public boolean checkDeclaration() {
        checkType(1);

        if(checkToken("IDE")) {
            currentVariable = new Variable();
            if(isCost) {
                currentVariable.setConst(true);
            } else {
                currentVariable.setConst(false);
            }
            currentVariable.setName(lastToken.getValue());
            currentVariable.setType(currentType);

            if (checkToken(";")) {
                if(checkToken("}")) {
                    System.out.println("Finishe");
                    return true;
                } else {
                    if (currentVariable.isConst()) {
                        //System.out.println("Constante: " + currentVariable.getName());
                    } else {
                        //System.out.println("Variavel: " + currentVariable.getName());
                        currentVariableList.add(currentVariable);
                    }
                    checkDeclaration();
                }

            } else {
                checkToken("REL");
                checkAssignment();
                if (currentVariable.isConst()) {
                    //System.out.println("Constante: " + currentVariable.getName());
                } else {
                    //System.out.println("Variavel: " + currentVariable.getName());
                    currentVariableList.add(currentVariable);
                }
            }
        }

        //Sair da recurção
        if (checkToken(";")) {
            if(checkToken("}")) {
                System.out.println("Finishe");
                return true;
            } else {
                analyzerVariable();
            }
        } else if(checkToken(",")) {
            //verificar se pode haver uma variável seguida de outra na mesma linha
            currentVariable = new Variable();
            currentVariable.setConst(false);
            checkDeclaration();
            if (currentVariable.isConst()) {
                //System.out.println("Constante: " + currentVariable.getName());
            } else {
                //System.out.println("Variavel: " + currentVariable.getName());
                currentVariableList.add(currentVariable);
            }
        }

        return false;
    }

    public boolean checkAssignment() {

        if(currentType.equals("string")) {
            if(currentToken.getType().equals("CDC")){
                currentVariable.setValue(currentToken.getValue());
                this.updateToken();
            } else {
                System.err.println("Erro");
            }
        } else if (currentType.equals("boolean")){

            if(currentToken.getValue().equals("true") || currentToken.getValue().equals("false")) {
                currentVariable.setValue(currentToken.getValue());
                this.updateToken();
            } else {
                System.err.println("Erro");
            }
        } else if (currentType.equals("int") || currentType.equals("float")) {

            if(currentToken.getType().equals("NRO")) {
                currentVariable.setValue(currentToken.getValue());
                this.updateToken();
            } else {
                System.err.println("Erro");
            }
        }
        symbleTable.addConst(currentVariable);
        return true;
    }

    public boolean chechClass() {
        if(checkToken("class")) {
            if(checkToken("IDE")) {
                if (checkToken("{")) {
                    checkVariable();
                    currenteClass.addVariableList(currentVariableList);
                    checkMethod();
                } else if(checkToken(currentToken.getValue())) {
                    if (checkToken("IDE")) {//Verificar se a classe herdade existe
                        if (checkToken("{")) {
                            checkVariable();
                            currenteClass.addVariableList(currentVariableList);
                            checkMethod();
                        }
                    }
                }
            }
        } else {
            checkMain();
        }

        return true;
    }

    public boolean checkMethod() {
        if (checkToken("method")) {
            if(nativeTypes.contains(currentToken.getValue())) {
                checkType(2);
                if (checkToken("IDE")) {
                    checkParams();
                    if (checkToken("{")) {
                        checkFunctionBody();
                        checkReturn();
                        if(checkToken(";")) {
                            if (checkToken("}")) {
                                checkMethod();
                            }else {
                                System.err.println("Não podem haver comandos após o return");
                            }
                        }
                        return true;
                    }
                }
            }
        } else  if (checkToken("}")) {
            chechClass();
        }
        return false;
    }

    public boolean checkFunctionBody() {
        return true;
    }

    public boolean checkReturn() {
        if(checkToken("return")) {
            System.out.println("valor do return para análise: " + currentToken.getValue());
            if(currenteClass.checkVariableType(currentToken.getValue(), getCurrentTypeMethod)) {
                updateToken();
                System.out.println("Return correto");
                return true;
            } else {
                System.out.println("Valor de retorn não é o mesmo do tipo da função");
            }
        }
        return false;
    }

    public boolean checkMain() {
        return true;
    }

    public boolean checkParams() {
        if(checkToken("(")) {
            if(nativeTypes.contains(currentToken.getValue())){
                updateToken();
                if(checkToken("IDE")) {
                    if (checkToken(")")) {
                        return true;
                    }
                }
            }
        }

        return true;
    }
}