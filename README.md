# KasirKu — Aplikasi Kasir & Manajemen Toko (Android)

MVP fondasi: kasir offline-first dengan Room, siap disambung ke Firebase.

## Cara membuka

1. Buka **Android Studio** → *Open* → pilih folder `pos-app/`
2. Biarkan Gradle sync selesai (butuh koneksi internet pertama kali)
3. *Run* di emulator atau HP (min. Android 8.0 / API 26)

> Catatan: proyek ini belum dicoba compile di mesin pembuatnya (tanpa Android SDK).
> Buka di Android Studio dan laporkan error bila ada — akan saya perbaiki.

## Yang sudah jalan (MVP)

- **Kasir**: cari produk, tambah ke keranjang, stepper qty, harga grosir otomatis
  per jumlah (tier), checkout tunai/QRIS/transfer/e-wallet/bon + hitung kembalian,
  struk otomatis setelah bayar (pratinjau + bagikan via WhatsApp/aplikasi lain)
- **Login PIN multi-user**: setup PIN pemilik saat pertama kali; kasir login
  pakai PIN; nama kasir tercatat di tiap transaksi; ganti pengguna dari Pengaturan
- **Produk**: CRUD, barcode, kategori, satuan, harga beli, harga ecer,
  tier grosir dinamis (nama + min. qty + harga), stok + stok minimum,
  peringatan stok menipis, hapus = soft delete
- **Supplier & barang masuk**: kelola supplier; catat barang masuk → stok
  bertambah otomatis + harga beli terupdate ke harga terakhir
- **Piutang**: checkout metode "Bon" (nama pelanggan wajib) → tercatat sebagai
  piutang; layar Piutang: total belum lunas, riwayat cicilan, catat pembayaran
- **Scan barcode**: CameraX + ML Kit — dari kasir langsung masuk keranjang,
  dari form produk langsung isi field barcode
- **Laporan harian**: omzet, laba, jumlah transaksi, rata-rata, per metode bayar,
  daftar transaksi + pemilih tanggal
- **Langganan**: trial 30 hari; layar Langganan + paywall otomatis saat trial
  habis; skeleton Google Play Billing siap (isi product ID di Play Console)
- **Offline-first**: semua data di Room (SQLite) lokal; tanpa internet tetap jalan
- **SyncWorker**: terjadwal tiap ~00:00 via WorkManager (hanya saat online);
  logika upload Firestore disiapkan sebagai TODO berlabel jelas
- **Ikon launcher** adaptif (struk + centang, emerald)
- **firestore.rules**: security rules multi-tenant siap deploy
  (`firebase deploy --only firestore:rules`)

## Struktur kode

```
com.kasirku.app
├── MainActivity.kt, KasirKuApp.kt      # entry point, DI manual sederhana
├── ui/
│   ├── navigation/Nav.kt               # login gate → paywall gate → bottom nav
│   ├── theme/                          # Material3: emerald + paper hangat, dark mode
│   ├── login/                          # setup PIN owner + PIN pad kasir
│   ├── kasir/                          # KasirScreen + KasirViewModel + checkout + struk
│   ├── produk/                         # list, tambah/edit (+ tier grosir)
│   ├── supplier/                       # supplier + barang masuk
│   ├── piutang/                        # daftar bon + cicilan
│   ├── scan/                           # CameraX + ML Kit
│   ├── laporan/                        # ringkasan harian + daftar transaksi
│   ├── langganan/                      # status, paywall, Play Billing
│   └── pengaturan/                     # nama toko, sync manual, langganan, keluar
├── data/
│   ├── local/                          # Room v4: produk, transaksi, user, piutang,
│   │                                   # pelanggan, supplier, stok masuk + DAO
│   ├── PosRepository.kt / Impl         # checkout transaksional (stok − atomik)
│   ├── SessionManager.kt               # sesi login PIN
│   ├── TrialManager.kt                 # trial 30 hari
│   └── remote/FirestoreModels.kt       # mapping ke skema Firestore multi-tenant
├── billing/BillingManager.kt           # skeleton Google Play Billing
├── domain/                             # Pricing, ReceiptBuilder, Security (hash PIN)
├── sync/SyncWorker.kt                  # sinkronisasi malam hari
└── firestore.rules                     # security rules multi-tenant
```

Keputusan penting:
- **Harga pakai `Long` (Rupiah)** — tanpa float, tanpa error pembulatan.
- **Item transaksi = snapshot** (nama + harga disalin) — laporan historis kebal
  perubahan harga.
- **Tier grosir otomatis** — qty memenuhi `minQty` → harga tier termurah yang cocok.

## Yang belum (butuh akun/konfigurasi Anda)

1. **Firebase**: tambah `google-services.json` + plugin `google-services`,
   isi TODO di `SyncWorker`, deploy `firestore.rules`, atur custom claims
   (`storeId`, `role`) via Admin SDK
2. **Play Billing**: buat produk subscription di Play Console, ganti
   `PRODUCT_IDS` di `BillingManager.kt`, verifikasi purchase di backend
3. **Cetak thermal Bluetooth**: kirim teks dari `buildReceipt()` ke printer
   ESC/POS via BluetoothSocket (butuh tipe printer untuk perintah pastinya)

## Skema Firestore (multi-tenant)

Lihat `data/remote/FirestoreModels.kt`. Pola: `stores/{storeId}/produk|transaksi|...`
dengan custom claims `storeId` + `role` di Firebase Auth untuk security rules.
