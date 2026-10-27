package crazypants.enderio.conduit.essentia;

public enum EssentiaConduitTier {

    BASIC,
    VOID,
    ICHOR;

    private static EssentiaConduitTier[] cachedValues = null;

    public static EssentiaConduitTier fromOrdinal(short i) {
        if (EssentiaConduitTier.cachedValues == null) {
            EssentiaConduitTier.cachedValues = EssentiaConduitTier.values();
        }
        return EssentiaConduitTier.cachedValues[i];
    }
}
