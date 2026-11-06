package mods.eln.gametest;

/** The existing whole-catalog render/menu contracts also guard incoming adapter integration. */
final class ForkPatchClientSmokeCase implements TestCase {
    @Override public String id() { return "forkpatch_client_catalog_publication_menus_and_items"; }
    @Override public int width() { return 24; }
    @Override public int height() { return 12; }
    @Override public int depth() { return 24; }
    @Override public int ticks() { return 1; }
    @Override public void build(TestContext ctx) { }
    @Override public void measure(TestContext ctx) {
        new PublishRenderCase().measure(ctx);
        new MenusOpenCase().measure(ctx);
        new ItemDrawCase().measure(ctx);
    }
}
