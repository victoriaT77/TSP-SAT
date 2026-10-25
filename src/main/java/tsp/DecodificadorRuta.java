package tsp;

/**
 * Fase 3: interpreta el modelo booleano (certificado) entregado por el solver
 * y reconstruye la ruta. No busca nada: solo lee qué variables x(i,t) son verdaderas.
 */
public final class DecodificadorRuta {

    private final ReductorTspSat reductor;
    private final int n;

    public DecodificadorRuta(ReductorTspSat reductor) {
        this.reductor = reductor;
        this.n = reductor.numNodos();
    }

    /** @return ruta donde ruta[t] es la ciudad visitada en el paso t */
    public int[] decodificar(int[] modelo) {
        boolean[] verdadera = tablaDeVerdad(modelo);
        int[] ruta = new int[n];
        for (int paso = 0; paso < n; paso++) {
            int encontrada = -1;
            for (int ciudad = 0; ciudad < n; ciudad++) {
                if (verdadera[reductor.variableCiudad(ciudad, paso)]) {
                    if (encontrada != -1) {
                        throw new IllegalStateException("Hay dos ciudades en el paso " + paso);
                    }
                    encontrada = ciudad;
                }
            }
            if (encontrada == -1) {
                throw new IllegalStateException("No hay ninguna ciudad en el paso " + paso);
            }
            ruta[paso] = encontrada;
        }
        return ruta;
    }

    /** Lista las variables x(ciudad,paso) asignadas como verdaderas (para mostrar el certificado). */
    public String variablesVerdaderas(int[] modelo) {
        boolean[] verdadera = tablaDeVerdad(modelo);
        StringBuilder texto = new StringBuilder();
        for (int paso = 0; paso < n; paso++) {
            for (int ciudad = 0; ciudad < n; ciudad++) {
                if (verdadera[reductor.variableCiudad(ciudad, paso)]) {
                    texto.append("x(").append(ciudad).append(',').append(paso).append(") ");
                }
            }
        }
        return texto.toString().trim();
    }

    private boolean[] tablaDeVerdad(int[] modelo) {
        boolean[] verdadera = new boolean[reductor.totalVariables() + 1];
        for (int literal : modelo) {
            if (literal > 0 && literal < verdadera.length) {
                verdadera[literal] = true;
            }
        }
        return verdadera;
    }
}
