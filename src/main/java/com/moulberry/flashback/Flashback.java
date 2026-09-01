package com.moulberry.flashback;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.serialization.Lifecycle;
import com.moulberry.flashback.action.*;
import com.moulberry.flashback.command.BetterColorArgument;
import com.moulberry.flashback.compat.DistantHorizonsSupport;
import com.moulberry.flashback.configuration.FlashbackConfig;
import com.moulberry.flashback.exporting.AsyncFileDialogs;
import com.moulberry.flashback.exporting.ExportJob;
import com.moulberry.flashback.ext.MinecraftExt;
import com.moulberry.flashback.keyframe.KeyframeRegistry;
import com.moulberry.flashback.keyframe.types.CameraKeyframeType;
import com.moulberry.flashback.keyframe.types.CameraOrbitKeyframeType;
import com.moulberry.flashback.keyframe.types.CameraShakeKeyframeType;
import com.moulberry.flashback.keyframe.types.FOVKeyframeType;
import com.moulberry.flashback.keyframe.types.FreezeKeyframeType;
import com.moulberry.flashback.keyframe.types.SpeedKeyframeType;
import com.moulberry.flashback.keyframe.types.TimeOfDayKeyframeType;
import com.moulberry.flashback.keyframe.types.TimelapseKeyframeType;
import com.moulberry.flashback.keyframe.types.TrackEntityKeyframeType;
import com.moulberry.flashback.packet.FlashbackAccurateEntityPosition;
import com.moulberry.flashback.packet.FlashbackClearParticles;
import com.moulberry.flashback.packet.FlashbackForceClientTick;
import com.moulberry.flashback.packet.FlashbackInstantlyLerp;
import com.moulberry.flashback.packet.FlashbackRemoteExperience;
import com.moulberry.flashback.packet.FlashbackRemoteFoodData;
import com.moulberry.flashback.packet.FlashbackRemoteSelectHotbarSlot;
import com.moulberry.flashback.packet.FlashbackRemoteSetSlot;
import com.moulberry.flashback.playback.EmptyLevelSource;
import com.moulberry.flashback.playback.ReplayServer;
import com.moulberry.flashback.record.FlashbackMeta;
import com.moulberry.flashback.record.Recorder;
import com.moulberry.flashback.record.ReplayExporter;
import com.moulberry.flashback.record.ReplayMarker;
import com.moulberry.flashback.screen.ConfigScreen;
import com.moulberry.flashback.screen.RecoverRecordingsScreen;
import com.moulberry.flashback.screen.SaveReplayScreen;
import com.moulberry.flashback.screen.UnsupportedLoaderScreen;
import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.EditorStateManager;
import com.moulberry.flashback.visuals.AccurateEntityPositionHandler;
import com.moulberry.flashback.visuals.ShaderManager;
import com.seibel.distanthorizons.api.DhApi;
import net.minecraft.ChatFormatting;
import net.minecraft.FileUtil;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.*;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Mod("flashback")
public class Flashback {
    public static final Logger LOGGER = LoggerFactory.getLogger("flashback");

    public static final int MAGIC = 0xD780E884;
    public static Recorder RECORDER = null;
    public static ExportJob EXPORT_JOB = null;
    private static FlashbackConfig config;
    private static Path configDirectory = null;

    private static int delayedStartRecording = 0;
    private static boolean delayedOpenConfig = false;
    private static volatile boolean isInReplay = false;
    private String unsupportedLoader;

    public static boolean supportsDistantHorizons = false;

    private static final List<Path> pendingReplaySave = new ArrayList<>();
    private static final List<Path> pendingReplayRecovery = new ArrayList<>();

    public static ResourceLocation createResourceLocation(String value) {
        return new ResourceLocation("flashback", value);
    }

    public static Path getDataDirectory() {
        return LoaderCompat.getGameDir().resolve("flashback");
    }

    public static Path getReplayFolder() {
        return Flashback.getDataDirectory().resolve("replays");
    }

    public static Path getConfigDirectory() {
        if (configDirectory == null) {
            configDirectory = LoaderCompat.getConfigDir().resolve("flashback");
            try {
                Files.createDirectories(configDirectory);
            } catch (Exception e) {
                LOGGER.error("Unable to create directories for config folder", e);
            }
        }
        return configDirectory;
    }

    public Flashback() {
        configureJavaCppCache();
        configureBundledFFmpegLibraryPath();
        preloadFFmpegNatives();
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::onClientSetup);
        MinecraftForge.EVENT_BUS.register(this);
        FlashbackNetwork.register();
    }

    /**
     * JavaCPP's default cache is shared by every Minecraft profile. On Windows,
     * a second game process can lock an FFmpeg DLL while another profile tries
     * to extract over it. Keep the native cache inside this profile instead.
     */
    private static void configureJavaCppCache() {
        if (System.getProperty("org.bytedeco.javacpp.cachedir") != null) {
            return;
        }

        Path cacheDirectory = LoaderCompat.getGameDir().resolve("flashback").resolve("native_cache")
            .toAbsolutePath().normalize();
        try {
            Files.createDirectories(cacheDirectory);
            System.setProperty("org.bytedeco.javacpp.cachedir", cacheDirectory.toString());
        } catch (IOException exception) {
            LOGGER.warn("Unable to create the profile-specific FFmpeg native cache", exception);
        }
    }

    private static final String[] WINDOWS_FFMPEG_NATIVE_RESOURCES = {
        "org/bytedeco/javacpp/windows-x86_64/jnijavacpp.dll",
        "org/bytedeco/ffmpeg/windows-x86_64/avutil-58.dll",
        "org/bytedeco/ffmpeg/windows-x86_64/jniavutil.dll",
        "org/bytedeco/ffmpeg/windows-x86_64/swresample-4.dll",
        "org/bytedeco/ffmpeg/windows-x86_64/jniswresample.dll",
        "org/bytedeco/ffmpeg/windows-x86_64/avcodec-60.dll",
        "org/bytedeco/ffmpeg/windows-x86_64/jniavcodec.dll",
        "org/bytedeco/ffmpeg/windows-x86_64/avformat-60.dll",
        "org/bytedeco/ffmpeg/windows-x86_64/jniavformat.dll",
        "org/bytedeco/ffmpeg/windows-x86_64/swscale-7.dll",
        "org/bytedeco/ffmpeg/windows-x86_64/jniswscale.dll",
        "org/bytedeco/ffmpeg/windows-x86_64/avfilter-9.dll",
        "org/bytedeco/ffmpeg/windows-x86_64/jniavfilter.dll",
        "org/bytedeco/ffmpeg/windows-x86_64/avdevice-60.dll",
        "org/bytedeco/ffmpeg/windows-x86_64/jniavdevice.dll"
    };

    /**
     * Forge exposes nested dependencies through union: URLs. JavaCPP 1.5.10
     * cannot determine the size of those resources and consequently tries to
     * extract the same DLL again after Windows has locked it. Extract the
     * bundled FFmpeg libraries once and make JavaCPP prefer normal file URLs.
     */
    private static void configureBundledFFmpegLibraryPath() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        String osArch = System.getProperty("os.arch", "").toLowerCase();
        if (!osName.contains("win") || !(osArch.equals("amd64") || osArch.equals("x86_64"))) {
            return;
        }

        Path libraryDirectory = getDataDirectory().resolve("native_cache")
            .resolve("ffmpeg-6.1.1-1.5.10-windows-x86_64").toAbsolutePath().normalize();
        try {
            Files.createDirectories(libraryDirectory);
            ClassLoader classLoader = Flashback.class.getClassLoader();
            for (String resource : WINDOWS_FFMPEG_NATIVE_RESOURCES) {
                String fileName = resource.substring(resource.lastIndexOf('/') + 1);
                Path destination = libraryDirectory.resolve(fileName);
                if (Files.isRegularFile(destination) && Files.size(destination) > 0) {
                    continue;
                }

                try (InputStream inputStream = classLoader.getResourceAsStream(resource)) {
                    if (inputStream == null) {
                        throw new IOException("Missing bundled native library: " + resource);
                    }
                    Path temporary = libraryDirectory.resolve(fileName + ".part");
                    Files.copy(inputStream, temporary, StandardCopyOption.REPLACE_EXISTING);
                    Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            prependJavaCppPath("org.bytedeco.javacpp.platform.linkpath", libraryDirectory);
            prependJavaCppPath("org.bytedeco.javacpp.platform.preloadpath", libraryDirectory);
            System.setProperty("org.bytedeco.javacpp.pathsFirst", "true");
        } catch (IOException exception) {
            LOGGER.warn("Unable to prepare bundled FFmpeg native libraries", exception);
        }
    }

    private static void prependJavaCppPath(String property, Path path) {
        String pathString = path.toString();
        String existing = System.getProperty(property);
        if (existing == null || existing.isBlank()) {
            System.setProperty(property, pathString);
        } else if (!List.of(existing.split(java.io.File.pathSeparator)).contains(pathString)) {
            System.setProperty(property, pathString + java.io.File.pathSeparator + existing);
        }
    }

    /**
     * Load the FFmpeg JNI libraries in dependency order before generated FFmpeg
     * classes initialize. On Windows, allowing avformat to initialize first can
     * make JavaCPP try to overwrite jniavutil.dll after it has already been
     * loaded by a dependent class, which fails because loaded DLLs are locked.
     */
    private static void preloadFFmpegNatives() {
        try {
            org.bytedeco.javacpp.Loader.load(org.bytedeco.ffmpeg.global.avutil.class);
            org.bytedeco.javacpp.Loader.load(org.bytedeco.ffmpeg.global.swresample.class);
            org.bytedeco.javacpp.Loader.load(org.bytedeco.ffmpeg.global.avcodec.class);
            org.bytedeco.javacpp.Loader.load(org.bytedeco.ffmpeg.global.avformat.class);
            org.bytedeco.javacpp.Loader.load(org.bytedeco.ffmpeg.global.swscale.class);
        } catch (Throwable throwable) {
            // VideoContainer will retry when the export window is opened and can
            // still fall back to PNG sequence export if native codecs are absent.
            LOGGER.warn("Unable to preload FFmpeg native libraries", throwable);
        }
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(this::onInitializeClient);
    }

	private void onInitializeClient() {
        Path configFolder = LoaderCompat.getConfigDir().resolve("flashback");

        try {
            Files.createDirectories(configFolder);
        } catch (IOException e) {
            Flashback.LOGGER.error("Failed to create config folder", e);
        }

        config = FlashbackConfig.tryLoadFromFolder(configFolder);

        TempFolderProvider.tryDeleteStaleFolders(TempFolderProvider.TempFolderType.SERVER);

        Path recordingFolder = TempFolderProvider.getTypedTempFolder(TempFolderProvider.TempFolderType.RECORDING);
        if (Files.exists(recordingFolder)) {
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(recordingFolder)) {
                Iterator<Path> iterator = directoryStream.iterator();
                while (iterator.hasNext()) {
                    Path folder = iterator.next();

                    if (Files.exists(folder.resolve("metadata.json"))) {
                        pendingReplayRecovery.add(folder);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (pendingReplayRecovery.isEmpty()) {
            TempFolderProvider.tryDeleteStaleFolders(TempFolderProvider.TempFolderType.RECORDING);
        }

        // Delete partial exports
        try {
            FileUtils.deleteDirectory(Path.of("replay_export_temp").toFile());
        } catch (Exception ignored) {}

        this.deleteUnusedReplayStates();

        ActionRegistry.register(ActionNextTick.INSTANCE);
        ActionRegistry.register(ActionGamePacket.INSTANCE);
        ActionRegistry.register(ActionCreateLocalPlayer.INSTANCE);
        ActionRegistry.register(ActionMoveEntities.INSTANCE);
        ActionRegistry.register(ActionLevelChunkCached.INSTANCE);
        ActionRegistry.register(ActionAccuratePlayerPosition.INSTANCE);

        KeyframeRegistry.register(CameraKeyframeType.INSTANCE);
        KeyframeRegistry.register(CameraOrbitKeyframeType.INSTANCE);
        KeyframeRegistry.register(CameraShakeKeyframeType.INSTANCE);
        KeyframeRegistry.register(FOVKeyframeType.INSTANCE);
        KeyframeRegistry.register(SpeedKeyframeType.INSTANCE);
        KeyframeRegistry.register(TimelapseKeyframeType.INSTANCE);
        KeyframeRegistry.register(TimeOfDayKeyframeType.INSTANCE);
        KeyframeRegistry.register(FreezeKeyframeType.INSTANCE);
        KeyframeRegistry.register(TrackEntityKeyframeType.INSTANCE);

        // Client commands are registered from RegisterClientCommandsEvent on Forge.

        this.unsupportedLoader = findUnsupportedLoaders();

        if (LoaderCompat.isModLoaded("distanthorizons")) {
            try {
                int apiMajorVersion = DhApi.getApiMajorVersion();
                if (apiMajorVersion >= 4) {
                    Flashback.LOGGER.info("Distant Horizons detected. Enabling Flashback+Distant Horizons integration");
                    supportsDistantHorizons = true;
                    DistantHorizonsSupport.register();
                } else {
                    Flashback.LOGGER.error("Distant Horizons is installed, but API version is too low ({}). Disabling integration.", apiMajorVersion);
                }
            } catch (Throwable throwable) {
                Flashback.LOGGER.error("Failed to initialize Distant Horizons integration", throwable);
            }
        }

        ClientTickHooks.END_CLIENT_TICK.register(minecraft -> {
            updateIsInReplay();

            AccurateEntityPositionHandler.tick();

            if (Flashback.isInReplay() && !Flashback.isExporting()) {
                EditorState editorState = EditorStateManager.getCurrent();
                if (editorState != null) {
                    editorState.recordCameraMovement();
                }
            }

            // Fix for camera entity sometimes being incorrect when respawning
            Entity camera = Minecraft.getInstance().cameraEntity;
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && camera != null && camera != player) {
                if (camera.isRemoved()) {
                    Entity other = player.level().getEntity(camera.getId());
                    if (other != null && !other.isRemoved()) {
                        Minecraft.getInstance().setCameraEntity(other);
                    }
                }
            }
        });

        ClientTickHooks.START_CLIENT_TICK.register(minecraft -> {
            if (unsupportedLoader != null &&
                    System.currentTimeMillis() > Flashback.getConfig().nextUnsupportedModLoaderWarning &&
                    canReplaceScreen(Minecraft.getInstance().screen)) {
                String warning = String.format("""
                    You are using an unsupported modloader: %s

                    Do not report crashes, bugs or other issues to Flashback

                    You will not receive support from Flashback

                    If you need assistance, please contact %s
                    """, unsupportedLoader, unsupportedLoader);

                Minecraft.getInstance().setScreen(new UnsupportedLoaderScreen(Minecraft.getInstance().screen,
                    Component.literal("Flashback: Unsupported"), Component.literal(warning)));
            }

            if (!pendingReplayRecovery.isEmpty() && canReplaceScreen(Minecraft.getInstance().screen)) {
                Component title = Component.literal("Flashback: Recovery");
                Component description = Component.empty()
                     .append(Component.literal("Flashback has detected ").append(Component.literal("unfinished recordings\n").withStyle(ChatFormatting.YELLOW)))
                     .append(Component.literal("This is usually because the game closed unexpectedly while recording\n\n"))
                     .append(Component.literal("Unfortunately, up to 5 minutes of gameplay from the end of the recording may be lost\n\n").withStyle(ChatFormatting.RED)
                     .append(Component.literal("Would you like to try to recover the recording?").withStyle(ChatFormatting.GREEN)));
                Minecraft.getInstance().setScreen(new RecoverRecordingsScreen(Minecraft.getInstance().screen, title, description, recover -> {
                    switch (recover) {
                        case RECOVER -> {
                            pendingReplaySave.addAll(pendingReplayRecovery);
                            pendingReplayRecovery.clear();
                        }
                        case SKIP -> {
                            pendingReplayRecovery.clear();
                        }
                        case DELETE -> {
                            TempFolderProvider.tryDeleteStaleFolders(TempFolderProvider.TempFolderType.RECORDING);
                            pendingReplayRecovery.clear();
                        }
                    }
                }));
            }

            if (!pendingReplaySave.isEmpty()) {
                Screen currentScreen = Minecraft.getInstance().screen;
                if (canReplaceScreen(currentScreen)) {
                    Path recordFolder = pendingReplaySave.get(0);

                    LocalDateTime dateTime = LocalDateTime.now();
                    dateTime = dateTime.withNano(0);
                    Minecraft.getInstance().setScreen(new SaveReplayScreen(Minecraft.getInstance().screen,
                        recordFolder, dateTime.toString()));
                }
            }

            if (Minecraft.getInstance().level != null && delayedStartRecording > 0) {
                IntegratedServer integratedServer = Minecraft.getInstance().getSingleplayerServer();
                if (integratedServer != null && integratedServer.getClass() != IntegratedServer.class) {
                    delayedStartRecording = 0; // Only allow on actual integrated servers, not replay servers or any other custom server a mod might spin up
                } else if (Flashback.getConfig().automaticallyStart && RECORDER == null) {
                    delayedStartRecording -= 1;
                    if (delayedStartRecording == 0) {
                        startRecordingReplay();
                    }
                } else {
                    delayedStartRecording = 0;
                }
            }

            if (delayedOpenConfig) {
                if (Minecraft.getInstance().screen == null) {
                    Minecraft.getInstance().setScreen(new ConfigScreen(null));
                }
                delayedOpenConfig = false;
            }

            updateIsInReplay();
        });

	}

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            ClientTickHooks.START_CLIENT_TICK.fire(Minecraft.getInstance());
        } else {
            ClientTickHooks.END_CLIENT_TICK.fire(Minecraft.getInstance());
        }
    }

    @SubscribeEvent
    public void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        if (!Flashback.isInReplay() && Flashback.getConfig().automaticallyStart && RECORDER == null) {
            delayedStartRecording = 20;
        }
    }

    @SubscribeEvent
    public void registerClientCommands(RegisterClientCommandsEvent event) {
        var flashback = Commands.literal("flashback");
        flashback.then(Commands.literal("start").executes(this::startRecordingReplay));
        flashback.then(Commands.literal("finish").executes(this::finishRecordingReplay));
        flashback.then(Commands.literal("end").executes(this::finishRecordingReplay));
        flashback.then(Commands.literal("config").executes(this::openFlashbackConfig));
        flashback.then(Commands.literal("mark")
            .executes(command -> {
                this.addMarker(command, null, null, null);
                return 0;
            }).then(Commands.argument("color", BetterColorArgument.color()).executes(command -> {
                int colour = command.getArgument("color", Integer.class);
                this.addMarker(command, colour, null, null);
                return 0;
            }).then(Commands.argument("savePosition", BoolArgumentType.bool()).executes(command -> {
                int colour = command.getArgument("color", Integer.class);
                boolean savePosition = command.getArgument("savePosition", Boolean.class);
                this.addMarker(command, colour, savePosition, null);
                return 0;
            }).then(Commands.argument("description", StringArgumentType.greedyString()).executes(command -> {
                int colour = command.getArgument("color", Integer.class);
                boolean savePosition = command.getArgument("savePosition", Boolean.class);
                String description = command.getArgument("description", String.class);
                this.addMarker(command, colour, savePosition, description);
                return 0;
            })))));
        event.getDispatcher().register(flashback);
    }

    public static List<String> getReplayIncompatibleMods() {
        List<String> incompatible = new ArrayList<>();
        if (LoaderCompat.isModLoaded("vmp")) {
            incompatible.add("VeryManyPlayers (vmp)");
        }
        if (LoaderCompat.isModLoaded("c2me")) {
            incompatible.add("Concurrent Chunk Management Engine (c2me)");
        }
        return incompatible;
    }

    private static @Nullable String findUnsupportedLoaders() {
        if (LoaderCompat.isModLoaded("feather")) {
            return "Feather Client";
        } else {
            return null;
        }
    }

    private static boolean canReplaceScreen(Screen screen) {
        return screen == null || screen instanceof PauseScreen || screen instanceof TitleScreen
            || screen instanceof RealmsMainScreen || screen instanceof JoinMultiplayerScreen;
    }

    private void addMarker(CommandContext<CommandSourceStack> command, @Nullable Integer colour, @Nullable Boolean savePosition, @Nullable String description) {
        if (RECORDER == null) {
            command.getSource().sendFailure(Component.literal("Not recording"));
            return;
        }

        ReplayMarker.MarkerPosition position = null;
        if (savePosition == null || savePosition) {
            Entity camera = Minecraft.getInstance().getCameraEntity();
            if (camera != null) {
                position = new ReplayMarker.MarkerPosition(camera.getEyePosition().toVector3f(),
                    camera.level().dimension().toString());
            }
        }

        String feedback;
        if (colour != null) {
            feedback = "Added #" + Integer.toHexString(colour) + " marker";
        } else {
            feedback = "Added marker";
        }

        if (position != null) {
            feedback += String.format(" at %.2f, %.2f, %.2f", position.position().x, position.position().y, position.position().z);
        }

        if (colour == null) {
            colour = 0xFF5555;
        }

        if (description != null) {
            description = description.trim();
            if (description.isEmpty()) {
                description = null;
            }
        }

        String feedbackMessage = feedback;
        command.getSource().sendSuccess(() -> Component.literal(feedbackMessage), false);
        RECORDER.addMarker(new ReplayMarker(colour, position, description));
    }

    private void deleteUnusedReplayStates() {
        Path flashbackDir = Flashback.getDataDirectory();
        Path replayDir = flashbackDir.resolve("replays");
        Path replayStatesDir = flashbackDir.resolve("editor_states");

        if (!Files.exists(replayDir) || !Files.isDirectory(replayDir)) {
            return;
        }
        if (!Files.exists(replayStatesDir) || !Files.isDirectory(replayStatesDir)) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            Map<UUID, Path> replayStates = new HashMap<>();

            // Find existing replay states
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(replayStatesDir)) {
                for (Path path : directoryStream) {
                    String filename = path.getFileName().toString();

                    String withoutExtension = null;

                    if (filename.endsWith(".json")) {
                        withoutExtension = filename.substring(0, filename.length() - 5);
                    } else if (filename.endsWith(".json.old")) {
                        withoutExtension = filename.substring(0, filename.length() - 9);
                    }

                    if (withoutExtension != null) {
                        UUID uuid;
                        try {
                            uuid = UUID.fromString(withoutExtension);
                        } catch (Exception ignored) {
                            continue;
                        }

                        replayStates.put(uuid, path);
                    }
                }
            } catch (IOException ignored) {}

            // Find which uuids are still valid because they have replays
            Set<UUID> replayUuids = new HashSet<>();
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(replayDir)) {
                for (Path path : directoryStream) {
                    if (!path.toString().endsWith(".zip")) {
                        continue;
                    }

                    String metadataString = null;

                    try (FileSystem fs = FileSystems.newFileSystem(path)) {
                        Path metadataPath = fs.getPath("/metadata.json");
                        if (Files.exists(metadataPath)) {
                            metadataString = Files.readString(metadataPath);
                        }
                    } catch (IOException ignored) {
                        continue;
                    }

                    if (metadataString != null) {
                        JsonObject metadataJson = new Gson().fromJson(metadataString, JsonObject.class);
                        FlashbackMeta metadata = FlashbackMeta.fromJson(metadataJson);
                        if (metadata != null) {
                            replayUuids.add(metadata.replayIdentifier);
                        }
                    }
                }
            } catch (IOException e) {
                Flashback.LOGGER.error("Unable to iterate replay directory", e);
                return;
            }

            for (Map.Entry<UUID, Path> entry : replayStates.entrySet()) {
                if (!replayUuids.contains(entry.getKey())) {
                    try {
                        Files.deleteIfExists(entry.getValue());
                    } catch (IOException ignored) {}
                }
            }
        }, Util.backgroundExecutor());
    }

    public static FlashbackConfig getConfig() {
        return config;
    }

    @Nullable
    public static ReplayServer getReplayServer() {
        if (Minecraft.getInstance().getSingleplayerServer() instanceof ReplayServer replayServer) {
            return replayServer;
        }
        return null;
    }

    public static void removePendingReplaySave(Path recordFolder) {
        pendingReplaySave.remove(recordFolder);
    }

    public static boolean isExporting() {
        return EXPORT_JOB != null && EXPORT_JOB.isRunning();
    }

    public static void updateIsInReplay() {
        isInReplay = Minecraft.getInstance().getSingleplayerServer() instanceof ReplayServer;
    }

    public static boolean isInReplay() {
        return isInReplay;
    }

    private int startRecordingReplay(CommandContext<CommandSourceStack> command) {
        startRecordingReplay();
        return 0;
    }

    private int finishRecordingReplay(CommandContext<CommandSourceStack> command) {
        finishRecordingReplay();
        return 0;
    }

    private int openFlashbackConfig(CommandContext<CommandSourceStack> command) {
        delayedOpenConfig = true;
        return 0;
    }

    public static void startRecordingReplay() {
        if (RECORDER != null) {
            SystemToast.add(Minecraft.getInstance().getToasts(), FlashbackSystemToasts.RECORDING_TOAST,
                    Component.literal("Already Recording"), Component.literal("Cannot start new recording when already recording"));
            return;
        }
        RECORDER = new Recorder();
        if (supportsDistantHorizons) {
            try {
                RECORDER.putDistantHorizonsPaths(DistantHorizonsSupport.getDimensionPaths());
            } catch (Throwable throwable) {
                Flashback.LOGGER.warn("Unable to capture Distant Horizons save paths when recording started", throwable);
            }
        }
        if (Flashback.getConfig().showRecordingToasts) {
            SystemToast.add(Minecraft.getInstance().getToasts(), FlashbackSystemToasts.RECORDING_TOAST,
                    Component.literal("Flashback"), Component.literal("Started recording"));
        }
    }

    public static void pauseRecordingReplay(boolean pause) {
        RECORDER.setPaused(pause);

        if (Flashback.getConfig().showRecordingToasts) {
            SystemToast.add(Minecraft.getInstance().getToasts(), FlashbackSystemToasts.RECORDING_TOAST,
                    Component.literal("Flashback"), Component.literal(pause ? "Paused recording" : "Unpaused recording"));
        }
    }

    public static void cancelRecordingReplay() {
        Recorder recorder = RECORDER;
        RECORDER = null;

        Path recordFolder = recorder.finish();
        try {
            FileUtils.deleteDirectory(recordFolder.toFile());
        } catch (Exception e) {
            Flashback.LOGGER.error("Exception deleting record folder", e);
        }

        if (Flashback.getConfig().showRecordingToasts) {
            SystemToast.add(Minecraft.getInstance().getToasts(), FlashbackSystemToasts.RECORDING_TOAST,
                    Component.literal("Flashback"), Component.literal("Cancelled recording"));
        }
    }

    public static void finishRecordingReplay() {
        if (RECORDER == null) {
            SystemToast.add(Minecraft.getInstance().getToasts(), FlashbackSystemToasts.RECORDING_TOAST,
                    Component.literal("Not Recording"), Component.literal("Cannot finish recording when not recording"));
            return;
        }

        Recorder recorder = RECORDER;
        RECORDER = null;
        recorder.endTick(true);

        if (Flashback.getConfig().quicksave) {
            Path replayDir = getReplayFolder();

            String filename;
            try {
                LocalDateTime dateTime = LocalDateTime.now();
                dateTime = dateTime.withNano(0);
                filename = FileUtil.findAvailableName(replayDir, dateTime.toString(), ".zip");
            } catch (IOException e) {
                Flashback.LOGGER.error("Error while trying to determine filename", e);
                filename = UUID.randomUUID() + ".zip";
            }

            Path outputFile = replayDir.resolve(filename);
            ReplayExporter.export(recorder.finish(), outputFile, null);
        } else {
            pendingReplaySave.add(recorder.finish());
        }

        if (Flashback.getConfig().showRecordingToasts) {
            SystemToast.add(Minecraft.getInstance().getToasts(), FlashbackSystemToasts.RECORDING_TOAST,
                    Component.literal("Flashback"), Component.literal("Finished recording"));
        }
    }

    @Nullable
    public static AbstractClientPlayer getSpectatingPlayer() {
        if (!isInReplay()) {
            return null;
        }
        if (Minecraft.getInstance().getCameraEntity() instanceof AbstractClientPlayer clientPlayer) {
            if (clientPlayer != Minecraft.getInstance().player) {
                return clientPlayer;
            }
        }
        return null;
    }

    public static void openReplayFromFileBrowser() {
        String defaultFolder = Flashback.getReplayFolder().toString();
        AsyncFileDialogs.openFileDialog(defaultFolder, "Zip File", "zip").thenAccept(pathStr -> {
            if (pathStr != null) {
                Path path = Path.of(pathStr);
                Minecraft.getInstance().submit(() -> {
                    Flashback.openReplayWorld(path);
                });
            }
        });
    }

    public static void openReplayWorld(Path path) {
        // Disconnect
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            minecraft.level.disconnect();
        }
        minecraft.clearLevel();
        minecraft.setScreen(new TitleScreen());

        // Add as recent
        String pathStr = path.toString();
        FlashbackConfig config = Flashback.getConfig();
        config.recentReplays.remove(pathStr);
        config.recentReplays.add(0, pathStr);
        if (config.recentReplays.size() > 32) {
            config.recentReplays.remove(config.recentReplays.size() - 1);
        }
        config.saveToDefaultFolder();

        // Actually load
        try {
            UUID replayUuid = UUID.randomUUID();
            Path replayTemp = TempFolderProvider.createTemp(TempFolderProvider.TempFolderType.SERVER, replayUuid);
            FileUtils.deleteDirectory(replayTemp.toFile());

            LevelStorageSource source = new LevelStorageSource(replayTemp.resolve("saves"), replayTemp.resolve("backups"),
                Minecraft.getInstance().getLevelSource().getWorldDirValidator(), Minecraft.getInstance().getFixerUpper());
            LevelStorageSource.LevelStorageAccess access = source.createAccess("replay");

            // Forge's login handshake (NetworkHooks -> ConfigSync) reads serverconfig/forge-server.toml off
            // disk on the connection's own Netty thread, racing the server thread that writes the
            // corrected defaults into that same file during initServer(). For a real world save that file
            // already has valid content from a previous session, so the race never matters; for our
            // brand-new temp folder an empty/missing file needs correcting, and losing the race throws a
            // NoSuchFileException that Forge turns into a client disconnect. Writing the fully-corrected
            // defaults up front (matching what ForgeConfigSpec would otherwise fill in) means there's
            // nothing left to correct, so no write-back race can happen.
            Path serverConfigDir = replayTemp.resolve("saves").resolve("replay").resolve("serverconfig");
            Files.createDirectories(serverConfigDir);
            Path serverConfigFile = serverConfigDir.resolve("forge-server.toml");
            if (!Files.exists(serverConfigFile)) {
                Files.writeString(serverConfigFile, """
                    [server]
                        removeErroringBlockEntities = false
                        removeErroringEntities = false
                        fullBoundingBoxLadders = false
                        zombieBaseSummonChance = 0.1
                        zombieBabyChance = 0.05
                        permissionHandler = "forge:default_handler"
                        advertiseDedicatedServerToLan = true
                        useItemWithDurationZero = false
                    """);
            }

            PackRepository packRepository = ServerPacksSource.createPackRepository(access);

            packRepository.reload();

            GameRules gameRules = new GameRules();
            gameRules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
            gameRules.getRule(GameRules.RULE_DOENTITYDROPS).set(false, null);
            gameRules.getRule(GameRules.RULE_ANNOUNCE_ADVANCEMENTS).set(false, null);
            gameRules.getRule(GameRules.RULE_DISABLE_RAIDS).set(true, null);
            gameRules.getRule(GameRules.RULE_DO_PATROL_SPAWNING).set(false, null);
            gameRules.getRule(GameRules.RULE_DO_WARDEN_SPAWNING).set(false, null);
            gameRules.getRule(GameRules.RULE_DO_TRADER_SPAWNING).set(false, null);
            gameRules.getRule(GameRules.RULE_DO_VINES_SPREAD).set(false, null);
            gameRules.getRule(GameRules.RULE_DOFIRETICK).set(false, null);
            gameRules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
            gameRules.getRule(GameRules.RULE_RANDOMTICKING).set(0, null);

            WorldDataConfiguration worldDataConfiguration = new WorldDataConfiguration(new DataPackConfig(List.of(), List.of()), FeatureFlags.DEFAULT_FLAGS);
            LevelSettings levelSettings = new LevelSettings("Replay", GameType.SPECTATOR, false, Difficulty.NORMAL, true, gameRules, worldDataConfiguration);
            WorldLoader.PackConfig packConfig = new WorldLoader.PackConfig(packRepository, worldDataConfiguration, false, true);
            WorldLoader.InitConfig initConfig = new WorldLoader.InitConfig(packConfig, Commands.CommandSelection.DEDICATED, 4);

            WorldStem worldStem = Util.blockUntilDone(executor -> WorldLoader.load(initConfig, dataLoadContext -> {
                Holder.Reference<Biome> plains = dataLoadContext.datapackWorldgen().registryOrThrow(Registries.BIOME).getHolder(Biomes.PLAINS).get();
                Holder.Reference<DimensionType> overworld = dataLoadContext.datapackWorldgen().registryOrThrow(Registries.DIMENSION_TYPE).getHolder(BuiltinDimensionTypes.OVERWORLD).get();

                WritableRegistry<LevelStem> registryOverworld = new MappedRegistry<>(Registries.LEVEL_STEM, Lifecycle.stable());
                registryOverworld.register(LevelStem.OVERWORLD,  new LevelStem(overworld, new EmptyLevelSource(plains)), Lifecycle.stable());
                registryOverworld.freeze();

                Registry<LevelStem> registryEmpty = new MappedRegistry<>(Registries.LEVEL_STEM, Lifecycle.stable()).freeze();

                WorldDimensions worldDimensions = new WorldDimensions(registryOverworld);
                WorldDimensions.Complete complete = worldDimensions.bake(registryEmpty);

                return new WorldLoader.DataLoadOutput<>(new PrimaryLevelData(levelSettings, new WorldOptions(0L, false, false),
                    complete.specialWorldProperty(), complete.lifecycle()), complete.dimensionsRegistryAccess());
            }, WorldStem::new, Util.backgroundExecutor(), executor)).get();

            ((MinecraftExt)Minecraft.getInstance()).flashback$startReplayServer(access, packRepository, worldStem, replayUuid, path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
