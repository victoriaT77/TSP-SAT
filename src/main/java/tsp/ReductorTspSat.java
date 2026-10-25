package tsp;

/**
Motor de reducción polinomial TSP (decisión) <=p SAT.
Variables booleanas
    x(i,t): la ciudad i se visita en el paso t (N*N variables).
    c(t,v): el costo acumulado de las aristas 0..t es >= v (variables auxiliares del "contador secuencial ponderado" que codifica la suma de costos).
Restricciones
    Visita única: cada ciudad aparece en exactamente un paso.
    Ocupación única: cada paso tiene exactamente una ciudad.
    Costo: la suma de las N aristas del ciclo es <= K. La arista del paso t va de la ciudad i (paso t) a la ciudad j (paso t+1 mod N) y cuesta d(i,j).
Optimizaciones (todas preservan la equivalencia lógica)
    Ruptura de simetría: el ciclo no cambia si se rota, así que se fija la ciudad 0 en el paso 0.
    K efectivo: ningún ciclo puede costar más que N * (distancia máxima), así que K se acota a ese valor.
    Cota inferior: al paso t le faltan (N-1-t) aristas que cuestan al menos dmin cada una, por lo que el acumulado en t no puede superar K - (N-1-t)*dmin. Esto reduce el tamaño de cada contador.
    Aristas imposibles: si una arista ya excede ese tope se prohíbe con una sola cláusula binaria.
    Se omiten las cláusulas que ya están satisfechas por la ciudad fija (origen/destino imposibles).
 */
public final class ReductorTspSat {

    /** Límites de seguridad para no agotar la memoria con instancias enormes. */
    private static final long MAX_VARIABLES = 5_000_000L;
    private static final long MAX_CLAUSULAS_ESTIMADAS = 20_000_000L;

    private final InstanciaTsp instancia;
    private final int n;
    private final int umbralEfectivo;
    private final int[] tope;            // tope[t]: mayor valor v del contador c(t,v)
    private final int[] inicioContador;  // id de variable previo al primer contador del paso t
    private final int totalVariables;

    public ReductorTspSat(InstanciaTsp instancia) {
        this.instancia = instancia;
        this.n = instancia.numNodos();

        // K efectivo: un ciclo nunca cuesta más que N * dmax
        long maximoPosible = (long) n * instancia.distanciaMaxima();
        this.umbralEfectivo = (int) Math.min((long) instancia.umbral(), maximoPosible);

        this.tope = new int[n];
        this.inicioContador = new int[n];
        long variables = (long) n * n;
        long clausulas = (long) n * n * n; // aproximación de las cláusulas de ciudad/paso
        for (int t = 0; t < n; t++) {
            long limite = (long) umbralEfectivo - (long) (n - 1 - t) * instancia.distanciaMinima();
            long topePaso = Math.max(1L, limite + 1);
            if (variables + topePaso > MAX_VARIABLES) {
                throw new IllegalArgumentException("La instancia necesita demasiadas variables. Reduzca N o K.");
            }
            tope[t] = (int) topePaso;
            inicioContador[t] = (int) variables;
            variables += topePaso;
            clausulas += (long) n * (n - 1) * 2 * topePaso;
        }
        if (clausulas > MAX_CLAUSULAS_ESTIMADAS) {
            throw new IllegalArgumentException("La instancia generaría unas " + clausulas
                    + " cláusulas (límite " + MAX_CLAUSULAS_ESTIMADAS + "). Reduzca N o K.");
        }
        this.totalVariables = (int) variables;
    }

    public int numNodos() { return n; }
    public int totalVariables() { return totalVariables; }
    public int umbralEfectivo() { return umbralEfectivo; }

    /** Variable x(ciudad, paso). Los ids de DIMACS empiezan en 1. */
    public int variableCiudad(int ciudad, int paso) {
        return ciudad * n + paso + 1;
    }

    /** Variable contador c(paso, valor) con valor en 1..tope[paso]. */
    public int variableContador(int paso, int valor) {
        return inicioContador[paso] + valor;
    }

    /** Ejecuta la reducción completa y devuelve la fórmula CNF. */
    public FormulaCnf generar() {
        FormulaCnf formula = new FormulaCnf(totalVariables);
        agregarVisitaUnica(formula);
        agregarOcupacionUnica(formula);
        fijarCiudadInicial(formula);
        agregarRestriccionCosto(formula);
        return formula;
    }

    /** (1) Cada ciudad se visita exactamente en un paso. */
    private void agregarVisitaUnica(FormulaCnf formula) {
        for (int ciudad = 0; ciudad < n; ciudad++) {
            int[] pasos = new int[n];
            for (int paso = 0; paso < n; paso++) {
                pasos[paso] = variableCiudad(ciudad, paso);
            }
            exactamenteUna(formula, pasos);
        }
    }

    /** (2) En cada paso hay exactamente una ciudad. */
    private void agregarOcupacionUnica(FormulaCnf formula) {
        for (int paso = 0; paso < n; paso++) {
            int[] ciudades = new int[n];
            for (int ciudad = 0; ciudad < n; ciudad++) {
                ciudades[ciudad] = variableCiudad(ciudad, paso);
            }
            exactamenteUna(formula, ciudades);
        }
    }

    /** "Exactamente una" = al menos una (una cláusula) + a lo sumo una (cláusulas por pares). */
    private void exactamenteUna(FormulaCnf formula, int[] variables) {
        formula.agregarClausula(variables);
        for (int a = 0; a < variables.length; a++) {
            for (int b = a + 1; b < variables.length; b++) {
                formula.agregarClausula(-variables[a], -variables[b]);
            }
        }
    }

    /** Ruptura de simetría: la ciudad 0 va en el paso 0 (y en ningún otro). */
    private void fijarCiudadInicial(FormulaCnf formula) {
        formula.agregarClausula(variableCiudad(0, 0));
        for (int ciudad = 1; ciudad < n; ciudad++) {
            formula.agregarClausula(-variableCiudad(ciudad, 0));
        }
        for (int paso = 1; paso < n; paso++) {
            formula.agregarClausula(-variableCiudad(0, paso));
        }
    }

    /**
     * (3) Costo total <= K mediante un contador secuencial ponderado.
     * c(t,v) = "el costo de las aristas 0..t es >= v".
     */
    private void agregarRestriccionCosto(FormulaCnf formula) {
        for (int paso = 0; paso < n; paso++) {
            int siguiente = (paso + 1) % n;
            int topePaso = tope[paso];

            // Monotonía: si el acumulado era >= v antes, sigue siendo >= v ahora.
            if (paso > 0) {
                for (int v = 1; v < tope[paso - 1]; v++) {
                    formula.agregarClausula(-variableContador(paso - 1, v), variableContador(paso, v));
                }
            }

            for (int origen = 0; origen < n; origen++) {
                // Con la ciudad 0 fija en el paso 0, solo estos orígenes son posibles
                if (paso == 0 ? origen != 0 : origen == 0) continue;

                for (int destino = 0; destino < n; destino++) {
                    if (destino == origen) continue;
                    if (siguiente == 0 ? destino != 0 : destino == 0) continue;

                    int peso = instancia.distancia(origen, destino);
                    int noOrigen = -variableCiudad(origen, paso);
                    int noDestino = -variableCiudad(destino, siguiente);

                    // La arista por sí sola ya excede el tope: queda prohibida
                    if (peso >= topePaso) {
                        formula.agregarClausula(noOrigen, noDestino);
                        continue;
                    }
                    // Si se usa la arista, el acumulado es >= peso
                    for (int v = 1; v <= peso; v++) {
                        formula.agregarClausula(noOrigen, noDestino, variableContador(paso, v));
                    }
                    // Si el acumulado previo era >= v, ahora es >= v + peso
                    if (paso > 0 && peso > 0) {
                        for (int v = 1; v < tope[paso - 1]; v++) {
                            formula.agregarClausula(noOrigen, noDestino,
                                    -variableContador(paso - 1, v),
                                    variableContador(paso, Math.min(v + peso, topePaso)));
                        }
                    }
                }
            }
            // El acumulado nunca puede alcanzar el tope (que es "se pasó del límite")
            formula.agregarClausula(-variableContador(paso, topePaso));
        }
    }
}
