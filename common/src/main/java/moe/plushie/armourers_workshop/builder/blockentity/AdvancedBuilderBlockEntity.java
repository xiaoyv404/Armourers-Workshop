package moe.plushie.armourers_workshop.builder.blockentity;

import moe.plushie.armourers_workshop.api.common.IBlockEntityHandler;
import moe.plushie.armourers_workshop.core.blockentity.UpdatableBlockEntity;
import moe.plushie.armourers_workshop.core.data.UserNotifications;
import moe.plushie.armourers_workshop.core.data.transform.SkinItemTransforms;
import moe.plushie.armourers_workshop.core.network.UpdateSkinDocumentPacket;
import moe.plushie.armourers_workshop.core.skin.Skin;
import moe.plushie.armourers_workshop.core.skin.SkinDescriptor;
import moe.plushie.armourers_workshop.core.skin.SkinLoader;
import moe.plushie.armourers_workshop.core.skin.document.SkinDocument;
import moe.plushie.armourers_workshop.core.skin.document.SkinDocumentExporter;
import moe.plushie.armourers_workshop.core.skin.document.SkinDocumentImporter;
import moe.plushie.armourers_workshop.core.skin.document.SkinDocumentListeners;
import moe.plushie.armourers_workshop.core.skin.document.SkinDocumentNode;
import moe.plushie.armourers_workshop.core.skin.document.SkinDocumentProvider;
import moe.plushie.armourers_workshop.core.skin.document.SkinDocumentSettings;
import moe.plushie.armourers_workshop.core.skin.exception.TranslatableException;
import moe.plushie.armourers_workshop.init.environment.EnvironmentExecutor;
import moe.plushie.armourers_workshop.init.platform.NetworkManager;
import moe.plushie.armourers_workshop.utils.BlockUtils;
import moe.plushie.armourers_workshop.utils.SkinUtils;
import moe.plushie.armourers_workshop.utils.math.Rectangle3f;
import moe.plushie.armourers_workshop.utils.math.Vector3f;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Collection;
import java.util.Optional;

public class AdvancedBuilderBlockEntity extends UpdatableBlockEntity implements IBlockEntityHandler, SkinDocumentProvider {

    private AABB renderBoundingBox;

    public final Vector3f carmeOffset = new Vector3f();
    public final Vector3f carmeRot = new Vector3f();
    public final Vector3f carmeScale = new Vector3f(1, 1, 1);

    public Vector3f offset = new Vector3f(0, 12, 0);

    private final SkinDocument document = new SkinDocument();

    public Vector3f getRenderOrigin() {
        BlockPos pos = getBlockPos();
        return new Vector3f(
                pos.getX() + offset.getX() + 0.5f,
                pos.getY() + offset.getY() + 0.5f,
                pos.getZ() + offset.getZ() + 0.5f
        );
    }

    public AdvancedBuilderBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
        this.document.addListener(new SkinDocumentListeners.Updater(this));
    }

    @Override
    public void readFromNBT(CompoundTag tag) {
        document.deserializeNBT(tag);
    }

    @Override
    public void writeToNBT(CompoundTag tag) {
        document.serializeNBT(tag);
    }

    public void importToNode(String identifier, Skin skin, SkinDocumentNode node) {
        SkinDescriptor descriptor = new SkinDescriptor(identifier, skin.getType());
        node.setSkin(descriptor);
        CompoundTag tag = new CompoundTag();
        tag.putOptionalSkinDescriptor(SkinDocumentNode.Keys.SKIN, descriptor, null);
        UpdateSkinDocumentPacket.Action action = new UpdateSkinDocumentPacket.UpdateNodeAction(node.getId(), tag);
        NetworkManager.sendToAll(new UpdateSkinDocumentPacket(this, action));
        if (skin.getItemTransforms() != null) {
            importToSettings(skin.getItemTransforms(), node);
        }
    }

    private void importToSettings(SkinItemTransforms itemTransforms, SkinDocumentNode node) {
        SkinItemTransforms newItemTransforms = new SkinItemTransforms();
        if (document.getItemTransforms() != null) {
            newItemTransforms.putAll(document.getItemTransforms());
        }
        Collection<String> overrideNames = SkinUtils.getItemOverrides(node.getType());
        if (!overrideNames.isEmpty()) {
            overrideNames.forEach(name -> itemTransforms.forEach((type, transform) -> newItemTransforms.put(name + ";" + type, transform)));
        } else {
            newItemTransforms.putAll(itemTransforms);
        }
        document.setItemTransforms(newItemTransforms);
        CompoundTag tag = new CompoundTag();
        tag.putOptionalItemTransforms(SkinDocumentSettings.Keys.ITEM_TRANSFORMS, itemTransforms, null);
        UpdateSkinDocumentPacket.Action action = new UpdateSkinDocumentPacket.UpdateSettingsAction(tag);
        NetworkManager.sendToAll(new UpdateSkinDocumentPacket(this, action));
    }

    public void importToDocument(String identifier, Skin skin) {
        BlockUtils.performBatch(() -> {
            SkinDocumentImporter importer = new SkinDocumentImporter(document);
            document.reset();
            document.setItemTransforms(skin.getItemTransforms());
            importer.execute(identifier, skin);
        });
    }

    public void exportFromDocument(ServerPlayer player) {
        SkinDocumentExporter exporter = new SkinDocumentExporter(document);
        exporter.setItemTransforms(document.getItemTransforms());
        EnvironmentExecutor.runOnBackground(() -> () -> {
            try {
                Skin skin = exporter.execute(player);
                player.server.execute(() -> {
                    String identifier = SkinLoader.getInstance().saveSkin("", skin);
                    SkinDescriptor descriptor = new SkinDescriptor(identifier, skin.getType());
                    ItemStack itemStack = descriptor.asItemStack();
                    player.giveItem(itemStack);
                });
            } catch (TranslatableException exception) {
                UserNotifications.sendErrorMessage(exception.getComponent(), player);
            }
        });
    }

    @Override
    public SkinDocument getDocument() {
        return document;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public AABB getRenderBoundingBox(BlockState blockState) {
        if (renderBoundingBox != null) {
            return renderBoundingBox;
        }
        float s = 16;
        Vector3f origin = getRenderOrigin();
        Rectangle3f rect = new Rectangle3f(origin.getX() - s / 2, origin.getY() - s / 2, origin.getZ() - s / 2, s, s, s);
        renderBoundingBox = rect.asAABB();
        return renderBoundingBox;
    }
}
