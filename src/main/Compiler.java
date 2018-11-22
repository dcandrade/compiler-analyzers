package main;

import analyzers.lexical.LexicalAnalyzer;
import analyzers.syntatical.SyntacticalAnalyzer;
import model.error.Error;
import model.token.Token;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Compiler {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            // Entrada padrão caso não seja passada nenhuma por linha de comando
            String path = "tests/entrada_exemplo_teste_lexico.txt";
            System.out.println("Nenhum arquivo passado. Utilizando teste padrão");
            args = new String[]{path};
        }

        for (String file : args) {
            try {
                String[] split = file.split("/");
                split[split.length - 1] = "saida-" + split[split.length - 1];
                String outputFile = String.join("/", split);

                BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFile));
                LexicalAnalyzer lexer = new LexicalAnalyzer(file);

                for (Token token : lexer.getTokens()) {
                    writer.write(token + "\n");
                }

                writer.write("\n\n");

                for (Error error : lexer.getErrors()) {
                    writer.write(error + "\n");
                    System.out.println("ERRROOOOOOO");
                }

                writer.close();
                System.out.println("-- Saída do léxico disponível em "+outputFile);
                SyntacticalAnalyzer parser = new SyntacticalAnalyzer(lexer.getTokens().iterator());
                parser.parseProgram();

            } catch (IOException e) {
                System.err.println("Não foi possível encontrar o arquivo " + file);
            }
        }
    }

}
