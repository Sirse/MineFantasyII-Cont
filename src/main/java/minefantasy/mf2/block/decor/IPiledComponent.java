package minefantasy.mf2.block.decor;

/** An item that is set down in the world as a pile of its kind, as {@link BlockComponent} lays it out. */
public interface IPiledComponent {

    /** The kind of pile it makes, or null if it makes none. */
    String getPileType();

    /** The texture the pile is drawn with. */
    String getPileTexture();
}
