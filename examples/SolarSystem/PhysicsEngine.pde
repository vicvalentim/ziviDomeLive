import processing.core.PVector;
import java.util.*;

/**
 * PhysicsEngine — cálculo híbrido em unidades físicas (AU, dias, M☉),
 * usando solver Kepleriano + perturbações determinísticas.
 */
public class PhysicsEngine {
    private final List<CelestialBody> bodies;
    private final double[][] propagatedPositions;
    private final double[][] nextPositions;
    private final double[][] nextVelocities;
    private final double[][] perturbations;
    private boolean enablePerturbations = true;

    public PhysicsEngine(List<CelestialBody> bodies) {
        this.bodies = Objects.requireNonNull(bodies, "bodies must not be null");
        int bodyCount = bodies.size();
        this.propagatedPositions = new double[bodyCount][3];
        this.nextPositions = new double[bodyCount][3];
        this.nextVelocities = new double[bodyCount][3];
        this.perturbations = new double[bodyCount][3];
    }

    public void setEnablePerturbations(boolean enable) { this.enablePerturbations = enable; }

    public void update(double dtDays) {
        if (dtDays <= 0.0 || bodies.isEmpty()) return;

        // Fase 1: propaga todos os corpos antes de calcular perturbações.
        // A ordem da lista preserva a hierarquia Sol -> planetas -> luas.
        int n = bodies.size();
        for (CelestialBody body : bodies) {
            body.propagateKepler(dtDays);
        }

        // Fase 2: captura um snapshot coerente do estado propagado.
        for (int i = 0; i < n; i++) {
            PVector position = bodies.get(i).getPositionAU();
            propagatedPositions[i][0] = position.x;
            propagatedPositions[i][1] = position.y;
            propagatedPositions[i][2] = position.z;
        }

        // Fase 3: calcula os estados futuros sem modificar o snapshot.
        for (int i = 0; i < n; i++) {
            CelestialBody body = bodies.get(i);
            double[] aPert = perturbations[i];
            aPert[0] = 0.0;
            aPert[1] = 0.0;
            aPert[2] = 0.0;
            if (enablePerturbations) {
                computePerturbations(i, propagatedPositions, aPert);
            }
            PVector velocity = body.getVelocityAU();
            double halfDtSquared = 0.5 * dtDays * dtDays;
            for (int axis = 0; axis < 3; axis++) {
                double currentVelocity = axis == 0 ? velocity.x : axis == 1 ? velocity.y : velocity.z;
                nextVelocities[i][axis] = currentVelocity + aPert[axis] * dtDays;
                nextPositions[i][axis] = propagatedPositions[i][axis] + aPert[axis] * halfDtSquared;
            }
        }

        // Fase 4: publica o frame simulado de uma só vez.
        for (int i = 0; i < n; i++) {
            bodies.get(i).getVelocityAU().set(
                (float) nextVelocities[i][0],
                (float) nextVelocities[i][1],
                (float) nextVelocities[i][2]);
            bodies.get(i).getPositionAU().set(
                (float) nextPositions[i][0],
                (float) nextPositions[i][1],
                (float) nextPositions[i][2]);
        }
    }

    private void computePerturbations(int selfIndex,
                                      double[][] positions,
                                      double[] destination) {
        CelestialBody self = bodies.get(selfIndex);
        double[] selfPos = positions[selfIndex];
        CelestialBody central = self.getCentralBody();

        for (int i = 0; i < bodies.size(); i++) {
            CelestialBody other = bodies.get(i);
            if (other == self || other == central) continue;
            double dx = positions[i][0] - selfPos[0];
            double dy = positions[i][1] - selfPos[1];
            double dz = positions[i][2] - selfPos[2];
            double r2 = dx * dx + dy * dy + dz * dz;
            if (r2 < 1e-24) continue;
            double invR3 = 1.0 / (r2 * Math.sqrt(r2));
            double factor = G_DAY * other.getMassSolar() * invR3;
            destination[0] += dx * factor;
            destination[1] += dy * factor;
            destination[2] += dz * factor;
        }
    }

    public void dispose() {
        // O engine não possui threads nem recursos gráficos.
    }
}
