package tsp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Construye instancias del TSP desde tres fuentes distintas:
 * un archivo de texto, un texto pasado por consola o un generador aleatorio.
 *
 * Formato del archivo (las líneas que empiezan con '#' son comentarios):
 * <pre>
 * N
 * K
 * fila 0 de la matriz (N enteros separados por espacios)
 * ...
 * fila N-1
 * </pre>
 */
public final class LectorGrafo {

    private LectorGrafo() { }

    /**
     * Lee una instancia desde un archivo.
     *
     * @param umbralForzado si no es null, reemplaza al K escrito en el archivo
     */
    public static InstanciaTsp desdeArchivo(Path archivo, Integer umbralForzado) throws IOException {
        List<String> tokens = new ArrayList<>();
        for (String linea : Files.readAllLines(archivo)) {
            String limpia = linea.trim();
            if (limpia.isEmpty() || limpia.startsWith("#")) {
                continue;
            }
            for (String token : limpia.split("\\s+")) {
                tokens.add(token);
            }
        }
        try {
            int posicion = 0;
            int n = Integer.parseInt(tokens.get(posicion++));
            int k = Integer.parseInt(tokens.get(posicion++));
            int[][] matriz = new int[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    matriz[i][j] = Integer.parseInt(tokens.get(posicion++));
                }
            }
            return new InstanciaTsp(matriz, umbralForzado != null ? umbralForzado : k);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            throw new IllegalArgumentException(
                    "Formato inválido en '" + archivo + "'. Se esperaba: N, K y luego N filas de N enteros.");
        }
    }

    /**
     * Lee la matriz desde un texto de consola. Las filas se separan con ';'
     * y los valores con espacios o comas. Ejemplo: "0 5 9;5 0 4;9 4 0".
     */
    public static InstanciaTsp desdeTexto(String texto, int umbral) {
        String[] filas = texto.trim().split(";");
        int[][] matriz = new int[filas.length][];
        try {
            for (int i = 0; i < filas.length; i++) {
                String[] valores = filas[i].trim().split("[,\\s]+");
                matriz[i] = new int[valores.length];
                for (int j = 0; j < valores.length; j++) {
                    matriz[i][j] = Integer.parseInt(valores[j]);
                }
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("La matriz contiene un valor que no es un entero.");
        }
        return new InstanciaTsp(matriz, umbral);
    }

    /** Genera una instancia simétrica con distancias aleatorias en [1, distanciaMaxima]. */
    public static InstanciaTsp aleatoria(int n, int distanciaMaxima, long semilla, int umbral) {
        if (n < 2) {
            throw new IllegalArgumentException("N debe ser al menos 2.");
        }
        if (distanciaMaxima < 1) {
            throw new IllegalArgumentException("La distancia máxima debe ser al menos 1.");
        }
        Random azar = new Random(semilla);
        int[][] matriz = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                matriz[i][j] = 1 + azar.nextInt(distanciaMaxima);
                matriz[j][i] = matriz[i][j];
            }
        }
        return new InstanciaTsp(matriz, umbral);
    }
}
