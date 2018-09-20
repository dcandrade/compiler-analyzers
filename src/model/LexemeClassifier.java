package model;

import exceptions.TokenClassificationException;

import java.util.*;
import java.util.regex.Pattern;

public class LexemeClassifier {
    public final static String RESERVED_WORD = "PRE";
    public final static String IDENTIFIER = "IDE";
    public final static String NUMBER = "NRO";
    public final static String DELIMITER = "DEL";
    public final static String RELATIONAL_OPERATOR = "REL";
    public final static String LOGICAL_OPERATOR = "LOG";
    public final static String ARITHMETICAL_OPERATOR = "ART";
    public final static String LINE_COMMENT = "LCO";
    public final static String BLOCK_COMMENT = "BCO";
    public final static String STRING = "CDC";
    public final static String SPACE = "ESP";


    private final static String[] PRIMITIVE_TYPES = {RESERVED_WORD, NUMBER, ARITHMETICAL_OPERATOR, LOGICAL_OPERATOR,
            RELATIONAL_OPERATOR, DELIMITER};


    private static String SYMBOL_REGEX;
    private static String STRING_REGEX;
    private static final String SPACE_REGEX = "[ \t\n]";
    private static final String DIGIT_REGEX = "[0-9]";
    private static final String LETTER_REGEX = "([a-z]|[A-Z])";
    private static final String ARITHMETICAL_OPERATOR_REGEX = "\\+|-|\\*|/|\\+\\+|--";
    private static final String RELATIONAL_OPERATOR_REGEX = "!=|==|\\<|\\<=|\\>|\\>=|=";
    private static final String RESERVERD_WORD_REGEX = "class|const|variables|method|return|main|if|then|else|while|read|write|void|int|float|bool|string|true|false|extends";
    private static final String IDENTIFIER_REGEX = LETTER_REGEX + "(" + LETTER_REGEX + "|" + DIGIT_REGEX + "|_)*";
    private static final String NUMBER_REGEX = "(-)?(" + SPACE_REGEX + ")*" + DIGIT_REGEX + "+" + "(." + DIGIT_REGEX + "+" + ")";
    private static final String LOGICAL_OPERATOR_REGEX = "!|&&|\\|\\|";
    private static final String DELIMITER_REGEX = ";|,|\\(|\\)|\\[|\\]|\\{|\\}|\\.";
    private static final String BLOCK_COMMENT_REGEX = "^/\\*";
    private static final String LINE_COMMENT_REGEX = "^//";

    private final Map<String, String> categories2Regex;

    public LexemeClassifier() {
        this.generatePendingRegexes();
        this.categories2Regex = new LinkedHashMap<>(); // MUST be linked hash map to preserve order of insertion
        this.populateClassificationMap();
    }

    public Optional<String> classify(String token) {

        for (Map.Entry<String, String> entry : this.categories2Regex.entrySet()) {
            String category = entry.getKey();
            String regex = entry.getValue();

            if (Pattern.matches(regex, token)) {
                return Optional.of(category);
            }
        }

        return Optional.empty();
    }

    public Optional<String> checkForPrimitiveTypes(String token) {
        for (String type : LexemeClassifier.PRIMITIVE_TYPES) {
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

    private void populateClassificationMap() {
        this.categories2Regex.put(LexemeClassifier.RESERVED_WORD, LexemeClassifier.RESERVERD_WORD_REGEX);
        this.categories2Regex.put(LexemeClassifier.IDENTIFIER, LexemeClassifier.IDENTIFIER_REGEX);
        this.categories2Regex.put(LexemeClassifier.NUMBER, LexemeClassifier.NUMBER_REGEX);
        this.categories2Regex.put(LexemeClassifier.DELIMITER, LexemeClassifier.DELIMITER_REGEX);
        this.categories2Regex.put(LexemeClassifier.RELATIONAL_OPERATOR, LexemeClassifier.RELATIONAL_OPERATOR_REGEX);
        this.categories2Regex.put(LexemeClassifier.LOGICAL_OPERATOR, LexemeClassifier.LOGICAL_OPERATOR_REGEX);
        this.categories2Regex.put(LexemeClassifier.ARITHMETICAL_OPERATOR, LexemeClassifier.ARITHMETICAL_OPERATOR_REGEX);
        this.categories2Regex.put(LexemeClassifier.BLOCK_COMMENT, LexemeClassifier.BLOCK_COMMENT_REGEX);
        this.categories2Regex.put(LexemeClassifier.LINE_COMMENT, LexemeClassifier.LINE_COMMENT_REGEX);
    }

    private void generatePendingRegexes() {
        LexemeClassifier.SYMBOL_REGEX = this.generateSymbolRegex();
        LexemeClassifier.STRING_REGEX = "\"(" + LETTER_REGEX + "|" + DIGIT_REGEX + "|" + SYMBOL_REGEX + ")*\"";
    }

    private String generateSymbolRegex() {
        String specialChars = "[]()*-+?|{}";

        StringBuilder symbolRegexBuilder = new StringBuilder();
        for (int ascii_index = 32; ascii_index <= 126; ascii_index++) {
            if (ascii_index != 34) {
                char ch = (char) ascii_index;
                if (specialChars.indexOf(ch) != -1) {
                    symbolRegexBuilder.append("\\").append(ch);
                    symbolRegexBuilder.append("|");
                } else {
                    symbolRegexBuilder.append(ch);
                    symbolRegexBuilder.append("|");
                }
            }
        }
        symbolRegexBuilder.deleteCharAt(symbolRegexBuilder.length() - 1); //deleting last |

        return symbolRegexBuilder.toString();
    }

    private static String getDelimiters() {
        return LexemeClassifier.DELIMITER_REGEX.replace("|", "");
    }

    private static String getOperators() {
        String logical = LexemeClassifier.LOGICAL_OPERATOR_REGEX.replace("|", "");
        String arithmetical = LexemeClassifier.ARITHMETICAL_OPERATOR_REGEX.replace("|", "");
        String relational = LexemeClassifier.RELATIONAL_OPERATOR_REGEX.replace("|", "");

        return logical + arithmetical + relational;
    }

    public static String getAllCompilerDemiliters() {
        return LexemeClassifier.getDelimiters() + LexemeClassifier.getOperators() + " \t\n";
    }

    public static void main(String[] args) {
        LexemeClassifier classifier = new LexemeClassifier();
        System.out.println(LexemeClassifier.STRING_REGEX);
        System.out.println(Pattern.matches(LexemeClassifier.STRING_REGEX, "\"a_123\""));
    }

}
