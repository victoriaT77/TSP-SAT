package tsp;

import java.io.IOException;
import java.nio.file.Path;

import org.sat4j.minisat.SolverFactory;
import org.sat4j.reader.DimacsReader;
import org.sat4j.reader.ParseFormatException;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

/**
 * Interfaz con la librería Sat4j: carga un archivo DIMACS CNF y le pide
 * al solver que decida si la fórmula es satisfacible.
 * Esta es la ÚNICA clase que conoce a Sat4j.
 */
public final class EjecutorSat {

    public ResultadoSolver resolver(Path archivoCnf, int timeoutSegundos) throws IOException {
        ISolver solver = SolverFactory.newDefault();
        solver.setTimeout(timeoutSegundos);
        org.sat4j.reader.Reader lector = new DimacsReader(solver);

        long inicio = System.nanoTime();
        try {
            IProblem problema = lector.parseInstance(archivoCnf.toString());
            if (problema.isSatisfiable()) {
                return new ResultadoSolver(EstadoSolver.SATISFACIBLE, problema.model(), milisDesde(inicio));
            }
            return new ResultadoSolver(EstadoSolver.INSATISFACIBLE, null, milisDesde(inicio));
        } catch (ContradictionException e) {
            // Sat4j detecta la contradicción ya al cargar las cláusulas
            return new ResultadoSolver(EstadoSolver.INSATISFACIBLE, null, milisDesde(inicio));
        } catch (TimeoutException e) {
            return new ResultadoSolver(EstadoSolver.TIEMPO_AGOTADO, null, milisDesde(inicio));
        } catch (ParseFormatException e) {
            throw new IOException("Archivo DIMACS inválido: " + e.getMessage(), e);
        }
    }

    private static long milisDesde(long inicioNano) {
        return (System.nanoTime() - inicioNano) / 1_000_000;
    }
}
