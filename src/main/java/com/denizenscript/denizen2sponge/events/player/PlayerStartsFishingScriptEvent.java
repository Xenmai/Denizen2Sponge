package com.denizenscript.denizen2sponge.events.player;

import com.denizenscript.denizen2core.events.ScriptEvent;
import com.denizenscript.denizen2core.tags.AbstractTagObject;
import com.denizenscript.denizen2sponge.Denizen2Sponge;
import com.denizenscript.denizen2sponge.tags.objects.EntityTag;
import com.denizenscript.denizen2sponge.tags.objects.PlayerTag;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.action.FishingEvent;
import org.spongepowered.api.event.filter.cause.Root;

import java.util.HashMap;

public class PlayerStartsFishingScriptEvent extends ScriptEvent {

    // <--[event]
    // @Since 0.5.5
    // @Events
    // player starts fishing
    //
    // @Updated 2018/12/18
    //
    // @Cancellable true
    //
    // @Group Player
    //
    // @Triggers when a player casts the fish hook.
    //
    // @Context
    // player (PlayerTag) returns the player that started fishing.
    // fish_hook (EntityTag) returns the fish hook as an entity.
    //
    // @Determinations
    // None.
    // -->

    @Override
    public String getName() {
        return "PlayerStartsFishing";
    }

    @Override
    public boolean couldMatch(ScriptEventData data) {
        return data.eventPath.startsWith("player starts fishing");
    }

    @Override
    public boolean matches(ScriptEventData data) {
        return true;
    }

    public PlayerTag player;

    public EntityTag fishHook;

    public FishingEvent.Start internal;

    @Override
    public HashMap<String, AbstractTagObject> getDefinitions(ScriptEventData data) {
        HashMap<String, AbstractTagObject> defs = super.getDefinitions(data);
        defs.put("player", player);
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
    public void onPlayerStartsFishing(FishingEvent.Start evt, @Root Player player) {
        PlayerStartsFishingScriptEvent event = (PlayerStartsFishingScriptEvent) clone();
        event.internal = evt;
        event.player = new PlayerTag(player);
        event.fishHook = new EntityTag(evt.getFishHook());
        event.cancelled = evt.isCancelled();
        event.run();
        evt.setCancelled(event.cancelled);
    }

    @Override
    public void applyDetermination(boolean errors, String determination, AbstractTagObject value) {
        super.applyDetermination(errors, determination, value);
    }
}
