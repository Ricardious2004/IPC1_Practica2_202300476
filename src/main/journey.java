package main;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import main.MainFrame.*;

/**
 *
 * @author Ricardious
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Journey extends Thread {

    private JLabel labelAMover;
    private JLabel labelDestino;

    public Journey(JLabel labelAMover, JLabel labelDestino) {
        this.labelAMover = labelAMover;
        this.labelDestino = labelDestino;
    }

    @Override
    public void run() {
        // Obtener las coordenadas del JLabel de destino
        Point destino = labelDestino.getLocation();
        // Mover el JLabel gradualmente hacia el JLabel de destino en el eje X
        int deltaX = (destino.x - labelAMover.getX()) / 10;
        try {
            for (int i = 0; i < 10; i++) {
                Point newLocation = new Point(labelAMover.getX() + deltaX, labelAMover.getY());
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        labelAMover.setLocation(newLocation);
                    }
                });
                Thread.sleep(50);
            }
            // Actualizar la posición del JLabel al final de la animación
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    labelAMover.setLocation(destino);
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
