package MainProgram;

import java.util.ArrayList;
import java.util.List;

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

    public static ProjectType get(int id) {
        return switch(id) {
            case 1 -> HOUSE;
            case 2 -> APARTMENT;
            case 3 -> SHOP;
            case 4 -> WAREHOUSE;
            case 5 -> OFFICE_BUILDING;
            case 6 -> HOTEL;
            case 7 -> LARGE_RETAIL;
            default -> null;
        };
    }

    public static List<ProjectType> getList() {
        ArrayList<ProjectType> answer = new ArrayList<>();
        answer.add(HOUSE);
        answer.add(APARTMENT);
        answer.add(SHOP);
        answer.add(WAREHOUSE);
        answer.add(OFFICE_BUILDING);
        answer.add(HOTEL);
        answer.add(LARGE_RETAIL);
        return answer;
    }
}
