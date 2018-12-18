package com.denizenscript.denizen2sponge.events.player;

import com.denizenscript.denizen2core.events.ScriptEvent;
import com.denizenscript.denizen2core.tags.AbstractTagObject;
import com.denizenscript.denizen2core.tags.objects.ListTag;
import com.denizenscript.denizen2sponge.Denizen2Sponge;
import com.denizenscript.denizen2sponge.tags.objects.EntityTag;
import com.denizenscript.denizen2sponge.tags.objects.ItemTag;
import com.denizenscript.denizen2sponge.tags.objects.PlayerTag;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.action.FishingEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;

import java.util.HashMap;

public class PlayerStopsFishingScriptEvent extends ScriptEvent {

    // <--[event]
    // @Since 0.5.5
    // @Events
    // player stops fishing
    //
    // @Updated 2018/12/18
    //
    // @Cancellable true
    //
    // @Group Player
    //
    // @Triggers when a player reels in the fish hook.
    //
    // @Context
    // player (PlayerTag) returns the player that stopped fishing.
    // items (ListTag) returns the fished items.
    // fish_hook (EntityTag) returns the fish hook as an entity.
    //
    // @Determinations
    // new_items (ListTag) sets the new fished items.
    // -->

    @Override
    public String getName() {
        return "PlayerStopsFishing";
    }

    @Override
    public boolean couldMatch(ScriptEventData data) {
        return data.eventPath.startsWith("player stops fishing");
    }

    @Override
    public boolean matches(ScriptEventData data) {
        return true;
    }

    public PlayerTag player;

    public ListTag items;

    public EntityTag fishHook;

    public FishingEvent.Stop internal;

    @Override
    public HashMap<String, AbstractTagObject> getDefinitions(ScriptEventData data) {
        HashMap<String, AbstractTagObject> defs = super.getDefinitions(data);
        defs.put("player", player);
        defs.put("items", items);
        defs.put("fish_hook", fishHook);
        return defs;
    }

    @Override
    public void enable() {
        Sponge.getEventManager().registerListeners(Denizen2Sponge.instance, this);
    }

    @Override
    public void disable() {
        Sponge.getEventManager().unregisterListeners(this);
    }

    @Listener
    public void onPlayerStopsFishing(FishingEvent.Stop evt, @Root Player player) {
        PlayerStopsFishingScriptEvent event = (PlayerStopsFishingScriptEvent) clone();
        event.internal = evt;
        event.player = new PlayerTag(player);
        ListTag list = new ListTag();
        for (Transaction<ItemStackSnapshot> transaction : evt.getTransactions()) {
            list.getInternal().add(new ItemTag(transaction.getFinal().createStack()));
        }
        event.items = list;
        event.fishHook = new EntityTag(evt.getFishHook());
        event.cancelled = evt.isCancelled();
        event.run();
        evt.setCancelled(event.cancelled);
    }

    @Override
    public void applyDetermination(boolean errors, String determination, AbstractTagObject value) {
        if (determination.equals("new_items")) {
            ListTag lt = ListTag.getFor(this::error, value);
            items = lt;
            internal.getTransactions().clear();
            for (AbstractTagObject ato : lt.getInternal()) {
                internal.getTransactions().add(new Transaction<>(ItemStackSnapshot.NONE,
                        ItemTag.getFor(this::error, ato).getInternal().createSnapshot()));
            }
        }
        else {
            super.applyDetermination(errors, determination, value);
        }
    }
}
