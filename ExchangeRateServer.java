import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Locale;

public class ExchangeRateServer {
    public static void main(String[] thamSo) {
        int cong = 5000;
        Map<InetSocketAddress, Long> danhSach = new HashMap<>();
        Random ngauNhien = new Random();
        DateTimeFormatter dinhDang = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        byte[] boDem = new byte[64];
        long lanGui = System.nanoTime();

        try (DatagramSocket ketNoi = new DatagramSocket(cong)) {
            ketNoi.setSoTimeout(200);
            System.out.println("Server UDP dang chay tai cong " + cong);
            System.out.println("Nhan Ctrl+C de dung server.");

            while (true) {
                DatagramPacket goiNhan = new DatagramPacket(boDem, boDem.length);
                try {
                    ketNoi.receive(goiNhan);
                    String lenh = new String(goiNhan.getData(), 0, goiNhan.getLength(),
                            StandardCharsets.UTF_8).trim();
                    InetSocketAddress diaChi = new InetSocketAddress(
                            goiNhan.getAddress(), goiNhan.getPort());

                    if (lenh.equals("RESUME")) {
                        if (!danhSach.containsKey(diaChi)) {
                            System.out.println("Client tham gia: " + diaChi);
                        }
                        danhSach.put(diaChi, System.nanoTime());
                    } else if (lenh.equals("PAUSE") || lenh.equals("STOP")) {
                        if (danhSach.remove(diaChi) != null) {
                            System.out.println("Client tam dung/thoat: " + diaChi);
                        }
                    }
                } catch (java.net.SocketTimeoutException loi) {
                }

                long hienTai = System.nanoTime();
                if (hienTai - lanGui >= 1_000_000_000L) {
                    lanGui = hienTai;
                    Iterator<Map.Entry<InetSocketAddress, Long>> duyet =
                            danhSach.entrySet().iterator();
                    while (duyet.hasNext()) {
                        if (hienTai - duyet.next().getValue() > 15_000_000_000L) {
                            duyet.remove();
                        }
                    }

                    if (danhSach.isEmpty()) {
                        continue;
                    }
                    String thoiGian = LocalDateTime.now().format(dinhDang);
                    double tokyo = 160 + ngauNhien.nextDouble() * 10;
                    double paris = 28000 + ngauNhien.nextDouble() * 1000;
                    double seoul = 17 + ngauNhien.nextDouble() * 2;
                    String noiDung = String.format(Locale.US, "%s|%.2f|%.2f|%.2f",
                            thoiGian, tokyo, paris, seoul);
                    byte[] duLieu = noiDung.getBytes(StandardCharsets.UTF_8);

                    for (InetSocketAddress diaChi : danhSach.keySet()) {
                        try {
                            DatagramPacket goiGui = new DatagramPacket(
                                    duLieu, duLieu.length, diaChi);
                            ketNoi.send(goiGui);
                        } catch (java.io.IOException loi) {
                            System.out.println("Khong gui duoc den " + diaChi);
                        }
                    }
                    System.out.println(noiDung + " | So client: " + danhSach.size());
                }
            }
        } catch (java.io.IOException loi) {
            System.out.println("Loi server: " + loi.getMessage());
        }
    }
}
