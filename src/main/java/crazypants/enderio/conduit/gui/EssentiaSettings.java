
package crazypants.enderio.conduit.gui;

import java.awt.Color;

import javax.annotation.Nonnull;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;

import org.lwjgl.opengl.GL11;

import com.enderio.core.client.gui.button.ColorButton;
import com.enderio.core.client.gui.button.MultiIconButton;
import com.enderio.core.client.render.ColorUtil;
import com.enderio.core.client.render.EnderWidget;
import com.enderio.core.common.util.DyeColor;

import crazypants.enderio.EnderIO;
import crazypants.enderio.conduit.ConnectionMode;
import crazypants.enderio.conduit.IConduit;
import crazypants.enderio.conduit.essentia.IEssentiaConduit;
import crazypants.enderio.conduit.packet.PacketEssentiaConduit;
import crazypants.enderio.conduit.packet.PacketExtractMode;
import crazypants.enderio.gui.IconEIO;
import crazypants.enderio.gui.RedstoneModeButton;
import crazypants.enderio.machine.IRedstoneModeControlable;
import crazypants.enderio.machine.RedstoneControlMode;
import crazypants.enderio.network.PacketHandler;
import crazypants.util.ColorUtils;

public class EssentiaSettings extends BaseSettingsPanel {

    private static final int ID_REDSTONE_BUTTON = 12614;
    private static final int ID_COLOR_BUTTON = 179816;
    private static final int ID_PRIORITY_UP = 25;
    private static final int ID_PRIORITY_DOWN = 26;
    private static final int ID_INSERT_CHANNEL = 23;
    private static final int ID_EXTRACT_CHANNEL = 27;

    private final @Nonnull IEssentiaConduit essentiaConduit;

    private final MultiIconButton priUpB;
    private final MultiIconButton priDownB;

    private final RedstoneModeButton rsB;
    private final @Nonnull ColorButton colorB;

    private final ColorButton insertChannelB;
    private final ColorButton extractChannelB;

    private static final int priLeft = 46;
    private static final int priWidth = 32;

    public EssentiaSettings(@Nonnull final GuiExternalConnection gui, @Nonnull IConduit con) {
        super(
                IconEIO.WRENCH_OVERLAY_ITEM,
                EnderIO.lang.localize("itemEssentiaConduit.name"),
                gui,
                con,
                "filter_upgrade_settings");
        essentiaConduit = (IEssentiaConduit) con;
        this.textureHeight += 48;

        int x = leftColumn;
        int y = customTop;

        insertChannelB = new ColorButton(gui, ID_INSERT_CHANNEL, x, y);
        insertChannelB.setColorIndex(0);
        insertChannelB.setToolTipHeading(EnderIO.lang.localize("gui.conduit.item.channel"));

        x = rightColumn;
        extractChannelB = new ColorButton(gui, ID_EXTRACT_CHANNEL, x, y);
        extractChannelB.setColorIndex(0);
        extractChannelB.setToolTipHeading(EnderIO.lang.localize("gui.conduit.item.channel"));

        y += insertChannelB.getHeight() + 6;
        x = rightColumn;

        int x0 = x + 20;
        colorB = new ColorButton(gui, ID_COLOR_BUTTON, x0, y);
        colorB.setColorIndex(essentiaConduit.getExtractionSignalColor(gui.getDir()).ordinal());
        colorB.setToolTipHeading(EnderIO.lang.localize("gui.conduit.item.channel"));

        rsB = new RedstoneModeButton(gui, ID_REDSTONE_BUTTON, x, y, new IRedstoneModeControlable() {

            @Override
            public void setRedstoneControlMode(RedstoneControlMode mode) {
                RedstoneControlMode curMode = getRedstoneControlMode();
                essentiaConduit.setExtractionRedstoneMode(mode, gui.getDir());
                if (curMode != mode) {
                    PacketHandler.INSTANCE.sendToServer(new PacketExtractMode(essentiaConduit, gui.getDir()));
                }
            }

            @Override
            public RedstoneControlMode getRedstoneControlMode() {
                return essentiaConduit.getExtractionRedstoneMode(gui.getDir());
            }
        });

        x = priLeft + priWidth + 9;
        priUpB = MultiIconButton.createAddButton(gui, ID_PRIORITY_UP, x, y);
        priDownB = MultiIconButton.createMinusButton(gui, ID_PRIORITY_DOWN, x, y + 8);
    }

    @Override
    protected void initCustomOptions() {
        updateGuiVisibility();
    }

    private void updateGuiVisibility() {
        updateButtons();
    }

    private void updateButtons() {
        rsB.onGuiInit();
        rsB.setMode(essentiaConduit.getExtractionRedstoneMode(gui.getDir()));
        colorB.onGuiInit();

        priUpB.onGuiInit();
        priDownB.onGuiInit();

        insertChannelB.onGuiInit();
        insertChannelB.setColorIndex(essentiaConduit.getOutputColor(gui.getDir()).ordinal());
        extractChannelB.onGuiInit();
        extractChannelB.setColorIndex(essentiaConduit.getInputColor(gui.getDir()).ordinal());
    }

    @Override
    public void actionPerformed(@Nonnull GuiButton guiButton) {
        super.actionPerformed(guiButton);
        if (guiButton.id == ID_COLOR_BUTTON) {
            essentiaConduit.setExtractionSignalColor(gui.getDir(), DyeColor.fromIndex(colorB.getColorIndex()));
            PacketHandler.INSTANCE.sendToServer(new PacketExtractMode(essentiaConduit, gui.getDir()));
            return;
        } else if (guiButton.id == ID_PRIORITY_UP) {
            essentiaConduit.setOutputPriority(gui.getDir(), essentiaConduit.getOutputPriority(gui.getDir()) + 1);
        } else if (guiButton.id == ID_PRIORITY_DOWN) {
            essentiaConduit.setOutputPriority(gui.getDir(), essentiaConduit.getOutputPriority(gui.getDir()) - 1);
        } else if (guiButton.id == ID_INSERT_CHANNEL) {
            DyeColor col = DyeColor.fromIndex(insertChannelB.getColorIndex());
            essentiaConduit.setOutputColor(gui.getDir(), col);
        } else if (guiButton.id == ID_EXTRACT_CHANNEL) {
            DyeColor col = DyeColor.fromIndex(extractChannelB.getColorIndex());
            essentiaConduit.setInputColor(gui.getDir(), col);
        } else {
            return;
        }
        PacketHandler.INSTANCE.sendToServer(new PacketEssentiaConduit(essentiaConduit, gui.getDir()));
    }

    @Override
    protected void connectionModeChanged(@Nonnull ConnectionMode mode) {
        super.connectionModeChanged(mode);
        PacketHandler.INSTANCE.sendToServer(new PacketExtractMode(essentiaConduit, gui.getDir()));
        updateGuiVisibility();
    }

    @Override
    protected void renderCustomOptions(int top1, float par1, int par2, int par3) {
        FontRenderer fr = gui.getFontRenderer();

        GL11.glColor3f(1, 1, 1);
        IconEIO.map.render(EnderWidget.BUTTON_DOWN, left + priLeft, top1 - 5, priWidth, 16, 0, true);
        String str = essentiaConduit.getOutputPriority(gui.getDir()) + "";
        int sw = fr.getStringWidth(str);

        String priority = EnderIO.lang.localize("gui.conduit.item.priority");
        fr.drawString(priority, left + 12, top1 + 25, ColorUtil.getRGB(Color.black));
        fr.drawString(str, left + priLeft + priWidth - sw - gap, top1 + 25, ColorUtils.priorityString.getColor());
    }

    @Override
    public void mouseClicked(int x, int y, int par3) {
        super.mouseClicked(x, y, par3);
    }

    @Override
    public void deactivate() {
        super.deactivate();
        rsB.detach();
        colorB.detach();
        priUpB.detach();
        priDownB.detach();
        insertChannelB.detach();
        extractChannelB.detach();
    }
}
