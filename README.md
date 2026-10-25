# Reducción polinomial TSP ≤p SAT (Java + Sat4j)

Trabajo práctico de demostración experimental de la NP-completitud.
El programa recibe una instancia del **Problema del Viajante (TSP)** en su versión de decisión
(*¿existe un recorrido que visite todas las ciudades una sola vez, regrese al origen y cueste ≤ K?*),
la **traduce a una fórmula CNF** en formato DIMACS y deja que el SAT-Solver **Sat4j** decida.
Si la fórmula es satisfacible, el programa lee el modelo y reconstruye la ruta.

> **Regla central del enunciado:** el código Java NO resuelve el TSP (nada de fuerza bruta,
> programación dinámica ni metaheurísticas). Solo traduce el problema a lógica y lee la respuesta
> del solver.

---

## 1. Requisitos

| Herramienta | Versión | Para qué |
|---|---|---|
| JDK (con `javac`) | 11 o superior | Compilar y ejecutar |
| Apache Maven | 3.6 o superior | Descarga Sat4j y JUnit, compila y empaqueta |
| Conexión a internet | solo la primera vez | Maven descarga las dependencias |

## 2. Estructura del proyecto

```
tsp-sat/
├── pom.xml                         Configuración de Maven (dependencias y empaquetado)
├── README.md                       Este archivo
├── .gitignore                      Archivos que Git debe ignorar (target/, *.cnf)
├── .vscode/launch.json             Configuraciones de ejecución para VS Code
├── instancias/                     Ejemplos de entrada
│   ├── n3_sat.txt                  N=3, K=6   -> satisfacible
│   ├── n3_insat.txt                N=3, K=5   -> insatisfacible
│   ├── n4_sat.txt                  N=4, K=80  -> satisfacible (el óptimo es 80)
│   └── n4_insat.txt                N=4, K=79  -> insatisfacible
└── src/
    ├── main/java/tsp/
    │   ├── Main.java               Punto de entrada y lectura de argumentos
    │   ├── LectorGrafo.java        Lee la instancia (archivo, consola o aleatoria)
    │   ├── InstanciaTsp.java       Datos del problema: N, matriz de distancias, K
    │   ├── ReductorTspSat.java     MOTOR DE REDUCCIÓN: TSP -> cláusulas CNF
    │   ├── FormulaCnf.java         Guarda las cláusulas y las exporta a DIMACS
    │   ├── EjecutorSat.java        Interfaz con Sat4j (única clase que lo conoce)
    │   ├── EstadoSolver.java       SATISFACIBLE / INSATISFACIBLE / TIEMPO_AGOTADO
    │   ├── ResultadoSolver.java    Estado, modelo y tiempo devueltos por el solver
    │   ├── DecodificadorRuta.java  Modelo booleano -> ruta
    │   ├── ProcesoTspSat.java      Encadena las 3 fases automáticamente
    │   └── PruebasRendimiento.java Genera la tabla de rendimiento del informe
    └── test/java/tsp/
        └── ReduccionTspSatTest.java  Pruebas unitarias (JUnit 5)
```

Flujo de datos:

```
 archivo / consola ──> LectorGrafo ──> InstanciaTsp
                                           │
                              ReductorTspSat (Fase 1)
                                           │  FormulaCnf ──> tsp.cnf (DIMACS)
                                           ▼
                                   EjecutorSat + Sat4j (Fase 2)
                                           │  ResultadoSolver (estado + modelo)
                                           ▼
                              DecodificadorRuta (Fase 3) ──> ruta y costo real
```

---

## 3. Compilar

Desde la carpeta `tsp-sat/`:

```bash
mvn clean package
```

Esto descarga las dependencias, **ejecuta las pruebas unitarias**, compila y genera
`target/tsp-sat.jar` (un jar ejecutable que ya incluye Sat4j).

Si solo quieres el jar sin correr las pruebas:
```bash
mvn clean package -DskipTests
```

---

## 4. Ejecutar

El programa acepta **una** de estas tres formas de entrada.

### 4.1 Archivo de texto
```bash
java -jar target/tsp-sat.jar instancias/n4_sat.txt
```
Puedes sobrescribir el K del archivo: `java -jar target/tsp-sat.jar instancias/n4_sat.txt --k 100`

### 4.2 Distancias por consola
Filas separadas por `;` y valores separados por espacios o comas:
```bash
java -jar target/tsp-sat.jar --distancias "0 10 15 20;10 0 35 25;15 35 0 30;20 25 30 0" --k 80
```

### 4.3 Instancia aleatoria
```bash
java -jar target/tsp-sat.jar --aleatoria 8 --k 40 --distmax 10 --semilla 1
```

### Opciones

| Opción | Descripción | Por defecto |
|---|---|---|
| `--k K` | Umbral de costo. Obligatorio con `--distancias` y `--aleatoria` | el del archivo |
| `--cnf ARCHIVO` | Dónde guardar el DIMACS generado | `tsp.cnf` |
| `--timeout S` | Límite de tiempo del solver en segundos | 300 |
| `--detalle` | Muestra las variables `x(i,t)` verdaderas (el certificado) | desactivado |
| `--distmax D` | Distancia máxima en el modo aleatorio | 10 |
| `--semilla S` | Semilla del generador aleatorio (reproducible) | 1 |
| `--ayuda` | Muestra la ayuda | |

### Formato del archivo de entrada
```
# Las líneas que empiezan con # son comentarios
4              <- N (número de ciudades)
80             <- K (costo máximo permitido)
0 10 15 20     <- fila 0 de la matriz de distancias
10 0 35 25
15 35 0 30
20 25 30 0
```
Las distancias deben ser enteros ≥ 0. La matriz puede ser asimétrica.

### Salidas esperadas

**Caso satisfacible** (`n4_sat.txt`):
```
N=4  K=80
CNF: 280 variables, 1567 cláusulas (reducción en 39 ms) -> tsp.cnf
Sat4j: SATISFACIBLE en 78 ms
Ruta encontrada: 0 -> 1 -> 3 -> 2 -> 0
Costo real: 80 (<= K=80)
```
(La ruta puede salir en el sentido contrario, `0 -> 2 -> 3 -> 1 -> 0`, con el mismo costo;
los tiempos varían según la máquina.)

**Caso insatisfacible** (`n4_insat.txt`):
```
N=4  K=79
CNF: 276 variables, 1549 cláusulas (reducción en 37 ms) -> tsp.cnf
Sat4j: INSATISFACIBLE en 72 ms
NO existe ninguna ruta con costo menor o igual a K
```

---

## 5. Pruebas unitarias

```bash
mvn test
```

Las pruebas (`ReduccionTspSatTest`) verifican que:
- con K igual al óptimo (80) la fórmula es satisfacible y la ruta tiene costo 80;
- con K = 79 y con K = 0 es insatisfacible;
- con K enorme es satisfacible;
- en 20 instancias aleatorias pequeñas (N = 3 a 6, con distancias 0 y matrices asimétricas),
  Sat4j responde igual que la respuesta correcta para K = óptimo − 1, óptimo y óptimo + 1.

> La comprobación "respuesta correcta" usa fuerza bruta **únicamente dentro del archivo de
> pruebas**, para validar la reducción. El programa principal nunca la utiliza.

---

## 6. Pruebas de rendimiento (tabla del informe)

```bash
java -cp target/tsp-sat.jar tsp.PruebasRendimiento --tamanos 3,4,6,8,10,12,14 --factores 0.3,0.5 --timeout 120
```

| Parámetro | Significado | Por defecto |
|---|---|---|
| `--tamanos` | Valores de N a probar (separados por coma) | 3,4,6,8,10 |
| `--factores` | Para cada factor f se usa K = f · N · distmax | 0.3,0.5 |
| `--distmax` | Distancia máxima | 10 |
| `--semilla` | Semilla aleatoria | 1 |
| `--timeout` | Segundos máximos por instancia | 120 |

Imprime una tabla en Markdown con N, K, variables, cláusulas, tiempo de reducción, tiempo de
Sat4j y resultado. Para cada factor se detiene en el primer `TIEMPO_AGOTADO` (el **punto de
quiebre**). Un factor chico acerca K al óptimo y suele dar instancias más difíciles; prueba
varias semillas y repite las mediciones para tener datos más confiables.

---

## 7. Usar VS Code

1. Abre la carpeta `tsp-sat/` (la que contiene `pom.xml`) con la extensión *Extension Pack for Java*.
2. El archivo `.vscode/launch.json` ya trae configuraciones: pulsa **Run and Debug** (Ctrl+Shift+D),
   elige una (por ejemplo *Principal - n4 satisfacible*) y presiona ▶.
3. Si ejecutas `Principal` con el botón ▶ de la esquina sin argumentos, solo verás la ayuda.
   Para pasar argumentos, usa las configuraciones de `launch.json`.

---

## 8. Compilar sin Maven (alternativa)

1. Descarga `org.ow2.sat4j.core-2.3.6.jar` desde [Maven Central](https://repo.maven.apache.org/maven2/org/ow2/sat4j/org.ow2.sat4j.core/2.3.6/).
2. Compila y ejecuta:
```bash
mkdir -p out
javac -encoding UTF-8 -cp org.ow2.sat4j.core-2.3.6.jar -d out src/main/java/tsp/*.java
java -cp out:org.ow2.sat4j.core-2.3.6.jar tsp.Principal instancias/n4_sat.txt
```
(En Windows el separador del classpath es `;` en lugar de `:`.)

---

## 9. Modelado resumido

**Variables**
- `x(i,t)`: la ciudad *i* se visita en el paso *t* (N² variables).
- `c(t,v)`: el costo acumulado de las aristas 0..t es ≥ *v* (variables auxiliares del contador).

**Restricciones**
1. **Visita única:** cada ciudad aparece en exactamente un paso (al menos uno + a lo sumo uno por pares).
2. **Ocupación única:** cada paso tiene exactamente una ciudad.
3. **Costo:** la suma de las N aristas del ciclo (incluida la de regreso) es ≤ K. No basta con
   prohibir aristas sueltas mayores que K, porque el costo es una *suma*; por eso se usa un
   **contador secuencial ponderado** que convierte esa suma en cláusulas.

**Optimizaciones** (todas mantienen la equivalencia lógica)
- Ruptura de simetría: se fija la ciudad 0 en el paso 0 (un ciclo rotado es el mismo ciclo).
- K efectivo: ningún ciclo cuesta más que N · (distancia máxima), así que K se acota a ese valor.
- Cota inferior: al paso *t* le faltan N−1−t aristas de costo ≥ dmin, lo que reduce el tamaño de cada contador.
- Aristas imposibles: se prohíben con una sola cláusula binaria.
- Se omiten cláusulas ya satisfechas por la ciudad fija.
- Memoria: las cláusulas se guardan en un único arreglo de enteros (formato DIMACS en memoria).
- Límite de seguridad: si la instancia generaría demasiadas cláusulas, el programa avisa en vez de agotar la memoria.

Con el ejemplo `n4_sat.txt` estas optimizaciones bajaron la fórmula de 340 variables / 4264 cláusulas
a 280 variables / 1567 cláusulas, con el mismo resultado.

**Complejidad de la reducción:** O(N³ · K) cláusulas. Es polinomial en N para K acotado y
*pseudo*-polinomial en K, porque K se codifica en "unario" mediante el contador.

---

## 10. Cumplimiento del enunciado

| Requisito | Dónde se cumple |
|---|---|
| Java 11+ | `pom.xml` (source/target 11) |
| Prohibido resolver el TSP internamente | Solo `EjecutorSat` + Sat4j deciden; el resto traduce o lee |
| Entrada por archivo o consola | `LectorGrafo` y `Principal` (archivo, `--distancias`, `--aleatoria`) |
| Modularidad | Lector, reductor/CNF, ejecutor del solver y decodificador están en clases separadas |
| Variables booleanas + DIMACS | `ReductorTspSat` y `FormulaCnf` |
| Visita única, ocupación única, costo | `ReductorTspSat` |
| Sat4j | `EjecutorSat` |
| Mensaje UNSAT exacto | `Main` imprime: `NO existe ninguna ruta con costo menor o igual a K` |
| Ruta y costo real si es SAT | `DecodificadorRuta` y `Main` |

---

## 11. Solución de problemas

| Problema | Causa y solución |
|---|---|
| `Unable to access jarfile target/tsp-sat.jar` | Aún no has compilado. Ejecuta `mvn clean package`. |
| `mvn: command not found` | Instala Maven (sección 1). |
| `release version ... not supported` / falta `javac` | Instala el JDK completo (`java-latest-openjdk-devel`), no solo el JRE. |
| Solo aparece el mensaje de uso | Ejecutaste `Main` sin argumentos. Pasa un archivo, `--distancias` o `--aleatoria`. |
| Fallan las pruebas durante `mvn package` | Usa `mvn clean package -DskipTests` y revisa el error con `mvn test`. |
| `La instancia generaría demasiadas cláusulas` | N o K son muy grandes para el contador. Reduce uno de los dos. |
| `TIEMPO_AGOTADO` | Sat4j no terminó dentro de `--timeout`; aumenta el límite o reduce N. Es el punto de quiebre. |
