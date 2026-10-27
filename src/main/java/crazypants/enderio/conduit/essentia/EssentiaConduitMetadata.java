package crazypants.enderio.conduit.essentia;

import java.util.EnumMap;
import java.util.Map.Entry;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import com.enderio.core.common.util.DyeColor;

import crazypants.enderio.machine.RedstoneControlMode;

public class EssentiaConduitMetadata {

    private static final String NBT_TIER = "metaData";
    private static final String NBT_EXTRACTION_MODE = "extRM.";
    private static final String NBT_EXTRACTION_COLOR = "extSC.";
    private static final String NBT_PRIORITY = "priority.";
    private static final String NBT_INPUT_COLOR = "inSC.";
    private static final String NBT_OUTPUT_COLOR = "outSC.";

    private static final String TYPE_EXTRACTION_COLOR = "extractionSignalColor";
    private static final String TYPE_EXTRACTION_MODE = "extractionRedstoneMode";
    private static final String TYPE_INPUT_COLOR = "inputColor";
    private static final String TYPE_OUTPUT_COLOR = "outputColor";
    private static final String TYPE_PRIORITY = "outputPriority";

    private EssentiaConduitTier tier;

    private final EnumMap<ForgeDirection, RedstoneControlMode> extractionModes = new EnumMap<>(ForgeDirection.class);
    private final EnumMap<ForgeDirection, DyeColor> extractionColors = new EnumMap<>(ForgeDirection.class);
    private final EnumMap<ForgeDirection, Integer> priority = new EnumMap<>(ForgeDirection.class);
    private final EnumMap<ForgeDirection, DyeColor> outputColors = new EnumMap<>(ForgeDirection.class);
    private final EnumMap<ForgeDirection, DyeColor> inputColors = new EnumMap<>(ForgeDirection.class);

    public EssentiaConduitMetadata(EssentiaConduitTier tier) {
        this.tier = tier;
    }

    public EssentiaConduitTier getTier() {
        return tier;
    }

    public int getOrdinalTier() {
        return tier.ordinal();
    }

    public RedstoneControlMode getExtractionRedstoneMode(ForgeDirection dir) {
        RedstoneControlMode res = extractionModes.get(dir);
        if (res == null) {
            res = RedstoneControlMode.ON;
        }
        return res;
    }

    public void setExtractionRedstoneMode(ForgeDirection dir, RedstoneControlMode mode) {
        extractionModes.put(dir, mode);
    }

    public DyeColor getExtractionSignalColor(ForgeDirection dir) {
        DyeColor result = extractionColors.get(dir);
        if (result == null) {
            return DyeColor.RED;
        }
        return result;
    }

    public void setExtractionSignalColor(ForgeDirection dir, DyeColor col) {
        extractionColors.put(dir, col);
    }

    public int getOutputPriority(ForgeDirection dir) {
        Integer res = priority.get(dir);
        if (res == null) {
            return 0;
        }
        return res;
    }

    public void setOutputPriority(ForgeDirection dir, int newPriority) {
        if (newPriority == 0) {
            priority.remove(dir);
        } else {
            priority.put(dir, newPriority);
        }
    }

    public DyeColor getInputColor(ForgeDirection dir) {
        DyeColor result = inputColors.get(dir);
        if (result == null) {
            return DyeColor.GREEN;
        }
        return result;
    }

    public void setInputColor(ForgeDirection dir, DyeColor col) {
        inputColors.put(dir, col);
    }

    public DyeColor getOutputColor(ForgeDirection dir) {
        DyeColor result = outputColors.get(dir);
        if (result == null) {
            return DyeColor.GREEN;
        }
        return result;
    }

    public void setOutputColor(ForgeDirection dir, DyeColor col) {
        outputColors.put(dir, col);
    }

    public void writeTypeSettings(ForgeDirection dir, NBTTagCompound dataRoot) {
        dataRoot.setShort(TYPE_EXTRACTION_COLOR, (short) getExtractionSignalColor(dir).ordinal());
        dataRoot.setShort(TYPE_EXTRACTION_MODE, (short) getExtractionRedstoneMode(dir).ordinal());
        dataRoot.setShort(TYPE_INPUT_COLOR, (short) getInputColor(dir).ordinal());
        dataRoot.setShort(TYPE_OUTPUT_COLOR, (short) getOutputColor(dir).ordinal());
        dataRoot.setInteger(TYPE_PRIORITY, getOutputPriority(dir));
    }

    public void readTypeSettings(ForgeDirection dir, NBTTagCompound dataRoot) {
        setExtractionSignalColor(dir, DyeColor.VALUES[dataRoot.getShort(TYPE_EXTRACTION_COLOR)]);
        setExtractionRedstoneMode(dir, RedstoneControlMode.VALUES[dataRoot.getShort(TYPE_EXTRACTION_MODE)]);
        setInputColor(dir, DyeColor.VALUES[dataRoot.getShort(TYPE_INPUT_COLOR)]);
        setOutputColor(dir, DyeColor.VALUES[dataRoot.getShort(TYPE_OUTPUT_COLOR)]);
        setOutputPriority(dir, dataRoot.getInteger(TYPE_PRIORITY));
    }

    public void writeToNBT(NBTTagCompound nbtRoot) {
        nbtRoot.setShort(NBT_TIER, (short) tier.ordinal());

        for (Entry<ForgeDirection, RedstoneControlMode> entry : extractionModes.entrySet()) {
            if (entry.getValue() != null) {
                nbtRoot.setShort(NBT_EXTRACTION_MODE + entry.getKey().name(), (short) entry.getValue().ordinal());
            }
        }

        for (Entry<ForgeDirection, Integer> entry : priority.entrySet()) {
            if (entry.getValue() != null) {
                nbtRoot.setInteger(NBT_PRIORITY + entry.getKey().name(), entry.getValue());
            }
        }

        writeColors(nbtRoot, NBT_EXTRACTION_COLOR, extractionColors);
        writeColors(nbtRoot, NBT_INPUT_COLOR, inputColors);
        writeColors(nbtRoot, NBT_OUTPUT_COLOR, outputColors);
    }

    public void readFromNBT(NBTTagCompound nbtRoot) {
        if (nbtRoot.hasKey(NBT_TIER)) {
            short num = nbtRoot.getShort(NBT_TIER);
            tier = EssentiaConduitTier.fromOrdinal(num);
        }

        String key;
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            key = NBT_EXTRACTION_MODE + dir.name();
            if (nbtRoot.hasKey(key)) {
                short ord = nbtRoot.getShort(key);
                if (ord >= 0 && ord < RedstoneControlMode.VALUES.length) {
                    extractionModes.put(dir, RedstoneControlMode.VALUES[ord]);
                }
            }
            key = NBT_PRIORITY + dir.name();
            if (nbtRoot.hasKey(key)) {
                priority.put(dir, nbtRoot.getInteger(key));
            }

            readColor(nbtRoot, NBT_EXTRACTION_COLOR + dir.name(), extractionColors, dir);
            readColor(nbtRoot, NBT_INPUT_COLOR + dir.name(), inputColors, dir);
            readColor(nbtRoot, NBT_OUTPUT_COLOR + dir.name(), outputColors, dir);
        }
    }

    private static void writeColors(NBTTagCompound nbtRoot, String prefix, EnumMap<ForgeDirection, DyeColor> colors) {
        for (Entry<ForgeDirection, DyeColor> entry : colors.entrySet()) {
            if (entry.getValue() != null) {
                nbtRoot.setShort(prefix + entry.getKey().name(), (short) entry.getValue().ordinal());
            }
        }
    }

    private static void readColor(NBTTagCompound nbtRoot, String key, EnumMap<ForgeDirection, DyeColor> target,
            ForgeDirection dir) {
        if (nbtRoot.hasKey(key)) {
            short ord = nbtRoot.getShort(key);
            if (ord >= 0 && ord < DyeColor.VALUES.length) {
                target.put(dir, DyeColor.VALUES[ord]);
            }
        }
    }
}
