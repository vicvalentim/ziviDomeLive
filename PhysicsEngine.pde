import processing.core.PVector;
import java.util.*;
import java.util.concurrent.*;

/**
 * PhysicsEngine — cálculo híbrido em unidades físicas (AU, dias, M☉),
 * usando solver Kepleriano + perturbações, tudo em paralelo.
 */
public class PhysicsEngine {

    private final List<CelestialBody> bodies;
    private final ExecutorService executor;
    private boolean enablePerturbations = false;

    public PhysicsEngine(List<CelestialBody> bodies) {
        this.bodies = bodies;
        int threads = Runtime.getRuntime().availableProcessors();
        this.executor = Executors.newFixedThreadPool(threads);
    }

    /**
     * Liga/desliga as perturbações N-Corpos.
     */
    public void setEnablePerturbations(boolean enable) {
        this.enablePerturbations = enable;
    }

    public boolean isEnablePerturbations() {
        return enablePerturbations;
    }

    /**
     * Avança todos os corpos em dtDays dias.
     */
    public void update(float dtDays) {
        if (bodies == null || bodies.isEmpty() || dtDays <= 0f) return;

        List<Callable<Void>> tasks = new ArrayList<>();
        for (CelestialBody body : bodies) {
            tasks.add(() -> {
                // 1) Movimento kepleriano puro (delegado ao próprio corpo)
                body.propagateKepler(dtDays);

                if (enablePerturbations) {
                    // 2) Perturbações gravitacionais
                    PVector aPert = computePerturbations(body);

                    // 3) Correção de velocidade e posição (Verlet simplificado)
                    body.getVelocityAU().add(PVector.mult(aPert, dtDays));
                    body.getPositionAU().add(PVector.mult(aPert, 0.5f * dtDays * dtDays));
                }

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

    private PVector computePerturbations(CelestialBody self) {
        PVector aTotal = new PVector();
        PVector dr = new PVector();
        PVector selfPos = self.getPositionAU();
        CelestialBody central = self.getCentralBody(); // referência

        for (CelestialBody other : bodies) {
            if (other == self || other == central) continue; // <<< IGNORA self e centralBody

            dr.set(other.getPositionAU());
            dr.sub(selfPos);

            float r2 = dr.magSq();
            if (r2 < 1e-12f) continue; // evita singularidade

            float invR3 = 1.0f / (r2 * (float)Math.sqrt(r2));
            float factor = G_DAY * other.getMassSolar() * invR3;
            aTotal.add(PVector.mult(dr, factor));
        }
        return aTotal;
    }

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
