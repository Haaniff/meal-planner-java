package mealplanner;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Scanner;

public class Main {
  public static void main(String[] args) throws SQLException, IOException {
      Scanner scanner = new Scanner(System.in);
      new Controller().menu(scanner);

  }
}