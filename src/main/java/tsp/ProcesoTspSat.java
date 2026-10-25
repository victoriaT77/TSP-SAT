package tsp;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Orquesta las tres fases de forma automática:
 * 1) reducción TSP -> CNF (archivo DIMACS), 2) resolución con Sat4j,
 * 3) reconstrucción de la ruta a partir del modelo.
 */
public final class ProcesoTspSat {

    /** Datos que produce una ejecución completa (se usan en la consola y en las pruebas). */
    public static final class Resultado {
        public final EstadoSolver estado;
        public final int numVariables;
        public final int numClausulas;
        public final int umbralEfectivo;
        public final long milisReduccion;
        public final long milisSolver;
        public final int[] ruta;   // null si no es satisfacible
        public final int costo;    // -1 si no es satisfacible
        public final int[] modelo; // null si no es satisfacible
        public final DecodificadorRuta decodificador;

        Resultado(EstadoSolver estado, int numVariables, int numClausulas, int umbralEfectivo,
                  long milisReduccion, long milisSolver, int[] ruta, int costo,
                  int[] modelo, DecodificadorRuta decodificador) {
            this.estado = estado;
            this.numVariables = numVariables;
            this.numClausulas = numClausulas;
            this.umbralEfectivo = umbralEfectivo;
            this.milisReduccion = milisReduccion;
            this.milisSolver = milisSolver;
            this.ruta = ruta;
            this.costo = costo;
            this.modelo = modelo;
            this.decodificador = decodificador;
        }
    }

    private ProcesoTspSat() { }

    public static Resultado ejecutar(InstanciaTsp instancia, Path archivoCnf, int timeoutSegundos)
            throws IOException {
        // Fase 1: reducción y escritura del archivo DIMACS
        long inicio = System.nanoTime();
        ReductorTspSat reductor = new ReductorTspSat(instancia);
        FormulaCnf formula = reductor.generar();
        formula.exportarDimacs(archivoCnf, "TSP N=" + instancia.numNodos() + " K=" + instancia.umbral());
        long milisReduccion = (System.nanoTime() - inicio) / 1_000_000;

        // Fase 2: Sat4j resuelve la fórmula
        ResultadoSolver respuesta = new EjecutorSat().resolver(archivoCnf, timeoutSegundos);

        // Fase 3: reconstrucción de la ruta (solo si es satisfacible)
        DecodificadorRuta decodificador = new DecodificadorRuta(reductor);
        int[] ruta = null;
        int costo = -1;
        if (respuesta.estado() == EstadoSolver.SATISFACIBLE) {
            ruta = decodificador.decodificar(respuesta.modelo());
            costo = instancia.costoRuta(ruta);
            if (!instancia.esRutaValida(ruta) || costo > instancia.umbral()) {
                throw new IllegalStateException("El modelo del solver no corresponde a una ruta válida.");
            }
        }
        return new Resultado(respuesta.estado(), formula.numVariables(), formula.numClausulas(),
                reductor.umbralEfectivo(), milisReduccion, respuesta.milisegundos(),
                ruta, costo, respuesta.modelo(), decodificador);
    }
}
