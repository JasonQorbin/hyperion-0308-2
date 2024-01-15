package MainProgram;

public class Person implements Pickable{

    public long id;
    public String firstName;
    public String surname;
    public String address;
    public String email;

    public String fullName() {
        return new StringBuilder().append(firstName).append(' ').append(surname).toString();
    }

    public String getOneLineString() {
        return new StringBuilder().append(firstName).append(' ').append(surname).append(" <").append(email).append(">").toString();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getOneLineString());
        builder.append("\nAddress: ").append(address);
        return builder.toString();
    }
}
