import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.JTable;

public class ExchangeRateClient extends JFrame {
    private DatagramSocket ketNoi;
    private InetAddress diaChi;
    private final int cong = 5000;
    private volatile boolean dangNhan = true;
    private volatile boolean dangChay = true;
    private final JButton nutDung = new JButton("Pause");
    private final JButton nutTiep = new JButton("Resume");
    private final DefaultTableModel duLieuBang = new DefaultTableModel(new String[]{"Time", "Tokyo", "Paris", "Seoul"}, 0) {
        public boolean isCellEditable(int hang, int cot) {
            return false;
        }
    };

    private final JTable bang = new JTable(duLieuBang);

    public ExchangeRateClient(String mayChu) throws Exception {
        diaChi = InetAddress.getByName(mayChu);
        ketNoi = new DatagramSocket();
        ketNoi.connect(diaChi, cong);
        ketNoi.setSoTimeout(1000);

        setTitle("Exchange Rate - " + mayChu);
        setSize(600, 350);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        bang.getTableHeader().setReorderingAllowed(false);
        bang.getColumnModel().getColumn(0).setPreferredWidth(200);
        add(new JScrollPane(bang), BorderLayout.CENTER);
        JPanel nut = new JPanel();
        nut.add(nutDung);
        nut.add(nutTiep);
        add(nut, BorderLayout.SOUTH);
        nutTiep.setEnabled(false);

        nutDung.addActionListener(suKien -> {
            dangNhan = false;
            guiLenh("PAUSE");
            nutDung.setEnabled(false);
            nutTiep.setEnabled(true);
        });
        nutTiep.addActionListener(suKien -> {
            dangNhan = true;
            guiLenh("RESUME");
            nutDung.setEnabled(true);
            nutTiep.setEnabled(false);
        });
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent suKien) {
                dangChay = false;
                guiLenh("STOP");
                ketNoi.close();
                dispose();
            }
        });
        setVisible(true);
        Thread luongNhan = new Thread(this::nhanTiGia);
        luongNhan.setDaemon(true);
        luongNhan.start();
    }

    private void guiLenh(String lenh) {
        try {
            byte[] duLieu = lenh.getBytes(StandardCharsets.UTF_8);
            ketNoi.send(new DatagramPacket(duLieu, duLieu.length, diaChi, cong));
        } catch (java.io.IOException loi) {
            if (dangChay) {
                System.out.println("Khong gui duoc lenh: " + loi.getMessage());
            }
        }
    }

    private void nhanTiGia() {
        byte[] boDem = new byte[256];
        long lanDangKy = 0;
        while (dangChay) {
            long hienTai = System.nanoTime();
            if (hienTai - lanDangKy >= 5_000_000_000L || lanDangKy == 0) {
                guiLenh(dangNhan ? "RESUME" : "PAUSE");
                lanDangKy = hienTai;
            }
            DatagramPacket goiNhan = new DatagramPacket(boDem, boDem.length);
            try {
                ketNoi.receive(goiNhan);
                String noiDung = new String(goiNhan.getData(), 0,
                        goiNhan.getLength(), StandardCharsets.UTF_8);
                String[] cacCot = noiDung.split("\\|");
                if (dangNhan && cacCot.length == 4) {
                    SwingUtilities.invokeLater(() -> {
                        if (dangNhan && dangChay) {
                            if (duLieuBang.getRowCount() >= 60) {
                                duLieuBang.removeRow(0);
                            }
                            duLieuBang.addRow(cacCot);
                            bang.scrollRectToVisible(bang.getCellRect(
                                    duLieuBang.getRowCount() - 1, 0, true));
                        }
                    });
                }
            } catch (java.net.SocketTimeoutException loi) {
            } catch (java.io.IOException loi) {
                if (dangChay) {
                    System.out.println("Chua nhan duoc du lieu server: " + loi.getMessage());
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ngat) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }

    public static void main(String[] thamSo) {
        String mayChu = thamSo.length > 0 ? thamSo[0] : "127.0.0.1";
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                new ExchangeRateClient(mayChu);
            } catch (Exception loi) {
                JOptionPane.showMessageDialog(null, "Loi client: " + loi.getMessage());
            }
        });
    }
}
