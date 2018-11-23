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
        for (Path p : inputs) {
            String file = p.toString();
            String[] split = file.split("/");
            String filename = split[split.length - 1];
            String outputFile = "output/lexico_" + filename;

            BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFile));
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
            SyntacticalAnalyzer parser = new SyntacticalAnalyzer(lexer.getTokens().iterator());
            parser.parseProgram();

            for (SyntaxError error : parser.getErrors()) {
                System.out.println(error);
            }
        }
    }

}
