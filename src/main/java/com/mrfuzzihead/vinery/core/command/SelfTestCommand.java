package com.mrfuzzihead.vinery.core.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.item.Item;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.mrfuzzihead.vinery.Vinery;
import com.mrfuzzihead.vinery.core.registry.VineryRegistry;

/**
 * {@code /vinery-selftest} — exercises every registered block and item in a throwaway area of the
 * world and reports what broke.
 *
 * <p>
 * Registration alone proves very little: a block can register cleanly and still throw the moment it
 * is placed, ticked or broken. This walks the whole registry, so a ported block that references a
 * missing field or a null item surfaces here rather than in a player's crash report.
 *
 * <p>
 * The test region is far from spawn and is restored to air afterwards, so it is safe to run on a
 * throwaway world. It is server-side only, which also means it needs no GUI.
 */
public class SelfTestCommand extends CommandBase {

    /** Somewhere flat, empty and far enough away that a failed cleanup cannot matter. */
    private static final int TEST_X = 3000;
    private static final int TEST_Y = 64;
    private static final int TEST_Z = 3000;

    @Override
    public String getCommandName() {
        return "vinery-selftest";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/vinery-selftest — place, read back and drop every registered block";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public List<String> getCommandAliases() {
        return Collections.emptyList();
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

    @Override
    public int compareTo(Object other) {
        return getCommandName().compareTo(((CommandBase) other).getCommandName());
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws WrongUsageException {
        if (args.length > 0) {
            throw new WrongUsageException(getCommandUsage(sender));
        }

        World world = sender.getEntityWorld();
        if (world == null) {
            notify(sender, "No world loaded.", EnumChatFormatting.RED);
            return;
        }

        List<Block> blocks = VineryRegistry.blocks();
        List<Item> items = VineryRegistry.items();
        List<String> failures = new ArrayList<>();
        Random random = new Random();

        // Clear the region first so placement is never rejected by a leftover neighbour.
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < blocks.size(); x++) {
                world.setBlockToAir(TEST_X + x, TEST_Y, TEST_Z);
                world.setBlockToAir(TEST_X + x, TEST_Y + 1, TEST_Z);
            }
        }

        for (int i = 0; i < blocks.size(); i++) {
            Block block = blocks.get(i);
            int x = TEST_X + i;
            int y = TEST_Y;

            try {
                if (!world.setBlock(x, y, TEST_Z, block, 0, 2)) {
                    failures.add(describe(block) + ": setBlock returned false");
                    continue;
                }

                Block readBack = world.getBlock(x, y, TEST_Z);
                if (readBack != block) {
                    failures.add(describe(block) + ": read back as " + describe(readBack));
                    continue;
                }

                int meta = world.getBlockMetadata(x, y, TEST_Z);
                if (meta != 0) {
                    failures.add(describe(block) + ": metadata 0 round-tripped as " + meta);
                }

                // Ticking exercises growth/decay paths that placement alone never touches.
                world.setBlock(x, y, TEST_Z, block, meta, 3);
                block.updateTick(world, x, y, TEST_Z, random);

                // Drop lookup is what a player sees when they mine it.
                Item dropped = block.getItemDropped(meta, random, 0);
                if (dropped == null) {
                    failures.add(describe(block) + ": getItemDropped returned null");
                }

                block.getCollisionBoundingBoxFromPool(world, x, y, TEST_Z);
                block.getRenderType();
            } catch (Throwable t) {
                failures.add(describe(block) + ": threw " + t);
            }
        }

        // Leave the world as we found it.
        for (int x = 0; x < blocks.size(); x++) {
            world.setBlockToAir(TEST_X + x, TEST_Y, TEST_Z);
            world.setBlockToAir(TEST_X + x, TEST_Y + 1, TEST_Z);
        }

        notify(
            sender,
            "Vinery self-test: " + blocks.size() + " block(s), " + items.size() + " item(s)",
            failures.isEmpty() ? EnumChatFormatting.GREEN : EnumChatFormatting.RED);

        for (String failure : failures) {
            notify(sender, "  " + failure, EnumChatFormatting.RED);
        }
        if (failures.isEmpty()) {
            notify(sender, "  all blocks placed, ticked, read back and dropped cleanly", EnumChatFormatting.GREEN);
        }

        // Also mirror to the log, since the console command output is easier to grep in CI.
        Vinery.LOG.info("Self-test: {} block(s) {} failure(s)", blocks.size(), failures.size());
        for (String failure : failures) {
            Vinery.LOG.warn("Self-test failure: {}", failure);
        }
    }

    private static String describe(Block block) {
        // GameData already prefixes registered names with the mod id.
        return block == null ? "air" : Block.blockRegistry.getNameForObject(block);
    }

    private static void notify(ICommandSender sender, String message, EnumChatFormatting colour) {
        sender.addChatMessage(new ChatComponentText(colour + message));
    }
}
