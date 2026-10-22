package crazypants.enderio.conduit.essentia;

import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;

import com.enderio.core.common.util.DyeColor;

import crazypants.enderio.conduit.IConduit;
import crazypants.enderio.conduit.IExtractor;
import thaumcraft.api.aspects.IAspectContainer;

public interface IEssentiaConduit extends IConduit, IExtractor {

    IAspectContainer getExternalContainer(ForgeDirection dir);

    boolean canExtractFromDir(ForgeDirection dir);

    boolean canInsertToDir(ForgeDirection dir);

    boolean isExtractionRedstoneConditionMet(ForgeDirection dir);

    int getMaximumExtracted(ForgeDirection dir);

    int getOutputPriority(ForgeDirection dir);

    void setOutputPriority(ForgeDirection dir, int priority);

    DyeColor getInputColor(ForgeDirection dir);

    DyeColor getOutputColor(ForgeDirection dir);

    void setInputColor(ForgeDirection dir, DyeColor col);

    void setOutputColor(ForgeDirection dir, DyeColor col);

    // temporary
    IIcon getTextureForInputMode();

    IIcon getTextureForOutputMode();

    IIcon getTextureForInOutMode(boolean input);

    IIcon getTextureForInOutBackground();

}
