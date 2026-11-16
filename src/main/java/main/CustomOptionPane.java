package main;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.FlowLayout;
import java.awt.GridLayout;

public class CustomOptionPane {
    private int minutes;
    private int increment;
    private boolean confirmed;

    public enum HostPlayerColor {
        COLOR_WHITE,
        COLOR_BLACK,
        COLOR_RANDOM
    }
    private HostPlayerColor color;

    public CustomOptionPane() {
        JPanel optionPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JTextField firstField = new JTextField(10);
        row1.add(new JLabel("Tempo de jogo (minutos):"));
        row1.add(firstField);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JTextField secondField = new JTextField(10);
        row2.add(new JLabel("Incremento (segundos):"));
        row2.add(secondField);

        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        row3.add(new JLabel("Escolha a cor das suas peças:"));

        JPanel row4 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JCheckBox whiteCheck = new JCheckBox("Brancas");
        JCheckBox blackCheck = new JCheckBox("Pretas");
        JCheckBox randomCheck = new JCheckBox("Aleatório", true);

        ButtonGroup group = new ButtonGroup();
        group.add(whiteCheck);
        group.add(blackCheck);
        group.add(randomCheck);

        row4.add(whiteCheck);
        row4.add(blackCheck);
        row4.add(randomCheck);

        optionPanel.add(row1);
        optionPanel.add(row2);
        optionPanel.add(row3);
        optionPanel.add(row4);

        Object[] buttons = {"OK", "Cancelar"};
        int choice = JOptionPane.showOptionDialog(
            null,
            optionPanel,
            "Configurar Partida",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            buttons,
            buttons
        );

        if(choice != 0) {
            confirmed = false;
            return;
        }

        try {
            int firstNum = Integer.parseInt(firstField.getText().trim());
            int secondNum = Integer.parseInt(secondField.getText().trim());

            if(firstNum <= 0 || secondNum < 0) {
                JOptionPane.showMessageDialog(null, "Os tempos devem ser positivos.", "Entrada inválida", JOptionPane.ERROR_MESSAGE);
                confirmed = false;
                return;
            }

            if(whiteCheck.isSelected()) {
                color = HostPlayerColor.COLOR_WHITE;
            }
            else if(blackCheck.isSelected()) {
                color = HostPlayerColor.COLOR_BLACK;
            }
            else {
                color = HostPlayerColor.COLOR_RANDOM;
            }

            minutes = firstNum;
            increment = secondNum;
            confirmed = true;
        }
        catch(NumberFormatException e){
            JOptionPane.showMessageDialog(null, "Digite apenas números inteiros", "Entrada inválida", JOptionPane.ERROR_MESSAGE);
            confirmed = false;
        }
    }

    public int getMinutes() {
        return minutes;
    }

    public int getIncrement() {
        return increment;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public HostPlayerColor getHostColor() {
        return color;
    }
}
