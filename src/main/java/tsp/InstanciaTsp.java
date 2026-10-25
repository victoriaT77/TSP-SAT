package tsp;

/**
 * Instancia del Problema del Viajante (TSP) en su versión de decisión:
 * ¿existe un recorrido cerrado que visite las N ciudades una sola vez
 * con costo total menor o igual a K?
 *
 * Esta clase solo guarda y valida los datos; no resuelve nada.
 */
public final class InstanciaTsp {

    private final int numNodos;
    private final int[][] distancias;
    private final int umbral;
    private final int distanciaMinima; // menor distancia entre dos ciudades distintas
    private final int distanciaMaxima; // mayor distancia entre dos ciudades distintas

    /**
     * @param distancias matriz cuadrada N x N de enteros no negativos (puede ser asimétrica)
     * @param umbral     costo máximo K permitido (K >= 0)
     */
    public InstanciaTsp(int[][] distancias, int umbral) {
        if (distancias == null || distancias.length < 2) {
            throw new IllegalArgumentException("Se necesitan al menos 2 ciudades.");
        }
        if (umbral < 0) {
            throw new IllegalArgumentException("El umbral K debe ser mayor o igual a 0.");
        }
        this.numNodos = distancias.length;
        this.umbral = umbral;
        this.distancias = new int[numNodos][numNodos];

        int minimo = Integer.MAX_VALUE;
        int maximo = 0;
        for (int i = 0; i < numNodos; i++) {
            if (distancias[i] == null || distancias[i].length != numNodos) {
                throw new IllegalArgumentException(
                        "La matriz de distancias debe ser cuadrada (" + numNodos + "x" + numNodos + ").");
            }
            for (int j = 0; j < numNodos; j++) {
                int d = distancias[i][j];
                if (d < 0) {
                    throw new IllegalArgumentException("Las distancias deben ser enteros >= 0.");
                }
                this.distancias[i][j] = d;
                if (i != j) {
                    minimo = Math.min(minimo, d);
                    maximo = Math.max(maximo, d);
                }
            }
        }
        this.distanciaMinima = minimo;
        this.distanciaMaxima = maximo;
    }

    public int numNodos() { return numNodos; }
    public int umbral() { return umbral; }
    public int distancia(int origen, int destino) { return distancias[origen][destino]; }
    public int distanciaMinima() { return distanciaMinima; }
    public int distanciaMaxima() { return distanciaMaxima; }

    /** Costo real de un recorrido cerrado (incluye el regreso a la ciudad inicial). */
    public int costoRuta(int[] ruta) {
        int costo = 0;
        for (int paso = 0; paso < ruta.length; paso++) {
            costo += distancias[ruta[paso]][ruta[(paso + 1) % ruta.length]];
        }
        return costo;
    }

    /** Verifica que la ruta sea una permutación de las N ciudades. */
    public boolean esRutaValida(int[] ruta) {
        if (ruta == null || ruta.length != numNodos) {
            return false;
        }
        boolean[] vista = new boolean[numNodos];
        for (int ciudad : ruta) {
            if (ciudad < 0 || ciudad >= numNodos || vista[ciudad]) {
                return false;
            }
            vista[ciudad] = true;
        }
        return true;
    }
}
