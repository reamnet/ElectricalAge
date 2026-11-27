package mods.eln.gridnode;

import mods.eln.misc.UtilsClient;
import mods.eln.gl.GL11;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;

import static mods.eln.gl.GL11.*;

/**
 * Created by svein on 25/08/15.
 */
public abstract class GridRender extends TransparentNodeElementRender {
    private final GridDescriptor descriptor;
    private static final Identifier SOLID_TEXTURE = Identifier.fromNamespaceAndPath("eln", "sprites/white.png");
    private final Identifier cableTexture;
    private ArrayList<Catenary> catenaries = new ArrayList<Catenary>();
    /**
     * Shells round the spans that carry a stasis halo (either pole anchored: Baughn 2026-11-08), HALO_LAYERS of them a
     * conductor, each wider and fainter, so the glow fades out to nothing at its edge (Baughn: not a sheath).
     */
    private ArrayList<Catenary> haloCatenaries = new ArrayList<Catenary>();
    /**
     * The halo's slow, aperiodic shimmer (Baughn: not periodic): three phases on frame time (mods.eln.anim) turning at
     * incommensurate rates (4 s, x golden ratio, x silver ratio), each from a random start, summed.
     */
    private final mods.eln.anim.Spin[] haloPhase = {new mods.eln.anim.Spin(90, 0.01).speed(1).randomPhase(),
        new mods.eln.anim.Spin(90 / 1.6180339887, 0.01).speed(1).randomPhase(), new mods.eln.anim.Spin(90 / 2.4142135624, 0.01).speed(1).randomPhase()};
    private float idealRenderingAngle;
    private record Wire(Vec3 start, Vec3 end, boolean halo, GridCableAppearance appearance) { }
    private java.util.List<Wire> geometry = java.util.List.of();
    private int spanGeometryRevision;
    private net.minecraft.world.phys.AABB spanBounds;

    @Override
    public net.minecraft.world.phys.AABB renderBounds() { return spanBounds; }

    /** Changes only when the published cable mesh actually changes. */
    public int getSpanGeometryRevision() { return spanGeometryRevision; }

    public GridRender(TransparentNodeEntity tileEntity, TransparentNodeDescriptor descriptor) {
        super(tileEntity, descriptor);
        this.descriptor = (GridDescriptor) descriptor;

        cableTexture = Identifier.fromNamespaceAndPath("eln", this.descriptor.cableTexture);
    }

    @Override
    public void draw() {
        descriptor.draw(idealRenderingAngle);
        drawCables();
    }

    public void drawCables() {
        glPushAttrib(0xFFFFF);
        try {
            UtilsClient.bindTexture(cableTexture);
            glDisable(GL_CULL_FACE);
            for (Catenary catenary : catenaries) catenary.draw();
            glColor4f(1f, 1f, 1f, 1f);
            drawHalo();
        } finally { glPopAttrib(); }
    }

    /** The angle the server turned this node to (degrees about y). */
    protected float idealRenderingAngle() {
        return idealRenderingAngle;
    }

    static final double CABLE_WIDTH = 0.05;
    /** The halo's shells: from just over the cable out to its edge. */
    static final int HALO_LAYERS = 6;
    static final double HALO_INNER = 0.06, HALO_OUTER = 0.2;

    static double haloWidth(int k) {
        return HALO_INNER + (HALO_OUTER - HALO_INNER) * k / (HALO_LAYERS - 1);
    }

    @Override
    public void refresh(float deltaT) {
        super.refresh(deltaT);
        if (!haloCatenaries.isEmpty()) for (var p : haloPhase) p.step(deltaT);
    }

    /**
     * The stasis halo: a faint blue glow round the conductors of spans with an anchored pole at either end, shimmering
     * slowly (Baughn 2026-11-08: subtle, aperiodic). Drawn after the cables, blended, writing no depth.
     */
    protected void drawHalo() {
        if (haloCatenaries.isEmpty()) return;
        // the shells overlap, so what one sees through the middle is the sum of all of them, at the edge only the
        // outermost: each shell is as faint as the falloff's step there
        float a = (float) (0.16 + 0.045 * Math.sin(Math.toRadians(haloPhase[0].deg())) + 0.035 * Math.sin(Math.toRadians(haloPhase[1].deg()))
            + 0.025 * Math.sin(Math.toRadians(haloPhase[2].deg())));
        UtilsClient.disableLight();
        UtilsClient.enableBlend();
        glDisable(GL_TEXTURE_2D);
        glDepthMask(false);
        glDisable(GL_CULL_FACE);
        for (int i = 0; i < haloCatenaries.size(); i++) {
            int k = i % HALO_LAYERS;
            // (dimmed, Baughn: the shells summed to ~1.6x v1's sheath at the core; now ~0.9x)
            float layer = a * 0.25f * (1f - (float) k / HALO_LAYERS);
            glColor4f(0.47f, 0.71f, 1f, layer);
            haloCatenaries.get(i).draw();
        }
        glColor4f(1f, 1f, 1f, 1f);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_CULL_FACE);
        glDepthMask(true);
        UtilsClient.disableBlend();
        UtilsClient.enableLight();
    }

    protected void drawCatenaries() {
        drawCables();
    }

    private Vec3 readVec(DataInputStream stream) throws IOException {
        float x=stream.readFloat(), y=stream.readFloat(), z=stream.readFloat();
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) throw new IOException("Invalid grid cable endpoint");
        return new Vec3(x,y,z);
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            idealRenderingAngle = stream.readFloat();
            int linkCount = stream.readInt();
            boolean previousVersion = linkCount == GridCableAppearance.PREVIOUS_PACKET_VERSION;
            boolean styled = previousVersion || linkCount == GridCableAppearance.PACKET_VERSION;
            if (styled) linkCount = stream.readInt();
            if (linkCount<0 || linkCount>4096) throw new IOException("Invalid grid span count");
            java.util.List<Wire> incoming = new ArrayList<>();
            for (int i=0;i<linkCount;i++) {
                int wires=stream.readInt();
                if (wires<1 || wires>3) throw new IOException("Invalid grid wire count");
                Vec3[] starts = new Vec3[wires];
                Vec3[] ends = new Vec3[wires];
                for (int wire = 0; wire < wires; wire++) {
                    starts[wire] = readVec(stream);
                    ends[wire] = readVec(stream);
                }
                boolean halo = stream.readBoolean();
                GridCableAppearance appearance = styled ? GridCableAppearance.read(stream, previousVersion) : GridCableAppearance.LEGACY;
                if (wires == 2) {
                    double straight = starts[0].vectorTo(ends[0]).normalize().dot(starts[1].vectorTo(ends[1]).normalize());
                    double cross = starts[0].vectorTo(ends[1]).normalize().dot(starts[1].vectorTo(ends[0]).normalize());
                    if (cross >= straight) {
                        Vec3 end = ends[0];
                        ends[0] = ends[1];
                        ends[1] = end;
                    }
                }
                for (int wire = 0; wire < wires; wire++) incoming.add(new Wire(starts[wire], ends[wire], halo, appearance));
            }
            // Live voltage/status updates reuse the native display lists when spans have not changed.
            if (!incoming.equals(geometry)) {
                ArrayList<Catenary> replacement=new ArrayList<>();
                for (Catenary old : haloCatenaries) old.destroy();
                haloCatenaries.clear();
                for (Wire wire : incoming) {
                    double width = wire.appearance().outsideRadius();
                    replacement.add(new Catenary(wire.start(), wire.end(), width, wire.appearance()));
                    // The AWG half-width reaches sqrt(2) times as far at its square corners.
                    double envelope = wire.appearance().usesAwgGeometry() ? Math.sqrt(2) * width : width;
                    if (wire.halo()) for (int k = 0; k < HALO_LAYERS; k++)
                        haloCatenaries.add(new Catenary(wire.start(), wire.end(), haloWidth(k) + envelope - CABLE_WIDTH));
                }
                for (Catenary old:catenaries) old.destroy();
                catenaries=replacement;geometry=java.util.List.copyOf(incoming);spanGeometryRevision++;
            }
            var at = tileEntity.getBlockPos();
            var bounds = new net.minecraft.world.phys.AABB(at).inflate(3);
            for (Wire wire : incoming) for (Vec3 v : new Vec3[]{wire.start(), wire.end()})
                bounds = bounds.minmax(new net.minecraft.world.phys.AABB(at.getX() + 0.5 + v.x, at.getY() + 0.5 + v.y,
                    at.getZ() + 0.5 + v.z, at.getX() + 0.5 + v.x, at.getY() + 0.5 + v.y, at.getZ() + 0.5 + v.z).inflate(1, 3, 1));
            spanBounds = bounds;
        } catch (IOException e) { e.printStackTrace(); }
    }

    @Override
    public void destructor() {
        for (Catenary catenary:catenaries) catenary.destroy();
        for (Catenary old : haloCatenaries) old.destroy();
        haloCatenaries.clear();
        catenaries.clear();geometry=java.util.List.of();
        super.destructor();
    }

    @Override
    public boolean cameraDrawOptimisation() {
        return false;
    }

    private class Catenary {
        final int list;

        final Vec3 origin = new Vec3(0, 0, 0);
        final int box[] = {
            3, 7, 5, 3, 5, 1,
            4, 8, 6, 4, 6, 2,
            1, 6, 5, 1, 2, 6,
            3, 8, 7, 3, 4, 8
        };
        // Maps box coordinates (above) to texture coordinates.
        final int boxTex[] = {
            0, 0, // 1
            0, 1, // 2
            0, 1, // 3
            0, 0, // 4
            1, 0, // 5
            1, 1, // 6
            1, 1, // 7
            1, 0, // 8
        };
        private final double cableWidth;
        private final GridCableAppearance appearance;

        // TODO: Lighting and such should not be the same across the entire cable.
        // Probably need make physical "cable" blocks, to make minecraft cooperate.
        // The individual blocks should do the rendering.
        // ...later. Much later.
        Catenary(Vec3 start, Vec3 end, double width) { this(start,end,width,null); }

        Catenary(Vec3 start, Vec3 end, double width, GridCableAppearance appearance) {
            this.cableWidth = width;
            this.appearance = appearance;
            // These are the central vertices of the catenary.
            Vec3[] catenary = getConnectionCatenary(start, end);

            list = glGenLists(1);
            glNewList(list, GL_COMPILE);
            glBegin(GL_TRIANGLES);

            if (appearance != null && appearance.usesAwgGeometry()) {
                drawAwg(start, end, catenary);
            } else if (start.x == end.x && start.z == end.z) {
                // Poles right on top of each other? No catenaries here.
                drawBox(spread(start, end), spread(end, start));
            } else {
                // Four points at the starting pole.
                Vec3 previous[] = spread(start, catenary[0]);
                for (int i = 0; i < catenary.length - 1; i++) {
                    // Some more points at intermediate junctions.
                    Vec3 next[] = spread(catenary[i], catenary[i + 1]);
                    drawBox(previous, next);
                    previous = next;
                }
                // Finally, at the ending pole. We'll just translate the second-to-last points to fit.
                Vec3 last[] = translate(previous, catenary[catenary.length - 2].vectorTo(catenary[catenary.length - 1]));
                drawBox(previous, last);
            }
            glEnd();
            glEndList();
        }

        private void drawAwg(Vec3 start, Vec3 end, Vec3[] catenary) {
            if (start.distanceToSqr(end) < 1e-12) return;
            boolean jacketed = appearance.surface() == GridCableAppearance.Surface.JACKET;
            ArrayList<Vec3> points = new ArrayList<>();
            points.add(start);
            if (start.x != end.x || start.z != end.z)
                for (int i = 0; i < catenary.length - 1; i++)
                    points.add(catenary[i]);
            points.add(end);
            if (jacketed) {
                // Keep stripped tips short, even when the first or last sag segment is short.
                points.add(1, strippedEnd(points.getFirst(), points.get(1)));
                points.add(points.size() - 1, strippedEnd(points.getLast(), points.get(points.size() - 2)));
            }

            // Adjacent faces share both the center and the cross section at every sample.
            Vec3[] previous = awgSection(points, 0, appearance.radius());
            setTint(appearance.tint());
            triangle(previous[0], previous[3], previous[1]);
            triangle(previous[0], previous[2], previous[3]);
            for (int i = 1; i < points.size(); i++) {
                boolean covered = jacketed && i > 1 && i < points.size() - 1;
                double width = covered ? cableWidth : appearance.radius();
                Vec3[] next = awgSection(points, i, width);
                if (jacketed && (i == 2 || i == points.size() - 1)) {
                    Vec3[] joint = awgSection(points, i - 1, width);
                    setTint(GridCableAppearance.JACKET);
                    joinSections(previous, joint);
                    previous = joint;
                }
                setTint(covered ? GridCableAppearance.JACKET : appearance.tint());
                joinSections(previous, next);
                previous = next;
            }
            triangle(previous[0], previous[1], previous[3]);
            triangle(previous[0], previous[3], previous[2]);
        }

        private Vec3 strippedEnd(Vec3 terminal, Vec3 sample) {
            Vec3 delta = sample.subtract(terminal);
            double fraction = Math.min(.4, .075 / delta.length());
            return terminal.add(delta.scale(fraction));
        }

        private Vec3[] awgSection(java.util.List<Vec3> points, int i, double width) {
            Vec3 center = points.get(i);
            Vec3 tangent = points.get(Math.min(i + 1, points.size() - 1))
                .subtract(points.get(Math.max(i - 1, 0))).normalize();
            Vec3 horizontal = new Vec3(-tangent.z, 0, tangent.x);
            Vec3 right = (horizontal.lengthSqr() < 1e-12 ? new Vec3(1, 0, 0) : horizontal.normalize()).scale(width);
            Vec3 up = tangent.cross(right).normalize().scale(width);
            Vec3 corner = center.add(right);
            Vec3 opposite = center.subtract(right);
            return new Vec3[]{corner.add(up), opposite.add(up), corner.subtract(up), opposite.subtract(up)};
        }

        private static final int[] RING = {0, 1, 3, 2};

        private void joinSections(Vec3[] a, Vec3[] b) {
            for (int i = 0; i < RING.length; i++) {
                int p = RING[i];
                int q = RING[(i + 1) % RING.length];
                triangle(a[p], a[q], b[q]);
                triangle(a[p], b[q], b[p]);
            }
        }

        private void triangle(Vec3 a, Vec3 b, Vec3 c) {
            Vec3 normal = b.subtract(a).cross(c.subtract(a));
            double inverseLength = 1 / normal.length();
            glNormal3f((float)(normal.x * inverseLength), (float)(normal.y * inverseLength), (float)(normal.z * inverseLength));
            glVertex3f((float)a.x, (float)a.y, (float)a.z);
            glVertex3f((float)b.x, (float)b.y, (float)b.z);
            glVertex3f((float)c.x, (float)c.y, (float)c.z);
        }

        private void setTint(int tint) {
            glColor4f(((tint >>> 16) & 255)/255f, ((tint >>> 8) & 255)/255f, (tint & 255)/255f, 1f);
        }

        private void drawBox(Vec3[] from, Vec3[] to) {
            Vec3 v[] = new Vec3[]{from[0], from[1], from[2], from[3], to[0], to[1], to[2], to[3]};

            // Figure out the lighting.
//            Vec3 middle = new Vec3(0, 0, 0);
//            for (Vec3 x : v) {
//                middle = middle.add(x.x, x.y, x.z);
//            }
//            middle = multiply(middle, v.length).add(tileEntity.getBlockPos().getX(), tileEntity.getBlockPos().getY(), tileEntity.getBlockPos().getZ());
//            glColor3d(
//                    139 / 255.0,
//                    69 / 255.0,
//                    19 / 255.0);

            // And let's draw it all.
            for (int i = 0; i < box.length; i++) {
                final int bc = box[i] - 1;
                glTexCoord2f(boxTex[bc * 2], boxTex[bc * 2 + 1]);
                glVertex3f((float) v[bc].x, (float) v[bc].y, (float) v[bc].z);
            }
        }

        private Vec3[] translate(Vec3[] start, Vec3 delta) {
            Vec3 ret[] = new Vec3[start.length];
            for (int i = 0; i < start.length; i++) {
                ret[i] = start[i].add(delta.x, delta.y, delta.z);
            }
            return ret;
        }

        private Vec3[] spread(Vec3 a, Vec3 b) {
            // We want to draw a box-shaped cable following the catenary.
            // To start with, compute a vector perpendicular to the first
            // catenary segment, then rotate it around the catenary to form four points.
            final Vec3 delta = b.vectorTo(a);
            // This is just to copy.
            // We don't care what r is, so long as it's linearly independent of delta.
            // 1.12: Vec3 is immutable (1.7.10 rotateAroundY/X mutated r in place)
            final Vec3 r = delta.normalize().yRot(1).xRot(1);
            // This gives us one vector which is perpendicular to delta.
            final Vec3 x1 = multiply(delta.cross(r).normalize(), cableWidth);
            // And this, another, perpendicular to delta and x1.
            final Vec3 y1 = multiply(delta.cross(x1).normalize(), cableWidth);
            // Now just invert those to get the other two corners.
            final Vec3 x2 = negate(x1), y2 = negate(y1);
            return translate(new Vec3[]{x1, y1, y2, x2}, a);
        }

        private Vec3 negate(Vec3 v) {
            return v.vectorTo(origin);
        }

        Vec3 multiply(Vec3 a, double b) {
            return new Vec3(
                a.x * b,
                a.y * b,
                a.z * b
            );
        }

        // This function borrowed from Immersive Engineering. Check them out!
        private Vec3[] getConnectionCatenary(Vec3 start, Vec3 end) {
            // TODO: Thermal heating.
            final double slack = 1.005;
            final int vertices = 16;

            double dx = (end.x) - (start.x);
            double dy = (end.y) - (start.y);
            double dz = (end.z) - (start.z);
            double dw = Math.sqrt(dx * dx + dz * dz);
            double k = Math.sqrt(dx * dx + dy * dy + dz * dz) * slack;
            double l = 0;
            int limiter = 0;
            while (limiter < 300) {
                limiter++;
                l += 0.01;
                if (Math.sinh(l) / l >= Math.sqrt(k * k - dy * dy) / dw)
                    break;
            }
            double a = dw / 2 / l;
            double p = (0 + dw - a * Math.log((k + dy) / (k - dy))) * 0.5;
            double q = (dy + 0 - k * Math.cosh(l) / Math.sinh(l)) * 0.5;

            Vec3[] vex = new Vec3[vertices];

            for (int i = 0; i < vertices; i++) {
                float n1 = (i + 1) / (float) vertices;
                double x1 = 0 + dx * n1;
                double z1 = 0 + dz * n1;
                double y1 = a * Math.cosh(((Math.sqrt(x1 * x1 + z1 * z1)) - p) / a) + q;
                vex[i] = new Vec3(start.x + x1, start.y + y1, start.z + z1);
            }
            return vex;
        }

        public void draw() {
            if (appearance != null) {
                UtilsClient.bindTexture(appearance.surface() == GridCableAppearance.Surface.LEGACY_TEXTURE ? cableTexture : SOLID_TEXTURE);
                if (!appearance.usesAwgGeometry()) setTint(appearance.tint());
            }
            glCallList(list);
        }

        public void destroy() {
            glDeleteLists(list, 1);
        }
    }
}
