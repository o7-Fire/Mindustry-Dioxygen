package io.anuke.mindustry.game.griefprevention;

import io.anuke.arc.Core;
import io.anuke.arc.func.Cons;
import io.anuke.arc.math.geom.Vector2;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.game.griefprevention.GriefWarnings.TileInfo;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.BlockPart;

import static io.anuke.mindustry.Vars.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

// introducing the worst command system known to mankind
public class CommandHandler {
    public class Context {
        public List<String> args;

        public Context(List<String> args) {
            this.args = args;
        }
    }

    public HashMap<String, Cons<Context>> commands = new HashMap<>();

    public CommandHandler() {
        addCommand("fixpower", this::fixPower);
        addCommand("verbose", this::verbose);
        addCommand("debug", this::debug);
        addCommand("spam", this::spam);
        addCommand("bc", this::broadcast);
        addCommand("tileinfo", this::tileInfo);
		
    }

    public void addCommand(String name, Cons<Context> handler) {
        commands.put(name, handler);
    }

    public void reply(String message) {
        ui.chatfrag.addMessage(message, null);
    }

    public boolean runCommand(String message) {
        if (!message.startsWith("/")) return false;
        String[] args = message.split(" ");
        args[0] = args[0].substring(1);
        Cons<Context> command = commands.get(args[0].toLowerCase());
        if (command == null) return false;
        command.get(new Context(Arrays.asList(args)));
        return true;
    }

    /**
     * Reconnect every power node to everything it can connect to, intended to
     * be used after power griefing incidents.
     * If "redundant" is present as an argument, connect the block even if it is
     * already part of the same power graph.
     */
    public void fixPower(Context ctx) {
        boolean redundant = ctx.args.contains("all");
        griefWarnings.fixer.fixPower(redundant);
        reply("[green]Done");
    }

    public void verbose(Context ctx) {
        if (ctx.args.size() < 2) {
            reply("[scarlet]Not enough arguments");
            reply("Usage: verbose <on|off>");
            return;
        }
        switch (ctx.args.get(1).toLowerCase()) {
            case "on":
                griefWarnings.verbose = true;
                reply("Enabled verbose logging");
                break;
            case "off":
                griefWarnings.verbose = false;
                reply("Disabled verbose logging");
                break;
            default:
                reply("[scarlet]Not enough arguments");
                reply("Usage: verbose <on|off>");
        }
    }

    public void debug(Context ctx) {
        if (ctx.args.size() < 2) {
            reply("[scarlet]Not enough arguments");
            reply("Usage: debug <on|off>");
            return;
        }
        switch (ctx.args.get(1).toLowerCase()) {
            case "on":
                griefWarnings.debug = true;
                reply("Enabled debug logging");
                break;
            case "off":
                griefWarnings.debug = false;
                reply("Disabled debug logging");
                break;
            default:
                reply("[scarlet]Not enough arguments");
                reply("Usage: debug <on|off>");
        }
    }

    public void broadcast(Context ctx) {
        if (ctx.args.size() < 2) {
            reply("[scarlet]Not enough arguments");
            reply("Usage: broadcast <on|off>");
            return;
        }
        switch (ctx.args.get(1).toLowerCase()) {
            case "on":
                griefWarnings.broadcast = true;
                reply("Enabled broadcast logging");
                break;
            case "off":
                griefWarnings.broadcast = false;
                reply("Disabled broadcast logging");
                break;
            default:
                reply("[scarlet]Not enough arguments");
                reply("Usage: broadcast <on|off>");
        }
    }

    public void spam(Context ctx) {
        if (ctx.args.size() < 2) {
            reply("[scarlet]Not enough arguments");
            reply("Usage: spam <on|off>");
            return;
        }
        switch (ctx.args.get(1).toLowerCase()) {
            case "on":
                griefWarnings.verbose = true;
                griefWarnings.debug = true;
                reply("Enabled verbose and debug logging");
                break;
            case "off":
                griefWarnings.verbose = false;
                griefWarnings.debug = false;
                reply("Disabled verbose and debug logging");
                break;
            default:
                reply("[scarlet]Not enough arguments");
                reply("Usage: spam <on|off>");
        }
    }

    /**
     * Get stored information for the tile under the cursor
     */
    public void tileInfo(Context ctx) {
        Vector2 vec = Core.input.mouseWorld(Core.input.mouseX(), Core.input.mouseY());
        Tile tile = world.tile(world.toTile(vec.x), world.toTile(vec.y));
        TileInfo info = griefWarnings.tileInfo.get(tile);
        if (info != null && info.link != null) info = info.link;
        reply("====================");
        reply("Tile at " + griefWarnings.formatTile(tile));
        Block currentBlock = tile.block();
        if (currentBlock instanceof BlockPart) currentBlock = tile.link().block();
        reply("Current block: " + currentBlock.name);
        if (info == null) {
            reply("[yellow]No information");
            return;
        }
        reply("Constructed by: " + griefWarnings.formatPlayer(info.constructedBy));
        reply("Deconstructed by: " + griefWarnings.formatPlayer(info.deconstructedBy));
        if (info.previousBlock != null) reply("Block that was here: " + info.previousBlock.name);
        reply("Configured [accent]" + info.configureCount + "[] times");
        if (info.interactedPlayers.size > 0) {
            reply("Players who have interacted with this block:");
            for (Player player : info.interactedPlayers.iterator()) {
                reply("  - " + griefWarnings.formatPlayer(player));
            }
        } else reply("No interaction information recorded");
        if (info.lastRotatedBy != null) reply("Last rotated by: " + griefWarnings.formatPlayer(info.lastRotatedBy));
    }
}
