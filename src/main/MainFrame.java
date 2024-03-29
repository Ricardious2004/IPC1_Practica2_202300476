package main;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import main.MainFrame.jPanelGradient;
import javax.swing.JLabel;
import javax.swing.SwingWorker;

/**
 *
 * @author Ricardious
 */
public class MainFrame extends javax.swing.JFrame {

    private int mouseX, mouseY;
    private JFileChooser fileChooser;
    private File JFileSelected;
    private Rectangle transport;

    public MainFrame() {
        this.setUndecorated(true);
        initComponents();
        this.setLocationRelativeTo(null);

        RoundRectangle2D.Float shape = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20);
        setShape(shape);

        // Agregar listener para arrastrar la ventana
        this.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent evt) {
                mouseX = evt.getX();
                mouseY = evt.getY();
            }
        });
        this.addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent evt) {
                // Obtener la posición actual del mouse en la pantalla
                int x = evt.getXOnScreen();
                int y = evt.getYOnScreen();

                // Calcular la nueva posición de la ventana
                setLocation(x - mouseX, y - mouseY);
            }
        });

    }

    //Choose CSV File
    private void chooseCSVFile() throws IOException {
        // Create a file chooser
        fileChooser = new JFileChooser();

        // Set a file filter if needed (optional)
        FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV files", "csv");
        fileChooser.setFileFilter(filter);

        // Show the file chooser dialog
        int result = fileChooser.showOpenDialog(this);

        // Check if a file was chosen
        if (result == JFileChooser.APPROVE_OPTION) {
            // Get the selected file
            JFileSelected = fileChooser.getSelectedFile();

            // Print the path of the selected file
            System.out.println("Selected File: " + JFileSelected.getAbsolutePath());

            // Perform CSV read
            // Perform CSV read and pass the file name
            readCSV();
            checkLists();
            loadRoutes();
        } else {
            System.out.println("No file selected");
        }
    }

// Check if the selected file is a CSV file
    private void readCSV() {
        if (!JFileSelected.getName().toLowerCase().endsWith(".csv")) {
            JOptionPane.showMessageDialog(null, "The selected file is not a CSV file.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Get the name of the selected file
        String name = JFileSelected.getName();
        int routesCount = 0; // Counter for the number of routes added
        int ignoredRoutesCount = 0; // Counter for ignored routes
        boolean hadDuplicates = false; // Flag indicating if duplicates were found
        boolean hadLessThanThreeElements = false; // Flag indicating if a route had less than three elements

        try (BufferedReader br = new BufferedReader(new FileReader(JFileSelected.getAbsolutePath()))) {
            String line; // Store each line of the CSV file
            boolean firstLine = true; // Flag to skip the header
            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue; // Skip the header
                }

                String[] values = line.split(","); // Split the line into parts using comma as delimiter
                if (values.length >= 3) { // Check if the line has at least three elements
                    String start = values[0]; // Extract start from the CSV
                    String end = values[1]; // Extract end from the CSV
                    int distance = Integer.parseInt(values[2]); // Extract distance from the CSV

                    if (!routeExists(start, end, distance)) { // Check if the route already exists
                        Main.inicioList.add(start); // Add start to inicioList
                        Main.finList.add(end); // Add end to finList
                        Main.routes.add(new Route(Main.generateId("route"), start, end, distance)); // Add a new Route object to routes
                        routesCount++; // Increment the routes counter
                    } else {
                        System.out.println("Route already exists: " + start + " to " + end);
                        ignoredRoutesCount++; // Increment ignored routes counter
                        hadDuplicates = true; // Set flag indicating duplicates were found
                    }
                } else {
                    System.out.println("The line doesn't have enough values: " + line);
                    ignoredRoutesCount++; // Increment ignored routes counter
                    hadLessThanThreeElements = true; // Set flag indicating a route had less than three elements
                }
            }

            // Make our lists bidirectional
            int totalRoutes = Main.inicioList.size(); // Get the total number of routes
            for (int i = 0; i < totalRoutes; i++) {
                Main.inicioList.add(Main.finList.get(i)); // Add end to inicioList
                Main.finList.add(Main.inicioList.get(i)); // Add start to finList
            }

            // Create message for successful loading
            String message = "Loaded " + routesCount + " routes from (" + name + ") successfully.";
            // Append information about ignored routes if any
            if (ignoredRoutesCount > 0) {
                if (hadDuplicates && hadLessThanThreeElements) {
                    message += "\nIgnored " + ignoredRoutesCount + " routes because they were duplicates and had less than 3 elements.";
                } else if (hadDuplicates) {
                    message += "\nIgnored " + ignoredRoutesCount + " routes because they were duplicates.";
                } else if (hadLessThanThreeElements) {
                    message += "\nIgnored " + ignoredRoutesCount + " routes because they had less than 3 elements.";
                }
            }

            // Show a success message dialog
            JOptionPane.showMessageDialog(null, message, "SUCCESS", JOptionPane.INFORMATION_MESSAGE);
            // Refresh the routes table
            refreshRoutesTable();
            // Load the routes into the ComboBoxes after reading the CSV file
            loadRoutes();
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(null, "File not found: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error reading the file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Invalid number format: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private boolean routeExists(String start, String end, int distance) {
        for (Route route : Main.routes) {
            if (route.getStart().equals(start) && route.getEnd().equals(end) && route.getDistance() == distance) {
                return true;
            }
        }
        return false;
    }

    private void checkLists() {
        // Verifica si Main.inicioList está vacío
        if (Main.inicioList.isEmpty()) {
            System.out.println("Main.inicioList está vacío. No se han agregado elementos.");
        } else {
            System.out.println("==================================");
            System.out.println("Elementos en Main.inicioList:");
            for (String item : Main.inicioList) {
                System.out.println(item);

            }
            System.out.println("==================================");
        }

        // Verifica si Main.finList está vacío
        if (Main.finList.isEmpty()) {
            System.out.println("Main.finList está vacío. No se han agregado elementos.");
        } else {
            System.out.println("Elementos en Main.finList:");
            for (String item : Main.finList) {
                System.out.println(item);
            }
            System.out.println("==================================");
        }
    }

// Load routes into ComboBoxes
    private void loadRoutes() {
        // Store the selected items
        String startSelected = (String) startBox.getSelectedItem();
        String endSelected = (String) endBox.getSelectedItem();

        // Create a model for startBox
        DefaultComboBoxModel<String> startModel = new DefaultComboBoxModel<>();
        // Add selected item back if exists
        if (startSelected != null) {
            startModel.addElement(startSelected);
        }
        // Add elements from inicioList to startModel
        for (int i = 0; i < Main.inicioList.size(); i++) {
            startModel.addElement(Main.inicioList.get(i));
        }
        // Set the startModel to startBox
        startBox.setModel(startModel);

        // Create a model for endBox
        DefaultComboBoxModel<String> endModel = new DefaultComboBoxModel<>();
        // Add selected item back if exists
        if (endSelected != null) {
            endModel.addElement(endSelected);
        }
        // Add elements from finList to endModel
        for (int i = 0; i < Main.finList.size(); i++) {
            endModel.addElement(Main.finList.get(i));
        }
        // Set the endModel to endBox
        endBox.setModel(endModel);
    }

    public DefaultTableModel getTableModel() {
        return (DefaultTableModel) jTable1.getModel();
    }

    class jPanelGradient extends JPanel {

        private boolean mouseOver = false;

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;

            Color color1 = new Color(84, 51, 255); // Color base
            Color color2 = new Color(32, 189, 255); // Color al que cambia cuando el mouse está sobre el panel

            if (mouseOver) {
                color1 = new Color(255, 153, 0); // Cambia el color base cuando el mouse está sobre el panel
                color2 = new Color(255, 204, 0); // Cambia el color al que cambia cuando el mouse está sobre el panel
            }

            int width = getWidth();
            int height = getHeight();

            GradientPaint gp = new GradientPaint(0, 0, color1, width, height, color2);
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, width, height);
        }

        public void setMouseOver(boolean mouseOver) {
            this.mouseOver = mouseOver;
            repaint();
        }
    }

    @SuppressWarnings("unchecked")


    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new jPanelGradient();
        jLabel3 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jPanel2 = new jPanelGradient();
        jLabel1 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jPanel3 = new jPanelGradient();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jPanel4 = new jPanelGradient();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jPanel5 = new jPanelGradient();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jPanel11 = new jPanelGradient();
        jLabel27 = new javax.swing.JLabel();
        jLabel28 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel6 = new javax.swing.JPanel();
        btnOpenCSV = new javax.swing.JButton();
        editDistance = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel7 = new javax.swing.JPanel();
        jPanel10 = new javax.swing.JPanel();
        jLabel24 = new javax.swing.JLabel();
        jLabel25 = new javax.swing.JLabel();
        jLabel26 = new javax.swing.JLabel();
        startBox = new javax.swing.JComboBox<>();
        endBox = new javax.swing.JComboBox<>();
        typeBox = new javax.swing.JComboBox<>();
        jButton1 = new javax.swing.JButton();
        jPanel8 = new javax.swing.JPanel();
        lblTransport1 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        start1 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        start2 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jLabel23 = new javax.swing.JLabel();
        start3 = new javax.swing.JButton();
        jButton7 = new javax.swing.JButton();
        jButton8 = new javax.swing.JButton();
        lblBarrera = new javax.swing.JLabel();
        lblTransport2 = new javax.swing.JLabel();
        lblTransport3 = new javax.swing.JLabel();
        jPanel9 = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setUndecorated(true);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel1.setBackground(new java.awt.Color(102, 255, 204));
        jPanel1.setForeground(new java.awt.Color(255, 255, 255));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel3.setIcon(new javax.swing.ImageIcon("C:\\Users\\Ricardious\\Videos\\logo.png")); // NOI18N
        jPanel1.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 10, 80, -1));

        jLabel2.setBackground(new java.awt.Color(255, 255, 255));
        jLabel2.setFont(new java.awt.Font("Corbel", 0, 24)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setText("UDRIVE");
        jPanel1.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 90, -1, -1));

        jPanel2.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                jPanel2MouseMoved(evt);
            }
        });
        jPanel2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jPanel2MouseClicked(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                jPanel2MouseExited(evt);
            }
        });
        jPanel2.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel1.setIcon(new javax.swing.ImageIcon("C:\\Users\\Ricardious\\Pictures\\iconos\\subir-archivo.png")); // NOI18N
        jPanel2.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 0, 30, 60));

        jLabel5.setFont(new java.awt.Font("Corbel", 0, 18)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(255, 255, 255));
        jLabel5.setText("Load routes");
        jPanel2.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 20, -1, 30));

        jPanel1.add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 140, 200, 60));

        jPanel3.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                jPanel3MouseMoved(evt);
            }
        });
        jPanel3.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jPanel3MouseClicked(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                jPanel3MouseExited(evt);
            }
        });
        jPanel3.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel6.setIcon(new javax.swing.ImageIcon("C:\\Users\\Ricardious\\Pictures\\iconos\\carro-nuevo.png")); // NOI18N
        jPanel3.add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 0, 40, 60));

        jLabel7.setFont(new java.awt.Font("Corbel", 0, 18)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(255, 255, 255));
        jLabel7.setText("Generate trip");
        jPanel3.add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 20, -1, 30));

        jPanel1.add(jPanel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 210, 200, 60));

        jPanel4.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                jPanel4MouseMoved(evt);
            }
        });
        jPanel4.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jPanel4MouseClicked(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                jPanel4MouseExited(evt);
            }
        });
        jPanel4.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel8.setIcon(new javax.swing.ImageIcon("C:\\Users\\Ricardious\\Pictures\\iconos\\ruta.png")); // NOI18N
        jPanel4.add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 0, -1, 60));

        jLabel9.setFont(new java.awt.Font("Corbel", 0, 18)); // NOI18N
        jLabel9.setForeground(new java.awt.Color(255, 255, 255));
        jLabel9.setText("Trip start");
        jPanel4.add(jLabel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 20, -1, -1));

        jPanel1.add(jPanel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 280, 200, 60));

        jPanel5.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                jPanel5MouseMoved(evt);
            }
        });
        jPanel5.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jPanel5MouseClicked(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                jPanel5MouseExited(evt);
            }
        });
        jPanel5.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel10.setIcon(new javax.swing.ImageIcon("C:\\Users\\Ricardious\\Pictures\\iconos\\historial-de-transacciones.png")); // NOI18N
        jPanel5.add(jLabel10, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 0, -1, 60));

        jLabel11.setFont(new java.awt.Font("Corbel", 0, 18)); // NOI18N
        jLabel11.setForeground(new java.awt.Color(255, 255, 255));
        jLabel11.setText("Trip history");
        jPanel5.add(jLabel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 20, -1, -1));

        jPanel1.add(jPanel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 350, 200, 60));

        jLabel27.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/cerrar-sesion.png"))); // NOI18N

        jLabel28.setBackground(new java.awt.Color(255, 255, 255));
        jLabel28.setFont(new java.awt.Font("Corbel", 0, 18)); // NOI18N
        jLabel28.setForeground(new java.awt.Color(255, 255, 255));
        jLabel28.setText("Logout");

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jLabel27)
                .addGap(33, 33, 33)
                .addComponent(jLabel28)
                .addContainerGap(61, Short.MAX_VALUE))
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel27, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jLabel28)
                .addContainerGap(17, Short.MAX_VALUE))
        );

        jPanel1.add(jPanel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 420, 200, 60));

        getContentPane().add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 200, 700));

        jLabel13.setIcon(new javax.swing.ImageIcon(getClass().getResource("/login/icons8_Multiply_32px.png"))); // NOI18N
        jLabel13.setToolTipText("");
        jLabel13.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel13MouseClicked(evt);
            }
        });
        getContentPane().add(jLabel13, new org.netbeans.lib.awtextra.AbsoluteConstraints(950, 10, -1, -1));

        jLabel12.setIcon(new javax.swing.ImageIcon(getClass().getResource("/login/icons8_Expand_Arrow_32px.png"))); // NOI18N
        jLabel12.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel12MouseClicked(evt);
            }
        });
        getContentPane().add(jLabel12, new org.netbeans.lib.awtextra.AbsoluteConstraints(910, 10, -1, -1));

        jLabel4.setIcon(new javax.swing.ImageIcon("C:\\Users\\Ricardious\\Downloads\\Reef.jpg")); // NOI18N
        getContentPane().add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(200, 0, 800, 50));

        jPanel6.setBackground(new java.awt.Color(255, 255, 255));

        btnOpenCSV.setText("Load Routes (.csv)");
        btnOpenCSV.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOpenCSVActionPerformed(evt);
            }
        });

        editDistance.setText("Edit Distance");
        editDistance.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editDistanceActionPerformed(evt);
            }
        });

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Start", "End", "Distance"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(50, 50, 50)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(btnOpenCSV, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(447, 447, 447)
                        .addComponent(editDistance, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 717, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(40, 40, 40)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnOpenCSV, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(editDistance, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 499, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("tab1", jPanel6);

        jPanel7.setBackground(new java.awt.Color(255, 255, 255));

        jPanel10.setPreferredSize(new java.awt.Dimension(500, 400));

        jLabel24.setBackground(new java.awt.Color(0, 153, 153));
        jLabel24.setFont(new java.awt.Font("Corbel", 2, 24)); // NOI18N
        jLabel24.setForeground(new java.awt.Color(0, 153, 153));
        jLabel24.setText("Select endpoint");

        jLabel25.setBackground(new java.awt.Color(0, 153, 153));
        jLabel25.setFont(new java.awt.Font("Corbel", 2, 24)); // NOI18N
        jLabel25.setForeground(new java.awt.Color(0, 153, 153));
        jLabel25.setText("Select starting point");

        jLabel26.setBackground(new java.awt.Color(0, 153, 153));
        jLabel26.setFont(new java.awt.Font("Corbel", 2, 24)); // NOI18N
        jLabel26.setForeground(new java.awt.Color(0, 153, 153));
        jLabel26.setText("Select type of transportation");

        startBox.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        startBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "✻✻Select starting point✻✻" }));
        startBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startBoxActionPerformed(evt);
            }
        });

        endBox.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        endBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "✻✻✻Select endpoint✻✻✻" }));
        endBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                endBoxActionPerformed(evt);
            }
        });

        typeBox.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        typeBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "✻✻Select type of transportation ✻✻", "Motorcycle 1", "Motorcycle 2", "Motorcycle 3", "Standard Vehicle 1", "Standard Vehicle 2", "Standard Vehicle 3", "Premium Vehicle 1", "Premium Vehicle 2", "Premium Vehicle 3", " " }));

        jButton1.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        jButton1.setText("Generate trip");

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel10Layout.createSequentialGroup()
                        .addGap(113, 113, 113)
                        .addComponent(jLabel26))
                    .addGroup(jPanel10Layout.createSequentialGroup()
                        .addGap(151, 151, 151)
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 190, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel10Layout.createSequentialGroup()
                .addContainerGap(49, Short.MAX_VALUE)
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel25, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(startBox, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 190, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel10Layout.createSequentialGroup()
                        .addGap(28, 28, 28)
                        .addComponent(endBox, javax.swing.GroupLayout.PREFERRED_SIZE, 193, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel10Layout.createSequentialGroup()
                        .addGap(48, 48, 48)
                        .addComponent(jLabel24)))
                .addGap(37, 37, 37))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel10Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(typeBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addGap(57, 57, 57)
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel24)
                    .addComponent(jLabel25))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(endBox, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(startBox, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 50, Short.MAX_VALUE)
                .addComponent(jLabel26)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(typeBox, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(47, 47, 47)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(48, 48, 48))
        );

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addGap(150, 150, 150)
                .addComponent(jPanel10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(180, 180, 180))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addGap(85, 85, 85)
                .addComponent(jPanel10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(158, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("tab2", jPanel7);

        jPanel8.setBackground(new java.awt.Color(255, 255, 255));
        jPanel8.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lblTransport1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/moto_1.gif"))); // NOI18N
        jPanel8.add(lblTransport1, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 70, 120, 66));

        jLabel14.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/carretera.png"))); // NOI18N
        jPanel8.add(jLabel14, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 480, 730, 30));

        start1.setFont(new java.awt.Font("Corbel", 3, 14)); // NOI18N
        start1.setText("Start");
        start1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                start1ActionPerformed(evt);
            }
        });
        jPanel8.add(start1, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 150, -1, -1));

        jButton3.setFont(new java.awt.Font("Corbel", 3, 14)); // NOI18N
        jButton3.setText("Return");
        jPanel8.add(jButton3, new org.netbeans.lib.awtextra.AbsoluteConstraints(700, 150, -1, -1));

        jLabel16.setText("jLabel16");
        jPanel8.add(jLabel16, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 20, -1, -1));

        jLabel17.setText("jLabel17");
        jPanel8.add(jLabel17, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 40, -1, -1));

        jLabel18.setText("jLabel18");
        jPanel8.add(jLabel18, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 60, -1, -1));

        jLabel19.setText("jLabel19");
        jPanel8.add(jLabel19, new org.netbeans.lib.awtextra.AbsoluteConstraints(730, 20, -1, -1));

        jLabel20.setText("jLabel20");
        jPanel8.add(jLabel20, new org.netbeans.lib.awtextra.AbsoluteConstraints(730, 40, -1, -1));

        jLabel21.setText("jLabel21");
        jPanel8.add(jLabel21, new org.netbeans.lib.awtextra.AbsoluteConstraints(730, 60, -1, -1));

        jLabel22.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/carretera.png"))); // NOI18N
        jPanel8.add(jLabel22, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 110, 730, 30));

        start2.setFont(new java.awt.Font("Corbel", 3, 14)); // NOI18N
        start2.setText("Start");
        jPanel8.add(start2, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 330, -1, -1));

        jButton5.setFont(new java.awt.Font("Corbel", 3, 14)); // NOI18N
        jButton5.setText("Return");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });
        jPanel8.add(jButton5, new org.netbeans.lib.awtextra.AbsoluteConstraints(700, 330, -1, -1));

        jLabel23.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/carretera.png"))); // NOI18N
        jPanel8.add(jLabel23, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 290, 730, 30));

        start3.setFont(new java.awt.Font("Corbel", 3, 14)); // NOI18N
        start3.setText("Start");
        jPanel8.add(start3, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 520, -1, -1));

        jButton7.setFont(new java.awt.Font("Corbel", 3, 14)); // NOI18N
        jButton7.setText("Return");
        jPanel8.add(jButton7, new org.netbeans.lib.awtextra.AbsoluteConstraints(700, 520, -1, -1));

        jButton8.setFont(new java.awt.Font("Corbel", 1, 18)); // NOI18N
        jButton8.setText("Start all");
        jButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton8ActionPerformed(evt);
            }
        });
        jPanel8.add(jButton8, new org.netbeans.lib.awtextra.AbsoluteConstraints(340, 590, -1, -1));
        jPanel8.add(lblBarrera, new org.netbeans.lib.awtextra.AbsoluteConstraints(770, 0, 40, 660));

        lblTransport2.setText("jLabel15");
        jPanel8.add(lblTransport2, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 270, -1, -1));

        lblTransport3.setText("jLabel15");
        jPanel8.add(lblTransport3, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 460, -1, -1));

        jTabbedPane1.addTab("tab3", jPanel8);

        jPanel9.setBackground(new java.awt.Color(255, 255, 255));
        jTabbedPane1.addTab("tab4", jPanel9);

        getContentPane().add(jTabbedPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 40, 820, 670));

        setSize(new java.awt.Dimension(1000, 700));
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void jPanel2MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel2MouseClicked
        // TODO add your handling code here:
        jTabbedPane1.setSelectedIndex(0);
    }//GEN-LAST:event_jPanel2MouseClicked

    private void jPanel2MouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel2MouseMoved
        // TODO add your handling code here:
        // Verifica si el mouse ya está sobre el panel
        ((jPanelGradient) jPanel2).setMouseOver(true);

    }//GEN-LAST:event_jPanel2MouseMoved

    private void jPanel2MouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel2MouseExited
        // TODO add your handling code here:
        ((jPanelGradient) jPanel2).setMouseOver(false);
    }//GEN-LAST:event_jPanel2MouseExited

    private void jPanel3MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel3MouseClicked
        // TODO add your handling code here:
        jTabbedPane1.setSelectedIndex(1);
    }//GEN-LAST:event_jPanel3MouseClicked

    private void jPanel3MouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel3MouseMoved
        // TODO add your handling code here:
        ((jPanelGradient) jPanel3).setMouseOver(true);
    }//GEN-LAST:event_jPanel3MouseMoved

    private void jPanel3MouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel3MouseExited
        // TODO add your handling code here:
        ((jPanelGradient) jPanel3).setMouseOver(false);
    }//GEN-LAST:event_jPanel3MouseExited

    private void jPanel4MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel4MouseClicked
        // TODO add your handling code here:
        jTabbedPane1.setSelectedIndex(2);
    }//GEN-LAST:event_jPanel4MouseClicked

    private void jPanel4MouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel4MouseMoved
        // TODO add your handling code here:
        ((jPanelGradient) jPanel4).setMouseOver(true);
    }//GEN-LAST:event_jPanel4MouseMoved

    private void jPanel4MouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel4MouseExited
        // TODO add your handling code here:
        ((jPanelGradient) jPanel4).setMouseOver(false);
    }//GEN-LAST:event_jPanel4MouseExited

    private void jPanel5MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel5MouseClicked
        // TODO add your handling code here:
        jTabbedPane1.setSelectedIndex(3);
    }//GEN-LAST:event_jPanel5MouseClicked

    private void jPanel5MouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel5MouseMoved
        // TODO add your handling code here:
        ((jPanelGradient) jPanel5).setMouseOver(true);
    }//GEN-LAST:event_jPanel5MouseMoved

    private void jPanel5MouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel5MouseExited
        // TODO add your handling code here:
        ((jPanelGradient) jPanel5).setMouseOver(false);
    }//GEN-LAST:event_jPanel5MouseExited

    private void jLabel13MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel13MouseClicked
        // TODO add your handling code here:
        System.exit(0);
    }//GEN-LAST:event_jLabel13MouseClicked

    private void jLabel12MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel12MouseClicked
        // TODO add your handling code here:
        this.setState(MainFrame.ICONIFIED);
    }//GEN-LAST:event_jLabel12MouseClicked

    private void btnOpenCSVActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOpenCSVActionPerformed

        try {
            chooseCSVFile();
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

    }//GEN-LAST:event_btnOpenCSVActionPerformed

    private void editDistanceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editDistanceActionPerformed
        // TODO add your handling code here:
        Distance g = new Distance(this);
        g.setVisible(true);

    }//GEN-LAST:event_editDistanceActionPerformed

    private void startBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startBoxActionPerformed
        // TODO add your handling code here:
        int selectedIndex = startBox.getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex <= Main.finList.size()) {
            endBox.setSelectedIndex(selectedIndex);
        }
    }//GEN-LAST:event_startBoxActionPerformed

    private void endBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_endBoxActionPerformed
        // TODO add your handling code here:
        int selectedIndex = endBox.getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex <= Main.inicioList.size()) {
            startBox.setSelectedIndex(selectedIndex);
        }
    }//GEN-LAST:event_endBoxActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton8ActionPerformed

    private void start1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_start1ActionPerformed

        // Crear una instancia de Journey y comenzar la animación
        Journey journey = new Journey(lblTransport1, lblBarrera);
        journey.start();
    }//GEN-LAST:event_start1ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnOpenCSV;
    private javax.swing.JButton editDistance;
    private javax.swing.JComboBox<String> endBox;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    public javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTabbedPane jTabbedPane1;
    public javax.swing.JTable jTable1;
    private javax.swing.JLabel lblBarrera;
    private javax.swing.JLabel lblTransport1;
    private javax.swing.JLabel lblTransport2;
    private javax.swing.JLabel lblTransport3;
    private javax.swing.JButton start1;
    private javax.swing.JButton start2;
    private javax.swing.JButton start3;
    private javax.swing.JComboBox<String> startBox;
    private javax.swing.JComboBox<String> typeBox;
    // End of variables declaration//GEN-END:variables

    public void refreshRoutesTable() {
        DefaultTableModel DT = new DefaultTableModel(new String[]{"ID", "Inicio", "Final", "Distancia"}, Main.routes.size());
        jTable1.setModel(DT);
        TableModel tbModel = jTable1.getModel();

        for (int i = 0; i < Main.routes.size(); i++) {
            Route ruta = Main.routes.get(i);
            tbModel.setValueAt(ruta.getId(), i, 0);
            tbModel.setValueAt(ruta.getStart(), i, 1);
            tbModel.setValueAt(ruta.getEnd(), i, 2);
            tbModel.setValueAt(ruta.getDistance(), i, 3);
        }

    }

}
