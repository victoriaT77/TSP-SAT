package tsp;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Punto de entrada. Modos de entrada (elegir exactamente uno):
 * <pre>
 *   Main archivo.txt [--k K]
 *   Main --distancias "0 5 9;5 0 4;9 4 0" --k K
 *   Main --aleatoria N --k K [--distmax D] [--semilla S]
 * </pre>
 * Opciones comunes: --cnf salida.cnf, --timeout segundos, --detalle, --ayuda
 */
public final class Main {

    private static final int TIMEOUT_POR_DEFECTO = 300;

    private Main() { }

    public static void main(String[] args) {
        try {
            ejecutar(args);
        } catch (IllegalArgumentException | IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println("Use --ayuda para ver las opciones disponibles.");
            System.exit(1);
        }
    }

    private static void ejecutar(String[] args) throws IOException {
        if (args.length == 0) {
            mostrarAyuda();
            return;
        }

        String archivo = null;
        String distancias = null;
        Integer nodosAleatorios = null;
        Integer umbral = null;
        int distanciaMaxima = 10;
        long semilla = 1;
        Path archivoCnf = Paths.get("tsp.cnf");
        int timeout = TIMEOUT_POR_DEFECTO;
        boolean detalle = false;

        for (int i = 0; i < args.length; i++) {
            String opcion = args[i];
            switch (opcion) {
                case "--ayuda":
                case "-h":
                    mostrarAyuda();
                    return;
                case "--k":
                    umbral = Integer.parseInt(valor(args, ++i, opcion));
                    break;
                case "--distancias":
                    distancias = valor(args, ++i, opcion);
                    break;
                case "--aleatoria":
                    nodosAleatorios = Integer.parseInt(valor(args, ++i, opcion));
                    break;
                case "--distmax":
                    distanciaMaxima = Integer.parseInt(valor(args, ++i, opcion));
                    break;
                case "--semilla":
                    semilla = Long.parseLong(valor(args, ++i, opcion));
                    break;
                case "--cnf":
                    archivoCnf = Paths.get(valor(args, ++i, opcion));
                    break;
                case "--timeout":
                    timeout = Integer.parseInt(valor(args, ++i, opcion));
                    break;
                case "--detalle":
                    detalle = true;
                    break;
                default:
                    if (opcion.startsWith("--")) {
                        throw new IllegalArgumentException("Opción desconocida: " + opcion);
                    }
                    if (archivo != null) {
                        throw new IllegalArgumentException("Solo se admite un archivo de entrada.");
                    }
                    archivo = opcion;
            }
        }

        int modos = (archivo != null ? 1 : 0) + (distancias != null ? 1 : 0) + (nodosAleatorios != null ? 1 : 0);
        if (modos != 1) {
            throw new IllegalArgumentException(
                    "Indique exactamente una entrada: un archivo, --distancias o --aleatoria.");
        }

        InstanciaTsp instancia;
        if (archivo != null) {
            instancia = LectorGrafo.desdeArchivo(Paths.get(archivo), umbral);
        } else {
            if (umbral == null) {
                throw new IllegalArgumentException("Este modo de entrada requiere --k.");
            }
            instancia = distancias != null
                    ? LectorGrafo.desdeTexto(distancias, umbral)
                    : LectorGrafo.aleatoria(nodosAleatorios, distanciaMaxima, semilla, umbral);
        }

        ProcesoTspSat.Resultado r = ProcesoTspSat.ejecutar(instancia, archivoCnf, timeout);
        mostrarResultado(instancia, r, archivoCnf, timeout, detalle);
    }

    private static void mostrarResultado(InstanciaTsp instancia, ProcesoTspSat.Resultado r,
                                         Path archivoCnf, int timeout, boolean detalle) {
        System.out.printf("N=%d  K=%d%n", instancia.numNodos(), instancia.umbral());
        System.out.printf("CNF: %d variables, %d cláusulas (reducción en %d ms) -> %s%n",
                r.numVariables, r.numClausulas, r.milisReduccion, archivoCnf);
        if (r.umbralEfectivo != instancia.umbral()) {
            System.out.printf("Nota: K se acotó a %d (ningún ciclo puede costar más que N * distancia máxima).%n",
                    r.umbralEfectivo);
        }
        System.out.printf("Sat4j: %s en %d ms%n", r.estado, r.milisSolver);

        switch (r.estado) {
            case INSATISFACIBLE:
                System.out.println("NO existe ninguna ruta con costo menor o igual a K");
                break;
            case TIEMPO_AGOTADO:
                System.out.println("Tiempo agotado (" + timeout + " s): el resultado es indeterminado.");
                break;
            default:
                StringBuilder ruta = new StringBuilder();
                for (int ciudad : r.ruta) {
                    ruta.append(ciudad).append(" -> ");
                }
                ruta.append(r.ruta[0]);
                System.out.println("Ruta encontrada: " + ruta);
                System.out.println("Costo real: " + r.costo + " (<= K=" + instancia.umbral() + ")");
                if (detalle) {
                    System.out.println("Variables verdaderas: " + r.decodificador.variablesVerdaderas(r.modelo));
                }
        }
    }

    private static String valor(String[] args, int indice, String opcion) {
        if (indice >= args.length) {
            throw new IllegalArgumentException("Falta el valor de " + opcion);
        }
        return args[indice];
    }

    private static void mostrarAyuda() {
        System.out.println("Reducción TSP -> SAT con Sat4j");
        System.out.println();
        System.out.println("Uso (elegir UNA entrada):");
        System.out.println("  Main <archivo.txt> [--k K]");
        System.out.println("     Ejemplo: java -jar target/tsp-sat.jar instancias/n4_sat.txt");
        System.out.println("  Main --distancias \"0 5 9;5 0 4;9 4 0\" --k K");
        System.err.println("     Ejemplo: java -jar target/tsp-sat.jar --distancias \"0 10 15 20;10 0 35 25;15 35 0 30;20 25 30 0\" --k 80");
        System.out.println("  Main --aleatoria N --k K [--distmax D] [--semilla S]");
        System.out.println("     Ejemplo: java -jar target/tsp-sat.jar --aleatoria 8 --k 40 --distmax 10 --semilla 1");
    }
}