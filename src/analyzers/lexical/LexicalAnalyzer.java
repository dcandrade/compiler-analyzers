package analyzers.lexical;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class LexicalAnalyzer {
    private final Map<Integer, String> symbolTable; // int to lexeme
    private final StringBuilder buffer;
    private int currentLineNumber;

    public LexicalAnalyzer() {
        this.symbolTable = new HashMap<>();
        this.buffer = new StringBuilder();
        this.currentLineNumber = 0;
    }

    public void processLine(String line) {
        this.currentLineNumber++;
        StringTokenizer tokenizer = new StringTokenizer(line);

        while (tokenizer.hasMoreTokens()) {
            System.out.print(tokenizer.nextToken());
        }
        System.out.println("\n");
    }


}