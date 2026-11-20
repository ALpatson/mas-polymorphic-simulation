package mas.model;

/** Singleton: there is exactly one Goblin Master. */
public final class MasterGoblin extends Master {

    private static MasterGoblin instance;

    private MasterGoblin() {
        super(Population.GOBLIN);
    }

    public static MasterGoblin getInstance() {
        if (instance == null) {
            instance = new MasterGoblin();
        }
        return instance;
    }

    @Override
    public Alliance getAlliance() {
        return Alliance.BAD;
    }
}
