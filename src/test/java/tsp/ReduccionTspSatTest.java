package tsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Pruebas de la reducción. La fuerza bruta de este archivo se usa SOLO para
 * comprobar que Sat4j responde igual que la respuesta correcta en instancias
 * diminutas; el programa principal nunca la utiliza.
 */
class ReduccionTspSatTest {

    @TempDir
    Path temporal;

    private static final int[][] MATRIZ_4 = {
        {0, 10, 15, 20},
        {10, 0, 35, 25},
        {15, 35, 0, 30},
        {20, 25, 30, 0}
    };

    private ProcesoTspSat.Resultado resolver(InstanciaTsp instancia) throws Exception {
        return ProcesoTspSat.ejecutar(instancia, temporal.resolve("prueba.cnf"), 60);
    }

    @Test
    void umbralIgualAlOptimoEsSatisfacible() throws Exception {
        InstanciaTsp instancia = new InstanciaTsp(MATRIZ_4, 80);
        ProcesoTspSat.Resultado r = resolver(instancia);
        assertEquals(EstadoSolver.SATISFACIBLE, r.estado);
        assertTrue(instancia.esRutaValida(r.ruta));
        assertEquals(80, r.costo);
    }

    @Test
    void umbralMenorAlOptimoEsInsatisfacible() throws Exception {
        assertEquals(EstadoSolver.INSATISFACIBLE, resolver(new InstanciaTsp(MATRIZ_4, 79)).estado);
    }

    @Test
    void umbralCeroEsInsatisfacibleSiTodasLasDistanciasSonPositivas() throws Exception {
        assertEquals(EstadoSolver.INSATISFACIBLE, resolver(new InstanciaTsp(MATRIZ_4, 0)).estado);
    }

    @Test
    void umbralMuyGrandeEsSatisfacible() throws Exception {
        ProcesoTspSat.Resultado r = resolver(new InstanciaTsp(MATRIZ_4, 1_000_000));
        assertEquals(EstadoSolver.SATISFACIBLE, r.estado);
    }

    @Test
    void coincideConLaRespuestaCorrectaEnInstanciasPequenas() throws Exception {
        Random azar = new Random(12345);
        for (int caso = 0; caso < 20; caso++) {
            int n = 3 + azar.nextInt(4); // N entre 3 y 6
            int[][] d = new int[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    d[i][j] = (i == j) ? 0 : azar.nextInt(10); // incluye distancias 0 y matrices asimétricas
                }
            }
            int optimo = optimoPorFuerzaBruta(d);
            for (int k = Math.max(0, optimo - 1); k <= optimo + 1; k++) {
                InstanciaTsp instancia = new InstanciaTsp(d, k);
                ProcesoTspSat.Resultado r = resolver(instancia);
                if (k >= optimo) {
                    assertEquals(EstadoSolver.SATISFACIBLE, r.estado, "N=" + n + " K=" + k);
                    assertTrue(instancia.esRutaValida(r.ruta));
                    assertTrue(r.costo <= k);
                } else {
                    assertEquals(EstadoSolver.INSATISFACIBLE, r.estado, "N=" + n + " K=" + k);
                }
            }
        }
    }

    // ---- utilidades solo para las pruebas ----

    private static int optimoPorFuerzaBruta(int[][] d) {
        int n = d.length;
        int[] ruta = new int[n];
        boolean[] usada = new boolean[n];
        usada[0] = true;
        return permutar(d, ruta, usada, 1);
    }

    private static int permutar(int[][] d, int[] ruta, boolean[] usada, int posicion) {
        int n = d.length;
        if (posicion == n) {
            int costo = 0;
            for (int t = 0; t < n; t++) {
                costo += d[ruta[t]][ruta[(t + 1) % n]];
            }
            return costo;
        }
        int mejor = Integer.MAX_VALUE;
        for (int ciudad = 1; ciudad < n; ciudad++) {
            if (!usada[ciudad]) {
                usada[ciudad] = true;
                ruta[posicion] = ciudad;
                mejor = Math.min(mejor, permutar(d, ruta, usada, posicion + 1));
                usada[ciudad] = false;
            }
        }
        return mejor;
    }
}
