package tsp;

/** Respuesta del SAT-Solver: estado, modelo (si es satisfacible) y tiempo empleado. */
public final class ResultadoSolver {
    private final EstadoSolver estado;
    private final int[] modelo;
    private final long milisegundos;

    public ResultadoSolver(EstadoSolver estado, int[] modelo, long milisegundos) {
        this.estado = estado;
        this.modelo = modelo;
        this.milisegundos = milisegundos;
    }

    public EstadoSolver estado() { return estado; }

    /** Literales con signo: positivo = variable verdadera. Solo existe si es SATISFACIBLE. */
    public int[] modelo() { return modelo; }

    public long milisegundos() { return milisegundos; }
}
