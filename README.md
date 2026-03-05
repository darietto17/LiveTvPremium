# LiveTvPremium

LiveTvPremium è un ecosistema completo per lo streaming IPTV premium, progettato nativamente per Android (Mobile & TV) e affiancato da un potente motore Python per la generazione e la classificazione dinamica delle liste M3U.

## 🚀 Novità Release v1.1.3 (Stability & DNS)

- **DNS-over-HTTPS (DoH)**: Integrazione nativa per cambiare i DNS (es. Cloudflare 1.1.1.1 o Google 8.8.8.8) direttamente nelle impostazioni per bypassare blocchi ISP senza configurazioni di sistema.
- **Universal Proxy Support**: Il Proxy URL ora viene applicato correttamente a tutti i tipi di contenuto (Live, Film, Serie TV).
- **Stabilità Live (Phase 30)**: Aumento del buffer iniziale a 15 secondi per i flussi Live e ottimizzazione dei timeout di rete (30s) per eliminare i blocchi continui.
- **TV Remote Avanzato**: Gestione intelligente del tasto BACK (nasconde i controlli prima di uscire) e ottimizzazione del focus D-PAD.
- **Update System**: Sezione aggiornamenti sempre visibile nelle impostazioni per un facile download delle nuove release APK.
- **Bug Fix Serie TV**: Risolto il problema di riproduzione ("Source error") su flussi VOD tramite proxy.

## 🛠️ Architettura del Progetto

Il progetto è diviso in due macro-componenti interconnessi:

### 1. Generatore Python (`/scripts`)
Uno script automatizzato che scarica, unisce, riordina e pulisce le playlist M3U grezze.
- **Parsing Intelligente**: Estrae i canali da molteplici fonti internazionali.
- **Supporto EPG**: Genera file XMLTV completi.
- **Classificazione Dinamica (`categories.json`)**: Il cuore del sistema. Permette di definire macro-categorie personalizzate e assegnarle ai canali in base a keyword, ripulendo i tag `group-title` sporchi.

### 2. App Android (`/app`)
Un client nativo all'avanguardia scritto in **Kotlin** e **Jetpack Compose**.
- **Interfaccia Netflix-Style**: Dashboard fluide con supporto Master-Detail per TV e Tablet (split-screen categorie/contenuti).
- **EPG Real-time**: Caricamento asincrono della Guida TV con visualizzazione del programma corrente e successivi.
- **Remote Management**: Pulsanti dedicati per attivare le GitHub Actions di aggiornamento liste direttamente dallo smartphone o telecomando TV.
- **Integrazione TMDB API**: Arricchisce on-the-fly Film e Serie con locandine HD, trame e trailer.
- **Player Premium**: Sfrutta `Media3` (ExoPlayer) con supporto PiP, selezione tracce e monitoraggio della Cronologia ("Continua a guardare").
- **Navigazione TV Ottimizzata**: Logica di "Focus Leap" per muoversi velocemente tra categorie e barra di navigazione con il D-PAD.

## 📱 Build & Setup

Per utilizzare o compilare l'App:
1. **Repository**: Scarica l'ultima release APK dalla sezione [Releases](https://github.com/darietto17/LiveTvPremium/releases).
2. **Setup Personale**: Inserisci il tuo Token GitHub nelle impostazioni dell'app per attivare la sincronizzazione privata e la gestione dei server.
3. **Sviluppo**: Apri la root in **Android Studio**, inserisci le tue chiavi API (TMDB) e compila il progetto.

---
*Progetto a scopo di studio sull'interoperabilità tra sistemi distribuiti Python/GitHub e interfacce Android moderne.*
