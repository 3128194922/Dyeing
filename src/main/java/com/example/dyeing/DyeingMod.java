package com.example.dyeing;

import com.example.dyeing.client.ClientPaintManager;
import com.example.dyeing.client.PaintRenderLayer;
import com.example.dyeing.command.DyeingCommand;
import com.example.dyeing.data.PaintData;
import com.example.dyeing.data.PaintSavedData;
import com.example.dyeing.network.DyeingNetwork;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.UUID;

@Mod(DyeingMod.MODID)
public class DyeingMod {
    public static final String MODID = "dyeing";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DyeingMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(DyeingNetwork::register);
    }

    public static PaintSavedData getPaintData(net.minecraft.server.MinecraftServer server) {
        return PaintSavedData.get(server);
    }

    public static void syncFull(ServerPlayer player) {
        DyeingNetwork.sendFullSync(player, getPaintData(player.server).getEntries());
    }

    public static void broadcastUpdate(UUID entityUuid, PaintData paintData) {
        DyeingNetwork.broadcastUpdate(entityUuid, paintData);
    }

    public static void broadcastRemove(UUID entityUuid) {
        DyeingNetwork.broadcastRemove(entityUuid);
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeEvents {
        private ForgeEvents() {
        }

        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            DyeingCommand.register(event.getDispatcher());
        }

        @SubscribeEvent
        public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                syncFull(serverPlayer);
            }
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ClientModEvents {
        private ClientModEvents() {
        }

        @SubscribeEvent
        public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
            for (EntityType<?> entityType : ForgeRegistries.ENTITY_TYPES.getValues()) {
                EntityRenderer<?> renderer = event.getEntityRenderer(entityType);
                tryAddLayer(renderer);
            }

            for (String skin : event.getSkins()) {
                EntityRenderer<?> renderer = event.getPlayerSkin(skin);
                tryAddLayer(renderer);
            }
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private static void tryAddLayer(EntityRenderer<?> renderer) {
            if (renderer instanceof LivingEntityRenderer<?, ?> livingRenderer) {
                livingRenderer.addLayer(new PaintRenderLayer(livingRenderer));
            }
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ClientForgeEvents {
        private ClientForgeEvents() {
        }

        @SubscribeEvent
        public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            ClientPaintManager.clear();
        }
    }
}
