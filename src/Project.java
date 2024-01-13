import java.math.BigDecimal;
import java.time.LocalDate;

public class Project {
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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(name).append(" | PM: ").append(projectManager.fullName()).append(" | Project size: ").append(totalFee);
        return builder.toString();
    }
}
