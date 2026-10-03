package com.mrfuzzihead.vinery.core.block;

/**
 * Implemented by blocks that the {@code /vinery-selftest} command should check for a texture.
 *
 * <p>
 * A block that never calls {@code setBlockTextureName} has a null {@code blockIcon}, so its item
 * form draws nothing in the creative inventory. That is invisible from a dedicated server, because
 * icons only exist on the client — so blocks opt in explicitly and the command asks for the value.
 *
 * <p>
 * The wine rack is the case that motivated this: it draws through a custom box renderer and so
 * never needed a plain cube texture, but it still needs one for its inventory icon.
 */
public interface VineryCheckableBlock {

    /** The texture this block registered, or null/empty when none was set. */
    String textureNameForTest();
}
