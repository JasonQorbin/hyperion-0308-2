public enum ProjectType {
    HOUSE(1),
    APARTMENT(2),
    SHOP(3),
    WAREHOUSE(4),
    OFFICE_BUILDING(5),
    HOTEL(6),
    LARGE_RETAIL(7);

    private final long id;

    private ProjectType(long id) {
        this.id = id;
    }

    public long id() {
        return id;
    }

    @Override
    public String toString() {
        return switch(this) {
            case HOUSE -> "House";
            case APARTMENT -> "Apartment";
            case SHOP -> "Shop";
            case WAREHOUSE -> "Warehouse";
            case OFFICE_BUILDING -> "Office Building";
            case HOTEL -> "Hotel";
            case LARGE_RETAIL -> "Large Retail";
        };
    }
}
