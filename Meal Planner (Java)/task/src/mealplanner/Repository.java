package mealplanner;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class Repository {
    String DB_URL = System.getenv("DB_URL");
    String USER = System.getenv("DB_USER");
    String PASS = System.getenv("DB_PASSWORD");

    Connection connection;


    public Repository() throws SQLException {
        connection = DriverManager.getConnection(DB_URL, USER, PASS);
    }

    public void createTables() throws SQLException {

        String createMeals = """
            
                CREATE TABLE IF NOT EXISTS meals (
                meal_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                category VARCHAR(1024) NOT NULL,
                meal VARCHAR(1024) NOT NULL
                            )
            """;

        String createIngredients = """
             CREATE TABLE IF NOT EXISTS ingredients (
             ingredient_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
              ingredient VARCHAR(1024) NOT NULL,
             meal_id INTEGER NOT NULL REFERENCES meals(meal_id)
              )               
              """;
                String createPlan = """
                CREATE TABLE IF NOT EXISTS plan (
                 plan_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                weekday VARCHAR(20) NOT NULL,
                category VARCHAR(20) NOT NULL,
                meal_id INTEGER NOT NULL REFERENCES meals(meal_id),
                UNIQUE (weekday, category)
                )
             """;
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(createMeals);
            statement.executeUpdate(createIngredients);
            statement.executeUpdate(createPlan);
        }
    }
    public int populateMeals(Meals.Category category, String mealName)
                throws SQLException {
                String sql = """
             INSERT INTO meals (category, meal)
                    VALUES (?, ?)
                    RETURNING meal_id
            """;
        connection.setAutoCommit(false);

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category.toString().toLowerCase());
            statement.setString(2, mealName);

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt("meal_id");
            }
        }
    }


    public void populateIngredients(List<String> ingredients, int mealId) throws SQLException {
                String sql =
                """
            INSERT INTO ingredients (ingredient, meal_id)
            VALUES (?, ?)
            """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String ingredient : ingredients) {
                statement.setString(1, ingredient);
                statement.setInt(2, mealId);
                statement.addBatch();
            }

            statement.executeBatch();
        }
    }
    public List<String> getIngredients() throws SQLException {
        String sql = """
            SELECT i.ingredient, COUNT(*) AS cnt
            FROM plan p
            JOIN ingredients i ON i.meal_id = p.meal_id
            GROUP BY i.ingredient
            ORDER BY i.ingredient
            """;

        List<String> ingredients = new ArrayList<>();

        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                String name = rs.getString("ingredient");
                int count = rs.getInt("cnt");
                ingredients.add(count > 1 ? name + " x" + count : name);
            }
        }

        return ingredients;
    }
    boolean isPlanEmpty() throws SQLException {
        String sql = "SELECT COUNT(*) FROM plan";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            return rs.next() && rs.getInt(1) == 0;
        }
    }
    public void writeToFile(String fileName){
        try{
        if (isPlanEmpty()) {
            System.out.println("Unable to save. Plan your meals first.");
        } else {
            Files.write(Path.of(fileName), getIngredients());
            System.out.println("Saved!");
        }
    }catch (SQLException | IOException e){
        System.out.println("Could not save: " + e.getMessage());
    }
    }


    public void showMeals(String category) throws SQLException {
        String mealSql = """
        SELECT meal_id, meal
        FROM  meals
        WHERE category = ?
        """;
        String  ingredientSql =
                """
            SELECT  ingredient
            FROM ingredients
            WHERE meal_id = ?
            """;

        boolean hasMeals = false;
        try (PreparedStatement statement = connection.prepareStatement(mealSql);
             PreparedStatement ingStatement = connection.prepareStatement(ingredientSql);) {

            statement.setString(1, category);

            try (ResultSet rs=statement.executeQuery() ;){

                if (rs.next()) {
                    hasMeals = true;

                    System.out.println("Category: " + category);

                    do {
                        int mealId = rs.getInt("meal_id");

                        System.out.println("Name: " + rs.getString("meal"));

                        System.out.println("Ingredients: ");

                        ingStatement.setInt(1, mealId);

                        try (ResultSet ingRs = ingStatement.executeQuery();) {

                            while (ingRs.next()) {

                                System.out.println(ingRs.getString(
                                        "ingredient"));

                            }
                        }
                        System.out.println();
                    } while (rs.next());
                }
            }
        }

        if (!hasMeals) {
    System.out.println("No meals found.");
        }
    }


    public Map<String, Integer> getMeals(String category) throws SQLException {
        String sql = """
        SELECT meal_id, meal
        FROM meals
        WHERE category = ?
        ORDER BY meal
        """;
        Map<String, Integer> meals = new LinkedHashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, category);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    meals.put(rs.getString("meal"), rs.getInt("meal_id"));
                }
            }
        }
        return meals;
    }

    public void commit() throws SQLException {
        connection.commit();
    }
    public void close() throws SQLException {
        connection.close();
    }

    public void savePlan(String weekday, String category, Integer integer) throws SQLException {
        String insertPlan = """
    INSERT INTO plan (weekday, category, meal_id)
    VALUES (?, ?, ?)
    ON CONFLICT (weekday, category)
    DO UPDATE SET meal_id = EXCLUDED.meal_id
    """;
        try (PreparedStatement ps = connection.prepareStatement(insertPlan)) {

                ps.setString(1, weekday);
                ps.setString(2, category);
                ps.setInt(3,integer);
                ps.executeUpdate();
    }
        if (!connection.getAutoCommit()) {
            connection.commit();
        }
}
    public void showPlan() throws SQLException {

        String planned = """
        SELECT p.weekday, p.category, m.meal
        FROM plan p
        JOIN meals m ON m.meal_id = p.meal_id
        ORDER BY
            CASE p.weekday
                WHEN 'monday' THEN 1
                WHEN 'tuesday' THEN 2
                WHEN 'wednesday' THEN 3
                WHEN 'thursday' THEN 4
                WHEN 'friday' THEN 5
                WHEN 'saturday' THEN 6
                WHEN 'sunday' THEN 7
            END ASC,
            CASE p.category
                WHEN 'breakfast' THEN 1
                WHEN 'lunch' THEN 2
                WHEN 'dinner' THEN 3
            END ASC
            """;

        try (Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(planned)){
            String currentDay = null;

            while (resultSet.next()) {
                String weekday = resultSet.getString("weekday");
                String category = resultSet.getString("category");

                if (!weekday.equals(currentDay)) {
                    currentDay = weekday;
                    System.out.println(weekday);
                }
                String title = Character.toUpperCase(category.charAt(0)) + category.substring(1);
                System.out.println(title + ": " + resultSet.getString("meal"));
            }
                }
        }
    }



