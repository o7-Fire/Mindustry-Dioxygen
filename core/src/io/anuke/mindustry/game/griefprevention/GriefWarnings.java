package io.anuke.mindustry.game.griefprevention;

import io.anuke.arc.Events;
import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.ObjectSet;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.math.Mathf;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.entities.type.Unit;
import io.anuke.mindustry.game.EventType.DepositEvent;
import io.anuke.mindustry.game.EventType.TileChangeEvent;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.distribution.MassDriver;
import io.anuke.mindustry.world.blocks.distribution.Sorter;
import io.anuke.mindustry.world.blocks.power.ItemLiquidGenerator;
import io.anuke.mindustry.world.blocks.power.NuclearReactor;
import io.anuke.mindustry.world.blocks.power.PowerGraph;
import io.anuke.mindustry.world.blocks.storage.StorageBlock;
import io.anuke.mindustry.world.blocks.storage.Vault;
import io.anuke.mindustry.world.blocks.production.Fracker;

import static io.anuke.mindustry.Vars.*;

import java.lang.ref.WeakReference;
import java.time.Instant;
import java.util.WeakHashMap;

public class GriefWarnings {
    public class PlayerStats {
        // don't prevent garbage collection
        public WeakReference<Player> player;

        public PlayerStats(Player player) {
            this.player = new WeakReference<>(player);
        }
    }

    public class TileInfo implements Cloneable {
        public Player constructedBy;
        // deconstructedBy ambiguously holds possibly either someone whp attempted to deconstruct
        // the current block or the person who deconstructed the previous block
        // TODO: implement full block history
        public Player deconstructedBy;
        public boolean constructSeen = false;
        public boolean deconstructSeen = false;
        public Block previousBlock;
        public int configureCount = 0;
        public ObjectSet<Player> interactedPlayers = new ObjectSet<>();
        public Player lastRotatedBy;
        public TileInfo link;

        public TileInfo clone() {
            try {
                return (TileInfo)super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException("literally not possible");
            }
        }

        public void reset() {
            constructSeen = false;
            deconstructSeen = false;
            configureCount = 0;
            interactedPlayers.clear();
            lastRotatedBy = null;
            link = null;
        }

        public void doLink(TileInfo primary) {
            reset();
            constructedBy = null;
            link = primary;
        }

        public void unlink() {
            if (link == null) return;
            constructedBy = link.constructedBy;
            deconstructedBy = link.deconstructedBy;
            reset();
        }
    }

    private Instant nextWarningTime = Instant.now();
    public WeakHashMap<Player, PlayerStats> playerStats = new WeakHashMap<>();
    /** whether or not to send warnings to all players */
    public boolean broadcast = true;
    // whether or not to be very noisy about everything
    public boolean verbose = false;
    // whether or not to flat out state the obvious, pissing everyone off
    public boolean debug = false;
    public WeakHashMap<Tile, TileInfo> tileInfo = new WeakHashMap<>();

    public CommandHandler commandHandler = new CommandHandler();
    public FixGrief fixer = new FixGrief();

    public GriefWarnings() {
        Events.on(DepositEvent.class, this::handleDeposit);
        Events.on(TileChangeEvent.class, this::handleTileChange);
    }

    public boolean sendMessage(String message, boolean throttled) {
        // if (!net.active()) return false;
        if (message.length() > maxTextLength) {
            ui.chatfrag.addMessage(
                    "[scarlet]WARNING: the following grief warning exceeded maximum allowed chat length and was not sent",
                    null);
            ui.chatfrag.addMessage(message, null);
            ui.chatfrag.addMessage("Message length was [accent]" + message.length(), null);
            return false;
        }
        if (!Instant.now().isAfter(nextWarningTime) && throttled) return false;
        nextWarningTime = Instant.now().plusSeconds(1);
        if (broadcast) Call.sendChatMessage(message);
        else ui.chatfrag.addMessage(message, null);
        return true;
    }

    public boolean sendMessage(String message) {
        return sendMessage(message, true);
    }

    public float getDistanceToCore(Unit unit, float x, float y) {
        Tile nearestCore = unit.getClosestCore().getTile();
        return Mathf.dst(x, y, nearestCore.x, nearestCore.y);
    }

    public float getDistanceToCore(Unit unit, Tile tile) {
        return getDistanceToCore(unit, tile.x, tile.y);
    }

    public float getDistanceToCore(Unit unit) {
        return getDistanceToCore(unit, unit.x, unit.y);
    }

    public void handleConnectFinish() {
        // TODO: future
    }

    public void handleDisconnect() {
        tileInfo.clear();
        playerStats.clear();
    }

    public void handleTileChange(TileChangeEvent event) {
        // if (event.tile.block() == Blocks.air) tileInfo.remove(event.tile);
    }

    public TileInfo getOrCreateTileInfo(Tile tile, boolean doLinking) {
        TileInfo info = tileInfo.get(tile);
        if (info == null) {
            TileInfo newInfo = new TileInfo();
            info = newInfo;
            tileInfo.put(tile, newInfo);
            if (doLinking) tile.getLinkedTiles(linked -> getOrCreateTileInfo(linked, false).doLink(newInfo));
        }
        return info;
    }

    public TileInfo getOrCreateTileInfo(Tile tile) {
        return getOrCreateTileInfo(tile, true);
    }

    public PlayerStats getOrCreatePlayerStats(Player player) {
        PlayerStats stats = playerStats.get(player);
        if (stats == null) {
            stats = new PlayerStats(player);
            playerStats.put(player, stats);
        }
        return stats;
    }

    public void handleBlockConstructProgress(Player builder, Tile tile, Block cblock, float progress, Block previous) {
        TileInfo info = getOrCreateTileInfo(tile);
        if (builder != null) info.constructedBy = builder;

        boolean didWarn = false;
        float coreDistance = getDistanceToCore(builder, tile);
        // persistent warnings that keep showing
        if (coreDistance < 50 && cblock instanceof NuclearReactor) {
            String message = "[scarlet]WARNING[] " + builder.name + "[white] ([stat]#" + builder.id
                    + "[]) is building a reactor [stat]" + Math.round(coreDistance) + "[] blocks from core. [stat]"
                    + Math.round(progress * 100) + "%";
            sendMessage(message);
            didWarn = true;
        } else if (coreDistance < 10 && cblock instanceof ItemLiquidGenerator) {
            String message = "[scarlet]WARNING[] " + builder.name + "[white] ([stat]#" + builder.id
                    + "[]) is building a generator [stat]" + Math.round(coreDistance) + "[] blocks from core. [stat]"
                    + Math.round(progress * 100) + "%";
            sendMessage(message);
            didWarn = true;
        }
        

        // one-time block construction warnings
        if (!info.constructSeen) {
            info.constructSeen = true;
            if (previous != null && previous != Blocks.air) info.previousBlock = previous;
            tile.getLinkedTiles(linked -> getOrCreateTileInfo(linked, false).doLink(info));

            if (!didWarn) {
                if (cblock instanceof NuclearReactor) {
                    Array<Tile> bordering = tile.entity.proximity;
                    boolean hasCryo = false;
                    for (Tile neighbor : bordering) {
                        if (
                            neighbor.entity != null && neighbor.entity.liquids != null &&
                            neighbor.entity.liquids.current() == Liquids.cryofluid
                        ) {
                            hasCryo = true;
                            break;
                        }
                    }
                    if (!hasCryo) {
                        String message = "[lightgray]Notice[] " + formatPlayer(builder) + 
                            " is building a reactor at " + formatTile(tile);
                        sendMessage(message, false);
                    }
                }
                /* doesn't seem very necessary for now
                if (cblock instanceof Fracker) {
                    String message = "[lightgray]Notice[] " + formatPlayer(builder) +
                        " is building an oil extractor at " + formatTile(tile);
                    sendMessage(message, false);
                }
                */
            }
        }
    }

    public void handleBlockConstructFinish(Tile tile, Block block, int builderId) {
        TileInfo info = getOrCreateTileInfo(tile);
        Player targetPlayer = playerGroup.getByID(builderId);
        tile.getLinkedTiles(linked -> getOrCreateTileInfo(linked, false).doLink(info));
        info.constructedBy = targetPlayer;

        if (debug && targetPlayer != null) {
            sendMessage("[cyan]Debug[] " + formatPlayer(targetPlayer) + " builds [accent]" +
                tile.block().name + "[] at " + formatTile(tile), false);
        }
    }

    public void handleBlockDeconstructProgress(Player builder, Tile tile, Block cblock, float progress, Block previous) {
        TileInfo info = getOrCreateTileInfo(tile);
        if (builder != null) info.deconstructedBy = builder;

        if (!info.deconstructSeen) {
            info.deconstructSeen = true;
        }
    }

    public void handleBlockDeconstructFinish(Tile tile, Block block, int builderId) {
        // this runs before the block is actually removed
        TileInfo info = getOrCreateTileInfo(tile);
        Player targetPlayer = playerGroup.getByID(builderId);
        if (targetPlayer != null) info.deconstructedBy = targetPlayer;
        info.previousBlock = block;
        info.reset();
        tile.getLinkedTiles(linked -> {
            TileInfo linkedInfo = getOrCreateTileInfo(linked, false);
            linkedInfo.unlink();
            linkedInfo.previousBlock = info.previousBlock;
        });

        if (debug && targetPlayer != null) {
            sendMessage("[cyan]Debug[] " + targetPlayer.name + "[white] ([stat]#" + builderId +
                "[]) deconstructs [accent]" + tile.block().name + "[] at " + formatTile(tile), false);
        }
    }

    public Player getNearestPlayerByLocation(float x, float y) {
        // grab every player in a 10x10 area and then find closest
        Array<Player> candidates = playerGroup.intersect(x - 5, y - 5, 10, 10);
        if (candidates.size == 0) return null;
        if (candidates.size == 1) return candidates.first();
        if (candidates.size > 1) {
            float nearestDistance = Float.MAX_VALUE;
            Player nearestPlayer = null;
            for (Player player : candidates) {
                float distance = Mathf.dst(x, y, player.x, player.y);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestPlayer = player;
                }
            }
            return nearestPlayer;
        }
        return null; // this should be impossible
    }

    public void handleDeposit(DepositEvent event) {
        Player targetPlayer = event.player;
        Tile tile = event.tile;
        Item item = event.item;
        int amount = event.amount;
        if (targetPlayer == null) return;
        if (verbose) {
            sendMessage("[green]Verbose[] " + targetPlayer.name + "[white] ([stat]#" + targetPlayer.id +
                "[]) transfers " + amount + " " + item.name + " to " + tile.block().name + " " + formatTile(tile), false);
        }
        if (item.equals(Items.thorium) && tile.block() instanceof NuclearReactor) {
            String message = "[scarlet]WARNING[] " + targetPlayer.name + "[white] ([stat]#" +
                targetPlayer.id + "[]) transfers [accent]" + amount + "[] thorium to a reactor. " + formatTile(tile);
            sendMessage(message);
            return;
        } else if (item.explosiveness > 0.5f) {
            Block block = tile.block();
            if (block instanceof ItemLiquidGenerator) {
                String message = "[scarlet]WARNING[] " + formatPlayer(targetPlayer) + " transfers [accent]" +
                    amount + "[] blast to a generator. " + formatTile(tile);
                sendMessage(message);
                return;
            } else if (block instanceof StorageBlock) {
                String message = "[scarlet]WARNING[] " + formatPlayer(targetPlayer) + " transfers [accent]" +
                    amount + "[] blast to a Container. " + formatTile(tile);
                sendMessage(message);
                return;
            } else if (block instanceof Vault) {
                String message = "[scarlet]WARNING[] " + formatPlayer(targetPlayer) + " transfers [accent]" +
                    amount + "[] blast to a Vault. " + formatTile(tile);
                sendMessage(message);
                return;
            }
        }
    }

    public void handlePlayerEntitySnapshot(Player targetPlayer) {
        // System.out.println("received entity snapshot for " + targetPlayer.name + "#" + targetPlayer.id);
        // System.out.println("entity previous: " + playerStats.get(targetPlayer));
        PlayerStats stats = getOrCreatePlayerStats(targetPlayer);
        if (debug) {
            sendMessage("[cyan]Debug[] Player snapshot: " + targetPlayer.name + "[white] ([stat]#" + targetPlayer.id + "[])", false);
        }
    }

    public void handlePlayerDisconnect(int playerId) {
        Player targetPlayer = playerGroup.getByID(playerId);
        // System.out.println("player disconnect: " + targetPlayer.name + "#" + targetPlayer.id);
        playerStats.remove(targetPlayer);
        if (debug) {
            sendMessage("[cyan]Debug[] Player disconnect: " + targetPlayer.name + "[white] ([stat]#" + targetPlayer.id + "[])", false);
        }
    }

    public void handleWorldDataBegin() {
        playerStats.clear();
    }

    public String formatPlayer(Player targetPlayer) {
        String playerString;
        if (targetPlayer != null) {
            playerString = targetPlayer.name + "[white] ([stat]#" + targetPlayer.id + "[])";
        } else {
            playerString = "[lightgray]unknown[]";
        }
        return playerString;
    }

    public String formatColor(Color color) {
        return "[#" + Integer.toHexString(((int)(255 * color.r) << 24) | ((int)(255 * color.g) << 16) | ((int)(255 * color.b) << 8)) + "]";
    }

    public String formatColor(Color color, String toFormat) {
        return formatColor(color) + toFormat + "[]";
    }

    public String formatItem(Item item) {
        if (item == null) return "(none)";
        return formatColor(item.color, item.name);
    }

    public String formatTile(Tile tile) {
        if (tile == null) return "(none)";
        return "(" + tile.x + ", " + tile.y + ")";
    }

    public void handlePowerGraphSplit(Player targetPlayer, Tile tile, PowerGraph oldGraph, PowerGraph newGraph1, PowerGraph newGraph2) {
        int oldGraphCount = oldGraph.all.size;
        int newGraph1Count = newGraph1.all.size;
        int newGraph2Count = newGraph2.all.size;

        if (Math.min(oldGraphCount - newGraph1Count, oldGraphCount - newGraph2Count) > 20) {
            sendMessage("[lightgray]Notice[] Power split by " + formatPlayer(targetPlayer) + " " + oldGraphCount + " -> " +
                newGraph1Count + "/" + newGraph2Count + " " + formatTile(tile));
        }
    }

    public void handleBlockBeforeConfigure(Tile tile, Player targetPlayer, int value) {
        TileInfo info = getOrCreateTileInfo(tile);
        if (targetPlayer != null) info.interactedPlayers.add(targetPlayer);
        info.configureCount++;

        Block block = tile.block();
        if (block instanceof Sorter) {
            Item oldItem = tile.<Sorter.SorterEntity>entity().sortItem;
            Item newItem = content.item(value);
            if (verbose) {
                sendMessage("[green]Verbose[] " + formatPlayer(targetPlayer) + " configures sorter " +
                    formatItem(oldItem) + " -> " + formatItem(newItem) + " " + formatTile(tile));
            }
        } else if (block instanceof MassDriver) {
            Tile oldLink = world.tile(tile.<MassDriver.MassDriverEntity>entity().link);
            Tile newLink = world.tile(value);
            if (verbose) {
                sendMessage("[green]Verbose[] " + formatPlayer(targetPlayer) + " configures mass driver at " +
                    formatTile(tile) + " from " + formatTile(oldLink) + " to " + formatTile(newLink));
            }
        }
    }
    
    public void handleRotateBlock(Player targetPlayer, Tile tile, boolean direction) {
        TileInfo info = getOrCreateTileInfo(tile);
        if (targetPlayer != null) {
            info.lastRotatedBy = targetPlayer;
            info.interactedPlayers.add(targetPlayer);
        }

        if (verbose) {
            sendMessage("[green]Verbose[] " + formatPlayer(targetPlayer) + " rotates " +
                tile.block().name + " at " + formatTile(tile));
        }
    }
}
