package mods.eln.node.voxel;

import org.erisia.cad.Model;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SagMeshJoinTest {
    record Vertex(double x, double y, double z) {}

    @Test
    void sagRingsMatchSupportedBoxesOnBothAxesAndInBothDirections() {
        for (int width = 1; width <= 4; width++) {
            for (boolean alongX : new boolean[]{true, false}) {
                for (int direction : new int[]{-1, 1}) {
                    int a = direction > 0 ? 4 : 12;
                    int b = 16 - a;
                    double[][] points = {point(alongX, a, 0), point(alongX, 8, -2), point(alongX, b, 0)};
                    double[][] tangents = new double[3][];
                    for (int i = 0; i < 3; i++)
                        tangents[i] = alongX ? new double[]{direction, 0, 0} : new double[]{0, 0, direction};
                    Model model = new Model();
                    VoxelGeometry.sweep(model, new CablePart.Drawn(0, 8, points,
                        new double[][]{{0, 1, 0}, {0, 1, 0}, {0, 1, 0}}, tangents),
                        width, 1, null, -1);
                    for (int end : List.of(a, b)) {
                        Set<Vertex> actual = new HashSet<>();
                        model.emit(face -> {
                            for (int i = 0; i < face.xyz.length; i += 3) {
                                double x = face.xyz[i], y = face.xyz[i + 1], z = face.xyz[i + 2];
                                if ((alongX ? x : z) == end) actual.add(new Vertex(x, y, z));
                            }
                        });
                        Set<Vertex> expected = new HashSet<>();
                        for (int across : new int[]{8 - width / 2, 8 + width - width / 2})
                            for (int y : new int[]{0, 1})
                                expected.add(alongX ? new Vertex(end, y, across) : new Vertex(across, y, end));
                        assertEquals(expected, actual, "width=" + width + ", axis X=" + alongX + ", direction=" + direction);
                    }
                }
            }
        }
    }

    private static double[] point(boolean alongX, int along, int y) {
        return alongX ? new double[]{along, y, 8} : new double[]{8, y, along};
    }
}
