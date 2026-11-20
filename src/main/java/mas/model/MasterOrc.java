package mas.model;

/** Singleton: there is exactly one Orc Master. */
public final class MasterOrc extends Master {

    private static MasterOrc instance;

    private MasterOrc() {
        super(Population.ORC);
    }

    public static MasterOrc getInstance() {
        if (instance == null) {
            instance = new MasterOrc();
        }
        return instance;
    }

    @Override
    public Alliance getAlliance() {
        return Alliance.BAD;
    }
}
