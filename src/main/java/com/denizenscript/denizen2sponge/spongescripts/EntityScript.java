package com.denizenscript.denizen2sponge.spongescripts;

import com.denizenscript.denizen2core.Denizen2Core;
import com.denizenscript.denizen2core.arguments.Argument;
import com.denizenscript.denizen2core.commands.CommandQueue;
import com.denizenscript.denizen2core.scripts.CommandScript;
import com.denizenscript.denizen2core.tags.AbstractTagObject;
import com.denizenscript.denizen2core.tags.objects.BooleanTag;
import com.denizenscript.denizen2core.tags.objects.MapTag;
import com.denizenscript.denizen2core.tags.objects.ScriptTag;
import com.denizenscript.denizen2core.utilities.Action;
import com.denizenscript.denizen2core.utilities.CoreUtilities;
import com.denizenscript.denizen2core.utilities.ErrorInducedException;
import com.denizenscript.denizen2core.utilities.Tuple;
import com.denizenscript.denizen2core.utilities.debugging.ColorSet;
import com.denizenscript.denizen2core.utilities.debugging.Debug;
import com.denizenscript.denizen2core.utilities.yaml.StringHolder;
import com.denizenscript.denizen2core.utilities.yaml.YAMLConfiguration;
import com.denizenscript.denizen2sponge.Denizen2Sponge;
import com.denizenscript.denizen2sponge.tags.objects.EntityTypeTag;
import com.denizenscript.denizen2sponge.utilities.AITaskHelper;
import com.denizenscript.denizen2sponge.utilities.DataKeys;
import com.denizenscript.denizen2sponge.utilities.EntityTemplate;
import com.denizenscript.denizen2sponge.utilities.Utilities;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.item.ItemType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EntityScript extends CommandScript {

    // <--[explanation]
    // @Since 0.5.5
    // @Name Entity Scripts
    // @Group Script Types
    // @Description
    // An entity script is a type of script that fully defines specific entity template, which can
    // be used to spawn entities afterwards. Keys in an entity script define which properties the
    // final entity will have.
    //
    // An entity script can be used in place of an entity type in certain spawning related commands or inputs.
    //
    // The entity script name may not be the same as an existing entity type name.
    //
    // Entities generated from an entity script will remember their type using flag "_d2_script".
    // To stop this from occurring, set key "plain" to "true".
    //
    // Set key "static" to "true" on the entity script to make it load once at startup and simply be duplicated on
    // all usages. If static is false or unspecified, the entity script will be loaded from data at each call
    // requesting it. This is likely preferred if any tags are used within the script.
    //
    // All options listed below are used to define the entity's specific details.
    // They all support tags on input. All options other than "base" may use the automatically
    // included definition tag <[base]> to get the base type.
    //
    // Set key "base" directly to an EntityTypeTag of the basic entity type to use. You may also
    // use an existing entity script to inherit its properties. Be careful to not list the
    // entity script within itself, even indirectly, as this can cause recursion errors.
    //
    // Set key "display name" directly to a TextTag or FormattedTextTag value of the name the entity should have.
    //
    // Set key "flags" as a section and within it put all flags keyed by name and with the value that each flag should hold.
    // If you wish to dynamically structure the mapping, see the "keys" option for specifying that.
    //
    // To specify other values, create a section labeled "keys" and within it put any valid entity keys.
    // TODO: Create and reference an explanation of basic entity keys.
    //
    // Set key "ai tasks" to specify AI Tasks that will be applied to the entity on spawn. Valid task types can be found in the
    // <@link explanation AI Task Types>AI task types explanation<@/link>, and the properties they accept
    // are explained in the <@link command addaitask>addaitask command<@/link> documentation.
    // On top of that, each task accepts a goal (see <@link explanation AI Goal Types>AI goal types<@/link>) and a priority value.
    // -->

    public EntityScript(String name, YAMLConfiguration section) {
        super(name, section);
        entityScriptName = CoreUtilities.toLowerCase(name);
    }

    public final String entityScriptName;

    @Override
    public boolean init() {
        if (super.init()) {
            try {
                prepValues();
                if (contents.contains("static") && BooleanTag.getFor(Denizen2Sponge.FORCE_TO_STATIC.error, contents.getString("static")).getInternal()) {
                    staticEntity = getEntityCopy(Denizen2Sponge.FORCE_TO_STATIC);
                }
            }
            catch (ErrorInducedException ex) {
                Debug.error("Entity generation for " + ColorSet.emphasis + title + ColorSet.warning + ": " + ex.getMessage());
                return false;
            }
            Denizen2Sponge.entityScripts.put(entityScriptName, this);
            return true;
        }
        return false;
    }

    public EntityTemplate staticEntity = null;

    public EntityTemplate getEntityCopy(CommandQueue queue) {
        if (staticEntity != null) {
            return staticEntity;
        }
        return generateEntity(queue);
    }

    public Argument displayName, plain, base;

    public List<Tuple<String, Argument>> otherValues, flags;

    public List<Tuple<String, List<Tuple<String, Argument>>>> taskData;

    public void prepValues() {
        Action<String> error = (es) -> {
            throw new ErrorInducedException(es);
        };
        if (Sponge.getRegistry().getType(ItemType.class, title).isPresent()) {
            Debug.error("Entity script may be unusable: a base entity type exists with the same name!");
        }
        if (contents.contains("display name")) {
            displayName = Denizen2Core.splitToArgument(contents.getString("display name"), true, true, error);
        }
        if (contents.contains("plain")) {
            plain = Denizen2Core.splitToArgument(contents.getString("plain"), true, true, error);
        }
        if (contents.contains("base")) {
            base = Denizen2Core.splitToArgument(contents.getString("base"), true, true, error);
        }
        else {
            throw new ErrorInducedException("Base key is missing. Cannot generate!");
        }
        if (contents.contains("flags")) {
            flags = new ArrayList<>();
            YAMLConfiguration sec = contents.getConfigurationSection("flags");
            for (StringHolder key : sec.getKeys(false)) {
                Argument arg = Denizen2Core.splitToArgument(sec.getString(key.str), true, true, error);
                flags.add(new Tuple<>(key.low, arg));
            }
        }
        if (contents.contains("keys")) {
            otherValues = new ArrayList<>();
            YAMLConfiguration sec = contents.getConfigurationSection("keys");
            for (StringHolder key : sec.getKeys(false)) {
                Argument arg = Denizen2Core.splitToArgument(sec.getString(key.str), true, true, error);
                otherValues.add(new Tuple<>(key.low, arg));
            }
        }
        if (contents.contains("ai tasks")) {
            taskData = new ArrayList<>();
            YAMLConfiguration sec = contents.getConfigurationSection("ai tasks");
            for (StringHolder key : sec.getKeys(false)) {
                YAMLConfiguration subsec = sec.getConfigurationSection(key.str);
                List<Tuple<String, Argument>> subList = new ArrayList<>();
                for (StringHolder subKey : subsec.getKeys(false)) {
                    Argument arg = Denizen2Core.splitToArgument(subsec.getString(subKey.str), true, true, error);
                    subList.add(new Tuple<>(subKey.low, arg));
                }
                taskData.add(new Tuple<>(key.low, subList));
            }
        }
    }

    public AbstractTagObject parseVal(CommandQueue queue, Argument arg, HashMap<String, AbstractTagObject> varBack) {
        return arg.parse(queue, varBack, getDebugMode(), queue.error);
    }

    public EntityTemplate generateEntity(CommandQueue queue) {
        EntityTemplate ent;
        HashMap<String, AbstractTagObject> varBack = new HashMap<>();
        String baseStr = parseVal(queue, base, varBack).toString();
        EntityType entType = (EntityType) Utilities.getTypeWithDefaultPrefix(EntityType.class, baseStr);
        if (entType != null) {
            ent = new EntityTemplate(entType);
            varBack.put("base", new EntityTypeTag(entType));
        }
        else {
            String baseLow = CoreUtilities.toLowerCase(baseStr);
            if (Denizen2Sponge.entityScripts.containsKey(baseLow)) {
                EntityTemplate baseEnt = Denizen2Sponge.entityScripts.get(baseLow).getEntityCopy(queue);
                ent = new EntityTemplate(baseEnt);
                varBack.put("base", new MapTag(baseEnt.properties));
            }
            else {
                queue.error.run("No entity types or scripts found for id '" + baseStr + "'.");
                return null;
            }
        }
        HashMap<String, AbstractTagObject> prop = new HashMap<>();
        if (displayName != null) {
            prop.put("display_name", parseVal(queue, displayName, varBack));
        }
        if (otherValues != null) {
            for (Tuple<String, Argument> input : otherValues) {
                if (!input.one.equalsIgnoreCase("clear_ai_tasks")
                        && !input.one.equalsIgnoreCase("orientation")) {
                    Key k = DataKeys.getKeyForName(input.one);
                    if (k == null) {
                        queue.error.run("Error handling entity script '" + ColorSet.emphasis + title + ColorSet.warning
                                + "': key '" + ColorSet.emphasis + input.one + ColorSet.warning + "' does not seem to exist.");
                        return null;
                    }
                }
                prop.put(input.one, parseVal(queue, input.two, varBack));
            }
        }
        MapTag flagsMap;
        AbstractTagObject ato = ent.properties.get("flagmap");
        if (ato != null) {
            flagsMap = new MapTag(((MapTag) ato).getInternal());
        }
        else {
            flagsMap = new MapTag();
        }
        if (flags != null) {
            for (Tuple<String, Argument> flagVal : flags) {
                flagsMap.getInternal().put(flagVal.one, parseVal(queue, flagVal.two, varBack));
            }
        }
        if (plain == null || !BooleanTag.getFor(queue.error, parseVal(queue, plain, varBack)).getInternal()) {
            flagsMap.getInternal().put("_d2_script", new ScriptTag(this));
        }
        if (!flagsMap.getInternal().isEmpty()) {
            prop.put("flagmap", flagsMap);
        }
        ent.properties.putAll(prop);
        if (taskData != null) {
            for (Tuple<String, List<Tuple<String, Argument>>> task : taskData) {
                if (AITaskHelper.handlers.get(task.one) == null) {
                    queue.error.run("Error handling entity script '" + ColorSet.emphasis + title + ColorSet.warning
                            + "': task type '" + ColorSet.emphasis + task.one + ColorSet.warning + "' does not seem to exist.");
                    return null;
                }
                HashMap<String, AbstractTagObject> map = new HashMap<>();
                for (Tuple<String, Argument> opt : task.two) {
                    map.put(opt.one, parseVal(queue, opt.two, varBack));
                }
                ent.tasks.put(task.one, map);
            }
        }
        if (queue == Denizen2Sponge.FORCE_TO_STATIC && contents.contains("static")
                && BooleanTag.getFor(queue.error, contents.getString("static")).getInternal()) {
            staticEntity = ent;
        }
        return ent;
    }

    @Override
    public boolean isExecutable(String section) {
        return false;
    }
}
