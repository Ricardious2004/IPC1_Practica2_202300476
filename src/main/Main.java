package main;

import com.formdev.flatlaf.themes.FlatMacLightLaf;
import java.io.File;
import java.util.ArrayList;
import javax.swing.JFileChooser;
import javax.swing.UIManager;

/**
 *
 * @author Ricardious
 */
public class Main {

    static ArrayList<Route> routes = new ArrayList<Route>();
    static ArrayList<String> inicioList = new ArrayList<>();
    static ArrayList<String> finList = new ArrayList<>();
    private static int counterRoutes = 0;
    private static int counterTrips = 0;
    
    
    private JFileChooser fileChooser;
    private File JFileSelected;

    public static void main(String args[]) {
        try {
            UIManager.setLookAndFeel(new FlatMacLightLaf());
//            UIManager.put("Button.arc", 999);
//            UIManager.put("Component.arc", 999);
//            UIManager.put("ProgressBar.arc", 999);
            UIManager.put("TextComponent.arc", 10);
//            UIManager.put("ScrollBar.trackArc", 999);
//            UIManager.put("ScrollBar.thumbArc", 999);
//            UIManager.put("ScrollBar.trackInsets", new Insets(2, 4, 2, 4));
//            UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
//            UIManager.put("ScrollBar.track", new Color(0xe0e0e0));
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }

        // Instanceando un objeto de tipo Window (es nuestra interfaz gráfica)
        MainFrame login = new MainFrame();
        login.setVisible(true);

        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        //Lectura del archivo serializado si ya existe
        // Si la función readArchivo nos retorna un objeto entonces realizamos el casteo para que ese 
        // objeto sea ahora un objeto arraylist que almacene rutas.
        // Ahora si nos retorna un valor null readArchivo entonces significa que el archivo no existe

        
    }
    

    

        
    public static int generateId(String option) {
        int id = 0;
        switch (option) {
            case "route":
                id = counterRoutes + 1;
                counterRoutes++;
                break;
            case "trip":
                id = counterTrips + 1;
                counterTrips++;
                break;
        }

        return id;
    }

        


    public static void editTable(int id, String start, String end, int distance){
        for (Route route : routes) {
            if (route.getId() == id) {
                route.getDistance();
                break;
            }
        }
    }

    
    

}
