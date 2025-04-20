import processing.core.PVector;
import java.util.*;
import java.util.concurrent.*;

/**
 * PhysicsEngine — cálculo híbrido em unidades físicas (AU, dias, M☉),
 * usando solver Kepleriano + perturbações, tudo em paralelo.
 */
public class PhysicsEngine {
    /** G em AU³ / (M☉·dia²) */
    private static final float G = 2.9591220828559093e-4f;

    private final List<CelestialBody> bodies;
    private final ExecutorService executor;

    /**
     * @param bodies lista de todos os corpos (Sun, Planet, Moon), cada um
     *               deve saber seu centralBody e implementar propagateKepler().
     */
    public PhysicsEngine(List<CelestialBody> bodies) {
        this.bodies = bodies;
        int threads = Runtime.getRuntime().availableProcessors();
        this.executor = Executors.newFixedThreadPool(threads);
    }

    /**
     * Avança todos os corpos em dtDays dias — Kepler + perturbações — em paralelo.
     */
    public void update(float dtDays) {
        if (bodies == null || bodies.isEmpty() || dtDays <= 0f) return;

        List<Callable<Void>> tasks = new ArrayList<>();
        for (CelestialBody body : bodies) {
            tasks.add(() -> {
                // 1) Movimento kepleriano puro (delegado ao próprio corpo)
                body.propagateKepler(dtDays);

                // 2) Perturbações gravitacionais
                PVector aPert = computePerturbations(body);

                // 3) Correção de velocidade e posição (Verlet simplificado)
                //    v += a * dt
                body.getVelocityAU().add(PVector.mult(aPert, dtDays));
                //    p += ½ * a * dt²
                body.getPositionAU().add(PVector.mult(aPert, 0.5f * dtDays * dtDays));

                return null;
            });
        }

        try {
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[PhysicsEngine] Atualização interrompida: " + e.getMessage());
        }
    }

    /**
     * Soma a aceleração gravitacional em AU/dia² de todos os demais corpos.
     */
    private PVector computePerturbations(CelestialBody self) {
        PVector aTotal = new PVector();
        PVector dr = new PVector(); // vetor temporário reutilizável
        PVector selfPos = self.getPositionAU();
        
        for (CelestialBody other : bodies) {
            if (other == self) continue;

            dr.set(other.getPositionAU());
            dr.sub(selfPos);
            
            float r2 = dr.magSq();
            if (r2 < 1e-12f) continue;

            float invR3 = 1.0f / (r2 * (float)Math.sqrt(r2));
            float factor = G * other.getMassSolar() * invR3;
            aTotal.add(PVector.mult(dr, factor));
        }
        return aTotal;
    }

    /**
     * Encerra o pool de threads de forma limpa.
     */
    public void dispose() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
