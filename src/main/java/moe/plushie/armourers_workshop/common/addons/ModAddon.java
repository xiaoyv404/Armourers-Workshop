package moe.plushie.armourers_workshop.common.addons;

import java.util.ArrayList;

import moe.plushie.armourers_workshop.utils.ModLogger;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


public class ModAddon {
    
    private final String modId;
    private final String modName;
    private final boolean isModLoaded;
    private final ArrayList<String> overrrides;
    
    public ModAddon(String modId, String modName) {
        this.modId = modId;
        this.modName = modName;
        this.isModLoaded = setIsModLoaded();
        if (isModLoaded) {
            ModLogger.log(String.format("Loading %s Compatibility Addon", getModName()));
        }
        this.overrrides = new ArrayList<String>();
    }
    
    protected boolean setIsModLoaded() {
        return Loader.isModLoaded(modId);
    }
    
    public void preInit() {}
    
    public void init() {}
    
    public void postInit() {}
    
    @SideOnly(Side.CLIENT)
    public void initRenderers() {}
    
    public String getModId() {
        return this.modId;
    }
    
    public String getModName() {
        return this.modName;
    }
    
    public boolean isModLoaded() {
        return isModLoaded;
    }
    
    public ArrayList<String> getItemOverrides() {
        return overrrides;
    }
    
    protected void addItemOverride(ItemOverrideType type, String itemName) {
        overrrides.add(type.toString().toLowerCase() + ":" + getModId() + ":" + itemName);
    }
    
    public static enum ItemOverrideType {
        SWORD,
        SHIELD,
        BOW,
        
        PICKAXE,
        AXE,
        SHOVEL,
        HOE,
        
        ITEM;
    }
}
