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

    public String getFullDescription() {
        StringBuilder builder = new StringBuilder()
                .append("Project: ").append(name).append(" (").append(type).append(')')
                .append("\n\n")
                .append("Address: ").append(address == null ? '\t' : address)
                .append("\tERF: ").append(erfNum == 0 ? '\t' : erfNum).append('\n')
                .append("Total Fee: ").append(totalFee).append("\tPaid to-date: ").append(totalPaid).append('\n')
                .append("Customer: ").append(customer.fullName())
                .append("\t\tProject Manager: ").append(projectManager == null ? '\t' : projectManager.fullName()).append('\n')
                .append("Architect: ").append(architect == null ? "\t\t" : architect.fullName())
                .append("\t\tEngineer: ").append(engineer == null ? '\t' : engineer.fullName()).append("\n\n");
        return builder.toString();

    }
}
