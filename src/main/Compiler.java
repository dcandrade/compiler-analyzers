package main;

import analyzers.lexical.LexicalAnalyzer;
import analyzers.syntatical.SyntacticalAnalyzer;
import model.error.LexicalError;
import model.error.SyntaxError;
import model.token.Token;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class Compiler {
    public static void main(String[] args) throws Exception {
        List<Path> inputs = Files.list(Paths.get("tests")).collect(Collectors.toList());
        for (Path file : inputs) {

            Files.createDirectories(Paths.get("output", "lexico"));
            Files.createDirectories(Paths.get("output", "sintatico"));

            Path outputFile = Paths.get("output",  "lexico", file.getFileName().toString());

            BufferedWriter writer = Files.newBufferedWriter(outputFile);
            LexicalAnalyzer lexer = new LexicalAnalyzer(file);

            for (Token token : lexer.getTokens()) {
                writer.write(token + "\n");
            }

            writer.write("\n\n");

            for (LexicalError lexicalError : lexer.getLexicalErrors()) {
                writer.write(lexicalError + "\n");
            }

            writer.close();
            System.out.println("-- Saída do léxico disponível em " + outputFile);
            SyntacticalAnalyzer parser = new SyntacticalAnalyzer(lexer.getTokens());
            parser.parseProgram();

            for (SyntaxError error : parser.getErrors()) {
                System.out.println(error);
            }
        }
    }

}
