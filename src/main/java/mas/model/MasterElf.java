package mas.model;

/** Singleton: there is exactly one Elf Master. */
public final class MasterElf extends Master {

    private static MasterElf instance;

    private MasterElf() {
        super(Population.ELF);
    }

    public static MasterElf getInstance() {
        if (instance == null) {
            instance = new MasterElf();
        }
        return instance;
    }

    @Override
    public Alliance getAlliance() {
        return Alliance.GOOD;
    }
}
