# ExchangeRate - Thi giữa kỳ đề 1

Chương trình Java dùng UDP Unicast, gồm đúng 2 file mã nguồn:

- `ExchangeRateServer.java`: server console, tạo tỉ giá và gửi đến từng client mỗi giây.
- `ExchangeRateClient.java`: client Java Swing, bảng 4 cột Time, Tokyo, Paris, Seoul; bên dưới có nút Pause và Resume. Giao diện dùng font và màu mặc định của hệ thống, không tùy chỉnh.

## Cách chạy

Cài JDK 8 trở lên, mở terminal tại thư mục chứa 2 file và biên dịch:

```sh
javac -encoding UTF-8 ExchangeRateServer.java ExchangeRateClient.java
```

Mở một terminal chạy server:

```sh
java ExchangeRateServer
```

Mở terminal khác chạy client:

```sh
java ExchangeRateClient
```

Có thể chạy nhiều client bằng cách mở thêm terminal. Nếu server chạy trên máy khác trong mạng LAN, truyền IP máy server:

```sh
java ExchangeRateClient 192.168.1.10
```

Hai máy cần kết nối được với nhau; cho phép UDP cổng 5000 trên firewall của máy server. Dừng server bằng Ctrl+C, đóng cửa sổ để thoát client.

## Cách hoạt động

1. Client gửi `RESUME` để đăng ký địa chỉ IP và cổng UDP với server.
2. Server dùng Random tạo 3 tỉ giá Tokyo, Paris, Seoul mỗi giây. Các giá trị minh họa lần lượt là VND cho 1 JPY, 1 EUR và 1 KRW, không phải dữ liệu thị trường thật.
3. Server gửi một gói UDP riêng đến từng client đang đăng ký. Ví dụ: `05/10/2026 14:30:01|165.25|28500.50|18.20`. Thời gian lấy từ đồng hồ máy server.
4. Client tách dữ liệu bằng dấu `|` rồi thêm một hàng vào bảng.
5. Pause gửi `PAUSE`, server xóa client khỏi danh sách nhận. Client cũng ngừng thêm hàng ngay, giữ lại các hàng đã có. Resume đăng ký lại để nhận các cập nhật mới, không tải lại dữ liệu đã bỏ qua.
6. Client gửi lại trạng thái mỗi 5 giây. Server xóa đăng ký nếu quá 15 giây không nhận được RESUME, tránh giữ client đã tắt đột ngột.

## Tối ưu xử lý, băng thông và lưu trữ

- Server dùng một luồng và HashMap lưu địa chỉ client, không tạo một luồng riêng cho mỗi client.
- Một bộ tỉ giá được tạo mỗi giây rồi dùng chung cho tất cả client.
- Gộp thời gian và 3 tỉ giá trong một gói văn bản nhỏ; không gửi riêng từng thị trường.
- Pause ngừng luồng tỉ giá từ server tới client đó; chỉ còn gói lệnh nhỏ mỗi 5 giây để khắc phục trường hợp mất lệnh UDP.
- Server không lưu lịch sử. Client chỉ giữ tối đa 50 hàng mới nhất, không ghi file. Bảng tự cuộn xuống hàng mới nhất; có thể Pause để cuộn xem các hàng đã nhận.
- Luồng nhận UDP chạy riêng; việc cập nhật bảng chạy trên luồng giao diện Swing để cửa sổ vẫn thao tác được.
- Socket có thời gian chờ, tránh vòng lặp kiểm tra liên tục khi không có dữ liệu.

UDP có thể mất gói, trùng gói hoặc đảo thứ tự. Bài này ưu tiên đơn giản, không gửi lại tỉ giá bị mất; client tiếp tục nhận ở lần cập nhật sau. Khoảng gửi là xấp xỉ 1 giây, có thể chênh lệch do xử lý và mạng. Cơ chế gửi lại trạng thái giúp đăng ký lại sau khi server khởi động lại.

Tên biến và phương thức tự đặt dùng tiếng Việt không dấu; tên lớp giữ theo đề. Không cần thư viện ngoài.
