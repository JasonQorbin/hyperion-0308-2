public enum ProjectStatus {
    CAPTURED(1),
    LOGGED(2),
    CONCEPT(3),
    PREFEAS(4),
    BANKABLE(5),
    CONSTRUCTION(6),
    FINAL(7);

    private int id;

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

}
