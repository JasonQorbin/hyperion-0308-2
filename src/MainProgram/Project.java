package MainProgram;

import database.DataSource;
import database.DatabaseException;

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

    private boolean canBeLogged() {
        return status == ProjectStatus.CAPTURED && address != null && !address.isBlank() && erfNum > 0;
    }

    private boolean canGoToConcept() {
        return status == ProjectStatus.LOGGED && architect != null;
    }

    private boolean canGoToPreFeas() {
        return status == ProjectStatus.CONCEPT && engineer != null;
    }

    private boolean canGoToBankable() {
        return status == ProjectStatus.PREFEAS && projectManager != null;
    }

    private boolean canGoToConstruction() {
        final BigDecimal ZERO = BigDecimal.valueOf(0);
        return status == ProjectStatus.BANKABLE && totalFee.compareTo(ZERO) > 0;
    }

    private boolean canBeFinalised() {
        final BigDecimal ZERO = BigDecimal.valueOf(0);
        return status == ProjectStatus.CONSTRUCTION && totalPaid.compareTo(ZERO) > 0;
    }

    /**
     * Checks if the current project is eligible to advance to the next stage and does to so if it is able.
     *
     * @throws DatabaseException If a database error occurs.
     */
    public void advanceStage() throws DatabaseException {
        DataSource dataSource = DataSource.getInstance();
        switch (status){
            case CAPTURED:
                if (canBeLogged()) {
                    if(dataSource.changeStage(number, status.id() + 1 )) {
                        status = ProjectStatus.LOGGED;
                        System.out.println("The project is now in " + status + " stage.\n");
                    }
                } else {
                    System.out.println("To advance the project please capture the address and ERF number of the property");
                }
                break;
            case LOGGED:
                if (canGoToConcept()) {
                    if(dataSource.changeStage(number, status.id() + 1 )) {
                        status = ProjectStatus.CONCEPT;
                        System.out.println("The project is now in " + status + " stage.\n");
                    }
                } else {
                    System.out.println("To advance the project please assign an architect");
                }
                break;
            case CONCEPT:
                if (canGoToPreFeas()) {
                    if(dataSource.changeStage(number, status.id() + 1 )) {
                        status = ProjectStatus.PREFEAS;
                        System.out.println("The project is now in " + status + " stage.\n");
                    }
                } else {
                    System.out.println("To advance the project please assign an engineer");
                }
                break;
            case PREFEAS:
                if (canGoToBankable()) {
                    if(dataSource.changeStage(number, status.id() + 1 )) {
                        status = ProjectStatus.BANKABLE;
                        System.out.println("The project is now in " + status + " stage.\n");
                    }
                } else {
                    System.out.println("To advance the project please assign an project manager");
                }
                break;
            case BANKABLE:
                if (canGoToConstruction()) {
                    if(dataSource.changeStage(number, status.id() + 1 )) {
                        status = ProjectStatus.CONSTRUCTION;
                        System.out.println("The project is now in " + status + " stage.\n");
                    }
                } else {
                    System.out.println("To advance the project please capture the project's total budget/fee");
                }
                break;
            case CONSTRUCTION:
                if (canBeFinalised()) {
                    if(dataSource.changeStage(number, status.id() + 1 )) {
                        status = ProjectStatus.FINAL;
                        System.out.println("The project is now in finalised.\n");
                    }
                } else {
                    System.out.println("To advance the project please capture the total amount paid.");
                }
                break;
        }
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
