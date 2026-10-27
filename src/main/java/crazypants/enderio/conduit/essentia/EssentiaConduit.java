package crazypants.enderio.conduit.essentia;

import static crazypants.enderio.conduit.item.ItemConduit.EXTERNAL_INTERFACE_GEOM;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.enderio.core.client.render.IconUtil;
import com.enderio.core.common.util.BlockCoord;
import com.enderio.core.common.util.DyeColor;

import crazypants.enderio.EnderIO;
import crazypants.enderio.conduit.AbstractConduit;
import crazypants.enderio.conduit.AbstractConduitNetwork;
import crazypants.enderio.conduit.ConduitUtil;
import crazypants.enderio.conduit.ConnectionMode;
import crazypants.enderio.conduit.IConduit;
import crazypants.enderio.conduit.IConduitBundle;
import crazypants.enderio.conduit.RaytraceResult;
import crazypants.enderio.conduit.geom.CollidableComponent;
import crazypants.enderio.conduit.render.ConduitRenderer;
import crazypants.enderio.config.Config;
import crazypants.enderio.machine.RedstoneControlMode;
import crazypants.enderio.tool.ToolUtil;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.nodes.INode;

public class EssentiaConduit extends AbstractConduit implements IEssentiaConduit {

    public static final String ICON_KEY = "enderio:essentiaConduit";
    public static final String ICON_KEY_CORE = "enderio:essentiaConduitCore";
    public static final String ICON_KEY_INPUT = "enderio:essentiaConduitInput";
    public static final String ICON_KEY_OUTPUT = "enderio:essentiaConduitOutput";
    public static final String ICON_KEY_IN_OUT_OUT = "enderio:essentiaConduitInOut_Out";
    public static final String ICON_KEY_IN_OUT_IN = "enderio:essentiaConduitInOut_In";
    public static final String ICON_KEY_IN_OUT_BG = "enderio:essentiaConduitIoConnector";

    static final Map<String, IIcon> ICONS = new HashMap<>();

    public static void initIcons() {
        IconUtil.addIconProvider(new IconUtil.IIconProvider() {

            @Override
            public void registerIcons(IIconRegister register) {
                ICONS.put(ICON_KEY, register.registerIcon(ICON_KEY));
                ICONS.put(ICON_KEY_CORE, register.registerIcon(ICON_KEY_CORE));
                ICONS.put(ICON_KEY_INPUT, register.registerIcon(ICON_KEY_INPUT));
                ICONS.put(ICON_KEY_OUTPUT, register.registerIcon(ICON_KEY_OUTPUT));
                ICONS.put(ICON_KEY_IN_OUT_OUT, register.registerIcon(ICON_KEY_IN_OUT_OUT));
                ICONS.put(ICON_KEY_IN_OUT_IN, register.registerIcon(ICON_KEY_IN_OUT_IN));
                ICONS.put(ICON_KEY_IN_OUT_BG, register.registerIcon(ICON_KEY_IN_OUT_BG));
            }

            @Override
            public int getTextureType() {
                return 0;
            }
        });
    }

    private EssentiaConduitNetwork network;
    private final EssentiaConduitMetadata metadata;

    public EssentiaConduit() {
        this(EssentiaConduitTier.BASIC);
    }

    public EssentiaConduit(EssentiaConduitTier tier) {
        metadata = new EssentiaConduitMetadata(tier);
        // updateFromNonUpgradeableVersion();
    }

    @Override
    public ConduitRenderer getRenderer() {
        return EssentiaConduitRenderer.instance.get();
    }

    @Override
    public IAspectContainer getExternalContainer(ForgeDirection direction) {
        World world = getBundle().getWorld();
        if (world == null) {
            return null;
        }
        BlockCoord loc = getLocation().getLocation(direction);
        TileEntity te = world.getTileEntity(loc.x, loc.y, loc.z);
        // why does INode extend this
        if (te instanceof IAspectContainer && !(te instanceof IConduitBundle) && !(te instanceof INode)) {
            return (IAspectContainer) te;
        }
        return null;
    }

    @Override
    public boolean canConnectToExternal(ForgeDirection direction, boolean ignoreDisabled) {
        return getExternalContainer(direction) != null;
    }

    @Override
    public boolean canExtractFromDir(ForgeDirection dir) {
        return getConnectionMode(dir).acceptsInput();
    }

    @Override
    public boolean canInsertToDir(ForgeDirection dir) {
        return getConnectionMode(dir).acceptsOutput();
    }

    @Override
    public int getMaximumExtracted(ForgeDirection dir) {
        EssentiaConduitTier tier = metadata.getTier();
        return switch (tier) {
            case BASIC -> Config.essentiaConduitRate;
            case VOID -> Config.essentiaConduitVoidRate;
            case ICHOR -> Integer.MAX_VALUE;
        };
    }

    @Override
    protected ConnectionMode getDefaultConnectionMode() {
        return ConnectionMode.INPUT;
    }

    @Override
    public Class<? extends IConduit> getBaseConduitType() {
        return IEssentiaConduit.class;
    }

    @Override
    public boolean onBlockActivated(EntityPlayer player, RaytraceResult res, List<RaytraceResult> all) {
        if (ConduitUtil.isProbeEquipped(player)) {
            if (!player.worldObj.isRemote) {
                // TODO: conduit probe code
            }
            return true;
        } else if (ToolUtil.isToolEquipped(player)) {
            if (!getBundle().getEntity().getWorldObj().isRemote) {
                if (res != null && res.component != null) {
                    ForgeDirection connDir = res.component.dir;
                    ForgeDirection faceHit = ForgeDirection.getOrientation(res.movingObjectPosition.sideHit);
                    if (connDir == ForgeDirection.UNKNOWN || connDir == faceHit) {
                        if (getConnectionMode(faceHit) == ConnectionMode.DISABLED) {
                            setConnectionMode(faceHit, getNextConnectionMode(faceHit));
                            return true;
                        }
                        // Attempt to join networks
                        return ConduitUtil.joinConduits(this, faceHit);
                    } else if (externalConnections.contains(connDir)) {
                        setConnectionMode(connDir, getNextConnectionMode(connDir));
                        return true;
                    } else if (containsConduitConnection(connDir)) {
                        ConduitUtil.disconectConduits(this, connDir);
                        return true;
                    }
                }
            }
        } else {
            if (res != null && res.component != null) {
                ForgeDirection connDir = res.component.dir;
                if (connDir != null && connDir != ForgeDirection.UNKNOWN && containsExternalConnection(connDir)) {
                    if (!player.worldObj.isRemote) {
                        // TODO: conduit probe code
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public ItemStack createItem() {
        return new ItemStack(EnderIO.itemEssentiaConduit, 1, metadata.getOrdinalTier());
    }

    @Override
    public AbstractConduitNetwork<?, ?> getNetwork() {
        return network;
    }

    @Override
    public boolean setNetwork(AbstractConduitNetwork<?, ?> network) {
        this.network = (EssentiaConduitNetwork) network;
        return true;
    }

    @Override
    public AbstractConduitNetwork<?, ?> createNetworkForType() {
        return new EssentiaConduitNetwork();
    }

    @Override
    public IIcon getTransmitionTextureForState(CollidableComponent component) {
        return null;
    }

    @Override
    public IIcon getTextureForState(CollidableComponent component) {
        if (component.dir == ForgeDirection.UNKNOWN) {
            return getCoreIcon();
        }
        if (EXTERNAL_INTERFACE_GEOM.equals(component.data)) {
            return getCoreIcon();
        }
        return ICONS.get(ICON_KEY);
    }

    @Override
    public IIcon getTextureForInputMode() {
        return ICONS.get(ICON_KEY_INPUT);
    }

    @Override
    public IIcon getTextureForOutputMode() {
        return ICONS.get(ICON_KEY_OUTPUT);
    }

    @Override
    public IIcon getTextureForInOutMode(boolean input) {
        return input ? ICONS.get(ICON_KEY_IN_OUT_IN) : ICONS.get(ICON_KEY_IN_OUT_OUT);
    }

    @Override
    public IIcon getTextureForInOutBackground() {
        return ICONS.get(ICON_KEY_IN_OUT_BG);
    }

    public IIcon getCoreIcon() {
        return ICONS.get(ICON_KEY_CORE);
    }

    @Override
    public void setExtractionRedstoneMode(RedstoneControlMode mode, ForgeDirection dir) {
        metadata.setExtractionRedstoneMode(dir, mode);
    }

    @Override
    public RedstoneControlMode getExtractionRedstoneMode(ForgeDirection dir) {
        return metadata.getExtractionRedstoneMode(dir);
    }

    @Override
    public void setExtractionSignalColor(ForgeDirection dir, DyeColor col) {
        metadata.setExtractionSignalColor(dir, col);
    }

    @Override
    public DyeColor getExtractionSignalColor(ForgeDirection dir) {
        return metadata.getExtractionSignalColor(dir);
    }

    @Override
    public boolean isExtractionRedstoneConditionMet(ForgeDirection dir) {
        RedstoneControlMode mode = getExtractionRedstoneMode(dir);
        if (mode == null) {
            return true;
        }
        return ConduitUtil.isRedstoneControlModeMet(getBundle(), mode, getExtractionSignalColor(dir));
    }

    @Override
    public int getOutputPriority(ForgeDirection dir) {
        return metadata.getOutputPriority(dir);
    }

    @Override
    public void setOutputPriority(ForgeDirection dir, int priority) {
        metadata.setOutputPriority(dir, priority);
    }

    @Override
    public DyeColor getInputColor(ForgeDirection dir) {
        return metadata.getInputColor(dir);
    }

    @Override
    public DyeColor getOutputColor(ForgeDirection dir) {
        return metadata.getOutputColor(dir);
    }

    @Override
    public void setInputColor(ForgeDirection dir, DyeColor col) {
        metadata.setInputColor(dir, col);
        setClientStateDirty();
        collidablesDirty = true;
    }

    @Override
    public void setOutputColor(ForgeDirection dir, DyeColor col) {
        metadata.setOutputColor(dir, col);
        setClientStateDirty();
        collidablesDirty = true;
    }

    @Override
    protected void readTypeSettings(ForgeDirection dir, NBTTagCompound dataRoot) {
        metadata.readTypeSettings(dir, dataRoot);
        setClientStateDirty();
        collidablesDirty = true;
    }

    @Override
    protected void writeTypeSettingsToNbt(ForgeDirection dir, NBTTagCompound dataRoot) {
        metadata.writeTypeSettings(dir, dataRoot);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbtRoot) {
        super.writeToNBT(nbtRoot);
        metadata.writeToNBT(nbtRoot);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbtRoot, short nbtVersion) {
        super.readFromNBT(nbtRoot, nbtVersion);
        metadata.readFromNBT(nbtRoot);
    }
}
