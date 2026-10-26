package tsp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Genera la tabla de rendimiento del informe. Dos modos:
 *
 * 1) Por factor:  --tamanos 3,4,10 --factores 0.3,0.5
 *    Para cada factor f y cada N se usa K = round(f * N * distmax).
 *    Se detiene en el primer tiempo agotado de cada factor.
 *
 * 2) Por casos:   --casos 10:26,10:25,12:29,12:28
 *    Cada par N:K se ejecuta tal cual (útil para probar K igual al óptimo o
 *    un valor menos, que son las instancias más difíciles). No se detiene
 *    en un tiempo agotado: sigue con el siguiente caso.
 *
 * Opciones comunes: --distmax 10  --semilla 1  --timeout 120
 */
public final class PruebasRendimiento {

    private PruebasRendimiento() { }

    public static void main(String[] args) throws IOException {
        String tamanos = "3,4,6,8,10";
        String factores = "0.3,0.5";
        String casos = null;
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
                case "--casos":    casos = args[i + 1]; break;
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
            if (casos != null) {
                for (String par : casos.split(",")) {
                    String[] partes = par.trim().split(":");
                    if (partes.length != 2) {
                        throw new IllegalArgumentException("Cada caso debe tener la forma N:K, por ejemplo 10:26");
                    }
                    int n = Integer.parseInt(partes[0].trim());
                    int umbral = Integer.parseInt(partes[1].trim());
                    medir(n, Double.NaN, umbral, distanciaMaxima, semilla, timeout, temporal);
                }
            } else {
                for (String textoFactor : factores.split(",")) {
                    double factor = Double.parseDouble(textoFactor.trim());
                    for (String textoN : tamanos.split(",")) {
                        int n = Integer.parseInt(textoN.trim());
                        int umbral = (int) Math.round(factor * n * distanciaMaxima);
                        boolean agotado = medir(n, factor, umbral, distanciaMaxima, semilla, timeout, temporal);
                        if (agotado) {
                            break; // punto de quiebre para este factor
                        }
                    }
                }
            }
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    /** Ejecuta un caso, imprime su fila y devuelve true si el solver agotó el tiempo. */
    private static boolean medir(int n, double factor, int umbral, int distanciaMaxima,
                                 long semilla, int timeout, Path temporal) throws IOException {
        InstanciaTsp instancia = LectorGrafo.aleatoria(n, distanciaMaxima, semilla, umbral);
        ProcesoTspSat.Resultado r = ProcesoTspSat.ejecutar(instancia, temporal, timeout);
        String textoFactor = Double.isNaN(factor) ? "-" : String.format("%.2f", factor);
        System.out.printf("| %d | %s | %d | %d | %d | %d | %d | %s |%n",
                n, textoFactor, umbral, r.numVariables, r.numClausulas,
                r.milisReduccion, r.milisSolver, r.estado);
        return r.estado == EstadoSolver.TIEMPO_AGOTADO;
    }
}