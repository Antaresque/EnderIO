package crazypants.enderio.conduit.packet;

import net.minecraftforge.common.util.ForgeDirection;

import com.enderio.core.common.util.DyeColor;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import crazypants.enderio.conduit.essentia.IEssentiaConduit;
import crazypants.enderio.conduit.item.IItemConduit;
import crazypants.enderio.conduit.item.filter.IItemFilter;
import crazypants.util.ForgeDirections;
import io.netty.buffer.ByteBuf;

public class PacketEssentiaConduit extends AbstractConduitPacket<IEssentiaConduit>
        implements IMessageHandler<PacketEssentiaConduit, IMessage> {

    private ForgeDirection dir;
    private DyeColor colIn;
    private DyeColor colOut;
    private int priority;

    // private IItemFilter inputFilter;
    // private IItemFilter outputFilter;

    public PacketEssentiaConduit() {}

    public PacketEssentiaConduit(IEssentiaConduit con, ForgeDirection dir) {
        super(con.getBundle().getEntity(), ConTypeEnum.ESSENTIA);
        this.dir = dir;
        colIn = con.getInputColor(dir);
        colOut = con.getOutputColor(dir);
        priority = con.getOutputPriority(dir);

        // inputFilter = con.getInputFilter(dir);
        // outputFilter = con.getOutputFilter(dir);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        super.toBytes(buf);
        buf.writeShort(dir.ordinal());
        buf.writeInt(priority);
        buf.writeShort(colIn.ordinal());
        buf.writeShort(colOut.ordinal());
        // FilterRegister.writeFilter(buf, inputFilter);
        // FilterRegister.writeFilter(buf, outputFilter);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        super.fromBytes(buf);
        dir = ForgeDirections.DIRECTIONS[buf.readShort()];
        priority = buf.readInt();
        colIn = DyeColor.VALUES[buf.readShort()];
        colOut = DyeColor.VALUES[buf.readShort()];
        // inputFilter = FilterRegister.readFilter(buf);
        // outputFilter = FilterRegister.readFilter(buf);
    }

    @Override
    public IMessage onMessage(PacketEssentiaConduit message, MessageContext ctx) {
        if (isInvalidPacketForGui(message, ctx)) return null;
        IEssentiaConduit conduit = message.getTileCasted(ctx);
        conduit.setInputColor(message.dir, message.colIn);
        conduit.setOutputColor(message.dir, message.colOut);
        conduit.setOutputPriority(message.dir, message.priority);
        // applyFilter(message.dir, conduit, message.inputFilter, true);
        // applyFilter(message.dir, conduit, message.outputFilter, false);

        message.getWorld(ctx).markBlockForUpdate(message.x, message.y, message.z);
        return null;
    }

    private void applyFilter(ForgeDirection dir, IItemConduit conduit, IItemFilter filter, boolean isInput) {
        if (isInput) {
            conduit.setInputFilter(dir, filter);
        } else {
            conduit.setOutputFilter(dir, filter);
        }
        return;
    }
}
