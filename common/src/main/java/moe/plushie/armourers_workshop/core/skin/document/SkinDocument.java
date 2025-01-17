package moe.plushie.armourers_workshop.core.skin.document;

import moe.plushie.armourers_workshop.api.skin.ISkinPartType;
import moe.plushie.armourers_workshop.api.skin.property.ISkinProperty;
import moe.plushie.armourers_workshop.core.data.transform.SkinItemTransforms;
import moe.plushie.armourers_workshop.core.skin.property.SkinProperties;
import moe.plushie.armourers_workshop.core.skin.property.SkinProperty;
import moe.plushie.armourers_workshop.utils.ObjectUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;


public class SkinDocument {

    private SkinDocumentType type;
    private SkinDocumentNode nodes;

    private SkinProperties properties = new SkinProperties();
    private SkinDocumentSettings settings = new SkinDocumentSettings();
    private final SkinDocumentListeners.Proxy listener = new SkinDocumentListeners.Proxy();

    public SkinDocument() {
        setType(SkinDocumentTypes.GENERAL_ARMOR_HEAD);
    }

    public void reset() {
        setType(type);
    }

    public void setType(SkinDocumentType type) {
        this.type = type;
        this.nodes = _generateDefaultNode(type);
        this.settings = _generateSkinSettings();
        this.properties = _generateSkinProperties();
        this.settings.setListener(listener);
        this.nodes.setListener(listener);
        this.listener.documentDidChangeType(type);
    }

    public void serializeNBT(CompoundTag tag) {
        tag.putOptionalType(Keys.TYPE, type, null);
        tag.put(Keys.NODES, nodes.serializeNBT());
        tag.put(Keys.SETTINGS, settings.serializeNBT());
        tag.putOptionalSkinProperties(Keys.PROPERTIES, properties);
    }

    public void deserializeNBT(CompoundTag tag) {
        type = tag.getOptionalType(Keys.TYPE, SkinDocumentTypes.GENERAL_ARMOR_HEAD, SkinDocumentTypes::byName);
        settings = new SkinDocumentSettings(tag.getCompound(Keys.SETTINGS));
        properties = tag.getOptionalSkinProperties(Keys.PROPERTIES);
        try {
            nodes = null;
            CompoundTag rootTag = tag.getCompound(Keys.NODES);
            if (!rootTag.isEmpty()) {
                nodes = new SkinDocumentNode(rootTag);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        if (nodes == null) {
            nodes = _generateDefaultNode(type);
        }
        settings.setListener(listener);
        nodes.setListener(listener);
        listener.documentDidReload();
    }

    public void updateSettings(CompoundTag tag) {
        settings.deserializeNBT(tag);
        listener.documentDidChangeSettings(tag);
    }

    public void updateProperties(CompoundTag value) {
        properties.putAll(new SkinProperties.Changes(value));
        listener.documentDidChangeProperties(value);
    }

    public <T> void put(ISkinProperty<T> property, T value) {
        properties.put(property, value);
        SkinProperties.Changes changes = new SkinProperties.Changes();
        changes.put(property, value);
        listener.documentDidChangeProperties(changes.serializeNBT());
    }

    public <T> T get(ISkinProperty<T> property) {
        return properties.get(property);
    }

    public void beginEditing() {
        listener.documentWillBeginEditing();
    }

    public void endEditing() {
        listener.documentDidEndEditing();
    }

    public void addListener(SkinDocumentListener listener) {
        this.listener.addListener(listener);
    }

    public void removeListener(SkinDocumentListener listener) {
        this.listener.removeListener(listener);
    }

    public void setItemTransforms(SkinItemTransforms itemTransforms) {
        settings.setItemTransforms(itemTransforms);
    }

    public SkinItemTransforms getItemTransforms() {
        return settings.getItemTransforms();
    }

    public SkinDocumentListener getHandler() {
        return listener;
    }

    public SkinDocumentNode nodeById(String id) {
        return _findNodeById(nodes, id);
    }

    public SkinDocumentNode getRoot() {
        return nodes;
    }

    public SkinDocumentSettings getSettings() {
        return settings;
    }

    public SkinProperties getProperties() {
        return properties;
    }

    public SkinDocumentType getType() {
        return this.type;
    }

    @Override
    public String toString() {
        return ObjectUtils.makeDescription(this, "type", type);
    }

    private SkinDocumentNode _findNodeById(SkinDocumentNode parent, String id) {
        if (id.equals(parent.getId())) {
            return parent;
        }
        for (SkinDocumentNode node : parent.children()) {
            SkinDocumentNode result = _findNodeById(node, id);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private SkinDocumentSettings _generateSkinSettings() {
        SkinDocumentSettings settings1 = new SkinDocumentSettings();
        settings1.setShowsOrigin(settings.showsOrigin());
        settings1.setShowsHelperModel(settings.showsHelperModel());
        return settings1;
    }

    private SkinProperties _generateSkinProperties() {
        String name = properties.get(SkinProperty.ALL_CUSTOM_NAME);
        String flavour = properties.get(SkinProperty.ALL_FLAVOUR_TEXT);
        SkinProperties properties = new SkinProperties();
        properties.put(SkinProperty.ALL_CUSTOM_NAME, name);
        properties.put(SkinProperty.ALL_FLAVOUR_TEXT, flavour);
        return properties;
    }

    private SkinDocumentNode _generateDefaultNode(SkinDocumentType category) {
        SkinDocumentNode root = new SkinDocumentNode("root", null);
        for (ISkinPartType partType : category.getSkinPartTypes()) {
            ResourceLocation registryName = partType.getRegistryName();
            SkinDocumentNode node = new SkinDocumentNode(registryName.getPath(), null);
            node.setType(partType);
            root.add(node);
        }
        root.add(new SkinDocumentNode("float", null));
        root.add(new SkinDocumentNode("static", null));
        return root;
    }

    protected static class Keys {
        public static final String NODES = "Nodes";
        public static final String TYPE = "Type";
        public static final String SETTINGS = "Settings";
        public static final String PROPERTIES = "Properties";
    }
}
