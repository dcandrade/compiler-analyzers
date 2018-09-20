package model.error;

import java.util.regex.Pattern;

public class ErrorClassifier {
    private static final String MALFORMED_STRING = "CMF";
    private static final String MALFORMED_NUMBER = "NMF";
    private static final String MALFORMED_IDENTIFIER = "IMF";
    private static final String MALFORMED_COMMENT = "CoMF";
    private static final String MALFORMED_TOKEN = "TMF";


    public static String classify(String occurence) {
        if (occurence.startsWith("\"")) {
            return ErrorClassifier.MALFORMED_STRING;
        }

        if (Pattern.matches("\\d.*", occurence)) {
            return ErrorClassifier.MALFORMED_NUMBER;
        }

        if (occurence.startsWith("/*")) {
            return ErrorClassifier.MALFORMED_COMMENT;
        }

        if (Pattern.matches("\\w.*", occurence)) {
            return ErrorClassifier.MALFORMED_IDENTIFIER;
        }

        return ErrorClassifier.MALFORMED_TOKEN;
    }
}
