# Rental Mobil REST API — Spring Boot Take-Home Assessment

Backend REST API untuk studi kasus **rental mobil**, dibuat untuk memenuhi persyaratan *Backend Developer Take-Home Test: Spring Boot Developer*. Fokus implementasi adalah master data mobil, pengecekan ketersediaan berdasarkan periode, dan transaksi rental yang konsisten.

## 1. Ringkasan studi kasus

Aplikasi menyediakan data mobil serta transaksi penyewaan mobil. Setiap mobil memiliki tarif harian dan status operasional. Pelanggan dapat membuat transaksi apabila mobil berstatus `AVAILABLE` dan tidak memiliki transaksi `BOOKED` yang bertabrakan dengan periode sewa yang diminta.

Total harga dihitung oleh backend berdasarkan tarif harian dari data mobil dan jumlah hari kalender, termasuk tanggal mulai dan tanggal akhir.

### Persyaratan assessment yang dipenuhi

| Persyaratan pada assessment | Implementasi dalam proyek |
|---|---|
| `POST` — Create Resource / Master Data | `POST /api/v1/cars` dengan validasi nama/merek, model, nomor plat, dan tarif |
| `GET` — List Resources | `GET /api/v1/cars`, menampilkan data mobil; dapat memeriksa ketersediaan untuk periode tertentu |
| `POST` — Create Transaction / Order | `POST /api/v1/rentals`, memvalidasi periode dan ketersediaan, menghitung harga, serta menyimpan transaksi |
| Database MySQL | Tabel `cars` dan `rentals` dengan relasi foreign key |
| `@Transactional` dan rollback | Proses pembuatan mobil/rental dan perubahan status dijalankan dalam transaksi Spring; exception runtime membatalkan transaksi |
| Clean architecture | Pemisahan Controller, Service, Repository, Entity, DTO, dan Exception Handler |
| Global error handling | `@RestControllerAdvice` menghasilkan response JSON terstruktur |
| Migration tool (nilai tambah) | Flyway migration pada `src/main/resources/db/migration` |
| Unit test (nilai tambah) | Unit test untuk validasi tanggal, periode bentrok, dan kalkulasi harga |

## 2. Teknologi

- Java 21
- Spring Boot 3.5.x
- Maven
- Spring Web
- Spring Data JPA / Hibernate
- Jakarta Bean Validation
- MySQL 8+
- Flyway
- Springdoc OpenAPI / Swagger UI
- JUnit 5, Mockito, dan H2 untuk pengujian

## 3. Skema database

### `cars`

Menyimpan master data mobil.

| Kolom | Fungsi |
|---|---|
| `id` | Primary key |
| `brand` | Merek mobil, wajib diisi |
| `model` | Model mobil, wajib diisi |
| `plate_number` | Nomor plat unik |
| `daily_rate` | Tarif sewa per hari, harus lebih besar dari 0 |
| `status` | Status operasional: `AVAILABLE`, `MAINTENANCE`, `INACTIVE` |
| `created_at`, `updated_at` | Waktu pembuatan dan perubahan data |

### `rentals`

Menyimpan transaksi penyewaan dan memiliki relasi many-to-one ke `cars` (satu mobil dapat memiliki banyak riwayat transaksi).

| Kolom | Fungsi |
|---|---|
| `id` | Primary key transaksi |
| `car_id` | Foreign key ke `cars.id` |
| `customer_name`, `customer_email` | Data pelanggan |
| `start_date`, `end_date` | Periode sewa |
| `total_price` | Total harga hasil kalkulasi backend |
| `status` | Status transaksi: `BOOKED`, `CANCELLED`, `COMPLETED` |
| `created_at`, `updated_at` | Waktu pembuatan dan perubahan transaksi |

Skema dibuat oleh Flyway melalui `V1__create_rental_schema.sql`. Hibernate memakai `ddl-auto: validate`, sehingga struktur tabel harus sesuai dengan migration.

## 4. Arsitektur dan struktur kode

```text
src/main/java/com/example/rental/
├── controller/    # HTTP endpoint dan pemetaan request/response
├── service/       # Aturan bisnis dan transaksi
├── repository/    # Query database melalui Spring Data JPA
├── entity/        # Entity Car dan Rental serta enum status
├── dto/           # Request/response DTO dan validasi input
└── exception/     # Custom exception dan global handler

src/main/resources/
├── application.yml
└── db/migration/  # Skrip Flyway

src/test/java/     # Unit test
postman/           # Collection dan environment Postman
```

Alur request utama adalah `Controller -> Service -> Repository -> MySQL`. Entity tidak dipakai sebagai input request secara langsung; controller menggunakan DTO yang divalidasi sebelum logika bisnis diproses.

## 5. Prasyarat

Untuk menjalankan proyek secara lokal, siapkan:

- JDK 21
- Maven 3.9 atau versi kompatibel
- MySQL 8+
- Git (opsional, untuk pengumpulan melalui repository publik)

## 6. Menjalankan aplikasi secara lokal

### 6.1 Buat database

Buka MySQL/DBeaver lalu jalankan:

```sql
CREATE DATABASE rental_mobil;
```

### 6.2 Atur koneksi database

Aplikasi membaca konfigurasi dari environment variable berikut. Sesuaikan nilainya dengan lingkungan lokal.

```text
DB_URL=jdbc:mysql://localhost:3306/rental_mobil?serverTimezone=Asia/Jakarta
DB_USERNAME=root
DB_PASSWORD=password_mysql_kamu
```

Jika tidak diatur, `application.yml` memakai URL lokal database `rental_mobil`, username `root`, dan password kosong.

### 6.3 Jalankan test dan aplikasi

Dari direktori yang memiliki `pom.xml`, jalankan:

```bash
mvn clean test
mvn spring-boot:run
```

Flyway akan menjalankan migration ketika aplikasi start. Jangan membuat tabel secara manual apabila migration Flyway sudah digunakan pada database tersebut.

## 7. Menjalankan dengan Docker Compose

Pastikan Docker Desktop/Docker Engine aktif, lalu dari direktori proyek jalankan:

```bash
docker compose up --build
```

Konfigurasi default lokal menggunakan database `rental_mobil` dan password MySQL `rental_local_password`. Untuk mengubah password, set environment variable `DB_PASSWORD` sebelum menjalankan Compose. Jangan gunakan kredensial default untuk produksi.

Untuk menghentikan aplikasi:

```bash
docker compose down
```

Data MySQL disimpan di Docker volume `rental_mysql_data`. Menghentikan container tidak otomatis menghapus volume tersebut.

## 8. REST API

Base URL lokal: `http://localhost:8080`

### Endpoint master data mobil

| Method | Endpoint | Fungsi |
|---|---|---|
| `POST` | `/api/v1/cars` | Membuat data mobil |
| `GET` | `/api/v1/cars` | Daftar mobil dengan pagination |
| `GET` | `/api/v1/cars/{id}` | Detail mobil |
| `PUT` | `/api/v1/cars/{id}` | Memperbarui data mobil |
| `PATCH` | `/api/v1/cars/{id}/status` | Mengubah status operasional mobil |

Untuk menampilkan ketersediaan terhadap periode tertentu, kirim kedua query parameter berikut:

```http
GET /api/v1/cars?startDate=2026-11-01&endDate=2026-11-03
```

`startDate` dan `endDate` harus diisi bersama. Tanpa kedua parameter tersebut, response menampilkan daftar mobil tanpa menghitung ketersediaan pada suatu periode. Pagination memakai `page`, `size`, dan `sort`, misalnya:

```http
GET /api/v1/cars?page=0&size=10&sort=id,asc
```

### Endpoint transaksi rental

| Method | Endpoint | Fungsi |
|---|---|---|
| `POST` | `/api/v1/rentals` | Membuat transaksi rental |
| `GET` | `/api/v1/rentals` | Daftar transaksi dengan pagination |
| `PATCH` | `/api/v1/rentals/{id}/cancel` | Membatalkan transaksi `BOOKED` |
| `PATCH` | `/api/v1/rentals/{id}/complete` | Menyelesaikan rental setelah tanggal akhir terlewati |

Pagination transaksi juga memakai `page`, `size`, dan `sort`.

## 9. Contoh request

### 9.1 Tambah mobil

`POST /api/v1/cars` dengan `Content-Type: application/json`:

```json
{
  "brand": "Toyota",
  "model": "Avanza",
  "plateNumber": "B 1234 XYZ",
  "dailyRate": 350000
}
```

Validasi: `brand`, `model`, dan `plateNumber` wajib diisi; nomor plat tidak boleh duplikat; `dailyRate` harus lebih besar dari 0.

### 9.2 Buat transaksi rental

`POST /api/v1/rentals` dengan `Content-Type: application/json`:

```json
{
  "carId": 1,
  "customerName": "Budi Santoso",
  "customerEmail": "budi@example.com",
  "startDate": "2026-11-01",
  "endDate": "2026-11-03"
}
```

Tanggal mulai dan tanggal akhir sama-sama dihitung. Dengan tarif Rp350.000 per hari selama tiga hari, backend menghitung total `1050000.00`. Client tidak mengirim atau menentukan `totalPrice`.

### 9.3 Ubah status operasional mobil

`PATCH /api/v1/cars/1/status`:

```json
{
  "status": "MAINTENANCE"
}
```

Nilai status yang valid: `AVAILABLE`, `MAINTENANCE`, dan `INACTIVE`.

## 10. Aturan bisnis dan transaksi

1. Mobil harus ditemukan dan berstatus `AVAILABLE` sebelum rental dibuat.
2. Tanggal sewa wajib diisi, tanggal mulai tidak boleh di masa lalu, dan `endDate` tidak boleh sebelum `startDate`.
3. Sistem menolak rental `BOOKED` lain untuk mobil yang sama jika periode tanggalnya beririsan. Tanggal awal dan akhir bersifat inklusif.
4. Total harga dihitung di sisi server: `tarif harian x jumlah hari kalender`.
5. Pembuatan rental memakai `@Transactional`. Exception yang terjadi dalam transaksi menyebabkan rollback otomatis oleh Spring.
6. Proses booking mengambil pessimistic write lock pada baris mobil untuk membantu menserialisasi upaya booking bersamaan pada mobil yang sama.
7. Rental hanya dapat dibatalkan atau diselesaikan ketika statusnya `BOOKED`. Rental hanya dapat diselesaikan setelah tanggal akhirnya terlewati.
8. Status mobil tidak dapat diubah menjadi `MAINTENANCE` atau `INACTIVE` jika masih memiliki rental `BOOKED` yang tanggal akhirnya belum terlewati.

## 11. Validasi dan format error

Global exception handler menggunakan `@RestControllerAdvice` dan mengembalikan response JSON terstruktur.

Contoh error validasi (`400 Bad Request`):

```json
{
  "timestamp": "2026-10-18T01:00:00Z",
  "success": false,
  "status": 400,
  "error": "Bad Request",
  "message": "Validasi input gagal",
  "details": {
    "brand": "must not be blank"
  }
}
```

Kode status yang umum:

- `201 Created`: resource atau transaksi berhasil dibuat.
- `200 OK`: operasi baca atau perubahan berhasil.
- `400 Bad Request`: input atau parameter tidak valid.
- `404 Not Found`: mobil atau transaksi tidak ditemukan.
- `409 Conflict`: pelanggaran aturan bisnis, misalnya jadwal bentrok atau plat duplikat.
- `500 Internal Server Error`: kesalahan yang tidak ditangani sebagai error bisnis.

Response sukses memakai envelope `timestamp`, `success`, `message`, dan `data`.

## 12. Swagger dan Postman

Setelah aplikasi berjalan:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Collection dan environment Postman berada di direktori `postman/`:

- `postman/Rental-Mobil-API.postman_collection.json`
- `postman/Rental-Mobil-Local.postman_environment.json`
- `postman/README.md`

Cara menjalankan:
1. Buka Postman dan pilih **Import**.
2. Import collection dan environment dari direktori `postman/`.
3. Pilih environment **Rental Mobil - Local**.
4. Pastikan Spring Boot dan MySQL aktif di `http://localhost:8080`.
5. Jalankan collection lewat **Run** agar request dieksekusi berurutan. Sejumlah request menggunakan ID yang disimpan oleh request sebelumnya.

Sebagian skenario negatif memang mengharapkan `400`, `404`, atau `409`. Periksa tab **Test Results** untuk melihat assertion yang lulus atau gagal. Test membuat data baru dan tidak menghapus data secara otomatis; gunakan database development/test, bukan database produksi.

## 13. Unit test

Jalankan:

```bash
mvn test
```

Unit test service yang tersedia mencakup:
- Penolakan tanggal akhir yang lebih awal daripada tanggal mulai.
- Penolakan periode rental yang bertabrakan.
- Perhitungan total harga berdasarkan tarif server.

Jalankan test di komputer sendiri dan pastikan hasilnya `BUILD SUCCESS` sebelum mengirimkan repository. Unit test yang tersedia bukan pengganti integration test penuh terhadap MySQL.
