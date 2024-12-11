import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import static java.lang.Thread.sleep;

public class MainGame extends JPanel implements ActionListener, KeyListener {
    private Timer timer;
    private int X, Y; // 내 위치
    private int previousX = -1, previousY = -1; // 위치의 이동을 파악하기위한 필드
    private final int SPEED = 5;
    private boolean[] keys;
    private int size = 300; // 내 크기
    private JFrame frame;
    private ArrayList<Food> Foods = new ArrayList<>(); // 먹이 리스트
    private Players Players; // 플레이어 데이터 클레스
    private boolean isColor = false;
    private int UserId; // 내 개인 아이디, 초기화 할때 서버로부터 받아옴
    private Color Pcolor = Color.BLACK; // 내 색상 초기화 할때 서버로부터 받아옴
    private int MaxMapSize = 1600; // 맵 사이즈
    private Random random = new Random();
    private boolean isGameOver = false;
    //private boolean eatingF = false; // 통신속도로인한 사이즈 변경이 중첩되어 적용되는것을 방지
    private String username;
    private Image backgroundImage;
    private boolean overlayVisible = false; // 채팅창의 활성화 비활성화
    private JPanel overlayPanel; // 채팅창
    private JLayeredPane layeredPane;
    private JTextPane chatPane;
    private JTextField chatInput;
    private Point point;
    private static final int BUF_LEN = 128;
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private ObjectInputStream ois;
    private Clip backgroundClip;
    private Clip eatingClip;


    public MainGame(String Username,String ip_addr, String port_no,Point point) {
        username = Username;
        backgroundImage = new ImageIcon(getClass().getResource("/images/space.jpg")).getImage();
        keys = new boolean[256];
        X = random.nextInt(MaxMapSize);
        Y = random.nextInt(MaxMapSize);
        this.point = point;
        timer = new Timer(30, this);
        timer.start();

       // playBackgroundMusic("/sounds/space.wav");
        new Thread(() -> playBackgroundMusic("/sounds/space.wav")).start();
        addKeyListener(this);
        setFocusable(true);
        setPreferredSize(new Dimension(600, 400));

        try {
            socket = new Socket(ip_addr, Integer.parseInt(port_no));
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());

            String msg = String.format("/PS %d %d %d %s", X, Y, size, username); // 접속시 서버에 선언
            SendMessage(msg);

            msg = dis.readUTF(); // 아이디를 받아옴
            String[] cmsg = msg.split(" ");
            UserId = Integer.parseInt(cmsg[1]);

            new ListenDatas().start(); // 클레스 수신 시작
            SendMessage("start"); // 초기화 요청
        } catch (IOException e) {
            e.printStackTrace();
        }
        create_game();
    }

    private void SendMessage(String msg) {
        if (socket.isClosed()) return;
        try {
            dos.writeUTF(msg);
        } catch (IOException e) {
            cleanup();
        }
    }

    private void cleanup() {
        try {
            if (timer != null) {
                timer.stop();
            }
            if (ois != null) {
                ois.close();
            }
            if (dos != null) {
                dos.close();
            }
            if (dis != null) {
                dis.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    class ListenDatas extends Thread {
        private int beforeSize = -1;

        public void run() {
            while (!socket.isClosed()) {
                try {
                    Object obj = ois.readObject();
                    if (obj instanceof Datas) {
                        Datas data = (Datas) obj;
                        synchronized (Foods) {
                            Foods = data.getFoods();
                        }
                        Players = data.getPlayers();
                      //  if (eatingF && beforeSize != Players.getPlayers().size()) eatingF = false;
                       // beforeSize = Players.getPlayers().size();
                        if (!isColor) {
                            for (Player p : Players.getPlayers()) {
                                if (p.getID() == UserId) {
                                    Pcolor = p.getColor();
                                    isColor = true;
                                }
                            }
                        }
                    } else if (obj instanceof Massage) {
                        Massage message = (Massage) obj;
                        setMessage(message.getName() + ": " + message.getMassage());
                    } else if (obj instanceof ChFoods) {
                        ChFoods f = (ChFoods) obj;
                        synchronized (Foods) {
                            System.out.println("Removing food: " + f.getDeleted().getId());
                            Foods.removeIf(food -> food.getId() == f.getDeleted().getId());
                            System.out.println("Adding food: " + f.getNew().getId());
                            Foods.add(f.getNew());
                        }
                    } else if (obj instanceof Players) {
                        Players = (Players) obj;
                    }
                } catch (IOException | ClassNotFoundException e) {
                    cleanup();
                    break;
                }
            }
        }
    }

    private void setMessage(String message) { // 메시지를 창에 출력
        StyledDocument doc = chatPane.getStyledDocument();
        try {
            doc.insertString(doc.getLength(), message + "\n", null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        chatPane.setCaretPosition(doc.getLength());
    }

    private int lengCul(int x1, int y1, int x2, int y2) {//플레이어와 먹이or플레이어간의 거리파악
        return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
    }

    private boolean isItHit(Objects ob) { // 충돌판정
        int obSize = ob.getSize();
        return lengCul(X, Y, ob.getX(), ob.getY()) < (size + obSize) * (size + obSize) / 400;
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        int backgroundX = 300 - X % backgroundImage.getWidth(null);
        int backgroundY = 200 - Y % backgroundImage.getHeight(null);

        //배경이미지 로드
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                g.drawImage(backgroundImage, backgroundX + i * backgroundImage.getWidth(null),
                        backgroundY + j * backgroundImage.getHeight(null), null);
            }
        }


        // 플레이어 자신의 크기와 위치
        int x, y, s;
        g.setColor(Pcolor);
        g.fillOval(300 - size / 20, 200 - size / 20, size / 10 + 1, size / 10 + 1); // 좌측 상단이 중점이기 떄문에 x,y를 반지름만큼 빼준다. 
        g.setColor(Color.WHITE);
        g.drawOval(300 - size / 20, 200 - size / 20, size / 10 + 1, size / 10 + 1);

        g.setColor(Color.lightGray);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString(username,  300 - username.length()*(4)+2,200 - size / 20 - 6 );
        //X + 반지름 - 글자길이/2, Y + 반지름 + 글자높이/2 


        // 화면에 출력되는것은 내 위치주변에 화면에 보일것만을 골라낸후 내 위치를 중점으로 좌표를 변환해 출력한다.
        //사이즈의 1/10 이 실질적으로 그려짐. 먹이 5개 ->  1사이즈 업
        Iterator<Food> iterator1 = Foods.iterator();
        while (iterator1.hasNext()) {
            Food F = iterator1.next();
            x = F.getX();
            y = F.getY();
            s = F.getSize();
            if (isItHit(F)) {
        
                playEatingSound("/sounds/eatingSound.wav");
                size += 2;
                String msg = String.format("/FD %d %d %d", F.getId(), size, UserId);
                SendMessage(msg);
                
            iterator1.remove();
            }
            if (x > X - 300 && x < X + 300 && y > Y - 200 && y < Y + 200) {
                g.setColor(Color.GREEN);
                g.fillOval(300 - (X - x) - s / 20, 200 - (Y - y) - s / 20, s / 10, s / 10);
                g.setColor(Color.WHITE);
                g.drawOval(300 - (X - x) - s / 20, 200 - (Y - y) - s / 20, s / 10, s / 10);
            }
        }



        // 다른 플레이어 표시

        Iterator<Player> playerIterator = Players.getPlayers().iterator();  // Create an iterator

        while (playerIterator.hasNext()) {
            Player P = playerIterator.next();

            if (P.getID() != UserId) {
                x = P.getX();
                y = P.getY();
                s = P.getSize();
                Color c = P.getColor();
                g.setColor(c);

                if (isItHit(P)) {
                    if (P.getSize() > size) {
                        isGameOver = true;
                        if (isGameOver) {
                            cleanup();
                            SwingUtilities.invokeLater(() -> {
                                MainClient client = new MainClient();
                                client.setSize(600, 400);
                                client.setLocationRelativeTo(null);
                                client.setVisible(true);
                            });
                            frame.dispose();
                            return;
                        }
                    }
                    if (P.getSize() < size) {

                       // if(!eatingF) {
                            playEatingSound("/sounds/eatingSound.wav");
                          //  System.out.println("EAT");
                            size += P.getSize();
                            SendMessage(String.format("/PE %d %d %d", size, UserId,P.getID()));
                            playerIterator.remove();
                           // eatingF = true;
                       // }
                       
                         Players.rmPlayer(P);
                        
                    }
                }

                if (x > X - 300 - s / 20 && x < X + 300 + s / 20 && y > Y - 200 - s / 20 && y < Y + 200 + s / 20) {
                    g.fillOval(300 - (X - x) - s / 20, 200 - (Y - y) - s / 20, s / 10, s / 10);
                    g.setColor(Color.WHITE);
                    g.drawOval(300 - (X - x) - s / 20, 200 - (Y - y) - s / 20, s / 10, s / 10);

                    g.setColor(Color.lightGray);
                    g.setFont(new Font("Arial", Font.BOLD, 14));
                    g.drawString(P.getName(),  300 - (X - x) - P.getName().length()*(4)+2, 200 - (Y - y) - s / 20 -6);
                 
                }
            }
        }
        //스코어 보드
        drawScoreBoard(g);
    }

    // 스코어 보드
    private void drawScoreBoard(Graphics g) {
        g.setColor(Color.lightGray);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("Score Board", 10, 20);

        int yPosition = 40;
        for (Player player : Players.getPlayers()) { // 플레이어의 사이즈가 기준
            g.drawString(player.getID() +". "+ player.getName() + ": " + player.getSize()*10, 10, yPosition);
            yPosition += 20;
        }
    }

    public void actionPerformed(ActionEvent e) { // 내 위치가 변경되었을때만 위치 정보를 서버로 넘김
        if (!isGameOver) {
            updatePlayerPosition();
            if (previousX != X || previousY != Y) {
                SendMessage(String.format("/PM %d %d %d", X, Y, UserId));
            }
            previousX = X;
            previousY = Y;
            repaint();
        }
    }

    private void updatePlayerPosition() {
        if (keys[KeyEvent.VK_LEFT] && X > 0) X -= SPEED;
        if (keys[KeyEvent.VK_RIGHT] && X < MaxMapSize) X += SPEED;
        if (keys[KeyEvent.VK_UP] && Y > 0) Y -= SPEED;
        if (keys[KeyEvent.VK_DOWN] && Y < MaxMapSize) Y += SPEED;
    }

    public void keyPressed(KeyEvent e) {
        keys[e.getKeyCode()] = true;
    
        if (e.getKeyCode() == KeyEvent.VK_T) { // 채팅창 활성화 T
           // System.out.println("T");
            overlayVisible = !overlayVisible;
            overlayPanel.setVisible(overlayVisible);
            chatInput.requestFocusInWindow();
            revalidate();
            repaint();
            chatInput.setText("");
        }
    }

    public void keyReleased(KeyEvent e) { // esc눌릴시 채팅창 비활성화
        keys[e.getKeyCode()] = false;
    }

    public void create_game() {
        frame = new JFrame("Agar.Io");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
    
        // JLayeredPane 초기화
        layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(600, 400));
        frame.add(layeredPane);
    
        // MainGame 패널 추가
        this.setBounds(0, 0, 600, 400);
        layeredPane.add(this, JLayeredPane.DEFAULT_LAYER);
    
        // Overlay panel 초기화 -> 채팅
        overlayPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(new Color(128, 128, 128, 128)); // 반투명 회색
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        overlayPanel.setBounds(0, 0, 600, 400);
        overlayPanel.setOpaque(false); // 패널을 투명하게 설정
        overlayPanel.setVisible(false); // 초기에는 보이지 않도록 설정
        overlayPanel.setLayout(new BorderLayout());
    
        // 채팅창 추가
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setOpaque(false); // JTextPane을 투명하게 설정
        chatPane.setBackground(new Color(0, 0, 0, 0)); // 배경색을 투명하게 설정
        chatPane.setForeground(Color.WHITE); // 텍스트 색상을 흰색으로 설정
        chatPane.setFont(new Font("Arial", Font.BOLD, 20));
        StyledDocument doc = chatPane.getStyledDocument();
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setLineSpacing(attrs, 0.1f); // 줄 간격 설정
        doc.setParagraphAttributes(0, doc.getLength(), attrs, false);
        JScrollPane chatScrollPane = new JScrollPane(chatPane);
        chatScrollPane.setOpaque(false); // JScrollPane을 투명하게 설정
        chatScrollPane.getViewport().setOpaque(false); // 뷰포트를 투명하게 설정
        overlayPanel.add(chatScrollPane, BorderLayout.CENTER);
    
        // 채팅입력 필드 추가
        chatInput = new JTextField();
        chatInput.setOpaque(false); // JTextField를 투명하게 설정
        chatInput.setBackground(new Color(0, 0, 0, 0)); // 배경색을 투명하게 설정
        chatInput.setForeground(Color.WHITE); // 텍스트 색상을 흰색으로 설정
        chatInput.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message = chatInput.getText();
                chatInput.setText("");
                SendMessage(String.format("/CH %s %s", username, message));
            }
        });
        chatInput.addKeyListener(new KeyAdapter() { // 채팅 입력 필드에 포커스가 가있을때 esc
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    overlayVisible = !overlayVisible;
                    overlayPanel.setVisible(overlayVisible);
                    MainGame.this.requestFocusInWindow(); // 포커스를 MainGame 패널로 다시 설정
                    // 안그러면 플레이어 컨트롤이 안됨.
                }
            }
        });
        chatPane.addKeyListener(new KeyAdapter() { // 채팅창에 포커스가 가있을때 esc
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    overlayVisible = !overlayVisible;
                    overlayPanel.setVisible(overlayVisible);
                    MainGame.this.requestFocusInWindow(); // 포커스를 MainGame 패널로 다시 설정
                }
            }
        });
        overlayPanel.add(chatInput, BorderLayout.SOUTH);
    
        layeredPane.add(overlayPanel, JLayeredPane.PALETTE_LAYER);
    
        frame.pack();
        frame.setVisible(true);
        frame.setLocation(point);
        this.requestFocusInWindow();
    }
    @Override
    public void keyTyped(KeyEvent e) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'keyTyped'");
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

    private void playEatingSound(String filePath) {
        try (InputStream audioSrc = getClass().getResourceAsStream(filePath);
             InputStream bufferedIn = new BufferedInputStream(audioSrc)) {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bufferedIn);
            eatingClip = AudioSystem.getClip();
            eatingClip.open(audioInputStream);
            eatingClip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
