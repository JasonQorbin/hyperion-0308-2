package MainProgram;

public enum ProjectStatus {
    CAPTURED(1),
    LOGGED(2),
    CONCEPT(3),
    PREFEAS(4),
    BANKABLE(5),
    CONSTRUCTION(6),
    FINAL(7);

    public long id() {
        return id;
    }

    private long id;

    private ProjectStatus(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return switch (this){
            case CAPTURED -> "Captured";
            case LOGGED -> "Logged";
            case CONCEPT -> "Concept and Viability";
            case PREFEAS -> "Pre-feasibility";
            case BANKABLE -> "Bankable feasibility";
            case CONSTRUCTION -> "Construction";
            case FINAL -> "Finalisation";
        };
    }

    public static ProjectStatus get(int id) {
        return switch(id) {
            case 1 -> CAPTURED;
            case 2 -> LOGGED;
            case 3 -> CONCEPT;
            case 4 -> PREFEAS;
            case 5 -> BANKABLE;
            case 6 -> CONSTRUCTION;
            case 7 -> FINAL;
            default -> null;
        };
    }
}
