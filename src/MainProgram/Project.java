package MainProgram;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Project implements Pickable {
    public long number;
    public String name;
    public String address;
    public int erfNum;
    public BigDecimal totalFee;
    public BigDecimal totalPaid;
    public LocalDate deadline;

    public Person engineer;
    public Person customer;
    public Person architect;
    public Person projectManager;

    public ProjectStatus status;
    public ProjectType type;

    public Project (String name, ProjectType type, Person customer) {
        this.name = name;
        this.type = type;
        this.customer = customer;
        this.status = ProjectStatus.CAPTURED;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(name).append(" | Customer: ").append(customer.fullName()).append(" | Project size: ").append(totalFee);
        return builder.toString();
    }

    @Override
    public String getOneLineString() {
        return toString();
    }
}
