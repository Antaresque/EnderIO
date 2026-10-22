package crazypants.enderio.conduit.essentia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraftforge.common.util.ForgeDirection;

import crazypants.enderio.conduit.AbstractConduitNetwork;
import crazypants.enderio.config.Config;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;

public class EssentiaConduitNetwork extends AbstractConduitNetwork<IEssentiaConduit, EssentiaConduit> {

    private int delay;

    public EssentiaConduitNetwork() {
        super(EssentiaConduit.class, IEssentiaConduit.class);
    }

    private static final class EssentiaConnection {

        final EssentiaConduit conduit;
        final ForgeDirection dir;
        final IAspectContainer container;
        final int priority;

        EssentiaConnection(EssentiaConduit conduit, ForgeDirection dir, IAspectContainer container) {
            this.conduit = conduit;
            this.dir = dir;
            this.container = container;
            this.priority = conduit.getOutputPriority(dir);
        }
    }

    @Override
    public void doNetworkTick() {
        if (delay > 0) {
            delay--;
            return;
        }
        delay = Math.max(1, Config.essentiaConduitTransferInterval) - 1; // a cycle every <interval> ticks

        List<EssentiaConnection> sources = new ArrayList<>();
        List<EssentiaConnection> targets = new ArrayList<>();
        for (EssentiaConduit con : conduits) {
            for (ForgeDirection dir : con.getExternalConnections()) {
                boolean extract = con.canExtractFromDir(dir) && con.isExtractionRedstoneConditionMet(dir);
                boolean insert = con.canInsertToDir(dir);
                if (!extract && !insert) {
                    continue;
                }
                IAspectContainer container = con.getExternalContainer(dir);
                if (container == null) {
                    continue;
                }
                EssentiaConnection c = new EssentiaConnection(con, dir, container);
                if (extract) {
                    sources.add(c);
                }
                if (insert) {
                    targets.add(c);
                }
            }
        }
        if (sources.isEmpty() || targets.isEmpty()) {
            return;
        }
        Collections.sort(targets, (a, b) -> Integer.compare(b.priority, a.priority));

        for (EssentiaConnection src : sources) {
            AspectList list = src.container.getAspects();
            Aspect[] aspects = list == null ? null : list.getAspects();
            if (aspects == null) {
                continue;
            }
            int budget = src.conduit.getMaximumExtracted(src.dir);
            for (Aspect aspect : aspects) {
                if (budget <= 0) {
                    break;
                }
                if (aspect != null) {
                    budget -= route(src, targets, aspect, budget);
                }
            }
        }
    }

    private int route(EssentiaConnection src, List<EssentiaConnection> targets, Aspect aspect, int wanted) {
        int moved = 0;
        for (EssentiaConnection tgt : targets) {
            if (moved >= wanted) {
                break;
            }
            // never feed the side we extract from, or the same block reached through another side
            if (tgt.container == src.container) {
                continue;
            }
            if (src.conduit.getInputColor(src.dir) != tgt.conduit.getOutputColor(tgt.dir)) {
                continue;
            }
            if (!tgt.container.doesContainerAccept(aspect)) {
                continue;
            }
            moved += transfer(src.container, tgt.container, aspect, wanted - moved);
        }
        return moved;
    }

    /**
     * Insert first, then remove from the source; roll back if the source no longer has it, so nothing is lost or
     * duplicated when a container rejects part of the amount.
     */
    private static int transfer(IAspectContainer from, IAspectContainer to, Aspect aspect, int amount) {
        int toMove = Math.min(amount, from.containerContains(aspect));
        if (toMove <= 0) {
            return 0;
        }
        // addToContainer returns the amount that did NOT fit
        int accepted = toMove - to.addToContainer(aspect, toMove);
        if (accepted <= 0) {
            return 0;
        }
        if (!from.takeFromContainer(aspect, accepted)) {
            to.takeFromContainer(aspect, accepted);
            return 0;
        }
        return accepted;
    }

}
