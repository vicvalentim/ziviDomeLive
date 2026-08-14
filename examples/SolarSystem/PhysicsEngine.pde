import processing.core.PVector;
import java.util.*;

/**
 * PhysicsEngine — cálculo híbrido em unidades físicas (AU, dias, M☉),
 * usando solver Kepleriano + perturbações determinísticas.
 */
public class PhysicsEngine {
    private final List<CelestialBody> bodies;
    private boolean enablePerturbations = true;

    public PhysicsEngine(List<CelestialBody> bodies) {
        this.bodies = Objects.requireNonNull(bodies, "bodies must not be null");
    }

    public void setEnablePerturbations(boolean enable) { this.enablePerturbations = enable; }

    public void update(float dtDays) {
        if (dtDays <= 0f || bodies.isEmpty()) return;

        // Fase 1: propaga todos os corpos antes de calcular perturbações.
        // A ordem da lista preserva a hierarquia Sol -> planetas -> luas.
        int n = bodies.size();
        for (CelestialBody body : bodies) {
            body.propagateKepler(dtDays);
        }

        // Fase 2: captura um snapshot coerente do estado propagado.
        PVector[] propagatedPositions = new PVector[n];
        for (int i = 0; i < n; i++) {
            propagatedPositions[i] = bodies.get(i).getPositionAU().copy();
        }

        // Fase 3: calcula os estados futuros sem modificar o snapshot.
        PVector[] nextPos = new PVector[n];
        PVector[] nextVel = new PVector[n];
        for (int i = 0; i < n; i++) {
            CelestialBody body = bodies.get(i);
            PVector aPert = enablePerturbations
                ? computePerturbations(i, propagatedPositions)
                : new PVector();
            nextVel[i] = PVector.add(body.getVelocityAU(), PVector.mult(aPert, dtDays));
            nextPos[i] = PVector.add(propagatedPositions[i],
                                     PVector.mult(aPert, 0.5f * dtDays * dtDays));
        }

        // Fase 4: publica o frame simulado de uma só vez.
        for (int i = 0; i < n; i++) {
            bodies.get(i).getVelocityAU().set(nextVel[i]);
            bodies.get(i).getPositionAU().set(nextPos[i]);
        }
    }

    private PVector computePerturbations(int selfIndex, PVector[] positions) {
        CelestialBody self = bodies.get(selfIndex);
        PVector aTotal = new PVector();
        PVector dr     = new PVector();
        PVector selfPos= positions[selfIndex];
        CelestialBody central = self.getCentralBody();

        for (int i = 0; i < bodies.size(); i++) {
            CelestialBody other = bodies.get(i);
            if (other == self || other == central) continue;
            dr.set(positions[i]).sub(selfPos);
            float r2 = dr.magSq();
            if (r2 < 1e-12f) continue;
            float invR3 = 1.0f / (r2 * sqrt(r2));
            float factor= G_DAY * other.getMassSolar() * invR3;
            aTotal.add(PVector.mult(dr, factor));
        }
        return aTotal;
    }

    public void dispose() {
        // O engine não possui threads nem recursos gráficos.
    }
}
