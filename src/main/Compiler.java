package main;

import analyzers.lexical.LexicalAnalyzer;

import java.io.IOException;

public class Compiler {
    public static void main(String[] args) {
        String path = "tests/entrada_exemplo_teste_lexico.txt";
        LexicalAnalyzer analyzer = new LexicalAnalyzer(path);

        analyzer.getTokens().forEach(System.out::println);
        System.out.println("\n\n");
        analyzer.getErrors().forEach(System.out::println);
    }
}
