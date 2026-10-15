package mealplanner;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Scanner;


public class Controller {


    public Controller(){
    }

    public void menu(Scanner scanner) throws SQLException, IOException {
        Meals meals = new Meals(scanner);

        while (true)  {
            System.out.println("What would you like to do (add, show, plan, list plan, save, exit)?");

            String choice = scanner.nextLine().trim().toLowerCase();
            switch(choice){
                case "add":meals.whichMeal();break;
                case "show": meals.showMeals();break;
                case "plan": meals.planMeals();break;
                case "list plan": meals.showWeeklyPlan();break;
                case "save" : meals.savePlan();break;
                case "exit":
                    System.out.println("bye!");
                    meals.close();
                    return;
                    default:break;

            }

        }
    }
}
