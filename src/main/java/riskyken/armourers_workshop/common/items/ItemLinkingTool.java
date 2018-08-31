package riskyken.armourers_workshop.common.items;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;
import riskyken.armourers_workshop.common.blocks.BlockLocation;
import riskyken.armourers_workshop.common.blocks.BlockSkinnable;
import riskyken.armourers_workshop.common.lib.LibItemNames;
import riskyken.armourers_workshop.common.tileentities.TileEntitySkinnable;

public class ItemLinkingTool extends AbstractModItem {

    private static final String TAG_LINK_LOCATION = "linkLocation";
    
    public ItemLinkingTool() {
        super(LibItemNames.LINKING_TOOL);
        setSortPriority(7);
    }
    
    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
        if (!world.isRemote) {
            ItemStack stack = player.getHeldItem(hand);
            IBlockState state = world.getBlockState(pos);
            Block block = state.getBlock();
            if (!hasLinkLocation(stack)) {
                if (!(block instanceof BlockSkinnable)) {
                    setLinkLocation(stack, new BlockLocation(pos.getX(), pos.getY(), pos.getZ()));
                    player.sendMessage(new TextComponentTranslation("chat.armourersworkshop:linkingTool.start", (Object)null));
                    return EnumActionResult.SUCCESS;
                } else {
                    player.sendMessage(new TextComponentTranslation("chat.armourersworkshop:linkingTool.linkedToSkinnable", (Object)null));
                    return EnumActionResult.SUCCESS;
                }
            } else {
                BlockLocation loc = getLinkLocation(stack);
                if (block instanceof BlockSkinnable) {
                    TileEntity te = world.getTileEntity(pos);
                    if (te != null && te instanceof TileEntitySkinnable) {
                        ((TileEntitySkinnable)te).getParent().setLinkedBlock(loc);
                        player.sendMessage(new TextComponentTranslation("chat.armourersworkshop:linkingTool.finish", (Object)null));
                        removeLinkLocation(stack);
                        return EnumActionResult.SUCCESS;
                    }
                }
            }
            removeLinkLocation(stack);
            player.sendMessage(new TextComponentTranslation("chat.armourersworkshop:linkingTool.fail", (Object)null));
        }
        return EnumActionResult.PASS;
    }
    
    private void setLinkLocation(ItemStack stack, BlockLocation loc) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setIntArray(TAG_LINK_LOCATION, new int[] {loc.x, loc.y, loc.z});
    }
    
    private void removeLinkLocation(ItemStack stack) {
        if (stack.hasTagCompound()) {
            stack.getTagCompound().removeTag(TAG_LINK_LOCATION);
        }
    }
    
    private boolean hasLinkLocation(ItemStack stack) {
        if (stack.hasTagCompound()) {
            return stack.getTagCompound().hasKey(TAG_LINK_LOCATION, NBT.TAG_INT_ARRAY);
        }
        return false;
    }
    
    private BlockLocation getLinkLocation(ItemStack stack) {
        if (hasLinkLocation(stack)) {
            int[] loc = stack.getTagCompound().getIntArray(TAG_LINK_LOCATION);
            return new BlockLocation(loc[0], loc[1], loc[2]);
        }
        return new BlockLocation(0, 0, 0);
    }
}