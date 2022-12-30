# udFramework database

udFramework database is a java database framework that can communicates with any database.

## Installation

you just need to add the jar in your lib folder or in your class path.
[download jar](https://github.com/TsinjoCoding/udFramework-database/raw/main/udFramework-database.jar)

## Usage

### step1: create a connection provider (optional)

```java
@ConnectionProvider
public class DBConnection implements ConnectionGetter {
    @Override
    public Connection getConnection() throws SQLException {
        // return the instance of your connection
    }
}
```
## step 2: map a class with your table
Let's assume that we have a table 'person' which has these columns:
- id
- name
- birthday

then the structure of your class will be

```java
import com.udframework.database.DBObject;
import com.udframework.database.annotations.DBColumn;

// by default the name of the table is the class name
// but if you dont want to use the class name as table name
// then you can use the annotation @DBTable("tableName")
@DBTable
public class Person extends DBObject<Person, Integer> {

    @DBColumn(isPrimaryKey = true)
    private Integer id;
    
    // by default the name of the column is the name of the field
    // but if you don't want to use the field name as column name
    // then you can use the annotation @DBColumn("columnName")
    @DBColumn
    private String name;
    
    @DBColumn
    private Date birthDay;

    public Person() throws DatabaseException, NoSuchMethodException {
    }

    // getters and setters
}
```

### step 3: there you go!!

```java

public class Main {
   public static void main(String[] args) throws Exception {
        Person person = new Person();
        
        person.setId(1);
        person.setName("John");
        person.setBirthDay(new Date(System.currentTimeMillis()));
        
        person.create();
        
        Person person1 = person.findById(1);
        
        person1.setName("John Doe");
        person1.update();
        
        person1.delete();

        List<Person> list1 = person.fetchAll().run();
        List<Person> list2 = person.fetchAll().where("name", "John").run();
        List<Person> list3 = person.fetchAll().where("birthDay > '2022-12-30'").run();
        
    }
}
```

## DOCs

Actually, there is no docs for this project.
but as you can see in the example, it's very easy to use,
and there are more features that you can discover, by reading the source code.
However if you have any question, you can ask me on my email
[tsinjo.coding@gmail.com](mailto:tsinjo.coding@gmail.com)
or you can send a message on my facebook page [TsinjoCoding](https://www.facebook.com/profile.php?id=100088985565445)

## Contributing

please open an issue first to discuss what you would like to change.

## License

[BSD-3-Clause](https://opensource.org/licenses/BSD-3-Clause)
