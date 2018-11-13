package model.token;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class LexemeClassifier {

    public static final String LINE_COMMENT_REGEX = "//.*";

    //Lista de REGEX
    private static final String SPACE_REGEX = "([ \t\n])*";
    private static final String DIGIT_REGEX = "[0-9]";
    private static final String LETTER_REGEX = "([a-z]|[A-Z])";
    private static final String IDENTIFIER_REGEX = LETTER_REGEX + "(" + LETTER_REGEX + "|" + DIGIT_REGEX + "|_)*";
    private static final String ARITHMETICAL_OPERATOR_REGEX = "\\+|-|\\*|/|\\+\\+|--";
    private static final String RELATIONAL_OPERATOR_REGEX = "!=|==|\\<|\\<=|\\>|\\>=|=|!";
    private static final String RESERVERD_WORD_REGEX = "class|const|variables|method|return|main|if|then|else|while|read|write|void|int|float|bool|string|true|false|extends";
    private static final String LOGICAL_OPERATOR_REGEX = "!|&&|\\|\\|";
    private static final String DELIMITER_REGEX = ";|,|\\(|\\)|\\[|\\]|\\{|\\}|\\.";
    private static final String BLOCK_COMMENT_START_REGEX = "/\\*";
    private static final String BLOCK_COMMENT_END_REGEX = "\\*/";
    private final static String NEGATIVE_NUMBER_REGEX = "(-)(" + LexemeClassifier.SPACE_REGEX + ")*" + LexemeClassifier.DIGIT_REGEX + "+" + "(\\." + LexemeClassifier.DIGIT_REGEX + "+" + ")*";
    private final static String POSITIVE_NUMBER_REGEX = LexemeClassifier.DIGIT_REGEX + "+" + "(\\." + LexemeClassifier.DIGIT_REGEX + "+" + ")*";
    private final static String NUMBER_REGEX = NEGATIVE_NUMBER_REGEX + "|" + POSITIVE_NUMBER_REGEX;
    private static String SYMBOL_REGEX;
    private static String STRING_REGEX;
    private final Map<String, String> categories2Regex;

    public LexemeClassifier() {
        this.generatePendingRegexes();
        this.categories2Regex = new LinkedHashMap<>(); // MUST be linked hash map to preserve order of insertion
        this.populateClassificationMap();
    }

    private static String getDelimiters() {
        return DELIMITER_REGEX.replace("|", "");
    }

    private static String getOperators() {
        String logical = LOGICAL_OPERATOR_REGEX.replace("|", "");
        String arithmetical = ARITHMETICAL_OPERATOR_REGEX.replace("|", "");
        String relational = RELATIONAL_OPERATOR_REGEX.replace("|", "");

        return logical + arithmetical + relational;
    }

    public static String getAllCompilerDemiliters() {
        return getDelimiters() + getOperators() + " \t\n";
    }

    public static void main(String[] args) {
        LexemeClassifier lexemeClassifier = new LexemeClassifier();
        System.out.println(NUMBER_REGEX);
        String str = "-    3";
        System.out.println(str);

        System.out.println(Pattern.matches(NUMBER_REGEX, str));
        System.out.println(lexemeClassifier.classify(str));
    }

    //Classifica um tokem
    public String classify(String token) {

        for (Map.Entry<String, String> entry : this.categories2Regex.entrySet()) {
            String category = entry.getKey();
            String regex = entry.getValue();

            if (Pattern.matches(regex, token)) {
                return category;
            }
        }

        return TokenTypes.INVALID_TOKEN;
    }

    public Optional<String> checkForPrimitiveTypes(String token) {
        for (String type : TokenTypes.PRIMITIVE_TYPES) {
            if (this.checkTokenType(token, type)) {
                return Optional.of(type);
            }
        }

        return Optional.empty();
    }

    public boolean checkTokenType(String token, String type) {
        String regex = this.categories2Regex.get(type);

        return regex != null && Pattern.matches(regex, token);
    }

    //Povoa o HashMap com os REGEX com base na prescedência
    private void populateClassificationMap() {
        this.categories2Regex.put(TokenTypes.RESERVED_WORD, RESERVERD_WORD_REGEX);
        this.categories2Regex.put(TokenTypes.IDENTIFIER, IDENTIFIER_REGEX);
        this.categories2Regex.put(TokenTypes.NUMBER, NUMBER_REGEX);
        this.categories2Regex.put(TokenTypes.RELATIONAL_OPERATOR, RELATIONAL_OPERATOR_REGEX);
        this.categories2Regex.put(TokenTypes.LOGICAL_OPERATOR, LOGICAL_OPERATOR_REGEX);
        this.categories2Regex.put(TokenTypes.ARITHMETICAL_OPERATOR, ARITHMETICAL_OPERATOR_REGEX);
        this.categories2Regex.put(TokenTypes.DELIMITER, DELIMITER_REGEX);
        this.categories2Regex.put(TokenTypes.STRING, STRING_REGEX);
        this.categories2Regex.put(TokenTypes.SPACE, SPACE_REGEX);
        this.categories2Regex.put(TokenTypes.BLOCK_COMMENT_START, BLOCK_COMMENT_START_REGEX);
        this.categories2Regex.put(TokenTypes.BLOCK_COMMENT_END, BLOCK_COMMENT_END_REGEX);
    }

    private void generatePendingRegexes() {
        SYMBOL_REGEX = this.generateSymbolRegex();
        STRING_REGEX = "\"(" + LETTER_REGEX + "|" + DIGIT_REGEX + "|" + SYMBOL_REGEX + ")*\"";
    }

    private String generateSymbolRegex() {
        String specialChars = "[]()*-+?|{}" + (char) 34;

        StringBuilder symbolRegexBuilder = new StringBuilder();
        for (int ascii_index = 32; ascii_index <= 126; ascii_index++) {
            char ch = (char) ascii_index;

            if (specialChars.indexOf(ch) != -1) {
                symbolRegexBuilder.append("\\").append(ch);
                symbolRegexBuilder.append("|");
            } else {
                symbolRegexBuilder.append(ch);
                symbolRegexBuilder.append("|");
            }
        }
        symbolRegexBuilder.deleteCharAt(symbolRegexBuilder.length() - 1); //deleting last |

        return symbolRegexBuilder.toString();
    }

}
