import helper_classes.CustomFontLoader;
import helper_classes.OnClickEventHelper;
import helper_classes.OnFocusEventHelper;
import helper_classes.RoundedBorder;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.InputStream;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class MainClient extends JFrame {

    private JPanel contentPane;
    private JTextField txtUserName;
    private Image backgroundImage;
    private int backgroundX = 0;
    private int backgroundY = 0;
    private final int MOVE_STEP = 1;
    private Clip backgroundClip;
    
    
        public static void main(String[] args) {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    try {
                        MainClient frame = new MainClient();
                        frame.setSize(600, 400);
                        frame.setVisible(true);
    
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        public void StartGame(){
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    try {
                        MainClient frame = new MainClient();
                        frame.setSize(600, 400);
                        frame.setVisible(true);
    
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    
        public MainClient() { // 클라이언트의 외형은 vsc에 확장프로그램으로 제작했습니다.
            //helper_classes도 그 안에 포함 되어있습니다.
            backgroundImage = new ImageIcon(getClass().getResource("/images/space.jpg")).getImage();
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setBounds(400, 100, 600, 400);
            ActionListener action = new Myaction();
            contentPane = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    int imageWidth = backgroundImage.getWidth(this);
                    int imageHeight = backgroundImage.getHeight(this);
                    for (int x = backgroundX; x < getWidth(); x += imageWidth) {
                        for (int y = backgroundY; y < getHeight(); y += imageHeight) {
                            g.drawImage(backgroundImage, x, y, this);
                        }
                    }
                    setOpaque(false);
                }
            };
            new Thread(() -> playBackgroundMusic("/sounds/space.wav")).start();
            contentPane.setLayout(new GridBagLayout());
            contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
            setContentPane(contentPane);
            contentPane.setBackground(Color.decode("#1e1e1e"));
    
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.CENTER;
    
            JLabel title = new JLabel("ARAG.IO");
            title.setFont(CustomFontLoader.loadFont("./resources/fonts/Lato.ttf", 50));
            title.setForeground(Color.decode("#D9D9D9"));
            contentPane.add(title, gbc);
    
            gbc.gridy++;
            JLabel element4 = new JLabel("User name");
            element4.setFont(CustomFontLoader.loadFont("./resources/fonts/Lato.ttf", 14));
            element4.setForeground(Color.decode("#D9D9D9"));
            contentPane.add(element4, gbc);
    
            gbc.gridy++;
            txtUserName = new JTextField("Player", 15);
            txtUserName.setFont(CustomFontLoader.loadFont("./resources/fonts/Lato.ttf", 14));
            txtUserName.setBackground(Color.decode("#B2B2B2"));
            txtUserName.setForeground(Color.decode("#656565"));
            txtUserName.setBorder(new RoundedBorder(2, Color.decode("#979797"), 0));
            txtUserName.addActionListener(action);
        OnFocusEventHelper.setOnFocusText(txtUserName, "Player", Color.decode("#353535"), Color.decode("#656565"));
        contentPane.add(txtUserName, gbc);

        gbc.gridy++;
        JButton startButton = new JButton("Start");
        startButton.setPreferredSize(new Dimension(106, 28));
        startButton.setBackground(Color.decode("#2e2e2e"));
        startButton.setForeground(Color.decode("#D9D9D9"));
        startButton.setFont(CustomFontLoader.loadFont("./resources/fonts/Lato.ttf", 14));
        startButton.setBorder(new RoundedBorder(4, Color.decode("#979797"), 1));
        startButton.setFocusPainted(false);
        startButton.addActionListener(action);
        OnClickEventHelper.setOnClickColor(startButton, Color.decode("#232323"), Color.decode("#2e2e2e"));
        contentPane.add(startButton, gbc);

        Timer timer = new Timer(30, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                backgroundX -= MOVE_STEP;
                backgroundY -= MOVE_STEP;
                repaint();
            }
        });
        timer.start();
    }
     private void playBackgroundMusic(String filePath) {
        try (InputStream audioSrc = getClass().getResourceAsStream(filePath);
             InputStream bufferedIn = new BufferedInputStream(audioSrc)) {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bufferedIn);
            backgroundClip = AudioSystem.getClip();
            backgroundClip.open(audioInputStream);
            backgroundClip.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    class Myaction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String username = txtUserName.getText().trim();
            String ip_addr = "127.0.0.1";
            String port_no = "30000";
            Point location = getLocation();
            MainGame game = new MainGame(username, ip_addr, port_no,location);
            dispose();
        }
    }
}
