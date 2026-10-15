package mealplanner;


import java.io.IOException;
import java.sql.SQLException;
import java.util.*;


public class Meals {

    Repository repository;
    private final Scanner scanner;
    private static final List<String> WEEK_DAYS =
            List.of("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday");

    public enum Category {
        BREAKFAST,
        LUNCH,
        DINNER;

        public String label() {
            return name().toLowerCase();
        }
    }

    public Meals(Scanner scanner) throws SQLException {
        this.scanner = scanner;

        repository = new Repository();

        repository.createTables();

    }

    public void whichMeal() {
        while (true) {
            System.out.println("Which meal do you want to add (breakfast, lunch, dinner)?");
            if (!scanner.hasNextLine()) {
                return;
            }
            String input = scanner.nextLine().toUpperCase();
            try {
                Category category = Category.valueOf(input);
                addMeals(category, scanner);
                return;

            } catch (IllegalArgumentException e) {
                System.out.println("Wrong meal category! Choose from: breakfast, lunch, dinner.");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }


    public void addMeals(Category category, Scanner scanner) throws SQLException {

        System.out.println("Input the meal's name:");
        String mealName;

        while (true) {
            if (!scanner.hasNextLine()) {
                return;
            }

            mealName = scanner.nextLine().trim();

            if (mealName.matches("[A-Za-z]+( [A-Za-z]+)*")) {
                break;
            }

            System.out.println("Wrong format. Use letters only!");
        }

        System.out.println("Input the ingredients:");

        while (true) {
            if (!scanner.hasNextLine()) {
                return;
            }

            String input = scanner.nextLine().trim();

            String[] ingredientArray = input.split(",", -1);
            List<String> ingredients = new ArrayList<>();

            boolean valid = true;

            for (String ingredient : ingredientArray) {
                ingredient = ingredient.trim();

                if (!ingredient.matches("[A-Za-z]+( [A-Za-z]+)*")) {
                    valid = false;
                    break;
                }

                ingredients.add(ingredient);
            }
            if (!valid) {
                System.out.println("Wrong format. Use letters only!");
                continue;
            }


            addMeals(ingredients, category, mealName);
            return;
        }
    }


    private void addMeals(List<String> ingredients, Category category, String mealName) throws SQLException {

        int mealId = repository.populateMeals(category, mealName);


        repository.populateIngredients(ingredients, mealId);


        repository.commit();


        System.out.println("The meal has been added!");

    }

    public void showMeals() throws SQLException {
        System.out.println("Which category do you want to print (breakfast, lunch, dinner)?");
        String category = scanner.nextLine().trim().toLowerCase();
        while (!category.equals("breakfast")
                && !category.equals("lunch")
                && !category.equals("dinner")) {
            System.out.println("Wrong meal category! Choose from: breakfast, lunch, dinner.");

            category = scanner.nextLine().trim();
        }
        repository.showMeals(category);
    }

    public void planMeals() throws SQLException {
        for (String day : WEEK_DAYS) {
            System.out.println(capitalize(day));

            for (Category category : Category.values()) {
                Map<String, Integer> options = repository.getMeals(category.label());
                options.keySet().forEach(System.out::println);

                System.out.println("Choose the " + category.label() + " for " + capitalize(day) + " from the list above:");
                String choice = scanner.nextLine().trim();

                while (!options.containsKey(choice)) {
                    System.out.println("This meal doesn’t exist. Choose a meal from the list above.");
                    choice = scanner.nextLine().trim();
                }

                repository.savePlan(day, category.label(), options.get(choice));
            }
            System.out.println("Yeah! We planned the meals for " + capitalize(day) + ".");
        }
        showWeeklyPlan();
    }
    public void savePlan() throws SQLException, IOException {
        if (repository.isPlanEmpty()) {
            System.out.println("Unable to save. Plan your meals first.");
            return;
        }
        System.out.println("Input a filename:");
        String filename = scanner.nextLine().trim();
        repository.writeToFile(filename);
    }

    private static String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    public void close() throws SQLException {
        repository.close();
    }

    public void showWeeklyPlan() throws SQLException {

            repository.showPlan();
    }
    public void saveIngredients() throws SQLException {

    }

    }


