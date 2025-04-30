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
    private static final int PARALLEL_THRESHOLD = 32;

    public PhysicsEngine(List<CelestialBody> bodies) {
        this.bodies = Objects.requireNonNull(bodies, "bodies must not be null");
        int threads = Runtime.getRuntime().availableProcessors();
        this.executor = Executors.newFixedThreadPool(threads);
    }

    public void setEnablePerturbations(boolean enable) { this.enablePerturbations = enable; }

    public void update(float dtDays) {
        if (dtDays <= 0f || bodies.isEmpty()) return;

        // Fase 1: calcula novos estados em buffers temporários
        int n = bodies.size();
        PVector[] nextPos = new PVector[n];
        PVector[] nextVel = new PVector[n];

        // Decide entre paralelo ou seqüencial
        if (n >= PARALLEL_THRESHOLD) {
            List<Callable<Void>> tasks = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                final int idx = i;
                tasks.add(() -> {
                    CelestialBody body = bodies.get(idx);
                    // 1) Kepler puro
                    body.propagateKepler(dtDays);

                    // 2) cálculo de perturbação (com estado antigo)
                    PVector aPert = enablePerturbations
                        ? computePerturbations(body)
                        : new PVector(0,0,0);

                    // 3) monta estado futuro
                    PVector vNew = PVector.add(body.getVelocityAU(), PVector.mult(aPert, dtDays));
                    PVector pNew = PVector.add(body.getPositionAU(),
                                               PVector.mult(aPert, 0.5f * dtDays * dtDays));
                    nextVel[idx] = vNew;
                    nextPos[idx] = pNew;
                    return null;
                });
            }
            try {
                executor.invokeAll(tasks);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("[PhysicsEngine] Interrupted: " + e.getMessage());
            }
        } else {
            // Seqüencial
            for (int i = 0; i < n; i++) {
                CelestialBody body = bodies.get(i);
                body.propagateKepler(dtDays);
                PVector aPert = enablePerturbations
                    ? computePerturbations(body)
                    : new PVector(0,0,0);
                nextVel[i] = PVector.add(body.getVelocityAU(), PVector.mult(aPert, dtDays));
                nextPos[i] = PVector.add(body.getPositionAU(),
                                         PVector.mult(aPert, 0.5f * dtDays * dtDays));
            }
        }

        // Fase 2: atualiza de verdade
        for (int i = 0; i < n; i++) {
            bodies.get(i).getVelocityAU().set(nextVel[i]);
            bodies.get(i).getPositionAU().set(nextPos[i]);
        }
    }

    private PVector computePerturbations(CelestialBody self) {
        PVector aTotal = new PVector();
        PVector dr     = new PVector();
        PVector selfPos= self.getPositionAU();
        CelestialBody central = self.getCentralBody();

        for (CelestialBody other : bodies) {
            if (other == self || other == central) continue;
            dr.set(other.getPositionAU()).sub(selfPos);
            float r2 = dr.magSq();
            if (r2 < 1e-12f) continue;
            float invR3 = 1.0f / (r2 * sqrt(r2));
            float factor= G_DAY * other.getMassSolar() * invR3;
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

