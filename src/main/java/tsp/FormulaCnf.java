package tsp;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Fórmula booleana en Forma Normal Conjuntiva (CNF) con exportación a DIMACS.
 *
 * Optimización de memoria: en lugar de guardar un objeto int[] por cláusula,
 * todos los literales se almacenan en UN solo arreglo, y cada cláusula termina
 * con un 0 (igual que en el formato DIMACS). Así se usa mucha menos memoria
 * cuando la fórmula tiene cientos de miles de cláusulas.
 */
public final class FormulaCnf {

    private final int numVariables;
    private int[] literales = new int[1024];
    private int tamano = 0;
    private int numClausulas = 0;

    public FormulaCnf(int numVariables) {
        this.numVariables = numVariables;
    }

    /** Agrega una cláusula (disyunción de literales). Un literal negativo es la negación. */
    public void agregarClausula(int... clausula) {
        asegurarCapacidad(clausula.length + 1);
        for (int literal : clausula) {
            if (literal == 0 || Math.abs(literal) > numVariables) {
                throw new IllegalArgumentException("Literal fuera de rango: " + literal);
            }
            literales[tamano++] = literal;
        }
        literales[tamano++] = 0; // fin de cláusula
        numClausulas++;
    }

    public int numVariables() { return numVariables; }
    public int numClausulas() { return numClausulas; }

    private void asegurarCapacidad(int extra) {
        if (tamano + extra > literales.length) {
            literales = Arrays.copyOf(literales, Math.max(literales.length * 2, tamano + extra));
        }
    }

    /** Escribe la fórmula en formato DIMACS CNF. */
    public void exportarDimacs(Path archivo, String comentario) throws IOException {
        try (BufferedWriter escritor = Files.newBufferedWriter(archivo)) {
            escritor.write("c " + comentario);
            escritor.newLine();
            escritor.write("p cnf " + numVariables + " " + numClausulas);
            escritor.newLine();
            StringBuilder linea = new StringBuilder();
            for (int i = 0; i < tamano; i++) {
                int literal = literales[i];
                if (literal == 0) {
                    linea.append('0');
                    escritor.write(linea.toString());
                    escritor.newLine();
                    linea.setLength(0);
                } else {
                    linea.append(literal).append(' ');
                }
            }
        }
    }
}
