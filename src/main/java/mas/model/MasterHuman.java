package mas.model;

/** Singleton: there is exactly one Human Master. */
public final class MasterHuman extends Master {

    private static MasterHuman instance;

    private MasterHuman() {
        super(Population.HUMAN);
    }

    public static MasterHuman getInstance() {
        if (instance == null) {
            instance = new MasterHuman();
        }
        return instance;
    }

    @Override
    public Alliance getAlliance() {
        return Alliance.GOOD;
    }
}
