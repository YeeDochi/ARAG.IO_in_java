import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class Server extends JFrame {
    private int sendingFrame = 2;
    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    private JTextArea textArea;
    private JTextField txtPortNumber;
    private ArrayList<Food> Foods = new ArrayList<>();
    private Players Players = new Players();
    private int MaxMapSize = 1600;
    private ServerSocket socket;
    private Vector<UserService> UserVec = new Vector<>();
    private static final int BUF_LEN = 128;
    private Datas data;
    private int PlayerCounter = 1;
    private boolean DiedPlayer = false;
    private boolean sendText = false;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                Server frame = new Server();
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void startServer(){
        EventQueue.invokeLater(() -> {
            try {
                Server frame = new Server();
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    public Server() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 338, 386);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(null);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBounds(12, 10, 300, 244);
        contentPane.add(scrollPane);

        textArea = new JTextArea();
        textArea.setEditable(false);
        scrollPane.setViewportView(textArea);

        JLabel lblNewLabel = new JLabel("Port Number");
        lblNewLabel.setBounds(12, 264, 87, 26);
        contentPane.add(lblNewLabel);

        txtPortNumber = new JTextField();
        txtPortNumber.setHorizontalAlignment(SwingConstants.CENTER);
        txtPortNumber.setText("30000");
        txtPortNumber.setBounds(111, 264, 199, 26);
        contentPane.add(txtPortNumber);
        txtPortNumber.setColumns(10);

        JButton btnServerStart = new JButton("Server Start");
        btnServerStart.addActionListener(e -> {
            try {
                socket = new ServerSocket(Integer.parseInt(txtPortNumber.getText()));
                AppendText("Chat Server Running..");
                btnServerStart.setText("Chat Server Running..");
                btnServerStart.setEnabled(false);
                txtPortNumber.setEnabled(false);
                new AcceptServer().start();
            } catch (NumberFormatException | IOException e1) {
                e1.printStackTrace();
            }
        });
        btnServerStart.setBounds(12, 300, 300, 35);
        contentPane.add(btnServerStart);

        for (int i = 0; i < 100; i++) {
            Foods.add(new Food(MaxMapSize, i));
        }
        data = new Datas(Foods, Players);
    }

    private void AppendText(String str) {
        textArea.append(str + "\n");
        textArea.setCaretPosition(textArea.getText().length());
    }

    class AcceptServer extends Thread {
        public void run() {
            while (true) {
                try {
                    AppendText("Waiting clients ...");
                    Socket client_socket = socket.accept();
                    AppendText("새로운 참가자 from " + client_socket);
                    UserService new_user = new UserService(client_socket);
                    synchronized (UserVec) {
                        UserVec.add(new_user);
                    }
                    AppendText("사용자 입장. 현재 참가자 수 " + UserVec.size());
                    new_user.start();
                } catch (IOException e) {
                    AppendText("accept 에러 발생");
                }
            }
        }
    }

    class UserService extends Thread {
        private DataInputStream dis;
        private DataOutputStream dos;
        private ObjectOutputStream oos;
        private Socket client_socket;
        private boolean isRunning = true; // 클라이언트 연결 상태
        private int Id;
        private Player Me;
        //private int chageCounter=sendingFrame; // 전송 트래픽을 줄이기 위한 카운터(해결)


        public UserService(Socket client_socket) {
            this.client_socket = client_socket;
            try {
                dis = new DataInputStream(client_socket.getInputStream());
                dos = new DataOutputStream(client_socket.getOutputStream());
                oos = new ObjectOutputStream(dos);
                String line1 = dis.readUTF();
                String[] msg = line1.split(" ");
                Me = new Player(Integer.parseInt(msg[1]), Integer.parseInt(msg[2]), Integer.parseInt(msg[3]), PlayerCounter, msg[4]);
                synchronized (Players) {
                    Players.addPlayer(Me);
                }
                Id = PlayerCounter;
                dos.writeUTF("/PID " + PlayerCounter++);
                //WriteAll(Players);
            } catch (IOException e) {
                AppendText("UserService 초기화 에러");
                cleanup();
            }
        }
        // 여기부터 WriteOne,WriteAll 오버로딩 전송하는 데이터 타입에 따라 오버로딩됨
        private void WriteOne(Datas datas) {
            if (!isRunning) return; // 클라이언트가 연결된 경우에만 전송
            try {
                oos.reset();
                oos.writeObject(datas); // 초기화용 유저, 먹이 위치값
                oos.flush();
            } catch (IOException e) {
                AppendText("클라이언트로 데이터 전송 중 에러 발생");
                //cleanup();
            }
        }


        private void WriteAll(Datas datas) { // 초기화 데이터 전송
            synchronized (UserVec) {
                Iterator<UserService> iterator = UserVec.iterator();
                while (iterator.hasNext()) {
                    UserService user = iterator.next();
                    if (user.isRunning) {
                        user.WriteOne(datas);
                    } else {
                        UserVec.remove(user);
                        AppendText("연결오류");
                    }
                }
            }
        }

        private void WriteOne(ChFoods datas) { // 먹이의 소멸과 생성 데이터
            if (!isRunning) return; 
            try {
                oos.reset();
                oos.writeObject(datas);
                oos.flush();
            } catch (IOException e) {
                AppendText("클라이언트로 데이터 전송 중 에러 발생");
                //cleanup();
            }
        }


        private void WriteAll(ChFoods datas) { // 먹이 전송
            synchronized (UserVec) {
                Iterator<UserService> iterator = UserVec.iterator();
                while (iterator.hasNext()) {
                    UserService user = iterator.next();
                    if (user.isRunning) {
                        user.WriteOne(datas);
                    } else {
                        UserVec.remove(user);
                        AppendText("연결오류");
                    }
                }
            }
        } 

        private void WriteOne(Players datas) { // 플레이어의 이동과 사망, 성장데이터
            if (!isRunning) return; 
            try {
                oos.reset();
                oos.writeObject(datas);
                oos.flush();
            } catch (IOException e) {
                AppendText("클라이언트로 데이터 전송 중 에러 발생");
              //  cleanup();
            }
        }


        private void WriteAll(Players datas) { // 플레이어 데이터
            synchronized (UserVec) {
                Iterator<UserService> iterator = UserVec.iterator();
                while (iterator.hasNext()) {
                    UserService user = iterator.next();
                    if (user.isRunning) {
                        user.WriteOne(datas);
                    } else {
                        UserVec.remove(user);
                        AppendText("연결오류");
                    }
                }
            }
        }
    
        private void WriteOneM(Massage datas) { // 채팅 메시지 전송을 위한 메소드
            if (!isRunning) return; 
            try {
                oos.reset();
                oos.writeObject(datas);
                oos.flush();
            } catch (IOException e) {
                AppendText("클라이언트로 데이터 전송 중 에러 발생");
              //  cleanup();
            }
        }


        private void WriteAllM(Massage datas) {
            synchronized (UserVec) {
                Iterator<UserService> iterator = UserVec.iterator();
                while (iterator.hasNext()) {
                    UserService user = iterator.next();
                    if (user.isRunning) {
                        user.WriteOneM(datas);
                    } else {
                        UserVec.remove(user);
                        AppendText("연결오류");
                    }
                }
            }
        }

        public void run() {
            try {
                while (isRunning) {
                    String msg = dis.readUTF();
                    
                    synchronized (data) {
                        if (fixData(msg)) { // 초기화, 혹은 플레이어의 상태 변화가 있을때만 플레이어 클레스를 전송한다.
                            data.setFoods(Foods);
                            data.setPlayers(Players);
                            WriteAll(Players);
                        }
                        else if(sendText){ // 채팅이 수신되었을때 전송하고 플레드를 비활성화한다.     
                            String[] cutedMsg = msg.split(" ",3); 
                            Massage massage = new Massage(cutedMsg[1],cutedMsg[2]);
                            WriteAllM(massage);
                            sendText = false;
                        }
                       
                        // if (chageCounter == sendingFrame) { // 버벅임을 줄이기위한 카운터
                        //     chageCounter = 0;// 해결
                        //     WriteAll(Players);
                        // } else {
                        //     chageCounter++;
                        // }
                    }
                }
            } catch (IOException e) {
                this.isRunning = false;
                AppendText("클라이언트 연결 종료: " + e.getMessage());
                cleanup(); 
            }
        }

        private boolean fixData(String msg) { // 클라이언트로부터 받아온 데이터를 처리
            //AppendText(msg);
            String[] cutedMsg = msg.split(" ");
            switch (cutedMsg[0]) {
                case "start": // 익명의 유저의 접속, 초기화
                    WriteAll(data);
                    return true;
                case "/PM": // 플레이어의 이동
                    synchronized (Players) {
                            if (cutedMsg[3].equals(String.valueOf(Id))) {
                                Me.setX(Integer.parseInt(cutedMsg[1]));
                                Me.setY(Integer.parseInt(cutedMsg[2]));
                                
                                return true;
                            }
                        }
                    break;
                case "/FD": // 먹이 섭취
                    synchronized (Foods) {
                        Iterator<Food> foodIterator = Foods.iterator();
                        while (foodIterator.hasNext()) {
                            Food f = foodIterator.next();
                            if (cutedMsg[1].equals(String.valueOf(f.getId()))) {
                                try {
                                    Food nF = new Food(MaxMapSize, f.getId()); // 먹이 데이터는 맵 크기 안에서 렌덤한 위치에 생성된다
                                    ChFoods Chf = new ChFoods(f, nF); // 사라진 먹이의 클레스와 생성된 먹이 클레스를 보냄으로써 클라이언트에서 제거 추가한다.
                                    foodIterator.remove();
                                    Foods.add(nF);// 서버측에서도 초기화할떄의 전송할 먹이 리스트를 수정한다.
                                    WriteAll(Chf);
                                    AppendText("CHF: " + f.getId() + " -> " + nF.getId());

                                    if (cutedMsg[3].equals(String.valueOf(Id))) {
                                        Me.setSize(Integer.parseInt(cutedMsg[2]));
                                        return true;
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    break;

                case "/PE": // 플레이어 사망, 성장
                    if (cutedMsg[2].equals(String.valueOf(Id))) {
                        ArrayList<Player> P = Players.getPlayers();
                        Me.setSize(Integer.parseInt(cutedMsg[1]));  // size 갱신
                        for (Player p: P){
                            if(cutedMsg[3].equals(String.valueOf(p.getID()))){
                                P.remove(p);
                                Players.setPlayers(P);
                                return true;
                            }
                        }
                    }
                    break;

                case "/CH": // 체팅
                    sendText = true;
                    return false;
                default:
                    return false;
            }
            return false;
        }

        private void cleanup() { // 접속 끊긴 객체 제거
            /*지금 플레이어 사망시 
            클라측에서 먼저 연결을 끊기때문에 
            서버에서는 끊긴 클라이언트를
            상시 확인할 필요가 있다.*/
            try {
                if (oos != null) oos.close();
                if (dos != null) dos.close();
                if (dis != null) dis.close();
                if (client_socket != null && !client_socket.isClosed()) client_socket.close();
                
                synchronized (UserVec) { // 연결되어있는 소켓 제거
                    UserVec.remove(this);
                    AppendText("UserVec에서 사용자 제거: " + this);
                }

                synchronized (Players) { // 남아있는 플레이어 데이터 제거
                    ArrayList<Player> P = Players.getPlayers();
                    for (Player p : P) {
                        if (p.getID() == Me.getID()) {
                            P.remove(p);
                            Players.setPlayers(P);
                            AppendText("Players에서 사용자 제거: " + p);
                            break;
                        }
                    }
                }
                AppendText("CleanUp 완료. 남은 참가자 수: " + UserVec.size());
            } catch (IOException e) {
                AppendText("cleanup 에러 발생: " + e.getMessage());
            }
        }
    }
}
