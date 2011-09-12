
package com.ardorcraft.examples.thegame;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import com.ardorcraft.generators.InterpolatedNoiseDataGenerator;
import com.ardorcraft.generators.NiceDataGenerator;

public class SelectDialog extends JDialog {

    private final JPanel contentPanel = new JPanel();

    final Class[] generators = {
            NiceDataGenerator.class, InterpolatedNoiseDataGenerator.class
    };
    final String[] textures = {
            "terrainQ.png", "terrain.png", "terrainDocu.png", "terrainSMP.png"
    };
    final int[] tileSizes = {
            16, 32, 32, 32
    };

    private JComboBox generatorCombo;

    private JComboBox textureCombo;

    private JSlider viewDistanceSpinner;

    private JCheckBox overwriteCheckbox;

    /**
     * Launch the application.
     */
    public static void main(final String[] args) {
        try {
            final SelectDialog dialog = new SelectDialog();
            dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            dialog.setVisible(true);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Create the dialog.
     */
    public SelectDialog() {
        super((Frame) null, "Setup", true);

        setAlwaysOnTop(true);

        setBounds(100, 100, 331, 190);
        getContentPane().setLayout(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        getContentPane().add(contentPanel, BorderLayout.CENTER);
        final GridBagLayout gbl_contentPanel = new GridBagLayout();
        gbl_contentPanel.columnWidths = new int[] {
                0, 0, 0
        };
        gbl_contentPanel.rowHeights = new int[] {
                0, 0, 0, 0, 0
        };
        gbl_contentPanel.columnWeights = new double[] {
                0.0, 1.0, Double.MIN_VALUE
        };
        gbl_contentPanel.rowWeights = new double[] {
                0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE
        };
        contentPanel.setLayout(gbl_contentPanel);
        {
            final JLabel lblSelectTerrainGenerator = new JLabel("Terrain generator:");
            final GridBagConstraints gbc_lblSelectTerrainGenerator = new GridBagConstraints();
            gbc_lblSelectTerrainGenerator.insets = new Insets(0, 0, 5, 5);
            gbc_lblSelectTerrainGenerator.anchor = GridBagConstraints.EAST;
            gbc_lblSelectTerrainGenerator.gridx = 0;
            gbc_lblSelectTerrainGenerator.gridy = 0;
            contentPanel.add(lblSelectTerrainGenerator, gbc_lblSelectTerrainGenerator);
        }
        {
            final String[] classNames = new String[generators.length];
            for (int i = 0; i < generators.length; i++) {
                classNames[i] = generators[i].getSimpleName();
            }
            generatorCombo = new JComboBox(classNames);
            final GridBagConstraints gbc_comboBox = new GridBagConstraints();
            gbc_comboBox.insets = new Insets(0, 0, 5, 0);
            gbc_comboBox.fill = GridBagConstraints.HORIZONTAL;
            gbc_comboBox.gridx = 1;
            gbc_comboBox.gridy = 0;
            contentPanel.add(generatorCombo, gbc_comboBox);
        }
        {
            final JLabel lblSelectTerrainTexture = new JLabel("Terrain texture:");
            final GridBagConstraints gbc_lblSelectTerrainTexture = new GridBagConstraints();
            gbc_lblSelectTerrainTexture.anchor = GridBagConstraints.EAST;
            gbc_lblSelectTerrainTexture.insets = new Insets(0, 0, 5, 5);
            gbc_lblSelectTerrainTexture.gridx = 0;
            gbc_lblSelectTerrainTexture.gridy = 1;
            contentPanel.add(lblSelectTerrainTexture, gbc_lblSelectTerrainTexture);
        }
        {
            textureCombo = new JComboBox(textures);
            final GridBagConstraints gbc_comboBox = new GridBagConstraints();
            gbc_comboBox.insets = new Insets(0, 0, 5, 0);
            gbc_comboBox.fill = GridBagConstraints.HORIZONTAL;
            gbc_comboBox.gridx = 1;
            gbc_comboBox.gridy = 1;
            contentPanel.add(textureCombo, gbc_comboBox);
        }
        {
            final JLabel lblViewDistance = new JLabel("View distance:");
            final GridBagConstraints gbc_lblViewDistance = new GridBagConstraints();
            gbc_lblViewDistance.insets = new Insets(0, 0, 5, 5);
            gbc_lblViewDistance.gridx = 0;
            gbc_lblViewDistance.gridy = 2;
            contentPanel.add(lblViewDistance, gbc_lblViewDistance);
        }
        {
            viewDistanceSpinner = new JSlider(4, 40, 20);
            viewDistanceSpinner.setPaintTicks(true);
            viewDistanceSpinner.setSnapToTicks(true);
            viewDistanceSpinner.setMinorTickSpacing(4);
            viewDistanceSpinner.setMajorTickSpacing(4);
            viewDistanceSpinner.setPaintLabels(true);
            final GridBagConstraints gbc_spinner = new GridBagConstraints();
            gbc_spinner.fill = GridBagConstraints.HORIZONTAL;
            gbc_spinner.insets = new Insets(0, 0, 5, 0);
            gbc_spinner.gridx = 1;
            gbc_spinner.gridy = 2;
            contentPanel.add(viewDistanceSpinner, gbc_spinner);
        }
        {
            final JLabel lblOverwriteMap = new JLabel("Overwrite map:");
            final GridBagConstraints gbc_lblOverwriteMap = new GridBagConstraints();
            gbc_lblOverwriteMap.insets = new Insets(0, 0, 0, 5);
            gbc_lblOverwriteMap.gridx = 0;
            gbc_lblOverwriteMap.gridy = 3;
            contentPanel.add(lblOverwriteMap, gbc_lblOverwriteMap);
        }
        {
            overwriteCheckbox = new JCheckBox("");
            final GridBagConstraints gbc_checkBox = new GridBagConstraints();
            gbc_checkBox.anchor = GridBagConstraints.WEST;
            gbc_checkBox.gridx = 1;
            gbc_checkBox.gridy = 3;
            contentPanel.add(overwriteCheckbox, gbc_checkBox);
        }
        {
            final JPanel buttonPane = new JPanel();
            buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
            getContentPane().add(buttonPane, BorderLayout.SOUTH);
            {
                final JButton okButton = new JButton("OK");
                okButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        SelectDialog.this.setVisible(false);
                    }
                });
                okButton.setActionCommand("OK");
                buttonPane.add(okButton);
                getRootPane().setDefaultButton(okButton);
            }
        }
    }

    public Class getSelectedGenerator() {
        return generators[generatorCombo.getSelectedIndex()];
    }

    public String getSelectedTexture() {
        return textures[textureCombo.getSelectedIndex()];
    }

    public int getSelectedTextureSize() {
        return tileSizes[textureCombo.getSelectedIndex()];
    }

    public int getViewDistance() {
        return viewDistanceSpinner.getValue();
    }

    public boolean getIsOverwriteMap() {
        return overwriteCheckbox.isSelected();
    }
}
