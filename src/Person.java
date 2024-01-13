public class Person {

    public long id;
    public String firstName;
    public String surname;
    public String address;
    public String email;

    public String fullName() {
        return new StringBuilder().append(firstName).append(' ').append(surname).toString();
    }
}
