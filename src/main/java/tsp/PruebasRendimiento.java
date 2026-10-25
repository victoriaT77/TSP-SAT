package tsp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Genera la tabla de rendimiento del informe.
 *
 * Uso: PruebasRendimiento [--tamanos 3,4,6,8,10] [--factores 0.3,0.5]
 *                         [--distmax 10] [--semilla 1] [--timeout 120]
 *
 * Para cada factor f y cada tamaño N se usa K = round(f * N * distmax).
 * Un factor chico acerca K al óptimo y suele producir instancias más difíciles.
 * Para cada factor se detiene en el primer tiempo agotado (punto de quiebre).
 */
public final class PruebasRendimiento {

    private PruebasRendimiento() { }

    public static void main(String[] args) throws IOException {
        String tamanos = "3,4,6,8,10";
        String factores = "0.3,0.5";
        int distanciaMaxima = 10;
        long semilla = 1;
        int timeout = 120;

        for (int i = 0; i < args.length; i += 2) {
            if (i + 1 >= args.length) {
                throw new IllegalArgumentException("Falta el valor de " + args[i]);
            }
            switch (args[i]) {
                case "--tamanos":  tamanos = args[i + 1]; break;
                case "--factores": factores = args[i + 1]; break;
                case "--distmax":  distanciaMaxima = Integer.parseInt(args[i + 1]); break;
                case "--semilla":  semilla = Long.parseLong(args[i + 1]); break;
                case "--timeout":  timeout = Integer.parseInt(args[i + 1]); break;
                default: throw new IllegalArgumentException("Opción desconocida: " + args[i]);
            }
        }

        System.out.println("| N | Factor | K | Variables | Cláusulas | Reducción (ms) | Sat4j (ms) | Resultado |");
        System.out.println("|---|--------|---|-----------|-----------|----------------|------------|-----------|");

        Path temporal = Files.createTempFile("tsp-rendimiento", ".cnf");
        try {
            for (String textoFactor : factores.split(",")) {
                double factor = Double.parseDouble(textoFactor.trim());
                for (String textoN : tamanos.split(",")) {
                    int n = Integer.parseInt(textoN.trim());
                    int umbral = (int) Math.round(factor * n * distanciaMaxima);
                    InstanciaTsp instancia = LectorGrafo.aleatoria(n, distanciaMaxima, semilla, umbral);
                    ProcesoTspSat.Resultado r = ProcesoTspSat.ejecutar(instancia, temporal, timeout);
                    System.out.printf("| %d | %.2f | %d | %d | %d | %d | %d | %s |%n",
                            n, factor, umbral, r.numVariables, r.numClausulas,
                            r.milisReduccion, r.milisSolver, r.estado);
                    if (r.estado == EstadoSolver.TIEMPO_AGOTADO) {
                        break; // punto de quiebre para este factor
                    }
                }
            }
        } finally {
            Files.deleteIfExists(temporal);
        }
    }
}
