package crazypants.enderio.conduit.essentia;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import crazypants.enderio.ModObject;
import crazypants.enderio.conduit.AbstractItemConduit;
import crazypants.enderio.conduit.IConduit;
import crazypants.enderio.conduit.ItemConduitSubtype;

public class ItemEssentiaConduit extends AbstractItemConduit {

    private static final ItemConduitSubtype[] subtypes = new ItemConduitSubtype[] {
            new ItemConduitSubtype(ModObject.itemEssentiaConduit.name(), "enderio:itemEssentiaConduit"),
            new ItemConduitSubtype(ModObject.itemEssentiaConduit.name() + "Thaumium", "enderio:itemEssentiaConduit"),
            new ItemConduitSubtype(ModObject.itemEssentiaConduit.name() + "Void", "enderio:itemEssentiaConduit"),
            new ItemConduitSubtype(ModObject.itemEssentiaConduit.name() + "Ichorium", "enderio:itemEssentiaConduit"), };

    public static ItemEssentiaConduit create() {
        ItemEssentiaConduit result = new ItemEssentiaConduit();
        result.init();
        return result;
    }

    protected ItemEssentiaConduit() {
        super(ModObject.itemEssentiaConduit, subtypes);
    }

    @Override
    public Class<? extends IConduit> getBaseConduitType() {
        return IEssentiaConduit.class;
    }

    @Override
    public IConduit createConduit(ItemStack item, EntityPlayer player) {
        return new EssentiaConduit();
    }

    @Override
    public boolean shouldHideFacades(ItemStack stack, EntityPlayer player) {
        return true;
    }
}
