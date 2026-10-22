package crazypants.enderio.conduit.essentia;

import static crazypants.enderio.conduit.item.ItemConduit.EXTERNAL_INTERFACE_GEOM;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

    // TODO: temporary item textures
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

    EssentiaConduitNetwork network;

    protected final EnumMap<ForgeDirection, RedstoneControlMode> extractionModes = new EnumMap<>(ForgeDirection.class);
    protected final EnumMap<ForgeDirection, DyeColor> extractionColors = new EnumMap<>(ForgeDirection.class);
    protected final EnumMap<ForgeDirection, Integer> priority = new EnumMap<>(ForgeDirection.class);
    protected final EnumMap<ForgeDirection, DyeColor> outputColors = new EnumMap<>(ForgeDirection.class);
    protected final EnumMap<ForgeDirection, DyeColor> inputColors = new EnumMap<>(ForgeDirection.class);

    private int metaData;

    public EssentiaConduit() {
        this(0);
    }

    public EssentiaConduit(int itemDamage) {
        metaData = itemDamage;
        updateFromNonUpgradeableVersion();
    }

    private void updateFromNonUpgradeableVersion() {
        int filterMeta = metaData;
    }

    @Override
    public ConduitRenderer getRenderer() {
        return EssentiaConduitRenderer.instance.get();
    }

    // region Neighbor logic
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
        int[] rates = Config.essentiaConduitRates;
        int tier = metaData;
        if (tier < 0 || tier > 3) return 1;
        return rates[tier];
    }

    @Override
    protected ConnectionMode getDefaultConnectionMode() {
        return ConnectionMode.INPUT;
    }

    @Override
    public Class<? extends IConduit> getBaseConduitType() {
        return IEssentiaConduit.class;
    }
    // endregion

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

    // region Settings
    @Override
    public void setExtractionRedstoneMode(RedstoneControlMode mode, ForgeDirection dir) {
        extractionModes.put(dir, mode);
    }

    @Override
    public RedstoneControlMode getExtractionRedstoneMode(ForgeDirection dir) {
        RedstoneControlMode res = extractionModes.get(dir);
        if (res == null) {
            res = RedstoneControlMode.ON;
        }
        return res;
    }

    @Override
    public void setExtractionSignalColor(ForgeDirection dir, DyeColor col) {
        extractionColors.put(dir, col);
    }

    @Override
    public DyeColor getExtractionSignalColor(ForgeDirection dir) {
        DyeColor result = extractionColors.get(dir);
        if (result == null) {
            return DyeColor.RED;
        }
        return result;
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
        Integer res = priority.get(dir);
        if (res == null) {
            return 0;
        }
        return res.intValue();
    }

    @Override
    public void setOutputPriority(ForgeDirection dir, int priority) {
        if (priority == 0) {
            this.priority.remove(dir);
        } else {
            this.priority.put(dir, priority);
        }
    }

    @Override
    public DyeColor getInputColor(ForgeDirection dir) {
        DyeColor result = inputColors.get(dir);
        if (result == null) {
            return DyeColor.GREEN;
        }
        return result;
    }

    @Override
    public DyeColor getOutputColor(ForgeDirection dir) {
        DyeColor result = outputColors.get(dir);
        if (result == null) {
            return DyeColor.GREEN;
        }
        return result;
    }

    @Override
    public void setInputColor(ForgeDirection dir, DyeColor col) {
        inputColors.put(dir, col);
        setClientStateDirty();
        collidablesDirty = true;
    }

    @Override
    public void setOutputColor(ForgeDirection dir, DyeColor col) {
        outputColors.put(dir, col);
        setClientStateDirty();
        collidablesDirty = true;
    }
    // endregion

    @Override
    public ItemStack createItem() {
        return new ItemStack(EnderIO.itemEssentiaConduit, 1, metaData);
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

    /*
     * @Override public IIcon getTextureForState(CollidableComponent component) { if (component.dir ==
     * ForgeDirection.UNKNOWN) { return ICONS.get(ICON_CORE_KEY); } ConnectionMode mode =
     * getConnectionMode(component.dir); if (mode == ConnectionMode.INPUT) { return ICONS.get(ICON_EXTRACT_KEY); } if
     * (mode == ConnectionMode.OUTPUT) { return ICONS.get(ICON_INSERT_KEY); } return ICONS.get(ICON_KEY); }
     */
    @Override
    public IIcon getTransmitionTextureForState(CollidableComponent component) {
        return null;
    }

    // TODO: temporary
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

    // region NBT Data
    @Override
    protected void readTypeSettings(ForgeDirection dir, NBTTagCompound dataRoot) {
        setExtractionSignalColor(dir, DyeColor.VALUES[dataRoot.getShort("extractionSignalColor")]);
        setExtractionRedstoneMode(RedstoneControlMode.VALUES[dataRoot.getShort("extractionRedstoneMode")], dir);
        setInputColor(dir, DyeColor.VALUES[dataRoot.getShort("inputColor")]);
        setOutputColor(dir, DyeColor.VALUES[dataRoot.getShort("outputColor")]);
        setOutputPriority(dir, dataRoot.getInteger("outputPriority"));
    }

    @Override
    protected void writeTypeSettingsToNbt(ForgeDirection dir, NBTTagCompound dataRoot) {
        dataRoot.setShort("extractionSignalColor", (short) getExtractionSignalColor(dir).ordinal());
        dataRoot.setShort("extractionRedstoneMode", (short) getExtractionRedstoneMode(dir).ordinal());
        dataRoot.setShort("inputColor", (short) getInputColor(dir).ordinal());
        dataRoot.setShort("outputColor", (short) getOutputColor(dir).ordinal());
        dataRoot.setInteger("outputPriority", getOutputPriority(dir));
    }

    @Override
    public void writeToNBT(NBTTagCompound nbtRoot) {
        super.writeToNBT(nbtRoot);

        for (Entry<ForgeDirection, RedstoneControlMode> entry : extractionModes.entrySet()) {
            if (entry.getValue() != null) {
                nbtRoot.setShort("extRM." + entry.getKey().name(), (short) entry.getValue().ordinal());
            }
        }

        for (Entry<ForgeDirection, DyeColor> entry : extractionColors.entrySet()) {
            if (entry.getValue() != null) {
                nbtRoot.setShort("extSC." + entry.getKey().name(), (short) entry.getValue().ordinal());
            }
        }

        for (Entry<ForgeDirection, Integer> entry : priority.entrySet()) {
            if (entry.getValue() != null) {
                nbtRoot.setInteger("priority." + entry.getKey().name(), entry.getValue());
            }
        }

        for (Entry<ForgeDirection, DyeColor> entry : inputColors.entrySet()) {
            if (entry.getValue() != null) {
                nbtRoot.setShort("inSC." + entry.getKey().name(), (short) entry.getValue().ordinal());
            }
        }

        for (Entry<ForgeDirection, DyeColor> entry : outputColors.entrySet()) {
            if (entry.getValue() != null) {
                nbtRoot.setShort("outSC." + entry.getKey().name(), (short) entry.getValue().ordinal());
            }
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbtRoot, short nbtVersion) {
        super.readFromNBT(nbtRoot, nbtVersion);

        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            String key = "extRM." + dir.name();
            if (nbtRoot.hasKey(key)) {
                short ord = nbtRoot.getShort(key);
                if (ord >= 0 && ord < RedstoneControlMode.VALUES.length) {
                    extractionModes.put(dir, RedstoneControlMode.VALUES[ord]);
                }
            }

            key = "extSC." + dir.name();
            if (nbtRoot.hasKey(key)) {
                short ord = nbtRoot.getShort(key);
                if (ord >= 0 && ord < DyeColor.VALUES.length) {
                    extractionColors.put(dir, DyeColor.VALUES[ord]);
                }
            }

            key = "priority." + dir.name();
            if (nbtRoot.hasKey(key)) {
                priority.put(dir, nbtRoot.getInteger(key));
            }

            key = "inSC." + dir.name();
            if (nbtRoot.hasKey(key)) {
                short ord = nbtRoot.getShort(key);
                if (ord >= 0 && ord < DyeColor.VALUES.length) {
                    inputColors.put(dir, DyeColor.VALUES[ord]);
                }
            }

            key = "outSC." + dir.name();
            if (nbtRoot.hasKey(key)) {
                short ord = nbtRoot.getShort(key);
                if (ord >= 0 && ord < DyeColor.VALUES.length) {
                    outputColors.put(dir, DyeColor.VALUES[ord]);
                }
            }
        }
    }
    // endregion
}
