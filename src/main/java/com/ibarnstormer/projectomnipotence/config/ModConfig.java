package com.ibarnstormer.projectomnipotence.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.ibarnstormer.projectomnipotence.Main;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@SuppressWarnings("unused")
public class ModConfig {

    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping().setLenient().setPrettyPrinting().registerTypeAdapterFactory(new RecordTypeAdapterFactory()).create();

    private final String _comment_version = "Config version, set to -1 to prevent config updates.";
    private int version = Main.CONFIG_VERSION;

    private final String _comment_PlayerConfig = "Player Configurations: Various modifiers for specific players";
    public List<POPlayerConfig> playerConfigs = new ArrayList<>();

    private final String _comment_DamageReflect = "Damage Reflection Black List: Entries are in the format: 'namespace:entity_id' (e.g. minecraft:creeper) or '*' for all entities";
    public Set<String> damageReflectionBlackList = new HashSet<>();

    private final String _comment_removeOnEnlighten = "Removal upon being enlightened: Entries are in the format: 'namespace:entity_id' (e.g. minecraft:creeper) or '*' for all entities";
    public Set<String> removeOnEnlightenList = new HashSet<>();

    private final String _comment_EnlightenmentBlacklist = "Enlightenment blacklist: Entries are in the format: 'namespace:entity_id' (e.g. minecraft:creeper) or '*' for all entities";
    public Set<String> enlightenmentBlackList = new HashSet<>();

    private final String _comment_ConvertUponEnlightened = "Entity to convert to upon enlightenment. Key is target entity, value is the entity to convert to. Entries are in the format: 'namespace:entity_id' (e.g. minecraft:creeper)";
    public final Map<String, String> convertUponEnlightened = new HashMap<>();

    public int flightEntityGoal = 10000;
    public int invulnerabilityEntityGoal = 1000;
    public int luckLevelEntityGoal = 250;
    public int totalLuckLevels = 3;

    public boolean omnipotentPlayersGlow = false;
    public boolean omnipotentPlayerParticles = true;
    public boolean omnipotentPlayerParticlesLocal = false;
    public boolean omnipotentPlayerRenderParticlesClient = true;

    public boolean omnipotentPlayersCanBecomeInvulnerable = true;
    public boolean omnipotentPlayersCanGainFlight = false;
    public boolean omnipotentPlayersDampenExplosions = false;
    public boolean omnipotentPlayersDeleteProjectiles = false;
    public boolean omnipotentPlayersDontGriefTrees = true;
    public boolean omnipotentPlayersReflectDamage = true;
    public boolean omnipotentPlayersRemoveCurses = true;

    public boolean carryOnCompat = true;


    private ModConfig() {
        playerConfigs.add(new POPlayerConfig("(Example Player Username Here)", "0", false, 0, 0));

        // Hard-coded entities that cause crashes when damage is reflected back
        damageReflectionBlackList.add("cataclysm:lionfish");

        // Hard-coded entities that create soft-locks unless explicitly removed from the world
        removeOnEnlightenList.add("blue_skies:alchemist");
        removeOnEnlightenList.add("blue_skies:arachnarch");
        removeOnEnlightenList.add("blue_skies:starlit_crusher");
        removeOnEnlightenList.add("blue_skies:summoner");
        removeOnEnlightenList.add("aether:sun_spirit");
        removeOnEnlightenList.add("aether:valkryie_queen");
        removeOnEnlightenList.add("aether:slider");
        removeOnEnlightenList.add("twilightforest:hydra");
        removeOnEnlightenList.add("twilightforest:naga");
        removeOnEnlightenList.add("twilightforest:knight_phantom");
        removeOnEnlightenList.add("mowziesmobs:ferrous_wroughtnaut");

        // Prevent exploit
        enlightenmentBlackList.add("dummmmmmy:target_dummy");
        enlightenmentBlackList.add("minecraft:armor_stand");

        // Built-in conversions
        convertUponEnlightened.put("minecraft:zombie_villager", "minecraft:villager");
        // convertUponEnlightened.put("minecraft:witch", "minecraft:villager");
        convertUponEnlightened.put("minecraft:zoglin", "minecraft:hoglin");
        convertUponEnlightened.put("minecraft:zombified_piglin", "minecraft:piglin");
        convertUponEnlightened.put("illageandspillage:ragno", "minecraft:villager");
        convertUponEnlightened.put("illageandportage:ragno", "minecraft:villager");

    }

    public static ModConfig initConfig() {
        try {
            File configFile = new File(FMLPaths.CONFIGDIR.get().toString(), "ProjectOmnipotence.json");
            ModConfig config;

            // Load config if it exists
            if (configFile.exists()) {
                String json = FileUtils.readFileToString(configFile, StandardCharsets.UTF_8);
                config = GSON.fromJson(json, ModConfig.class);

                // Update config fields
                if(config.version != Main.CONFIG_VERSION && config.version != -1) updateConfig(config);
            }
            else config = new ModConfig();

            // Update the config
            FileUtils.writeStringToFile(configFile, GSON.toJson(config), StandardCharsets.UTF_8);

            return config;
        }
        catch(Exception e) {
            e.printStackTrace();
            return new ModConfig();
        }
    }

    private static void updateConfig(ModConfig config) {
        ModConfig freshConfig = new ModConfig();
        config.version = freshConfig.version;

        config.damageReflectionBlackList.addAll(freshConfig.damageReflectionBlackList);
        config.removeOnEnlightenList.addAll(freshConfig.removeOnEnlightenList);
        config.damageReflectionBlackList.addAll(freshConfig.damageReflectionBlackList);
        config.enlightenmentBlackList.addAll(freshConfig.enlightenmentBlackList);
        config.convertUponEnlightened.putAll(freshConfig.convertUponEnlightened);

    }

    // Taken from https://github.com/google/gson/issues/1794
    public static class RecordTypeAdapterFactory implements TypeAdapterFactory {

        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            @SuppressWarnings("unchecked")
            Class<T> clazz = (Class<T>) type.getRawType();
            if (!clazz.isRecord()) {
                return null;
            }
            TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);

            return new TypeAdapter<T>() {
                @Override
                public void write(JsonWriter out, T value) throws IOException {
                    delegate.write(out, value);
                }

                @Override
                public T read(JsonReader reader) throws IOException {
                    if (reader.peek() == JsonToken.NULL) {
                        reader.nextNull();
                        return null;
                    } else {
                        var recordComponents = clazz.getRecordComponents();
                        var typeMap = new HashMap<String,TypeToken<?>>();
                        for (int i = 0; i < recordComponents.length; i++) {
                            typeMap.put(recordComponents[i].getName(), TypeToken.get(recordComponents[i].getGenericType()));
                        }
                        var argsMap = new HashMap<String,Object>();
                        reader.beginObject();
                        while (reader.hasNext()) {
                            String name = reader.nextName();
                            argsMap.put(name, gson.getAdapter(typeMap.get(name)).read(reader));
                        }
                        reader.endObject();

                        var argTypes = new Class<?>[recordComponents.length];
                        var args = new Object[recordComponents.length];
                        for (int i = 0; i < recordComponents.length; i++) {
                            argTypes[i] = recordComponents[i].getType();
                            args[i] = argsMap.get(recordComponents[i].getName());
                        }
                        Constructor<T> constructor;
                        try {
                            constructor = clazz.getDeclaredConstructor(argTypes);
                            constructor.setAccessible(true);
                            return constructor.newInstance(args);
                        } catch (NoSuchMethodException | InstantiationException | SecurityException |
                                 IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            };
        }

    }

}
